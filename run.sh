#!/bin/bash
# ============================================================
# run.sh — Build JAR + Docker image rồi khởi động toàn bộ
# Chạy từ thư mục chứa docker-compose.yml
# ============================================================

set -e

PROJECT_DIR="notification-service/notification-service"
IMAGE_NAME="quarkus/notification-service-jvm"

echo ""
echo "========================================"
echo "  NOTIFICATION SERVICE - BUILD & RUN"
echo "========================================"

# ── Bước 1: Build JAR ────────────────────────────────────────
echo ""
echo "[1/3] Building JAR (Maven)..."
cd "$PROJECT_DIR"
./mvnw package -DskipTests -q
echo "✅ JAR build thành công"

# ── Bước 2: Build Docker image ───────────────────────────────
echo ""
echo "[2/3] Building Docker image '$IMAGE_NAME'..."
docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE_NAME" . -q
echo "✅ Docker image build thành công"

# ── Bước 3: Khởi động Docker Compose ────────────────────────
cd ../..
echo ""
echo "[3/3] Starting Docker Compose..."
docker compose down --remove-orphans 2>/dev/null || true
docker compose up -d

echo ""
echo "========================================"
echo "  Đang chờ service sẵn sàng..."
echo "========================================"

# Chờ notification-service healthy
for i in {1..30}; do
  STATUS=$(docker inspect --format='{{.State.Status}}' notification-service 2>/dev/null || echo "not_found")
  if [ "$STATUS" = "running" ]; then
    sleep 3
    HTTP=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8084/q/health 2>/dev/null || echo "000")
    if [ "$HTTP" = "200" ]; then
      echo ""
      echo "✅ notification-service đã sẵn sàng!"
      break
    fi
  fi
  echo -n "."
  sleep 3
done

echo ""
echo "========================================"
echo "  XONG! Truy cập:"
echo "  • Service    : http://localhost:8084"
echo "  • Health     : http://localhost:8084/q/health"
echo "  • Kafka UI   : http://localhost:8090"
echo "  • MySQL      : localhost:3307 (root/changeme)"
echo "========================================"
echo ""
echo "  Xem log service:"
echo "  docker logs -f notification-service"
echo ""
echo "  Chạy test:"
echo "  ./test-notification.sh"
echo "========================================"