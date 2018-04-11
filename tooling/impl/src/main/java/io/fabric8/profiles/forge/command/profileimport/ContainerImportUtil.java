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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.config.ContainerConfigDTO;

import org.jboss.forge.addon.ui.result.CompositeResult;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

/**
 * Utility class to import container config.
 * @author dhirajsb
 */
public abstract class ContainerImportUtil {

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public static CompositeResult execute(Path dataDir, Path targetDir, CompositeResult result, Map<String, String> renamedProfiles, List<String> allProfiles) {

        final List<Result> results = new ArrayList<>(result.getResults());
        final Path configsDir = dataDir.resolve("fabric").resolve("configs");

        // get container names with versions
        final Path containersDir = configsDir.resolve("containers");
        final int[] containers = {0};
        if (Files.exists(containersDir)) {
            try {
                final Map<String, String> versions = Files.list(containersDir).collect(Collectors.toMap(c -> c.toFile
                        ().getName().replaceAll("\\..*", ""), c -> {
                    try {
                        return Files.readAllLines(c, UTF_8).get(0);
                    } catch (IOException e) {
                        final String msg = String.format("Error reading container config for %s: %s", c, e
                                .getMessage());
                        results.add(Results.fail(msg, e));
                    }
                    // null if error
                    return null;
                }));

                // read container config from versions and create new containers
                versions.forEach((container, version) -> {
                    if (version != null) {
                        final Path profilesFile = configsDir.resolve("versions").resolve(version).resolve
                                ("containers").resolve(container);

                        try {
                            final String oldProfiles = Files.readAllLines(profilesFile, UTF_8).get(0);
                            // rename profiles as needed
                            // TODO handle deleted profiles
                            final String newProfiles = Arrays.stream(oldProfiles.split(" "))
                                    .map(p -> renamedProfiles.getOrDefault(p, p))
                                    .filter(allProfiles::contains)
                                    .collect(Collectors.joining(" "));

                            // is the container being skipped
                            if (newProfiles.trim().isEmpty()) {
                                results.add(Results.success(String.format("Skipping container %s, no migrated profiles found", container)));
                            } else {

                                final ContainerConfigDTO configDTO = new ContainerConfigDTO();
                                configDTO.setName(container);
                                configDTO.setProfiles(newProfiles);
                                // hard coded list of container types
                                configDTO.setContainerType("karaf jenkinsfile");

                                // create new container config
                                final HashMap<String, Object> config = new HashMap<>();
                                config.put("container", configDTO);
                                ProfilesHelpers.YAML_MAPPER.writeValue(targetDir.resolve(container + ".yaml").toFile(), config);

                                containers[0]++;
                            }

                        } catch (IOException e1) {
                            final String msg = String.format("Error writing container config for %s: " + "%s",
                                    container, e1.getMessage());
                            results.add(Results.fail(msg, e1));
                        }
                    }
                });

            } catch (IOException e) {
                final String msg = String.format("Error reading containers from %s: %s", containersDir, e
                        .getMessage());
                results.add(Results.fail(msg, e));
            }
        }

        final CompositeResult aggregate = Results.aggregate(results);
        if (aggregate.getResults().stream().noneMatch( r -> r instanceof Failed)) {
            results.add(Results.success(String.format("Successfully imported %s containers!", containers[0])));
        }
        return aggregate;
    }
}
