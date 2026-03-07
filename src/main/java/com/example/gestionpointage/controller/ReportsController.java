package com.example.gestionpointage.controller;

import com.example.gestionpointage.dto.WeeklyReportDTO;
import com.example.gestionpointage.dto.MonthlyReportDTO;
import com.example.gestionpointage.service.PointageService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.example.gestionpointage.dto.DailyReportRowDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/reports")
public class ReportsController {

    private static final Logger log =
            LoggerFactory.getLogger(ReportsController.class);

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final PointageService pointageService;

    public ReportsController(PointageService pointageService) {
        this.pointageService = pointageService;
    }

    // ═══════════════════════════════════════════════════
    //  JSON ENDPOINTS (unchanged)
    // ═══════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════
    //  ✅ EXPORT DAILY
    // ═══════════════════════════════════════════════════

    @GetMapping(
            value = "/export/daily",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<byte[]> exportDaily(
            @RequestParam Long siteId,
            @RequestParam String date) {

        log.info("📊 Export daily — site={}, date={}", siteId, date);

        try {
            var rows = pointageService.generateDailyReport(
                    siteId, LocalDate.parse(date));

            log.info("✅ Daily report: {} rows", rows.size());

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Daily Report");

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Nom");
                header.createCell(1).setCellValue("Prenom");
                header.createCell(2).setCellValue("Entree");
                header.createCell(3).setCellValue("Sortie");
                header.createCell(4).setCellValue("Total Minutes");
                header.createCell(5).setCellValue("Statut");

                int rowIndex = 1;
                for (var r : rows) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(
                            safeStr(r.getNom()));
                    row.createCell(1).setCellValue(
                            safeStr(r.getPrenom()));
                    row.createCell(2).setCellValue(
                            r.getHeureEntree() != null
                                    ? r.getHeureEntree().toString()
                                    : "");
                    row.createCell(3).setCellValue(
                            r.getHeureSortie() != null
                                    ? r.getHeureSortie().toString()
                                    : "");
                    row.createCell(4).setCellValue(
                            r.getTotalMinutes());
                    row.createCell(5).setCellValue(
                            safeStr(r.getStatut()));
                }

                setColumnWidths(sheet, 6);
                return buildExcelResponse(workbook, "daily_report.xlsx");
            }

        } catch (Exception e) {
            log.error("❌ Export daily failed — site={}, date={}",
                    siteId, date, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBytes(e));
        }
    }

    // ═══════════════════════════════════════════════════
    //  ✅ EXPORT WEEKLY
    // ═══════════════════════════════════════════════════

    @GetMapping(
            value = "/export/weekly",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<byte[]> exportWeekly(
            @RequestParam Long siteId,
            @RequestParam String date) {

        log.info("📊 Export weekly — site={}, date={}", siteId, date);

        try {
            var rows = pointageService.generateWeeklyReport(
                    siteId, LocalDate.parse(date));

            log.info("✅ Weekly report: {} rows", rows.size());

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Weekly Report");

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Nom");
                header.createCell(1).setCellValue("Prenom");
                header.createCell(2).setCellValue("Présences");
                header.createCell(3).setCellValue("Absences");
                header.createCell(4).setCellValue("Retards");
                header.createCell(5).setCellValue("Total Minutes");

                int rowIndex = 1;
                for (var r : rows) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(
                            safeStr(r.getNom()));
                    row.createCell(1).setCellValue(
                            safeStr(r.getPrenom()));
                    row.createCell(2).setCellValue(
                            r.getJoursPresence());
                    row.createCell(3).setCellValue(
                            r.getJoursAbsence());
                    row.createCell(4).setCellValue(
                            r.getRetards());
                    row.createCell(5).setCellValue(
                            r.getTotalMinutes());
                }

                setColumnWidths(sheet, 6);
                return buildExcelResponse(workbook, "weekly_report.xlsx");
            }

        } catch (Exception e) {
            log.error("❌ Export weekly failed — site={}, date={}",
                    siteId, date, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBytes(e));
        }
    }

    // ═══════════════════════════════════════════════════
    //  ✅ EXPORT MONTHLY
    // ═══════════════════════════════════════════════════

    @GetMapping(
            value = "/export/monthly",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<byte[]> exportMonthly(
            @RequestParam Long siteId,
            @RequestParam int year,
            @RequestParam int month) {

        log.info("📊 Export monthly — site={}, {}-{}",
                siteId, year, month);

        try {
            var rows = pointageService.generateMonthlyReport(
                    siteId, year, month);

            log.info("✅ Monthly report: {} rows", rows.size());

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Monthly Report");

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Nom");
                header.createCell(1).setCellValue("Prenom");
                header.createCell(2).setCellValue("Total Jours");
                header.createCell(3).setCellValue("Présences");
                header.createCell(4).setCellValue("Absences");
                header.createCell(5).setCellValue("Total Minutes");

                int rowIndex = 1;
                for (var r : rows) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(
                            safeStr(r.getNom()));
                    row.createCell(1).setCellValue(
                            safeStr(r.getPrenom()));
                    row.createCell(2).setCellValue(
                            r.getTotalJoursTravail());
                    row.createCell(3).setCellValue(
                            r.getTauxPresence());
                    row.createCell(4).setCellValue(
                            r.getAbsences());
                    row.createCell(5).setCellValue(
                            r.getTotalMinutes());
                }

                setColumnWidths(sheet, 6);
                return buildExcelResponse(workbook, "monthly_report.xlsx");
            }

        } catch (Exception e) {
            log.error("❌ Export monthly failed — site={}, {}-{}",
                    siteId, year, month, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBytes(e));
        }
    }

    // ═══════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════

    private ResponseEntity<byte[]> buildExcelResponse(
            Workbook workbook,
            String filename) throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);

        byte[] bytes = out.toByteArray();
        log.info("📦 Excel generated: {} bytes — {}", bytes.length, filename);

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
            sheet.setColumnWidth(i, 5000);
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