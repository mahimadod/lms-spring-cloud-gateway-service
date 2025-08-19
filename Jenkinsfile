pipeline {
    agent any

    environment {
        IMAGE_NAME = 'mahimadod/lms-spring-cloude-gateway-service'
        IMAGE_TAG = 'latest'
    }

    stages {
        stage('Checkout') {
            steps {
                git credentialsId: 'github-creds', url: 'https://github.com/mahimadod/lms-discovery-service.git'
            }
        }

        stage('Build JAR') {
                    steps {
                        sh 'mvn clean package'
                    }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    dockerImage = docker.build("${IMAGE_NAME}:${IMAGE_TAG}")
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    docker.withRegistry('', 'dockerhub-creds') {
                        dockerImage.push()
                    }
                }
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                sh '''
                docker-compose down
                docker-compose pull
                docker-compose up -d --build
                '''
            }
        }
    }
}