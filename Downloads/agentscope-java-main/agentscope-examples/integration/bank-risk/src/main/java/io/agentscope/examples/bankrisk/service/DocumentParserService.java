package io.agentscope.examples.bankrisk.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Service
public class DocumentParserService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserService.class);

    public record ParsedDocument(String fileName, String fileType, String content) {}

    public ParsedDocument parse(String fileName, byte[] bytes) {
        String lower = fileName.toLowerCase();
        try {
            if (lower.endsWith(".docx")) {
                return parseWord(fileName, bytes);
            } else if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                return parseExcel(fileName, bytes);
            } else if (lower.endsWith(".ofd")) {
                return parseOfd(fileName, bytes);
            } else {
                return new ParsedDocument(fileName, "unknown", "");
            }
        } catch (Exception e) {
            log.error("Failed to parse {}: {}", fileName, e.getMessage());
            return new ParsedDocument(fileName, lower.substring(lower.lastIndexOf('.') + 1), "");
        }
    }

    private ParsedDocument parseWord(String fileName, byte[] bytes) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes));
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return new ParsedDocument(fileName, "Word", extractor.getText());
        }
    }

    private ParsedDocument parseExcel(String fileName, byte[] bytes) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            Workbook wb;
            if (fileName.endsWith(".xls")) {
                wb = new HSSFWorkbook(is);
            } else {
                wb = new XSSFWorkbook(is);
            }
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                sb.append("\n## ").append(sheet.getSheetName()).append("\n\n");

                // Find max column count for this sheet
                int maxCols = 0;
                for (Row row : sheet) {
                    maxCols = Math.max(maxCols, row.getLastCellNum());
                }
                if (maxCols == 0) continue;

                boolean firstRow = true;
                for (Row row : sheet) {
                    // Skip empty rows
                    if (isEmptyRow(row, maxCols)) continue;

                    sb.append("| ");
                    for (int c = 0; c < maxCols; c++) {
                        sb.append(getCellText(row.getCell(c))).append(" | ");
                    }
                    sb.append("\n");

                    // Add markdown table separator after header row
                    if (firstRow) {
                        sb.append("|");
                        for (int c = 0; c < maxCols; c++) {
                            sb.append(" --- |");
                        }
                        sb.append("\n");
                        firstRow = false;
                    }
                }
                sb.append("\n");
            }
            wb.close();
        }
        return new ParsedDocument(fileName, "Excel", sb.toString());
    }

    private boolean isEmptyRow(Row row, int maxCols) {
        if (row == null) return true;
        for (int c = 0; c < maxCols; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !getCellText(cell).isBlank()) return false;
        }
        return true;
    }

    private ParsedDocument parseOfd(String fileName, byte[] bytes) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith("Content.xml")) {
                    byte[] entryBytes = zis.readAllBytes();
                    try {
                        var dbf = DocumentBuilderFactory.newInstance();
                        dbf.setNamespaceAware(true);
                        Document doc =
                                dbf.newDocumentBuilder()
                                        .parse(new ByteArrayInputStream(entryBytes));
                        // OFD namespace: http://www.ofdspec.org/2016
                        NodeList textNodes =
                                doc.getElementsByTagNameNS("http://www.ofdspec.org/2016", "Text");
                        if (textNodes.getLength() == 0) {
                            // Fallback: try without namespace
                            textNodes = doc.getElementsByTagName("Text");
                        }
                        for (int i = 0; i < textNodes.getLength(); i++) {
                            String text = textNodes.item(i).getTextContent();
                            if (text != null && !text.isBlank()) {
                                sb.append(text.trim()).append("\n");
                            }
                        }
                    } catch (Exception e) {
                        log.warn(
                                "Failed to parse OFD Content.xml in {}: {}",
                                fileName,
                                e.getMessage());
                    }
                }
                zis.closeEntry();
            }
        }
        return new ParsedDocument(fileName, "OFD", sb.toString());
    }

    private String getCellText(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
