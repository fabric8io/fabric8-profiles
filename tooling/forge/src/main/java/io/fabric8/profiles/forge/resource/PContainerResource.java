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
package io.fabric8.profiles.forge.resource;

import java.io.File;
import java.util.List;

/**
 * A {@link org.jboss.forge.addon.resource.Resource} that represents a Fabric8 Profiled Container.
 */
public interface PContainerResource extends DelegatingResource<YamlConfigResource, File> {

    List<ProfileResource> getProfiles();

    void addProfiles(Iterable<ProfileResource> profileResources);

    void changeProfiles(Iterable<ProfileResource> profileResources);

    void removeProfiles(Iterable<ProfileResource> profileResources);
}
