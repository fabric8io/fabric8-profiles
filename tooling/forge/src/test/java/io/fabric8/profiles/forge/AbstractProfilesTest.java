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
package io.fabric8.profiles.forge;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Base class for command tests.
 */
public abstract class AbstractProfilesTest {

    @Inject
    protected ProjectHelper projectHelper;

    @Inject
    protected UITestHarness uiTestHarness;

    @Deployment
	@AddonDependencies( {
        @AddonDependency(name = "org.jboss.forge.addon:ui-test-harness", version="3.3.1.Final"),
        @AddonDependency(name = "io.fabric8.profiles.forge:fabric8-profiles", version = "1.0-SNAPSHOT"),
        @AddonDependency(name = "org.jboss.forge.addon:maven", version="3.3.1.Final"),
        @AddonDependency(name = "org.jboss.forge.furnace.container:cdi", version="2.23.8.Final")
    })
	public static AddonArchive getDeployment() {
		return ShrinkWrap.create(AddonArchive.class)
            .addBeansXML()
            .addClass(AbstractProfilesTest.class)
            .addClass(ProjectHelper.class);
	}

    protected Project executeCommand(Project project, Class<? extends AbstractProjectCommand> commandClass, CommandInit commandInit, String success) throws Exception {
        try(CommandController tester = uiTestHarness.createCommandController(commandClass, project.getRoot())) {
            tester.initialize();

            commandInit.init(tester);
            assertTrue(tester.isValid());

            Result result = tester.execute();
            assertEquals(success, result.getMessage());
        }
        return projectHelper.refreshProject(project);
    }

    protected interface CommandInit {
        void init(CommandController tester);
    }
}
