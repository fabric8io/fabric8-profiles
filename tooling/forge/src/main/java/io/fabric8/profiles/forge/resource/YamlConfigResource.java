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
import java.util.Map;
import java.util.Optional;

import org.jboss.forge.addon.parser.yaml.resource.YamlResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.ResourceException;

/**
 * A {@link FileResource} that represents a Fabric8 Config file.
 */
public interface YamlConfigResource extends FileResource<YamlResource> {

    /**
     * @return an {@link Optional} YAML {@link Map} representation of the underlying {@link File}
     * @throws ResourceException if the {@link File} cannot be read properly
     */
    Optional<Map<String, Object>> getConfig();

    /**
     * @return A list of YAML {@link Map} of the underlying {@link File} when it contains several documents.
     * @throws ResourceException if the {@link File} cannot be read properly
     */
    List<Map<String, Object>> getAllConfig();

    /**
     * Writes the contents from the {@link Map} object to the underlying {@link File}
     *
     * @param data the {@link Map} object. May not be <code>null</code>.
     * @return this instance
     */
    YamlConfigResource setConfig(Map<String, Object> data);

    /**
     * Writes the contents from the {@link List} object to the underlying {@link File}
     *
     * @param data the {@link List} object. May not be <code>null</code>.
     * @return this instance
     */
    YamlConfigResource setConfig(List<Map<String, Object>> data);
}
