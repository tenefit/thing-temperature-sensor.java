#!/usr/bin/env groovy
@Library("jenkins.pipeline.shared.library")_


properties([
    buildDiscarder(logRotator(artifactDaysToKeepStr: '5', artifactNumToKeepStr: '5', daysToKeepStr: '5', numToKeepStr: '5')),
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/tenefit/build-template.java/'],
    pipelineTriggers([])
])
node('java') {
    stage("Preparation") {
        checkout scm
        sh 'sudo apt-get -y install zip'
        sh 'curl -s "https://get.sdkman.io" | bash'
    }
    stage("Test"){
        withJavaVersion{
            javaVersion = '9.0.1-oracle'
            command = 'mvn -B -U clean verify'
        }
    }
}

