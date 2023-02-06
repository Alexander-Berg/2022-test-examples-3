package ru.yandex.market.logistics.test.integration.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import one.util.streamex.StreamEx;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;

@UtilityClass
@ParametersAreNonnullByDefault
public class ExcelFileComparisonUtils {
    public void assertEquals(byte[] expected, byte[] actual) {
        assertEquals(new ByteArrayInputStream(expected), new ByteArrayInputStream(actual));
    }

    @SneakyThrows
    public void assertEquals(InputStream expected, InputStream actual) {
        List<String> expectedCells = getCellValues(expected);
        List<String> actualCells = getCellValues(actual);
        Assertions.assertIterableEquals(expectedCells, actualCells);
    }

    @Nonnull
    @SneakyThrows
    private List<String> getCellValues(InputStream inputStream) {
        try (OPCPackage pkg = OPCPackage.open(inputStream)) {
            try (XSSFWorkbook workbook = new XSSFWorkbook(pkg)) {
                return StreamEx.of(workbook.sheetIterator())
                    .flatMap(sheet -> StreamEx.of(sheet.rowIterator()))
                    .flatMap(row -> StreamEx.of(row.cellIterator()))
                    .map(cell -> {
                        switch (cell.getCellType()) {
                            case NUMERIC:
                                return String.valueOf(cell.getNumericCellValue());
                            case BOOLEAN:
                                return String.valueOf(cell.getBooleanCellValue());
                            case ERROR:
                                return String.valueOf(cell.getErrorCellValue());
                            case FORMULA:
                                return cell.getCellFormula();
                            default:
                                return cell.getStringCellValue();
                        }
                    })
                    .collect(Collectors.toList());
            }
        }
    }
}
