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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

import io.fabric8.profiles.forge.ResourceUtils;
import io.fabric8.profiles.forge.resource.YamlConfigResource;
import org.jboss.forge.addon.parser.yaml.resource.YamlResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.URLResource;
import org.jboss.forge.addon.templates.Template;
import org.jboss.forge.addon.templates.TemplateFactory;
import org.jboss.forge.addon.templates.freemarker.FreemarkerTemplate;

public class ResourceUtilsImpl implements ResourceUtils {

    @Inject
    ResourceFactory resourceFactory;

    @Inject
    TemplateFactory templateFactory;

    @Override
    public <T> String listToString(List<T> list) {
        String result = list.toString();
        return result.substring(1, result.length() - 1);
    }

    @Override
    public void createResourceFile(FileResource<?> fileResource, String templateResource, Map<String, Object> params) throws IOException {
        Resource<URL> urlResource = resourceFactory.create(this.getClass().getResource(templateResource)).reify(URLResource.class);
        Template template = templateFactory.create(urlResource, FreemarkerTemplate.class);

        fileResource.setContents(template.process(params));
        fileResource.createNewFile();
    }

    @Override
    public YamlConfigResource getYamlResource(Project project, String fileName) {
        DirectoryResource rootDir = project.getRoot().reify(DirectoryResource.class);
        return convertResource(rootDir.getChild(fileName).reify(YamlResource.class), YamlConfigResource.class);
    }

    @Override
    public FileResource<?> getFileResource(Project project, String fileName) {
        DirectoryResource rootDir = project.getRoot().reify(DirectoryResource.class);
        return rootDir.getChild(fileName).reify(FileResource.class);
    }

    @Override
    public <T extends Resource<?>> List<T> findRecursive(Resource<?> root, Function<Resource<?>, T> resourceFunction) {
        List<T> result = new ArrayList<>();
        findRecursive(root, resourceFunction, result);
        return result;
    }

    @Override
    public <E, T extends Resource<E>> T convertResource(Resource<E> resource, Class<T> type) {
        return resource.getResourceFactory().create(type, resource.getUnderlyingResourceObject());
    }

    private <T extends Resource<?>> void findRecursive(Resource<?> root, final Function<Resource<?>, T> resourceFunction, List<T> result) {
        result.addAll(root.listResources().stream().map(res -> {
            findRecursive(res, resourceFunction, result);
            return resourceFunction.apply(res);
        }).filter(res -> res != null).collect(Collectors.toList()));
    }
}