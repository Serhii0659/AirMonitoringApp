package io.github.serhii0659.air_monitoring.airmonitoringapp;

// OpenPDF imports for PDF generation
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

// Apache POI imports for Excel generation
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class ReportGenerator {

    public static class ReportData {
        public String title;
        public List<String> headers = new ArrayList<>();
        public List<List<String>> rows = new ArrayList<>();
        public boolean hasTotalRow = false;
        public List<String> totalRowData = new ArrayList<>();
    }

    /**
     * Generate Excel report
     */
    public static void generateExcel(ReportData data, String filePath) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Звіт");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            // Create title style with wrap
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setWrapText(true);
            titleStyle.setVerticalAlignment(VerticalAlignment.TOP);

            // Create title with proper line breaks
            int currentRow = 0;
            String[] titleLines = data.title.split("\n");
            for (String line : titleLines) {
                org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(currentRow++);
                org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue(line);
                titleCell.setCellStyle(titleStyle);
                titleRow.setHeightInPoints(20);
            }

            currentRow++; // Empty row after title

            // Create headers
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(currentRow++);
            for (int i = 0; i < data.headers.size(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(data.headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            for (List<String> rowData : data.rows) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(currentRow++);
                for (int i = 0; i < rowData.size(); i++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
                    cell.setCellValue(rowData.get(i));
                }
            }

            // Add total row if this is statistics report (has numeric columns)
            if (data.hasTotalRow) {
                org.apache.poi.ss.usermodel.Row totalRow = sheet.createRow(currentRow);

                CellStyle boldStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
                boldFont.setBold(true);
                boldStyle.setFont(boldFont);

                for (int i = 0; i < data.totalRowData.size(); i++) {
                    org.apache.poi.ss.usermodel.Cell cell = totalRow.createCell(i);
                    cell.setCellValue(data.totalRowData.get(i));
                    cell.setCellStyle(boldStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < data.headers.size(); i++) {
                sheet.autoSizeColumn(i);
                // Add some padding
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }

    /**
     * Generate PDF report with Cyrillic support using OpenPDF
     */
    public static void generatePDF(ReportData data, String filePath) throws Exception {
        Document document = new Document(PageSize.A4.rotate()); // Landscape for wider tables
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        try {
            // Create font that supports Cyrillic - using system font with UTF-8 encoding
            // Try to use Arial (Windows) or fallback to embedded font
            BaseFont baseFont;
            try {
                // Try loading Arial from system (supports Cyrillic with UTF-8)
                baseFont = BaseFont.createFont("c:/windows/fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception e) {
                // Fallback to Times Roman with Cp1251 if Arial not available
                baseFont = BaseFont.createFont(BaseFont.TIMES_ROMAN, "Cp1251", BaseFont.EMBEDDED);
            }

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFont, 14, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(baseFont, 10, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font cellFont = new com.lowagie.text.Font(baseFont, 9, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font boldCellFont = new com.lowagie.text.Font(baseFont, 9, com.lowagie.text.Font.BOLD);

            // Add title
            String[] titleLines = data.title.split("\n");
            for (String line : titleLines) {
                Paragraph titlePara = new Paragraph(line, titleFont);
                titlePara.setAlignment(Element.ALIGN_LEFT);
                titlePara.setSpacingAfter(5);
                document.add(titlePara);
            }

            // Add spacing after title
            document.add(new Paragraph(" "));

            // Create table
            PdfPTable table = new PdfPTable(data.headers.size());
            table.setWidthPercentage(100);

            // Add header cells
            for (String header : data.headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setBackgroundColor(Color.LIGHT_GRAY);
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setPadding(5);
                table.addCell(headerCell);
            }

            // Add data rows
            for (List<String> rowData : data.rows) {
                for (String cellData : rowData) {
                    PdfPCell cell = new PdfPCell(new Phrase(cellData != null ? cellData : "", cellFont));
                    cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                    cell.setPadding(3);
                    table.addCell(cell);
                }
            }

            // Add total row if exists
            if (data.hasTotalRow) {
                for (String cellData : data.totalRowData) {
                    PdfPCell cell = new PdfPCell(new Phrase(cellData != null ? cellData : "", boldCellFont));
                    cell.setBackgroundColor(Color.LIGHT_GRAY);
                    cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                    cell.setPadding(3);
                    table.addCell(cell);
                }
            }

            document.add(table);
        } finally {
            document.close();
        }
    }

    /**
     * Convert ResultSet to ReportData
     */
    public static ReportData resultSetToReportData(ResultSet rs, String title) throws Exception {
        ReportData data = new ReportData();
        data.title = title;

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Get headers
        for (int i = 1; i <= columnCount; i++) {
            data.headers.add(metaData.getColumnLabel(i));
        }

        // Get rows
        while (rs.next()) {
            List<String> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
                row.add(value != null ? value.toString() : "");
            }
            data.rows.add(row);
        }

        return data;
    }
}