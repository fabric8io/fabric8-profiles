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
package io.fabric8.profiles.forge.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import io.fabric8.profiles.forge.ProfileUtils;
import io.fabric8.profiles.forge.ResourceUtils;
import io.fabric8.profiles.forge.resource.ProfileResource;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;

/**
 * Utility class for working with {@link ProfileResource}.
 */
public class ProfileUtilsImpl implements ProfileUtils {

    public static final String PROFILES_DIR = "profiles";
    private static final String PROFILE_SUFFIX = ".profile";

    @Inject
    private ResourceUtils resourceUtils;

    @Override
    public ProfileResource getProfile(DirectoryResource rootDir, String name) {
        return getProfileResource(getProfileDirectory(rootDir, name));
    }

    @Override
    public List<ProfileResource> getProfiles(DirectoryResource rootDir, String value) {
        DirectoryResource profilesDir = getProfilesDirectory(rootDir);
        List<ProfileResource> resources = resourceUtils.findRecursive(profilesDir, res -> res.getResourceFactory().create(ProfileResource.class, (File) res.getUnderlyingResourceObject()));
        List<ProfileResource> result = new ArrayList<>(resources.size());
        for (Resource<?> resource : resources) {
            ProfileResource profileResource = (ProfileResource) resource;
            if (value == null || profileResource.getProfileName().startsWith(value)) {
                result.add(profileResource);
            }
        }
        return result;
    }

    private DirectoryResource getProfilesDirectory(DirectoryResource rootDir) {
        return rootDir.getOrCreateChildDirectory(PROFILES_DIR);
    }

    @Override
    public ProfileResource createProfile(DirectoryResource rootDir, String name, Iterable<ProfileResource> parents) throws IOException {
        // create profile directory
        FileResource<?> profileDirectory = getProfileDirectory(rootDir, name);
        profileDirectory.mkdirs();

        // create ProfileResource and set parents
        ProfileResource profileResource = getProfileResource(profileDirectory);
        profileResource.getDelegate().mkdirs();
        profileResource.setParentProfiles(parents);
        return profileResource;
    }

    private ProfileResource getProfileResource(FileResource<?> profileDirectory) {
        return resourceUtils.convertResource(profileDirectory, ProfileResource.class);
    }

    private FileResource<?> getProfileDirectory(DirectoryResource rootDir, String name) {
        return resourceUtils.convertResource(getProfilesDirectory(rootDir).getChild(getProfileDirname(name)), FileResource.class);
    }

    private String getProfileDirname(String name) {
        return name.replaceAll("\\-", File.separator) + PROFILE_SUFFIX;
    }

    @Override
    public String getProfileNameList(Iterable<ProfileResource> parents) {
        StringBuilder builder = new StringBuilder();
        for (ProfileResource parent : parents) {
            builder.append(parent.getProfileName()).append(' ');
        }
        return builder.toString().trim();
    }
}
