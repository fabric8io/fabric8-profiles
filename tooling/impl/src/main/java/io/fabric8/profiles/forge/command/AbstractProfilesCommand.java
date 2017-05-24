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
package io.fabric8.profiles.forge.command;

import javax.inject.Inject;

import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginInstaller;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIInputMany;

/**
 * Base class for Fabric8 Profiles commands.
 */
public abstract class AbstractProfilesCommand extends AbstractProjectCommand {

    @Inject
    protected MavenPluginInstaller mavenPluginInstaller;

    @Inject
    protected ProjectFactory projectFactory;

    protected Project getSelectedProjectOrNull(UIContext context) {
        return Projects.getSelectedProject(this.getProjectFactory(), context);
    }

    protected DirectoryResource getRoot(UIExecutionContext context) {
        return getRoot(getSelectedProjectOrNull(context.getUIContext()));
    }

    protected static DirectoryResource getRoot(Project project) {
        return project.getRoot().reify(DirectoryResource.class);
    }

    protected boolean isProfilesProject(Project project) {
        return isPluginInstalled(project, CoordinateBuilder.create("io.fabric8.profiles:fabric8-profiles-maven-plugin"));
    }

    protected boolean isPluginInstalled(Project project, CoordinateBuilder coordinateBuilder) {
        return mavenPluginInstaller.isInstalled(project, MavenPluginBuilder.create()
            .setCoordinate(coordinateBuilder));
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return this.projectFactory;
    }

    protected boolean isEmpty(UIInput<String> option) {
        return isEmpty(option.getValue());
    }

    protected boolean isEmpty(UIInputMany<?> option) {
        return option.getValue() == null || !option.getValue().iterator().hasNext();
    }

    protected boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
