pipeline {
  agent any
  stages {
    stage('Test') {
      agent { label 'medium' }
      tools {
        jdk 'jdk8'
      }
      steps {
        step([$class: 'WsCleanup'])
        checkout scm
        sh './gradlew --no-daemon --parallel clean test'
      }
    }
  }
}
