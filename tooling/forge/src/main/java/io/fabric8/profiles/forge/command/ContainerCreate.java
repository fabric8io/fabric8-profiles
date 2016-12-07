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

import java.util.Collections;
import javax.inject.Inject;

import io.fabric8.profiles.config.ContainerConfigDTO;
import io.fabric8.profiles.forge.resource.ProfileResource;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class ContainerCreate extends AbstractProfilesProjectCommand {

    private static final String DEFAULT_CONTAINER_TYPES = "karaf jenkinsfile";
    private static final String DEFAULT_PROFILES = "default";

	@Inject
	@WithAttributes(label = "Name", required = true, description = "Fabric8 Profiled Container name.")
	private UIInput<String> name;

	@Inject
	@WithAttributes(label = "Profiles", required = true, description = "Fabric8 Profiles to deploy.")
	private UISelectMany<ProfileResource> profiles;

	@Inject
	@WithAttributes(label = "Container Types", required = true, description = "Fabric8 Container Types.", defaultValue = DEFAULT_CONTAINER_TYPES)
	private UIInput<String> containerTypes;

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ContainerCreate.class)
			.name("PContainer: Create")
			.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		Project project = getSelectedProjectOrNull(builder.getUIContext());

        profileResourceConverter.setProject(project);
        profiles.setValueChoices(profileUtils.getProfiles(project.getRoot().reify(DirectoryResource.class), ""))
			.setValueConverter(profileResourceConverter)
            .setDefaultValue(Collections.singletonList(profileResourceConverter.convert(DEFAULT_PROFILES)));

		builder.add(name)
            .add(profiles)
            .add(containerTypes);
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {

        ContainerConfigDTO configDTO = new ContainerConfigDTO();
        configDTO.setName(name.getValue());
        configDTO.setProfiles(profileUtils.getProfileNameList(profiles.getValue()));
		configDTO.setContainerType(containerTypes.getValue());
        containerUtils.createContainer(getRoot(context), configDTO);

		return Results.success("Fabric8 PContainer has been installed.");
	}
}