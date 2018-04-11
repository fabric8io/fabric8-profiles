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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.fabric8.profiles.ProfilesHelpers;

import org.jboss.forge.addon.ui.result.CompositeResult;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Results;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author dhirajsb
 */
public class ContainerImportUtilTest {

    private Path testPath;

    @Before
    public void setUp() throws Exception {
        // remove old test output
        testPath = Paths.get("target", "test-data", "configs", "containers");
        ProfilesHelpers.deleteDirectory(testPath);
        Files.createDirectories(testPath);
    }

    @Test
    public void execute() throws Exception {
        final CompositeResult result = Results.aggregate(Arrays.asList(Results.success()));
        final List<String> allProfiles = Arrays.asList("fabric");
        ContainerImportUtil.execute(
                Paths.get("target", "test-classes", "repos", "karafA", "zk"),
                testPath, result, new HashMap<>(), allProfiles);
        assertTrue(result.getResults().stream().noneMatch(res -> res instanceof Failed));
    }

}