package io.agentscope.examples.bankriskdiagnosis.service;

import io.agentscope.examples.bankriskdiagnosis.model.SourceFileType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentParserService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserService.class);

    public record ParsedDocument(String fileName, SourceFileType fileType, String content) {}

    public ParsedDocument parse(String fileName, byte[] bytes) {
        SourceFileType type = SourceFileType.fromExtension(fileName);
        String content =
                switch (type) {
                    case WORD -> parseWord(bytes);
                    case EXCEL -> parseExcel(bytes);
                    case OFD -> parseOfd(fileName, bytes);
                };
        return new ParsedDocument(fileName, type, content);
    }

    // --- Word (.docx) ---

    String parseWord(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = new ByteArrayInputStream(bytes);
                XWPFDocument doc = new XWPFDocument(is)) {

            // Extract paragraphs
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    // Detect heading-like paragraphs (short, bold, standalone)
                    String styleId = para.getStyleID();
                    if (styleId != null && styleId.startsWith("Heading")) {
                        sb.append("\n## ").append(text).append("\n\n");
                    } else {
                        sb.append(text).append("\n");
                    }
                }
            }

            // Extract tables
            for (XWPFTable table : doc.getTables()) {
                sb.append("\n|");
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        sb.append(cell.getText().replaceAll("\\s+", " ")).append(" | ");
                    }
                    sb.append("\n|");
                }
                sb.append("\n");
            }
        } catch (IOException e) {
            log.error("Failed to parse Word document", e);
            sb.append("[Error parsing Word document: ").append(e.getMessage()).append("]");
        }
        return sb.toString().trim();
    }

    // --- Excel (.xlsx/.xls) ---

    String parseExcel(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = new ByteArrayInputStream(bytes);
                Workbook wb = isXlsx(bytes) ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                sb.append("\n### Sheet: ").append(sheet.getSheetName()).append("\n\n");

                for (Row row : sheet) {
                    sb.append("|");
                    for (Cell cell : row) {
                        sb.append(formatCell(cell)).append(" | ");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        } catch (IOException e) {
            log.error("Failed to parse Excel document", e);
            sb.append("[Error parsing Excel: ").append(e.getMessage()).append("]");
        }
        return sb.toString().trim();
    }

    private boolean isXlsx(byte[] bytes) {
        // .xlsx files are ZIP archives (PK header)
        return bytes.length > 4 && bytes[0] == 0x50 && bytes[1] == 0x4B;
    }

    private String formatCell(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            if (val == Math.floor(val) && !Double.isInfinite(val)) {
                return String.valueOf((long) val);
            }
            return String.valueOf(val);
        } else if (type == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        String text = cell.toString();
        return text.replaceAll("\\s+", " ").trim();
    }

    // --- OFD (GB/T 33190: ZIP + XML) ---

    String parseOfd(String fileName, byte[] bytes) {
        // OFD is a ZIP archive containing XML files per GB/T 33190.
        // Structure: OFD.xml → Doc_0/Page_0/Content.xml (TextObject elements).
        try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(bytes);
                java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(bis)) {

            StringBuilder pageContent = new StringBuilder();

            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Target the page Content.xml which contains <Text> elements
                if (entry.getName().endsWith("Content.xml")) {
                    String xml =
                            new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    pageContent.append(extractOfdText(xml));
                }
                zis.closeEntry();
            }

            String text = pageContent.toString().trim();
            if (!text.isEmpty()) {
                return text;
            }
            return "[OFD: " + fileName + " — 未找到文本内容]";
        } catch (Exception e) {
            log.warn("OFD parsing failed for {}: {}", fileName, e.getMessage());
            return "[OFD: " + fileName + " — 解析失败: " + e.getMessage() + "]";
        }
    }

    /** Extract text from <Text> elements within OFD Content.xml. */
    private String extractOfdText(String xml) {
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        while (true) {
            int start = xml.indexOf("<Text>", pos);
            if (start < 0) break;
            start += 6;
            int end = xml.indexOf("</Text>", start);
            if (end < 0) break;
            String text = xml.substring(start, end).trim();
            if (!text.isEmpty()) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(text);
            }
            pos = end + 7;
        }
        return sb.toString();
    }
}
