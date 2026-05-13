pipeline {
    agent any

    environment {
        APP_NAME   = 'jenkins-demo'
        APP_PORT   = '8787'
        DEPLOY_DIR = '/opt/deploy/jenkins-demo'
    }

    stages {
        stage('Check Environment') {
            steps {
                sh '''
                    echo "===== 检查 Java ====="
                    if type java >/dev/null 2>&1; then
                        echo "Java 已安装: $(java -version 2>&1 | head -1)"
                    else
                        echo "Java 未安装，正在安装..."
                        sudo apt-get update -qq && sudo apt-get install -y -qq openjdk-17-jdk || \
                        sudo yum install -y java-17-openjdk-devel
                        echo "Java 安装完成: $(java -version 2>&1 | head -1)"
                    fi

                    echo "===== 检查 Maven ====="
                    if type mvn >/dev/null 2>&1; then
                        echo "Maven 已安装: $(mvn -version 2>&1 | head -1)"
                    else
                        echo "Maven 未安装，正在安装..."
                        sudo apt-get install -y -qq maven || sudo yum install -y maven
                        echo "Maven 安装完成: $(mvn -version 2>&1 | head -1)"
                    fi

                    echo "===== 检查 Git ====="
                    if type git >/dev/null 2>&1; then
                        echo "Git 已安装: $(git --version)"
                    else
                        echo "Git 未安装，正在安装..."
                        sudo apt-get install -y -qq git || sudo yum install -y git
                    fi
                '''
            }
        }

        stage('Checkout') {
            steps {
                git branch: 'linux-host',
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
                    # 创建部署目录
                    mkdir -p ${DEPLOY_DIR}

                    # 停掉旧进程
                    ps aux | grep ${APP_NAME} | grep -v grep | awk '{print \$2}' | xargs -r kill || true
                    sleep 2

                    # 拷贝 jar
                    cp target/*.jar ${DEPLOY_DIR}/${APP_NAME}.jar

                    # 后台启动
                    cd ${DEPLOY_DIR}
                    nohup java -jar ${APP_NAME}.jar --server.port=${APP_PORT} > ${APP_NAME}.log 2>&1 &
                    sleep 3

                    # 检查是否启动成功
                    ps aux | grep ${APP_NAME} | grep -v grep && echo "启动成功" || echo "启动失败，请查看日志: ${DEPLOY_DIR}/${APP_NAME}.log"
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
