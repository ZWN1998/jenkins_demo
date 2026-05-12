pipeline {
    agent any

    environment {
        APP_NAME    = 'jenkins-demo'
        APP_PORT    = '8787'
        DEPLOY_DIR  = '/deploy'
        JAVA_VERSION = '8'
        MAVEN_VERSION = '3.8.8'
    }

    stages {
        stage('Check Environment') {
            steps {
                sh '''
                    echo "===== 检查 Java 环境 ====="
                    if command -v java &>/dev/null; then
                        echo "Java 已安装: $(java -version 2>&1 | head -1)"
                    else
                        echo "Java 未安装，正在安装 JDK ${JAVA_VERSION}..."
                        apt-get update -qq
                        apt-get install -y -qq openjdk-${JAVA_VERSION}-jdk
                        echo "Java 安装完成: $(java -version 2>&1 | head -1)"
                    fi

                    echo "===== 检查 Maven 环境 ====="
                    if command -v mvn &>/dev/null; then
                        echo "Maven 已安装: $(mvn -version | head -1)"
                    else
                        echo "Maven 未安装，正在安装 Maven..."
                        apt-get install -y -qq maven
                        echo "Maven 安装完成: $(mvn -version | head -1)"
                    fi

                    echo "===== 检查 Git 环境 ====="
                    if command -v git &>/dev/null; then
                        echo "Git 已安装: $(git --version)"
                    else
                        echo "Git 未安装，正在安装..."
                        apt-get install -y -qq git
                        echo "Git 安装完成: $(git --version)"
                    fi
                '''
            }
        }

        stage('Checkout') {
            steps {
                echo '从 GitHub 拉取代码...'
                git branch: 'main',
                    url: 'https://github.com/ZWN1998/jenkins_demo.git'
            }
        }

        stage('Build') {
            steps {
                echo '编译项目...'
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                echo '运行测试...'
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
                echo '打包项目...'
                sh 'mvn package -DskipTests'
            }
        }

        stage('Deploy') {
            steps {
                echo '部署项目...'
                sh """
                    # 停掉旧进程
                    ps aux | grep '${APP_NAME}' | grep -v grep | awk '{print \\$2}' | xargs -r kill || true
                    sleep 2

                    # 拷贝 jar 到部署目录
                    cp target/*.jar ${DEPLOY_DIR}/${APP_NAME}.jar

                    # 后台启动
                    cd ${DEPLOY_DIR}
                    nohup java -jar ${APP_NAME}.jar --server.port=${APP_PORT} > ${APP_NAME}.log 2>&1 &
                    echo "启动完成，等待 5 秒检查进程..."
                    sleep 5
                    ps aux | grep '${APP_NAME}' | grep -v grep || echo "警告: 进程未运行，请检查日志 ${DEPLOY_DIR}/${APP_NAME}.log"
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
