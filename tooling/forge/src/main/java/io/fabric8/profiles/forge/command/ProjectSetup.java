/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.profiles.forge.command;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import io.fabric8.profiles.config.GitConfigDTO;
import io.fabric8.profiles.config.MavenConfigDTO;
import io.fabric8.profiles.forge.ProfileUtils;
import io.fabric8.profiles.forge.ResourceUtils;
import io.fabric8.profiles.forge.resource.YamlConfigResource;
import org.apache.maven.model.Model;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.resources.MavenModelResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class ProjectSetup extends AbstractProfilesCommand {

    private static final String UTF_8 = "UTF-8";

    @Inject
	@WithAttributes(label = "Fabric8 Profiles Version", required = false, description = "Fabric8 Profiles version to use. If none provided then the latest version will be used.")
	private UIInput<String> version;

    @Inject
    @WithAttributes(label = "Git Remote URI Pattern", required = true, defaultValue = "http://gogs/gogsadmin/${name}.git", description = "Git Remote URI Pattern for pushing Profiled Container projects")
    private UIInput<String> gitRemoteUriPattern;

    @Inject
    @WithAttributes(label = "Gogs Service Hostname", required = false, description = "Host where Gogs service resides for Container Source repository.")
    private UIInput<String> gogsServiceHost;

    @Inject
	@WithAttributes(label = "Gogs Username", required = true, defaultValue = "gogsadmin", description = "Gogs service username.")
	private UIInput<String> gogsUsername;

    @Inject
	@WithAttributes(label = "Gogs Password", required = true, defaultValue = "RedHat$1", description = "Gogs service password.")
	private UIInput<String> gogsPassword;

    @Inject
    @WithAttributes(label = "Default Container Description", required = false, description = "Maven project description for Profiled Containers.")
    private UIInput<String> containerDescription;

    @Inject
    @WithAttributes(label = "Default Container Group ID", required = false, description = "Maven project groupId for Profiled Containers.")
    private UIInput<String> containerGroupId;

    @Inject
    @WithAttributes(label = "Default Container Version", required = false, description = "Maven version for Profiled Containers.")
    private UIInput<String> containerVersion;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private ResourceUtils resourceUtils;

    @Inject
    private ProfileUtils profileUtils;

    @Override
	public boolean isEnabled(UIContext context) {
        Project project = getSelectedProjectOrNull(context);
        // only enable if we do not have Camel yet
        // must have a project
        // and the project must not have camel api component plugin already
        return project != null && !isProfilesProject(project);
    }

    @Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ProjectSetup.class)
				.name("Setup")
				.category(Categories.create("Fabric8 Profiles"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		builder.add(version);
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);
        if (isProfilesProject(project)) {
            return Results.success("Fabric8 Profiles is already setup!");
        }

        // configure maven project
        configureProject(project);

        // create directory structure
        createResources(project);

        return Results.success("Fabric8 Profiles has been installed.");
	}

    private void configureProject(Project project) {
        // set component properties
        setProjectProperties(project);

        // add plugins
        addPlugins(project);
    }

    private void setProjectProperties(Project project) {
        MavenModelResource modelResource = project.getFacet(MavenFacet.class).getModelResource();
        Model model = modelResource.getCurrentModel();

        if (model.getName() == null) {
            model.setName("Fabric8 Profiles Project");
        }
        if (model.getDescription() == null) {
            model.setDescription("Repository for Fabric8 Profiles and PContainer Configuration");
        }
        model.setPackaging("fabric8-profiles");

        model.addProperty("project.build.sourceEncoding", UTF_8);

        modelResource.setCurrentModel(model);
    }

    private void addPlugins(Project project) {

        // version set?
        String value = version.getValue();
        if (value == null) {
            value = "";
        } else {
            value = ":" + value;
        }

        // fabric8-profiles-maven-plugin
        MavenPluginBuilder profilesPlugin = MavenPluginBuilder.create()
            .setCoordinate(CoordinateBuilder.create("io.fabric8.profiles:fabric8-profiles-maven-plugin" + value));
        profilesPlugin.setExtensions(true);
        mavenPluginInstaller.install(project, profilesPlugin);
    }

    private void createResources(Project project) throws IOException {

        // resources
        YamlConfigResource yamlResource = resourceUtils.getYamlResource(project, "fabric8-profiles.yaml");
        Map<String, Object> contents = new HashMap<>();
        GitConfigDTO gitConfigDTO = new GitConfigDTO();

        gitConfigDTO.setGitRemoteUriPattern(gitRemoteUriPattern.getValue());
        gitConfigDTO.setGogsUsername(gogsUsername.getValue());
        gitConfigDTO.setGogsPassword(gogsPassword.getValue());
        gitConfigDTO.setGogsServiceHost(gogsServiceHost.getValue());
        contents.put("git", gitConfigDTO);

        MavenConfigDTO mavenConfigDTO = new MavenConfigDTO();
        mavenConfigDTO.setDescription(containerDescription.getValue());
        Model model = project.getFacet(MavenFacet.class).getModel();
        mavenConfigDTO.setGroupId(isEmpty(containerGroupId) ? model.getGroupId() : containerGroupId.getValue());
        mavenConfigDTO.setVersion(isEmpty(containerVersion) ? model.getVersion() : containerVersion.getValue());
        contents.put("maven", mavenConfigDTO);

        yamlResource.setConfig(contents);
        yamlResource.createNewFile();

        // create container and profile directories
        FileResource containers = resourceUtils.getFileResource(project, "configs/containers");
        if (!containers.mkdirs()) {
            throw new IOException("Failed to create " + containers);
        }
        profileUtils.createProfile(project.getRoot().reify(DirectoryResource.class), "default", Collections.emptyList());
    }
}