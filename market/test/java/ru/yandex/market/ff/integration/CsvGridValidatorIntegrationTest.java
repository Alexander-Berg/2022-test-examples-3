package ru.yandex.market.ff.integration;

import ru.yandex.market.ff.grid.reader.GridReader;
import ru.yandex.market.ff.grid.reader.csv.CsvGridReader;

/**
 * Тест на валидацию документа в csv формате.
 *
 * @author avetokhin 27/12/17.
 */
public class CsvGridValidatorIntegrationTest extends AbstractGridValidatorIntegrationTest {

    private final GridReader gridReader = new CsvGridReader();

    @Override
    protected GridReader getGridReader() {
        return gridReader;
    }

    @Override
    protected String getExtension() {
        return "csv";
    }
}
