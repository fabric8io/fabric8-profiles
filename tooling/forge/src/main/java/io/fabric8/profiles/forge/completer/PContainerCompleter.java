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
package io.fabric8.profiles.forge.completer;

import javax.inject.Inject;

import io.fabric8.profiles.forge.ContainerUtils;
import io.fabric8.profiles.forge.resource.PContainerResource;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

/**
 * {@link UICompleter} for {@link PContainerResource}.
 */
public class PContainerCompleter implements UICompleter<PContainerResource> {

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private ContainerUtils containerUtils;

    @Override
    public Iterable<PContainerResource> getCompletionProposals(UIContext uiContext, InputComponent<?, PContainerResource> inputComponent, String value) {
        Project project = Projects.getSelectedProject(projectFactory, uiContext);
        return containerUtils.getContainers(project.getRoot().reify(DirectoryResource.class), value);
    }
}
