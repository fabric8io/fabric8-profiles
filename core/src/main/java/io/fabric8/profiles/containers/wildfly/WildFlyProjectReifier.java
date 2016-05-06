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
package io.fabric8.profiles.containers.wildfly;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import io.fabric8.profiles.containers.VelocityBasedReifier;

/**
 * Reify Karaf container from Profiles
 */
public class WildFlyProjectReifier extends VelocityBasedReifier {

    private static final String KARAF_POM_VM = "/containers/wildfly/pom.vm";

    private static final String CONFIG_PREFIX = "config.";
    private static final String SYSTEM_PREFIX = "system.";

    public static final String CONTAINER_TYPE = "wildfly";

    private static final String SWARM_FRACTIONS_PROPERTIES = "swarm.fractions.properties";
    
    public WildFlyProjectReifier(Properties properties) {
        super(properties);
    }

    public void reify(Path target, Properties config, Path profilesDir) throws IOException {
        // reify maven project using template
        Properties containerProperties = new Properties();
        containerProperties.putAll(defaultProperties);
        containerProperties.putAll(config);
        reifyProject(target, profilesDir, containerProperties);
    }

    private void reifyProject(Path target, final Path profilesDir, Properties properties) throws IOException {
        final File pojoFile = new File(target.toFile(), "pom.xml");
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(pojoFile));

            if( properties.getProperty("groupId")==null ) {
                properties.setProperty("groupId", "container");
            }
            if( properties.getProperty("version")==null ) {
                properties.setProperty("version", getProjectVersion());
            }
            if( properties.getProperty("description")==null ) {
                properties.setProperty("description", "");
            }

            VelocityContext context = new VelocityContext();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                context.put(entry.getKey().toString(), entry.getValue());
            }

            // read profile properties
            loadProperties(context, profilesDir);

            log.debug(String.format("Writing %s...", pojoFile));
            Template pojoTemplate = engine.getTemplate(KARAF_POM_VM);
            pojoTemplate.merge(context, writer);

            // close pojoFile
            writer.close();

            // add other resource files under src/main/resources/assembly
            final Path assemblyPath = target.resolve("src/main/resources/assembly/etc");
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

    private String getProjectVersion() {
        // TODO: perhpas use the git hash?
        return "1.0-SNAPSHOT";
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

    	Properties props = new Properties();

        // get config.properties
        Map<String, String> configMap = new HashMap<>();
        getPrefixedProperty(props, CONFIG_PREFIX, configMap);
        context.put("configProperties", configMap.entrySet());

        // get system.properties
        Map<String, String> systemMap = new HashMap<>();
        getPrefixedProperty(props, SYSTEM_PREFIX, systemMap);
        context.put("systemProperties", systemMap.entrySet());
        
        // add profile fractions
        File propsFile = profilesDir.resolve(SWARM_FRACTIONS_PROPERTIES).toFile();
        if (propsFile.exists()) {
        	Properties fprops = new Properties();
        	try (Reader fr = new FileReader(propsFile)) {
            	fprops.load(fr);
        	}
            List<Fraction> fractions = new ArrayList<>();
        	String[] specs = fprops.getProperty("fractions").split(",");
        	for (String spec : specs) {
        		spec = spec.trim();
        		int index = spec.indexOf(':');
        		String groupId = spec.substring(0, index);
        		String artifactId = spec.substring(index + 1);
                fractions.add(new Fraction(groupId, artifactId));
        	}
            context.put("fractions", fractions);
        }
    }

	public class Fraction {
		public final String groupId;
		public final String artifactId;
	
		Fraction(String groupId, String artifactId) {
			this.groupId = groupId;
			this.artifactId = artifactId;
		}

		public String getGroupId() {
			return groupId;
		}

		public String getArtifactId() {
			return artifactId;
		}
	}
}
