# Notification Service

Microservice gửi thông báo **Email / SMS** cho hệ thống **Bakery Shop**.

## Kiến trúc

```
  orders-service ──┐
  user-service  ──►├──► Kafka topic: notification-events
                   │         │
                   │    NotificationConsumer
                   │         │
                   │    NotificationService
                   │     ├── EmailService (SMTP / Jakarta Mail)
                   │     └── SmsService  (placeholder)
                   │         │
                   │    RetryScheduler (@Scheduled mỗi 5 phút)
                   │
                   └──► REST API :8084  (JWT Auth)
                              │
                         MySQL: notification_db
```

## Cấu trúc package

```
com.example/
├── AppLifecycleBean.java
├── consumer/
│   ├── NotificationConsumer.java          # @Incoming("notification-events")
│   └── NotificationEventDeserializer.java # Jackson Kafka deserializer
├── dto/request/
│   ├── NotificationEvent.java             # DTO Kafka event
│   └── NotificationRequest.java          # DTO REST
├── dto/response/
│   ├── NotificationResponse.java
│   └── PageResponse.java
├── entity/Notification.java               # JPA entity + Active Record
├── exception/                             # GlobalExceptionHandler + custom exceptions
├── health/NotificationHealthCheck.java    # /q/health/ready
├── mapper/NotificationMapper.java
├── repository/NotificationRepository.java
├── resource/NotificationResource.java     # REST endpoints
└── service/
    ├── EmailService.java     # SMTP
    ├── NotificationService.java
    ├── RetryScheduler.java   # @Scheduled
    └── SmsService.java       # placeholder
```

## REST API (port 8084)

| Method | Path                             | Role        | Mô tả                     |
|--------|----------------------------------|-------------|---------------------------|
| POST   | /api/notifications               | ADMIN       | Tạo thủ công              |
| GET    | /api/notifications               | ADMIN       | Lấy tất cả (phân trang)   |
| GET    | /api/notifications/{id}          | ADMIN       | Lấy theo ID               |
| GET    | /api/notifications/my            | USER, ADMIN | Lấy của user hiện tại     |
| GET    | /api/notifications/user/{userId} | ADMIN       | Lấy theo userId           |
| POST   | /api/notifications/retry         | ADMIN       | Trigger retry FAILED      |
| GET    | /q/health                        | Public      | Health check              |
| GET    | /swagger-ui                      | Public      | Swagger UI                |

## Chạy local

```bash
# 1. Khởi động MySQL + Kafka
docker compose up -d

# 2. Chạy service (dev mode)
./mvnw quarkus:dev

# Swagger: http://localhost:8084/swagger-ui
# Health:  http://localhost:8084/q/health
```

## Biến môi trường

| Biến                    | Mặc định                            |
|-------------------------|-------------------------------------|
| DB_URL                  | jdbc:mysql://localhost:3306/notification_db |
| DB_USERNAME             | root                                |
| DB_PASSWORD             | changeme                            |
| KAFKA_BOOTSTRAP_SERVERS | localhost:9092                      |
| JWT_ISSUER              | bakery-shop                         |
| MAIL_ENABLED            | false (chỉ log, không gửi thật)    |
| MAIL_USERNAME           | your-email@gmail.com                |
| MAIL_PASSWORD           | your-app-password                   |
| NOTIFICATION_MAX_RETRY  | 3                                   |

## Kafka Event Format

Topic: `notification-events`

```json
{
  "userId": "uuid-user",
  "type": "EMAIL",
  "title": "Thanh toán thành công",
  "content": "Đơn hàng #456 đã được thanh toán.",
  "eventType": "PAYMENT_SUCCESS"
}
```

## Retry Logic

- Gửi thất bại → `status=FAILED`, `retry_count++`
- Scheduler retry mỗi **5 phút** cho các record `FAILED` và `retry_count < maxRetry`
- Sau khi đạt `maxRetry` → giữ `FAILED` để audit
- ADMIN có thể trigger retry thủ công: `POST /api/notifications/retry`
