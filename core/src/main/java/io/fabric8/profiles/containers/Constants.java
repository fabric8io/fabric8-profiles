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

import java.util.regex.Pattern;

/**
 * Shared constants for Containers.
 */
public interface Constants {

    String KARAF_CONTAINER_TYPE = "karaf";
    String JENKINSFILE_CONTAINER_TYPE = JenkinsfileReifier.CONTAINER_TYPE;

    String DEFAULT_CONTAINER_TYPE =
        KARAF_CONTAINER_TYPE + " " + JENKINSFILE_CONTAINER_TYPE;

    String CONTAINERS = "containers/%s.yaml";
    String CONTAINER_FIELD = "container";

    String DEFAULT_PROFILE = "default";

    String FABRIC8_AGENT_PROPERTIES = "io.fabric8.agent.properties";
    String FABRIC8_VERSION_PROPERTIES = "io.fabric8.version.properties";

    String ATTRIBUTE_PARENTS = "attribute.parents";
    Pattern ATTRIBUTE_PARENTS_PATTERN = Pattern.compile(ATTRIBUTE_PARENTS + "\\s*=\\s*(.+)$");

}
