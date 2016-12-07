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
package io.fabric8.maven.profiles;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.config.ConfigHelper;
import io.fabric8.profiles.config.ProfilesConfigDTO;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.fabric8.profiles.ProfilesHelpers;

/**
 * Base class for Profiles mojos.
 */
public abstract class AbstractProfilesMojo extends AbstractMojo {

    protected static final String FABRIC8_PROFILES_YAML = "fabric8-profiles.yaml";
    protected final Log log = getLog();

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Profile and container repository directory, defaults to ${project.basedir}.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = false, required = true)
    protected File sourceDirectory;

    /**
     * Build directory, defaults to ${project.build.outputDir}.
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = false, required = true)
    protected File targetDirectory;

    /**
     * Build properties, overrides fabric8-profiles.yaml under {@literal sourceDirectory}.
     */
    @Parameter(readonly = false, required = false)
    protected Map<String, String> profilesProperties;

    protected ObjectNode profilesConfig;
    protected Path configs;
    protected Path profiles;
    protected String lastCommitId;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // validate paths
        if (!Files.isDirectory(Paths.get(sourceDirectory.toURI()))) {
            throw new MojoExecutionException("Missing source directory " + sourceDirectory);
        }
        Path targetPath = Paths.get(targetDirectory.toURI());
        if (!Files.isDirectory(targetPath)) {
            try {
                Files.createDirectories(targetPath);
            } catch (IOException e) {
                throwMojoException("Error creating output directory", targetDirectory, e);
            }
        }

        // read generator properties
        final Path sourcePath = Paths.get(sourceDirectory.getAbsolutePath());
        try {
            final JsonNode yamlFile = ProfilesHelpers.readYamlFile(sourcePath.resolve(FABRIC8_PROFILES_YAML));
            if (profilesProperties == null) {
                profilesConfig = (ObjectNode) yamlFile;
            } else {
                profilesConfig = (ObjectNode) ProfilesHelpers.merge(yamlFile, ConfigHelper.toJson(profilesProperties));
            }

            // last build id
            ProfilesConfigDTO profilesConfigDTO = ConfigHelper.toValue(profilesConfig, ProfilesConfigDTO.class);
            lastCommitId = profilesConfigDTO.getLastBuildCommitId();

        } catch (IOException e) {
            throw new MojoExecutionException("Error reading " + FABRIC8_PROFILES_YAML + ": " + e.getMessage(), e);
        }

        // repository paths
        final Path repository = Paths.get(sourceDirectory.getAbsolutePath());

        configs = repository.resolve("configs");
        if (!Files.isDirectory(configs)) {
            throw new MojoExecutionException("Missing container directory " + configs);
        }
        this.profiles = repository.resolve("profiles");
        if (!Files.isDirectory(this.profiles)) {
            throw new MojoExecutionException("Missing profiles directory " + configs);
        }
    }

    protected void throwMojoException(String message, Object target, Exception e) throws MojoExecutionException {
        throw new MojoExecutionException(String.format("%s %s : %s", message, target, e.getMessage()), e);
    }

    protected ClassLoader getProjectClassLoader() throws MojoExecutionException {
        List<String> classpathElements = Collections.emptyList();
        if (project != null) {
            try {
                List<String> elements = project.getRuntimeClasspathElements();
                if (elements != null) {
                	classpathElements = elements;
                }
            } catch (DependencyResolutionRequiredException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        URL[] urls = new URL[classpathElements.size()];
        int i = 0;
        for (Iterator<String> it = classpathElements.iterator(); it.hasNext(); i++) {
            try {
                urls[i] = new File((String) it.next()).toURI().toURL();
                log.debug("Adding project path " + urls[i]);
            } catch (MalformedURLException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return new URLClassLoader(urls, tccl != null ? tccl : getClass().getClassLoader());
    }
}
