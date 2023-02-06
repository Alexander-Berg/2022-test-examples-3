package ru.yandex.market.ff.grid.reader;

import ru.yandex.market.ff.grid.reader.csv.CsvGridReader;

/**
 * Unit тесты для {@link CsvGridReader}.
 *
 * @author avetokhin 18/09/2017.
 */
public class CsvGridReaderTest extends BaseGridReaderTest {

    private final GridReader gridReader = new CsvGridReader();

    @Override
    protected String getExtension() {
        return "csv";
    }

    @Override
    protected GridReader getReader() {
        return gridReader;
    }
}
