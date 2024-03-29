package com.questworld;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.questworld.api.QuestExtension;
import com.questworld.util.Log;
import org.bukkit.Bukkit;

public final class ExtensionLoader {
	private ClassLoader loader;
	private File folder;
	private Map<ClassLoader, List<QuestExtension>> classLoaders = new HashMap<>();

	private URL urlOf(File file) {
		try {
			return file.toURI().toURL();
		}
		catch (Exception e) {
			return null;
		}
	}

	public ExtensionLoader(ClassLoader loader, File folder) {
		this.loader = loader;
		this.folder = folder;
	}

	public List<QuestExtension> loadLocal() {
		// This is as much as bukkit checks, good enough for me!
		File[] extensionFiles = folder.listFiles((file, name) -> name.endsWith(".jar"));

		List<QuestExtension> extensions = new ArrayList<>();
		// Not a directory or unable to list files for some reason
		if (extensionFiles != null)
			for (File f : extensionFiles)
				extensions.addAll(load(f));

		return extensions;
	}

	public List<QuestExtension> load(File extensionFile) {
		Bukkit.getLogger().info("[FaceQuest] Loading quest expansion " + extensionFile.getName());

		JarFile jar;
		try {
			jar = new JarFile(extensionFile);
			Bukkit.getLogger().warning("[FaceQuest] - jar loaded!");
		} catch (Exception e) {
			Bukkit.getLogger().warning("[FaceQuest] - Failed to load " + extensionFile.getName());
			e.printStackTrace();
			return Collections.emptyList();
		}

		URL[] jarURLs = { urlOf(extensionFile) };

		URLClassLoader newLoader = AccessController.doPrivileged(
				(PrivilegedAction<URLClassLoader>) () -> new URLClassLoader(jarURLs, loader));

		Enumeration<JarEntry> entries = jar.entries();
		ArrayList<Class<?>> extensionClasses = new ArrayList<>();

		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if (entry.isDirectory() || !entry.getName().endsWith(".class"))
				continue;

			String className = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
			Log.finer("Loader - Loading class: " + className);
			Class<?> clazz;
			try {
				clazz = newLoader.loadClass(className);
			} catch (Throwable e) {
				Bukkit.getLogger().warning("Could not load class \"" + className + "\"");
				e.printStackTrace();
				continue;
			}

			if (QuestExtension.class.isAssignableFrom(clazz)) {
				Log.finer("Loader - Found extension class: " + className);
				extensionClasses.add(clazz);
			}
		}

		List<QuestExtension> extensions = new ArrayList<>();

		for (Class<?> extensionClass : extensionClasses) {
			Log.fine("Loader - Constructing: " + extensionClass.getName());
			QuestExtension extension;
			try {
				extension = (QuestExtension) extensionClass.getConstructor().newInstance();
			}
			catch (Throwable e) {
				Bukkit.getLogger().warning("Exception while constructing extension class \"" + extensionClass + "\"!");
				Bukkit.getLogger().warning("Is it missing a default constructor?");
				e.printStackTrace();
				continue;
			}

			extensions.add(extension);
		}

		classLoaders.put(newLoader, extensions);

		try {
			jar.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return extensions;
	}

	void unload() {

	}
}
