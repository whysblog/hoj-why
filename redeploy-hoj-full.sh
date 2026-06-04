#!/bin/bash
set -e
HOJ_SRC=/home/ubuntu/hoj-why
DEPLOY_DIR=/root/HOJ-Deploy
STANDALONE=$DEPLOY_DIR/standAlone

echo "=== [1/6] 编译后端 (Maven) ==="
cd $HOJ_SRC/hoj-springboot
mvn clean install -DskipTests -T 4

echo "=== [2/6] 复制后端 JAR 到部署目录 ==="
cp -f DataBackup/target/hoj-backend-4.6.jar $DEPLOY_DIR/src/backend/hoj-backend.jar
cp -f JudgeServer/target/hoj-judgeServer-4.6.jar $DEPLOY_DIR/src/judgeserver/hoj-judgeserver.jar
mkdir -p $STANDALONE/backend-jars
cp -f DataBackup/target/hoj-backend-4.6.jar $STANDALONE/backend-jars/
cp -f JudgeServer/target/hoj-judgeServer-4.6.jar $STANDALONE/backend-jars/
cp -f api/target/hoj-api-4.6.jar $STANDALONE/backend-jars/

echo "=== [3/6] 编译前端 (npm) ==="
cd $HOJ_SRC/hoj-vue
npm run build

echo "=== [4/6] 复制前端 dist 到 /root/dist ==="
rm -rf /root/dist/*
cp -r dist/* /root/dist/
cp -r $HOJ_SRC/hoj-scrollBoard/* $DEPLOY_DIR/src/frontend/scrollBoard/

echo "=== [5/6] 重建 Docker 镜像 ==="
cd $STANDALONE
docker-compose build hoj-backend hoj-judgeserver

echo "=== [6/6] 重启所有容器 ==="
docker-compose up -d

echo ""
echo "=== 部署完成 ==="
docker-compose ps
