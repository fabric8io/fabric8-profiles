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
package io.fabric8.profiles.containers.resources;

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
import java.util.Map;
import java.util.Properties;
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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import static io.fabric8.profiles.ProfilesHelpers.readPropertiesFile;
import static io.fabric8.profiles.config.ConfigHelper.toValue;

/**
 * Reify Fabric8 Resources project from Profiles
 */
public class ResourcesProjectReifier extends VelocityBasedReifier {

    private static final String RESOURCES_POM_VM = "/containers/resources/pom.vm";

    public static final String CONTAINER_TYPE = "fabric8-resources";

    private static final Pattern PROFILE_URL_PATTERN = Pattern.compile("([^:]+):profile:([^:]+)");

    private static final String FABRIC8_RESOURCES_PATH = "src/main/resources/fabric8/";
    private static final Pattern RESOURCE_FILE_EXTENSIONS = Pattern.compile("(yaml)|(yml)|(json)");

    public ResourcesProjectReifier(JsonNode defaultConfig) {
        super(defaultConfig);
    }

    public void reify(Path target, JsonNode config, Path profilesDir) throws IOException {
        // reify maven project using template
        final JsonNode containerProperties = ProfilesHelpers.merge(config.deepCopy(), defaultProperties);
        reifyProject(target, profilesDir, containerProperties);
    }

    private void reifyProject(Path target, final Path profilesDir, JsonNode config) throws IOException {
        final File pojoFile = new File(target.toFile(), "pom.xml");
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(pojoFile));

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

            log.debug(String.format("Writing %s...", pojoFile));
            Template pojoTemplate = engine.getTemplate(RESOURCES_POM_VM);
            pojoTemplate.merge(context, writer);

            // close pojoFile
            writer.close();

            // add other resource files under src/main/resources/fabric8
            Path resourcesPath = target.resolve(FABRIC8_RESOURCES_PATH);
            log.debug(String.format("Writing resources to %s...", resourcesPath));
            Files.createDirectories(resourcesPath,
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));
            Files.walkFileTree(profilesDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(final Path dir,
                                                         final BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(resourcesPath.resolve(profilesDir.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file,
                                                 final BasicFileAttributes attrs) throws IOException {

                    Path targetPath = resourcesPath.resolve(profilesDir.relativize(file));
                    String fileName = file.getFileName().toString();

                    // Skip over profile file that we know are not resource configs.
                    if (!Constants.FABRIC8_AGENT_PROPERTIES.equals(fileName)) {

                        String extension = extension(fileName);

                        // copy resource config files
                        if (extension != null && RESOURCE_FILE_EXTENSIONS.matcher(extension).matches()) {

                            Files.createDirectories(targetPath.getParent());
                            Files.copy(file, targetPath);

                            // inject properties in config maps
                            final JsonNode jsonNode;
                            if ("json".equals(extension)) {
                                jsonNode = ProfilesHelpers.readJsonFile(file);
                            } else {
                                jsonNode = ProfilesHelpers.readYamlFile(file);
                            }

                            final boolean[] updated = {false};
                            jsonNode.findValues("data").stream()
                                .forEach( data ->
                                    data.fields().forEachRemaining( entry -> {
                                        Path propertiesFile = profilesDir.resolve(entry.getKey());
                                        if (Files.exists(propertiesFile)) {
                                            try {
                                                TextNode value = JsonNodeFactory.instance.textNode(Files.readAllLines(propertiesFile).stream().collect(Collectors.joining(System.getProperty("line.separator"))));
                                                ((ObjectNode)data).set(entry.getKey(), value);

                                                updated[0] = true;
                                            } catch (IOException e) {
                                                throw new IllegalArgumentException(String.format("Error reading file %s: %s", propertiesFile, e.getMessage()), e);
                                            }
                                        }
                                    }));

                            // write updated config back to file
                            if (updated[0]) {
                                if (extension.equals("json")) {
                                    Files.write(targetPath, ProfilesHelpers.toJsonBytes(jsonNode));
                                } else {
                                    Files.write(targetPath, ProfilesHelpers.toYamlBytes(jsonNode));
                                }
                            }
                        }
                    }

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

    private void loadProperties(VelocityContext context, Path profilesDir) throws IOException {
        Path agentProperties = profilesDir.resolve(Constants.FABRIC8_AGENT_PROPERTIES);
        if (Files.exists(agentProperties)) {
            Properties props = readPropertiesFile(agentProperties);
        } else {
            throw new IOException("Missing file " + agentProperties);
        }
    }

}
