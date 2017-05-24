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
package io.fabric8.profiles.forge.resource.impl;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

import io.fabric8.profiles.config.ContainerConfigDTO;
import io.fabric8.profiles.forge.ProfileUtils;
import io.fabric8.profiles.forge.resource.PContainerResource;
import io.fabric8.profiles.forge.resource.ProfileResource;
import io.fabric8.profiles.forge.resource.YamlConfigResource;
import io.fabric8.profiles.forge.resource.config.ContainerConfigResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;

/**
 * {@link PContainerResource} implementation.
 */
public class PContainerResourceImpl extends AbstractDelegatingResourceImpl<YamlConfigResource, File> implements PContainerResource {

    @Inject
    ProfileUtils profileUtils;

    protected PContainerResourceImpl(ResourceFactory factory, YamlConfigResource delegate) {
        super(factory, delegate);
    }

    @Override
    public String getName() {
        return getUnderlyingResourceObject().getName().replaceAll("\\.yaml", "");
    }

    @Override
    public List<ProfileResource> getProfiles() {
        String[] profiles = getContainerConfigResource().getUnderlyingResourceObject().getProfiles().split(" ");
        return Arrays.asList(profiles).stream().map(new Function<String, ProfileResource>() {
            @Override
            public ProfileResource apply(String name) {
                return profileUtils.getProfile(getDelegate().getParent().getParent(), name);
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void addProfiles(Iterable<ProfileResource> profileResources) {
        ContainerConfigResource configResource = getContainerConfigResource();
        ContainerConfigDTO configDTO = configResource.getUnderlyingResourceObject();
        StringBuilder profileNames = new StringBuilder(configDTO.getProfiles());
        for (ProfileResource profileResource : profileResources) {
            profileNames.append(' ').append(profileResource.getProfileName());
        }
        configDTO.setProfiles(profileNames.toString());
        configResource.setContents(configDTO);
    }

    @Override
    public void changeProfiles(Iterable<ProfileResource> profileResources) {
        ContainerConfigResource configResource = getContainerConfigResource();
        ContainerConfigDTO configDTO = configResource.getUnderlyingResourceObject();
        StringBuilder profileNames = new StringBuilder();
        for (ProfileResource profileResource : profileResources) {
            profileNames.append(' ').append(profileResource.getProfileName());
        }
        configDTO.setProfiles(profileNames.toString().trim());
        configResource.setContents(configDTO);
    }

    @Override
    public void removeProfiles(Iterable<ProfileResource> profileResources) {
        ContainerConfigResource configResource = getContainerConfigResource();
        ContainerConfigDTO configDTO = configResource.getUnderlyingResourceObject();
        Set<String> removeNames = new HashSet<>();
        for (ProfileResource profileResource : profileResources) {
            removeNames.add(profileResource.getProfileName());
        }
        StringBuilder profileNames = new StringBuilder();
        for (String name : configDTO.getProfiles().split(" ")) {
            if (!removeNames.contains(name)) {
                profileNames.append(' ').append(name);
            }
        }
        configDTO.setProfiles(profileNames.toString().trim());
        configResource.setContents(configDTO);
    }

    private ContainerConfigResource getContainerConfigResource() {
        Optional<Resource<?>> optional = listResources().stream().filter(res -> res.reify(ContainerConfigResource.class) != null).findFirst();
        if (!optional.isPresent()) {
            throw new IllegalArgumentException(String.format("Configuration file %s is missing container config", getFullyQualifiedName()));
        }
        return optional.get().reify(ContainerConfigResource.class);
    }

    @Override
    public Resource<File> createFrom(File file) {
        ResourceFactory resourceFactory = getResourceFactory();
        return new PContainerResourceImpl(resourceFactory, resourceFactory.create(YamlConfigResource.class, file));
    }
}
