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

import org.jboss.forge.addon.parser.yaml.resource.YamlResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.visit.ResourceVisitor;
import org.jboss.forge.addon.resource.visit.VisitContext;

/**
 * {@link org.jboss.forge.addon.resource.visit.ResourceVisitor} for {@link io.fabric8.profiles.config.AbstractConfigDTO} resources in a {@link org.jboss.forge.addon.parser.yaml.resource.YamlResource}.
 */
public abstract class YamlResourceVisitor implements ResourceVisitor {

    public abstract void visit(VisitContext context, YamlResource resource);

    @Override
    public void visit(VisitContext context, Resource<?> resource) {
        if (resource instanceof YamlResourceVisitor) {
            visit(context, (YamlResource) resource);
        }
    }
}
