package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

import ru.yandex.market.fulfillment.wrap.core.transformer.FulfillmentModelTransformer;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.exception.ModelConversionException;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteWaybillItem;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.Consignment;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Tax;
import ru.yandex.market.logistic.api.model.fulfillment.TaxType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.VatValue;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@SpringBootTest(classes = {ConversionConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class WaybillItemConverterTest extends BaseIntegrationTest {
    @Autowired
    private FulfillmentModelTransformer transformer;

    @BeforeEach
    void init() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    @MethodSource("data")
    @ParameterizedTest
    void convert(Class<? extends Throwable> exception, Consignment consignment, MarschrouteWaybillItem item) {
        if (exception != null) {
            softly.assertThatThrownBy(() -> new WaybillItemConverter(transformer).convert(consignment))
                .isInstanceOf(exception);
        } else {
            MarschrouteWaybillItem actual = new WaybillItemConverter(transformer).convert(consignment);

            softly.assertThat(actual.getItemId())
                .as("check converted item id")
                .isEqualTo(item.getItemId());
            softly.assertThat(actual.getBarcode())
                .as("check converted item barcode")
                .isEqualTo(item.getBarcode());
            softly.assertThat(actual.getName())
                .as("check converted item name")
                .isEqualTo(item.getName());
            softly.assertThat(actual.getComment())
                .as("check converted item comment")
                .isEqualTo(item.getComment());
            softly.assertThat(actual.getQuantity())
                .as("check converted item quantity")
                .isEqualTo(item.getQuantity());
        }
    }

    @Nonnull
    static Stream<Arguments> data() {
        Item item = new Item.ItemBuilder("name", 10, BigDecimal.valueOf(110))
            .setUnitId(new UnitId("", 123L, "shop_sku"))
            .setBarcodes(ImmutableList.of(
                new Barcode("code1", "type"),
                new Barcode("code2", "type"))
            )
            .setDescription("desc")
            .setUntaxedPrice(BigDecimal.valueOf(100))
            .setTax(new Tax(TaxType.VAT, VatValue.TEN))
            .setBoxCapacity(2)
            .setComment("desc")
            .build();

        return Stream.of(
            Arguments.of(
                null,
                new Consignment(
                    null,
                    item,
                    null
                ),
                new MarschrouteWaybillItem().setItemId("shop_sku.123")
                    .setBarcode(Arrays.asList("code1", "code2"))
                    .setName("СПАЙКА name (shop_sku)")
                    .setComment("desc")
                    .setPriceNds(BigDecimal.valueOf(110).setScale(2, BigDecimal.ROUND_UP))
                    .setQuantity(10)
            ),
            Arguments.of(
                null,
                new Consignment(
                    null,
                    new Item.ItemBuilder("name", 2, null)
                        .setUnitId(new UnitId("", 123L, "shop_sku"))
                        .build(),
                    null
                ),
                new MarschrouteWaybillItem().setItemId("shop_sku.123").setName("name (shop_sku)").setQuantity(2)
            ),
            Arguments.of(
                IllegalStateException.class,
                new Consignment(
                    null,
                    new Item.ItemBuilder("name", null, null)
                        .setUnitId(new UnitId("", 123L, "shop_sku"))
                        .build(), null
                ),
                null
            ),
            Arguments.of(
                ModelConversionException.class,
                new Consignment(
                    null,
                    new Item.ItemBuilder("name", null, null)
                        .setUnitId(new UnitId("", null, "shop_sku"))
                        .build(),
                    null
                ),
                null
            ),
            Arguments.of(
                ModelConversionException.class,
                new Consignment(
                    null,
                    new Item.ItemBuilder("name", null, null)
                        .setUnitId(new UnitId("", 123L, ""))
                        .build(),
                    null
                ),
                null
            )
        );
    }
}
