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
package io.fabric8.profiles.containers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import io.fabric8.devops.ProjectConfig;
import io.fabric8.devops.connector.DevOpsConnector;
import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.config.ConfigHelper;
import io.fabric8.profiles.config.GitConfigDTO;
import io.fabric8.repo.git.CreateRepositoryDTO;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.RepositoryDTO;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pushes a reified container to a remote git repo.
 */
public class GitRemoteProcessor extends ProjectProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(GitRemoteProcessor.class);

    /**
     * Initialize a processor with default configuration.
     *
     * @param config default configuration properties.
     */
    public GitRemoteProcessor(JsonNode config) {
        super(config);
    }

    @Override
    public void processDelete(String name, Properties lastConfig) throws IOException {
        // TODO
    }

    @Override
    public void process(String name, JsonNode config, Path containerDir) throws IOException {

        // skip during testing, etc.
        if (System.getProperty("skipGitProcessor") != null) {
            return;
        }

        // get git config
        GitConfigDTO configDTO = ConfigHelper.toValue(ProfilesHelpers.merge(config, defaultConfig), "/git", GitConfigDTO.class);

        // get or create remote repo URL
        String remoteUri = configDTO.getGitRemoteUri();
        if (remoteUri == null || remoteUri.isEmpty()) {
            remoteUri = getRemoteUri(configDTO, name);
        }

        // try to clone remote repo in temp dir
        String remote = configDTO.getGitRemoteName();
        if (remote == null) {
            remote = Constants.DEFAULT_REMOTE_NAME;
        }
        Path tempDirectory = null;
        try {
            tempDirectory = Files.createTempDirectory(containerDir, "cloned-remote-");
        } catch (IOException e) {
            throwException("Error creating temp directory while cloning ", remoteUri, e);
        }

        final String userName = configDTO.getGogsUsername();
        final String password = configDTO.getGogsPassword();
        final UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(
            userName, password);

        Git clonedRepo = null;
        try {
            String currentVersion = configDTO.getCurrentVersion();
            try {
                clonedRepo = Git.cloneRepository()
                    .setDirectory(tempDirectory.toFile())
                    .setBranch(currentVersion)
                    .setRemote(remote)
                    .setURI(remoteUri)
                    .setCredentialsProvider(credentialsProvider)
                    .call();
            } catch (InvalidRemoteException e) {
                if (e.getCause() instanceof NoRemoteRepositoryException) {
                    final String address;
                    if (configDTO.getGogsServiceHost() == null) {
                        address = "http://gogs.vagrant.f8";
                    } else {
                        address = "http://" + configDTO.getGogsServiceHost();
                    }

                    GitRepoClient client = new GitRepoClient(address, userName, password);

                    CreateRepositoryDTO request = new CreateRepositoryDTO();
                    request.setName(name);
                    request.setDescription("Fabric8 Profiles generated project for container " + name);
                    RepositoryDTO repository = client.createRepository(request);

                    // create new repo with Gogs clone URL
                    clonedRepo = Git.init()
                        .setDirectory(tempDirectory.toFile())
                        .call();
                    final RemoteAddCommand remoteAddCommand = clonedRepo.remoteAdd();
                    remoteAddCommand.setName(remote);
                    try {
                        remoteAddCommand.setUri(new URIish(repository.getCloneUrl()));
                    } catch (URISyntaxException e1) {
                        throwException("Error creating remote repo ", repository.getCloneUrl(), e1);
                    }
                    remoteAddCommand.call();

                    // add currentVersion branch
                    clonedRepo.add().addFilepattern(".").call();
                    clonedRepo.commit().setMessage("Adding version " + currentVersion).call();
                    try {
                        clonedRepo.branchRename()
                            .setNewName(currentVersion)
                            .call();
                    } catch (RefAlreadyExistsException ignore) {
                        // ignore
                    }

                    // Call the fabric8 devops connector to create the Git repository
                    // and the Jenkins jobs along with other discoverable devops services
                    DevOpsConnector connector = new DevOpsConnector();
                    ProjectConfig projectConfig = new ProjectConfig();
                    projectConfig.setUseLocalFlow(true);
                    projectConfig.setBuildName(name);
                    connector.setProjectConfig(projectConfig);
                    connector.setGitRepoClient(client);
                    connector.setBasedir(tempDirectory.toFile());
                    connector.setUsername(userName);
                    connector.setPassword(password);
                    connector.setRepoName(name);
                    connector.setProjectName(name);
                    connector.setRegisterWebHooks(true);
                    try {
                        connector.execute();
                    } catch (Exception cause) {
                        throwException("Error creating Devops project", remoteUri, cause);
                    }
                } else {
                    throwException("Error cloning ", remoteUri, e);
                }
            }

            // handle missing remote branch
            if (!clonedRepo.getRepository().getBranch().equals(currentVersion)) {
                clonedRepo.branchCreate()
                    .setName(currentVersion)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .call();
            }

            // move .git dir to parent and drop old source altogether
            // TODO things like .gitignore, etc. need to be handled, perhaps through Profiles??
            Files.move(tempDirectory.resolve(".git"), containerDir.resolve(".git"));

        } catch (GitAPIException e) {
            throwException("Error cloning ", remoteUri, e);
        } catch (IOException e) {
            throwException("Error copying files from ", remoteUri, e);
        } finally {
            // close clonedRepo
            if (clonedRepo != null) {
                try {
                    clonedRepo.close();
                } catch (Exception ignored) {
                }
            }
            // cleanup tempDirectory
            try {
                ProfilesHelpers.deleteDirectory(tempDirectory);
            } catch (IOException e) {
                // ignore
            }
        }

        try (Git containerRepo = Git.open(containerDir.toFile())) {

            // diff with remote
            List<DiffEntry> diffEntries = containerRepo.diff().call();
            if (!diffEntries.isEmpty()) {

                // add all changes
                containerRepo.add().addFilepattern(".").call();

                // with latest Profile repo commit ID in message
                // TODO provide other identity properties
                containerRepo.commit().setMessage("Container updated for commit " + configDTO.getCurrentCommitId()).call();

                // push to remote
                containerRepo.push().setRemote(remote).setCredentialsProvider(credentialsProvider).call();
            } else {
                LOG.debug("No changes to container" + name);
            }

        } catch (GitAPIException e) {
            throwException("Error processing container Git repo ", containerDir, e);
        } catch (IOException e) {
            throwException("Error reading container Git repo ", containerDir, e);
        }
    }

    private void throwException(String message, Object target, Throwable t) throws IOException {
        throw new IOException(String.format("%s: %s: %s", message, target, t.getMessage()), t);
    }

    private String getRemoteUri(GitConfigDTO config, String name) throws IOException {
        String gitRemotePattern = config.getGitRemoteUriPattern();
        if (gitRemotePattern == null) {
            throw new IOException("Missing property gitRemoteUriPattern");
        }
        return gitRemotePattern.replace("${name}", name);
    }
}
