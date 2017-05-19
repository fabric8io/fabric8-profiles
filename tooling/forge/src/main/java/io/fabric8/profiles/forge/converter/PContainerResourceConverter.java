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
package io.fabric8.profiles.forge.converter;

import javax.inject.Inject;

import io.fabric8.profiles.forge.ContainerUtils;
import io.fabric8.profiles.forge.resource.PContainerResource;
import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;

/**
 * {@link Converter} for {@link PContainerResource}.
 */
public class PContainerResourceConverter implements Converter<String, PContainerResource> {

    @Inject
    private ContainerUtils containerUtils;

    private Project project;

    public PContainerResourceConverter setProject(Project project) {
        this.project = project;
        return this;
    }

    @Override
    public PContainerResource convert(String value) {
        return containerUtils.getContainer(project.getRoot().reify(DirectoryResource.class), value);
    }
}
