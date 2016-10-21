package io.fabric8.profiles.forge.command;

import java.util.Collections;
import javax.inject.Inject;

import io.fabric8.profiles.forge.resource.ProfileResource;

import org.jboss.forge.addon.projects.Project;
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

public class ProfileCreate extends AbstractProfilesProjectCommand {

    @Inject
	@WithAttributes(label = "Fabric8 Profile Name", required = true, description = "Fabric8 Profile name to create.")
	private UIInput<String> name;

	@Inject
	@WithAttributes(label = "Fabric8 Profile Parents", required = false, description = "Fabric8 Profile parents for the new Profile.")
	private UISelectMany<ProfileResource> parents;

    @Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ProfileCreate.class).name("Fabric8 Profiles: Profile Create")
				.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
        Project project = getSelectedProjectOrNull(builder.getUIContext());

        parents.setValueChoices(profileUtils.getProfiles(getRoot(project), ""))
            .setValueConverter(profileResourceConverter.setProject(project))
            .setDefaultValue(Collections.singletonList(profileResourceConverter.convert("default")));

		builder.add(name)
			.add(parents);
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		profileUtils.createProfile(getRoot(context), name.getValue(), parents.getValue());

		return Results.success("Fabric8 Profile successfully created.");
	}
}