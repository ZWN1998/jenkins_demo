pipeline {
    agent any

    environment {
        APP_NAME    = 'jenkins-demo'
        APP_PORT    = '8787'
        DEPLOY_DIR  = '/deploy'
    }

    stages {
        stage('Check Environment') {
            steps {
                sh '''
                    echo "===== 检查 Java 环境 ====="
                    if type java >/dev/null 2>&1; then
                        echo "Java 已安装: $(java -version 2>&1 | head -1)"
                    else
                        echo "Java 未安装，正在安装 JDK 8..."
                        apt-get update -qq && apt-get install -y -qq openjdk-8-jdk
                    fi

                    echo "===== 检查 Maven 环境 ====="
                    if type mvn >/dev/null 2>&1; then
                        echo "Maven 已安装: $(mvn -version 2>&1 | head -1)"
                    else
                        echo "Maven 未安装，正在安装..."
                        apt-get update -qq && apt-get install -y -qq maven
                        echo "Maven 安装完成: $(mvn -version 2>&1 | head -1)"
                    fi

                    echo "===== 当前环境 ====="
                    java -version 2>&1
                    mvn -version 2>&1
                '''
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
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
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
                sh 'mvn package -DskipTests'
            }
        }

        stage('Deploy') {
            steps {
                sh """
                    ps aux | grep ${APP_NAME} | grep -v grep | awk '{print \$2}' | xargs -r kill || true
                    sleep 2
                    cp target/*.jar ${DEPLOY_DIR}/${APP_NAME}.jar
                    cd ${DEPLOY_DIR}
                    nohup java -jar ${APP_NAME}.jar --server.port=${APP_PORT} > ${APP_NAME}.log 2>&1 &
                    sleep 5
                    ps aux | grep ${APP_NAME} | grep -v grep || echo "进程未运行，请检查日志"
                """
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
