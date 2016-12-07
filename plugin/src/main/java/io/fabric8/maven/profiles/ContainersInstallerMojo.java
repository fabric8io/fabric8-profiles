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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.config.ConfigHelper;
import io.fabric8.profiles.containers.GitRemoteProcessor;
import io.fabric8.profiles.containers.ProjectProcessor;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * After all containers are generated, push generated container source to Git repos.
 */
@Mojo(name = "install", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    defaultPhase = LifecyclePhase.INSTALL)
//@Execute(lifecycle = "fabric8-profiles", phase = LifecyclePhase.INSTALL)
public class ContainersInstallerMojo extends AbstractProfilesMojo {

    private static final String CURRENT_VERSION_PROPERTY = "currentVersion";
    private static final String CURRENT_COMMIT_ID_PROPERTY = "currentCommitId";

    /**
     * Project processor list, applied in sequence.
     * Default is to use {@link io.fabric8.profiles.containers.GitRemoteProcessor}.
     */
    @Parameter(readonly = false, required = false)
    protected List<Processor> projectProcessors;

    private ObjectId currentCommitId;
    private ProjectProcessor[] processors;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // initialize inherited fields
        super.execute();

        if (!Files.isDirectory(configs)) {
            log.warn("No containers are present in directory " + configs);
            return;
        }

        // get current repository branch version to compare against remotes
        try (final Git sourceRepo = Git.open(sourceDirectory)) {

            String currentVersion = sourceRepo.getRepository().getBranch();

            for (RevCommit revCommit : sourceRepo.log().setMaxCount(1).call()) {
                currentCommitId = revCommit.getId();
            }

            // add current version and commit id to config
            ObjectNode gitNode = profilesConfig.with("git");
            gitNode.put(CURRENT_VERSION_PROPERTY, currentVersion);
            gitNode.put(CURRENT_COMMIT_ID_PROPERTY, currentCommitId.name());

            // build processor list
            if (projectProcessors != null && !projectProcessors.isEmpty()) {

                processors = new ProjectProcessor[projectProcessors.size()];
                int i = 0;
                for (Processor processor : projectProcessors) {

                    final String className = processor.getName();
                    try {
                        ClassLoader classLoader = getProjectClassLoader();
                        final Class<?> aClass = classLoader.loadClass(className);
                        final Class<? extends ProjectProcessor> reifierClass = aClass.asSubclass(ProjectProcessor.class);
                        final Constructor<? extends ProjectProcessor> constructor = reifierClass.getConstructor(Properties.class);

                        JsonNode properties = ConfigHelper.copyObjectNode(profilesConfig);
                        ConfigHelper.putAll((ObjectNode) properties, processor.getProperties());
                        processors[i++] = constructor.newInstance(properties);
                    } catch (ClassCastException e) {
                        throwMojoException("Class is not of type ProjectProcessor", className, e);
                    } catch (ReflectiveOperationException e) {
                        throwMojoException("Error loading ProjectProcessor", className, e);
                    }
                }
            } else {
                processors = new ProjectProcessor[] { new GitRemoteProcessor(profilesConfig) };
            }

            // list all containers, and update under targetDirectory
            final Path target = Paths.get(targetDirectory.getAbsolutePath());
            final List<Path> names = Files.list(configs.resolve("containers"))
                .filter(p -> p.getFileName().toString().endsWith(".cfg"))
                .collect(Collectors.toList());

            // TODO handle container deletes

            // generate all current containers
            for (Path name : names) {
                manageContainer(target, name);
            }

        } catch (IOException e) {
            throwMojoException("Error reading Profiles Git repo", sourceDirectory, e);
        } catch (NoHeadException e) {
            throwMojoException("Error reading Profiles Git repo", sourceDirectory, e);
        } catch (GitAPIException e) {
            throwMojoException("Error reading Profiles Git repo", sourceDirectory, e);
        }
    }

    /**
     * Allow overriding to do something sophisticated, for example,
     * check what changed in git log from last build to only build containers whose profiles changed.
     */
    protected void manageContainer(Path target, Path configFile) throws MojoExecutionException {

        // read container config
        JsonNode config = null;
        try {
            config = ConfigHelper.copyObjectNode(profilesConfig);
            ProfilesHelpers.merge(config, ProfilesHelpers.readYamlFile(configFile));
        } catch (IOException e) {
            throwMojoException("Error reading container configuration", configFile, e);
        }

        final String configFileName = configFile.getFileName().toString();
        final String name = configFileName.substring(0, configFileName.lastIndexOf('.'));

        // make sure container dir exists
        final Path containerDir = target.resolve(name);
        if (!Files.isDirectory(containerDir)) {
            throw new MojoExecutionException("Missing generated container " + containerDir);
        }

        // process reified container
        for (ProjectProcessor processor : processors) {
            try {
                processor.process(name, config, containerDir);
            } catch (IOException e) {
                throwMojoException("Error processing container", name, e);
            }
        }
    }

}
