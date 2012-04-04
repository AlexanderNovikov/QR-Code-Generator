package com.qrCodeGenerator;

import com.qrCodeGenerator.util.EncodeHintType;
import com.qrCodeGenerator.util.MultiFormatWriter;
import com.qrCodeGenerator.util.WriterException;
import com.qrCodeGenerator.util.client.j2se.MatrixToImageWriter;
import com.qrCodeGenerator.util.common.BitMatrix;
import com.qrCodeGenerator.util.qrcode.decoder.ErrorCorrectionLevel;
import com.qrCodeGenerator.util.BarcodeFormat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class QRCodeGenerator {

    private static final String DOT = ".";

    public static BitMatrix generateQRCode(String textForGeneratig, int width, int height) {
        MultiFormatWriter writer = new MultiFormatWriter();
        Map<EncodeHintType, ErrorCorrectionLevel> hints = new HashMap<EncodeHintType, ErrorCorrectionLevel>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
        BitMatrix qrCode = null;
        try {
            qrCode = writer.encode(textForGeneratig, BarcodeFormat.QR_CODE, width, height, hints);
        } catch (WriterException e) {e.printStackTrace();}
        return qrCode;
    }
    
    public static void generateQRCodeInImage(BitMatrix qrCode, String fileName, String fileExtension) {
        try {
            MatrixToImageWriter.writeToFile(qrCode, fileExtension, new File(fileName + DOT + fileExtension));
        } catch (IOException e) {e.printStackTrace();}
    }
    
    public static void main(String[] args) {
        String text = args[0];
        int width = Integer.parseInt(args[1]);
        int height = Integer.parseInt(args[2]);
        String fileName = args[3];
        String extension = args[4];
        generateQRCodeInImage(generateQRCode(text, width, height), fileName, extension);
    }
}
