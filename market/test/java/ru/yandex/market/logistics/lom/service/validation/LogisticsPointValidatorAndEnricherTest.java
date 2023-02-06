package ru.yandex.market.logistics.lom.service.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.LmsModelFactory;
import ru.yandex.market.logistics.lom.converter.EnumConverter;
import ru.yandex.market.logistics.lom.converter.lms.AddressLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.ContactLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.DeliveryTypeLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.LogisticsPointLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.PartnerExternalParamLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.PartnerTypeLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.ScheduleDayLmsConverter;
import ru.yandex.market.logistics.lom.entity.Location;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WarehouseWorkTime;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Address;
import ru.yandex.market.logistics.lom.entity.embedded.OffsetTimeInterval;
import ru.yandex.market.logistics.lom.entity.embedded.PickupPoint;
import ru.yandex.market.logistics.lom.entity.embedded.TimeInterval;
import ru.yandex.market.logistics.lom.entity.enums.LocationType;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.LogisticsPointValidatorAndEnricher;
import ru.yandex.market.logistics.lom.lms.client.LmsLomLightClient;
import ru.yandex.market.logistics.lom.lms.model.LogisticsPointLightModel;
import ru.yandex.market.logistics.lom.lms.model.PartnerLightModel;
import ru.yandex.market.logistics.lom.service.partner.LogisticsPointsServiceImpl;
import ru.yandex.market.logistics.lom.service.partner.PartnerServiceImpl;
import ru.yandex.market.logistics.lom.utils.PlatformClientUtils;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Валидация и обогащение адресов складов")
class LogisticsPointValidatorAndEnricherTest extends AbstractTest {
    private final LmsLomLightClient lmsLomLightClient = mock(LmsLomLightClient.class);
    private final LogisticsPointValidatorAndEnricher validatorAndEnricher =
        new LogisticsPointValidatorAndEnricher(
            new LogisticsPointsServiceImpl(lmsLomLightClient),
            new LogisticsPointLmsConverter(
                new AddressLmsConverter(),
                new ScheduleDayLmsConverter(),
                new ContactLmsConverter()
            ),
            new PartnerServiceImpl(
                lmsLomLightClient,
                new DeliveryTypeLmsConverter(),
                new PartnerTypeLmsConverter(new EnumConverter()),
                new PartnerExternalParamLmsConverter()
            )
        );

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsLomLightClient);
    }

    @Test
    @DisplayName("Идентификатор склада не указан в вейбилле")
    void noWarehouseIdInWaybill() {
        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(
            new Order().setWaybill(List.of(new WaybillSegment().setWaybillShipment(
                new WaybillSegment.WaybillShipment()
                    .setLocationFrom(warehouseLocation(null))
                    .setLocationTo(new Location())
            ))),
            new ValidateAndEnrichContext().setReturnWarehouse(createReturnWarehouse().build())
        );

        assertFail(results, "There isn't warehouse id in waybill segment");
    }

    @Test
    @DisplayName("Нет информации о возвратном складе в контексте")
    void noReturnWarehouseInContext() {
        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(
            new Order(),
            new ValidateAndEnrichContext()
        );
        assertFail(results, "There isn't warehouse id in waybill segment");
    }

    @Test
    @DisplayName("Не получаем данные по складам от LMS")
    void noWarehousesFromLms() {
        Order order = new Order().setWaybill(List.of(new WaybillSegment().setWaybillShipment(
            new WaybillSegment.WaybillShipment()
                .setLocationFrom(warehouseLocation(1L))
                .setLocationTo(warehouseLocation(2L))
        )));

        Set<Long> partnerIds = Set.of(1003L);
        var partners = List.of(partnerResponse(1003L, 125L));

        when(lmsLomLightClient.getPartners(partnerIds)).thenReturn(partners);

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(
            order,
            new ValidateAndEnrichContext().setReturnWarehouse(createReturnWarehouse().build())
        );

        assertFail(results, "Failed to find logistic points by ids [1, 2]");

        verify(lmsLomLightClient).getLogisticsPointsByIds(Set.of(1L, 2L));
        verify(lmsLomLightClient).getPartners(partnerIds);
    }

    @Test
    @DisplayName("Не получаем данные по партнерам от LMS")
    void noWarehousesPartnersFromLms() {
        Order order = new Order().setWaybill(List.of(new WaybillSegment().setWaybillShipment(
            new WaybillSegment.WaybillShipment()
                .setLocationFrom(warehouseLocation(1L))
                .setLocationTo(warehouseLocation(2L))
        )));

        SearchPartnerFilter filter = partnerFilter(Set.of(1001L, 1002L, 1003L));
        Set<Long> partnerIds = Set.of(1001L, 1002L, 1003L);

        var warehouses = List.of(
            LmsModelFactory.createLogisticsPointResponse(1L, 1001L, "warehouse1", PointType.WAREHOUSE).build(),
            LmsModelFactory.createLogisticsPointResponse(2L, 1002L, "warehouse2", PointType.WAREHOUSE).build()
        );

        when(lmsLomLightClient.getLogisticsPointsByIds(Set.of(1L, 2L))).thenReturn(
            warehouses.stream()
                .map(LogisticsPointLightModel::build)
                .collect(Collectors.toList())
        );
        when(lmsLomLightClient.getPartners(partnerIds)).thenReturn(List.of());

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(
            order,
            new ValidateAndEnrichContext().setReturnWarehouse(createReturnWarehouse().build())
        );

        assertFail(results, "Logistic points [1, 2, 3] partners were without marketIds");

        verify(lmsLomLightClient).getLogisticsPointsByIds(Set.of(1L, 2L));
        verify(lmsLomLightClient).getPartners(partnerIds);
    }

    @ParameterizedTest
    @EnumSource(PointType.class)
    @DisplayName("Логистическая точка без расписания")
    void noSchedule(PointType pointType) {
        Order order = new Order()
            .setWaybill(List.of(waybillSegment(1000L)))
            .setPlatformClient(PlatformClient.BERU);

        Set<Long> pointIds = Set.of(1L, 2L, 4L);
        Set<Long> partnerIds = Set.of(1004L, 1001L, 1002L, 1003L);

        ValidateAndEnrichContext context = context(createReturnWarehouse(), null, null)
            .setPartnerTypeById(Map.of(1004L, PartnerType.DROPSHIP_BY_SELLER));

        var warehouses = List.of(
            LmsModelFactory.createLogisticsPointResponse(1L, 1001L, "warehouse1", pointType).build(),
            LmsModelFactory.createLogisticsPointResponse(2L, 1002L, "warehouse2", pointType)
                .schedule(Set.of())
                .build(),
            LmsModelFactory.createLogisticsPointResponse(4L, 1004L, "warehouse4", PointType.WAREHOUSE)
                .schedule(null)
                .build()
        );

        var partners = List.of(
            partnerResponse(1001L, 123L),
            partnerResponse(1002L, 124L),
            partnerResponse(1003L, 125L),
            partnerResponse(1004L, 126L)
        );

        when(lmsLomLightClient.getLogisticsPointsByIds(pointIds)).thenReturn(
            warehouses.stream()
                .map(LogisticsPointLightModel::build)
                .collect(Collectors.toList())
        );
        when(lmsLomLightClient.getPartners(partnerIds)).thenReturn(partners);

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(order, context);

        softly.assertThat(results.isValidationFailed()).isTrue();
        softly.assertThat(results.getErrorMessage()).isEqualTo("Logistic points [2] without schedule");
        verify(lmsLomLightClient).getLogisticsPointsByIds(pointIds);
        verify(lmsLomLightClient).getPartners(partnerIds);
    }

    @ParameterizedTest
    @DisplayName("Успешное обогащение (не ядо)")
    @MethodSource("nonYaDoPlatforms")
    void enrichingSucceeded(PlatformClient platformClient) {
        Order order = new Order().setWaybill(List.of(waybillSegment(1000L)))
            .setPlatformClient(platformClient);

        ValidateAndEnrichContext context = context(createReturnWarehouse(), null, null)
            .setPartnerTypeById(Map.of(1002L, PartnerType.DROPSHIP_BY_SELLER));

        Set<Long> pointIds = Set.of(1L, 2L, 4L);
        Set<Long> partnerIds = Set.of(1000L, 1001L, 1002L, 1003L);

        var warehouses = List.of(
            LmsModelFactory.createLogisticsPointResponse(1L, 1000L, "warehouse1", PointType.WAREHOUSE).build(),
            LmsModelFactory.createLogisticsPointResponse(2L, 1001L, "warehouse2", PointType.WAREHOUSE).build(),
            LmsModelFactory.createLogisticsPointResponse(4L, 1002L, "warehouse4", PointType.WAREHOUSE)
                .schedule(null)
                .build()
        );

        var partners = List.of(
            partnerResponse(1000L, 123L),
            partnerResponse(1001L, 124L),
            partnerResponse(1003L, 125L),
            partnerResponse(1002L, 126L)
        );
        var warehouseMap = warehouseMap(context, warehouses, partners);

        when(lmsLomLightClient.getPartners(partnerIds)).thenReturn(partners);
        when(lmsLomLightClient.getLogisticsPointsByIds(pointIds)).thenReturn(
            warehouses.stream()
                .map(LogisticsPointLightModel::build)
                .collect(Collectors.toList())
        );

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(order, context);

        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getWarehouseIdToMarketId()).isEqualTo(warehouseMap);
        Order enrichedOrder = results.getOrderModifier().apply(order);
        softly.assertThat(enrichedOrder.getWaybill().get(0).getWarehouseLocation().getWarehouseWorkTime())
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(IntStream.range(1, 8).mapToObj(this::workTimeRecord).collect(Collectors.toSet()));

        verify(lmsLomLightClient).getLogisticsPointsByIds(pointIds);
        verify(lmsLomLightClient).getPartners(partnerIds);
    }

    @Test
    @DisplayName("Успешное обогащение (ядо)")
    void enrichingSucceededYaDo() {
        Order order = new Order().setWaybill(List.of(
            new WaybillSegment()
                .setPartnerId(1000L)
                .setPartnerType(PartnerType.SORTING_CENTER)
                .setWaybillShipment(
                    new WaybillSegment.WaybillShipment()
                        .setLocationFrom(warehouseLocation(1L))
                        .setLocationTo(warehouseLocation(2L))
                )
        ))
            .setPlatformClient(PlatformClient.YANDEX_DELIVERY)
            .setMarketIdFrom(200L);

        Set<Long> filter = Set.of(1L, 2L);
        ValidateAndEnrichContext context = context(createReturnWarehouse().partnerId(null), 1000L, null);

        var warehouses = List.of(
            LmsModelFactory.createLogisticsPointResponse(1L, 1000L, "warehouse1", PointType.WAREHOUSE)
                .partnerId(null)
                .build(),
            LmsModelFactory.createLogisticsPointResponse(2L, 1001L, "warehouse2", PointType.WAREHOUSE)
                .partnerId(null)
                .build()
        );

        var warehouseMap = Map.of(1L, 200L, 2L, 200L, 3L, 200L);

        when(lmsLomLightClient.getLogisticsPointsByIds(filter)).thenReturn(
            warehouses.stream()
                .map(LogisticsPointLightModel::build)
                .collect(Collectors.toList())
        );

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(order, context);

        WaybillSegment segment = results.getOrderModifier().apply(order).getWaybill().iterator().next();

        assertEnriching(context, warehouseMap, results, segment.getWaybillShipment().getLocationTo());

        verify(lmsLomLightClient).getLogisticsPointsByIds(filter);
    }

    @Test
    @DisplayName("Успешное обогащение с локацией получателя")
    void enrichingSucceededRecipientLocation() {
        Order order = new Order().setWaybill(List.of(
            waybillSegment(1000L).setWaybillShipment(
                new WaybillSegment.WaybillShipment()
                    .setLocationFrom(warehouseLocation(1L))
                    .setLocationTo(new Location().setType(LocationType.RECIPIENT))
            )
                .setWarehouseLocation(null)
        ))
            .setPlatformClient(PlatformClient.YANDEX_DELIVERY);

        Set<Long> partnerIds = Set.of(1000L, 1003L);
        Set<Long> pointIds = Set.of(1L);

        ValidateAndEnrichContext context = context(createReturnWarehouse(), 1000L, null);

        var warehouses = List.of(
            LmsModelFactory.createLogisticsPointResponse(1L, 1000L, "warehouse1", PointType.WAREHOUSE).build()
        );

        var partners = List.of(
            partnerResponse(1000L, 123L),
            partnerResponse(1003L, 124L)
        );

        var warehouseMap = warehouseMap(context, warehouses, partners);

        when(lmsLomLightClient.getPartners(partnerIds)).thenReturn(partners);
        when(lmsLomLightClient.getLogisticsPointsByIds(pointIds)).thenReturn(
            warehouses.stream()
                .map(LogisticsPointLightModel::build)
                .collect(Collectors.toList())
        );

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(order, context);

        WaybillSegment segment = results.getOrderModifier().apply(order).getWaybill().iterator().next();

        assertEnriching(context, warehouseMap, results, segment.getWaybillShipment().getLocationFrom());

        verify(lmsLomLightClient).getLogisticsPointsByIds(pointIds);
        verify(lmsLomLightClient).getPartners(partnerIds);
    }

    @Test
    @DisplayName("Успешное обогащение склада для невыкупов")
    void enrichingExpressReturnScId() {
        Order order = new Order().setWaybill(List.of(
                waybillSegment(1000L)
                    .setWaybillShipment(null)
                    .setReturnWarehouseLocation(new Location().setType(LocationType.WAREHOUSE).setWarehouseId(100L))
            ))
            .setPlatformClient(PlatformClient.BERU);

        ValidateAndEnrichContext context = context(createReturnWarehouse(), null, null);
        Set<Long> pointIds = Set.of(100L, 4L);
        Set<Long> partnerIds = Set.of(2001L, 1002L, 1003L);

        var warehouses = List.of(
            LmsModelFactory.createLogisticsPointResponse(100L, 2001L, "return_warehouse", PointType.WAREHOUSE)
                .address(LmsModelFactory.createReturnLmsAddress(213))
                .build(),
            LmsModelFactory.createLogisticsPointResponse(4L, 1002L, "warehouse4", PointType.WAREHOUSE)
                .build()
        );

        var partners = List.of(
            partnerResponse(1003L, 125L),
            partnerResponse(2001L, 1260L),
            partnerResponse(1002L, 126L)
        );
        var warehouseMap = warehouseMap(context, warehouses, partners);

        when(lmsLomLightClient.getPartners(partnerIds)).thenReturn(partners);
        when(lmsLomLightClient.getLogisticsPointsByIds(pointIds)).thenReturn(
            warehouses.stream()
                .map(LogisticsPointLightModel::build)
                .collect(Collectors.toList())
        );

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(order, context);

        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getWarehouseIdToMarketId()).isEqualTo(warehouseMap);
        Order enrichedOrder = results.getOrderModifier().apply(order);
        softly.assertThat(enrichedOrder.getWaybill().get(0).getReturnWarehouseLocation().getAddress())
            .usingRecursiveComparison()
            .isEqualTo(returnAddress());
        verify(lmsLomLightClient).getLogisticsPointsByIds(pointIds);
        verify(lmsLomLightClient).getPartners(partnerIds);
    }

    @Test
    @DisplayName("Успешное обогащение с локацией ПВЗ")
    void enrichingSucceededPickupLocation() {
        testEnrichingPickupLocation(null, SegmentType.SORTING_CENTER, PartnerType.SORTING_CENTER, null, null);
    }

    @Test
    @DisplayName("Успешное обогащение с локацией ПВЗ для сегмента средней мили")
    void enrichingSucceededPickupLocationForMiddleMileSegment() {
        testEnrichingPickupLocation(
            LocalDate.parse("2021-11-01"),
            SegmentType.MOVEMENT,
            PartnerType.DELIVERY,
            null,
            new OffsetTimeInterval().setFrom(LocalTime.parse("10:00")).setTo(LocalTime.parse("18:00"))
        );
    }

    @Test
    @DisplayName("Успешное обогащение с локацией ПВЗ для сегмента средней мили - inboundInterval уже есть")
    void enrichingSucceededPickupLocationForMiddleMileSegmentInboundIntervalAlreadyExists() {
        testEnrichingPickupLocation(
            LocalDate.parse("2021-11-01"),
            SegmentType.MOVEMENT,
            PartnerType.DELIVERY,
            new OffsetTimeInterval().setFrom(LocalTime.parse("12:00")).setTo(LocalTime.parse("19:00")),
            new OffsetTimeInterval().setFrom(LocalTime.parse("12:00")).setTo(LocalTime.parse("19:00"))
        );
    }

    @Test
    @DisplayName("Успешное обогащение с локацией ПВЗ для сегмента средней мили - нет нужного дня в расписании")
    void enrichingSucceededPickupLocationForMiddleMileSegmentNoSuchDayInSchedule() {
        testEnrichingPickupLocation(
            LocalDate.parse("2021-11-02"),
            SegmentType.MOVEMENT,
            PartnerType.DELIVERY,
            null,
            null
        );
    }

    @Test
    @DisplayName("Ошибка обогащения беру - у склада нет партнера")
    void enrichingFail() {
        Order order = new Order().setWaybill(List.of(waybillSegment(1000L))).setPlatformClient(PlatformClient.BERU);

        ValidateAndEnrichContext context = context(createReturnWarehouse(), null, null);

        Set<Long> pointIds = Set.of(1L, 2L, 4L);
        Set<Long> partnerIds = Set.of(1003L);

        var warehouses = List.of(
            LmsModelFactory.createLogisticsPointResponse(1L, null, "warehouse1", PointType.WAREHOUSE).build(),
            LmsModelFactory.createLogisticsPointResponse(2L, null, "warehouse2", PointType.WAREHOUSE).build(),
            LmsModelFactory.createLogisticsPointResponse(4L, null, "warehouse4", PointType.WAREHOUSE).build()
        );

        when(lmsLomLightClient.getPartners(partnerIds)).thenReturn(List.of());
        when(lmsLomLightClient.getLogisticsPointsByIds(pointIds)).thenReturn(
            warehouses.stream()
                .map(LogisticsPointLightModel::build)
                .collect(Collectors.toList())
        );

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(order, context);

        assertFail(results, "Logistic points [1, 2, 3, 4] partners were without marketIds");

        verify(lmsLomLightClient).getLogisticsPointsByIds(pointIds);
        verify(lmsLomLightClient).getPartners(partnerIds);
    }

    @Nonnull
    private SearchPartnerFilter partnerFilter(Set<Long> longs) {
        return SearchPartnerFilter.builder().setIds(longs).build();
    }

    private void testEnrichingPickupLocation(
        LocalDate shipmentDate,
        SegmentType segmentType,
        PartnerType partnerType,
        OffsetTimeInterval existingInboundInterval,
        OffsetTimeInterval expectedInboundInterval
    ) {
        long pickupPointId = 55L;
        WaybillSegment.WaybillShipment waybillShipment = new WaybillSegment.WaybillShipment()
            .setLocationFrom(warehouseLocation(1L))
            .setDate(shipmentDate);
        Set<Long> pointIds = Set.of(1L);
        Set<Long> partnerIds = Set.of(1002L, 1003L, 1001L);

        Order order = new Order()
            .setPickupPoint(new PickupPoint().setPickupPointId(pickupPointId))
            .setWaybill(List.of(
                waybillSegment(1001L)
                    .setSegmentType(segmentType)
                    .setPartnerType(partnerType)
                    .setWaybillShipment(waybillShipment)
                    .setWarehouseLocation(null)
            ))
            .setPlatformClient(PlatformClient.YANDEX_DELIVERY);
        LogisticsPointResponse pickup = LmsModelFactory.createLogisticsPointResponse(
            pickupPointId,
            1001L,
            "pickup2",
            PointType.PICKUP_POINT
        )
            .build();

        ValidateAndEnrichContext context = context(createReturnWarehouse(), 1001L, pickup);

        var warehouses = List.of(
            LmsModelFactory.createLogisticsPointResponse(1L, 1002L, "warehouse1", PointType.WAREHOUSE).build()
        );
        var partners = List.of(
            partnerResponse(1002L, 123L),
            partnerResponse(1003L, 124L),
            partnerResponse(1001L, 125L)
        );

        var warehouseMap = warehouseMap(context, warehouses, partners);
        warehouseMap.put(pickupPointId, 125L);

        when(lmsLomLightClient.getPartners(partnerIds)).thenReturn(partners);
        when(lmsLomLightClient.getLogisticsPointsByIds(pointIds)).thenReturn(
            warehouses.stream()
                .map(LogisticsPointLightModel::build)
                .collect(Collectors.toList())
        );

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(order, context);

        waybillShipment.setLocationTo(
            new Location()
                .setType(LocationType.PICKUP)
                .setWarehouseId(pickupPointId)
                .setInboundInterval(existingInboundInterval)
        );

        WaybillSegment segment = results.getOrderModifier().apply(order).getWaybill().iterator().next();
        WaybillSegment.WaybillShipment shipment = segment.getWaybillShipment();

        assertEnriching(context, warehouseMap, results, shipment.getLocationFrom());

        softly.assertThat(shipment.getLocationTo().getAddress()).usingRecursiveComparison().isEqualTo(address());
        softly.assertThat(shipment.getLocationTo().getInstruction()).isEqualTo("Комментарий, как проехать");
        softly.assertThat(shipment.getLocationTo().getInboundInterval()).isEqualTo(expectedInboundInterval);

        verify(lmsLomLightClient).getPartners(partnerIds);
        verify(lmsLomLightClient).getLogisticsPointsByIds(pointIds);
    }

    private void assertEnriching(
        ValidateAndEnrichContext context,
        Map<Long, Long> warehouseMap,
        ValidateAndEnrichResults results,
        Location location
    ) {
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getWarehouseIdToMarketId()).isEqualTo(warehouseMap);
        softly.assertThat(location.getIncorporation()).isEqualTo("market-account-legal-name");
        softly.assertThat(location.getInstruction()).isEqualTo("Комментарий, как проехать");
    }

    @Nonnull
    private Address address() {
        return new Address()
            .setCountry("Россия")
            .setRegion("Регион")
            .setLocality("Новосибирск")
            .setSettlement("Новосибирск")
            .setStreet("Николаева")
            .setHouse("1")
            .setHousing("1")
            .setBuilding("")
            .setRoom("")
            .setZipCode("649220")
            .setGeoId(1);
    }

    @Nonnull
    private Address returnAddress() {
        return new Address()
            .setCountry("Россия")
            .setRegion("return-region")
            .setLocality("return-locality")
            .setSettlement("return-locality")
            .setStreet("return-street")
            .setHouse("return-house")
            .setHousing("return-housing")
            .setBuilding("return-building")
            .setRoom("return-room")
            .setZipCode("return-zipCode")
            .setLatitude(BigDecimal.valueOf(10.1))
            .setLongitude(BigDecimal.valueOf(100.2))
            .setGeoId(213);
    }

    @Nonnull
    private LogisticsPointResponse.LogisticsPointResponseBuilder createReturnWarehouse() {
        return LmsModelFactory.createLogisticsPointResponse(3L, 1003L, "warehouse3", PointType.WAREHOUSE);
    }

    @Nonnull
    private static Set<PlatformClient> nonYaDoPlatforms() {
        return Arrays.stream(PlatformClient.values())
            .filter(Predicate.not(PlatformClientUtils::isYaDo))
            .collect(Collectors.toSet());
    }

    @Nonnull
    private Location warehouseLocation(Long logisticsPointId) {
        return new Location()
            .setType(LocationType.WAREHOUSE)
            .setWarehouseId(logisticsPointId);
    }

    @Nonnull
    private PartnerLightModel partnerResponse(long partnerId, Long marketId) {
        return PartnerLightModel.build(
            LmsModelFactory.createPartnerResponse(
                partnerId,
                marketId,
                ru.yandex.market.logistics.management.entity.type.PartnerType.DROPSHIP
            )
        );
    }

    @Nonnull
    private Map<Long, Long> warehouseMap(
        ValidateAndEnrichContext context,
        List<LogisticsPointResponse> warehouses,
        List<? extends PartnerResponse> partners
    ) {
        return Stream.concat(warehouses.stream(), Stream.of(context.getReturnWarehouse()))
            .collect(Collectors.toMap(
                LogisticsPointResponse::getId,
                lp -> partners.stream()
                    .filter(p -> p.getId() == lp.getPartnerId())
                    .findFirst()
                    .map(PartnerResponse::getMarketId)
                    .orElse(-1L)
            ));
    }

    @Nonnull
    private LogisticsPointFilter logisticsPointFilter(Set<Long> longs) {
        return LogisticsPointFilter.newBuilder().ids(longs).build();
    }

    @Nonnull
    private WaybillSegment waybillSegment(Long partnerId) {
        return new WaybillSegment()
            .setPartnerId(partnerId)
            .setPartnerType(PartnerType.SORTING_CENTER)
            .setWaybillShipment(
                new WaybillSegment.WaybillShipment()
                    .setLocationFrom(warehouseLocation(1L))
                    .setLocationTo(warehouseLocation(2L))
            )
            .setWarehouseLocation(warehouseLocation(4L));
    }

    @Nonnull
    private ValidateAndEnrichContext context(
        LogisticsPointResponse.LogisticsPointResponseBuilder returnWarehouse,
        @Nullable Long partnerId,
        @Nullable LogisticsPointResponse pickupPointData
    ) {
        return new ValidateAndEnrichContext()
            .setReturnWarehouse(returnWarehouse.build())
            .setMarketAccountFromLegalName("market-account-legal-name")
            .setPartnerTypeById(partnerId == null ? Map.of() : Map.of(partnerId, PartnerType.SORTING_CENTER))
            .setPickupPointData(pickupPointData);
    }

    private void assertFail(ValidateAndEnrichResults results, String s) {
        softly.assertThat(results.isValidationPassed()).isFalse();
        softly.assertThat(results.getErrorMessage()).isEqualTo(s);
    }

    @Nonnull
    private WarehouseWorkTime workTimeRecord(int dayOfWeek) {
        return new WarehouseWorkTime().setDay(dayOfWeek)
            .setInterval(new TimeInterval().setFrom(LocalTime.of(0, 0)).setTo(LocalTime.of(23, 59)));
    }
}
