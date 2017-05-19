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
package io.fabric8.profiles.forge.resource.impl.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.profiles.config.AbstractConfigDTO;
import io.fabric8.profiles.forge.resource.YamlConfigResource;
import io.fabric8.profiles.forge.resource.config.ConfigResource;
import org.jboss.forge.addon.parser.yaml.resource.YamlResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.VirtualResource;

/**
 * Base class for all Config DTO resources.
 */
public abstract class AbstractConfigResourceImpl<T extends AbstractConfigDTO> extends VirtualResource<T> implements ConfigResource<T> {

    private final T member;

    protected AbstractConfigResourceImpl(ResourceFactory factory, Resource<?> parent, T member) {
        super(factory, parent);
        this.member = member;
    }

    @Override
    public Resource<T> createFrom(T resource) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String toString() {
        return member.toString();
    }

    @Override
    protected List<Resource<?>> doListResources() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getName() {
        return member.getKey();
    }

    @Override
    public T getUnderlyingResourceObject() {
        return member;
    }

    @Override
    public boolean delete() throws UnsupportedOperationException {
        boolean result = false;
        YamlResource yamlResource = getParent().reify(YamlResource.class);
        if (yamlResource.getModel().isPresent()) {
            Map<String, Object> model = yamlResource.getModel().get();
            model.remove(getName());
            yamlResource.setContents(model);
            result = true;
        }
        return result;
    }

    @Override
    public boolean delete(boolean recursive) throws UnsupportedOperationException {
        return delete();
    }


    @Override
    public boolean setContents(T content) {
        boolean result = false;
        YamlConfigResource yamlResource = getParent().reify(YamlConfigResource.class);
        Optional<Map<String, Object>> config = yamlResource.getConfig();
        if (config.isPresent()) {
            Map<String, Object> model = config.get();
            model.put(getName(), content);
            yamlResource.setConfig(model);
            result = true;
        }
        return result;
    }

}
