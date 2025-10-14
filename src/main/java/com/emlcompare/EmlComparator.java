package com.emlcompare;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EmlComparator {

    public static void main(String[] args) {
        // Hardcoded file paths
        String file1Path = "Factura Hidroelectrica FX-25107863124 a fost generata.eml";
        String file2Path = "Factura Hidroelectrica FX-25108508638 a fost generata.eml";
        String outputPath = "comparison-report.html";

        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║       EML Comparison Tool                              ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();

        try {
            // Parse EML files
            System.out.println("[1/5] Parsing first EML file: " + file1Path);
            File file1 = new File(file1Path);
            if (!file1.exists()) {
                System.err.println("Error: File not found - " + file1Path);
                System.exit(1);
            }
            EmlParser.EmailData email1 = EmlParser.parseEml(file1);
            System.out.println("      ✓ Found " + email1.getPdfAttachments().size() + " PDF attachment(s)");

            System.out.println();
            System.out.println("[2/5] Parsing second EML file: " + file2Path);
            File file2 = new File(file2Path);
            if (!file2.exists()) {
                System.err.println("Error: File not found - " + file2Path);
                System.exit(1);
            }
            EmlParser.EmailData email2 = EmlParser.parseEml(file2);
            System.out.println("      ✓ Found " + email2.getPdfAttachments().size() + " PDF attachment(s)");

            // Compare email bodies
            System.out.println();
            System.out.println("[3/5] Comparing email bodies...");
            String body1 = email1.getTextBody() != null ? email1.getTextBody() :
                          (email1.getHtmlBody() != null ? stripHtml(email1.getHtmlBody()) : "");
            String body2 = email2.getTextBody() != null ? email2.getTextBody() :
                          (email2.getHtmlBody() != null ? stripHtml(email2.getHtmlBody()) : "");

            InlineDiffGenerator.InlineDiffResult bodyDiff = InlineDiffGenerator.generateInlineDiff(body1, body2);
            System.out.println("      ✓ " + (bodyDiff.hasDifferences() ? "Differences found" : "No differences") + " in email body");

            // Extract and compare PDFs
            System.out.println();
            System.out.println("[4/5] Extracting and comparing PDF attachments...");
            List<InlineDiffGenerator.InlineDiffResult> pdfDiffs = new ArrayList<>();

            int pdfCount = Math.min(email1.getPdfAttachments().size(), email2.getPdfAttachments().size());
            for (int i = 0; i < pdfCount; i++) {
                System.out.println("      Processing PDF #" + (i + 1) + ": " + email1.getPdfNames().get(i));
                String pdf1Text = PdfExtractor.extractText(email1.getPdfAttachments().get(i));
                String pdf2Text = PdfExtractor.extractText(email2.getPdfAttachments().get(i));

                InlineDiffGenerator.InlineDiffResult pdfDiff = InlineDiffGenerator.generateInlineDiff(pdf1Text, pdf2Text);
                pdfDiffs.add(pdfDiff);
                System.out.println("      ✓ " + (pdfDiff.hasDifferences() ? "Differences found" : "No differences"));
            }

            if (email1.getPdfAttachments().size() != email2.getPdfAttachments().size()) {
                System.out.println("      ⚠ Warning: Different number of PDF attachments (" +
                                 email1.getPdfAttachments().size() + " vs " +
                                 email2.getPdfAttachments().size() + ")");
            }

            // Generate HTML report
            System.out.println();
            System.out.println("[5/5] Generating HTML report: " + outputPath);
            InlineHtmlReportGenerator.generateReport(outputPath, bodyDiff, pdfDiffs,
                                             file1.getName(), file2.getName());
            System.out.println("      ✓ Report generated successfully");

            // Summary
            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║ Comparison Summary                                     ║");
            System.out.println("╠════════════════════════════════════════════════════════╣");
            System.out.printf("║ Email body has differences:  %-25s ║%n", bodyDiff.hasDifferences() ? "Yes" : "No");
            System.out.printf("║ PDF attachments compared:    %-25d ║%n", pdfDiffs.size());
            long pdfWithDiffs = pdfDiffs.stream().filter(InlineDiffGenerator.InlineDiffResult::hasDifferences).count();
            System.out.printf("║ PDFs with differences:       %-25d ║%n", pdfWithDiffs);
            System.out.println("╚════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("✓ Done! Open '" + outputPath + "' in your browser to view the detailed comparison.");

        } catch (Exception e) {
            System.err.println();
            System.err.println("╔════════════════════════════════════════════════════════╗");
            System.err.println("║ ERROR                                                  ║");
            System.err.println("╚════════════════════════════════════════════════════════╝");
            System.err.println("An error occurred during comparison:");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        // Simple HTML stripping - removes tags but keeps content
        return html.replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ").trim();
    }
}
