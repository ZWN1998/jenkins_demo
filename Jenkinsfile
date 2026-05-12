pipeline {
    agent any

    environment {
        APP_NAME      = 'jenkins-demo'
        APP_PORT      = '8787'
        DEPLOY_DIR    = '/deploy'
        MAVEN_VERSION = '3.8.8'
        MAVEN_HOME    = '/var/jenkins_home/tools/apache-maven-3.8.8'
    }

    stages {
        stage('Setup Maven') {
            steps {
                sh """
                    if [ -d "${MAVEN_HOME}" ]; then
                        echo "Maven 已安装"
                    else
                        echo "Maven 未安装，开始下载..."
                        mkdir -p /var/jenkins_home/tools
                        curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o /var/jenkins_home/tools/maven.tar.gz
                        tar xzf /var/jenkins_home/tools/maven.tar.gz -C /var/jenkins_home/tools
                        echo "Maven 下载完成"
                    fi
                    ${MAVEN_HOME}/bin/mvn -version
                """
            }
        }

        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/ZWN1998/jenkins_demo.git'
            }
        }

        stage('Build') {
            steps {
                sh "${MAVEN_HOME}/bin/mvn clean compile"
            }
        }

        stage('Test') {
            steps {
                sh "${MAVEN_HOME}/bin/mvn test"
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh "${MAVEN_HOME}/bin/mvn package -DskipTests"
            }
        }

        stage('Deploy') {
            steps {
                sh 'mkdir -p /var/jenkins_home/deploy && cp target/*.jar /var/jenkins_home/deploy/jenkins-demo.jar'
                sh 'pkill -f jenkins-demo.jar || true'
                sh 'cd /var/jenkins_home/deploy && nohup java -jar jenkins-demo.jar --server.port=8787 > jenkins-demo.log 2>&1 &'
            }
        }
    }

    post {
        success {
            echo "部署成功！访问 http://服务器IP:${APP_PORT}"
        }
        failure {
            echo '部署失败，请查看控制台输出'
        }
        always {
            cleanWs()
        }
    }
}
