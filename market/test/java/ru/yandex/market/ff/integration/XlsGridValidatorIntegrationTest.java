package ru.yandex.market.ff.integration;

import ru.yandex.market.ff.grid.reader.GridReader;
import ru.yandex.market.ff.grid.reader.excel.XlsGridReader;

import static org.mockito.Mockito.when;

/**
 * Тест на валидацию документа в xls формате.
 *
 * @author avetokhin 27/12/17.
 */
public class XlsGridValidatorIntegrationTest extends AbstractGridValidatorIntegrationTest {

    private GridReader gridReader = new XlsGridReader(paramService);

    @Override
    protected GridReader getGridReader() {
        when(paramService.getMaxFileSizeForXlsGridReader()).thenReturn(5_000_000);
        return gridReader;
    }

    @Override
    protected String getExtension() {
        return "xls";
    }
}
