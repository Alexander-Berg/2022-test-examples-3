package ru.yandex.market.ff.integration;

import ru.yandex.market.ff.grid.reader.GridReader;
import ru.yandex.market.ff.grid.reader.excel.XlsxGridReader;

import static org.mockito.Mockito.when;

/**
 * Тест на валидацию документа в xlsx формате.
 *
 * @author avetokhin 27/12/17.
 */
public class XlsxGridValidatorIntegrationTest extends AbstractGridValidatorIntegrationTest {

    private GridReader gridReader = new XlsxGridReader(paramService);

    @Override
    protected GridReader getGridReader() {
        when(paramService.getMaxFileSizeForXlsxGridReader()).thenReturn(500_000);
        return gridReader;
    }

    @Override
    protected String getExtension() {
        return "xlsx";
    }
}
