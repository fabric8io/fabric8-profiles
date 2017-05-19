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

public class ContainerRemoveProfiles extends AbstractContainerCommand {

	@Inject
	@WithAttributes(label = "Profiles", required = true)
	private UISelectMany<ProfileResource> profiles;

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ContainerRemoveProfiles.class)
			.name("PContainer: Remove Profiles")
			.description("Removes Profiles from a PContainer")
			.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	public void doInitializeUI(UIBuilder builder, Project project) throws Exception {
		profiles.setValueChoices(profileUtils.getProfiles(getRoot(project), ""))
			.setValueConverter(profileResourceConverter.setProject(project));

		builder.add(profiles);
	}

    @Override
	public Result execute(UIExecutionContext context) throws Exception {

		container.getValue().removeProfiles(profiles.getValue());

		return Results.success("Fabric8 Profiles successfully removed.");
	}
}