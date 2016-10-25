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

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.config.ContainerConfigDTO;
import io.fabric8.profiles.config.MavenConfigDTO;

import org.junit.Test;

import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;
import static io.fabric8.profiles.config.ConfigHelper.fromValue;
import static io.fabric8.profiles.config.ConfigHelper.toValue;

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

		Path config = REPOSITORY_BASE_DIR.resolve("configs/containers/root.yaml");
        ContainerConfigDTO containerConfigDTO = toValue(ProfilesHelpers.readYamlFile(config), ContainerConfigDTO.class);
        String[] profileNames = containerConfigDTO.getProfiles().split(" ");
		new Profiles(REPOSITORY_BASE_DIR.resolve("profiles")).materialize(materialized, profileNames);

		WildFlyProjectReifier reifier;
		Path defaultPath = materialized.resolve("default.yaml");
		JsonNode defaultConfig = ProfilesHelpers.readYamlFile(defaultPath);
		reifier = new WildFlyProjectReifier(defaultConfig);

        final MavenConfigDTO mavenConfigDTO = new MavenConfigDTO();
        mavenConfigDTO.setGroupId("io.fabric8.profiles.test");
        mavenConfigDTO.setVersion("1.0-SNAPSHOT");
        mavenConfigDTO.setName("WildFly Swarm Profile Test");
        mavenConfigDTO.setDescription("WildFly Swarm Camel Container");
        containerConfigDTO = new ContainerConfigDTO();
        containerConfigDTO.setName("wildfly-swarm-test");

        final ObjectNode containerConfig = (ObjectNode) fromValue(mavenConfigDTO);
        containerConfig.set("container", fromValue(containerConfigDTO).get("container"));

		reifier.reify(target, containerConfig, materialized);
	}
}
