package io.fabric8.profiles.forge.command;

import io.fabric8.profiles.forge.resource.ProfileResource;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class ProfileEdit extends AbstractProfileCommand {

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ProfileDelete.class)
				.name("Profile: Edit")
				.description("Edits a Fabric8 Profile")
				.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	protected void doInitializeUI(UIBuilder builder, Project project) {
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		final ProfileResource profile = profileUtils.getProfile(getRoot(context), this.profile.getName());
		context.getUIContext().setSelection(profile);
		return Results.success();
	}
}