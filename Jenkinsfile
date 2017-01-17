#!/usr/bin/groovy
@Library('github.com/fabric8io/fabric8-pipeline-library@v2.2.309')

utils = new io.fabric8.Utils()

mavenNode {
  checkout scm
  echo 'NOTE: running pipelines for the first time will take longer as build and base docker images are pulled onto the node'
  container(name: 'maven') {
    stage 'Deploy'
    sh 'mvn clean install -U org.apache.maven.plugins:maven-deploy-plugin:2.8.2:deploy'
  }
}
