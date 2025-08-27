pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKER_IMAGE = "mahimadod/lms-spring-cloud-gateway-service"
        JAVA_HOME = tool name: 'JDK17', type: 'jdk'
        MAVEN_HOME = tool name: 'Maven3.9.9', type: 'maven'
    }

    stages {
        stage('Clean Workspace') {
                    steps {
                        deleteDir()
                    }
                }

        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/mahimadod/lms-spring-cloud-gateway-service.git'
            }
        }
        stage('Build & Test') {
            steps {
                // Properly inject JAVA_HOME and MAVEN_HOME into the shell PATH
                withEnv([
                    "JAVA_HOME=${env.JAVA_HOME}",
                    "MAVEN_HOME=${env.MAVEN_HOME}",
                    "PATH=${env.JAVA_HOME}/bin:${env.MAVEN_HOME}/bin:$PATH"
                ]) {
                    sh 'mvn clean install'
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'dockerhub-credentials') {
                        def customImage = docker.build("${DOCKER_IMAGE}:${env.BUILD_NUMBER}")
                        customImage.push()
                        customImage.tag('latest')
                        customImage.push('latest')
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                echo 'Deploying Docker container...'
                sh '''
                    docker rm -f lms-spring-cloud-gateway || true
                    docker run -d --name lms-spring-cloud-gateway -p 8085:8085 ${DOCKER_IMAGE}:${BUILD_NUMBER}
                '''
            }
        }
    }
}
