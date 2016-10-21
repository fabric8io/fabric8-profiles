/**
 * Copyright 2005-2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.profiles.forge.command;

import javax.inject.Inject;

import io.fabric8.profiles.forge.resource.PContainerResource;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;

/**
 * Base class for Container commands.
 */
public abstract class AbstractContainerCommand extends AbstractProfilesProjectCommand {

    @Inject
    @WithAttributes(label = "Profiled Container", required = true, description = "Profiled container name")
    protected UIInput<PContainerResource> container;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Project project = getSelectedProjectOrNull(builder.getUIContext());

        container.setCompleter(containerCompleter)
            .setValueConverter(containerResourceConverter.setProject(project));

        Object selection = builder.getUIContext().getInitialSelection().get();
        if (selection instanceof PContainerResource) {
            container.setDefaultValue((PContainerResource) selection);
        }

        builder.add(container);

        doInitializeUI(builder, project);
    }

    protected abstract void doInitializeUI(UIBuilder builder, Project project) throws Exception;
}
