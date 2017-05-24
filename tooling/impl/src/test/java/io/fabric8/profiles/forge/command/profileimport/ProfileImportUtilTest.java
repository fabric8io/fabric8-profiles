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

import io.fabric8.profiles.ProfilesHelpers;
import org.jboss.forge.addon.ui.result.CompositeResult;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import static org.junit.Assert.assertTrue;

/**
 * Test profile import support.
 */
public class ProfileImportUtilTest {

    private Path testPath;

    @Before
    public void setUp() throws Exception {
        // remove old test output
        testPath = Paths.get("target", "test-data", "profiles");
        ProfilesHelpers.deleteDirectory(testPath);
        Files.createDirectories(testPath);
    }

    @Test
    public void execute() throws Exception {
        Result result = ProfileImportUtil.execute(ProfileImportUtil.getDefaultConfig(new Yaml()),
            Paths.get("target", "test-classes", "repos", "karafA", "profiles"),
            testPath);
        assertTrue(((CompositeResult)result).getResults().stream().filter( res -> res instanceof Failed ).count() == 0);
    }

}