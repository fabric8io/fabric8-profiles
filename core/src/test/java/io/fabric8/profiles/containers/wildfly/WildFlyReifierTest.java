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

import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.Test;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.ProfilesHelpers;

public class WildFlyReifierTest {

	static final Path REPOSITORY_BASE_DIR = PROJECT_BASE_DIR.resolve("src/test/resources/repos/wildflyA");

	@Test
	public void testReify() throws Exception {

		Path target = PROJECT_BASE_DIR.resolve("target/test-data/wildflyA");
		ProfilesHelpers.deleteDirectory(target);
		Files.createDirectories(target);

		Path materialized = PROJECT_BASE_DIR.resolve("target/test-data/wildflyA-materialized");
		ProfilesHelpers.deleteDirectory(materialized);
		Files.createDirectories(materialized);

		Path config = REPOSITORY_BASE_DIR.resolve("configs/containers/root.cfg");
		String[] profileNames = ProfilesHelpers.readPropertiesFile(config).getProperty("profiles").split(" ");
		new Profiles(REPOSITORY_BASE_DIR.resolve("profiles")).materialize(materialized, profileNames);

		WildFlyProjectReifier reifier;
		Path defaultPath = materialized.resolve("default.properties");
		try (FileInputStream input = new FileInputStream(defaultPath.toFile())) {
			Properties properties = new Properties();
			properties.load(input);
			reifier = new WildFlyProjectReifier(properties);
		}

		Properties properties = new Properties();
		properties.put("groupId", "io.fabric8.profiles.test");
		properties.put("artifactId", "wildfly-swarm-test");
		properties.put("version", "1.0-SNAPSHOT");
		properties.put("name", "WildFly Swarm Profile Test");
		properties.put("description", "WildFly Swarm Camel Container");

		reifier.reify(target, properties, materialized);
	}
}
