package ru.yandex.market.ff.service.mapper;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.model.row.DefaultGridRow;
import ru.yandex.market.ff.grid.model.row.GridRow;
import ru.yandex.market.ff.model.entity.RequestItem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit тесты для {@link SupplyRequestItemRowMapperV1}.
 *
 * @author avetokhin 11/12/17.
 */
class SupplyRequestItemRowMapperTest {

    private static final String ARTICLE = "article";
    private static final String NAME = "name";
    private static final String BARCODES = "aaa;bbb";
    private static final String COUNT = "15";
    private static final String PRICE = "125.44";
    private static final String COMMENT = "comment";
    private static final String BOX_COUNT = "12";

    @Test
    void testNumVat() {
        test("1", VatRate.VAT_18);
    }

    @Test
    void testStrVat() {
        test("VAT_18", VatRate.VAT_18);
    }

    private void test(final String vat, final VatRate vatRate) {
        final GridRow row = new DefaultGridRow(0);
        row.appendCell(new DefaultGridCell(0, 0, ARTICLE));
        row.appendCell(new DefaultGridCell(0, 1, NAME));
        row.appendCell(new DefaultGridCell(0, 2, BARCODES));
        row.appendCell(new DefaultGridCell(0, 3, COUNT));
        row.appendCell(new DefaultGridCell(0, 4, PRICE));
        row.appendCell(new DefaultGridCell(0, 5, vat));
        row.appendCell(new DefaultGridCell(0, 6, BOX_COUNT));
        row.appendCell(new DefaultGridCell(0, 7, COMMENT));

        final RequestItem item = SupplyRequestItemRowMapperV1.INSTANCE.mapRow(row);

        assertThat(item, notNullValue());
        assertThat(item.getId(), nullValue());
        assertThat(item.getRequestId(), nullValue());
        assertThat(item.getArticle(), equalTo(ARTICLE));
        assertThat(item.getName(), equalTo(NAME));
        assertThat(item.getCount(), equalTo(Integer.parseInt(COUNT)));
        assertThat(item.getBarcodes(), equalTo(Arrays.asList("aaa", "bbb")));
        assertThat(item.getSupplyPrice(), equalTo(new BigDecimal(PRICE)));
        assertThat(item.getVatRate(), equalTo(vatRate));
        assertThat(item.getComment(), equalTo(COMMENT));
        assertThat(item.getBoxCount(), nullValue());
        assertThat(item.getFactCount(), nullValue());
        assertThat(item.getDefectCount(), nullValue());
        assertThat(item.getSku(), nullValue());
    }

}
