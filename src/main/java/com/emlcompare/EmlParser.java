package com.emlcompare;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.*;
import java.util.*;

public class EmlParser {

    public static class EmailData {
        private String textBody;
        private String htmlBody;
        private List<byte[]> pdfAttachments;
        private List<String> pdfNames;

        public EmailData() {
            this.pdfAttachments = new ArrayList<>();
            this.pdfNames = new ArrayList<>();
        }

        public String getTextBody() { return textBody; }
        public void setTextBody(String textBody) { this.textBody = textBody; }

        public String getHtmlBody() { return htmlBody; }
        public void setHtmlBody(String htmlBody) { this.htmlBody = htmlBody; }

        public List<byte[]> getPdfAttachments() { return pdfAttachments; }
        public List<String> getPdfNames() { return pdfNames; }

        public void addPdfAttachment(String name, byte[] data) {
            pdfNames.add(name);
            pdfAttachments.add(data);
        }
    }

    public static EmailData parseEml(File emlFile) throws Exception {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props);

        try (InputStream is = new FileInputStream(emlFile)) {
            MimeMessage message = new MimeMessage(session, is);
            EmailData emailData = new EmailData();

            // Extract headers and subject
            StringBuilder emailText = new StringBuilder();
            emailText.append("Subject: ").append(message.getSubject() != null ? message.getSubject() : "").append("\n");
            emailText.append("From: ").append(message.getFrom() != null && message.getFrom().length > 0 ? message.getFrom()[0].toString() : "").append("\n");
            emailText.append("To: ").append(message.getAllRecipients() != null && message.getAllRecipients().length > 0 ? message.getAllRecipients()[0].toString() : "").append("\n");
            emailText.append("Date: ").append(message.getSentDate() != null ? message.getSentDate().toString() : "").append("\n\n");

            Object content = message.getContent();
            processContent(content, emailData);

            // Store original body content before combining with headers
            String originalTextBody = emailData.getTextBody();
            String originalHtmlBody = emailData.getHtmlBody();

            // Combine headers with body (strip HTML if needed)
            String body = "";
            if (originalTextBody != null && !originalTextBody.trim().isEmpty()) {
                body = originalTextBody;
            } else if (originalHtmlBody != null && !originalHtmlBody.trim().isEmpty()) {
                // Strip HTML tags and decode entities
                body = stripHtml(originalHtmlBody);
            }

            // Add separator if body has content
            String fullText = emailText.toString();
            if (body != null && !body.trim().isEmpty()) {
                fullText += "Body:\n" + body;
            }
            emailData.setTextBody(fullText);

            return emailData;
        }
    }

    private static void processContent(Object content, EmailData emailData) throws Exception {
        if (content instanceof String) {
            if (emailData.getTextBody() == null) {
                emailData.setTextBody((String) content);
            }
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            processMimeMultipart(multipart, emailData);
        }
    }

    private static void processMimeMultipart(Multipart multipart, EmailData emailData) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            String disposition = bodyPart.getDisposition();
            String contentType = bodyPart.getContentType().toLowerCase();

            // Handle text/html content first (even if it has inline disposition)
            if (bodyPart.isMimeType("text/html")) {
                if (emailData.getHtmlBody() == null) {
                    String content = bodyPart.getContent().toString();
                    emailData.setHtmlBody(content);
                }
            }
            // Handle text/plain content
            else if (bodyPart.isMimeType("text/plain")) {
                if (emailData.getTextBody() == null) {
                    String content = bodyPart.getContent().toString();
                    emailData.setTextBody(content);
                }
            }
            // Recursively process nested multiparts
            else if (bodyPart.getContent() instanceof Multipart) {
                processMimeMultipart((Multipart) bodyPart.getContent(), emailData);
            }
            // Handle PDF attachments (check after text content)
            else if ((Part.ATTACHMENT.equalsIgnoreCase(disposition) || Part.INLINE.equalsIgnoreCase(disposition))
                     && contentType.contains("pdf")) {
                String fileName = bodyPart.getFileName();
                InputStream is = bodyPart.getInputStream();
                byte[] pdfData = readAllBytes(is);
                emailData.addPdfAttachment(fileName, pdfData);
            }

            // Check for embedded PDFs without disposition
            if (disposition == null && contentType.contains("pdf")) {
                String fileName = bodyPart.getFileName();
                if (fileName != null) {
                    InputStream is = bodyPart.getInputStream();
                    byte[] pdfData = readAllBytes(is);
                    emailData.addPdfAttachment(fileName, pdfData);
                }
            }
        }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        // Remove script and style tags with their content
        String text = html.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        text = text.replaceAll("(?i)<style[^>]*>.*?</style>", "");
        // Remove HTML tags
        text = text.replaceAll("<[^>]+>", "");
        // Decode common HTML entities
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&quot;", "\"");
        text = text.replaceAll("&#39;", "'");
        // Clean up multiple spaces and blank lines
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("(?m)^[ \\t]*\\r?\\n", "");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.trim();
    }
}
