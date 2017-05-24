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

import java.util.LinkedList;
import java.util.List;

import io.fabric8.profiles.forge.resource.PContainerResource;
import io.fabric8.profiles.forge.resource.config.ListConfigResource;
import org.jboss.forge.addon.parser.yaml.resource.YamlResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.VirtualResource;

/**
 * {@link PContainerResource} implementation.
 */
public class ListConfigResourceImpl extends VirtualResource<List<Resource<?>>> implements ListConfigResource {

    private final List<Resource<?>> member;

    public ListConfigResourceImpl(ResourceFactory factory, Resource<?> parent, List<Resource<?>> member) {
        super(factory, parent);
        this.member = new LinkedList<>(member);
    }

    @Override
    protected List<Resource<?>> doListResources() {
        return (List<Resource<?>>) member;
    }

    @Override
    public boolean delete() throws UnsupportedOperationException {
        getParent().reify(YamlResource.class).getAllModel().clear();
        return true;
    }

    @Override
    public boolean delete(boolean recursive) throws UnsupportedOperationException {
        return delete();
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public List<Resource<?>> getUnderlyingResourceObject() {
        return member;
    }
}
