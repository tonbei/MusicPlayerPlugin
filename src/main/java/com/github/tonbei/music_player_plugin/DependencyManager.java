package com.github.tonbei.music_player_plugin;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

public class DependencyManager {

    public static ClassLoader load(File pluginFile, ClassLoader classLoader) {
        try {
            HashSet<URL> dependencies = new HashSet<>();
            URI jarURI = pluginFile.toURI();
            JarFile jarFile = new JarFile(pluginFile);
            Enumeration<JarEntry> entries = jarFile.entries();
            URLClassLoader cl;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("dependencies/") && name.endsWith(".jar")) {
//                    Bukkit.getLogger().warning(name);
                    URL url = new URL("jar:" + jarURI + "!/" + entry.getName());
//                    Bukkit.getLogger().warning(url.toString());

                    dependencies.add(url);
                }
            }

            cl = URLClassLoader.newInstance(dependencies.toArray(new URL[0]), classLoader);
            Bukkit.getLogger().severe(Arrays.toString(cl.getURLs()));

            entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("dependencies/") && name.endsWith(".jar")) {
                    JarInputStream jarIS = new JarInputStream(jarFile.getInputStream(entry));
                    JarEntry innerEntry = jarIS.getNextJarEntry();
                    while (innerEntry != null) {
//                        Bukkit.getLogger().warning(innerEntry.getName());

                        // ファイル要素に限って（＝ディレクトリをはじいて）スキャン
                        if (innerEntry.isDirectory()) {
                            innerEntry = jarIS.getNextJarEntry();
                            continue;
                        }

                        // classファイルに限定
                        final String fileName = innerEntry.getName();
                        if (!fileName.endsWith(".class")) {
                            innerEntry = jarIS.getNextJarEntry();
                            continue;
                        }

                        try {
                            cl.loadClass(fileName.substring(0, fileName.length() - 6).replace('/', '.'));
                        } catch (ClassNotFoundException e) {
                            Bukkit.getLogger().warning(fileName.substring(0, fileName.length() - 6).replace('/', '.'));
                            innerEntry = jarIS.getNextJarEntry();
                            continue;
                        }

                        innerEntry = jarIS.getNextJarEntry();
                    }
                }
            }
        } catch (IOException | IllegalStateException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }
}
