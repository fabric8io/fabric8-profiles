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

import java.io.File;
import javax.inject.Inject;

import io.fabric8.profiles.forge.ProfileUtils;
import io.fabric8.profiles.forge.resource.ProfileResource;
import org.jboss.forge.addon.convert.AbstractConverter;
import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.ResourceException;

/**
 * {@link Converter} for {@link io.fabric8.profiles.forge.resource.ProfileResource}.
 */
public class ProfileResourceConverter extends AbstractConverter<String, ProfileResource> implements Converter<String, ProfileResource> {

    private Project project;

    @Inject
    private ProfileUtils profileUtils;

    public ProfileResourceConverter() {
        super(String.class, ProfileResource.class);
    }

    public ProfileResourceConverter setProject(Project project) {
        this.project = project;
        return this;
    }

    @Override
    public ProfileResource convert(String value) {
        // for multi-value inputs, value is showing up as rootDir/profile FileResource
        if (value.contains(File.separator)) {
            value = value.substring(value.lastIndexOf(File.separatorChar));
        }
        ProfileResource profile = profileUtils.getProfile(project.getRoot().reify(DirectoryResource.class), value);
        if (profile == null || !profile.exists()) {
            throw new ResourceException("Missing Profile " + value);
        }
        return profile;
    }
}
