pipeline {
  agent any
  stages {
    stage('Test') {
      agent { label 'medium' }
      tools {
        jdk 'jdk11'
      }
      steps {
        step([$class: 'WsCleanup'])
        checkout scm
        sh './gradlew --no-daemon --parallel clean checkstyleMain forbiddenApisMain test'
      }
    }
  }
}
