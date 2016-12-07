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

import java.io.File;

import io.fabric8.profiles.forge.command.ContainerAddProfiles;
import io.fabric8.profiles.forge.command.ContainerChangeProfiles;
import io.fabric8.profiles.forge.command.ContainerCreate;
import io.fabric8.profiles.forge.command.ContainerDelete;
import io.fabric8.profiles.forge.command.ContainerRemoveProfiles;
import io.fabric8.profiles.forge.command.ProfileCreate;
import io.fabric8.profiles.forge.command.ProfileDelete;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class ContainerTest extends AbstractProfilesTest {

    @Test
	public void testAddon() throws Exception {
        Project project = projectHelper.createProfilesProject();
        project = executeCommand(project, ContainerCreate.class, tester -> {
            tester.setValueFor("name", "test-container");
//            tester1.setValueFor("profiles", "default");
//            tester.setValueFor("containerTypes", "karaf jenkinsfile"));
        }, "Fabric8 PContainer has been installed.");
        assertTrue(project.getRoot().getChild("configs/containers/test-container.yaml").exists());
        assertTrue(project.getRoot().getChild("configs/containers/test-container.yaml").getContents().contains("profiles: default"));

        project = executeCommand(project, ProfileCreate.class, tester -> {
            tester.setValueFor("name", "new-profile");
        }, "Fabric8 Profile successfully created.");
        assertTrue(new File(project.getRoot().reify(DirectoryResource.class).getUnderlyingResourceObject(), "profiles/new/profile.profile").isDirectory());

        project = executeCommand(project, ContainerAddProfiles.class, tester -> {
            tester.setValueFor("container", "test-container");
            tester.setValueFor("profiles", "new-profile");
        }, "Fabric8 Profiles successfully added.");
        assertTrue(project.getRoot().getChild("configs/containers/test-container.yaml").getContents().contains("profiles: default new-profile"));

        project = executeCommand(project, ContainerRemoveProfiles.class, tester -> {
            tester.setValueFor("container", "test-container");
            tester.setValueFor("profiles", "default");
        }, "Fabric8 Profiles successfully removed.");
        assertFalse(project.getRoot().getChild("configs/containers/test-container.yaml").getContents().contains("default"));

        project = executeCommand(project, ContainerChangeProfiles.class, tester -> {
            tester.setValueFor("container", "test-container");
            tester.setValueFor("profiles", "default");
        }, "Fabric8 Profiles successfully changed.");
        assertTrue(project.getRoot().getChild("configs/containers/test-container.yaml").getContents().contains("default"));
        assertFalse(project.getRoot().getChild("configs/containers/test-container.yaml").getContents().contains("new-profile"));

        project = executeCommand(project, ContainerDelete.class, tester -> {
            tester.setValueFor("container", "test-container");
        }, "Fabric8 PContainer successfully deleted.");
        assertFalse(project.getRoot().getChild("configs/containers/test-container.yaml").exists());

        project = executeCommand(project, ProfileDelete.class, tester -> {
            tester.setValueFor("profile", "new-profile");
        }, "Fabric8 Profile successfully deleted.");
        assertFalse(project.getRoot().getChild("profiles/new/profile.profile").exists());
	}

}