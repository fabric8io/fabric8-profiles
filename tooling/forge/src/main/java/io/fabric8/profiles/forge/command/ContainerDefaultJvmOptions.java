package io.fabric8.profiles.forge.command;

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class ContainerDefaultJvmOptions extends AbstractProfilesProjectCommand {

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ContainerDefaultJvmOptions.class)
				.name("PContainer: Default JVM Options")
				.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		return Results
				.success("Command 'pcontainer-default-jvm-options' successfully executed!");
	}
}