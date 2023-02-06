package ru.yandex.market.logistics.cte.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.yandex.market.logistics.cte.client.dto.GetUnitsRequestDTO;
import ru.yandex.market.logistics.cte.client.dto.GetUnitsResponseDTO;
import ru.yandex.market.logistics.cte.client.dto.QualityAttributeDTO;
import ru.yandex.market.logistics.cte.client.dto.QualityAttributesResponseDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemIdentifier;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemRequestDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemStockDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemUUIDResponseDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemUpdateBatchRequestDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemUpdateBatchResponseDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemUpdateRequestDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemWithAttributesDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemsStockDTO;
import ru.yandex.market.logistics.cte.client.dto.TransportationUnitRequestDTO;
import ru.yandex.market.logistics.cte.client.dto.UnitDTO;
import ru.yandex.market.logistics.cte.client.enums.MatrixType;
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType;
import ru.yandex.market.logistics.cte.client.enums.RegistryType;
import ru.yandex.market.logistics.cte.client.enums.StockType;
import ru.yandex.market.logistics.cte.client.enums.SupplyItemAttributeType;
import ru.yandex.market.logistics.cte.client.enums.SupplyItemIdentifierType;
import ru.yandex.market.logistics.cte.client.enums.UnitType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public abstract class BaseFulfillmentCteClientTest {

    protected static final long YANDEX_SUPPLY_ID = 123L;
    protected static final String UUID = "uuid1";
    private static final List<Long> ATTRIBUTE_IDS = List.of(201L, 202L);
    private static final List<String> UNIT_LABELS = List.of("boxId1", "palletId1");
    private static final long SERVICE_ID = 100L;
    private static final String UUID2 = "uuid2";
    private static final String EXTERNAL_SKU = "ROVexternalSku1";
    private static final String EXTERNAL_SKU2 = "ROVexternalSku2";
    private static final String MARKET_SHOP_SKU = "marketShopSku1";
    private static final String MATRIX_TYPE = MatrixType.FULFILLMENT.getString();
    private static final long VENDOR_ID = 10264169L;
    private static final String BOX_ID = "boxId1";
    private static final String BOX_ID2 = "boxId2";
    private static final String ORDER_ID = "orderId1";
    private static final String ORDER_ID2 = "orderId2";
    private static final int CATEGORY_ID = 3;
    private static final BigDecimal PRICE = new BigDecimal("11.5");
    private static final String IDENTIFIER_VALUE = "123456789";
    private static final String CONSIGNOR_SUPPLY_ID = "100500";
    private static final String CONSIGNOR_NAME = "consignor_name";
    private static final String FULFILLMENT_SUPPLY_ID = "ff_supply_id";
    private static final String CREATED_BY = "operator3";
    private static final String CREATED_BY2 = "operator4";
    private static final String SUPPLY_ITEM_IDENTIFIER_VALUE_1 = "value1";
    private static final String SUPPLY_ITEM_IDENTIFIER_VALUE_2 = "value2";
    private static final String UIT = "1uituit";
    private static final String UIT2 = "2uituit";
    private static final String NAME = "itemName";
    private static final String WAREHOUSE_ID = "500";

    protected void evaluateResupplyItemAndCheckResult(FulfillmentCteClientApi clientApi) {
        SupplyItemDTO result = clientApi.evaluateResupplyItem(YANDEX_SUPPLY_ID, UUID,
                returnItem(CATEGORY_ID, BOX_ID, ORDER_ID, NAME, WAREHOUSE_ID));


        assertThat(result, notNullValue());
        assertThat(result.getUuid(), equalTo(UUID));

        assertThat(result.getSupplyDTO().getConsignorName(), equalTo(CONSIGNOR_NAME));
        assertThat(result.getSupplyDTO().getConsignorSupplyId(), equalTo(CONSIGNOR_SUPPLY_ID));
        assertThat(result.getSupplyDTO().getFulfillmentSupplyId(), equalTo(FULFILLMENT_SUPPLY_ID));
        assertThat(result.getSupplyDTO().getRegistryType(), equalTo(RegistryType.REFUND));

        assertThat(result.getExternalSku(), equalTo(EXTERNAL_SKU));
        assertThat(result.getVendorId(), equalTo(VENDOR_ID));
        assertThat(result.getBoxId(), equalTo(BOX_ID));
        assertThat(result.getOrderId(), equalTo(ORDER_ID));
        assertThat(result.getName(), equalTo(NAME));
        assertThat(result.getWarehouseId(), equalTo(WAREHOUSE_ID));
        assertThat(result.getCategoryId(), equalTo(CATEGORY_ID));
        assertThat(result.getPrice(), equalTo(PRICE));
        assertThat(result.getStockType(), equalTo(StockType.OK));


        assertThat(result.getAttributes(),
                containsInAnyOrder("PACKAGE_SCRATCHES", "PACKAGE_JAMS"));

        assertThat(result.getCreatedBy(), equalTo(CREATED_BY));
        assertThat(result.getCreatedAt(),
                equalTo(LocalDateTime.of(2020, 10, 10, 10, 0, 0)));
    }

    protected void evaluateResupplyItemForNullValuesAndCheck(FulfillmentCteClientApi clientApi) {

        SupplyItemDTO result = clientApi.evaluateResupplyItem(YANDEX_SUPPLY_ID, UUID,
                returnItem(0, null, null, null, null));

        assertThat(result.getBoxId(), equalTo(null));
        assertThat(result.getOrderId(), equalTo(null));
        assertThat(result.getCategoryId(), equalTo(0));
    }

    protected void updateResupplyItemsAndCheck(FulfillmentCteClientApi clientApi) {
        SupplyItemUpdateBatchResponseDTO result = clientApi.updateResupplyItems(returnBatchItems());

        List<SupplyItemDTO> resultItems = result.getItems();
        assertThat(resultItems, hasSize(2));
        SupplyItemDTO resultItem = resultItems.get(0);
        assertThat(resultItem, notNullValue());
        assertThat(resultItem.getUuid(), equalTo(UUID));

        assertThat(resultItem.getSupplyDTO().getConsignorName(), equalTo(CONSIGNOR_NAME));
        assertThat(resultItem.getSupplyDTO().getConsignorSupplyId(), equalTo(CONSIGNOR_SUPPLY_ID));
        assertThat(resultItem.getSupplyDTO().getFulfillmentSupplyId(), equalTo(FULFILLMENT_SUPPLY_ID));

        assertThat(resultItem.getExternalSku(), equalTo(EXTERNAL_SKU));
        assertThat(resultItem.getVendorId(), equalTo(VENDOR_ID));
        assertThat(resultItem.getBoxId(), equalTo(BOX_ID));
        assertThat(resultItem.getOrderId(), equalTo(ORDER_ID));
        assertThat(resultItem.getCategoryId(), equalTo(CATEGORY_ID));
        assertThat(resultItem.getPrice(), equalTo(PRICE));
        assertThat(resultItem.getStockType(), equalTo(StockType.OK));
        assertThat(resultItem.getUit(), equalTo(UIT));


        assertThat(resultItem.getAttributes(),
                containsInAnyOrder("PACKAGE_SCRATCHES", "PACKAGE_JAMS"));

        assertThat(resultItem.getCreatedBy(), equalTo(CREATED_BY));
        assertThat(resultItem.getCreatedAt(),
                equalTo(LocalDateTime.of(2020, 10, 10, 10, 0, 0)));

        resultItem = resultItems.get(1);
        assertThat(resultItem, notNullValue());
        assertThat(resultItem.getUuid(), equalTo(UUID2));

        assertThat(resultItem.getSupplyDTO().getConsignorName(), equalTo(CONSIGNOR_NAME));
        assertThat(resultItem.getSupplyDTO().getConsignorSupplyId(), equalTo(CONSIGNOR_SUPPLY_ID));
        assertThat(resultItem.getSupplyDTO().getFulfillmentSupplyId(), equalTo(FULFILLMENT_SUPPLY_ID));

        assertThat(resultItem.getExternalSku(), equalTo(EXTERNAL_SKU2));
        assertThat(resultItem.getVendorId(), equalTo(VENDOR_ID));
        assertThat(resultItem.getBoxId(), equalTo(BOX_ID2));
        assertThat(resultItem.getOrderId(), equalTo(ORDER_ID2));
        assertThat(resultItem.getCategoryId(), equalTo(CATEGORY_ID));
        assertThat(resultItem.getPrice(), equalTo(PRICE));
        assertThat(resultItem.getStockType(), equalTo(StockType.OK));
        assertThat(resultItem.getUit(), equalTo(UIT2));

        assertThat(resultItem.getAttributes(),
                containsInAnyOrder("PACKAGE_SCRATCHES", "PACKAGE_JAMS"));

        assertThat(resultItem.getCreatedBy(), equalTo(CREATED_BY2));
        assertThat(resultItem.getCreatedAt(),
                equalTo(LocalDateTime.of(2020, 10, 10, 10, 0, 0)));
    }

    protected void resolveQualityAttributesAndCheck(FulfillmentCteClientApi clientApi) {

        QualityAttributesResponseDTO result =
                clientApi.resolveQualityAttributes(CATEGORY_ID, VENDOR_ID, MARKET_SHOP_SKU);

        assertThat(result, notNullValue());

        QualityAttributesResponseDTO expectedQualityAttributes = returnQualityAttributes();

        assertThat(result, equalTo(expectedQualityAttributes));
    }

    protected void resolveQualityAttributesWithMatrixTypeAndCheck(FulfillmentCteClientApi clientApi) {

        QualityAttributesResponseDTO result =
                clientApi.resolveQualityAttributes(CATEGORY_ID, VENDOR_ID, MARKET_SHOP_SKU, MATRIX_TYPE);

        assertThat(result, notNullValue());

        QualityAttributesResponseDTO expectedQualityAttributes = returnQualityAttributes();

        assertThat(result, equalTo(expectedQualityAttributes));
    }

    protected void resolveQualityAttributesByUnitTypeAndCheck(FulfillmentCteClientApi clientApi) {

        QualityAttributesResponseDTO result =
                clientApi.resolveQualityAttributes(UnitType.BOX);

        assertThat(result, notNullValue());

        QualityAttributesResponseDTO expectedQualityAttributes = returnQualityAttributesByUnitType();

        assertThat(result, equalTo(expectedQualityAttributes));
    }

    protected void resolveQualityAttributesByUnitTypeAndMatrixTypeAndCheck(FulfillmentCteClientApi clientApi) {

        QualityAttributesResponseDTO result =
                clientApi.resolveQualityAttributes(UnitType.BOX, MATRIX_TYPE);

        assertThat(result, notNullValue());

        QualityAttributesResponseDTO expectedQualityAttributes = returnQualityAttributesByUnitType();

        assertThat(result, equalTo(expectedQualityAttributes));
    }

    protected void updateItemsStockTypeAndCheck(FulfillmentCteClientApi clientApi) {
        List<SupplyItemStockDTO> items = List.of(
                new SupplyItemStockDTO("uit1", "sku1", 111, StockType.ASC),
                new SupplyItemStockDTO("uit2", "sku2", 222, StockType.ASC),
                new SupplyItemStockDTO("uit3", "sku3", 333, StockType.DAMAGE)
        );
        var result = clientApi.updateItemsStockType(new SupplyItemsStockDTO(items));
        assertThat(result, notNullValue());
        assertThat(result.getItems(), emptyCollectionOf(SupplyItemStockDTO.class));
    }

    protected void getAscStockItemsAndCheck(FulfillmentCteClientApi clientApi) {
        List<SupplyItemStockDTO> items = List.of(
                new SupplyItemStockDTO("uit1", "sku1", 111, StockType.ASC),
                new SupplyItemStockDTO("uit2", "sku2", 222, StockType.ASC)
        );
        var result = clientApi.getAscStockItems();
        assertThat(result, notNullValue());
        assertThat(result.getItems(), equalTo(items));
    }

    private SupplyItemRequestDTO returnItem(Integer categoryId, String boxId, String orderId,
                                            String name, String warehouseId) {
        SupplyDTO supplyDTO = new SupplyDTO(CONSIGNOR_SUPPLY_ID, CONSIGNOR_NAME, FULFILLMENT_SUPPLY_ID,
                RegistryType.REFUND);
        SupplyItemIdentifier itemIdentifier =
                new SupplyItemIdentifier(IDENTIFIER_VALUE, SupplyItemIdentifierType.CIS);

        return SupplyItemRequestDTO.builder()
                .externalSku(EXTERNAL_SKU)
                .marketShopSku(MARKET_SHOP_SKU)
                .vendorId(VENDOR_ID)
                .categoryId(categoryId)
                .boxId(boxId)
                .orderId(orderId)
                .name(name)
                .warehouseId(warehouseId)
                .price(PRICE)
                .identifiers(Set.of(itemIdentifier))
                .criteria(Set.of(SupplyItemAttributeType.DISPLAY_BROKEN, SupplyItemAttributeType.PACKAGE_CONTAMINATION))
                .createdBy(CREATED_BY)
                .expiredAndDamaged(false)
                .supplyDTO(supplyDTO)
                .build();
    }

    protected void evaluateTransportationUnitMinimalRequest(FulfillmentCteClientApi clientApi) {
        clientApi.evaluateTransportationUnit(YANDEX_SUPPLY_ID,
            new TransportationUnitRequestDTO(BOX_ID, SERVICE_ID, null, null));
    }

    protected void evaluateTransportationUnitLessThanMinimalRequest(FulfillmentCteClientApi clientApi) {
        clientApi.evaluateTransportationUnit(YANDEX_SUPPLY_ID,
            new TransportationUnitRequestDTO(null, SERVICE_ID, null, null));
    }

    protected void evaluateTransportationUnitNormalRequest(FulfillmentCteClientApi clientApi) {
        clientApi.evaluateTransportationUnit(YANDEX_SUPPLY_ID,
            new TransportationUnitRequestDTO(UnitType.PALLET, BOX_ID, SERVICE_ID,
                getSomeMeta(), ATTRIBUTE_IDS));
    }

    protected void getQualityAttributesForUnitLabels(FulfillmentCteClientApi clientApi) {
        GetUnitsResponseDTO result = clientApi.getQualityAttributesForUnitLabels(YANDEX_SUPPLY_ID,
                new GetUnitsRequestDTO(SERVICE_ID, UNIT_LABELS));

        assertThat(result, notNullValue());

        GetUnitsResponseDTO expected = getQualityAttributesForUnitLabels();

        assertThat(result, equalTo(expected));
    }

    protected void getSupplyItemsBySupplyId(FulfillmentCteClientApi clientApi) {
        SupplyItemUUIDResponseDTO result = clientApi.getSupplyItemsBySupplyId(YANDEX_SUPPLY_ID);

        assertThat(result, notNullValue());

        SupplyItemUUIDResponseDTO expected = getSupplyItemsBySupplyId();

        assertThat(result, equalTo(expected));
    }

    private SupplyItemUUIDResponseDTO getSupplyItemsBySupplyId() {
        return new SupplyItemUUIDResponseDTO(List.of(
                new SupplyItemWithAttributesDTO(1,
                        null,
                        "uuid1",
                        10264169,
                        "5467465",
                        "453453",
                        6,
                        StockType.DAMAGE_RESELL,
                        "shopSku3",
                        "ROVSku3",
                        "111",
                        "PACKAGE_HOLES,DISPLAY_BROKEN",
                        10,
                        "ff_supply_id",
                        "171",
                        LocalDateTime.of(2021, 12, 8, 10, 55, 45)
                ),
                new SupplyItemWithAttributesDTO(
                        2,
                        null,
                        "uuid2",
                        10264169,
                        "4565467",
                        "45674567",
                        6,
                        StockType.DAMAGE_RESELL,
                        "shopSku3",
                        "ROVSku3",
                        "222",
                        "PACKAGE_SCRATCHES,PACKAGE_JAMS,DISPLAY_BROKEN",
                        10,
                        "ff_supply_id",
                        "171",
                        LocalDateTime.of(2021, 12, 8, 10, 55, 45)
                )
        ));
    }


    private Map<String, Object> getSomeMeta() {
        return Map.of(
            "orderId", ORDER_ID,
            "vendorId", VENDOR_ID,
            "objList", List.of(
                ORDER_ID,
                VENDOR_ID,
                Map.of(
                    "orderId", ORDER_ID,
                    "vendorId", VENDOR_ID
                )
            )
        );
    }

    private SupplyItemUpdateBatchRequestDTO returnBatchItems() {
        return new SupplyItemUpdateBatchRequestDTO(
                YANDEX_SUPPLY_ID,
                List.of(
                        new SupplyItemUpdateRequestDTO("uuid100", "uit100"),
                        new SupplyItemUpdateRequestDTO("uuid200", "uit200")
                ));
    }

    private QualityAttributesResponseDTO returnQualityAttributes() {
        return new QualityAttributesResponseDTO(
                Set.of(
                        new QualityAttributeDTO(
                                1,
                                "PACKAGE_SCRATCHES",
                                "PACKAGE_SCRATCHES",
                                "1.1",
                                QualityAttributeType.PACKAGE,
                                "Царапины (Свыше 10% площади сто0роны)"
                        ), new QualityAttributeDTO(
                                4,
                                "PACKAGE_HOLES",
                                "PACKAGE_HOLES",
                                "1.4",
                                QualityAttributeType.PACKAGE,
                                "Отверстия (Исключающие повреждение товара, утерю комплектующих)"
                        )
                )
        );
    }

    private QualityAttributesResponseDTO returnQualityAttributesByUnitType() {
        return new QualityAttributesResponseDTO(
                Set.of(
                        new QualityAttributeDTO(
                                201,
                                "TRANSPORTATION_UNIT_MOISTURE_TRACES",
                                "TRANSPORTATION_UNIT_MOISTURE_TRACES",
                                "2.1",
                                QualityAttributeType.TRANSPORTATION_UNIT,
                                "Следы влаги"
                        ), new QualityAttributeDTO(
                                208,
                                "TRANSPORTATION_UNIT_MANIPULATION_SYMBOLS",
                                "TRANSPORTATION_UNIT_MANIPULATION_SYMBOLS",
                                "2.8",
                                QualityAttributeType.TRANSPORTATION_UNIT,
                                "Манипуляционные знаки"
                        )
                )
        );
    }

    private GetUnitsResponseDTO getQualityAttributesForUnitLabels() {
        return new GetUnitsResponseDTO(
                List.of(
                    new UnitDTO(
                        "boxId1",
                        List.of(
                            new QualityAttributeDTO(
                                201,
                                "TRANSPORTATION_UNIT_MOISTURE_TRACES",
                                "TRANSPORTATION_UNIT_MOISTURE_TRACES",
                                "2.1",
                                QualityAttributeType.TRANSPORTATION_UNIT,
                                "Следы влаги"
                            ),
                            new QualityAttributeDTO(
                                202,
                                "PACKAGE_CONTAMINATION",
                                "Загрязнения",
                                "2.2",
                                QualityAttributeType.PACKAGE,
                                "Загрязнения, следы влаги (Пятна, протечки)"
                            )
                        )
                    ),
                    new UnitDTO(
                        "palletId1",
                        List.of(
                            new QualityAttributeDTO(
                                203,
                                "WRONG_OR_DAMAGED_LABELS",
                                "Повреждена этикетка",
                                "2.3",
                                QualityAttributeType.ITEM,
                                "Повреждена этикетка (Отсутствие или нарушение заводской этикетки, " +
                                    "печатных изданий)"
                            ),
                            new QualityAttributeDTO(
                                204,
                                "WRONG_OR_DAMAGED_SN",
                                "Повреждён Серийный номер",
                                "2.4",
                                QualityAttributeType.ITEM,
                                "Повреждён Серийный номер (Отсутствие или нарушение Серийного номера)"
                            )
                        )
                    )
                )
        );
    }
}
