package io.github.shangor.service;

import io.github.shangor.model.CellData;
import io.github.shangor.model.SheetData;
import io.github.shangor.model.MergedRegion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelService {

    public Flux<List<SheetData>> processExcelFile(byte[] fileContent) {
        return Flux.create(sink -> {
            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileContent))) {
                List<SheetData> sheets = new ArrayList<>();
                
                // Process all sheets
                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                    Sheet sheet = workbook.getSheetAt(sheetIndex);
                    List<CellData> cells = new ArrayList<>();
                    List<MergedRegion> mergedRegions = new ArrayList<>();
                    
                    // Process merged regions
                    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
                        CellRangeAddress mergedRegion = sheet.getMergedRegion(i);
                        mergedRegions.add(MergedRegion.builder()
                            .startRow(mergedRegion.getFirstRow())
                            .endRow(mergedRegion.getLastRow())
                            .startColumn(mergedRegion.getFirstColumn())
                            .endColumn(mergedRegion.getLastColumn())
                            .build());
                    }

                    // Process cells
                    for (Row row : sheet) {
                        for (Cell cell : row) {
                            CellData cellData = CellData.builder()
                                .row(cell.getRowIndex())
                                .column(cell.getColumnIndex())
                                .value(getCellValueAsString(cell))
                                .type(getCellType(cell))
                                .formula(getCellFormula(cell))
                                .style(getCellStyleJson(cell))
                                .build();
                            cells.add(cellData);
                        }
                    }
                    
                    sheets.add(SheetData.builder()
                        .sheetName(sheet.getSheetName())
                        .sheetIndex(sheetIndex)
                        .cells(cells)
                        .mergedRegions(mergedRegions)
                        .build());
                }

                sink.next(sheets);
                sink.complete();
            } catch (IOException e) {
                sink.error(e);
            }
        });
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                // For formulas, return the calculated value for display
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception ex) {
                        yield cell.getCellFormula();
                    }
                }
            }
            case BLANK -> "";
            default -> "";
        };
    }

    private String getCellFormula(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.FORMULA) {
            return "=" + cell.getCellFormula(); // Add the leading '=' sign
        }
        return null;
    }

    private String getCellType(Cell cell) {
        if (cell == null) return "BLANK";
        return cell.getCellType().name();
    }

    private String getCellStyleJson(Cell cell) {
        if (cell == null) return "";

        CellStyle style = cell.getCellStyle();
        if (style == null) return "";

        StringBuilder jsonBuilder = new StringBuilder("{");
        List<String> properties = new ArrayList<>();

        // Font properties
        var fontIndex = style.getFontIndex();
        Font font = cell.getSheet().getWorkbook().getFontAt(fontIndex);
        if (font != null) {
            if (font.getBold()) properties.add("\"bold\":true");
            if (font.getItalic()) properties.add("\"italic\":true");
            if (font.getUnderline() != Font.U_NONE) properties.add("\"underline\":true");
            
            // Font size
            properties.add("\"fontSize\":" + font.getFontHeightInPoints());
            
            // Font color
            if (font instanceof org.apache.poi.xssf.usermodel.XSSFFont) {
                org.apache.poi.xssf.usermodel.XSSFFont xssfFont = (org.apache.poi.xssf.usermodel.XSSFFont) font;
                XSSFColor fontColor = xssfFont.getXSSFColor();
                if (fontColor != null && fontColor.getARGBHex() != null) {
                    properties.add("\"fontColor\":\"#" + fontColor.getARGBHex().substring(2) + "\"");
                }
            }
        }

        // Alignment
        if (style.getAlignment() != HorizontalAlignment.GENERAL) {
            properties.add("\"alignment\":\"" + style.getAlignment().name().toLowerCase() + "\"");
        }

        // Background color
        if (style instanceof XSSFCellStyle) {
            XSSFCellStyle xssfStyle = (XSSFCellStyle) style;
            XSSFColor bgColor = xssfStyle.getFillForegroundColorColor();
            if (bgColor != null && bgColor.getARGBHex() != null) {
                properties.add("\"backgroundColor\":\"#" + bgColor.getARGBHex().substring(2) + "\"");
            }
        }

        jsonBuilder.append(String.join(",", properties));
        jsonBuilder.append("}");

        return properties.isEmpty() ? "" : jsonBuilder.toString();
    }
}
