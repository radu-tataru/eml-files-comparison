package com.emlcompare;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class PdfExtractor {

    public static String extractText(byte[] pdfData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(pdfData);
             PDDocument document = Loader.loadPDF(pdfData)) {

            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public static String extractTextWithLineNumbers(byte[] pdfData) throws IOException {
        String text = extractText(pdfData);
        String[] lines = text.split("\n");
        StringBuilder numbered = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            numbered.append(String.format("%4d: %s\n", i + 1, lines[i]));
        }

        return numbered.toString();
    }
}
