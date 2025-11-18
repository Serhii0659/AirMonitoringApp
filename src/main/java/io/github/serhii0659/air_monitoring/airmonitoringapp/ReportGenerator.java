package io.github.serhii0659.air_monitoring.airmonitoringapp;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Create title style with wrap
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setWrapText(true);
            titleStyle.setVerticalAlignment(VerticalAlignment.TOP);

            // Create title with proper line breaks
            int currentRow = 0;
            String[] titleLines = data.title.split("\n");
            for (String line : titleLines) {
                Row titleRow = sheet.createRow(currentRow++);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue(line);
                titleCell.setCellStyle(titleStyle);
                titleRow.setHeightInPoints(20);
            }

            currentRow++; // Empty row after title

            // Create headers
            Row headerRow = sheet.createRow(currentRow++);
            for (int i = 0; i < data.headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(data.headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            for (List<String> rowData : data.rows) {
                Row row = sheet.createRow(currentRow++);
                for (int i = 0; i < rowData.size(); i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(rowData.get(i));
                }
            }

            // Add total row if this is statistics report (has numeric columns)
            if (data.hasTotalRow) {
                Row totalRow = sheet.createRow(currentRow);

                CellStyle boldStyle = workbook.createCellStyle();
                Font boldFont = workbook.createFont();
                boldFont.setBold(true);
                boldStyle.setFont(boldFont);

                for (int i = 0; i < data.totalRowData.size(); i++) {
                    Cell cell = totalRow.createCell(i);
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