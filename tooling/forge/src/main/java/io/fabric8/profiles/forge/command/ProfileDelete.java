package io.fabric8.profiles.forge.command;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class ProfileDelete extends AbstractProfileCommand {

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ContainerAddProfiles.class)
			.name("Fabric8 Profiles: Profile Delete")
			.description("Deletes a Fabric8 Profile")
			.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	protected void doInitializeUI(UIBuilder builder, Project project) {
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {

		profile.getValue().delete(true);

		return Results.success("Fabric8 Profile successfully deleted.");
	}
}