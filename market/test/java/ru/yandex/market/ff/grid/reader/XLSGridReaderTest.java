package ru.yandex.market.ff.grid.reader;

import ru.yandex.market.ff.grid.reader.excel.XlsGridReader;

import static org.mockito.Mockito.when;

/**
 * Unit тесты для XLS {@link XlsGridReader}.
 *
 * @author kotovdv 31/07/2017.
 */
public class XLSGridReaderTest extends BaseExcelGridReaderTest {

    private final GridReader gridReader = new XlsGridReader(paramService);

    @Override
    protected String getExtension() {
        return "xls";
    }

    @Override
    protected GridReader getReader() {
        when(paramService.getMaxFileSizeForXlsGridReader()).thenReturn(5_000_000);
        return gridReader;
    }
}
