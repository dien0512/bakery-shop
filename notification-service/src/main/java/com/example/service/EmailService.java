package com.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;

@ApplicationScoped
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class);

    @ConfigProperty(name = "mail.smtp.host", defaultValue = "smtp.gmail.com")
    String smtpHost;

    @ConfigProperty(name = "mail.smtp.port", defaultValue = "587")
    int smtpPort;

    @ConfigProperty(name = "mail.smtp.username", defaultValue = "dien05122005@gmail.com")
    String username;

    @ConfigProperty(name = "mail.smtp.password", defaultValue = "samd etnj xeki ufde")
    String password;

    @ConfigProperty(name = "mail.smtp.from", defaultValue = "no-reply@bakery-shop.com")
    String fromAddress;

    @ConfigProperty(name = "mail.enabled", defaultValue = "false")
    boolean enabled;

    public void send(String toEmail, String subject, String body) {
        if (!enabled) {
            LOG.infof("[EMAIL-DEV] To=%s | Subject=%s | Body=%s", toEmail, subject, body);
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress, "Bakery Shop", "UTF-8"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject, "UTF-8");

            // HTML body
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(wrapHtml(subject, body), "text/html; charset=UTF-8");

            MimeMultipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(htmlPart);
            message.setContent(multipart);

            Transport.send(message);
            LOG.infof("[EMAIL] Sent to %s | Subject: %s", toEmail, subject);
        } catch (Exception e) {
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }

    /**
     * Bọc nội dung vào template HTML đẹp kiểu Shopee/Tiki
     */
    private String wrapHtml(String subject, String body) {
        return """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
          <title>%s</title>
        </head>
        <body style="margin:0;padding:0;background:#f5f5f5;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f5f5;padding:32px 0;">
            <tr>
              <td align="center">
                <table width="600" cellpadding="0" cellspacing="0"
                       style="background:#ffffff;border-radius:12px;overflow:hidden;
                              box-shadow:0 2px 12px rgba(0,0,0,0.08);">

                  <!-- HEADER -->
                  <tr>
                    <td style="background:linear-gradient(135deg,#f97316,#ea580c);
                               padding:28px 40px;text-align:center;">
                      <h1 style="margin:0;color:#ffffff;font-size:26px;
                                 font-weight:700;letter-spacing:1px;">
                        Bakery Shop
                      </h1>
                      <p style="margin:6px 0 0;color:rgba(255,255,255,0.85);font-size:13px;">
                        Tiệm bánh ngon mỗi ngày
                      </p>
                    </td>
                  </tr>

                  <!-- BODY -->
                  <tr>
                    <td style="padding:36px 40px;">
                      <h2 style="margin:0 0 16px;color:#1a1a1a;font-size:20px;font-weight:600;">
                        %s
                      </h2>
                      <div style="color:#444444;font-size:15px;line-height:1.7;
                                  background:#fafafa;border-left:4px solid #f97316;
                                  border-radius:0 8px 8px 0;padding:16px 20px;
                                  margin-bottom:24px;">
                        %s
                      </div>
                      <p style="color:#888888;font-size:13px;margin:0;">
                        Nếu bạn có thắc mắc, vui lòng liên hệ chúng tôi qua
                        <a href="mailto:support@bakery-shop.com"
                           style="color:#f97316;text-decoration:none;">
                          support@bakery-shop.com
                        </a>
                      </p>
                    </td>
                  </tr>

                  <!-- DIVIDER -->
                  <tr>
                    <td style="padding:0 40px;">
                      <hr style="border:none;border-top:1px solid #eeeeee;margin:0;"/>
                    </td>
                  </tr>

                  <!-- FOOTER -->
                  <tr>
                    <td style="padding:20px 40px;text-align:center;">
                      <p style="margin:0;color:#aaaaaa;font-size:12px;line-height:1.6;">
                        © 2026 Bakery Shop. Đây là email tự động, vui lòng không trả lời.<br/>
                      </p>
                    </td>
                  </tr>

                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.formatted(subject, subject, body.replace("\n", "<br/>"));
    }
}