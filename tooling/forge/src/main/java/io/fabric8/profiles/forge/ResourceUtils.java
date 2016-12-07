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
import java.util.Map;
import java.util.function.Function;

import io.fabric8.profiles.forge.resource.YamlConfigResource;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;

/**
 * Utility for working with {@link org.jboss.forge.addon.resource.Resource} subclasses.
 */
public interface ResourceUtils {

    <T> String listToString(List<T> list);

    void createResourceFile(FileResource<?> fileResource, String templateResource, Map<String, Object> params) throws IOException;

    YamlConfigResource getYamlResource(Project project, String fileName);

    FileResource<?> getFileResource(Project project, String fileName);

    <T extends Resource<?>> List<T> findRecursive(Resource<?> root, Function<Resource<?>, T> resourceFunction);

    public <E, T extends Resource<E>> T convertResource(Resource<E> resource, Class<T> type);
}
