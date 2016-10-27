package io.fabric8.profiles.forge.command;

import javax.inject.Inject;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

public class ProfileCopy extends AbstractProfileCommand {

	@Inject
	@WithAttributes(label = "Name", required = true, description = "New Fabric8 Profile name.")
	private UIInput<String> name;

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ProfileCopy.class).name("Profile: Copy")
			.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	protected void doInitializeUI(UIBuilder builder, Project project) {
		builder.add(name);
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		profile.getValue().copyTo(name.getValue());

		return Results.success("Fabric8 Profile renamed successfully.");
	}
}