package com.example.gestionpointage.controller;

import com.example.gestionpointage.dto.WeeklyReportDTO;
import com.example.gestionpointage.dto.MonthlyReportDTO;
import com.example.gestionpointage.service.PointageService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.example.gestionpointage.dto.DailyReportRowDTO;

@RestController
@RequestMapping("/reports")
public class ReportsController {

    private final PointageService pointageService;

    public ReportsController(PointageService pointageService) {
        this.pointageService = pointageService;
    }

    @GetMapping("/daily")
    public List<DailyReportRowDTO> daily(
            @RequestParam Long siteId,
            @RequestParam String date
    ) {
        return pointageService.generateDailyReport(
                siteId,
                LocalDate.parse(date)
        );
    }

    @GetMapping("/weekly")
    public List<WeeklyReportDTO> weekly(
            @RequestParam Long siteId,
            @RequestParam String date
    ) {
        return pointageService.generateWeeklyReport(
                siteId,
                LocalDate.parse(date)
        );
    }

    @GetMapping("/monthly")
    public List<MonthlyReportDTO> monthly(
            @RequestParam Long siteId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return pointageService.generateMonthlyReport(
                siteId,
                year,
                month
        );
    }

    // ═══════════════════════════════════════════════════
    //  ✅ EXPORT DAILY
    // ═══════════════════════════════════════════════════

    @GetMapping("/export/daily")
    public ResponseEntity<byte[]> exportDaily(
            @RequestParam Long siteId,
            @RequestParam String date
    ) throws Exception {

        var rows = pointageService.generateDailyReport(
                siteId,
                LocalDate.parse(date)
        );

        Workbook workbook = new XSSFWorkbook();
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
            row.createCell(0).setCellValue(r.getNom());
            row.createCell(1).setCellValue(r.getPrenom());
            row.createCell(2).setCellValue(
                    r.getHeureEntree() != null
                            ? r.getHeureEntree().toString() : "");
            row.createCell(3).setCellValue(
                    r.getHeureSortie() != null
                            ? r.getHeureSortie().toString() : "");
            row.createCell(4).setCellValue(r.getTotalMinutes());
            row.createCell(5).setCellValue(r.getStatut());
        }

        // ✅ عرض الأعمدة يدوياً بدل autoSizeColumn
        setColumnWidths(sheet, 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=daily_report.xlsx")
                .body(out.toByteArray());
    }

    // ═══════════════════════════════════════════════════
    //  ✅ EXPORT WEEKLY (مُصحّح)
    // ═══════════════════════════════════════════════════

    @GetMapping("/export/weekly")
    public ResponseEntity<byte[]> exportWeekly(
            @RequestParam Long siteId,
            @RequestParam String date
    ) throws Exception {

        var rows = pointageService.generateWeeklyReport(
                siteId,
                LocalDate.parse(date)
        );

        Workbook workbook = new XSSFWorkbook();
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
            row.createCell(0).setCellValue(r.getNom());
            row.createCell(1).setCellValue(r.getPrenom());
            row.createCell(2).setCellValue(r.getJoursPresence());
            row.createCell(3).setCellValue(r.getJoursAbsence());
            row.createCell(4).setCellValue(r.getRetards());
            row.createCell(5).setCellValue(r.getTotalMinutes());
        }

        // ✅ عرض يدوي بدل autoSizeColumn
        setColumnWidths(sheet, 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=weekly_report.xlsx")
                .body(out.toByteArray());
    }

    // ═══════════════════════════════════════════════════
    //  ✅ EXPORT MONTHLY (مُصحّح)
    // ═══════════════════════════════════════════════════

    @GetMapping("/export/monthly")
    public ResponseEntity<byte[]> exportMonthly(
            @RequestParam Long siteId,
            @RequestParam int year,
            @RequestParam int month
    ) throws Exception {

        var rows = pointageService.generateMonthlyReport(
                siteId,
                year,
                month
        );

        Workbook workbook = new XSSFWorkbook();
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
            row.createCell(0).setCellValue(r.getNom());
            row.createCell(1).setCellValue(r.getPrenom());
            row.createCell(2).setCellValue(r.getTotalJoursTravail());
            row.createCell(3).setCellValue(r.getTauxPresence());
            row.createCell(4).setCellValue(r.getAbsences());
            row.createCell(5).setCellValue(r.getTotalMinutes());
        }

        // ✅ عرض يدوي بدل autoSizeColumn
        setColumnWidths(sheet, 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=monthly_report.xlsx")
                .body(out.toByteArray());
    }

    // ═══════════════════════════════════════════════════
    //  ✅ HELPER: عرض أعمدة ثابت (بدل autoSizeColumn)
    // ═══════════════════════════════════════════════════

    private void setColumnWidths(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.setColumnWidth(i, 5000);  
        }
    }
}