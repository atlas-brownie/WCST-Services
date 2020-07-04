pipeline {
  agent any
  environment {
    HOME = '.'
  }
  
  stages {
    stage('Initialize') {
      steps {
        sh '''
          echo "PATH = ${PATH}"
          echo $JAVA_HOME
          mvn -v
        '''
      }
    }

    stage('Build') {
      steps {
        sh 'mvn clean install'
      }
    }

    stage('Maven JUnit Test'){
      steps {
        sh 'mvn test'
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
        }
      }
    }

    stage('Cleanup Old Docker Artifacts'){
      steps{
        sh 'docker image prune -f'
        sh 'docker volume prune -f'
        sh 'docker container prune -f'
      }
    }

    stage('Build Docker Image'){
      steps {
        script {
          docker.build('wcst-ui-${BUILD_NUMBER}')
          }
      }
    }
    
    stage('Deploy Docker Image on AWS'){
      steps {
        script{
          docker.withRegistry('https://494587492891.dkr.ecr.us-east-1.amazonaws.com/wcst-services', 'ecr:us-east-1:pchong-aws-credentials'){
            docker.image('wcst-ui-${BUILD_NUMBER}').push('latest')
          }
        }
      }
    }

    stage('Remove unused docker image'){
      steps{
        sh "docker image prune -f"
      }
    }
    
  }
}
