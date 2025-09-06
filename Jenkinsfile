pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKER_IMAGE = "mahimadod/lms-spring-cloud-gateway-service"
        JAVA_HOME = tool name: 'JDK17', type: 'jdk'
        MAVEN_HOME = tool name: 'Maven3.9.9', type: 'maven'
        SONAR_TOKEN = credentials('sonarqube-token')
    }

    tools {
        maven 'Maven3.9.9'
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
                        withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                            configFileProvider([configFile(fileId: 'github-settings', variable: 'MAVEN_SETTINGS')]) {
                                withEnv([
                                    "JAVA_HOME=${env.JAVA_HOME}",
                                    "MAVEN_HOME=${env.MAVEN_HOME}",
                                    "PATH=${env.JAVA_HOME}\\bin;${env.MAVEN_HOME}\\bin;%PATH%" // ✅ CHANGED FOR WINDOWS
                                ]) {
                                    // ❌ Old (for Unix): sh 'mvn clean install --settings $MAVEN_SETTINGS'
                                    // ✅ NEW (for Windows):
                                    bat 'mvn clean install --settings %MAVEN_SETTINGS%'
                                }
                            }
                        }
                    }
                    post {
                        always {
                            // ✅ MAKE SURE THIS PATTERN MATCHES YOUR FILES!
                            junit '**/target/surefire-reports/*.xml'
                            archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                        }
                    }
                }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    bat """
                        mvn clean verify sonar:sonar ^
                            -Dsonar.projectKey=lms-spring-cloud-gateway-service ^
                            -Dsonar.host.url=%SONAR_HOST_URL% ^
                            -Dsonar.login=%SONAR_TOKEN%
                    """
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
                bat """
                    docker rm -f lms-spring-cloud-gateway || exit 0
                    docker run -d --name lms-spring-cloud-gateway --network lms-network -p 8085:8085 ${DOCKER_IMAGE}:${BUILD_NUMBER}
                """
            }
        }
    }
}
