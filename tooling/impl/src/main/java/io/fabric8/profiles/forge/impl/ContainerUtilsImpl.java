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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;

import io.fabric8.profiles.config.ContainerConfigDTO;
import io.fabric8.profiles.forge.ContainerUtils;
import io.fabric8.profiles.forge.ResourceUtils;
import io.fabric8.profiles.forge.resource.PContainerResource;
import io.fabric8.profiles.forge.resource.YamlConfigResource;
import io.fabric8.profiles.forge.resource.impl.PContainerResourceGenerator;
import org.jboss.forge.addon.parser.yaml.resource.YamlResource;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.Resource;

/**
 * Implements {@link ContainerUtils}.
 */
public class ContainerUtilsImpl implements ContainerUtils {

    private static final String YAML_EXT = ".yaml";

    @Inject
    private ResourceUtils resourceUtils;

    @Override
    public PContainerResource getContainer(DirectoryResource rootDir, String name) {
        DirectoryResource containersDir = getContainersDirectory(rootDir);
        Resource<?> containersDirChild = containersDir.getChild(name + YAML_EXT);
        PContainerResource containerResource = containersDirChild.reify(PContainerResource.class);
        return containerResource != null ? containerResource : resourceUtils.convertResource(containersDirChild.reify(YamlResource.class), PContainerResource.class);
    }

    @Override
    public PContainerResource createContainer(DirectoryResource rootDir, ContainerConfigDTO configDTO) throws IOException {

        PContainerResource containerResource = getContainer(rootDir, configDTO.getName());

        YamlConfigResource yamlResource = containerResource.getDelegate();
        HashMap<String, Object> contents = new HashMap<>();
        contents.put("container", configDTO);
        yamlResource.setConfig(contents);
        yamlResource.createNewFile();

        return containerResource;
    }

    @Override
    public List<PContainerResource> getContainers(DirectoryResource rootDir) {
        return getContainers(rootDir, null);
    }

    @Override
    public List<PContainerResource> getContainers(DirectoryResource rootDir, String prefix) {
        List<Resource<?>> resources = getContainersDirectory(rootDir).listResources();

        List<PContainerResource> result = new ArrayList<>(resources.size());
        for (Resource<?> resource : resources) {

            PContainerResource pContainerResource = resource.reify(PContainerResource.class);
            if (pContainerResource == null) {
                pContainerResource = resourceUtils.convertResource(resource.reify(YamlResource.class), PContainerResource.class);
            }

            if (pContainerResource != null) {
                if (prefix == null || pContainerResource.getName().startsWith(prefix)) {
                    result.add(pContainerResource);
                }
            }
        }
        return result;
    }

    private DirectoryResource getContainersDirectory(DirectoryResource rootDir) {
        return rootDir.getChildDirectory(PContainerResourceGenerator.CONFIGS_CONTAINERS);
    }
}
