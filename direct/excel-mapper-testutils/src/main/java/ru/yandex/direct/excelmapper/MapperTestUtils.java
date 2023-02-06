package ru.yandex.direct.excelmapper;

import java.util.List;

import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

import static java.lang.String.format;

public class MapperTestUtils {
    private MapperTestUtils() {
    }

    public static SheetRange createEmptySheet() {
        XSSFWorkbook workbook = new XSSFWorkbook(XSSFWorkbookType.XLSX);
        XSSFSheet sheet = workbook.createSheet("test");
        return createSheetRange(sheet);
    }

    @SuppressWarnings("CodeBlock2Expr")
    public static SheetRange createStringSheetFromLists(List<List<String>> sheetRangeListRepresentation) {
        return createSheetFromLists(sheetRangeListRepresentation, false);
    }

    public static SheetRange createFormulasSheetFromLists(List<List<String>> sheetRangeListRepresentation) {
        return createSheetFromLists(sheetRangeListRepresentation, true);
    }

    private static SheetRange createSheetFromLists(List<List<String>> sheetRangeListRepresentation, boolean formulas) {
        XSSFWorkbook workbook = new XSSFWorkbook(XSSFWorkbookType.XLSX);
        XSSFSheet sheet = workbook.createSheet("test");
        SheetRange sheetRange = createSheetRange(sheet);

        EntryStream.of(sheetRangeListRepresentation).forKeyValue((rowIndex, rowContent) -> {
            EntryStream.of(rowContent).forKeyValue((columnIndex, cellValue) -> {
                if (formulas) {
                    sheetRange.getCell(rowIndex, columnIndex).setCellFormula(cellValue);
                } else {
                    sheetRange.getCell(rowIndex, columnIndex).setCellValue(cellValue);
                }
            });
        });
        return sheetRange.makeSubRange(0, 0, sheetRangeListRepresentation.size());
    }

    public static List<List<String>> sheetToLists(Sheet sheet, int width) {
        return sheetToLists(createSheetRange(sheet), width);
    }

    @SuppressWarnings("CodeBlock2Expr")
    public static List<List<String>> sheetToLists(SheetRange sheetRange, int width) {
        SheetRange restrictedRange = sheetRange.makeSubRange(0, 0, SheetRange.HeightMode.AUTODETECT_HEIGHT);
        return IntStreamEx.range(restrictedRange.getHeight()).boxed().map(rowIndex -> {
            return IntStreamEx.range(0, width).mapToObj(columnIndex -> {
                try {
                    Cell cell = restrictedRange.getCell(rowIndex, columnIndex);
                    switch (cell.getCellTypeEnum()) {
                        case FORMULA:
                            return cell.getCellFormula();
                        default:
                            return cell.getStringCellValue();
                    }
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(
                            format("Cannot get cell (%d, %d): %s", rowIndex, columnIndex, e.getMessage()), e);
                }
            }).toImmutableList();
        }).toImmutableList();
    }

    private static SheetRange createSheetRange(Sheet sheet) {
        return new SheetRangeImpl(sheet, 0, 0, SheetRange.HeightMode.MAX_HEIGHT);
    }
}
