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
package io.fabric8.profiles.forge;

import javax.inject.Inject;

import io.fabric8.profiles.containers.Constants;
import io.fabric8.profiles.forge.command.ProjectSetup;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginInstaller;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class ProjectSetupTest extends AbstractProfilesTest {

    @Inject
    private MavenPluginInstaller pluginInstaller;

    private MavenPlugin profilesPlugin = MavenPluginBuilder.create()
        .setCoordinate(CoordinateBuilder.create("io.fabric8.profiles:fabric8-profiles-maven-plugin:1.0-SNAPSHOT"));

    @Test
	public void testAddon() throws Exception {
        Project project = projectHelper.createEmptyProject();

        project = executeCommand(project, ProjectSetup.class, tester -> {
            tester.setValueFor("version", "1.0-SNAPSHOT");
        }, "Fabric8 Profiles has been installed.");

        assertTrue(pluginInstaller.isInstalled(project, profilesPlugin));
        Resource<?> projectRoot = project.getRoot();
        assertTrue(projectRoot.getChild("fabric8-profiles.yaml").exists());
        assertTrue(projectRoot.getChild("configs/containers").exists());
        assertTrue(projectRoot.getChild("profiles/default.profile").exists());
        assertTrue(projectRoot.getChild("profiles/default.profile/" + Constants.FABRIC8_AGENT_PROPERTIES).exists());
	}
}