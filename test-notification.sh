#!/bin/bash
# ============================================================
# test-notification.sh
# Script test notification-service thủ công
# Chạy sau khi: ./run.sh hoặc docker compose up -d
# ============================================================

KAFKA_CONTAINER="notification-kafka"
# Dùng localhost:9092 vì lệnh chạy từ host machine
BOOTSTRAP="localhost:9092"

echo ""
echo "========================================"
echo "  NOTIFICATION SERVICE - TEST SCRIPT"
echo "========================================"

# ── 1. Kiểm tra Kafka đang chạy ─────────────────────────────
echo ""
echo "[1] Kiểm tra Kafka..."
docker exec $KAFKA_CONTAINER kafka-broker-api-versions \
  --bootstrap-server $BOOTSTRAP > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "❌ Kafka chưa chạy. Hãy chạy: docker compose up -d"
  exit 1
fi
echo "✅ Kafka đang chạy"

# ── 2. Tạo topic nếu chưa có ────────────────────────────────
echo ""
echo "[2] Tạo Kafka topics..."

docker exec $KAFKA_CONTAINER kafka-topics \
  --bootstrap-server $BOOTSTRAP \
  --create --if-not-exists \
  --topic notification-events \
  --partitions 1 --replication-factor 1
echo "✅ Topic 'notification-events' ready"

docker exec $KAFKA_CONTAINER kafka-topics \
  --bootstrap-server $BOOTSTRAP \
  --create --if-not-exists \
  --topic notification-retry-trigger \
  --partitions 1 --replication-factor 1
echo "✅ Topic 'notification-retry-trigger' ready"

# ── 3. Gửi test event: ORDER_CREATED ────────────────────────
echo ""
echo "[3] Gửi event ORDER_CREATED (type EMAIL)..."

EVENT_ORDER='{"userId":"user-001","type":"EMAIL","title":"Dat hang thanh cong","content":"Don hang #ORD-2024-001 da duoc xac nhan.","eventType":"ORDER_CREATED"}'

echo "$EVENT_ORDER" | docker exec -i $KAFKA_CONTAINER \
  kafka-console-producer \
  --bootstrap-server $BOOTSTRAP \
  --topic notification-events

echo "✅ Event ORDER_CREATED đã gửi"

# ── 4. Gửi test event: PAYMENT_SUCCESS ──────────────────────
echo ""
echo "[4] Gửi event PAYMENT_SUCCESS (type EMAIL)..."

EVENT_PAYMENT='{"userId":"user-002","type":"EMAIL","title":"Thanh toan thanh cong","content":"Don hang #ORD-2024-002 da duoc thanh toan 600000d qua VNPAY.","eventType":"PAYMENT_SUCCESS"}'

echo "$EVENT_PAYMENT" | docker exec -i $KAFKA_CONTAINER \
  kafka-console-producer \
  --bootstrap-server $BOOTSTRAP \
  --topic notification-events

echo "✅ Event PAYMENT_SUCCESS đã gửi"

# ── 5. Gửi test event: ORDER_SHIPPING (SMS) ──────────────────
echo ""
echo "[5] Gửi event ORDER_SHIPPING (type SMS)..."

EVENT_SMS='{"userId":"user-003","type":"SMS","title":"Don hang dang giao","content":"Don hang #ORD-2024-003 dang duoc giao. Du kien: hom nay.","eventType":"ORDER_SHIPPING"}'

echo "$EVENT_SMS" | docker exec -i $KAFKA_CONTAINER \
  kafka-console-producer \
  --bootstrap-server $BOOTSTRAP \
  --topic notification-events

echo "✅ Event ORDER_SHIPPING (SMS) đã gửi"

# ── 6. Trigger retry ─────────────────────────────────────────
echo ""
echo "[6] Trigger retry các notification FAILED..."

echo "trigger" | docker exec -i $KAFKA_CONTAINER \
  kafka-console-producer \
  --bootstrap-server $BOOTSTRAP \
  --topic notification-retry-trigger

echo "✅ Retry trigger đã gửi"

# ── 7. Kiểm tra DB ───────────────────────────────────────────
echo ""
echo "[7] Kiểm tra DB (notifications table)..."
sleep 3

docker exec notification-mysql mysql \
  -uroot -pchangeme notification_db \
  -e "SELECT id, user_id, type, title, status, retry_count, created_at FROM notifications ORDER BY created_at DESC LIMIT 10;" \
  2>/dev/null

echo ""
echo "========================================"
echo "  TEST XONG!"
echo "  Kafka UI : http://localhost:8090"
echo "  Service  : http://localhost:8084"
echo "========================================"