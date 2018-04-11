package io.fabric8.profiles.forge.command;

import javax.inject.Inject;

import io.fabric8.profiles.forge.resource.ProfileResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class ProfileChangeParents extends AbstractProfileCommand {

	@Inject
	@WithAttributes(label = "Profiles", required = true)
	private UISelectMany<ProfileResource> parents;

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ProfileChangeParents.class)
			.name("Profile: Change Parents")
			.description("Changes Parent Profiles for a Fabric8 Profile")
			.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	public void doInitializeUI(UIBuilder builder, Project project) {
		parents.setValueChoices(profileUtils.getProfiles(getRoot(project)))
			.setValueConverter(profileResourceConverter.setProject(project));

		builder.add(parents);
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {

		profile.getValue().setParentProfiles(parents.getValue());

		return Results.success("Fabric8 Parent Profiles successfully changed.");
	}
}