#!/bin/bash
set -e
HOJ_SRC=/home/ubuntu/hoj-why
DEPLOY_DIR=/root/HOJ-Deploy
STANDALONE=$DEPLOY_DIR/standAlone

echo "=== [1/6] 编译后端 (Maven) ==="
cd $HOJ_SRC/hoj-springboot
MAVEN_SETTINGS=$(mktemp)
trap 'rm -f "$MAVEN_SETTINGS"' EXIT
cat > "$MAVEN_SETTINGS" <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyun-central</id>
      <name>Aliyun Maven Central Mirror</name>
      <url>https://maven.aliyun.com/repository/central</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
mvn -s "$MAVEN_SETTINGS" clean install -DskipTests -T 4

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
