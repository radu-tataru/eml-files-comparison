# EML Files Comparison Tool

A Java application that compares two EML email files (including their PDF attachments) and generates an HTML report showing the differences with inline highlighting.

## Features

- Parse EML files and extract email headers, body, and PDF attachments
- Compare email body text with phrase-level granularity
- Extract and compare text content from PDF attachments
- Generate HTML report with inline highlighted differences:
  - Deleted text: red background with strikethrough
  - Inserted text: green background
- Clean, readable diff output optimized for readability

## Requirements

- Java 17 or higher
- Maven 3.6+

## Dependencies

- Jakarta Mail API 2.1.2 (for EML parsing)
- Eclipse Angus Mail 2.0.2 (Jakarta Mail implementation)
- Apache PDFBox 3.0.0 (for PDF text extraction)
- java-diff-utils 4.12 (for text comparison)
- jsoup 1.16.1 (for HTML processing)

## Building

```bash
mvn clean compile
```

## Running

```bash
mvn exec:java -Dexec.mainClass="com.emlcompare.EmlComparator"
```

## Configuration

The file paths are currently hardcoded in `EmlComparator.java`:

```java
String file1Path = "Factura Hidroelectrica FX-25107863124 a fost generata.eml";
String file2Path = "Factura Hidroelectrica FX-25108508638 a fost generata.eml";
String outputPath = "comparison-report.html";
```

Edit these paths to compare your own EML files.

## Output

The application generates:
- `comparison-report.html` - HTML report with inline highlighted differences

## Project Structure

```
src/main/java/com/emlcompare/
├── EmlComparator.java              # Main application entry point
├── EmlParser.java                  # EML file parser
├── PdfExtractor.java               # PDF text extraction
├── InlineDiffGenerator.java        # Phrase-level diff generator
└── InlineHtmlReportGenerator.java  # HTML report generator
```

## License

This project is open source.
