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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.forge.ProfileUtils;
import io.fabric8.profiles.forge.ResourceUtils;
import io.fabric8.profiles.forge.resource.ProfileResource;

import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceException;
import org.jboss.forge.addon.resource.ResourceFactory;

import static io.fabric8.profiles.forge.impl.ProfileUtilsImpl.PROFILES_DIR;

/**
 * {@link ProfileResource} implementation.
 */
public class ProfileResourceImpl extends AbstractDelegatingResourceImpl<DirectoryResource, File> implements ProfileResource {

    private static final String PROFILES_DIR_PATH = File.separator + PROFILES_DIR + File.separator;

    private ProfileUtils profileUtils;

    private ResourceUtils resourceUtils;

    public ProfileResourceImpl(ResourceFactory factory, DirectoryResource delegate, ProfileUtils profileUtils, ResourceUtils resourceUtils) {
        super(factory, delegate);
        this.profileUtils = profileUtils;
        this.resourceUtils = resourceUtils;
    }

    @Override
    public String getName() {
        return getProfileName();
    }

    @Override
    public Resource<File> createFrom(File file) {
        ResourceFactory resourceFactory = getResourceFactory();
        return new ProfileResourceImpl(resourceFactory, resourceFactory.create(DirectoryResource.class, file), profileUtils, resourceUtils);
    }

    public String getProfileName() {
        String absolutePath = getUnderlyingResourceObject().getAbsolutePath();
        return absolutePath.substring(absolutePath.indexOf(PROFILES_DIR_PATH) + PROFILES_DIR_PATH.length()).replace(File.separatorChar, '-').replaceAll("\\.profile", "");
    }

    @Override
    public Iterable<ProfileResource> getParentProfiles() {
        Properties properties = getAgentProperties();
        String[] parentNames = properties.getProperty(Profiles.ATTRIBUTE_PARENTS).split(" ");

        List<ProfileResource> result = new LinkedList<>();
        DirectoryResource rootDir = getProfilesDir().getParent();
        for (String name : parentNames) {
            ProfileResource profile = profileUtils.getProfile(rootDir, name);
            if (profile == null || !profile.exists()) {
                throw new ResourceException("Missing profile " + name);
            }
            result.add(profile);
        }
        return result;
    }

    private DirectoryResource getProfilesDir() {
        DirectoryResource parent = getDelegate();
        while (parent != null && !"profiles".equals(parent.getName())) {
            parent = parent.getParent();
        }
        return parent;
    }

    @Override
    public void setParentProfiles(Iterable<ProfileResource> parents) {
        // create agent properties file and set parents
        FileResource propFile = getAgentPropertiesFile();
        Properties properties = new Properties();
        // read existing properties
        try (InputStream is = propFile.getResourceInputStream()) {
            properties.load(is);
        } catch (IOException e) {
            throw new ResourceException(e.getMessage(), e);
        }
        if (parents != null && parents.iterator().hasNext()) {
            properties.setProperty(Profiles.ATTRIBUTE_PARENTS, profileUtils.getProfileNameList(parents));
        }
        StringWriter writer = new StringWriter();
        try {
            properties.store(writer, "");
        } catch (IOException e) {
            throw new ResourceException(e.getMessage(), e);
        }
        propFile.setContents(writer.toString());
    }

    @Override
    public void rename(String name) {
        File file = getUnderlyingResourceObject().getAbsoluteFile();
        if (!file.renameTo(new File(file.getParent(), name))) {
            throw new ResourceException(
                String.format("Failed to rename profile %s to %s", file.getName(), name));
        }
    }

    @Override
    public void copyTo(String name) {
        File file = getUnderlyingResourceObject().getAbsoluteFile();
        try {
            ProfilesHelpers.copyDirectory(file.toPath(), new File(file.getParent(), name).toPath());
        } catch (IOException e) {
            throw new ResourceException(e.getMessage(), e);
        }
    }

    private FileResource getAgentPropertiesFile() {
        FileResource resource = getChild(Profiles.FABRIC8_AGENT_PROPERTIES).reify(FileResource.class);
        if (!resource.exists()) {
            resource.createNewFile();
        }
        return resource;
    }

    private Properties getAgentProperties() {
        Properties properties = new Properties();
        try {
            try (InputStream is = getAgentPropertiesFile().getResourceInputStream()) {
                properties.load(is);
            }
        } catch (IOException e) {
            throw new ResourceException(e.getMessage(), e);
        }
        return properties;
    }
}
