package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.math.BigDecimal;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.core.transformer.FulfillmentModelTransformer;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteItem;
import ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util.ItemIdentifierUtil;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.Tax;
import ru.yandex.market.logistic.api.model.fulfillment.TaxType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.VatValue;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class FulfillmentModelTransformerWithItemsTest extends BaseIntegrationTest {

    @Autowired
    private FulfillmentModelTransformer transformer;

    @Test
    void convert() {
        Item firstItem = createFirstItem();
        Item secondItem = createSecondItem();

        List<MarschrouteItem> converted = transformer.transformFromListToList(
            ImmutableList.of(firstItem, secondItem),
            MarschrouteItem.class
        );

        softly.assertThat(converted)
            .as("Asserting the converted array size")
            .hasSize(2);

        MarschrouteItem firstConverted = converted.get(0);
        MarschrouteItem secondConverted = converted.get(1);

        assertItem(firstConverted, firstItem);
        assertItem(secondConverted, secondItem);
    }

    private void assertItem(MarschrouteItem converted, Item initial) {
        softly.assertThat(converted.getItemId())
            .as("Asserting converted item id")
            .isEqualTo(ItemIdentifierUtil.toItemIdentifier(initial.getUnitId()));
        softly.assertThat(converted.getName())
            .as("Asserting item name")
            .isEqualTo(initial.getName() + " (" + initial.getUnitId().getArticle() + ")");
        softly.assertThat(converted.getPrice())
            .as("Asserting price value")
            .isEqualTo(initial.getPrice().intValueExact());
        softly.assertThat(converted.getComment())
            .as("Asserting comment value")
            .isEqualTo(initial.getComment());
        softly.assertThat(converted.getQuantity())
            .as("Asserting count")
            .isEqualTo(initial.getCount());
        softly.assertThat(converted.getBarcode())
            .as("Asserting barcode value")
            .contains("code1", "code2");
    }

    private Item createFirstItem() {
        UnitId unitId = new UnitId("id1", 1L, "article1");
        Korobyte korobyte = new Korobyte(12, 10, 11, BigDecimal.valueOf(10), BigDecimal.valueOf(9), BigDecimal.valueOf(8));

        return new Item.ItemBuilder("Item1", 10, BigDecimal.TEN)
                .setUnitId(unitId)
                .setArticle("article2")
                .setBarcodes(ImmutableList.of(
                        new Barcode("code1", "type1"),
                        new Barcode("code2", "type1")))
                .setComment("desc1")
                .setCargoType(CargoType.JEWELRY)
                .setKorobyte(korobyte)
                .setHasLifeTime(true)
                .setBoxCapacity(15)
                .setTax(new Tax(TaxType.VAT, VatValue.TEN))
                .build();
    }

    private Item createSecondItem() {
        UnitId unitId = new UnitId("id2", 2L, "article1");
        Korobyte korobyte = new Korobyte(24, 20, 22, BigDecimal.valueOf(20), BigDecimal.valueOf(18), BigDecimal.valueOf(16));

        return new Item.ItemBuilder("Item2", 20, BigDecimal.valueOf(20))
                .setUnitId(unitId)
                .setArticle("article2")
                .setBarcodes(ImmutableList.of(
                        new Barcode("code1", "type2"),
                        new Barcode("code2", "type2")))
                .setComment("desc2")
                .setCargoType(CargoType.ANIMALS)
                .setKorobyte(korobyte)
                .setHasLifeTime(false)
                .setBoxCapacity(30)
                .setTax(new Tax(TaxType.VAT, VatValue.EIGHTEEN))
                .build();
    }
}
