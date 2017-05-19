package io.fabric8.profiles.forge.command;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;

import io.fabric8.profiles.forge.command.profileimport.ProfileImportConfig;
import io.fabric8.profiles.forge.command.profileimport.ProfileImportUtil;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

public class ProfileImport extends AbstractProfilesProjectCommand {

	@Inject
	@WithAttributes(label = "Import Config", required = false, description = "Profile Import Actions Configuration.")
	private UIInput<URI> importConfig;

	@Inject
	@WithAttributes(label = "Profile Data Dir", required = true, description = "Location of Fabric8 V1 Profile data.")
	private UIInput<File> profileDataDir;

	// TODO handle container config from ZK export data

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ProfileImport.class).name("profile-import")
				.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		builder.add(importConfig)
			.add(profileDataDir);
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {

		Yaml yaml = new Yaml();
		ProfileImportConfig defaultConfig = ProfileImportUtil.getDefaultConfig(yaml);

		URI configUri = importConfig.getValue();
		ProfileImportConfig config;
		if (!StringUtils.isEmpty(configUri)) {
			try (InputStream is = configUri.toURL().openStream()) {
				config = yaml.loadAs(is, ProfileImportConfig.class);
			}

			// always add default commands to user defined commands
			config.getProfileCommands().addAll(defaultConfig.getProfileCommands());
			config.getResourceCommands().addAll(defaultConfig.getResourceCommands());
		} else {
			config = defaultConfig;
		}

		// validate data dir
		Path dataDir = Paths.get(profileDataDir.getValue().toURI());
		if (!Files.exists(dataDir)) {
			throw new IllegalArgumentException("Missing Profile data directory " + dataDir);
		}

		Project selectedProject = getSelectedProject(context.getUIContext());
		Path target = Paths.get(selectedProject.getRoot().reify(DirectoryResource.class).getChildDirectory("profiles").getUnderlyingResourceObject().toURI());
		return ProfileImportUtil.execute(config, dataDir, target);
	}

}