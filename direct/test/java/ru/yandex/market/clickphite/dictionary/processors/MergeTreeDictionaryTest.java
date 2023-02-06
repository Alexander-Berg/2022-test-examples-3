package ru.yandex.market.clickphite.dictionary.processors;

import junit.framework.Assert;
import org.junit.Test;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickphite.dictionary.Dictionary;
import ru.yandex.market.clickphite.dictionary.MergeTreeDictionary;

import java.util.Arrays;
import java.util.List;

/**
 * Created by astepanel on 19.06.17.
 */
public class MergeTreeDictionaryTest {
    private class SomeBigDictionary implements MergeTreeDictionary {

        @Override
        public String getTable() {
            return "SomeBigDictionary";
        }

        @Override
        public List<Column> getColumns() {
            return Arrays.asList(
                    new Column("some_date", ColumnType.Date),
                    new Column("some_id", ColumnType.Int64, "-1"),
                    new Column("some_another_id", ColumnType.Int64, "-1"),
                    new Column("some_value", ColumnType.ArrayUInt64, "-1")
            );
        }

        @Override
        public Column getDateColumn() {
            return new Column("some_date", ColumnType.Date);
        }

        @Override
        public List<Column> getPrimaryKey() {
            return Arrays.asList(
                    new Column("some_id", ColumnType.Int64, "-1"),
                    new Column("some_another_id", ColumnType.Int64, "-1")
            );
        }

        @Override
        public Integer getGranularity() {
            return 1024;
        }
    }

    @Test
    public void getEngineSpecTest() {
        Dictionary someBigDictionary = new SomeBigDictionary();
        Assert.assertEquals("(some_date, (some_id, some_another_id), 1024)", someBigDictionary.getEngineSpec());
    }
}
