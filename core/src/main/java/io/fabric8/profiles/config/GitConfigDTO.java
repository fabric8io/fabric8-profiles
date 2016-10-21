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
package io.fabric8.profiles.config;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Git config DTO.
 */
@JsonTypeName("git")
public class GitConfigDTO extends AbstractConfigDTO {

    private String gitRemoteUriPattern;

    private String gitRemoteUri;

    private String gitRemoteName;

    private String gogsUsername;

    private String gogsPassword;

    private String gogsServiceHost;

    private String currentVersion;

    private String currentCommitId;

    public String getGitRemoteUriPattern() {
        return gitRemoteUriPattern;
    }

    public void setGitRemoteUriPattern(String gitRemoteUriPattern) {
        this.gitRemoteUriPattern = gitRemoteUriPattern;
    }

    public String getGitRemoteUri() {
        return gitRemoteUri;
    }

    public void setGitRemoteUri(String gitRemoteUri) {
        this.gitRemoteUri = gitRemoteUri;
    }

    public String getGitRemoteName() {
        return gitRemoteName;
    }

    public void setGitRemoteName(String gitRemoteName) {
        this.gitRemoteName = gitRemoteName;
    }

    public String getGogsUsername() {
        return gogsUsername;
    }

    public void setGogsUsername(String gogsUsername) {
        this.gogsUsername = gogsUsername;
    }

    public String getGogsPassword() {
        return gogsPassword;
    }

    public void setGogsPassword(String gogsPassword) {
        this.gogsPassword = gogsPassword;
    }

    public String getGogsServiceHost() {
        return gogsServiceHost;
    }

    public void setGogsServiceHost(String gogsServiceHost) {
        this.gogsServiceHost = gogsServiceHost;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getCurrentCommitId() {
        return currentCommitId;
    }

    public void setCurrentCommitId(String currentCommitId) {
        this.currentCommitId = currentCommitId;
    }
}
