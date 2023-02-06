package ru.yandex.direct.binlogclickhouse.schema;

import java.util.Arrays;
import java.util.HashSet;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.direct.mysql.MySQLColumnData;
import ru.yandex.direct.mysql.schema.ColumnSchema;

@ParametersAreNonnullByDefault
public class FieldValueListTest {
    @Test
    public void fromColumnDataListDiff() throws Exception {
        ColumnSchema[] schema = new ColumnSchema[]{
                new ColumnSchema("not_changed", "text", "text", "", false),
                new ColumnSchema("changed", "text", "text", "", false),
                new ColumnSchema("changed_from_null", "text", "text", "", true),
                new ColumnSchema("changed_to_null", "text", "text", "", true)
        };
        FieldValueList fieldValueList = FieldValueList.fromColumnDataListDiff(
                Arrays.asList(
                        new MySQLColumnData(schema[0], "same"),
                        new MySQLColumnData(schema[1], "after"),
                        new MySQLColumnData(schema[2], "ok"),
                        new MySQLColumnData(schema[3], null)),
                Arrays.asList(
                        new MySQLColumnData(schema[0], "same"),
                        new MySQLColumnData(schema[1], "before"),
                        new MySQLColumnData(schema[2], null),
                        new MySQLColumnData(schema[3], "ok")));
        Assert.assertEquals(fieldValueList.getNames(),
                Arrays.asList("changed", "changed_from_null", "changed_to_null"));
        Assert.assertEquals(fieldValueList.getValues(), Arrays.asList("after", "ok", null));
    }

    @Test
    public void fromColumnDataListDiffFilter() throws Exception {
        ColumnSchema[] schema = new ColumnSchema[]{
                new ColumnSchema("not_changed", "text", "text", "", false),
                new ColumnSchema("changed", "text", "text", "", false),
                new ColumnSchema("filtered", "text", "text", "", false),
                new ColumnSchema("filtered2", "text", "text", "", false),
                new ColumnSchema("changed_from_null", "text", "text", "", true),
                new ColumnSchema("changed_to_null", "text", "text", "", true)
        };
        FieldValueList fieldValueList = FieldValueList.fromColumnDataListFilter(
                Arrays.asList(
                        new MySQLColumnData(schema[0], "same"),
                        new MySQLColumnData(schema[1], "after"),
                        new MySQLColumnData(schema[2], "you_should_not_see_it"),
                        new MySQLColumnData(schema[3], "you_should_not_see_it"),
                        new MySQLColumnData(schema[4], "ok"),
                        new MySQLColumnData(schema[5], null)),
                new HashSet<>(Arrays.asList("filtered", "filtered2", "non_existent")));
        Assert.assertEquals(fieldValueList.getNames(),
                Arrays.asList("not_changed", "changed", "changed_from_null", "changed_to_null"));
        Assert.assertEquals(fieldValueList.getValues(), Arrays.asList("same", "after", "ok", null));
    }

    @Test
    public void fromColumnDataListFilter() throws Exception {
        ColumnSchema[] schema = new ColumnSchema[]{
                new ColumnSchema("not_changed", "text", "text", "", false),
                new ColumnSchema("changed", "text", "text", "", false),
                new ColumnSchema("filtered", "text", "text", "", false),
                new ColumnSchema("filtered2", "text", "text", "", false),
                new ColumnSchema("changed_from_null", "text", "text", "", true),
                new ColumnSchema("changed_to_null", "text", "text", "", true)
        };
        FieldValueList fieldValueList = FieldValueList.fromColumnDataListDiffFilter(
                Arrays.asList(
                        new MySQLColumnData(schema[0], "same"),
                        new MySQLColumnData(schema[1], "after"),
                        new MySQLColumnData(schema[2], "you_should_not_see_it"),
                        new MySQLColumnData(schema[3], "you_should_not_see_it1"),
                        new MySQLColumnData(schema[4], "ok"),
                        new MySQLColumnData(schema[5], null)),
                Arrays.asList(
                        new MySQLColumnData(schema[0], "same"),
                        new MySQLColumnData(schema[1], "before"),
                        new MySQLColumnData(schema[2], "you_should_not_see_it"),
                        new MySQLColumnData(schema[3], "you_should_not_see_it2"),
                        new MySQLColumnData(schema[4], null),
                        new MySQLColumnData(schema[5], "ok")),
                new HashSet<>(Arrays.asList("filtered", "filtered2", "non_existent")));
        Assert.assertEquals(fieldValueList.getNames(),
                Arrays.asList("changed", "changed_from_null", "changed_to_null"));
        Assert.assertEquals(fieldValueList.getValues(), Arrays.asList("after", "ok", null));
    }

}
