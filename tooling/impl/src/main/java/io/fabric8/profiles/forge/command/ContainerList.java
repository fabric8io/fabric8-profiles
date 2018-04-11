package io.fabric8.profiles.forge.command;

import java.util.stream.Collectors;

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class ContainerList extends AbstractProfilesProjectCommand {

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ContainerList.class).name("PContainer: List")
				.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
        final String containers = containerUtils.getContainers(getRoot(context)).stream()
                .map(c -> c.getName()).collect(Collectors.joining(" "));
        context.getUIContext().getProvider().getOutput().out().println(containers);
        return Results.success();
	}
}