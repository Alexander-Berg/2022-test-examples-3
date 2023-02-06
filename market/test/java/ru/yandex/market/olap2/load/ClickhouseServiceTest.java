package ru.yandex.market.olap2.load;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import ru.yandex.market.olap2.dao.ClickhouseDao;
import ru.yandex.market.olap2.dao.ClickhouseService;
import ru.yandex.market.olap2.model.RejectException;

import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertEquals;

public class ClickhouseServiceTest {

    public static final String TABLE = "my_table";
    public static final String TASK_STRING = "my_table_load";
    private final ClickhouseDao dao = Mockito.mock(ClickhouseDao.class);
    private final ClickhouseService service = new ClickhouseService(dao, false);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void mustAddNewColumns() {
        Mockito.when(dao.getColumns("main_tbl")).thenReturn(ImmutableMap.of(
                "col_1", "int64",
                "col_2", "Nullable(String)",
                "col_3", "Nullable(String)",
                "col_4", "LowCardinality(Nullable(String))"));

        Mockito.when(dao.getColumns("tmp_new_tbl")).thenReturn(ImmutableMap.of(
                "col_1", "int64",
                "col_2", "Nullable(String)"));

        service.addClickhouseExtraColumns("main_tbl", "tmp_new_tbl");

        verify(dao, Mockito.atLeastOnce()).addColumnRaw("tmp_new_tbl", "col_3 Nullable(String)");
        verify(dao, Mockito.atLeastOnce()).addColumnRaw("tmp_new_tbl", "col_4 LowCardinality(Nullable(String))");
    }

    @Test(expected = RejectException.class)
    public void mustFailOnNotNullableExtraColumns() {
        Mockito.when(dao.getColumns("main_tbl")).thenReturn(ImmutableMap.of(
                "col_1", "int64",
                "col_2", "String"));

        Mockito.when(dao.getColumns("tmp_new_tbl")).thenReturn(ImmutableMap.of(
                "col_1", "int64"));

        service.addClickhouseExtraColumns("main_tbl", "tmp_new_tbl");
    }

    @Test
    public void mustNotFailOnSameTypes() {
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "uint64",
                "fieldTwo", "int64",
                "fieldThree", "string",
                "fieldFour", "double",
                "datetime", "string");
        Map<String, String> chColumns = ImmutableMap.of("fieldOne", "UInt64",
                "fieldTwo", "Int64",
                "fieldThree", "String",
                "fieldFour", "Float64",
                "datetime", "DateTime");
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "UInt64",
                "fieldTwo", "Int64",
                "fieldFour", "Float64",
                "datetime", "DateTime");

        service.checkSameTypes(TABLE, TASK_STRING, chColumns, ytColumns, someAttrs(), fabrikClickhouseTypes);
    }


    @Test
    public void mustFailOnDifferentTypes() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Column 'fieldFour' type changed. In clickhouse 'UInt64', " +
                "in yt 'double' will be converted to 'Float64', task: my_table_load. Alter manually.");
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "uint64",
                "fieldTwo", "int64",
                "fieldThree", "string",
                "fieldFour", "double",
                "datetime", "string");
        Map<String, String> chColumns = ImmutableMap.of("fieldOne", "UInt64",
                "fieldTwo", "Int64",
                "fieldThree", "String",
                "fieldFour", "UInt64", //<----wrong type
                "datetime", "DateTime");
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "UInt64",
                "fieldTwo", "Int64",
                "datetime", "DateTime");

        service.checkSameTypes(TABLE, TASK_STRING, chColumns, ytColumns, someAttrs(), fabrikClickhouseTypes);
    }

    @Test
    public void mustFailWhenExtraColumnsInCh() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Found extra columns in clickhouse which are not present in YT: [fieldFour]");
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "uint64",
                "fieldTwo", "int64",
                "fieldThree", "string",
                "datetime", "string");
        Map<String, String> chColumns = ImmutableMap.of("fieldOne", "UInt64",
                "fieldTwo", "Int64",
                "fieldThree", "String",
                "fieldFour", "Float64",//<-----extra field
                "datetime", "DateTime");
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "UInt64",
                "fieldTwo", "Int64",
                "fieldFour", "Float64",
                "datetime", "DateTime");
        service.checkSameTypes(TABLE, TASK_STRING, chColumns, ytColumns, someAttrs(), fabrikClickhouseTypes);
    }

    @Test
    public void mustNotFailWhenExtraNullableColumnsInCh() {
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "uint64",
                "fieldTwo", "int64",
                "fieldThree", "string",
                "datetime", "string");
        Map<String, String> chColumns = ImmutableMap.of("fieldOne", "UInt64",
                "fieldTwo", "Int64",
                "fieldThree", "String",
                "fieldFour", "Nullable(Float64)",//<-----extra field
                "datetime", "DateTime");
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "UInt64",
                "fieldTwo", "Int64",
                "datetime", "DateTime");
        service.checkSameTypes(TABLE, TASK_STRING, chColumns, ytColumns, someAttrs(), fabrikClickhouseTypes);
    }

    @Test
    public void mustNotFailWhenExtraColumnsInYt() {
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "uint64",
                "fieldTwo", "int64",
                "fieldThree", "string",
                "fieldFour", "double", //<-----extra field
                "datetime", "string");
        Map<String, String> chColumns = ImmutableMap.of("fieldOne", "UInt64",
                "fieldTwo", "Int64",
                "fieldThree", "String",
                "datetime", "DateTime");
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "UInt64",
                "fieldTwo", "Int64",
                "datetime", "DateTime");
        service.checkSameTypes(TABLE, TASK_STRING, new HashMap<>(chColumns), ytColumns, someAttrs(), fabrikClickhouseTypes);
        verify(dao).addColumn(TABLE, "fieldFour", "Float64", null, ImmutableList.of("lowCardinality"));
    }

    @Test
    public void getTableSortingTest() {
        Mockito.when(dao.getRawTableSorting(TABLE)).thenReturn("fieldOne, fieldTwo");
        List<String> sorting = service.getTableSorting(TABLE);
        assertEquals(sorting, Arrays.asList("fieldOne", "fieldTwo"));
    }

    @Test
    public void mustNotFailOnExactSameSorting() {
        Mockito.when(dao.getRawTableSorting(TABLE)).thenReturn("fieldOne, fieldTwo");
        service.checkSameSorting(TABLE, Arrays.asList("fieldOne", "fieldTwo"));
    }

    @Test
    public void mustNotFailOnSimilarSorting() {
        Mockito.when(dao.getRawTableSorting(TABLE)).thenReturn("fieldOne, fieldTwo");
        service.checkSameSorting(TABLE, Arrays.asList("fieldOne", "fieldTwo", "fieldThree"));
    }

    @Test
    public void mustNotFailOnNoSorting() {
        Mockito.when(dao.getRawTableSorting(TABLE)).thenReturn(new String(""));
        service.checkSameSorting(TABLE, Collections.emptyList());
    }

    @Test
    public void mustNotFailOnNoCHSorting() {
        Mockito.when(dao.getRawTableSorting(TABLE)).thenReturn("");
        service.checkSameSorting(TABLE, Arrays.asList("fieldOne", "fieldTwo", "fieldThree"));
    }

    @Test
    public void mustFailOnInvalidSortingKeyCount() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Clickhouse table has more sorting keys than source table (3 > 2)");
        Mockito.when(dao.getRawTableSorting(TABLE)).thenReturn("fieldOne, fieldTwo, fieldThree");
        service.checkSameSorting(TABLE, Arrays.asList("fieldOne", "fieldTwo"));
    }

    @Test
    public void mustFailOnDifferentSorting() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Sorting key mismatch at position 0" +
                ". Clickhouse sorting key: 'fieldOne', source sorting key: 'fieldThree'.");
        Mockito.when(dao.getRawTableSorting(TABLE)).thenReturn("fieldOne, fieldTwo");
        service.checkSameSorting(TABLE, Arrays.asList("fieldThree", "fieldFour"));
    }

    @Test
    public void mustFailOnDifferentSortingOrder() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Sorting key mismatch at position 1" +
                ". Clickhouse sorting key: 'fieldTwo', source sorting key: 'fieldThree'.");
        Mockito.when(dao.getRawTableSorting(TABLE)).thenReturn("fieldOne, fieldTwo, fieldThree");
        service.checkSameSorting(TABLE, Arrays.asList("fieldOne", "fieldThree", "fieldTwo"));
    }

    @Test
    public void mustFailOnNullCHSorting() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Trying to get sorting of non-existing table '" + TABLE + "'");
        Mockito.when(dao.getRawTableSorting(TABLE)).thenReturn(null);
        service.checkSameSorting(TABLE, Collections.emptyList());
    }

    private Map<String, List<String>> someAttrs() {
        return ImmutableMap.of("fieldOne", ImmutableList.of("SamplingKey"),
                "fieldTwo", ImmutableList.of(""),
                "fieldThree", ImmutableList.of("lowCardinality"),
                "fieldFour", ImmutableList.of("lowCardinality"),
                "datetime", ImmutableList.of("")
        );
    }
}
