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

import java.beans.IntrospectionException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.profiles.config.AbstractConfigDTO;
import io.fabric8.profiles.config.ContainerConfigDTO;
import io.fabric8.profiles.config.GitConfigDTO;
import io.fabric8.profiles.config.MavenConfigDTO;
import io.fabric8.profiles.config.ProfilesConfigDTO;
import io.fabric8.profiles.config.ProjectPropertiesDTO;
import io.fabric8.profiles.forge.resource.PContainerResource;
import io.fabric8.profiles.forge.resource.YamlConfigResource;
import io.fabric8.profiles.forge.resource.impl.config.ContainerConfigResourceImpl;
import io.fabric8.profiles.forge.resource.impl.config.GitConfigResourceImpl;
import io.fabric8.profiles.forge.resource.impl.config.ListConfigResourceImpl;
import io.fabric8.profiles.forge.resource.impl.config.MavenConfigResourceImpl;
import io.fabric8.profiles.forge.resource.impl.config.ProfilesConfigResourceImpl;
import io.fabric8.profiles.forge.resource.impl.config.ProjectPropertiesResourceImpl;
import org.jboss.forge.addon.parser.yaml.resource.AbstractYamlResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * {@link PContainerResource} implementation.
 */
public class YamlConfigResourceImpl extends AbstractYamlResource implements YamlConfigResource {

    public YamlConfigResourceImpl(ResourceFactory factory, File resource) {
        super(factory, resource);
    }

    @Override
    public Resource<File> createFrom(File file) {
        return new YamlConfigResourceImpl(getResourceFactory(), file);
    }

    @Override
    protected List<Resource<?>> doListResources() {
        List<Resource<?>> list = new LinkedList<>();

        // find all AbstractConfigResources
        Yaml yaml = getYaml();
        if (getModel().isPresent()) {
            for (Map.Entry<String, Object> entry : getModel().get().entrySet()) {
                addResource(list, yaml, entry);
            }
        } else if (!getAllModel().isEmpty()) {
            for (Map<String, Object> map : getAllModel()) {
                List<Resource<?>> resources = new LinkedList<>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    addResource(resources, yaml, entry);
                }
                list.add(new ListConfigResourceImpl(getResourceFactory(), getParent(), resources));
            }
        }
        return list;
    }

    private Yaml getYaml() {
        Representer representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                // skip null values
                if (propertyValue == null) {
                    return null;
                }

                NodeTuple defaultNode = super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                if (javaBean.getClass().equals(ContainerConfigDTO.class) && "containerType".equals(property.getName())) {
                    return new NodeTuple(representData("container-type"), defaultNode.getValueNode());
                }

                return defaultNode;
            }
        };
        representer.setPropertyUtils(new PropertyUtils() {
            @Override
            public Property getProperty(Class<? extends Object> type, String name) throws IntrospectionException {
                if ("container-type".equals(name)) {
//                    name = toCamelCase(name);
                    name = "containerType";
                }
                return super.getProperty(type, name);
            }

/*
            private String toCamelCase(String name) {
                char[] chars = name.toCharArray();
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < chars.length; i++) {
                    if (chars[i] == '-') {
                        builder.append(Character.toUpperCase(chars[++i]));
                    } else {
                        builder.append(chars[i]);
                    }
                }
                return builder.toString();
            }
*/
        });
        for (Class aClass : new Class[] {ContainerConfigDTO.class, GitConfigDTO.class, MavenConfigDTO.class, ProfilesConfigDTO.class, ProjectPropertiesDTO.class}) {
            representer.addClassTag(aClass, Tag.MAP);
        }
        return new Yaml(representer);
    }

    private void addResource(List<Resource<?>> list, Yaml yaml, Map.Entry<String, Object> entry) {
        switch (entry.getKey()) {
        case "container":
            list.add(new ContainerConfigResourceImpl(getResourceFactory(), this, getMember(yaml, entry, ContainerConfigDTO.class)));
            break;
        case "git":
            list.add(new GitConfigResourceImpl(getResourceFactory(), this, getMember(yaml, entry, GitConfigDTO.class)));
            break;
        case "maven":
            list.add(new MavenConfigResourceImpl(getResourceFactory(), this, getMember(yaml, entry, MavenConfigDTO.class)));
            break;
        case "profiles":
            list.add(new ProfilesConfigResourceImpl(getResourceFactory(), this, getMember(yaml, entry, ProfilesConfigDTO.class)));
            break;
        case "projectProperties":
            list.add(new ProjectPropertiesResourceImpl(getResourceFactory(), this, getMember(yaml, entry, ProjectPropertiesDTO.class)));
            break;
        default:
            // ignore
        }
    }

    private <T extends AbstractConfigDTO> T getMember(Yaml yaml, Map.Entry<String, Object> entry, Class<T> type) {
        return yaml.loadAs(yaml.dumpAsMap(entry.getValue()), type);
    }

    @Override
    public Optional<Map<String, Object>> getConfig() {
        return super.getModel();
    }

    @Override
    public List<Map<String, Object>> getAllConfig() {
        return super.getAllModel();
    }

    @Override
    public YamlConfigResource setConfig(Map<String, Object> data) {
        Yaml yaml = getYaml();
        String dump = yaml.dumpAsMap(data);
        setContents(dump);
        return this;
    }

    @Override
    public YamlConfigResource setConfig(List<Map<String, Object>> data) {

        Yaml yaml = getYaml();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true);
        Iterator<Map<String, Object>> it = data.iterator();
        while (it.hasNext())
        {
           Map<String, Object> model = it.next();
           ps.print(yaml.dumpAsMap(model));
           if (it.hasNext())
              ps.println("---");
        }
        setContents(baos.toString());
        return this;
    }
}
