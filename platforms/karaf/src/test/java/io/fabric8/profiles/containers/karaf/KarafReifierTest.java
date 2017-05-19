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

import java.nio.file.Files;
import java.nio.file.Path;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.config.ContainerConfigDTO;
import io.fabric8.profiles.config.MavenConfigDTO;
import io.fabric8.profiles.containers.VelocityBasedReifier;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;
import static io.fabric8.profiles.config.ConfigHelper.fromValue;
import static io.fabric8.profiles.config.ConfigHelper.toValue;

/**
 * Test Karaf reifier.
 */
public class KarafReifierTest {

    static final Path REPOSITORIES_BASE_DIR = PROJECT_BASE_DIR.resolve("src/test/resources/repos");
    
    @Test
    public void testReify() throws Exception {

        Path target = PROJECT_BASE_DIR.resolve("target/test-data/karafA");
        ProfilesHelpers.deleteDirectory(target);
        Files.createDirectories(target);

        Path repository = REPOSITORIES_BASE_DIR.resolve("karafA/profiles");
        final Path materialized = PROJECT_BASE_DIR.resolve("target/test-data/karafA-materialized");
        ProfilesHelpers.deleteDirectory(materialized);
        Files.createDirectories(materialized);

        final Path containerConfig = REPOSITORIES_BASE_DIR.resolve("karafA/configs/containers/root.yaml");
        ContainerConfigDTO containerConfigDTO = toValue(ProfilesHelpers.readYamlFile(containerConfig), ContainerConfigDTO.class);
        String[] profileNames = containerConfigDTO.getProfiles().replaceAll(" ?fabric-ensemble-\\S+", "").split(" ");
        new Profiles(repository).materialize(materialized, profileNames);

        final MavenConfigDTO mavenConfigDTO = new MavenConfigDTO();
        mavenConfigDTO.setGroupId("io.fabric8.quickstarts");
        mavenConfigDTO.setVersion("1.0-SNAPSHOT");
        mavenConfigDTO.setDescription("Karaf root container");
        containerConfigDTO = new ContainerConfigDTO();
        containerConfigDTO.setName("root");

        final ObjectNode config = (ObjectNode) fromValue(mavenConfigDTO);
        config.set("container", fromValue(containerConfigDTO).get("container"));

        final VelocityBasedReifier reifier = new KarafProjectReifier(null);
        reifier.reify(target, config, materialized);
    }
}
