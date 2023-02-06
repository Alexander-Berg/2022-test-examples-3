package ru.yandex.market.ff.grid.reader;

import ru.yandex.market.ff.grid.reader.excel.XlsGridReader;
import ru.yandex.market.ff.grid.reader.excel.XlsxGridReader;

import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link XlsGridReader}.
 *
 * @author kotovdv 31/07/2017.
 */
public class XLSXGridReaderTest extends BaseExcelGridReaderTest {

    private final GridReader gridReader = new XlsxGridReader(paramService);

    @Override
    protected String getExtension() {
        return "xlsx";
    }

    @Override
    protected GridReader getReader() {
        when(paramService.getMaxFileSizeForXlsxGridReader()).thenReturn(500_000);
        return gridReader;
    }
}
