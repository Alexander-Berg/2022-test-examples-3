package ru.yandex.market.clickphite.dictionary;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;

import java.util.Arrays;
import java.util.List;

/**
 * Created by astepanel on 22.06.17.
 */
public class DictionaryTest {
    @Test(expected = IllegalArgumentException.class)
    public void badCalculatedColumnTest() {
        Dictionary dictionary = new Dictionary() {

            @Override
            public String getTable() {
                return "";
            }

            @Override
            public List<Column> getColumns() {
                return Arrays.asList(
                        new Column("some_date", ColumnType.Date)
                );
            }

            @Override
            public List<Column> getCalculatedColumns() {
                return Arrays.asList(
                        new Column("some_val", ColumnType.UInt8)
                );
            }
        };
        dictionary.getAllColumns();
    }

    @Test
    public void getAllColumnsTest() {
        Dictionary dictionary = new Dictionary() {

            @Override
            public String getTable() {
                return "";
            }

            @Override
            public List<Column> getColumns() {
                return Arrays.asList(
                        new Column("some_date", ColumnType.Date)
                );
            }

            @Override
            public List<Column> getCalculatedColumns() {
                return Arrays.asList(
                        new Column("some_val", ColumnType.UInt8, "-1")
                );
            }
        };

        List<Column> expected = Arrays.asList(
                new Column("some_date", ColumnType.Date),
                new Column("some_val", ColumnType.UInt8, "-1")
        );
        Assert.assertArrayEquals(expected.toArray(), dictionary.getAllColumns().toArray());

    }
}
