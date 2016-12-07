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

public class ContainerDelete extends AbstractContainerCommand {

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ContainerDelete.class)
			.name("PContainer: Delete")
			.description("Deletes a PContainer")
			.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	public void doInitializeUI(UIBuilder builder, Project project) throws Exception {
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {

		container.getValue().delete();

		return Results.success("Fabric8 PContainer successfully deleted.");
	}

}