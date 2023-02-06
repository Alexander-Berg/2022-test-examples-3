package ru.yandex.market.logistic.api.client.fulfillment;


import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.Tax;
import ru.yandex.market.logistic.api.model.fulfillment.TaxType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.VatValue;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetReferenceItemsResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemReference;
import ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class GetReferenceItemsTest extends CommonServiceClientTest {

    private static final int UNIT_IDS_COUNT = 3;
    private static final int LIMIT = 10;
    private static final int OFFSET = 100;

    private static final String FIRST_ITEM_ID = "1005614218901";
    private static final String SECOND_ITEM_ID = "1005614218902";
    private static final String THIRD_ITEM_ID = "1005614218903";

    @Test
    void getReferenceItemsSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_reference_items", PARTNER_URL);

        GetReferenceItemsResponse actualResponse = fulfillmentClient.getReferenceItems(
            LIMIT,
            OFFSET,
            DtoFactory.generateUnitIds(UNIT_IDS_COUNT),
            getPartnerProperties()
        );

        GetReferenceItemsResponse expectedResponse = createGetReferenceItemsResponse(
            createItem(FIRST_ITEM_ID), createItem(SECOND_ITEM_ID), createItem(THIRD_ITEM_ID));

        assertEquals(expectedResponse, actualResponse, "Должен вернуть корректный ответ GetReferenceItemsResponse");
    }

    @Test
    void getReferenceItemsWithoutItemElementSucceeded() throws Exception {
        prepareMockServiceNormalized(
            "ff_get_reference_items",
            "ff_get_reference_items_without_item_element",
            PARTNER_URL
        );

        GetReferenceItemsResponse actualResponse = fulfillmentClient.getReferenceItems(
            LIMIT,
            OFFSET,
            DtoFactory.generateUnitIds(UNIT_IDS_COUNT),
            getPartnerProperties()
        );

        GetReferenceItemsResponse expectedResponse = createGetReferenceItemsResponse(null, null, null);

        assertEquals(expectedResponse, actualResponse, "Должен вернуть корректный ответ GetReferenceItemsResponse");
    }

    @Test
    void getReferenceItemsWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_get_reference_items",
            "ff_get_reference_items_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getReferenceItems(
                LIMIT,
                OFFSET,
                DtoFactory.generateUnitIds(UNIT_IDS_COUNT),
                getPartnerProperties()
            )
        );
    }

    @Test
    void getReferenceItemsEmptyResponseSucceeded() throws Exception {
        prepareMockServiceNormalized(
            "ff_get_reference_items",
            "ff_get_reference_items_empty",
            PARTNER_URL
        );

        fulfillmentClient.getReferenceItems(
            LIMIT, OFFSET,
            DtoFactory.generateUnitIds(UNIT_IDS_COUNT),
            getPartnerProperties());
    }

    @Test
    void getReferenceItemsListWithOneEmptyItemReferenceSucceeded() throws Exception {
        prepareMockServiceNormalized(
                "ff_get_reference_items",
                "ff_get_reference_items_list_with_one_empty_item_reference",
                PARTNER_URL
        );

        fulfillmentClient.getReferenceItems(
                LIMIT, OFFSET,
                DtoFactory.generateUnitIds(UNIT_IDS_COUNT),
                getPartnerProperties());
    }

    private GetReferenceItemsResponse createGetReferenceItemsResponse(Item firstItem, Item secondItem, Item thirdItem) {
        Korobyte korobyte = new Korobyte.KorobyteBuiler(1, 2, 3, BigDecimal.TEN)
            .setWeightNet(BigDecimal.valueOf(9))
            .setWeightTare(BigDecimal.ONE)
            .build();

        List<ItemReference> itemReferences = Arrays.asList(
            new ItemReference(
                new UnitId("id0", 0L, "article0"),
                korobyte,
                30,
                ImmutableSet.of(new Barcode.BarcodeBuilder("BARCODE").build()),
                firstItem
            ),
            new ItemReference(
                new UnitId("id1", 1L, "article1"),
                null,
                30,
                null,
                secondItem
            ),
            new ItemReference(
                new UnitId("id2", 2L, "article2"),
                korobyte,
                30,
                null,
                thirdItem
            )
        );
        return new GetReferenceItemsResponse(itemReferences);
    }

    private Item createItem(String id) {
        return new Item.ItemBuilder("Novosvit Лифтинг-полоски для области вокруг глаз (12 шт.)", 1,
            BigDecimal.valueOf(226))
            .setUnitId(new UnitId(id, 549309L, "4607086562215"))
            .setArticle("475690556620.4607086562215")
            .setTax(new Tax(TaxType.VAT, VatValue.TWENTY))
            .setCargoType(CargoType.UNKNOWN)
            .build();
    }
}
