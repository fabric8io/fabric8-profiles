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
package io.fabric8.profiles.config;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric8.profiles.ProfilesHelpers;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static io.fabric8.profiles.ProfilesHelpers.YAML_MAPPER;

/**
 * Utility class for working with YAML config files. It uses format similar to Spring externalized configuration.
 * @see <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html">Spring Externalized Configuration</a>
 */
public abstract class ConfigHelper {

    private static final Pattern ARRAY_PATTERN = Pattern.compile("([^\\[]+)\\[(\\d+)\\]");
    private static final JsonNodeFactory JSON_NODE_FACTORY = JsonNodeFactory.instance;

    /**
     * Parse properties and convert to JSON.
     * @param properties properties to convert.
     * @return JsonNode hierarchical representation of properties.
     */
    public static JsonNode toJson(Map<String, String> properties) {
        // all properties start as members of a root object
        JsonNode root = JSON_NODE_FACTORY.objectNode();

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();

            // find parent path
            final int parentEnd = key.lastIndexOf('.');
            final String parentPath;
            final String child;
            if (parentEnd == -1) {
                parentPath = "";
                child = key;
            } else {
                parentPath = key.substring(0, parentEnd).replace('.', '/');
                child = key.substring(parentEnd, key.length());
            }

            // find parent node
            JsonNode parentNode = root;
            parentNode = root;
            for (String field : parentPath.split("\\.")) {
                JsonNode node = parentNode.get(field);
                if (node == null) {
                    node = JSON_NODE_FACTORY.objectNode();
                    if (!(parentNode instanceof ObjectNode)) {
                        throw new IllegalArgumentException("Missing Object node at " + field);
                    }
                    ((ObjectNode) parentNode).set(field, node);
                }
                parentNode = node;
            }

            // handle array entries
            Matcher matcher = ARRAY_PATTERN.matcher(child);
            if (matcher.find()) {
                final String array = matcher.group(1);
                final int index = Integer.parseInt(matcher.group(2));
                JsonNode node = parentNode.get(array);
                if (!(node instanceof ArrayNode)) {
                    throw new IllegalArgumentException("Missing Array node at " + array);
                }
                ArrayNode arrayNode = (ArrayNode) node;
                int more = index - arrayNode.size() + 1;
                if (more > 0) {
                    for (int i = 0; i < more; i++) {
                        arrayNode.add(JSON_NODE_FACTORY.nullNode());
                    }
                }
                arrayNode.set(index, JSON_NODE_FACTORY.textNode(value));
            } else {
                ((ObjectNode)parentNode).set(child, JSON_NODE_FACTORY.textNode(value));
            }
        }

        return root;
    }

    /**
     * Maps JSON tree to {@link Properties} values.
     * @param node      JSON tree to map.
     * @return  JSON tree mapped to properties.
     * @see ConfigHelper#toJson(Map)
     */
    public static Properties fromJson(JsonNode node) {
        final Properties result = new Properties();
        parseNode(result, "", node);
        return result;
    }

    private static void parseNode(Properties result, String prefix, JsonNode node) {
        if (node.isTextual()) {
            result.setProperty(prefix, node.textValue());
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                parseNode(result, String.format("%s[%s]", prefix, i), arrayNode.get(i));
            }
        } else if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                parseNode(result, String.format("%s.%s", prefix, name), objectNode.get(name));
            }
        }
    }

    /**
     * Creates a POJO using the {@link com.fasterxml.jackson.annotation.JsonTypeName} name as path from the {@literal valueType}.
     * @param node      root node
     * @param valueType Java type of POJO to create.
     * @return POJO of type {@literal valueType}.
     * @throws com.fasterxml.jackson.core.JsonProcessingException on parse error.
     */
    public static <T> T toValue(JsonNode node, Class<T> valueType) throws JsonProcessingException {
        JsonTypeName annotation = valueType.getAnnotation(JsonTypeName.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Missing @JsonTypeName annotation on class " + valueType.getName());
        }
        return toValue(node, "/" + annotation.value(), valueType);
    }

    /**
     * Converts given JSON path into a POJO.
     * @param node      root node for path
     * @param path      {@link com.fasterxml.jackson.core.JsonPointer} path.
     * @param valueType Java type of POJO to create.
     * @return POJO of type {@literal valueType}.
     * @throws com.fasterxml.jackson.core.JsonProcessingException on parse error.
     */
    public static <T> T toValue(JsonNode node, String path, Class<T> valueType)
        throws com.fasterxml.jackson.core.JsonProcessingException {

        JsonNode jsonNode = node.at(path);
        if (jsonNode.isMissingNode()) {
            jsonNode = JsonNodeFactory.instance.objectNode();
        }
        return YAML_MAPPER.treeToValue(jsonNode, valueType);
    }

    /**
     * Converts given POJO into a JSON tree.
     * @param value     POJO to convert.
     * @return  root node of JSON tree from value.
     */
    public static JsonNode fromValue(Object value) {
        JsonTypeName annotation = value.getClass().getAnnotation(JsonTypeName.class);
        JsonNode tree = YAML_MAPPER.valueToTree(value);
        if (annotation != null) {
            ObjectNode wrapper = JsonNodeFactory.instance.objectNode();
            wrapper.set(annotation.value(), tree);
            return wrapper;
        } else {
            return tree;
        }
    }

    /**
     * Converts given POJO into {@link Properties}.
     * @param value     POJO to convert.
     * @return      property values from POJO.
     */
    public static Properties toProperties(Object value) {
        return fromJson(fromValue(value));
    }

    /**
     * Create an empty object node.
     * @return  empty {@link ObjectNode}.
     */
    public static JsonNode createObjectNode() {
        return YAML_MAPPER.createObjectNode();
    }

    /**
     * Create a mutable copy of a node.
     * @param jsonNode  source node to copy.
     * @return          copy of the source node.
     */
    public static JsonNode copyObjectNode(JsonNode jsonNode) {
        return ProfilesHelpers.merge(createObjectNode(), jsonNode);
    }

    /**
     * Put all the values from map into an object node.
     * @param targetNode    target JSON object node.
     * @param sourceMap     source {@link Map}.
     */
    public static void putAll(ObjectNode targetNode, Map<String, String> sourceMap) {
        for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
            targetNode.put(entry.getKey(), entry.getValue());
        }
    }
}
