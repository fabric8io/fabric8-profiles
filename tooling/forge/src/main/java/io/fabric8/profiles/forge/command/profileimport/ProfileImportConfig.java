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
package io.fabric8.profiles.forge.command.profileimport;

import java.util.ArrayList;
import java.util.List;

public class ProfileImportConfig {

	private List<ImportCommand> profileCommands = new ArrayList<>();
	private List<ImportCommand> resourceCommands = new ArrayList<>();

	public List<ImportCommand> getProfileCommands() {
		return profileCommands;
	}

	public void setProfileCommands(List<ImportCommand> profileCommands) {
		this.profileCommands = profileCommands;
	}

	public List<ImportCommand> getResourceCommands() {
		return resourceCommands;
	}

	public void setResourceCommands(List<ImportCommand> resourceCommands) {
		this.resourceCommands = resourceCommands;
	}

	public static class ImportCommand {

		private String 			name;
		private boolean 		regExp;
		private ImportAction 	action;
		private String 			replaceWith;
		private Level 			logLevel;
		private String 			logMessage;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean getRegExp() {
			return regExp;
		}

		public void setRegExp(boolean regExp) {
			this.regExp = regExp;
		}

		public ImportAction getAction() {
			return action;
		}

		public void setAction(ImportAction action) {
			this.action = action;
		}

		public String getReplaceWith() {
			return replaceWith;
		}

		public void setReplaceWith(String replaceWith) {
			this.replaceWith = replaceWith;
		}

		public Level getLogLevel() {
			return logLevel;
		}

		public void setLogLevel(Level logLevel) {
			this.logLevel = logLevel;
		}

		public String getLogMessage() {
			return logMessage;
		}

		public void setLogMessage(String logMessage) {
			this.logMessage = logMessage;
		}
	}

	public enum ImportAction {
		delete,
		rename,
		fail
	}

	public enum Level {
		INFO,
		WARN,
		FATAL
	}
}