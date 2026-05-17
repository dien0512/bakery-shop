package com.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@ApplicationScoped
public class VnpayService {

    private static final Logger LOG = Logger.getLogger(VnpayService.class);

    @ConfigProperty(name = "vnpay.tmn-code")
    String tmnCode;

    @ConfigProperty(name = "vnpay.hash-secret")
    String hashSecret;

    @ConfigProperty(name = "vnpay.url")
    String vnpayUrl;

    @ConfigProperty(name = "vnpay.return-url")
    String returnUrl;

    /**
     * Tạo URL redirect sang trang thanh toán VNPAY
     */
    public String createPaymentUrl(String orderId, long amountVnd, String orderInfo, String ipAddress) {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version",    "2.1.0");
        params.put("vnp_Command",    "pay");
        params.put("vnp_TmnCode",    tmnCode);
        params.put("vnp_Amount",     String.valueOf(amountVnd * 100)); // VNPAY tính đơn vị x100
        params.put("vnp_CurrCode",   "VND");
        params.put("vnp_TxnRef",     orderId);
        params.put("vnp_OrderInfo",  orderInfo);
        params.put("vnp_OrderType",  "other");
        params.put("vnp_Locale",     "vn");
        params.put("vnp_ReturnUrl",  returnUrl);
        params.put("vnp_IpAddr",     ipAddress);
        params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        String queryString = buildQueryString(params);
        String secureHash  = hmacSha512(hashSecret, queryString);

        return vnpayUrl + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Xác thực chữ ký từ VNPAY callback (return URL hoặc IPN)
     * @return true nếu chữ ký hợp lệ
     */
    public boolean verifySignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) return false;

        // Loại bỏ các field không tham gia ký
        Map<String, String> signParams = new TreeMap<>(params);
        signParams.remove("vnp_SecureHash");
        signParams.remove("vnp_SecureHashType");

        String queryString   = buildQueryString(signParams);
        String expectedHash  = hmacSha512(hashSecret, queryString);

        boolean valid = expectedHash.equalsIgnoreCase(receivedHash);
        if (!valid) LOG.warnf("VNPAY signature mismatch. Expected: %s, Got: %s", expectedHash, receivedHash);
        return valid;
    }

    /**
     * Kiểm tra giao dịch có thành công không
     * vnp_ResponseCode = "00" là thành công
     */
    public boolean isPaymentSuccess(Map<String, String> params) {
        return "00".equals(params.get("vnp_ResponseCode"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                if (!sb.isEmpty()) sb.append('&');
                sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            }
        });
        return sb.toString();
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("VNPAY HMAC-SHA512 error", e);
        }
    }
}