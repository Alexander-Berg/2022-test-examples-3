package ru.yandex.market.logistics.lom.service.validation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.MarketIdModelFactory;
import ru.yandex.market.logistics.lom.entity.Location;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.LocationType;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WarehouseLegalInfoValidatorAndEnricher;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("WarehouseLegalInfoValidatorAndEnricher")
class WarehouseLegalInfoValidatorAndEnricherTest extends AbstractTest {
    private final MarketIdService marketIdService = mock(MarketIdService.class);
    private final WarehouseLegalInfoValidatorAndEnricher warehouseLegalInfoValidatorAndEnricher =
        new WarehouseLegalInfoValidatorAndEnricher(marketIdService);
    private final Map<Long, PartnerType> partnerTypeMap = Map.of(
        1L, PartnerType.DELIVERY,
        2L, PartnerType.FULFILLMENT,
        3L, PartnerType.SORTING_CENTER,
        4L, PartnerType.DROPSHIP,
        5L, PartnerType.YANDEX_GO_SHOP
    );
    private final Map<Long, Long> warehouseIdToMarketId = Map.of(
        1L, 10L,
        2L, 20L,
        3L, 30L,
        4L, 40L,
        5L, 50L
    );
    private ValidateAndEnrichContext context;

    @BeforeEach
    void setUp() {
        context = new ValidateAndEnrichContext();
        context.setReturnWarehouse(LogisticsPointResponse.newBuilder()
            .id(3L)
            .partnerId(3L)
            .type(PointType.WAREHOUSE)
            .name("WAREHOUSE")
            .build());
        context.setPartnerTypeById(partnerTypeMap);
    }

    @Test
    @DisplayName("Не найдена юр инфа")
    void marketIdAccountNotFound() {
        Order order = createOrder()
            .setWaybill(List.of(
                createSegment(PartnerType.FULFILLMENT, 4, 2),
                createSegment(PartnerType.DELIVERY, 2, 1),
                createSegment(PartnerType.DELIVERY, 1, 1)
            ));
        ValidateAndEnrichResults results = warehouseLegalInfoValidatorAndEnricher.validateAndEnrich(
            order,
            context
        );
        softly.assertThat(results.isValidationPassed()).isFalse();
        softly.assertThat(results.getErrorMessage())
            .isEqualTo("Warehouses with ids [%s] have no incorporation", List.of(1, 2, 3, 4));
    }

    @Test
    @DisplayName("Успешная валидация и обогащение заказа Беру с наличием marketId у склада")
    void validationPassedForBeruWithMarketId() {
        context.setWarehouseIdToMarketId(warehouseIdToMarketId);
        warehouseIdToMarketId.values().forEach(
            marketId -> when(marketIdService.findAccountById(eq(marketId)))
                .thenReturn(Optional.of(MarketIdModelFactory.marketAccount(marketId)))
        );
        WaybillSegment fulfillmentSegment = createSegment(PartnerType.FULFILLMENT, 4, 2);
        Order order = createOrder()
            .setPlatformClient(PlatformClient.BERU)
            .setWaybill(List.of(
                fulfillmentSegment,
                createSegment(PartnerType.DELIVERY, 2, 1),
                createSegment(PartnerType.DELIVERY, 1, 1)
            ));
        validationPassed(order, fulfillmentSegment);
    }

    @Test
    @DisplayName("Успешная валидация и обогащение заказа Беру с DROPSHIP-сегментом")
    void validationPassedForBeruDropship() {
        context.setWarehouseIdToMarketId(warehouseIdToMarketId);
        warehouseIdToMarketId.values().forEach(
            marketId -> when(marketIdService.findAccountById(eq(marketId)))
                .thenReturn(Optional.of(MarketIdModelFactory.marketAccount(marketId)))
        );

        WaybillSegment dropshipSegment = createSegment(PartnerType.DROPSHIP, 1, 4, 5);
        WaybillSegment sortingCenterSegment = createSegment(PartnerType.SORTING_CENTER, 2, 3);
        List<WaybillSegment> waybill = List.of(
            dropshipSegment,
            sortingCenterSegment,
            createSegment(PartnerType.DELIVERY, 3, 1)
        );
        Order order = createOrder().setPlatformClient(PlatformClient.BERU).setWaybill(waybill);
        validationPassed(order, dropshipSegment, sortingCenterSegment);
    }

    @Test
    @DisplayName("Неудачная валидация и обогащение заказа Беру с отсутствием marketId у склада")
    void validationFailedForBeruWithoutMarketId() {
        context.setWarehouseIdToMarketId(Map.of());

        Order order = createOrder()
            .setPlatformClient(PlatformClient.BERU)
            .setWaybill(List.of(
                createSegment(PartnerType.FULFILLMENT, 4, 2),
                createSegment(PartnerType.DELIVERY, 2, 1),
                createSegment(PartnerType.DELIVERY, 1, 1)
            ));
        ValidateAndEnrichResults results = warehouseLegalInfoValidatorAndEnricher.validateAndEnrich(order, context);

        verify(marketIdService, never()).findAccountByPartnerIdAndPartnerType(any(), any());

        softly.assertThat(results.isValidationPassed()).isFalse();
        softly.assertThat(results.getErrorMessage())
            .isEqualTo("Warehouses with ids [%s] have no incorporation", List.of(1, 2, 3, 4));
    }

    @Test
    @DisplayName("Успешная валидация и обогащение заказа DAAS с наличием marketId у склада")
    void validationPassedForDaasWithMarketId() {
        context.setWarehouseIdToMarketId(warehouseIdToMarketId);

        warehouseIdToMarketId.values().forEach(
            marketId -> when(marketIdService.findAccountById(eq(marketId)))
                .thenReturn(Optional.of(MarketIdModelFactory.marketAccount(marketId)))
        );
        WaybillSegment fulfillmentSegment = createSegment(PartnerType.FULFILLMENT, 4, 2);
        Order order = createOrder()
            .setPlatformClient(PlatformClient.YANDEX_DELIVERY)
            .setWaybill(List.of(
                fulfillmentSegment,
                createSegment(PartnerType.DELIVERY, 2, 1),
                createSegment(PartnerType.DELIVERY, 1, 1)
            ));
        validationPassed(order, fulfillmentSegment);
    }

    @Test
    @DisplayName("Неудачная валидация и обогащение заказа DAAS с отсутствием marketId у склада")
    void validationPassedForDaasWithoutMarketId() {
        context.setWarehouseIdToMarketId(Map.of());

        Order order = createOrder()
            .setPlatformClient(PlatformClient.YANDEX_DELIVERY)
            .setWaybill(List.of(
                createSegment(PartnerType.FULFILLMENT, 4, 2),
                createSegment(PartnerType.DELIVERY, 2, 1),
                createSegment(PartnerType.DELIVERY, 1, 1)
            ));
        ValidateAndEnrichResults results = warehouseLegalInfoValidatorAndEnricher.validateAndEnrich(order, context);

        verify(marketIdService, never()).findAccountByPartnerIdAndPartnerType(any(), any());

        softly.assertThat(results.isValidationPassed()).isFalse();
        softly.assertThat(results.getErrorMessage())
            .isEqualTo("Warehouses with ids [%s] have no incorporation", List.of(1, 2, 3, 4));
    }

    @Test
    @DisplayName("Успешная валидация и обогащение заказа YandexGo с наличием marketId у склада")
    void validationPassedForYandexGoShopWithMarketId() {
        context.setWarehouseIdToMarketId(warehouseIdToMarketId);

        warehouseIdToMarketId.values().forEach(
            marketId -> when(marketIdService.findAccountById(eq(marketId)))
                .thenReturn(Optional.of(MarketIdModelFactory.marketAccount(marketId)))
        );
        WaybillSegment yandexGoSegment = createSegment(PartnerType.YANDEX_GO_SHOP, 4, 5);
        Order order = createOrder()
            .setPlatformClient(PlatformClient.YANDEX_GO)
            .setWaybill(List.of(
                yandexGoSegment,
                createSegment(PartnerType.DELIVERY, 2, 1),
                createSegment(PartnerType.DELIVERY, 1, 1)
            ));
        validationPassed(order, yandexGoSegment);
    }

    @Test
    @DisplayName("Неудачная валидация и обогащение заказа YandexGo с отсутствием marketId у склада")
    void validationPassedForYandexGoShopWithoutMarketId() {
        context.setWarehouseIdToMarketId(Map.of());

        Order order = createOrder()
            .setPlatformClient(PlatformClient.YANDEX_GO)
            .setWaybill(List.of(
                createSegment(PartnerType.YANDEX_GO_SHOP, 4, 5),
                createSegment(PartnerType.DELIVERY, 2, 1),
                createSegment(PartnerType.DELIVERY, 1, 1)
            ));
        ValidateAndEnrichResults results = warehouseLegalInfoValidatorAndEnricher.validateAndEnrich(order, context);

        verify(marketIdService, never()).findAccountByPartnerIdAndPartnerType(any(), any());

        softly.assertThat(results.isValidationPassed()).isFalse();
        softly.assertThat(results.getErrorMessage())
            .isEqualTo("Warehouses with ids [%s] have no incorporation", List.of(1, 2, 3, 4));
    }

    void validationPassed(Order order, WaybillSegment... incorporationSegments) {
        var legalName = MarketIdModelFactory.marketAccount(0).getLegalInfo().getLegalName();

        ValidateAndEnrichResults results = warehouseLegalInfoValidatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();

        results.getOrderModifier().apply(order);
        softly.assertThat(order.getReturnSortingCenterWarehouse().getIncorporation()).isEqualTo(legalName);
        softly.assertThat(incorporationSegments)
            .extracting(s -> s.getWaybillShipment().getLocationFrom().getIncorporation())
            .allMatch(legalName::equals);
        softly.assertThat(incorporationSegments)
            .extracting(s -> s.getWarehouseLocation().getIncorporation())
            .allMatch(legalName::equals);
    }

    private Order createOrder() {
        return new Order()
            .setReturnSortingCenterId(3L)
            .setReturnSortingCenterWarehouse(createLocation(3));
    }

    private WaybillSegment createSegment(PartnerType partnerType, long id, long partnerId) {
        return createSegment(partnerType, id, partnerId, id);
    }

    private WaybillSegment createSegment(PartnerType partnerType, long id, long partnerId, long warehouseLocationId) {
        var location = createLocation(id);
        var shipment = new WaybillSegment.WaybillShipment()
            .setLocationFrom(location)
            .setLocationTo(createLocation(99L));

        return new WaybillSegment()
            .setPartnerType(partnerType)
            .setPartnerId(partnerId)
            .setWaybillSegmentIndex((int) id)
            .setWaybillShipment(shipment)
            .setWarehouseLocation(createLocation(warehouseLocationId));
    }

    private Location createLocation(long id) {
        return new Location()
            .setType(LocationType.WAREHOUSE)
            .setWarehouseId(id)
            .setIncorporation(null);
    }
}
