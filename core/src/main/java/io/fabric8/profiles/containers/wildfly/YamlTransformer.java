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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.jdom.Element;
import org.jdom.Namespace;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;

import io.fabric8.profiles.IllegalArgumentAssertion;
import io.fabric8.profiles.IllegalStateAssertion;

public class YamlTransformer implements Iterable<String> {

	private final Map<String, Namespace> namespaces = new HashMap<>();
	private Map<String, ?> result;
	
	public YamlTransformer(Properties properties) {
		Enumeration<?> names = properties.propertyNames();
		while (names.hasMoreElements()) {
			String key = (String) names.nextElement();
			if (key.toString().startsWith("namespace.")) {
				String value = properties.getProperty(key);
				namespaces.put(key.substring(10), Namespace.getNamespace(value));
			}
		}
	}

	public Namespace getNamespace(String key) {
		return namespaces.get(key);
	}
	
	public YamlTransformer transform(Path path) throws IOException {
		try (FileInputStream input = new FileInputStream(path.toFile())) {
			return transform(input);
		}
	}
	
	@SuppressWarnings("unchecked")
	public YamlTransformer transform(InputStream input) {
		IllegalArgumentAssertion.assertNotNull(input, "input");
		Yaml yaml = new Yaml();
		result = (Map<String, ?>) yaml.load(input);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public YamlTransformer transform(Reader input) {
		IllegalArgumentAssertion.assertNotNull(input, "input");
		Yaml yaml = new Yaml();
		result = (Map<String, ?>) yaml.load(input);
		return this;
	}
	
	public Map<String, ?> getResult() {
		return getResultInternal();
	}

	@Override
	public Iterator<String> iterator() {
		return getResultInternal().keySet().iterator();
	}
	
	public Object getValue(String key) {
		return getValueInternal(key, getResultInternal());
	} 

	@SuppressWarnings("unchecked")
	public Element getElement(String key) {
		key = key == null ? iterator().next() : key;
		Map<String, ?> resmap = (Map<String, ?>) getValue(key);
		return getElement(key, null, resmap);
	}
	
	private Element getElement(String name, Namespace namespace, Map<String, ?> map) {
		IllegalArgumentAssertion.assertNotNull(name, "name");
		IllegalArgumentAssertion.assertNotNull(map, "map");
		namespace = namespace == null ? namespaces.get(name) : namespace;
		IllegalStateAssertion.assertNotNull(namespace, "Cannot obtain namespace for: " + name);
		Element element = new Element(name, namespace);
		for (String key : map.keySet()) {
			addChildElement(element, name + "." + key, map.get(key));
		}
		return element;
	}
	
	@SuppressWarnings("unchecked")
	private void addChildElement(Element element, String key, Object value) {
		IllegalArgumentAssertion.assertNotNull(element, "element");
		IllegalArgumentAssertion.assertNotNull(key, "key");
		IllegalArgumentAssertion.assertNotNull(value, "value");
		Namespace namespace = element.getNamespace();
		if (key.endsWith("-attr")) {
			int index = key.lastIndexOf("-attr");
			String name = key.substring(key.lastIndexOf('.') + 1, index);
			element.setAttribute(name, value.toString());
		} else if (value instanceof Map) {
			Map<String, ?> map = (Map<String, ?>) value;
			String name = key.substring(key.lastIndexOf('.') + 1);
			Element child = new Element(name, namespace);
			for (String subkey : map.keySet()) {
				addChildElement(child, key + "." + subkey, map.get(subkey));
			}
			element.addContent(child);
		} else {
			element.getNamespace();
			String name = key.substring(key.lastIndexOf('.') + 1);
			Element child = new Element(name, namespace);
			child.setText(value.toString());
			element.addContent(child);
		}
	}

	@SuppressWarnings("unchecked")
	private Object getValueInternal(String key, Map<String, ?> map) {
		IllegalArgumentAssertion.assertNotNull(key, "key");
		IllegalArgumentAssertion.assertNotNull(map, "map");
		int dotindex = key.indexOf('.');
		if (dotindex > 0) {
			Object value = map.get(key.substring(0, dotindex));
			if (value instanceof String) {
				return value;
			} else {
				String subkey = key.substring(dotindex + 1);
				return getValueInternal(subkey, (Map<String, ?>) value);
			}
		}
		return map.get(key);
	}

	private Map<String, ?> getResultInternal() {
		IllegalStateAssertion.assertNotNull(result, "No result object available");
		return Collections.unmodifiableMap(result);
	}
}
