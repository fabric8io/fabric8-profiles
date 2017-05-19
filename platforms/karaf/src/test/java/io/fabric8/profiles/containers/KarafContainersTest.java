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
package io.fabric8.profiles.containers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.config.ConfigHelper;
import io.fabric8.profiles.config.MavenConfigDTO;
import io.fabric8.profiles.containers.karaf.KarafProjectReifier;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import static io.fabric8.profiles.ProfilesHelpers.deleteDirectory;
import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;

/**
 * Test Karaf Containers.
 */
public class KarafContainersTest {

    static final Path REPOSITORIES_BASE_DIR = PROJECT_BASE_DIR.resolve("src/test/resources/repos");
    
    @Test
    public void testReify() throws Exception {
        // temp karaf project output dir
        Path target = PROJECT_BASE_DIR.resolve("target/test-data/karafA");
        deleteDirectory(target);
        Files.createDirectories(target);

        // temp profile git repo
        final Path repository = PROJECT_BASE_DIR.resolve("target/test-data/repos/karafA");
        final Path profilesRoot = repository.resolve("profiles");
        final Path configsRoot = repository.resolve("configs");
        deleteDirectory(repository);
        Files.createDirectories(profilesRoot);
        Files.createDirectories(configsRoot);

        // copy integration test repository
        ProfilesHelpers.copyDirectory(REPOSITORIES_BASE_DIR.resolve("karafA"), repository);

        final MavenConfigDTO mavenConfigDTO = new MavenConfigDTO();
        mavenConfigDTO.setGroupId("io.fabric8.karaf-swarm");
        mavenConfigDTO.setDescription("Karaf Swarm container");
        final JsonNode karafDefaults = ConfigHelper.fromValue(mavenConfigDTO);

        final HashMap<String, ProjectReifier> reifierMap = new HashMap<>();
        reifierMap.put(KarafProjectReifier.CONTAINER_TYPE, new KarafProjectReifier(karafDefaults));
        reifierMap.put(JenkinsfileReifier.CONTAINER_TYPE, new JenkinsfileReifier(karafDefaults));

        final Containers containers = new Containers(configsRoot, reifierMap, new Profiles(profilesRoot));
        containers.reify(target, "root");
    }

}