package com.example.service;

import com.example.dto.request.NotificationEvent;
import com.example.dto.request.NotificationRequest;
import com.example.dto.response.NotificationResponse;
import com.example.dto.response.PageResponse;
import com.example.entity.Notification;
import com.example.entity.Notification.NotificationStatus;
import com.example.entity.Notification.NotificationType;
import com.example.exception.NotFoundException;
import com.example.mapper.NotificationMapper;
import com.example.repository.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    @Inject NotificationRepository repo;
    @Inject NotificationMapper mapper;
    @Inject EmailService emailService;
    @Inject SmsService smsService;

    @ConfigProperty(name = "notification.max-retry", defaultValue = "3")
    int maxRetry;

    // ── Tạo thủ công qua API ─────────────────────────────────────────────────

    @Transactional
    public NotificationResponse create(NotificationRequest req) {
        Notification n = Notification.create(
                req.getUserId(), req.getType(), req.getTitle(), req.getContent());
        repo.persist(n);
        send(n);
        LOG.infof("Notification created: id=%s userId=%s type=%s status=%s",
                n.getId(), n.getUserId(), n.getType(), n.getStatus());
        return mapper.toResponse(n);
    }

    // ── Xử lý event từ Kafka ─────────────────────────────────────────────────

    @Transactional
    public void processEvent(NotificationEvent event) {
        LOG.infof("Processing event: type=%s userId=%s email=%s",
                event.getType(), event.getUserId(), event.getRecipientEmail());

        if (event.getRecipientEmail() == null || event.getRecipientEmail().isBlank()) {
            LOG.warnf("No recipientEmail for event type=%s userId=%s — skipping",
                    event.getType(), event.getUserId());
            return;
        }

        String title   = buildTitle(event.getType(), event.getData());
        String content = buildContent(event.getType(), event.getData());

        Notification n = Notification.create(
                event.getUserId(), NotificationType.EMAIL, title, content);
        repo.persist(n);

        sendToEmail(n, event.getRecipientEmail());
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public PageResponse<NotificationResponse> getByUser(String userId, int page, int size) {
        List<NotificationResponse> content = Notification.findByUserId(userId, page, size)
                .stream().map(mapper::toResponse).toList();
        long total = Notification.countByUserId(userId);
        return buildPage(content, page, size, total);
    }

    public PageResponse<NotificationResponse> getAll(int page, int size) {
        List<NotificationResponse> content = Notification.findAllPaged(page, size)
                .stream().map(mapper::toResponse).toList();
        long total = Notification.countAll();
        return buildPage(content, page, size, total);
    }

    public NotificationResponse getById(String id) {
        Notification n = repo.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
        return mapper.toResponse(n);
    }

    // ── Retry FAILED notifications ────────────────────────────────────────────

    @Transactional
    public void retryFailed() {
        List<Notification> failed = Notification.findFailed(maxRetry);
        if (failed.isEmpty()) return;
        LOG.infof("Retrying %d failed notifications...", failed.size());
        failed.forEach(this::send);
    }

    // ── Internal send ─────────────────────────────────────────────────────────

    private void send(Notification n) {
        try {
            switch (n.getType()) {
                case EMAIL -> emailService.send(n.getUserId(), n.getTitle(), n.getContent());
                case SMS   -> smsService.send(n.getUserId(), n.getContent());
            }
            n.setStatus(NotificationStatus.SENT);
            LOG.infof("Notification sent: id=%s", n.getId());
        } catch (Exception e) {
            n.setRetryCount(n.getRetryCount() + 1);
            n.setStatus(NotificationStatus.FAILED);
            LOG.warnf("Notification send failed (retry=%d): id=%s error=%s",
                    n.getRetryCount(), n.getId(), e.getMessage());
        }
    }

    private void sendToEmail(Notification n, String recipientEmail) {
        try {
            emailService.send(recipientEmail, n.getTitle(), n.getContent());
            n.setStatus(NotificationStatus.SENT);
            LOG.infof("Email sent to %s: id=%s", recipientEmail, n.getId());
        } catch (Exception e) {
            n.setRetryCount(n.getRetryCount() + 1);
            n.setStatus(NotificationStatus.FAILED);
            LOG.warnf("Email send failed (retry=%d): id=%s error=%s",
                    n.getRetryCount(), n.getId(), e.getMessage());
        }
    }

    // ── Build title ───────────────────────────────────────────────────────────

    private String buildTitle(String type, Map<String, Object> data) {
        return switch (type != null ? type : "") {
            case "USER_REGISTERED"      -> "Chào mừng bạn đến với Bakery Shop";
            case "ORDER_CREATED"        -> "Đặt hàng thành công - Đơn #" + get(data, "orderId");
            case "ORDER_PAID"           -> "Thanh toán thành công - Đơn #" + get(data, "orderId");
            case "PAYMENT_FAILED"       -> "Thanh toán thất bại - Đơn #" + get(data, "orderId");
            case "ORDER_STATUS_UPDATED" -> "Cập nhật đơn hàng #" + get(data, "orderId");
            case "ORDER_CANCELLED"      -> "Đơn hàng #" + get(data, "orderId") + " đã bị huỷ";
            default                     -> "Thông báo từ Bakery Shop";
        };
    }

    // ── Build content ─────────────────────────────────────────────────────────

    private String buildContent(String type, Map<String, Object> data) {
        return switch (type != null ? type : "") {

            case "USER_REGISTERED" -> """
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Tài khoản của bạn đã được tạo thành công trên <strong>Bakery Shop</strong>.</p>
                    <p>Chúc bạn có những trải nghiệm mua sắm tuyệt vời!</p>
                    """.formatted(get(data, "fullName"));

            case "ORDER_CREATED" -> """
                    <p>Cảm ơn bạn đã đặt hàng tại <strong>Bakery Shop</strong>!</p>
                    <table width="100%%" style="border-collapse:collapse;margin:12px 0;">
                      <tr style="background:#fff7ed;">
                        <td style="padding:8px 12px;color:#888;width:40%%;">Mã đơn hàng</td>
                        <td style="padding:8px 12px;font-weight:600;color:#1a1a1a;">#%s</td>
                      </tr>
                      <tr>
                        <td style="padding:8px 12px;color:#888;">Tổng tiền</td>
                        <td style="padding:8px 12px;font-weight:600;color:#f97316;">%s VNĐ</td>
                      </tr>
                      <tr style="background:#fff7ed;">
                        <td style="padding:8px 12px;color:#888;">Phương thức thanh toán</td>
                        <td style="padding:8px 12px;font-weight:600;color:#1a1a1a;">%s</td>
                      </tr>
                    </table>
                    <p>Chúng tôi sẽ xử lý đơn hàng của bạn sớm nhất có thể.</p>
                    """.formatted(get(data, "orderId"), get(data, "totalPrice"), get(data, "paymentMethod"));

            case "ORDER_PAID" -> """
                    <p>Thanh toán của bạn đã được xác nhận thành công!</p>
                    <table width="100%%" style="border-collapse:collapse;margin:12px 0;">
                      <tr style="background:#fff7ed;">
                        <td style="padding:8px 12px;color:#888;width:40%%;">Mã đơn hàng</td>
                        <td style="padding:8px 12px;font-weight:600;color:#1a1a1a;">#%s</td>
                      </tr>
                      <tr>
                        <td style="padding:8px 12px;color:#888;">Số tiền</td>
                        <td style="padding:8px 12px;font-weight:600;color:#f97316;">%s VNĐ</td>
                      </tr>
                      <tr style="background:#fff7ed;">
                        <td style="padding:8px 12px;color:#888;">Mã giao dịch</td>
                        <td style="padding:8px 12px;font-weight:600;color:#1a1a1a;">%s</td>
                      </tr>
                    </table>
                    <p>Đơn hàng của bạn đang được chuẩn bị và sẽ sớm được giao đến bạn.</p>
                    """.formatted(get(data, "orderId"), get(data, "totalPrice"), get(data, "transactionId"));

            case "PAYMENT_FAILED" -> """
                    <p>Rất tiếc, thanh toán cho đơn hàng <strong>#%s</strong> không thành công.</p>
                    <p>Vui lòng kiểm tra lại thông tin thanh toán và thử lại, hoặc chọn phương thức thanh toán khác.</p>
                    <p>Nếu cần hỗ trợ, vui lòng liên hệ với chúng tôi qua
                       <a href="mailto:support@bakery-shop.com" style="color:#f97316;">support@bakery-shop.com</a>.
                    </p>
                    """.formatted(get(data, "orderId"));

            case "ORDER_STATUS_UPDATED" -> """
                    <p>Đơn hàng của bạn vừa được cập nhật trạng thái mới.</p>
                    <table width="100%%" style="border-collapse:collapse;margin:12px 0;">
                      <tr style="background:#fff7ed;">
                        <td style="padding:8px 12px;color:#888;width:40%%;">Mã đơn hàng</td>
                        <td style="padding:8px 12px;font-weight:600;color:#1a1a1a;">#%s</td>
                      </tr>
                      <tr>
                        <td style="padding:8px 12px;color:#888;">Trạng thái mới</td>
                        <td style="padding:8px 12px;">
                          <span style="background:#f97316;color:#fff;padding:4px 12px;
                                       border-radius:20px;font-size:13px;font-weight:600;">
                            %s
                          </span>
                        </td>
                      </tr>
                    </table>
                    <p>Cảm ơn bạn đã tin tưởng Bakery Shop!</p>
                    """.formatted(get(data, "orderId"), translateStatus(get(data, "newStatus")));

            case "ORDER_CANCELLED" -> """
                    <p>Đơn hàng <strong>#%s</strong> đã bị huỷ.</p>
                    <p>Nếu bạn không yêu cầu huỷ đơn hoặc cần hỗ trợ hoàn tiền, vui lòng liên hệ với chúng tôi:</p>
                    <ul style="color:#444;line-height:1.8;">
                      <li>Email: <a href="mailto:support@bakery-shop.com" style="color:#f97316;">support@bakery-shop.com</a></li>
                      <li>Hotline: 1800-BAKERY (miễn phí)</li>
                    </ul>
                    <p>Chúng tôi luôn sẵn sàng hỗ trợ bạn.</p>
                    """.formatted(get(data, "orderId"));

            default -> "<p>Bạn có thông báo mới từ <strong>Bakery Shop</strong>. Vui lòng đăng nhập để xem chi tiết.</p>";
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String translateStatus(String status) {
        return switch (status != null ? status : "") {
            case "PENDING"   -> "Chờ xác nhận";
            case "CONFIRMED" -> "Đã xác nhận";
            case "SHIPPING"  -> "Đang giao hàng";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã huỷ";
            default          -> status;
        };
    }

    private String get(Map<String, Object> data, String key) {
        if (data == null) return "";
        Object val = data.get(key);
        return val != null ? val.toString() : "";
    }

    private PageResponse<NotificationResponse> buildPage(
            List<NotificationResponse> content, int page, int size, long total) {
        return PageResponse.<NotificationResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages((int) Math.ceil((double) total / size))
                .build();
    }
}