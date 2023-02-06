package ru.yandex.market.ff.service.mapper;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.model.row.DefaultGridRow;
import ru.yandex.market.ff.grid.model.row.GridRow;
import ru.yandex.market.ff.model.entity.RequestItem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit тесты для {@link WithdrawRequestItemRowMapper}.
 *
 * @author avetokhin 11/12/17.
 */
class WithdrawRequestItemRowMapperTest {

    private static final String ARTICLE = "article";
    private static final String NAME = "name";
    private static final String COUNT = "15";

    @Test
    void test() {
        final GridRow row = new DefaultGridRow(0);
        row.appendCell(new DefaultGridCell(0, 0, ARTICLE));
        row.appendCell(new DefaultGridCell(0, 1, NAME));
        row.appendCell(new DefaultGridCell(0, 2, COUNT));

        final RequestItem item = WithdrawRequestItemRowMapper.INSTANCE.mapRow(row);

        assertThat(item, notNullValue());
        assertThat(item.getId(), nullValue());
        assertThat(item.getRequestId(), nullValue());
        assertThat(item.getArticle(), equalTo(ARTICLE));
        assertThat(item.getName(), equalTo(NAME));
        assertThat(item.getCount(), equalTo(Integer.parseInt(COUNT)));
        assertThat(item.getBarcodes(), equalTo(Collections.emptyList()));
        assertThat(item.getSupplyPrice(), nullValue());
        assertThat(item.getVatRate(), nullValue());
        assertThat(item.getComment(), nullValue());
        assertThat(item.getBoxCount(), nullValue());
        assertThat(item.getFactCount(), nullValue());
        assertThat(item.getDefectCount(), nullValue());
        assertThat(item.getSku(), nullValue());
    }
}
