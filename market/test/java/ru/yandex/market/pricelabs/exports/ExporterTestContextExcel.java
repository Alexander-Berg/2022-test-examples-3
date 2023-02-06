package ru.yandex.market.pricelabs.exports;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ru.yandex.market.pricelabs.misc.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class ExporterTestContextExcel<T> implements ExporterTestContext<T> {

    private final ListExporter exporter = ExcelListExporter.getInstance();

    public ExporterTestContextExcel() {
        //
    }

    @Override
    public ListExporter exporter() {
        return exporter;
    }

    @Override
    public void verify(String expectResource, byte[] actualBytes) {
        try {
            verifyImpl(expectResource, actualBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyImpl(String expectResource, byte[] actualBytes) throws IOException {

        try (var input = Utils.getResourceStream(expectResource)) {
            try (var expect = new XSSFWorkbook(input)) {

                try (var actual = new XSSFWorkbook(new ByteArrayInputStream(actualBytes))) {
                    var expectSheet = expect.getSheetAt(0);
                    var actualSheet = actual.getSheetAt(0);
                    assertEquals(expectSheet.getSheetName(), actualSheet.getSheetName());

                    int rowCount = expectSheet.getPhysicalNumberOfRows();
                    assertEquals(rowCount, actualSheet.getPhysicalNumberOfRows());

                    var expectHeader = expectSheet.getRow(0);
                    int cellCount = expectHeader.getPhysicalNumberOfCells();
                    String[] headers = new String[cellCount];
                    for (int i = 0; i < cellCount; i++) {
                        headers[i] = expectHeader.getCell(i).getStringCellValue();
                    }
                    check(headers, expectHeader, actualSheet.getRow(0));

                    for (int i = 1; i < rowCount; i++) {
                        check(headers, expectSheet.getRow(i), actualSheet.getRow(i));
                    }
                }

            }
        } catch (RuntimeException e) {
            tryCopy(e, expectResource, actualBytes);
            throw e;
        }
    }

    private void check(String[] headers, XSSFRow expect, XSSFRow actual) {
        var cellCount = expect.getPhysicalNumberOfCells();
        assertEquals(cellCount, actual.getPhysicalNumberOfCells());
        for (int i = 0; i < cellCount; i++) {
            var expectCell = expect.getCell(i);
            var actualCell = actual.getCell(i);
            assertNotNull(expectCell.toString());
            assertEquals(expectCell.toString(), actualCell.toString(), "Checking " + headers[i]);
        }
    }

    @Override
    public String toString() {
        return exporter().getFileType().toString();
    }

    private static void tryCopy(RuntimeException e, String expectResource, byte[] actualBytes) throws IOException {
        if (e.getCause() instanceof FileNotFoundException) {
            var targetPath = System.getProperty("user.home") +
                    "/arc/arcadia/market/pricelabs/integration-tests/src/test-medium/resources";
            if (new File(targetPath).exists()) {
                var name = StringUtils.removeStart(expectResource, "classpath:");
                var file = new File(targetPath, expectResource);
                try (var output = new FileOutputStream(file)) {
                    output.write(actualBytes);
                }
                log.info("Prepared file: {}", file);
            }
        }
    }
}
