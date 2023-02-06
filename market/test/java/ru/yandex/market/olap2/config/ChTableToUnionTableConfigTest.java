package ru.yandex.market.olap2.config;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import ru.yandex.market.olap2.model.ChUnionsHolder;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ChTableToUnionTableConfigTest {
    @Test
    public void testTablesToUnionsConfig() throws IOException {
        Map<String, String> m = ChUnionsConfig.getTablesToUnions(ChUnionsConfig.TABLES_TO_UNIONS_CONFIG_FILE);
        assertThat(m.size(), is(2));
        assertThat(m.get("some_real_table_1"), is("some_union_table_distributed"));
        assertThat(m.get("some_real_table_2"), is("some_union_table_distributed"));
        assertThat(m.get("some_non_unioned_table"), nullValue());
    }
    @Test
    public void testUnionsHolder() throws IOException {
        ChUnionsHolder h = new ChUnionsConfig().chUnionsHolder();
        assertThat(h.getUnionForTable("some_real_table_1"), is("some_union_table_distributed"));
        assertThat(h.getUnionForTable("some_real_table_2"), is("some_union_table_distributed"));
        assertThat(h.getUnionForTable("some_non_unioned_table"), nullValue());
    }

}
