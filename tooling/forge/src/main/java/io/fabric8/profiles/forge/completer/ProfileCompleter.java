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

import io.fabric8.profiles.forge.ProfileUtils;
import io.fabric8.profiles.forge.resource.ProfileResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

/**
 * {@link UICompleter} for {@link ProfileResource}.
 */
public class ProfileCompleter implements UICompleter<ProfileResource> {

    @Inject
    private ProfileUtils profileUtils;

    @Inject
    private ProjectFactory projectFactory;

    @Override
    public Iterable<ProfileResource> getCompletionProposals(UIContext context, InputComponent<?, ProfileResource> input, String value) {
        Project project = Projects.getSelectedProject(projectFactory, context);
        return profileUtils.getProfiles(project.getRoot().reify(DirectoryResource.class), value);
    }
}
