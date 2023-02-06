package ru.yandex.market.clickphite.dictionary.processors;

import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickphite.dictionary.ClickhouseService;
import ru.yandex.market.clickphite.dictionary.Dictionary;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JsonEmptyAsNullDictionaryProcessorTest {
    private final ClickhouseTemplate clickhouseTemplate = Mockito.mock(ClickhouseTemplate.class);
    private final ClickhouseService clickhouseService = new ClickhouseService() {{
        setClickhouseTemplate(clickhouseTemplate);
    }};

    @Test
    public void insertDataWithEmptyDateValues() throws Exception {
        // Given
        String line = "{\"to_date\":\"\"}";

        // When
        Dictionary dictionary = new Dictionary() {
            @Override
            public String getTable() {
                return "test";
            }

            @Override
            public List<Column> getColumns() {
                return Collections.singletonList(new Column("to_date", ColumnType.DateTime, "'default:0000-00-00 00:00:00'"));
            }
        };
        insertLineToDictionary(line, dictionary);

        // Then
        Mockito.verify(clickhouseTemplate).update("INSERT INTO tmp_tbl (to_date) VALUES\n" +
            "('default:0000-00-00 00:00:00')\n", "test-host");
    }

    @Test(expected = NullPointerException.class)
    public void insertDataWithEmptyDateValuesNoDefaults() throws Exception {
        // Given
        String line = "{\"to_date\":\"\"}";

        // When
        Dictionary dictionary = new Dictionary() {
            @Override
            public String getTable() {
                return "test";
            }

            @Override
            public List<Column> getColumns() {
                return Collections.singletonList(new Column("to_date", ColumnType.DateTime));
            }
        };
        insertLineToDictionary(line, dictionary);
    }

    @Test
    public void insertDataWithEmptyIntValues() throws Exception {
        // Given
        String line = "{\"to_int16\":\"\", \"to_uint64\":\"\"}";

        // When
        Dictionary dictionary = new Dictionary() {
            @Override
            public String getTable() {
                return "test";
            }

            @Override
            public List<Column> getColumns() {
                return Arrays.asList(new Column("to_int16", ColumnType.Int16, "-13"), new Column("to_uint64", ColumnType.UInt64, "42"));
            }
        };
        insertLineToDictionary(line, dictionary);

        // Then
        Mockito.verify(clickhouseTemplate).update("INSERT INTO tmp_tbl (to_int16, to_uint64) VALUES\n" +
            "(-13,42)\n", "test-host");
    }

    @Test(expected = NullPointerException.class)
    public void insertDataWithEmptyIntValuesNoDefaults() throws Exception {
        // Given
        String line = "{\"to_int16\":\"\", \"to_uint64\":\"\"}";

        // When
        Dictionary dictionary = new Dictionary() {
            @Override
            public String getTable() {
                return "test";
            }

            @Override
            public List<Column> getColumns() {
                return Arrays.asList(new Column("to_int16", ColumnType.Int16), new Column("to_uint64", ColumnType.UInt64));
            }
        };
        insertLineToDictionary(line, dictionary);
    }

    private void insertLineToDictionary(String line, Dictionary dictionary) throws IOException {
        JsonDictionaryProcessorTest.insertLineToDictionary(line, dictionary, clickhouseService, new JsonEmptyAsNullDictionaryProcessor());
    }
}
