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
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInputMany;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class ContainerAddProfiles extends AbstractContainerCommand {

    @Inject
    @WithAttributes(label = "Profiles", required = true, description = "Fabric Profiles to add to the container.")
    private UIInputMany<ProfileResource> profiles;

    @Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ContainerAddProfiles.class)
				.name("PContainer: Add Profiles")
				.description("Adds Profiles to a PContainer")
				.category(Categories.create("Fabric8 Profiles"));
	}

    @Override
    protected void doInitializeUI(UIBuilder builder, Project project) {
        profiles.setCompleter(profileCompleter)
            .setValueConverter(profileResourceConverter.setProject(project));

        builder.add(profiles);
    }

    @Override
	public Result execute(UIExecutionContext context) throws Exception {

        container.getValue().addProfiles(profiles.getValue());

        return Results.success("Fabric8 Profiles successfully added.");
	}

}