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
                    sh '''#!/bin/bash
                        set +e
                        echo "===== 远程服务器环境检查 ====="

                        REMOTE_SCRIPT=$(cat <<'REMOTE_EOF'
echo "主机名: $(hostname)"

NEED_INSTALL=0
if ! type java >/dev/null 2>&1; then
    NEED_INSTALL=1
else
    JAVA_VER=$(java -version 2>&1 | head -1)
    echo "当前 Java: $JAVA_VER"
    if echo "$JAVA_VER" | grep -q '"1\\.'; then
        NEED_INSTALL=1
    else
        MAJOR=$(echo "$JAVA_VER" | grep -oP '"\\K[0-9]+' | head -1)
        if [ "$MAJOR" -lt 17 ] 2>/dev/null; then
            NEED_INSTALL=1
        fi
    fi
fi

if [ "$NEED_INSTALL" -eq 1 ]; then
    echo "Java 版本过低或未安装，正在安装 OpenJDK 17..."
    if command -v apt-get >/dev/null 2>&1; then
        apt-get update -qq && apt-get install -y -qq openjdk-17-jdk
    elif command -v yum >/dev/null 2>&1; then
        yum install -y java-17-openjdk-devel
    fi
fi

# 设置 Java 17 为默认版本
JAVA17_BIN=$(find /usr/lib/jvm -name java -path "*/java-17*" -type f 2>/dev/null | head -1)
if [ -n "$JAVA17_BIN" ]; then
    echo "找到 Java 17: $JAVA17_BIN"
    JAVA17_HOME=$(dirname $(dirname "$JAVA17_BIN"))
    # 通过 alternatives 设置为默认（如果支持）
    if command -v update-alternatives >/dev/null 2>&1; then
        update-alternatives --install /usr/bin/java java "$JAVA17_BIN" 1700 2>/dev/null || true
        update-alternatives --set java "$JAVA17_BIN" 2>/dev/null || true
    fi
    # 直接创建符号链接作为兜底
    ln -sf "$JAVA17_BIN" /usr/local/bin/java
fi

echo "Java:"
java -version 2>&1
echo "磁盘空间:"
df -h / | tail -1
REMOTE_EOF
                        )

                        ENCODED=$(echo "$REMOTE_SCRIPT" | base64 -w0)
                        ssh -i $SSH_KEY -o StrictHostKeyChecking=no -o ConnectTimeout=15 ''' + "${params.SSH_USER}@${params.TARGET_HOST}" + ''' "echo $ENCODED | base64 -d | bash"

                        echo "===== 远程环境检查完成 ====="
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: params.SSH_CRED_ID, keyFileVariable: 'SSH_KEY')]) {
                    sh """
                        set +e
                        echo "===== 部署到 ${params.TARGET_HOST} ====="
                        SSH_OPTS="-i \$SSH_KEY -o StrictHostKeyChecking=no -o ConnectTimeout=15 -o ServerAliveInterval=10 -o ServerAliveCountMax=3"

                        # 1. 创建远程目录
                        ssh \$SSH_OPTS ${params.SSH_USER}@${params.TARGET_HOST} "mkdir -p ${params.DEPLOY_DIR}"
                        if [ \$? -ne 0 ]; then echo "创建目录失败"; exit 1; fi

                        # 2. 停止旧进程（忽略不存在的情况）
                        ssh \$SSH_OPTS ${params.SSH_USER}@${params.TARGET_HOST} "pkill -f ${APP_NAME}.jar || true"
                        sleep 2

                        # 3. 上传 jar
                        scp \$SSH_OPTS repo/target/*.jar ${params.SSH_USER}@${params.TARGET_HOST}:${params.DEPLOY_DIR}/${APP_NAME}.jar
                        if [ \$? -ne 0 ]; then echo "上传文件失败"; exit 1; fi

                        # 4. 启动应用
                        ssh \$SSH_OPTS ${params.SSH_USER}@${params.TARGET_HOST} "cd ${params.DEPLOY_DIR} && nohup java -jar ${APP_NAME}.jar --server.port=${params.APP_PORT} > ${APP_NAME}.log 2>&1 &"
                        if [ \$? -ne 0 ]; then echo "启动应用失败"; exit 1; fi

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
