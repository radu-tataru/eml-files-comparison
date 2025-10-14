package com.emlcompare;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InlineHtmlReportGenerator {

    public static void generateReport(String outputPath,
                                     InlineDiffGenerator.InlineDiffResult emailBodyDiff,
                                     List<InlineDiffGenerator.InlineDiffResult> pdfDiffs,
                                     String file1Name,
                                     String file2Name) throws IOException {

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>EML Comparison Report</title>\n");
        html.append("    <style>\n");
        html.append(getStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("    <div class=\"header\">\n");
        html.append("        <h1>EML Comparison Report</h1>\n");
        html.append("        <p class=\"timestamp\">Generated: ")
            .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .append("</p>\n");
        html.append("    </div>\n");

        // File names
        html.append("    <div class=\"file-info\">\n");
        html.append("        <div class=\"file-box original\">").append(escapeHtml(file1Name)).append("</div>\n");
        html.append("        <div class=\"vs\">vs</div>\n");
        html.append("        <div class=\"file-box revised\">").append(escapeHtml(file2Name)).append("</div>\n");
        html.append("    </div>\n");

        // Summary
        html.append("    <div class=\"summary\">\n");
        html.append("        <h2>Summary</h2>\n");
        html.append("        <p>Email body has differences: <strong>").append(emailBodyDiff.hasDifferences() ? "Yes" : "No").append("</strong></p>\n");
        int pdfWithDiffs = (int) pdfDiffs.stream().filter(InlineDiffGenerator.InlineDiffResult::hasDifferences).count();
        html.append("        <p>PDF attachments compared: <strong>").append(pdfDiffs.size()).append("</strong></p>\n");
        html.append("        <p>PDFs with differences: <strong>").append(pdfWithDiffs).append("</strong></p>\n");
        html.append("    </div>\n");

        // Email body diff
        html.append("    <div class=\"section\">\n");
        html.append("        <h2>Email Body Comparison</h2>\n");
        if (emailBodyDiff.hasDifferences()) {
            html.append("        <div class=\"diff-content\">\n");
            html.append(generateInlineDiffHtml(emailBodyDiff));
            html.append("        </div>\n");
        } else {
            html.append("        <p class=\"no-diff\">✓ No differences found in email body</p>\n");
        }
        html.append("    </div>\n");

        // PDF diffs
        for (int i = 0; i < pdfDiffs.size(); i++) {
            html.append("    <div class=\"section\">\n");
            html.append("        <h2>PDF Attachment Comparison #").append(i + 1).append("</h2>\n");
            InlineDiffGenerator.InlineDiffResult pdfDiff = pdfDiffs.get(i);
            if (pdfDiff.hasDifferences()) {
                html.append("        <div class=\"diff-content\">\n");
                html.append(generateInlineDiffHtml(pdfDiff));
                html.append("        </div>\n");
            } else {
                html.append("        <p class=\"no-diff\">✓ No differences found in PDF attachment</p>\n");
            }
            html.append("    </div>\n");
        }

        html.append("</body>\n");
        html.append("</html>");

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(html.toString());
        }
    }

    private static String generateInlineDiffHtml(InlineDiffGenerator.InlineDiffResult diffResult) {
        StringBuilder html = new StringBuilder();

        for (InlineDiffGenerator.DiffSegment segment : diffResult.getSegments()) {
            String escapedText = escapeHtml(segment.getText());

            switch (segment.getType()) {
                case UNCHANGED:
                    html.append("<span class=\"unchanged\">").append(escapedText).append("</span>");
                    break;
                case DELETED:
                    html.append("<span class=\"deleted\">").append(escapedText).append("</span>");
                    break;
                case INSERTED:
                    html.append("<span class=\"inserted\">").append(escapedText).append("</span>");
                    break;
            }
        }

        return html.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;")
                   .replace("\n", "<br>\n");
    }

    private static String getStyles() {
        return """
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
            background-color: #ffffff;
            color: #24292f;
            line-height: 1.6;
            padding: 20px;
            max-width: 1400px;
            margin: 0 auto;
        }

        .header {
            text-align: center;
            padding: 30px 0;
            border-bottom: 2px solid #d0d7de;
            margin-bottom: 30px;
        }

        .header h1 {
            color: #0969da;
            font-size: 2em;
            margin-bottom: 10px;
        }

        .timestamp {
            color: #57606a;
            font-size: 0.9em;
        }

        .file-info {
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 20px;
            margin-bottom: 30px;
            flex-wrap: wrap;
        }

        .file-box {
            background-color: #f6f8fa;
            border: 1px solid #d0d7de;
            padding: 15px 25px;
            border-radius: 6px;
            font-family: 'Courier New', monospace;
            font-size: 0.9em;
            max-width: 500px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .file-box.original {
            border-left: 4px solid #cf222e;
        }

        .file-box.revised {
            border-left: 4px solid #1a7f37;
        }

        .vs {
            color: #57606a;
            font-weight: bold;
            font-size: 1.2em;
        }

        .summary {
            background-color: #f6f8fa;
            border: 1px solid #d0d7de;
            padding: 20px;
            border-radius: 6px;
            margin-bottom: 30px;
        }

        .summary h2 {
            color: #0969da;
            margin-bottom: 15px;
            font-size: 1.3em;
        }

        .summary p {
            margin: 8px 0;
            color: #24292f;
            font-size: 1.05em;
        }

        .summary strong {
            color: #0969da;
        }

        .section {
            margin-bottom: 40px;
        }

        .section h2 {
            color: #0969da;
            margin-bottom: 15px;
            font-size: 1.3em;
            padding-bottom: 10px;
            border-bottom: 2px solid #d0d7de;
        }

        .no-diff {
            background-color: #dafbe1;
            border: 1px solid #1a7f37;
            padding: 20px;
            border-radius: 6px;
            color: #1a7f37;
            text-align: center;
            font-size: 1.1em;
            font-weight: 500;
        }

        .diff-content {
            background-color: #ffffff;
            border: 1px solid #d0d7de;
            padding: 20px;
            border-radius: 6px;
            font-family: 'Courier New', 'Courier', monospace;
            font-size: 14px;
            line-height: 1.8;
            white-space: pre-wrap;
            word-wrap: break-word;
            overflow-x: auto;
        }

        .diff-content .unchanged {
            color: #24292f;
        }

        .diff-content .deleted {
            background-color: #ffebe9;
            color: #cf222e;
            text-decoration: line-through;
            padding: 3px 6px;
            margin: 0 2px;
            border-radius: 4px;
            border: 1px solid #ffcccb;
            font-weight: 600;
            display: inline-block;
        }

        .diff-content .inserted {
            background-color: #dafbe1;
            color: #1a7f37;
            padding: 3px 6px;
            margin: 0 2px;
            border-radius: 4px;
            border: 1px solid #a8ddb5;
            font-weight: 600;
            display: inline-block;
        }

        @media (max-width: 768px) {
            body {
                padding: 10px;
            }

            .header h1 {
                font-size: 1.5em;
            }

            .file-info {
                flex-direction: column;
            }

            .file-box {
                max-width: 100%;
            }

            .diff-content {
                font-size: 12px;
                padding: 15px;
            }
        }

        @media print {
            body {
                background-color: white;
            }

            .diff-content .deleted {
                background-color: #ffebe9 !important;
                -webkit-print-color-adjust: exact;
                print-color-adjust: exact;
            }

            .diff-content .inserted {
                background-color: #dafbe1 !important;
                -webkit-print-color-adjust: exact;
                print-color-adjust: exact;
            }
        }
        """;
    }
}
