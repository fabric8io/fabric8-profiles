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

import io.fabric8.profiles.forge.resource.PContainerResource;
import io.fabric8.profiles.forge.resource.YamlConfigResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.ResourceGenerator;

/**
 * A {@link org.jboss.forge.addon.resource.ResourceGenerator} for {@link PContainerResource}.
 */
public class PContainerResourceGenerator implements ResourceGenerator<PContainerResource, File> {

    public static final String CONFIGS_CONTAINERS = "configs" + File.separator + "containers";

    @Override
    public boolean handles(Class<?> type, Object resource) {
        if (resource instanceof File) {
            File file = (File) resource;
            if (!file.isDirectory() && file.getParent() != null && file.getParent().endsWith(CONFIGS_CONTAINERS) && file.getName() != null && file.getName().endsWith(".yaml")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T extends Resource<File>> Class<?> getResourceType(ResourceFactory factory, Class<PContainerResource> type, File resource) {
        return PContainerResource.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Resource<File>> T getResource(ResourceFactory factory, Class<PContainerResource> type, File resource) {
        return (T) new PContainerResourceImpl(factory, factory.create(YamlConfigResource.class, resource));
    }
}
