pipeline {
    agent any

    tools {
        maven 'Maven3.8'
        jdk 'JDK1.8'
    }

    environment {
        APP_NAME = 'jenkins-demo'
        APP_PORT = '8787'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building the project...'
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging the application...'
                sh 'mvn package -DskipTests'
            }
        }

        stage('Deploy') {
            steps {
                echo 'Deploying the application...'
                sh '''
                    JAR_FILE=$(find target -name "*.jar" | head -1)
                    echo "Deployed JAR: ${JAR_FILE}"
                    echo "Application ${APP_NAME} is ready to run on port ${APP_PORT}"
                '''
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
        always {
            cleanWs()
        }
    }
}
