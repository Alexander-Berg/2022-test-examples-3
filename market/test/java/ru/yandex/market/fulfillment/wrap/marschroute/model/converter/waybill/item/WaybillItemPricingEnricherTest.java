package ru.yandex.market.fulfillment.wrap.marschroute.model.converter.waybill.item;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteWaybillItem;
import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Tax;
import ru.yandex.market.logistic.api.model.fulfillment.TaxType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.VatValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;

class WaybillItemPricingEnricherTest {

    private final WaybillItemPricingEnricher pricingEnricher = new WaybillItemPricingEnricher();

    @Test
    void testEnrichment() {
        Item source = new Item.ItemBuilder("name", 10, BigDecimal.valueOf(110))
                .setUnitId(new UnitId("", 123L, "shop_sku"))
                .setBarcodes(Collections.singletonList(new Barcode("code", "type")))
                .setDescription("desc")
                .setUntaxedPrice(BigDecimal.valueOf(100))
                .setTax(new Tax(TaxType.VAT, VatValue.TEN))
                .build();

        MarschrouteWaybillItem target = new MarschrouteWaybillItem();
        pricingEnricher.enrich(target, source);

        SoftAssertions.assertSoftly(softAssertions ->
                softAssertions.assertThat(target.getSumNds())
                        .as("Asserting sum with NDS")
                        .isEqualTo(createBigDecimal(1100)));

    }

    private BigDecimal createBigDecimal(long value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.UP);
    }
}
