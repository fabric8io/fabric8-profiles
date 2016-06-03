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
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import io.fabric8.profiles.containers.VelocityBasedReifier;

/**
 * Reify WildFly container from Profiles
 */
public class WildFlyProjectReifier extends VelocityBasedReifier {

	public static final String CONTAINER_TYPE = "wildfly";
	public static final String DOMAIN_NAMESPACE = "urn:jboss:domain:4.0";

	private static final String CONTAINER_PATH = "/containers/wildfly";
	private static final String POM_VM = CONTAINER_PATH + "/pom.vm";
	private static final String MAIN_VM = CONTAINER_PATH + "/src/main/java/Main.java";

	private static final String FRACTION_PREFIX = "fraction.";
	private static final String CONFIG_PREFIX = "config.";
	private static final String SYSTEM_PREFIX = "system.";

	private static final String SWARM_FRACTION_PROPERTIES = "fraction.properties";

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

		if (properties.getProperty("artifactId") == null) {
			properties.setProperty("artifactId", "acme-swarm-test");
		}
		if (properties.getProperty("groupId") == null) {
			properties.setProperty("groupId", "org.acme.fabric8.swarm");
		}
		if (properties.getProperty("version") == null) {
			properties.setProperty("version", getProjectVersion());
		}
		if (properties.getProperty("description") == null) {
			properties.setProperty("description", "");
		}

		properties.setProperty("mainClass", properties.getProperty("groupId") + ".Main");

		VelocityContext context = new VelocityContext();
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			context.put(entry.getKey().toString(), entry.getValue());
		}

		// read profile properties
		loadProperties(context, profilesDir);

		reifyPOM(target, context);
		reifyMainJava(target, context, properties);
		reifyStandaloneXML(target, context, profilesDir, properties);
	}

	private void reifyPOM(Path target, VelocityContext context) throws IOException {
		final File targetFile = new File(target.toFile(), "pom.xml");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
			log.debug(String.format("Writing %s...", targetFile));
			Template pojoTemplate = engine.getTemplate(POM_VM);
			pojoTemplate.merge(context, writer);
		}
	}

	private void reifyMainJava(Path target, VelocityContext context, Properties properties) throws IOException {
		String groupId = properties.getProperty("groupId");
		Path packagePath = target.resolve("src/main/java/" + groupId.replace('.', '/'));
		File targetFile = new File(packagePath.toFile(), "Main.java");
		targetFile.getParentFile().mkdirs();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
			log.debug(String.format("Writing %s...", targetFile));
			Template pojoTemplate = engine.getTemplate(MAIN_VM);
			pojoTemplate.merge(context, writer);
		}
	}

	private void reifyStandaloneXML(Path target, VelocityContext context, Path profilesDir, Properties properties) throws IOException {
		String groupId = properties.getProperty("groupId");
		Path packagePath = target.resolve("src/main/resources/" + groupId.replace('.', '/'));

		String targetFileName = "standalone.xml";
		File targetFile = new File(packagePath.toFile(), targetFileName);
		targetFile.getParentFile().mkdirs();
		try (PrintWriter targetWriter = new PrintWriter(new FileWriter(targetFile))) {
			log.debug(String.format("Writing %s...", targetFile));
			Path stagesPath = profilesDir.resolve("project-stages.yml");
	        YamlTransformer transformer = createYamlTransformer().transform(stagesPath);
			XMLOutputter outputer = new XMLOutputter(Format.getPrettyFormat());
			StringWriter strwr = new StringWriter();
	        for (String key : transformer) { 
	        	if (transformer.getNamespace(key) != null) {
			        Element element = transformer.getElement(key);
			        if (key.equals("datasources")) {
			        	element = new Element("datasources", element.getNamespace()).addContent(element);
			        }
			        element.setName("subsystem");
					outputer.output(element, strwr);
					new PrintWriter(strwr).println();
	        	}
	        }
			targetWriter.println("<server xmlns=\"" + DOMAIN_NAMESPACE + "\">");
			targetWriter.println("<profile>");
			if (!engine.evaluate(context, targetWriter, targetFileName, strwr.toString())) 
				throw new IllegalStateException("Cannot render: " + targetFileName);
			targetWriter.println("</profile>");
			targetWriter.println("</server>");
		}
	}

	private YamlTransformer createYamlTransformer() {
		return new YamlTransformer().namespace("datasources", "urn:jboss:domain:datasources:4.0");
	}

	private String getProjectVersion() {
		return "1.0-SNAPSHOT";
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
		File propsFile = profilesDir.resolve(SWARM_FRACTION_PROPERTIES).toFile();
		if (propsFile.exists()) {
			Properties fprops = new Properties();
			try (Reader fr = new FileReader(propsFile)) {
				fprops.load(fr);
			}
			List<Fraction> fractions = new ArrayList<>();
			Set<String> specs = getPrefixedProperty(fprops, FRACTION_PREFIX);
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
