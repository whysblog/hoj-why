#!/bin/bash

# HOJ 本地部署脚本（在服务器上直接运行）
# 使用方法: ./deploy.sh
# 功能: 自动检测并安装缺失的依赖（Maven、Node.js、Docker等）

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}HOJ 本地部署脚本${NC}"
echo -e "${GREEN}========================================${NC}"

# 自动检测部署路径
if [ -d "/root/HOJ-Deploy/standAlone" ]; then
    DEPLOY_PATH="/root/HOJ-Deploy/standAlone"
elif [ -d "$(pwd)/../standAlone" ]; then
    DEPLOY_PATH="$(cd $(pwd)/../standAlone && pwd)"
elif [ -f "docker-compose.yml" ]; then
    DEPLOY_PATH="$(pwd)"
else
    read -p "请输入部署路径 (默认: /root/HOJ-Deploy/standAlone): " DEPLOY_PATH
    DEPLOY_PATH=${DEPLOY_PATH:-/root/HOJ-Deploy/standAlone}
fi

echo -e "${GREEN}检测到部署路径: ${DEPLOY_PATH}${NC}"

# 自动检测代码路径
if [ -d "/root/hoj" ]; then
    CODE_PATH="/root/hoj"
elif [ -d "$(pwd)/../.." ] && [ -f "$(pwd)/../../hoj-vue/package.json" ]; then
    CODE_PATH="$(cd $(pwd)/../.. && pwd)"
else
    read -p "请输入代码路径 (默认: /root/hoj): " CODE_PATH
    CODE_PATH=${CODE_PATH:-/root/hoj}
fi

echo -e "${GREEN}检测到代码路径: ${CODE_PATH}${NC}"

# 输入MySQL容器名
read -p "请输入MySQL容器名 (默认: hoj-mysql): " MYSQL_CONTAINER
MYSQL_CONTAINER=${MYSQL_CONTAINER:-hoj-mysql}

# 输入MySQL root密码
read -p "请输入MySQL root密码 (默认: hoj123456): " MYSQL_PASSWORD
MYSQL_PASSWORD=${MYSQL_PASSWORD:-hoj123456}

# 检查并安装Git（如果需要）
if ! command -v git &> /dev/null; then
    echo -e "${YELLOW}未找到Git，开始安装...${NC}"
    if command -v apt-get &> /dev/null; then
        sudo apt-get update && sudo apt-get install -y git
    elif command -v yum &> /dev/null; then
        sudo yum install -y git
    else
        echo -e "${RED}错误: 无法自动安装Git，请手动安装${NC}"
        exit 1
    fi
    echo -e "${GREEN}Git安装完成${NC}"
fi

# 检查Git仓库
if [ ! -d "$CODE_PATH/.git" ]; then
    read -p "代码目录不是Git仓库，是否执行git clone? (y/n, 默认: y): " DO_CLONE
    DO_CLONE=${DO_CLONE:-y}
    if [ "$DO_CLONE" = "y" ] || [ "$DO_CLONE" = "Y" ]; then
        read -p "请输入Git仓库地址 (默认: https://gitee.com/whysblog/hoj-why.git): " GIT_REPO
        GIT_REPO=${GIT_REPO:-https://gitee.com/whysblog/hoj-why.git}
        echo -e "${YELLOW}执行git clone...${NC}"
        cd $(dirname $CODE_PATH)
        git clone $GIT_REPO $(basename $CODE_PATH)
        echo -e "${GREEN}Git clone完成${NC}"
    fi
fi

# 检查必要文件
BACKEND_SOURCE="$CODE_PATH/hoj-springboot"
FRONTEND_SOURCE="$CODE_PATH/hoj-vue"
CLIPBOARD_SQL="$CODE_PATH/create_clipboard_table.sql"
NEW_ROLES_SQL="$CODE_PATH/add_new_roles.sql"

if [ ! -d "$BACKEND_SOURCE" ]; then
    echo -e "${RED}错误: 后端源码目录不存在: $BACKEND_SOURCE${NC}"
    exit 1
fi

if [ ! -d "$FRONTEND_SOURCE" ]; then
    echo -e "${RED}错误: 前端源码目录不存在: $FRONTEND_SOURCE${NC}"
    exit 1
fi

if [ ! -f "$CLIPBOARD_SQL" ]; then
    echo -e "${RED}错误: 数据库SQL文件不存在: $CLIPBOARD_SQL${NC}"
    exit 1
fi

if [ ! -f "$NEW_ROLES_SQL" ]; then
    echo -e "${RED}错误: 新角色SQL文件不存在: $NEW_ROLES_SQL${NC}"
    exit 1
fi

# 检查并安装curl（如果需要）
if ! command -v curl &> /dev/null; then
    echo -e "${YELLOW}未找到curl，开始安装...${NC}"
    if command -v apt-get &> /dev/null; then
        sudo apt-get update && sudo apt-get install -y curl
    elif command -v yum &> /dev/null; then
        sudo yum install -y curl
    else
        echo -e "${RED}错误: 无法自动安装curl，请手动安装${NC}"
        exit 1
    fi
    echo -e "${GREEN}curl安装完成${NC}"
fi

# 检查并安装Docker（如果需要）
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}未找到Docker，开始安装...${NC}"
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    rm -f get-docker.sh
    sudo usermod -aG docker $USER
    echo -e "${GREEN}Docker安装完成，请重新登录或执行: newgrp docker${NC}"
    echo -e "${YELLOW}注意: 如果Docker命令仍不可用，请重新运行脚本${NC}"
    # 尝试加载Docker（如果可能）
    if [ -f /usr/bin/docker ]; then
        export PATH="/usr/bin:$PATH"
    fi
fi

# 检查并安装Docker Compose（如果需要）
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null 2>&1; then
    echo -e "${YELLOW}未找到Docker Compose，开始安装...${NC}"
    if command -v apt-get &> /dev/null; then
        sudo apt-get update && sudo apt-get install -y docker-compose-plugin
    elif command -v yum &> /dev/null; then
        sudo yum install -y docker-compose-plugin
    else
        # 安装docker-compose standalone
        sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
    fi
    echo -e "${GREEN}Docker Compose安装完成${NC}"
fi

# 检查MySQL容器
if ! docker ps | grep -q "$MYSQL_CONTAINER"; then
    echo -e "${RED}错误: MySQL容器 $MYSQL_CONTAINER 未运行${NC}"
    exit 1
fi

echo -e "${YELLOW}开始部署...${NC}"

# 1. 编译后端
echo -e "${YELLOW}[1/6] 编译后端...${NC}"

# 检查并安装Java 8（如果需要）
REQUIRED_JAVA_VERSION="1.8"
if ! command -v java &> /dev/null; then
    echo -e "${YELLOW}未找到Java，开始安装Java 8...${NC}"
    if command -v apt-get &> /dev/null; then
        sudo apt-get update
        sudo apt-get install -y openjdk-8-jdk
        sudo update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
        sudo update-alternatives --set javac /usr/lib/jvm/java-8-openjdk-amd64/bin/javac
    elif command -v yum &> /dev/null; then
        sudo yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel
    else
        echo -e "${RED}错误: 无法自动安装Java，请手动安装Java 8${NC}"
        exit 1
    fi
    echo -e "${GREEN}Java 8安装完成${NC}"
else
    # 检查Java版本
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1,2)
    JAVA_MAJOR=$(echo $JAVA_VERSION | cut -d'.' -f1)
    JAVA_MINOR=$(echo $JAVA_VERSION | cut -d'.' -f2)
    
    if [ -z "$JAVA_MINOR" ]; then
        JAVA_MINOR=0
    fi
    
    # 检查是否是Java 8
    if [ "$JAVA_MAJOR" != "1" ] || [ "$JAVA_MINOR" != "8" ]; then
        echo -e "${YELLOW}当前Java版本: $JAVA_VERSION，项目需要Java 8，开始安装...${NC}"
        if command -v apt-get &> /dev/null; then
            sudo apt-get update
            sudo apt-get install -y openjdk-8-jdk
            # 设置Java 8为默认版本
            if [ -d "/usr/lib/jvm/java-8-openjdk-amd64" ]; then
                sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java 1
                sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-8-openjdk-amd64/bin/javac 1
                sudo update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
                sudo update-alternatives --set javac /usr/lib/jvm/java-8-openjdk-amd64/bin/javac
            fi
        elif command -v yum &> /dev/null; then
            sudo yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel
            sudo alternatives --set java /usr/lib/jvm/java-1.8.0-openjdk-amd64/jre/bin/java
        else
            echo -e "${RED}错误: 无法自动安装Java 8，请手动安装${NC}"
            exit 1
        fi
        echo -e "${GREEN}Java 8安装完成${NC}"
        # 重新加载环境
        export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
        export PATH=$JAVA_HOME/bin:$PATH
    else
        echo -e "${GREEN}Java版本检查通过: $JAVA_VERSION${NC}"
    fi
fi

# 设置JAVA_HOME（如果未设置）
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/usr/lib/jvm/java-8-openjdk-amd64" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
    elif [ -d "/usr/lib/jvm/java-1.8.0-openjdk-amd64" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
    fi
    if [ -n "$JAVA_HOME" ]; then
        export PATH=$JAVA_HOME/bin:$PATH
        echo -e "${GREEN}已设置JAVA_HOME: $JAVA_HOME${NC}"
    fi
fi

# 检查并安装Maven（如果需要）
if ! command -v mvn &> /dev/null; then
    echo -e "${YELLOW}未找到Maven，开始安装...${NC}"
    if command -v apt-get &> /dev/null; then
        sudo apt-get update && sudo apt-get install -y maven
    elif command -v yum &> /dev/null; then
        sudo yum install -y maven
    else
        echo -e "${RED}错误: 无法自动安装Maven，请手动安装${NC}"
        exit 1
    fi
    echo -e "${GREEN}Maven安装完成${NC}"
fi

cd "$BACKEND_SOURCE"
if [ -f "pom.xml" ]; then
    echo -e "${GREEN}Java版本: $(java -version 2>&1 | head -1)${NC}"
    echo -e "${GREEN}Maven版本: $(mvn --version | head -1)${NC}"
    # 确保Maven使用正确的Java版本
    export JAVA_HOME=${JAVA_HOME:-$(readlink -f $(which java) | sed "s:bin/java::")}
    mvn clean package -DskipTests
    BACKEND_JAR="$BACKEND_SOURCE/DataBackup/target/hoj-backend-4.6.jar"
    if [ ! -f "$BACKEND_JAR" ]; then
        echo -e "${RED}错误: 后端编译失败，jar文件不存在${NC}"
        exit 1
    fi
    echo -e "${GREEN}后端编译完成${NC}"
else
    echo -e "${RED}错误: 未找到pom.xml文件${NC}"
    exit 1
fi

# 2. 编译前端
echo -e "${YELLOW}[2/6] 编译前端...${NC}"
cd "$FRONTEND_SOURCE"
if [ -f "package.json" ]; then
    # 检查dist目录是否已存在
    if [ -d "dist" ] && [ -n "$(ls -A dist 2>/dev/null)" ]; then
        echo -e "${GREEN}dist目录已存在，跳过前端编译${NC}"
        echo -e "${GREEN}前端编译完成${NC}"
        echo -e "${YELLOW}前端编译步骤完成，继续执行下一步...${NC}"
    else
    # 加载nvm（如果存在）
    if [ -s "$HOME/.nvm/nvm.sh" ]; then
        export NVM_DIR="$HOME/.nvm"
        [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
        [ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"
    fi
    
    # 检查并安装nvm（如果需要）
    if ! command -v node &> /dev/null; then
        echo -e "${YELLOW}未找到node，检查nvm...${NC}"
        
        # 如果nvm目录存在但未加载，尝试加载
        if [ -d "$HOME/.nvm" ] && [ ! -s "$HOME/.nvm/nvm.sh" ]; then
            echo -e "${YELLOW}nvm目录存在但未正确安装，重新安装...${NC}"
            rm -rf "$HOME/.nvm"
        fi
        
        # 如果nvm不存在，安装nvm
        if [ ! -d "$HOME/.nvm" ]; then
            echo -e "${YELLOW}开始安装nvm...${NC}"
            curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash || {
                echo -e "${RED}错误: nvm安装失败，尝试使用备用源...${NC}"
                curl -o- https://gitee.com/mirrors/nvm/raw/master/install.sh | bash || {
                    echo -e "${RED}错误: nvm安装失败，请手动安装${NC}"
                    exit 1
                }
            }
            export NVM_DIR="$HOME/.nvm"
            [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
            [ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"
            echo -e "${GREEN}nvm安装完成${NC}"
        else
            # nvm目录存在，加载它
            export NVM_DIR="$HOME/.nvm"
            [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
            [ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"
        fi
        
        # 使用nvm安装node
        if command -v nvm &> /dev/null; then
            echo -e "${YELLOW}使用nvm安装Node.js LTS版本...${NC}"
            nvm install --lts
            nvm use --lts
            nvm alias default node
        else
            echo -e "${RED}错误: nvm安装后仍无法使用，请手动安装node${NC}"
            exit 1
        fi
    fi
    
    # 再次加载nvm环境（确保node可用）
    if [ -s "$HOME/.nvm/nvm.sh" ]; then
        export NVM_DIR="$HOME/.nvm"
        [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
    fi
    
    # 检查node和npm
    if ! command -v node &> /dev/null; then
        echo -e "${RED}错误: node未找到，请检查安装${NC}"
        exit 1
    fi
    
    if ! command -v npm &> /dev/null; then
        echo -e "${RED}错误: npm未找到，请检查node安装${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Node版本: $(node --version)${NC}"
    echo -e "${GREEN}NPM版本: $(npm --version)${NC}"
    
    echo -e "${YELLOW}开始安装前端依赖...${NC}"
    npm install --loglevel=error 2>&1 | tail -20
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
        echo -e "${RED}错误: npm install 失败${NC}"
        exit 1
    fi
    echo -e "${GREEN}依赖安装完成${NC}"
    
    echo -e "${YELLOW}开始编译前端...${NC}"
    # 监控npm run build的输出，检测"Build Complete"字符串
    BUILD_COMPLETE_DETECTED=false
    
    # 使用timeout防止卡住（如果可用），最多等待30分钟
    if command -v timeout &> /dev/null; then
        timeout 1800 sh -c '
            npm run build 2>&1 | while IFS= read -r line; do
                echo "$line"
                # 检测"Build Complete"或类似的完成标志（不区分大小写）
                if echo "$line" | grep -qiE "(Build Complete|build complete|Build complete|DONE|Compiled successfully|compiled successfully)"; then
                    echo -e "\033[0;32m检测到编译完成标志，结束前端编译\033[0m"
                    # 立即结束编译进程
                    pkill -P $$ npm 2>/dev/null || true
                    exit 0
                fi
            done
        ' || {
            BUILD_EXIT_CODE=$?
            if [ $BUILD_EXIT_CODE -eq 124 ]; then
                echo -e "${RED}错误: 前端编译超时（超过30分钟）${NC}"
                pkill -f "npm run build" 2>/dev/null || true
                exit 1
            fi
        }
    else
        # 如果没有timeout命令，直接运行并监控输出
        npm run build 2>&1 | while IFS= read -r line; do
            echo "$line"
            # 检测"Build Complete"或类似的完成标志
            if echo "$line" | grep -qiE "(Build Complete|build complete|Build complete|DONE|Compiled successfully|compiled successfully)"; then
                echo -e "${GREEN}检测到编译完成标志，结束前端编译${NC}"
                # 立即结束编译进程
                pkill -P $$ npm 2>/dev/null || true
                break
            fi
        done
    fi
    
    # 等待一下确保文件写入完成
    sleep 2
    
    echo -e "${GREEN}前端编译完成${NC}"
        echo -e "${YELLOW}前端编译步骤完成，继续执行下一步...${NC}"
    fi
else
    echo -e "${RED}错误: 未找到package.json文件${NC}"
    exit 1
fi

# 3. 创建clipboard表
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}开始数据库操作...${NC}"
echo -e "${YELLOW}[3/6] 创建clipboard数据表...${NC}"
if docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_PASSWORD" hoj < "$CLIPBOARD_SQL" 2>/dev/null; then
    echo -e "${GREEN}clipboard表创建成功${NC}"
else
    # 检查表是否已存在
    if docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_PASSWORD" hoj -e "SHOW TABLES LIKE 'clipboard';" 2>/dev/null | grep -q clipboard; then
        echo -e "${YELLOW}clipboard表已存在，跳过创建${NC}"
    else
        echo -e "${YELLOW}警告: 创建clipboard表时出现错误，请手动检查${NC}"
    fi
fi

# 4. 添加新用户角色
echo -e "${YELLOW}[4/6] 添加新用户角色 (1009, 1010)...${NC}"
if docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_PASSWORD" hoj < "$NEW_ROLES_SQL" 2>/dev/null; then
    echo -e "${GREEN}新用户角色添加成功${NC}"
else
    echo -e "${YELLOW}警告: 添加新用户角色时出现错误，请手动检查${NC}"
    # 检查角色是否已存在
    if docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_PASSWORD" hoj -e "SELECT id FROM role WHERE id IN (1009, 1010);" 2>/dev/null | grep -qE "(1009|1010)"; then
        echo -e "${YELLOW}新用户角色可能已存在${NC}"
    fi
fi

# 5. 更新后端jar文件
echo -e "${YELLOW}[5/6] 更新后端jar文件...${NC}"
if [ -d "$DEPLOY_PATH/backend" ]; then
    cp "$BACKEND_JAR" "$DEPLOY_PATH/backend/app.jar"
    echo -e "${GREEN}后端jar文件已更新${NC}"
elif [ -d "$DEPLOY_PATH/../src/backend" ]; then
    cp "$BACKEND_JAR" "$DEPLOY_PATH/../src/backend/app.jar"
    echo -e "${GREEN}后端jar文件已更新到构建目录${NC}"
else
    echo -e "${YELLOW}警告: 未找到后端目录，跳过后端更新${NC}"
fi

# 6. 更新前端dist文件
echo -e "${YELLOW}[6/6] 更新前端dist文件...${NC}"
if [ -d "$DEPLOY_PATH/../src/frontend/dist" ]; then
    rm -rf "$DEPLOY_PATH/../src/frontend/dist"
    cp -r "$FRONTEND_SOURCE/dist" "$DEPLOY_PATH/../src/frontend/"
    echo -e "${GREEN}前端dist文件已更新${NC}"
elif [ -d "/root/dist" ]; then
    rm -rf /root/dist/*
    cp -r "$FRONTEND_SOURCE/dist"/* /root/dist/
    echo -e "${GREEN}前端dist文件已更新到 /root/dist${NC}"
else
    # 创建dist目录
    mkdir -p /root/dist
    cp -r "$FRONTEND_SOURCE/dist"/* /root/dist/
    echo -e "${GREEN}前端dist文件已更新到 /root/dist${NC}"
fi

# 7. 检查并修改docker-compose.yml配置
echo -e "${YELLOW}[7/7] 检查docker-compose.yml配置...${NC}"
if [ -f "$DEPLOY_PATH/docker-compose.yml" ]; then
    # 检查前端是否使用dist挂载
    if grep -q "/root/dist:/usr/share/nginx/html" "$DEPLOY_PATH/docker-compose.yml"; then
        echo -e "${GREEN}✓ 前端已配置使用 /root/dist 挂载${NC}"
    else
        echo -e "${YELLOW}⚠ 前端未配置使用 /root/dist 挂载${NC}"
        read -p "是否自动修改为使用 /root/dist 挂载? (y/n, 默认: y): " FIX_FRONTEND
        FIX_FRONTEND=${FIX_FRONTEND:-y}
        if [ "$FIX_FRONTEND" = "y" ] || [ "$FIX_FRONTEND" = "Y" ]; then
            # 备份原文件
            cp "$DEPLOY_PATH/docker-compose.yml" "$DEPLOY_PATH/docker-compose.yml.bak.$(date +%Y%m%d_%H%M%S)"
            # 使用sed修改volumes配置
            if grep -q "hoj-frontend:" "$DEPLOY_PATH/docker-compose.yml"; then
                # 替换volumes中的路径为/root/dist
                sed -i '/hoj-frontend:/,/^[[:space:]]*[a-zA-Z-]/ {
                    s|- .*:/usr/share/nginx/html|- /root/dist:/usr/share/nginx/html|
                }' "$DEPLOY_PATH/docker-compose.yml"
                
                # 如果volumes不存在，添加volumes配置
                if ! grep -A 10 "hoj-frontend:" "$DEPLOY_PATH/docker-compose.yml" | grep -q "volumes:"; then
                    sed -i '/hoj-frontend:/a\    volumes:\n      - /root/dist:/usr/share/nginx/html' "$DEPLOY_PATH/docker-compose.yml"
                fi
                
                echo -e "${GREEN}✓ 已自动修改前端配置为使用 /root/dist 挂载${NC}"
            else
                echo -e "${YELLOW}警告: 未找到 hoj-frontend 配置，无法自动修改${NC}"
            fi
        fi
    fi
    
    # 检查后端是否使用build方式（自己的镜像）
    if grep -A 5 "hoj-backend:" "$DEPLOY_PATH/docker-compose.yml" | grep -q "build:"; then
        echo -e "${GREEN}✓ 后端已配置使用 build 方式（自己的镜像）${NC}"
    else
        echo -e "${YELLOW}⚠ 后端未配置使用 build 方式${NC}"
        read -p "是否自动修改为使用 build 方式? (y/n, 默认: y): " FIX_BACKEND
        FIX_BACKEND=${FIX_BACKEND:-y}
        if [ "$FIX_BACKEND" = "y" ] || [ "$FIX_BACKEND" = "Y" ]; then
            # 备份原文件
            cp "$DEPLOY_PATH/docker-compose.yml" "$DEPLOY_PATH/docker-compose.yml.bak.$(date +%Y%m%d_%H%M%S)"
            # 修改后端配置，添加build方式
            if grep -q "hoj-backend:" "$DEPLOY_PATH/docker-compose.yml"; then
                # 检查是否已有build配置
                if ! grep -A 5 "hoj-backend:" "$DEPLOY_PATH/docker-compose.yml" | grep -q "build:"; then
                    # 在hoj-backend:后面添加build配置（在container_name之前）
                    sed -i '/hoj-backend:/a\    build:\n      context: ../src/backend\n      dockerfile: Dockerfile\n    image: hoj-backend-custom' "$DEPLOY_PATH/docker-compose.yml"
                    echo -e "${GREEN}✓ 已自动修改后端配置为使用 build 方式${NC}"
                else
                    echo -e "${GREEN}✓ 后端已有 build 配置${NC}"
                fi
            else
                echo -e "${YELLOW}警告: 未找到 hoj-backend 配置，无法自动修改${NC}"
            fi
        fi
    fi
else
    echo -e "${YELLOW}警告: 未找到docker-compose.yml文件${NC}"
fi

# 重启服务
echo ""
read -p "是否重启服务? (y/n, 默认: y): " RESTART_SERVICES
RESTART_SERVICES=${RESTART_SERVICES:-y}

if [ "$RESTART_SERVICES" = "y" ] || [ "$RESTART_SERVICES" = "Y" ]; then
    echo -e "${YELLOW}重建并重启服务（hoj-backend/hoj-frontend）...${NC}"
    cd "$DEPLOY_PATH"
    if [ -f "docker-compose.yml" ]; then
        REBUILD_OK=false
        # 优先使用 docker-compose 执行重建
        if command -v docker-compose &> /dev/null; then
            if docker-compose up -d --build --force-recreate hoj-backend hoj-frontend; then
                REBUILD_OK=true
            fi
        # 尝试使用 docker compose 执行重建
        elif docker compose version &> /dev/null 2>&1; then
            if docker compose up -d --build --force-recreate hoj-backend hoj-frontend; then
                REBUILD_OK=true
            fi
        fi

        if [ "$REBUILD_OK" = true ]; then
            echo -e "${GREEN}服务重建并重启完成${NC}"
        else
            echo -e "${RED}错误: 服务重建失败，请检查 docker-compose 配置和构建日志${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}警告: 未找到docker-compose.yml文件${NC}"
    fi
else
    echo -e "${YELLOW}跳过服务重启，请手动重启${NC}"
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}部署完成!${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${YELLOW}部署信息:${NC}"
echo -e "  代码路径: ${CODE_PATH}"
echo -e "  部署路径: ${DEPLOY_PATH}"
echo -e "  MySQL容器: ${MYSQL_CONTAINER}"
echo -e "  后端jar: 已编译并更新"
echo -e "  前端dist: 已编译并更新到 /root/dist"
echo -e "  数据库表: clipboard表已创建"
echo -e "  用户角色: 角色1009、1010已添加"
echo -e "  docker-compose.yml: 已检查配置"
echo -e ""
echo -e "${YELLOW}请检查服务是否正常运行:${NC}"
echo -e "  cd ${DEPLOY_PATH} && docker-compose ps"
