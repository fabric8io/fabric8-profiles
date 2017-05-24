/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.profiles.forge.command.profileimport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.fabric8.profiles.containers.Constants;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.yaml.snakeyaml.Yaml;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class ProfileImportUtil {

    private static final String LATIN1_ENCODING = "8859-1";

    public static Result execute(ProfileImportConfig config, Path dataDir, Path target) throws Exception {

        final String separator = dataDir.getFileSystem().getSeparator();
	    List<Result> results = new ArrayList<>();

        final int[] profiles = {0};
        final int[] resources = { 0 };
        final boolean[] failed = {false};
	    Map<String, String> renamedProfiles = new HashMap<>();

		// process profiles
		Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                Path resourcePath = dataDir.relativize(file);
			    // handle profile rename
                String profilePath = "";
                if (resourcePath.getParent() != null) {
                    profilePath = resourcePath.getParent().toString().replaceAll(separator, "-");
                }
                profilePath = renamedProfiles.getOrDefault(profilePath, profilePath);

                // process resources
                final FileVisitResult[] result = {CONTINUE};
                String[] name = { file.getFileName().toString() };
                String[] path = { profilePath };
                config.getResourceCommands().forEach(resource -> {

                    if (matches(name[0], path[0], resource)) {

                        ProfileImportConfig.Level logLevel = resource.getLogLevel();
                        switch (resource.getAction()) {

                        case delete:
                            result[0] = SKIP_SUBTREE;
                            if (logLevel != null) {
                                String msg = resource.getLogMessage() == null ?
                                    "Ignoring profile %s" : resource.getLogMessage();
                                results.add(Results.success(String.format(logLevel + ": " + msg, name[0])));
                            }
                            break;

                        case rename:
                            result[0] = CONTINUE;
                            if (logLevel != null) {
                                String msg = resource.getLogMessage() == null ?
                                    "Renaming resource %s" : resource.getLogMessage();
                                results.add(Results.success(String.format(logLevel + ": " + msg, name[0])));
                            }
                            if (!resource.getRegExp()) {
                                name[0] = resource.getReplaceWith();
                            } else {
                                name[0] = name[0].replaceAll(resource.getName(), resource.getReplaceWith());
                            }
                            break;

                        case fail:
                            result[0] = TERMINATE;
                            String msg = resource.getLogMessage() == null ?
                                "Failing due to resource %s" : resource.getLogMessage();
                            results.add(Results.fail(String.format("FATAL: " + msg, name[0])));
                            failed[0] = true;
                            break;
                        }
                    }
                });

                if (result[0] == CONTINUE) {
                    Files.copy(file, target.resolve((path[0].equals("") ? "" : path[0].replaceAll("-", separator) + separator) + name[0]));
                    resources[0]++;
                }
				return result[0];
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                final Path profilePath = dataDir.relativize(dir);
                final String[] name = {dir.getFileName().toString()};
                final String[] path = {profilePath.toString().replace(separator, "-")};

                final FileVisitResult[] result = { CONTINUE };
                config.getProfileCommands().forEach(profile -> {

                    if (matches(name[0], path[0], profile)) {

                        ProfileImportConfig.Level logLevel = profile.getLogLevel();
                        switch (profile.getAction()) {

                        case delete:
                            result[0] = SKIP_SUBTREE;
                            if (logLevel != null) {
                                String msg = profile.getLogMessage() == null ?
                                    "Ignoring profile %s" : profile.getLogMessage();
                                results.add(Results.success(String.format(logLevel + ": " + msg, name[0])));
                            }
                            break;

                        case rename:
                            result[0] = CONTINUE;
                            if (logLevel != null) {
                                String msg = profile.getLogMessage() == null ?
                                    "Renaming profile %s" : profile.getLogMessage();
                                results.add(Results.success(String.format(logLevel + ": " + msg, name[0])));
                            }
                            String key = path[0];
                            if (!profile.getRegExp()) {
                                path[0] = path[0].substring(0, path[0].lastIndexOf(name[0])) + profile.getReplaceWith();
                            } else {
                                path[0] = path[0].replaceAll(profile.getName(), profile.getReplaceWith());
                            }
                            renamedProfiles.put(key, path[0]);
                            break;

                        case fail:
                            result[0] = TERMINATE;
                            String msg = profile.getLogMessage() == null ?
                                "Failing due to profile %s" : profile.getLogMessage();
                            results.add(Results.fail(String.format("FATAL: " + msg, name[0])));
                            failed[0] = true;
                            break;
                        }
                    }
                });

                if (result[0] == CONTINUE) {
                    final Path copy = target.resolve(path[0].replace("-", separator));
                    if (Files.notExists(copy)) {
                        Files.createDirectories(copy);
                        profiles[0]++;
                    }
                }

                return result[0];
			}

        });

		if (!failed[0]) {

            // process profile names in io.fabric8.profiles.properties
		    Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    final FileVisitResult[] result = {CONTINUE};
                    if (dir.endsWith(".profile")) {

                        Path profileProps = dir.resolve(Constants.FABRIC8_AGENT_PROPERTIES);
                        if (Files.exists(profileProps)) {
                            List<String> profilePropContents = Files.readAllLines(profileProps).stream().map(line -> {

                                Matcher matcher = Constants.ATTRIBUTE_PARENTS_PATTERN.matcher(line);
                                if (matcher.matches()) {
                                    // replace profile names according to config
                                    String profiles = matcher.group(1);
                                    return Arrays.stream(profiles.split(" ")).map(profileName -> {

                                        final String[] retVal = { profileName.replaceAll("-", "/") };
                                        int lastIndex = profileName.lastIndexOf('-');
                                        final String name;
                                        if (lastIndex != -1) {
                                            name = profileName.substring(lastIndex + 1);
                                        } else {
                                            name = profileName;
                                        }
                                        config.getProfileCommands().forEach(profile -> {

                                            if (matches(name, profileName, profile)) {
                                                switch (profile.getAction()) {

                                                case delete:
                                                    // skip this profile
                                                    retVal[0] = "";

                                                case rename:
                                                    if (!profile.getRegExp()) {
                                                        retVal[0] = profile.getReplaceWith();
                                                    } else {
                                                        retVal[0] = profileName.replaceAll(profile.getName(), profile.getReplaceWith());
                                                    }
                                                    break;

                                                case fail:
                                                    result[0] = TERMINATE;
                                                    retVal[0] = "";
                                                    String msg = profile.getLogMessage() == null ?
                                                        "Failing due to referenced profile %s" : profile.getLogMessage();
                                                    results.add(Results.fail(String.format("FATAL: " + msg, name)));
                                                    failed[0] = true;
                                                    break;
                                                }
                                            }
                                        });

                                        return retVal[0];

                                    }).collect(Collectors.joining(" "));

                                } else {
                                    return line;
                                }

                            }).collect(Collectors.toList());
                            Files.write(profileProps, profilePropContents);
                        } else {
                            results.add(Results.fail("FATAL: Missing file " + profileProps));
                            failed[0] = true;
                            result[0] = TERMINATE;
                        }
                    }

                    return result[0];
                }
            });

            if (!failed[0]) {
                results.add(Results.success(String.format("Successfully imported %s profiles with %s resources!",
                    profiles[0], resources[0])));
            }
        }
		return Results.aggregate(results);
	}

    private static boolean matches(String name, String path, ProfileImportConfig.ImportCommand profile) {
        final String profileName = profile.getName();
        boolean matches = profileName.equals(name) || profileName.equals(path);
        if (!matches && profile.getRegExp() && Pattern.matches(profileName, path)) {
            matches = true;
        }
        return matches;
    }

    public static ProfileImportConfig getDefaultConfig(Yaml yaml) throws IOException {
        ProfileImportConfig config;
        try (InputStream is = ProfileImportUtil.class.getResourceAsStream("/defaults/profile-import.yaml")) {
            config = yaml.loadAs(is, ProfileImportConfig.class);
        }
        return config;
    }
}