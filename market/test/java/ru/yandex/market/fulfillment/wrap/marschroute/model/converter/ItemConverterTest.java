package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.math.BigDecimal;
import java.util.Arrays;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteItem;
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

import static ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util.ItemIdentifierUtil.toItemIdentifier;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ItemConverter.class)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class ItemConverterTest extends BaseIntegrationTest {

    @Autowired
    private ItemConverter itemConverter;

    @Test
    void testItemConversion() throws Exception {
        Item firstInitial = createFirstItem();
        MarschrouteItem firstConverted = itemConverter.convert(firstInitial);
        Item secondInitial = createSecondItem();
        MarschrouteItem secondConverted = itemConverter.convert(secondInitial);
        Item thirdInitial = createThirdItem();
        MarschrouteItem thirdConverted = itemConverter.convert(thirdInitial);
        Item fourthInitial = createFourthItem();
        MarschrouteItem fourthConverted = itemConverter.convert(fourthInitial);

        assertItem(firstConverted, firstInitial, "Item1 (article)");
        assertItem(secondConverted, secondInitial, "Item2 (VENDOR1, VENDOR2)");
        assertItem(thirdConverted, thirdInitial, "Item3 (VENDOR2)");
        assertItem(fourthConverted, fourthInitial, "Item4 (article)");
    }

    private Item createFirstItem() {
        UnitId unitId = new UnitId("id1", 1L, "article");

        Korobyte korobyte = new Korobyte(12, 10, 11, BigDecimal.valueOf(10), BigDecimal.valueOf(9), BigDecimal.valueOf(8));

       return new Item.ItemBuilder("Item1", 10, BigDecimal.TEN)
                .setUnitId(unitId)
                .setArticle("article1")
                .setBarcodes(ImmutableList.of(
                        new Barcode("code1", "type"),
                        new Barcode("code2", "type")))
                .setComment("desc")
                .setCargoType(CargoType.JEWELRY)
                .setKorobyte(korobyte)
                .setHasLifeTime(true)
                .setBoxCapacity(15)
                .setTax(new Tax(TaxType.VAT, VatValue.TEN))
                .build();
    }

    private Item createSecondItem() {
        UnitId unitId = new UnitId("id2", 2L, "article");

        Korobyte korobyte = new Korobyte(12, 10, 11, BigDecimal.valueOf(10), BigDecimal.valueOf(9), BigDecimal.valueOf(8));

        return new Item.ItemBuilder("Item2", 10, BigDecimal.TEN)
                .setUnitId(unitId)
                .setArticle("article2")
                .setBarcodes(ImmutableList.of(
                        new Barcode("code1", "type"),
                        new Barcode("code2", "type")))
                .setComment("desc")
                .setCargoType(CargoType.JEWELRY)
                .setKorobyte(korobyte)
                .setHasLifeTime(true)
                .setBoxCapacity(15)
                .setTax(new Tax(TaxType.VAT, VatValue.TEN))
                .setVendorCodes(Arrays.asList("VENDOR1", "VENDOR2"))
                .build();
    }

    private Item createThirdItem() {
        UnitId unitId = new UnitId("id3", 3L, "article");

        Korobyte korobyte = new Korobyte(12, 10, 11, BigDecimal.valueOf(10), BigDecimal.valueOf(9), BigDecimal.valueOf(8));

        return new Item.ItemBuilder("Item3", 10, BigDecimal.TEN)
                .setUnitId(unitId)
                .setArticle("article3")
                .setBarcodes(ImmutableList.of(
                        new Barcode("code1", "type"),
                        new Barcode("code2", "type")))
                .setComment("desc")
                .setCargoType(CargoType.JEWELRY)
                .setKorobyte(korobyte)
                .setHasLifeTime(true)
                .setBoxCapacity(15)
                .setTax(new Tax(TaxType.VAT, VatValue.TEN))
                .setVendorCodes(Arrays.asList(null, "VENDOR2"))
                .build();
    }

    private Item createFourthItem() {
        UnitId unitId = new UnitId("id4", 4L, "article");

        Korobyte korobyte = new Korobyte(12, 10, 11, BigDecimal.valueOf(10), BigDecimal.valueOf(9), BigDecimal.valueOf(8));

        return new Item.ItemBuilder("Item4", 10, BigDecimal.TEN)
                .setUnitId(unitId)
                .setArticle("article4")
                .setBarcodes(ImmutableList.of(
                        new Barcode("code1", "type"),
                        new Barcode("code2", "type")))
                .setComment("desc")
                .setCargoType(CargoType.JEWELRY)
                .setKorobyte(korobyte)
                .setHasLifeTime(true)
                .setBoxCapacity(15)
                .setTax(new Tax(TaxType.VAT, VatValue.TEN))
                .setVendorCodes(Arrays.asList(null, null))
                .build();
    }

    private void assertItem(MarschrouteItem actualConvertedItem, Item initialItem, String expectedConvertedItemName) {
        softly.assertThat(actualConvertedItem.getItemId())
            .as("Asserting converted item id")
            .isEqualTo(toItemIdentifier(initialItem.getUnitId()));

        softly.assertThat(actualConvertedItem.getName())
            .as("Asserting item name")
            .isEqualTo(expectedConvertedItemName);

        softly.assertThat(actualConvertedItem.getPrice())
            .as("Asserting price value")
            .isEqualTo(initialItem.getPrice().intValueExact());

        softly.assertThat(actualConvertedItem.getComment())
            .as("Asserting comment value")
            .isEqualTo(initialItem.getComment());

        softly.assertThat(actualConvertedItem.getQuantity())
            .as("Asserting count")
            .isEqualTo(initialItem.getCount());

        softly.assertThat(actualConvertedItem.getBarcode())
            .as("Asserting barcode value")
            .contains("code1", "code2");
    }
}
