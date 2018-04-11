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
package io.fabric8.profiles.forge;

import java.io.IOException;
import java.util.List;

import io.fabric8.profiles.forge.resource.ProfileResource;
import org.jboss.forge.addon.resource.DirectoryResource;

/**
 * Service for managing Fabric8 Profiles.
 */
public interface ProfileUtils {

    ProfileResource getProfile(DirectoryResource rootDir, String name);

    List<ProfileResource> getProfiles(DirectoryResource rootDir);

    List<ProfileResource> getProfiles(DirectoryResource rootDir, String value);

    ProfileResource createProfile(DirectoryResource rootDir, String name, Iterable<ProfileResource> parents) throws IOException;

    String getProfileNameList(Iterable<ProfileResource> parents);
}
