package com.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * SMS Gateway service.
 * Hiện tại chỉ log để demo — tích hợp Twilio / Viettel SMS sau.
 *
 * Để tích hợp Twilio:
 *   1. Thêm dependency twilio vào pom.xml
 *   2. Config: sms.twilio.account-sid, sms.twilio.auth-token, sms.twilio.from-number
 *   3. Implement send() dùng Twilio SDK
 */
@ApplicationScoped
public class SmsService {

    private static final Logger LOG = Logger.getLogger(SmsService.class);

    @ConfigProperty(name = "sms.enabled", defaultValue = "false")
    boolean enabled;

    /**
     * Gửi SMS đến số điện thoại.
     *
     * @param toPhone  Số điện thoại người nhận (VD: +84912345678)
     * @param content  Nội dung tin nhắn
     */
    public void send(String toPhone, String content) {
        if (!enabled) {
            LOG.infof("[SMS-DEV] To=%s | Content=%s", toPhone, content);
            return;
        }

        // TODO: tích hợp SMS gateway thực tế tại đây
        // Ví dụ với Twilio:
        // Twilio.init(accountSid, authToken);
        // Message.creator(new PhoneNumber(toPhone), new PhoneNumber(fromNumber), content).create();

        LOG.infof("[SMS] Sent to %s", toPhone);
    }
}
