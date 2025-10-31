package com.resumeai.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class PDFService {

    private static final Logger logger =  LoggerFactory.getLogger(PDFService.class);

    public String extractTextFromPDF(MultipartFile pdfFile) throws IOException {
        String text = "";
        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            if (document.isEncrypted()) {
                document.setAllSecurityToBeRemoved(true);
            }
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(document).trim();
        } catch (IOException e) {
            logger.warn("PDF parsing failed for {}: {} => falling back to OCR.", pdfFile.getOriginalFilename(), e.getMessage());
        }
        if (text.isBlank()) {
            text = performOCR(pdfFile);
        }
        return cleanText(text);
    }

    private String performOCR(MultipartFile file) {
        try {
            ITesseract tesseract = new Tesseract();
            // set datapath if needed: tessdata folder
            BufferedImage image = ImageIO.read(file.getInputStream());
            String ocrResult = tesseract.doOCR(image);
            return ocrResult != null ? ocrResult.trim() : "";
        } catch (Exception ex) {
            logger.error("OCR failed for file {}: {}", file.getOriginalFilename(), ex.getMessage());
            return "";
        }
    }

    private String cleanText(String raw) {
        String cleaned = raw
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return cleaned.length() > 0 ? cleaned : "";
    }

    public void validatePDFFile(MultipartFile pdfFile) {
        if (pdfFile == null || pdfFile.isEmpty()) throw new IllegalArgumentException("PDF required");
        if (!pdfFile.getOriginalFilename().toLowerCase().endsWith(".pdf"))
            throw new IllegalArgumentException("Only PDF allowed");
        if (pdfFile.getSize() > 10 * 1024 * 1024L) throw new IllegalArgumentException("Max size 10MB");
    }
}
