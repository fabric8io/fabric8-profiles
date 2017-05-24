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
package io.fabric8.profiles.containers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.Log4JLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Reifies projects using <a href="https://velocity.apache.org/">Apache Velocity Engine</a>.
 */
public abstract class VelocityBasedReifier extends ProjectReifier {

    private static final String USER_DEFINED_POM_VM = "pom.vm";
    private final Properties velocityProperties;

	/**
     * Configures reifier with default configuration and a velocity engine.
     * @param defaultConfig default configuration values.
     */
    public VelocityBasedReifier(JsonNode defaultConfig) {
        super(defaultConfig);

        // initialize velocity to load resources from class loader and use Log4J
        velocityProperties = new Properties();
        velocityProperties.setProperty(RuntimeConstants.RESOURCE_LOADER, "cloader");
        velocityProperties.setProperty("cloader.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityProperties.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, Log4JLogChute.class.getName());
        velocityProperties.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM + ".log4j.logger", log.getName());
    }

	protected VelocityEngine getEngine(Properties properties) {
    	Properties props = new Properties();
    	props.putAll(velocityProperties);
    	if (properties != null) {
            props.putAll(properties);
        }
		VelocityEngine engine = new VelocityEngine(props);
		engine.init();
		return engine;
	}

	protected Set<String> getPrefixedProperty(Properties props, String featurePrefix) {
	    return getPrefixedProperty(props, featurePrefix, null);
	}

	protected Set<String> getPrefixedProperty(Properties props, String prefix, Map<String, String> idMap) {
	
	    final Set<String> values = new HashSet<String>();
	    for (Map.Entry<Object, Object> entry : props.entrySet()) {
	
	        final String key = entry.getKey().toString();
	        if (key.startsWith(prefix)) {
	            final String value = entry.getValue().toString();
	            values.add(value);
	
	            if (idMap != null) {
	                idMap.put(key.substring(prefix.length()), value);
	            }
	        }
	    }
	
	    return values;
	}

    protected String getProjectVersion() {
        // TODO: perhaps use the git hash?
        return "1.0-SNAPSHOT";
    }

    protected Template getTemplate(Path templatesDir, java.util.function.Supplier<String> templateName) throws IOException {
        final Template pomTemplate;
        if (Files.exists(templatesDir.resolve(USER_DEFINED_POM_VM))) {

            // user defined template in profile
            Properties filePathProps = new Properties();
            filePathProps.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
            filePathProps.setProperty("file.resource.loader.class", FileResourceLoader.class.getName());
            filePathProps.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templatesDir.toAbsolutePath().toString());
            pomTemplate = getEngine(filePathProps).getTemplate(USER_DEFINED_POM_VM);

        } else {
            pomTemplate = getEngine(null).getTemplate(templateName.get());
        }
        return pomTemplate;
    }
}
