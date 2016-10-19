#!/usr/bin/groovy

node {
  git 'http://gogs/gogsadmin/fabric8-profiles.git'

  echo 'NOTE: running pipelines for the first time will take longer as build and base docker images are pulled onto the node'
  kubernetes.pod('buildpod').withImage('fabric8/maven-builder')
      .withPrivileged(true)
      .withHostPathMount('/var/run/docker.sock','/var/run/docker.sock')
      .withHostPathMount('/root/.mvnrepository','/var/lib/maven/repository')
      .withEnvVar('DOCKER_CONFIG','/home/jenkins/.docker/')
      .withSecret('jenkins-docker-cfg','/home/jenkins/.docker')
      .withSecret('jenkins-maven-settings','/root/.m2')
      .withServiceAccount('jenkins')
      .inside {

    stage 'Deploy'
    sh 'mvn clean install -U org.apache.maven.plugins:maven-deploy-plugin:2.8.2:deploy'
  }
}
