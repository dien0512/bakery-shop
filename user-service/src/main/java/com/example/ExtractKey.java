package com.example;

import java.io.*;
import java.nio.file.*;
import java.security.cert.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Base64;

public class ExtractKey {
    public static void main(String[] args) throws Exception {
        String certPem = new String(Files.readAllBytes(Paths.get("cert.pem")));
        certPem = certPem.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] certBytes = Base64.getDecoder().decode(certPem);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(new ByteArrayInputStream(certBytes));
        PublicKey pub = cert.getPublicKey();
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(pub.getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----\n";
        Files.write(Paths.get("src/main/resources/publicKey.pem"), pem.getBytes());
        System.out.println("Done! publicKey.pem updated.");
    }
}
