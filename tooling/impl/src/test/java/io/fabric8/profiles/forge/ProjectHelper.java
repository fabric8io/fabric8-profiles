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
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginInstaller;
import org.jboss.forge.addon.maven.projects.MavenBuildSystem;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;

/**
 * Project Helper.
 */
public class ProjectHelper {

    @Inject
    ProjectFactory projectFactory;

    @Inject
    MavenBuildSystem mavenBuildSystem;

    @Inject
    MavenPluginInstaller pluginInstaller;

    public Project createEmptyProject() {
        return projectFactory.createTempProject(mavenBuildSystem);
    }

    public Project refreshProject(Project project) {
        return projectFactory.findProject(project.getRoot(), mavenBuildSystem);
    }

    public Project createProfilesProject() {
        Project project = createEmptyProject();
        MavenPluginBuilder profilesPlugin = MavenPluginBuilder.create()
            .setCoordinate(CoordinateBuilder.create("io.fabric8.profiles:fabric8-profiles-maven-plugin:1.0-SNAPSHOT"));
        profilesPlugin.setExtensions(true);
        pluginInstaller.install(project, profilesPlugin);
        // create a default profile
        Resource<?> root = project.getRoot();
        FileResource<?> defaultProfile = root.getChild("profiles/default.profile").reify(FileResource.class);
        defaultProfile.mkdirs();
        FileResource<?> agentProps = root.getChild("profiles/default.profile/" + Constants.FABRIC8_AGENT_PROPERTIES).reify(FileResource.class);
        agentProps.createNewFile();

        return refreshProject(project);
    }
}
