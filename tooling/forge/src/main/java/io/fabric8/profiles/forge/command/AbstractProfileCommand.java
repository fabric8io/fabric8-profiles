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

import io.fabric8.profiles.forge.resource.ProfileResource;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;

/**
 * Base class for Profile commands.
 */
public abstract class AbstractProfileCommand extends AbstractProfilesProjectCommand {

    @Inject
    @WithAttributes(label = "Fabric8 Profile", required = true, description = "Fabric8 Profile for operation.")
    protected UIInput<ProfileResource> profile;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Project project = getSelectedProjectOrNull(builder.getUIContext());
        profile.setCompleter(profileCompleter)
            .setValueConverter(profileResourceConverter.setProject(project));

        Object selection = builder.getUIContext().getInitialSelection().get();
        if (selection instanceof ProfileResource) {
            profile.setDefaultValue((ProfileResource) selection);
        }

        builder.add(profile);

        doInitializeUI(builder, project);
    }

    protected abstract void doInitializeUI(UIBuilder builder, Project project);
}
