package ru.yandex.market.olap2.load;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.olap2.dao.ClickhouseDao;
import ru.yandex.market.olap2.dao.ClickhouseDaoImpl;
import ru.yandex.market.olap2.model.RejectException;

public class ClickhouseServiceTest {

    private final ClickhouseDao dao = Mockito.mock(ClickhouseDaoImpl.class);
    private final ClickhouseService service = new ClickhouseService(dao, false);

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

        Mockito.verify(dao, Mockito.atLeastOnce()).addColumnRaw("tmp_new_tbl", "col_3 Nullable(String)");
        Mockito.verify(dao, Mockito.atLeastOnce()).addColumnRaw("tmp_new_tbl", "col_4 LowCardinality(Nullable(String))");
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
}
