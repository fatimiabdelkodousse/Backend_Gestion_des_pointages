package com.example.gestionpointage.controller;

import com.example.gestionpointage.dto.WeeklyReportDTO;
import com.example.gestionpointage.dto.MonthlyReportDTO;
import com.example.gestionpointage.dto.DailyReportRowDTO;
import com.example.gestionpointage.service.PointageService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@RestController
@RequestMapping("/reports")
public class ReportsController {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final DateTimeFormatter REPORT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final PointageService pointageService;

    public ReportsController(PointageService pointageService) {
        this.pointageService = pointageService;
    }


    @GetMapping("/daily")
    public List<DailyReportRowDTO> daily(
            @RequestParam Long siteId,
            @RequestParam String date) {
        return pointageService.generateDailyReport(
                siteId, LocalDate.parse(date));
    }

    @GetMapping("/weekly")
    public List<WeeklyReportDTO> weekly(
            @RequestParam Long siteId,
            @RequestParam String date) {
        return pointageService.generateWeeklyReport(
                siteId, LocalDate.parse(date));
    }

    @GetMapping("/monthly")
    public List<MonthlyReportDTO> monthly(
            @RequestParam Long siteId,
            @RequestParam int year,
            @RequestParam int month) {
        return pointageService.generateMonthlyReport(
                siteId, year, month);
    }


    @GetMapping(
            value = "/export/daily",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<byte[]> exportDaily(
            @RequestParam Long siteId,
            @RequestParam String date) {

        try {
            var rows = pointageService.generateDailyReport(
                    siteId, LocalDate.parse(date));
            
            System.out.println("=== DEBUG ===");
            System.out.println("siteId: " + siteId);
            System.out.println("date: " + date);
            System.out.println("rows size: " + rows.size());
            System.out.println("=============");
            
            if (rows.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Rapport Journalier");

                addGeneratedDateRow(sheet, 6);

                Row header = sheet.createRow(1);
                header.createCell(0).setCellValue("Nom");
                header.createCell(1).setCellValue("Prénom");
                header.createCell(2).setCellValue("Entrée");
                header.createCell(3).setCellValue("Sortie");
                header.createCell(4).setCellValue("Total Heures de Travail");  // ✅
                header.createCell(5).setCellValue("Statut");

                // ✅ Style en-têtes en gras
                applyHeaderStyle(workbook, header, 6);

                // Row 2+: Données
                int rowIndex = 2;
                for (var r : rows) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(safeStr(r.getNom()));
                    row.createCell(1).setCellValue(safeStr(r.getPrenom()));
                    row.createCell(2).setCellValue(
                            r.getHeureEntree() != null
                                    ? r.getHeureEntree().toString() : "—");
                    row.createCell(3).setCellValue(
                            r.getHeureSortie() != null
                                    ? r.getHeureSortie().toString() : "—");
                    row.createCell(4).setCellValue(r.getTotalHeuresTravail());  // ✅
                    row.createCell(5).setCellValue(safeStr(r.getStatut()));
                }

                setColumnWidths(sheet, 6);
                return buildExcelResponse(workbook, "rapport_journalier.xlsx");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBytes(e));
        }
    }


    @GetMapping(
            value = "/export/weekly",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<byte[]> exportWeekly(
            @RequestParam Long siteId,
            @RequestParam String date) {

        try {
            var rows = pointageService.generateWeeklyReport(
                    siteId, LocalDate.parse(date));

            if (rows.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Rapport Hebdomadaire");

                addGeneratedDateRow(sheet, 6);

                Row header = sheet.createRow(1);
                header.createCell(0).setCellValue("Nom");
                header.createCell(1).setCellValue("Prénom");
                header.createCell(2).setCellValue("Jours Présence");
                header.createCell(3).setCellValue("Jours Absence");
                header.createCell(4).setCellValue("Retards");
                header.createCell(5).setCellValue("Total Heures de Travail");  // ✅

                applyHeaderStyle(workbook, header, 6);

                int rowIndex = 2;
                for (var r : rows) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(safeStr(r.getNom()));
                    row.createCell(1).setCellValue(safeStr(r.getPrenom()));
                    row.createCell(2).setCellValue(r.getJoursPresence());
                    row.createCell(3).setCellValue(r.getJoursAbsence());
                    row.createCell(4).setCellValue(r.getRetards());
                    row.createCell(5).setCellValue(r.getTotalHeuresTravail());  // ✅
                }

                setColumnWidths(sheet, 6);
                return buildExcelResponse(workbook, "rapport_hebdomadaire.xlsx");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBytes(e));
        }
    }

    // ═══════════════════════════════════════════════════
    //  EXPORT MONTHLY
    // ═══════════════════════════════════════════════════

    @GetMapping(
            value = "/export/monthly",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<byte[]> exportMonthly(
            @RequestParam Long siteId,
            @RequestParam int year,
            @RequestParam int month) {

        try {
            var rows = pointageService.generateMonthlyReport(
                    siteId, year, month);

            // ✅ Pas de données → 204
            if (rows.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Rapport Mensuel");

                // ✅ Row 0: "Édité le ..."
                addGeneratedDateRow(sheet, 6);

                // ✅ Row 1: En-têtes
                Row header = sheet.createRow(1);
                header.createCell(0).setCellValue("Nom");
                header.createCell(1).setCellValue("Prénom");
                header.createCell(2).setCellValue("Jours Travaillés");
                header.createCell(3).setCellValue("Taux Présence (%)");
                header.createCell(4).setCellValue("Absences");
                header.createCell(5).setCellValue("Total Heures de Travail");  // ✅

                applyHeaderStyle(workbook, header, 6);

                int rowIndex = 2;
                for (var r : rows) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(safeStr(r.getNom()));
                    row.createCell(1).setCellValue(safeStr(r.getPrenom()));
                    row.createCell(2).setCellValue(r.getTotalJoursTravail());
                    row.createCell(3).setCellValue(
                            Math.round(r.getTauxPresence() * 100.0) / 100.0 + "%");
                    row.createCell(4).setCellValue(r.getAbsences());
                    row.createCell(5).setCellValue(r.getTotalHeuresTravail());  // ✅
                }

                setColumnWidths(sheet, 6);
                return buildExcelResponse(workbook, "rapport_mensuel.xlsx");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBytes(e));
        }
    }

    // ═══════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════

    private void addGeneratedDateRow(Sheet sheet, int columnCount) {
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue(
                "Édité le " + LocalDateTime.now().format(REPORT_DATE_FORMAT)
        );

        if (columnCount > 1) {
            sheet.addMergedRegion(
                    new CellRangeAddress(0, 0, 0, columnCount - 1)
            );
        }

        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        cell.setCellStyle(style);
    }

    private void applyHeaderStyle(Workbook workbook, Row headerRow, int columnCount) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < columnCount; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                cell.setCellStyle(style);
            }
        }
    }

    private ResponseEntity<byte[]> buildExcelResponse(
            Workbook workbook,
            String filename) throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        byte[] bytes = out.toByteArray();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + filename)
                .header(HttpHeaders.CONTENT_TYPE, XLSX_CONTENT_TYPE)
                .header(HttpHeaders.CONTENT_LENGTH,
                        String.valueOf(bytes.length))
                .body(bytes);
    }

    private void setColumnWidths(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.setColumnWidth(i, 6000);
        }
    }

    private String safeStr(String value) {
        return value != null ? value : "";
    }

    private byte[] errorBytes(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
        return msg.getBytes();
    }
}