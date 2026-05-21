pipeline {
    agent any

    parameters {
        string(name: 'TARGET_HOST', defaultValue: '118.25.133.170', description: '目标服务器IP')
        string(name: 'SSH_USER', defaultValue: 'root', description: 'SSH登录用户')
        string(name: 'SSH_CRED_ID', defaultValue: 'server-118.25.133.170-root', description: 'Jenkins中SSH凭据的ID')
        string(name: 'DEPLOY_DIR', defaultValue: '/opt/app/jenkins-demo', description: '远程服务器部署目录')
        string(name: 'APP_PORT', defaultValue: '8787', description: '应用端口')
    }

    environment {
        APP_NAME = 'jenkins-demo'
    }

    stages {
        stage('Check Local Environment') {
            steps {
                sh '''
                    echo "===== Jenkins 节点环境检查 ====="

                    echo "Java:"
                    java -version 2>&1 || echo "Java 未安装"

                    echo "Git:"
                    git --version 2>&1 || echo "Git 未安装"

                    echo "Maven:"
                    if mvn -version >/dev/null 2>&1; then
                        mvn -version 2>&1 | head -1
                    else
                        echo "Maven 未安装，通过 curl 下载..."
                        mkdir -p /var/jenkins_home/tools
                        curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz -o /var/jenkins_home/tools/maven.tar.gz
                        tar xzf /var/jenkins_home/tools/maven.tar.gz -C /var/jenkins_home/tools
                        echo "Maven 下载完成"
                        /var/jenkins_home/tools/apache-maven-3.8.8/bin/mvn -version 2>&1 | head -1
                    fi
                    echo "===== 环境检查完成 ====="
                '''
            }
        }

        stage('Checkout') {
            steps {
                sh '''
                    rm -rf repo && mkdir repo && cd repo
                    for i in 1 2 3; do
                        echo "尝试第 ${i} 次下载..."
                        curl -k -fsSL --retry 3 --retry-delay 5 --connect-timeout 30 --max-time 120 \
                          https://github.com/ZWN1998/jenkins_demo/archive/refs/heads/linux-host.tar.gz -o code.tar.gz && break
                        sleep 5
                    done
                    tar xzf code.tar.gz --strip-components=1
                    rm -f code.tar.gz
                    echo "代码拉取完成: $(ls -la | wc -l) 个文件"
                '''
            }
        }

        stage('Build & Package') {
            steps {
                sh 'cd repo && /var/jenkins_home/tools/apache-maven-3.8.8/bin/mvn clean package -DskipTests'
            }
        }

        stage('Check Remote Environment') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: params.SSH_CRED_ID, keyFileVariable: 'SSH_KEY')]) {
                    sh """
                        echo "===== 远程服务器环境检查 ====="
                        ssh -i \$SSH_KEY -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} '
                            echo "主机名: \$(hostname)"
                            echo "Java:"
                            if type java >/dev/null 2>&1; then
                                java -version 2>&1
                            else
                                echo "Java 未安装，正在安装..."
                                apt-get update -qq && apt-get install -y -qq openjdk-17-jdk || yum install -y java-17-openjdk-devel
                                java -version 2>&1
                            fi
                            echo "磁盘空间:"
                            df -h / | tail -1
                        '
                        echo "===== 远程环境检查完成 ====="
                    """
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: params.SSH_CRED_ID, keyFileVariable: 'SSH_KEY')]) {
                    sh """
                        echo "===== 部署到 ${params.TARGET_HOST} ====="

                        # 1. 创建远程目录
                        ssh -i \$SSH_KEY -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} mkdir -p ${params.DEPLOY_DIR}

                        # 2. 停止旧进程
                        ssh -i \$SSH_KEY -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} "pkill -f ${APP_NAME}.jar" || true
                        sleep 2

                        # 3. 上传 jar
                        scp -i \$SSH_KEY -o StrictHostKeyChecking=no repo/target/*.jar ${params.SSH_USER}@${params.TARGET_HOST}:${params.DEPLOY_DIR}/${APP_NAME}.jar

                        # 4. 启动应用
                        ssh -i \$SSH_KEY -o StrictHostKeyChecking=no ${params.SSH_USER}@${params.TARGET_HOST} "cd ${params.DEPLOY_DIR} && nohup java -jar ${APP_NAME}.jar --server.port=${params.APP_PORT} > ${APP_NAME}.log 2>&1 &"

                        sleep 3
                        echo "===== 部署完成，访问 http://${params.TARGET_HOST}:${params.APP_PORT} ====="
                    """
                }
            }
        }
    }

    post {
        success {
            echo "部署成功！访问 http://${params.TARGET_HOST}:${params.APP_PORT}"
        }
        failure {
            echo '部署失败，请查看控制台输出'
        }
        always {
            cleanWs()
        }
    }
}
