package ru.yandex.direct.ytwrapper.dynamic;

import com.google.common.collect.ImmutableMap;
import org.jooq.Name;
import org.jooq.Schema;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.SchemaImpl;
import org.jooq.impl.TableImpl;
import org.jooq.impl.TableRecordImpl;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.ytwrapper.dynamic.dsl.YtDSL;

import static com.google.common.primitives.Ints.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.row;
import static ru.yandex.direct.ytwrapper.dynamic.YtQueryComposerTest.TestTable.TEST_TABLE;
import static ru.yandex.direct.ytwrapper.dynamic.dsl.YtDSL.toLong;

public class YtQueryComposerTest {

    private YtQueryComposer queryComposer;

    @Before
    public void setUp() {
        TableMappings tableMappings = () -> ImmutableMap.of(
                TEST_TABLE, "/tmp/test_table"
        );
        queryComposer = new YtQueryComposer(tableMappings);
    }

    @Test
    public void testQueryCompose() {
        TestTable firstTable = TEST_TABLE.as("T1");
        TestTable secondTable = TEST_TABLE.as("T2");

        Select select = YtDSL.ytContext().select(firstTable.id)
                .from(firstTable)
                .join(secondTable).on(firstTable.id.eq(secondTable.id)
                        .and(firstTable.id.eq(secondTable.id)));

        String actual = queryComposer.apply(select);
        assertThat(actual).isEqualTo(
                "T1.ID FROM [/tmp/test_table] AS T1 JOIN [/tmp/test_table] AS T2 ON ( T1.ID = T2.ID AND T1.ID = T2.ID" +
                        " )");
    }

    @Test
    public void testQueryCompose_badBracketsInJooqGenerated() {
        TestTable firstTable = TEST_TABLE.as("T1");
        TestTable secondTable = TEST_TABLE.as("T2");

        Select select = YtDSL.ytContext().select(firstTable.id)
                .from(firstTable)
                .join(secondTable).on(row(firstTable.id, firstTable.id).eq(row(secondTable.id, secondTable.id))
                        .and(firstTable.id.in(asList(1, 2, 3))))
                .and(secondTable.id.in(asList(1, 2, 3)));

        String actual = queryComposer.apply(select);
        assertThat(actual).isEqualTo(
                "T1.ID FROM [/tmp/test_table] AS T1 JOIN [/tmp/test_table] AS T2 ON ( T1.ID, T1.ID) = (T2.ID, T2.ID) " +
                        "AND T1.ID IN ( 1, 2, 3 ) AND T2.ID IN ( 1, 2, 3 ) ");
    }

    @Test
    public void testQueryCompose_badBracketsWithFunctionsInJooqGenerated() {
        TestTable table = TEST_TABLE.as("T");

        Select select = YtDSL.ytContext().select(table.text)
                .from(table)
                .where(table.id.ne(3L));

        String actual = queryComposer.apply(select);
        assertThat(actual).isEqualTo(
                "T.TEXT FROM [/tmp/test_table] AS T WHERE T.ID != 3");
    }

    @Test
    public void testQueryCompose_badNotEqualSymbolInJooqGenerated() {
        TestTable firstTable = TEST_TABLE.as("T1");
        TestTable secondTable = TEST_TABLE.as("T2");

        Select select = YtDSL.ytContext().select(firstTable.id)
                .from(firstTable)
                .join(secondTable).on(row(firstTable.id, toLong(firstTable.id)).eq(row(secondTable.id, secondTable.id))
                        .and(firstTable.id.in(asList(1, 2, 3))));

        String actual = queryComposer.apply(select);
        assertThat(actual).isEqualTo(
                "T1.ID FROM [/tmp/test_table] AS T1 JOIN [/tmp/test_table] AS T2 ON ( T1.ID, int64(T1.ID)) = (T2.ID, " +
                        "T2.ID) AND T1.ID IN ( 1, 2, 3 ) ");
    }

    @Test
    public void testQueryCompose_exchangeLimitOffset1() {
        TestTable table = TEST_TABLE.as("T");

        Select select = YtDSL.ytContext().select(table.id)
                .from(table)
                .limit(100)
                .offset(100);

        String actual = queryComposer.apply(select);
        assertThat(actual).isEqualTo(
                "T.ID FROM [/tmp/test_table] AS T OFFSET 100 LIMIT 100");
    }

    @Test
    public void testQueryCompose_exchangeLimitOffset2() {
        TestTable table1 = TEST_TABLE.as("T1");
        TestTable table2 = TEST_TABLE.as("T2");

        Select select = YtDSL.ytContext().select(table1.id)
                .from(table1)
                .where(table1.id.eq(YtDSL.ytContext()
                        .select(table2.id)
                        .from(table2)
                        .limit(100)
                        .offset(100)))
                .limit(10)
                .offset(10);

        String actual = queryComposer.apply(select);
        assertThat(actual).isEqualTo(
                "T1.ID FROM [/tmp/test_table] AS T1 WHERE T1.ID = " +
                        "( SELECT T2.ID FROM [/tmp/test_table] AS T2 OFFSET 100 LIMIT 100 )" +
                        " OFFSET 10 LIMIT 10");
    }

    @Test
    public void testQueryCompose_withTotals() {
        TestTable table = TEST_TABLE.as("T");

        Select select = YtDSL.ytContext().select(table.text)
                .from(table)
                .groupBy(table.text);

        String actual = queryComposer.apply(select, true);
        assertThat(actual).isEqualTo(
                "T.TEXT FROM [/tmp/test_table] AS T GROUP BY T.TEXT \nWITH TOTALS ");
    }

    @Test
    public void testQueryCompose_withTotalsBeforeLimit() {
        TestTable table = TEST_TABLE.as("T");

        Select select = YtDSL.ytContext().select(table.text)
                .from(table)
                .groupBy(table.text)
                .limit(1000);

        String actual = queryComposer.apply(select, true);
        assertThat(actual).isEqualTo(
                "T.TEXT FROM [/tmp/test_table] AS T GROUP BY T.TEXT \nWITH TOTALS  LIMIT 1000");
    }

    @Test
    public void testQueryCompose_withoutTotalsWhenNotHaveGroupBy() {
        TestTable table = TEST_TABLE.as("T");

        Select select = YtDSL.ytContext().select(table.text)
                .from(table)
                .limit(1000);

        String actual = queryComposer.apply(select, true);
        assertThat(actual).isEqualTo(
                "T.TEXT FROM [/tmp/test_table] AS T LIMIT 1000");
    }

    public static class Yt extends SchemaImpl {

        public static final Yt YT = new Yt();

        Yt() {
            super("yt");
        }

    }

    public static class TestTable extends TableImpl<TestTableRecord> {

        static final TestTable TEST_TABLE = new TestTable();

        public final TableField<TestTableRecord, Long> id =
                createField("ID", org.jooq.impl.SQLDataType.BIGINT, this, "");

        public final TableField<TestTableRecord, String> text =
                createField("TEXT", org.jooq.impl.SQLDataType.CLOB, this, "");

        TestTable() {
            super(DSL.name("test_table"));
        }

        private TestTable(Name alias, Table<TestTableRecord> aliased) {
            super(alias, null, aliased, null, "");
        }

        @Override
        public Class<? extends TestTableRecord> getRecordType() {
            return TestTableRecord.class;
        }

        @Override
        public TestTable as(String alias) {
            return new TestTable(DSL.name(alias), this);
        }

        @Override
        public Schema getSchema() {
            return Yt.YT;
        }

    }

    public static class TestTableRecord extends TableRecordImpl<TestTableRecord> {
        public TestTableRecord() {
            super(TEST_TABLE);
        }
    }

}
