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
                    MAVEN_BIN=/var/jenkins_home/tools/apache-maven-3.8.8/bin/mvn
                    if [ -x "$MAVEN_BIN" ]; then
                        echo "Maven 已缓存，跳过下载"
                        $MAVEN_BIN -version 2>&1 | head -1
                    else
                        echo "Maven 未安装，通过 curl 下载..."
                        mkdir -p /var/jenkins_home/tools
                        curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz -o /var/jenkins_home/tools/maven.tar.gz
                        tar xzf /var/jenkins_home/tools/maven.tar.gz -C /var/jenkins_home/tools
                        echo "Maven 下载完成"
                        $MAVEN_BIN -version 2>&1 | head -1
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

                    echo "=========================================="
                    echo "  代码拉取详情"
                    echo "=========================================="
                    echo "仓库: https://github.com/ZWN1998/jenkins_demo"
                    echo "分支: linux-host"
                    echo ""
                    echo "--- 最近提交记录 ---"
                    curl -s "https://api.github.com/repos/ZWN1998/jenkins_demo/commits?sha=linux-host&per_page=5" | \
                        python3 -c "
import sys, json
try:
    commits = json.load(sys.stdin)
    for c in commits:
        sha = c['sha'][:7]
        msg = c['commit']['message'].split('\\n')[0]
        author = c['commit']['author']['name']
        date = c['commit']['author']['date'][:10]
        print(f'  {sha}  {date}  {author}  {msg}')
except:
    print('  (无法获取提交记录)')
" 2>/dev/null || echo "  (无法获取提交记录)"
                    echo ""
                    echo "--- 文件列表 ---"
                    find . -type f -not -path './.git/*' | sort | sed 's/^/  /'
                    echo ""
                    echo "文件总数: $(find . -type f -not -path './.git/*' | wc -l)"
                    echo "=========================================="
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
                    sh '''#!/bin/bash
                        set +e
                        echo "=========================================="
                        echo "  部署到 ''' + params.TARGET_HOST + '''"
                        echo "=========================================="
                        SSH_OPTS="-i $SSH_KEY -o StrictHostKeyChecking=no -o ConnectTimeout=15 -o ServerAliveInterval=10 -o ServerAliveCountMax=3"

                        # 1. 创建远程目录
                        ssh $SSH_OPTS ''' + params.SSH_USER + '''@''' + params.TARGET_HOST + ''' "mkdir -p ''' + params.DEPLOY_DIR + '''"
                        if [ $? -ne 0 ]; then echo "创建目录失败"; exit 1; fi
                        echo "[1/5] 远程目录已就绪"

                        # 2. 停止旧进程（忽略不存在的情况）
                        ssh $SSH_OPTS ''' + params.SSH_USER + '''@''' + params.TARGET_HOST + ''' "pkill -f ''' + params.APP_NAME + '''.jar 2>/dev/null || true"
                        sleep 2
                        echo "[2/5] 旧进程已停止"

                        # 3. 上传 jar
                        scp $SSH_OPTS repo/target/*.jar ''' + params.SSH_USER + '''@''' + params.TARGET_HOST + ''':''' + params.DEPLOY_DIR + '''/''' + params.APP_NAME + '''.jar
                        if [ $? -ne 0 ]; then echo "上传文件失败"; exit 1; fi
                        echo "[3/5] JAR 文件已上传"

                        # 4. 启动应用
                        echo "[4/5] 启动应用..."
                        ssh -f $SSH_OPTS ''' + params.SSH_USER + '''@''' + params.TARGET_HOST + ''' "cd ''' + params.DEPLOY_DIR + ''' && nohup java -jar ''' + params.APP_NAME + '''.jar --server.port=''' + params.APP_PORT + ''' > ''' + params.APP_NAME + '''.log 2>&1 </dev/null"
                        sleep 2

                        # 5. 等待启动并健康检查
                        echo "[5/5] 等待应用就绪..."
                        READY=0
                        for i in $(seq 1 20); do
                            sleep 3
                            HTTP_CODE=$(ssh $SSH_OPTS ''' + params.SSH_USER + '''@''' + params.TARGET_HOST + ''' "curl -s -o /dev/null -w '%{http_code}' http://localhost:''' + params.APP_PORT + '''/health 2>/dev/null" | tr -d "'")
                            if [ "$HTTP_CODE" = "200" ]; then
                                echo "应用启动成功 (第 ${i} 次检查, HTTP $HTTP_CODE)"
                                READY=1
                                break
                            fi
                            echo "  第 ${i} 次检查: HTTP $HTTP_CODE, 等待中..."
                        done

                        if [ $READY -ne 1 ]; then
                            echo ""
                            echo "=========================================="
                            echo "  应用启动超时 - 输出启动日志:"
                            echo "=========================================="
                            ssh $SSH_OPTS ''' + params.SSH_USER + '''@''' + params.TARGET_HOST + ''' "cat ''' + params.DEPLOY_DIR + '''/''' + params.APP_NAME + '''.log"
                            echo "=========================================="
                            exit 1
                        fi

                        # 显示启动日志
                        echo ""
                        echo "=========================================="
                        echo "  应用启动日志:"
                        echo "=========================================="
                        ssh $SSH_OPTS ''' + params.SSH_USER + '''@''' + params.TARGET_HOST + ''' "cat ''' + params.DEPLOY_DIR + '''/''' + params.APP_NAME + '''.log"
                        echo "=========================================="

                        # 验证应用响应
                        echo ""
                        echo "--- 应用健康检查 ---"
                        ssh $SSH_OPTS ''' + params.SSH_USER + '''@''' + params.TARGET_HOST + ''' "curl -s http://localhost:''' + params.APP_PORT + '''/health"
                        echo ""
                        echo ""
                        echo "=========================================="
                        echo "  部署完成"
                        echo "  访问地址: http://''' + params.TARGET_HOST + ''':''' + params.APP_PORT + '''"
                        echo "=========================================="
                    '''
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
