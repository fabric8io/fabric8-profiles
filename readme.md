Fabric8 Profiles: Composable configuration for Fabric8 Microservices
--------------------------------------------------------------------

This project provides [Fabric8 V1](https://github.com/jboss-fuse/fabric8/) style Profiles support for [Fabric8](http://fabric8.io/).

<p align="center">
  <a href="http://github.com/fabric8io/profiles/">
  	<img src="https://raw.githubusercontent.com/fabric8io/profiles/master/docs/images/logo.png" height="200" width="200" alt="profiles logo"/>
  </a>
</p>

Specifically it provides:

* Support for composable configuration using Profiles
* Simple maven plugin to generate Container projects using materialized Profiles that are pushed to remote Container repos.
* A Sample project to get started

### Profiles

Profiles allow creating and managing configuration fragments that can be combined together in a container. 
A Profile looks like a directory that contains files. Profiles can also reference parent profiles in the {{attribute.parent}} property in io.fabric8.agent.properties file.
This allows Profiles to inherit configuration from other Profiles. Files with the same name are _composed_ to create a new file. 
Profiles are maintained in a Profiles project under {{/profiles}} directory. 

#### Profile Composition

TODO describe file composition for *.properties, *.yaml, *.json
 
### Containers

Containers simply refer to profiles required to generate a project using the property {{profiles}} in the configuration file {{/config/containers/<name>.cfg}}, where name is the container name.

### Profile Project
-----------------------------------

A profiles project is simply a collection of Profiles, containers, global configuration in a fabric8-profiles.cfg and a pom.xml to run he Profiles maven plugin.  

    ...
      <packaging>fabric8-profiles</packaging>
    ...
      <build>
        <plugins>
          <plugin>
            <groupId>io.fabric8.profiles</groupId>
            <artifactId>fabric8-profiles-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <extensions>true</extensions>
          </plugin>
        </plugins>
      </build>
    ...
    
The profiles plugin supports the following goals

- generate:   Generates container projects
- install:   Installs generated container projects into remote repos

## Architecture

The build workflow is triggered from a maven build when a file changes in a Profiles project. This can be done using a tool like Jenkins, which monitors the profiles source and runs a maven build on changes.  
The overall design in the plugin is pluggable, and consists of two major pluggable components:

### Project Reifiers

Are responsible for taking _materialized_ Profiles and generating a container project. 

### Project Processors

Are responsible for tasks such as installing the generated source into remote repo, and updating OpenShift artifacts, etc.

## Sample project:

A sample project is generated under fabric8-profiles-maven-plugin/target/*.tgz or *.zip.
