package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util.ItemIdentifierUtil;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.MarschrouteUpdateProductRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.MarschrouteUpdateProductsRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Consignment;
import ru.yandex.market.logistic.api.model.fulfillment.Inbound;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(CreateInboundUpdateRequestConverter.class)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class CreateInboundUpdateRequestConverterTest extends BaseIntegrationTest {

    @Autowired
    private CreateInboundUpdateRequestConverter converter;

    @Test
    void convert() {
        Consignment firstConsignment = new Consignment(
                null,
                new Item.ItemBuilder("NAME1", null, null)
                        .setUnitId(new UnitId(null, 123L, "ARTICLE1"))
                        .setLifeTime(1000)
                        .setComment("Не кантовать")
                        .build(),
                null
        );

        Consignment secondConsignment = new Consignment(
                null,
                new Item.ItemBuilder("NAME2", null, null)
                        .setUnitId(new UnitId(null, 456L, "ARTICLE2"))
                        .setLifeTime(2000)
                        .setComment("Кантовать, но нежно")
                        .build(),
                null
        );

        Consignment thirdConsignment = new Consignment(
                null,
                new Item.ItemBuilder("NAME3", null, null)
                        .setUnitId(new UnitId(null, 789L, "ARTICLE3"))
                        .setLifeTime(3000)
                        .build(),
                null
        );


        Consignment fourthConsignment = new Consignment(
                null,
                new Item.ItemBuilder("NAME4", null, null)
                        .setUnitId(new UnitId(null, 987L, "ARTICLE4"))
                        .setLifeTime(4000)
                        .setVendorCodes(Arrays.asList("123", "456", "ABC"))
                        .build(),
                null
        );

        Consignment fifthConsignment = new Consignment(
                null,
                new Item.ItemBuilder("NAME5", null, null)
                        .setUnitId(new UnitId(null, 987L, "ARTICLE5"))
                        .setLifeTime(5000)
                        .setVendorCodes(Arrays.asList("123", null, "ABC"))
                        .build(),
                null
        );

        Consignment sixthConsignment = new Consignment(
                null,
                new Item.ItemBuilder("NAME6", null, null)
                        .setUnitId(new UnitId(null, 987L, "ARTICLE6"))
                        .setLifeTime(6000)
                        .setVendorCodes(Arrays.asList(null, null, null))
                        .build(),
                null
        );

        Inbound inbound = new Inbound.InboundBuilder(null, null, ImmutableList.of(firstConsignment, secondConsignment, thirdConsignment,
                fourthConsignment, fifthConsignment, sixthConsignment), null).build();

        MarschrouteUpdateProductsRequest converted = converter.convert(inbound);

        List<MarschrouteUpdateProductRequest> convertedItems = converted.getItems();

        assertConvertedItem(convertedItems.get(0), firstConsignment.getItem(), "NAME1 (ARTICLE1)");
        assertConvertedItem(convertedItems.get(1), secondConsignment.getItem(), "NAME2 (ARTICLE2)");
        assertConvertedItem(convertedItems.get(2), thirdConsignment.getItem(), "NAME3 (ARTICLE3)");
        assertConvertedItem(convertedItems.get(3), fourthConsignment.getItem(), "NAME4 (123, 456, ABC)");
        assertConvertedItem(convertedItems.get(4), fifthConsignment.getItem(), "NAME5 (123, ABC)");
        assertConvertedItem(convertedItems.get(5), sixthConsignment.getItem(), "NAME6 (ARTICLE6)");
    }

    private void assertConvertedItem(MarschrouteUpdateProductRequest actualConvertedItem,
                                     Item initialItem, String expectedConvertedItemName) {
        softly.assertThat(actualConvertedItem.getItemId())
            .as("Asserting the item id")
            .isEqualTo(ItemIdentifierUtil.toItemIdentifier(initialItem.getUnitId()));
        softly.assertThat(actualConvertedItem.getName())
            .as("Asserting the item name")
            .isEqualTo(expectedConvertedItemName);
        softly.assertThat(actualConvertedItem.getLifetime())
            .as("Asserting that the item lifetime is null")
            .isNull(); // TODO: DELIVERY-5908.
        softly.assertThat(actualConvertedItem.getComment())
            .as("Asserting the item comment")
            .isEqualTo(Optional.ofNullable(initialItem.getComment()).orElse(""));
    }
}
