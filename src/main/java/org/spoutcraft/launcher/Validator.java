/*
 * This file is part of Spoutcraft Launcher.
 *
 * Copyright (c) 2011-2012, SpoutDev <http://www.spout.org/>
 * Spoutcraft Launcher is licensed under the SpoutDev License Version 1.
 *
 * Spoutcraft Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Spoutcraft Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spoutcraft.launcher;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import org.spoutcraft.launcher.api.Launcher;
import org.spoutcraft.launcher.api.util.FileType;
import org.spoutcraft.launcher.util.MD5Utils;
import org.spoutcraft.launcher.yml.Resources;
import org.spoutcraft.launcher.yml.SpoutcraftBuild;

public class Validator implements Runnable {
	private boolean passed = false;
	private boolean errors = false;

	public void run() {
		((SimpleGameUpdater)Launcher.getGameUpdater()).setStartValidationTime(System.currentTimeMillis());
		errors = !validate();
		((SimpleGameUpdater)Launcher.getGameUpdater()).validationFinished(passed);
	}

	/**
	 * Returns true if validation completed without errors, false if something went wrong deleting files
	 * 
	 * @return true on validation completion, false on failure
	 */
	private boolean validate() {
		SpoutcraftBuild build = SpoutcraftBuild.getSpoutcraftBuild();
		File minecraftJar = new File(Launcher.getGameUpdater().getBinDir(), "minecraft.jar");
		if (minecraftJar.exists()) {
			if (!compareMD5s(build, FileType.MINECRAFT, minecraftJar)) {
				err("Invalid minecraft.jar");
				return minecraftJar.delete();
			}
		} else {
			err("There is no minecraft.jar!");
			return true;
		}

		File spoutcraft = new File(Launcher.getGameUpdater().getBinDir(), "spoutcraft.jar");
		if (spoutcraft.exists()) {
			if (!compareSpoutcraftMD5s(build, spoutcraft)) {
				err("Invalid spoutcraft.jar");
				return spoutcraft.delete();
			}
		} else {
			err("There is no spoutcraft.jar");
			return true;
		}

		File jinputJar = new File(Launcher.getGameUpdater().getBinDir(), "jinput.jar");
		if (jinputJar.exists()) {
			if (!compareMD5s(build, FileType.JINPUT, jinputJar)) {
				err("Invalid jinput.jar");
				return jinputJar.delete();
			}
		} else {
			err("There is no jinput.jar");
			return true;
		}

		File lwjglJar = new File(Launcher.getGameUpdater().getBinDir(), "lwjgl.jar");
		if (lwjglJar.exists()) {
			if (!compareMD5s(build, FileType.LWJGL, lwjglJar)) {
				err("Invalid lwjgl.jar");
				return lwjglJar.delete();
			}
		} else {
			err("There is no lwjgl.jar");
			return true;
		}

		File lwjgl_utilJar = new File(Launcher.getGameUpdater().getBinDir(), "lwjgl_util.jar");
		if (lwjgl_utilJar.exists()) {
			if (!compareMD5s(build, FileType.LWJGL_UTIL, lwjgl_utilJar)) {
				err("Invalid lwjgl_util.jar");
				return lwjgl_utilJar.delete();
			}
		} else {
			err("There is no lwjgl_util.jar");
			return true;
		}

		File libDir = new File(Launcher.getGameUpdater().getBinDir(), "lib");
		Map<String, Object> libraries = build.getLibraries();
		Iterator<Map.Entry<String, Object>> i = libraries.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<String, Object> lib = i.next();
			String version = String.valueOf(lib.getValue());
			//String name = lib.getKey() + "-" + version;

			File libraryFile = new File(libDir, lib.getKey() + ".jar");

			if (libraryFile.exists()) {
				if (!compareLibraryMD5s(lib.getKey(), version, libraryFile)) {
					err("Invalid " + libraryFile.getName());
					return libraryFile.delete();
				}
			} else {
				err("There is no " + libraryFile.getName());
				return true;
			}
		}
		passed = true;
		return true;
	}

	/**
	 * Returns true if the validator confirmed that all the files were correct
	 * 
	 * @return passed validation
	 */
	public boolean isValid() {
		return passed;
	}

	/**
	 * Returns true if the validator encountered an error while validating
	 * 
	 * @return true if an error occured
	 */
	public boolean hasErrors() {
		return errors;
	}

	private boolean compareMD5s(SpoutcraftBuild build, FileType type, File file) {
		return compareMD5s(type, build.getMinecraftVersion(), file);
	}

	private boolean compareMD5s(FileType type, String version, File file) {
		String expected = MD5Utils.getMD5(type, version);
		String actual = MD5Utils.getMD5(file);
		debug("Checking MD5 of " + type.name() + ". Expected MD5: " + expected + " | Actual MD5: " + actual);
		if (expected == null || actual == null) {
			return false;
		}
		return expected.equals(actual);
	}

	private boolean compareSpoutcraftMD5s(SpoutcraftBuild build, File file) {
		String expected = build.getMD5();
		String actual = MD5Utils.getMD5(file);
		debug("Checking MD5 of Spoutcraft. Expected MD5: " + expected + " | Actual MD5: " + actual);
		if (expected == null || actual == null) {
			return false;
		}
		return expected.equals(actual);
	}

	private boolean compareLibraryMD5s(String lib, String version, File file) {
		String expected = Resources.getLibraryMD5(lib, version);
		String actual = MD5Utils.getMD5(file);
		debug("Checking MD5 of " + lib + ". Expected MD5: " + expected + " | Actual MD5: " + actual);
		if (expected == null || actual == null) {
			return false;
		}
		return expected.equals(actual);
	}

	@SuppressWarnings("unused")
	private void print(Object obj) {
		System.out.println(obj);
	}

	private void debug(Object obj) {
		System.out.println(obj);
	}

	private void err(Object obj) {
		System.err.println(obj);
	}
}
