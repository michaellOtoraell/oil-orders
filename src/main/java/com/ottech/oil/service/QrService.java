package com.ottech.oil.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ottech.oil.entity.Customer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class QrService {

    @Value("${app.base-url}")
    private String baseUrl;

    public String customerUrl(Customer c) {
        return baseUrl.replaceAll("/+$", "") + "/c/" + c.getToken();
    }

    public byte[] generatePng(Customer c) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(customerUrl(c), BarcodeFormat.QR_CODE, 800, 800);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}
