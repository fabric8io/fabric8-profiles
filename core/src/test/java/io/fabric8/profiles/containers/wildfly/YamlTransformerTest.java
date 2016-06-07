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
package io.fabric8.profiles.containers.wildfly;

import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;


public class YamlTransformerTest {

    static final Path REPOSITORY_BASE_DIR = PROJECT_BASE_DIR.resolve("src/test/resources/repos/wildflyA/profiles");
    
    @Test
    public void testReify() throws Exception {

        Path filePath = REPOSITORY_BASE_DIR.resolve("datasource/simple.profile/project-stages.yml");
        
        Properties props = new Properties();
        props.put("namespace.datasources", "urn:jboss:domain:datasources:4.0");
        YamlTransformer transformer = new YamlTransformer(props).transform(filePath);
        
		Assert.assertEquals("ProfileDS", transformer.getValue("datasources.datasource.pool-name-attr"));
		Assert.assertEquals("sa", transformer.getValue("datasources.datasource.security.user-name"));
		Assert.assertTrue(transformer.getValue("datasources.datasource.security") instanceof Map);
		
		Element el = transformer.getElement("datasources");
		StringWriter sw = new StringWriter();
		new XMLOutputter(Format.getPrettyFormat()).output(el, sw);
		// System.out.println(sw);
		Assert.assertTrue(sw.toString().contains("pool-name=\"ProfileDS\""));
		Assert.assertTrue(sw.toString().contains("<user-name>sa</user-name>"));
    }
}
