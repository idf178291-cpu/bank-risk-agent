package io.agentscope.examples.bankrisk.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
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

                int lastRow = sheet.getLastRowNum();
                if (lastRow < 0) continue;

                // Find max columns (including those only referenced by merged regions)
                int maxCols = 0;
                for (Row row : sheet) {
                    maxCols = Math.max(maxCols, row.getLastCellNum());
                }
                for (CellRangeAddress region : sheet.getMergedRegions()) {
                    maxCols = Math.max(maxCols, region.getLastColumn() + 1);
                }
                if (maxCols == 0) continue;

                // Build 2D grid of cell text (filled by row,col)
                String[][] grid = new String[lastRow + 1][maxCols];
                for (Row row : sheet) {
                    int r = row.getRowNum();
                    for (int c = 0; c < maxCols; c++) {
                        grid[r][c] = getCellText(row.getCell(c));
                    }
                }

                // Fill values from merged regions into all covered cells
                for (CellRangeAddress region : sheet.getMergedRegions()) {
                    String value = null;
                    Row topRow = sheet.getRow(region.getFirstRow());
                    if (topRow != null) {
                        value = getCellText(topRow.getCell(region.getFirstColumn()));
                    }
                    if (value == null || value.isBlank()) continue;
                    for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
                        for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                            if (grid[r][c] == null || grid[r][c].isBlank()) {
                                grid[r][c] = value;
                            }
                        }
                    }
                }

                // Output rows linearly: [行N] col1 | col2 | col3
                // Consecutive empty rows are collapsed; large gaps get a section break
                int consecutiveEmpty = 0;
                for (int r = 0; r <= lastRow; r++) {
                    boolean hasContent = false;
                    StringBuilder line = new StringBuilder();
                    line.append("[").append(r + 1).append("] ");
                    for (int c = 0; c < maxCols; c++) {
                        String val = grid[r][c];
                        if (val != null && !val.isBlank()) {
                            hasContent = true;
                            if (!line.toString().endsWith("] ")) {
                                line.append(" | ");
                            }
                            line.append(val);
                        }
                    }
                    if (hasContent) {
                        // If there was a large gap (>3 empty rows), insert a section break
                        if (consecutiveEmpty > 3) {
                            sb.append("\n--- 以下为新数据段 ---\n\n");
                        }
                        consecutiveEmpty = 0;
                        sb.append(line).append("\n");
                    } else {
                        consecutiveEmpty++;
                    }
                }
                sb.append("\n");
            }
            wb.close();
        }
        return new ParsedDocument(fileName, "Excel", sb.toString());
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
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) {
                    yield String.valueOf((long) v);
                }
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
