package ru.yandex.market.api.cpa.tax.converter;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.api.cpa.tax.dto.ShopVatForm;
import ru.yandex.market.core.tax.model.ShopVat;
import ru.yandex.market.core.tax.model.TaxSystem;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.core.tax.model.VatSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit тесты для {@link FormToShopVatConverter}.
 *
 * @author avetokhin 18/07/17.
 */
public class FormToShopVatConverterTest {

    private static final long DATASOURCE_ID = 1;

    private FormToShopVatConverter converter = new FormToShopVatConverter();

    /**
     * Проверяет валидность конвертации при пустом списке идентификаторов магазинов в форме.
     */
    @Test
    public void emptyDatasourcesTest() {
        final ShopVatForm form = new ShopVatForm(null, TaxSystem.OSN, VatRate.VAT_18, VatSource.WEB, VatRate.NO_VAT);
        final Collection<ShopVat> result = converter.convert(DATASOURCE_ID, form);

        assertThat(result, notNullValue());
        assertThat(result, hasSize(1));

        final ShopVat shopVat = result.iterator().next();
        assertThat(shopVat.getDatasourceId(), equalTo(DATASOURCE_ID));
        assertThat(shopVat.getTaxSystem(), equalTo(form.getTaxSystem()));
        assertThat(shopVat.getVatRate(), equalTo(form.getVatRate()));
        assertThat(shopVat.getVatSource(), equalTo(form.getVatSource()));
        assertThat(shopVat.getDeliveryVatRate(), equalTo(form.getDeliveryVatRate()));
    }

    /**
     * Проверяет валидность конвертации при заполненном списке идентификаторов магазинов в форме.
     */
    @Test
    public void notEmptyDatasourcesTest() {
        final long datasourceId = 2;
        final ShopVatForm form = new ShopVatForm(Collections.singletonList(datasourceId), TaxSystem.OSN, VatRate.VAT_18,
                VatSource.WEB, VatRate.NO_VAT);
        final Collection<ShopVat> result = converter.convert(DATASOURCE_ID, form);

        assertThat(result, notNullValue());
        assertThat(result, hasSize(1));

        final ShopVat shopVat = result.iterator().next();
        assertThat(shopVat.getDatasourceId(), equalTo(datasourceId));
        assertThat(shopVat.getTaxSystem(), equalTo(form.getTaxSystem()));
        assertThat(shopVat.getVatRate(), equalTo(form.getVatRate()));
        assertThat(shopVat.getVatSource(), equalTo(form.getVatSource()));
        assertThat(shopVat.getDeliveryVatRate(), equalTo(form.getDeliveryVatRate()));
    }

}
