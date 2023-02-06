package ru.yandex.market.ff.integration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.grid.model.grid.Grid;
import ru.yandex.market.ff.grid.reader.GridReader;
import ru.yandex.market.ff.grid.reader.excel.XlsGridReader;
import ru.yandex.market.ff.grid.reader.excel.XlsxGridReader;
import ru.yandex.market.ff.grid.writer.GridWriter;
import ru.yandex.market.ff.grid.writer.excel.XLSGridWriter;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;

import static org.mockito.Mockito.when;

public class ExcelReadWriteIntegrationTest {

    protected final ConcreteEnvironmentParamService paramService = Mockito.mock(ConcreteEnvironmentParamService.class);

    private static final String BASE_DIR = "integration/read-write/";

    /**
     * Проверяем, что если записать Grid в XLS и потом считать, то получим такой же Grid
     */
    @Test
    void testReadWriteXls() {
        when(paramService.getMaxFileSizeForXlsGridReader()).thenReturn(5_000_000);
        readWriteAndCompare("rw-test.xls", new XlsGridReader(paramService), new XLSGridWriter());
    }

    /**
     * Проверяем, что если записать Grid в XLSX и потом считать, то получим такой же Grid
     */
    @Test
    void testReadWriteXlsx() {
        when(paramService.getMaxFileSizeForXlsxGridReader()).thenReturn(500_000);
        readWriteAndCompare("rw-test.xlsx", new XlsxGridReader(paramService), new XLSGridWriter());
    }

    private void readWriteAndCompare(String fileName, GridReader reader, GridWriter writer) {
        Grid grid1 = reader.read(readFileAsInputStream(fileName));

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writer.write(grid1, buf);
        Grid grid2 = reader.read(new ByteArrayInputStream(buf.toByteArray()));

        SoftAssertions.assertSoftly(softly ->
            softly.assertThat(grid1).isEqualToComparingFieldByFieldRecursively(grid2));
    }

    private InputStream readFileAsInputStream(String name) {
        return ClassLoader.getSystemResourceAsStream(BASE_DIR + name);
    }
}
