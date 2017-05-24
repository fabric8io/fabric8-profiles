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
package io.fabric8.profiles.containers.karaf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.config.ContainerConfigDTO;
import io.fabric8.profiles.config.MavenConfigDTO;
import io.fabric8.profiles.config.ProjectPropertiesDTO;
import io.fabric8.profiles.containers.Constants;
import io.fabric8.profiles.containers.VelocityBasedReifier;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import com.fasterxml.jackson.databind.JsonNode;

import static io.fabric8.profiles.ProfilesHelpers.readPropertiesFile;
import static io.fabric8.profiles.config.ConfigHelper.toValue;

/**
 * Reify Karaf container from Profiles
 */
public class KarafProjectReifier extends VelocityBasedReifier {

    private static final String KARAF_POM_VM = "/containers/karaf/pom.vm";
    private static final String KARAF2_POM_VM = "/containers/karaf2/pom.vm";

    private static final String REPOSITORY_PREFIX = "repository.";
    private static final String FEATURE_PREFIX = "feature.";
    private static final String BUNDLE_PREFIX = "bundle.";
    private static final String OVERRIDE_PREFIX = "override.";
    private static final String CONFIG_PREFIX = "config.";
    private static final String SYSTEM_PREFIX = "system.";
    private static final String LIB_PREFIX = "lib.";

    public static final String CONTAINER_TYPE = "karaf";

    private static final Pattern PROFILE_URL_PATTERN = Pattern.compile("([^:]+):profile:([^:]+)");

    private static final String KARAF_ASSEMBLY_PATH = "src/main/resources/assembly/etc";

    public KarafProjectReifier(JsonNode defaultConfig) {
        super(defaultConfig);
    }

    public void reify(Path target, JsonNode config, Path profilesDir) throws IOException {
        // reify maven project using template
        final JsonNode containerProperties = ProfilesHelpers.merge(config.deepCopy(), defaultProperties);
        reifyProject(target, profilesDir, containerProperties);
    }

    private void reifyProject(Path target, final Path profilesDir, JsonNode config) throws IOException {
        final File pomFile = new File(target.toFile(), "pom.xml");
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(pomFile));

            final MavenConfigDTO mavenConfigDTO = toValue(config, MavenConfigDTO.class);
            if (mavenConfigDTO.getGroupId() == null) {
                mavenConfigDTO.setGroupId("container");
            }
            if (mavenConfigDTO.getVersion() == null) {
                mavenConfigDTO.setVersion(getProjectVersion());
            }
            if (mavenConfigDTO.getDescription() == null) {
                mavenConfigDTO.setDescription("");
            }

            final VelocityContext context = new VelocityContext();
            // add container and maven config
            context.put("maven", mavenConfigDTO);
            context.put("container", toValue(config, ContainerConfigDTO.class));

            // add generic project properties
            final ProjectPropertiesDTO projectPropertiesDTO = toValue(config, ProjectPropertiesDTO.class);
            for (Map.Entry<String, Object> entry : projectPropertiesDTO.getProperties().entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }

            // read profile properties
            loadProperties(context, profilesDir);

            log.debug(String.format("Writing %s...", pomFile));

            // select template to use
            final Template pomTemplate = getTemplate(profilesDir, () -> {
                // read fabric8 properties to get Karaf version
                Path versionPropsFile = profilesDir.resolve(Constants.FABRIC8_VERSION_PROPERTIES);
                final Properties versionProps;
                if (Files.exists(versionPropsFile)) {
                    try {
                        versionProps = ProfilesHelpers.readPropertiesFile(versionPropsFile);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                } else {
                    versionProps = new Properties();
                }

                String karafVersion = versionProps.getProperty("karaf");
                final String templateName;
                if (karafVersion == null || karafVersion.startsWith("4.")) {
                    // Karaf4 template
                    templateName = KARAF_POM_VM;
                } else if (karafVersion.startsWith("2.")) {
                    // karaf2 template
                    templateName = KARAF2_POM_VM;
                } else {
                    throw new IllegalArgumentException("Provide user defined pom.vm template for unsupported Karaf version " + karafVersion);
                }
                return templateName;
            });

            // merge template and write pom.xml
            pomTemplate.merge(context, writer);

            // close pomFile
            writer.close();

            // add other resource files under src/main/resources/assembly
            Path assemblyPath = target.resolve(KARAF_ASSEMBLY_PATH);
            log.debug(String.format("Writing resources to %s...", assemblyPath));
            Files.createDirectories(assemblyPath,
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));
            Files.walkFileTree(profilesDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(final Path dir,
                                                         final BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(assemblyPath.resolve(profilesDir.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file,
                                                 final BasicFileAttributes attrs) throws IOException {

                    Path targetPath = assemblyPath.resolve(profilesDir.relativize(file));
                    String fileName = file.getFileName().toString();
                    if (Constants.FABRIC8_AGENT_PROPERTIES.equals(fileName)) {
                        return FileVisitResult.CONTINUE;
                    }

                    // Skip over profile file that we know are not karaf config.
                    if (
                        fileName.equalsIgnoreCase("icon.svg") ||
                            fileName.equalsIgnoreCase("readme.md") ||
                            fileName.equalsIgnoreCase("summary.md") ||
                            fileName.equalsIgnoreCase("Jenkinsfile") ||
                            fileName.equalsIgnoreCase("welcome.dashboard") ||
                            fileName.endsWith("#docker") ||
                            fileName.endsWith("#openshift")
                        ) {
                        return FileVisitResult.CONTINUE;
                    }

                    String extension = extension(fileName);
                    if ("properties".equals(extension)) {

                        // Lets put auth related files in the auth dir to keep things neat.
                        boolean isAuthFile = fileName.startsWith("jmx.acl.") || fileName.startsWith("org.apache.karaf.command.acl");
                        if (isAuthFile && profilesDir.relativize(file).getParent() == null) {
                            targetPath = assemblyPath.resolve("auth").resolve(profilesDir.relativize(file));
                        }

                        // Rename .properties files to .cfg files.
                        String targetName = withoutExtension(fileName) + ".cfg";
                        targetPath = targetPath.getParent().resolve(targetName);
                    }

                    Files.createDirectories(targetPath.getParent());
                    Files.copy(file, targetPath);
                    return FileVisitResult.CONTINUE;
                }
            });

            log.debug("Done!");

        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    static private String extension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1);
        }
        return null;
    }

    static private String withoutExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(0, i);
        }
        return fileName;
    }

    private void loadProperties(VelocityContext context, Path profilesDir) throws IOException {
        Path agentProperties = profilesDir.resolve(Constants.FABRIC8_AGENT_PROPERTIES);
        if (Files.exists(agentProperties)) {
            Properties props = readPropertiesFile(agentProperties);

            // read repositories
            context.put("repositories", getPrefixedProperty(props, REPOSITORY_PREFIX));

            // read features
            context.put("features", getPrefixedProperty(props, FEATURE_PREFIX));

            // read bundles
            context.put("bundles", relocateProfileUrls(getPrefixedProperty(props, BUNDLE_PREFIX)));

            // read bundle overrides
            context.put("blacklistedBundles", getPrefixedProperty(props, OVERRIDE_PREFIX));

            // get config.properties
            Map<String, String> configMap = new HashMap<>();
            getPrefixedProperty(props, CONFIG_PREFIX, configMap);
            context.put("configProperties", configMap.entrySet());

            // get system.properties
            Map<String, String> systemMap = new HashMap<>();
            getPrefixedProperty(props, SYSTEM_PREFIX, systemMap);
            context.put("systemProperties", systemMap.entrySet());

            // get libraries
            context.put("libraries", getPrefixedProperty(props, LIB_PREFIX));

            // TODO add support for lib/ext (ext.xxx), lib/endorsed (endorsed.xxx) in karaf maven plugin

        } else {
            throw new IOException("Missing file " + agentProperties);
        }
    }

    private Set<String> relocateProfileUrls(Set<String> properties) {
        return properties.stream()
            .map(PROFILE_URL_PATTERN::matcher)
            .filter(Matcher::matches)
            .map(matcher -> matcher.group(1) + ":file:" + KARAF_ASSEMBLY_PATH + "/" + matcher.group(2))
            .collect(Collectors.toSet());
    }
}
