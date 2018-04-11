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
import io.fabric8.profiles.forge.command.ProfileImport;
import io.fabric8.profiles.forge.resource.YamlConfigResource;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ContainerTest extends AbstractProfilesTest {

    @Test
	public void testAddon() throws Exception {
        Project project = projectHelper.createProfilesProject();
        final Resource<?> projectRoot = project.getRoot();

        new CommandTest(project)
        .execute(ContainerCreate.class, tester -> {
            tester.setValueFor("name", "test-container");
//            tester1.setValueFor("profiles", "default");
//            tester.setValueFor("containerTypes", "karaf jenkinsfile"));
            }, "Fabric8 PContainer has been installed.")
        .assertTrue(projectRoot.getChild("configs/containers/test-container.yaml").exists())
        .assertTrue(convert(projectRoot.getChild("configs/containers/test-container.yaml"), YamlConfigResource.class) != null)
        .assertTrue(projectRoot.getChild("configs/containers/test-container.yaml").getContents().contains("profiles: default"))

        .execute(ProfileCreate.class, tester -> {
            tester.setValueFor("name", "new-profile");
        }, "Fabric8 Profile successfully created.")
//        .assertTrue(convert(project.getRoot().getChild("profiles/new/profile.profile"), ProfileResource.class) != null)
        .assertTrue(new File(projectRoot.reify(DirectoryResource.class).getUnderlyingResourceObject(), "profiles/new/profile.profile").isDirectory())

        .execute(ContainerAddProfiles.class, tester -> {
            tester.setValueFor("container", "test-container");
            tester.setValueFor("profiles", "new-profile");
        }, "Fabric8 Profiles successfully added.")
        .assertTrue(projectRoot.getChild("configs/containers/test-container.yaml").getContents().contains("profiles: default new-profile"))

        .execute(ContainerRemoveProfiles.class, tester -> {
            tester.setValueFor("container", "test-container");
            tester.setValueFor("profiles", "default");
        }, "Fabric8 Profiles successfully removed.")
        .assertFalse(projectRoot.getChild("configs/containers/test-container.yaml").getContents().contains("default"))

        .execute(ContainerChangeProfiles.class, tester -> {
            tester.setValueFor("container", "test-container");
            tester.setValueFor("profiles", "default");
        }, "Fabric8 Profiles successfully changed.")
        .assertTrue(projectRoot.getChild("configs/containers/test-container.yaml").getContents().contains("default"))
        .assertFalse(projectRoot.getChild("configs/containers/test-container.yaml").getContents().contains("new-profile"))

        .execute(ContainerDelete.class, tester -> {
            tester.setValueFor("container", "test-container");
        }, "Fabric8 PContainer successfully deleted.")
        .assertFalse(projectRoot.getChild("configs/containers/test-container.yaml").exists())

        .execute(ProfileDelete.class, tester -> {
            tester.setValueFor("profile", "new-profile");
        }, "Fabric8 Profile successfully deleted.")
        .assertFalse(projectRoot.getChild("profiles/new/profile.profile").exists())

        .execute(ProfileImport.class, tester -> {
            tester.setValueFor("profileDataDir", "target/test-classes/repos/karafA/profiles");
            tester.setValueFor("zkDataDir", "target/test-classes/repos/karafA/zk");
        }, "Successfully imported 67 profiles with 396 resources!", "Successfully imported 1 containers!")
        .assertTrue(projectRoot.getChild("profiles/autoscale.profile/io.fabric8.agent.properties").exists())
        .assertTrue(projectRoot.getChild("configs/containers/root.yaml").exists());
	}

}