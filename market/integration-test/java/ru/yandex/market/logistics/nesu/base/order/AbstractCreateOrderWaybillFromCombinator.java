package ru.yandex.market.logistics.nesu.base.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import yandex.market.combinator.v0.CombinatorOuterClass;

import ru.yandex.common.geocoder.model.response.Component;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.LocationType;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.api.model.Dimensions;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraft;
import ru.yandex.market.logistics.nesu.dto.order.OrderRecipientAddress;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.service.combinator.CombinatorConverterUtils;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.defaultDeliverySearchRequestBuilder;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.mockCourierSchedule;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createAddressBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createItem;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createLomOrderCost;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createMonetary;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createPlace;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createRecipientBuilder;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createAddressDto;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponseBuilder;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartnerResponseBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractCreateOrderWaybillFromCombinator extends AbstractCreateOrderVirtualPartnersCasesTest {
    private static final ZoneOffset OFFSET = ZoneOffset.ofTotalSeconds(10800);
    private static final OffsetDateTime START = OffsetDateTime.of(
        INITIAL_SHIPMENT_DATE,
        LocalTime.of(12, 0),
        OFFSET
    );

    @Captor
    ArgumentCaptor<CombinatorOuterClass.DeliveryRouteFromPointRequest> captor;

    @AfterEach
    void tearDownCreateOrderWaybillFromCombinator() {
        when(featureProperties.isEnableCombinatorRoute()).thenReturn(false);
        when(featureProperties.isEnableCombinatorRouteFallback()).thenReturn(false);
    }

    @Test
    @DisplayName("Создание маршрута комбинатора с МК на последней миле")
    void courierRoute() throws Exception {
        when(featureProperties.isEnableCombinatorRoute()).thenReturn(true);
        when(combinatorGrpcClient.getDeliveryRouteFromPoint(any())).thenReturn(getCourierRoute());

        mockVirtualDeliveryService();
        mockGeoService();
        mockCourierSchedule(lmsClient, Set.of(213, 20279, 120542), Set.of(5L));

        mockSearchSortingCenter(List.of(
            LmsFactory.createPartner(SORTING_CENTER_ID, PartnerType.SORTING_CENTER),
            createMkPartner()
        ));
        when(lmsClient.getLogisticsPoints(
            refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 5L), true))
        ))
            .thenReturn(List.of(
                WAREHOUSE_FROM,
                SORTING_CENTER_WAREHOUSE_TO
            ));
        doReturn(List.of(LOCAL_SORTING_CENTER))
            .when(lmsClient).getLogisticsPoints(refEq(LmsFactory.createWarehousesFilter(Set.of(123L))));
        mockDeliveryOption(
            defaultDeliverySearchRequestBuilder()
                .pickupPoints(null)
                .deliveryServiceIds(Set.of(420L, 5L, 421L, 45L))
                .locationsTo(Set.of(213, 20279, 120542))
                .build()
        );

        createOrder("order_with_coordinates", 1L)
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        WaybillOrderRequestDto expectedOrder = sortingCenterOrder(OrderDtoFactory.createLocation(5L));
        expectedOrder.setWaybill(expectedCourierWaybill());
        setExpectedItems(expectedOrder, true);

        // тк партнёр МК, то заполняются координаты
        expectedOrder.setRecipient(
            createRecipientBuilder()
                .address(getAddress())
                .build()
        );

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(captor.capture());
        CombinatorOuterClass.DeliveryRouteFromPointRequest request = captor.getValue();
        softly.assertThat(request.getItemsList()).isEqualTo(List.of(
            CombinatorOuterClass.DeliveryRequestPackage.newBuilder()
                .setRequiredCount(1)
                .setWeight(4000)
                .addAllDimensions(List.of(1, 2, 3))
                .setPrice(200)
                .build(),
            CombinatorOuterClass.DeliveryRequestPackage.newBuilder()
                .setRequiredCount(1)
                .setWeight(1000)
                .addAllDimensions(List.of(4, 3, 2))
                .setPrice(0)
                .build()
        ));

        softly.assertThat(request.getDestination().getRegionId()).isEqualTo(120542);

        verifyLomOrderCreate(expectedOrder);
        verify(lmsClient, times(0)).getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(SORTING_CENTER_ID))
            .active(true)
            .build()));
    }

    @Test
    @DisplayName("Создание маршрута комбинатора с ПВЗ на последней миле")
    void pvzRoute() throws Exception {
        when(featureProperties.isEnableCombinatorRoute()).thenReturn(true);
        when(combinatorGrpcClient.getDeliveryRouteFromPoint(any())).thenReturn(getPvzRoute());
        long tariffId = 100_033L;

        WaybillOrderRequestDto orderDto = createLomOrderRequest();
        orderDto.setCost(
            createLomOrderCost()
                .services(addLomSortService(OrderDtoFactory.defaultLomDeliveryServices(
                    "3.86",
                    "0.75"
                )))
                .tariffId(tariffId)
                .build()
        );
        orderDto.setWaybill(expectedPvzWaybill());
        orderDto.setRecipient(
            createRecipientBuilder()
                .address(
                    createAddressBuilder()
                        .longitude(BigDecimal.valueOf(37.5846221554295))
                        .latitude(BigDecimal.valueOf(55.7513100141919))
                        .build()
                )
                .build()
        );
        setExpectedItems(orderDto, false);

        doReturn(List.of(
            LmsFactory.createPartner(SORTING_CENTER_ID, PartnerType.SORTING_CENTER),
            LmsFactory.createPartner(110L, PartnerType.DELIVERY),
            createPartnerResponseBuilder(4L, PartnerType.DELIVERY, 100L)
                .subtype(PartnerSubtypeResponse.newBuilder().id(5).build())
                .build(),
            createPartnerResponseBuilder(5L, PartnerType.DELIVERY, 100L)
                .subtype(PartnerSubtypeResponse.newBuilder().id(2).build())
                .build()
        ))
            .when(lmsClient)
            .searchPartners(refEq(LmsFactory.createPartnerFilter(
                Set.of(SORTING_CENTER_ID, 5L, 110L, 4L),
                null,
                Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
            )));

        LogisticsPointResponse pickupPoint = createLogisticsPointResponseBuilder(
            101L,
            4L,
            "pick",
            PointType.PICKUP_POINT
        )
            .businessId(202L)
            .address(createAddressDto(213))
            .build();
        when(lmsClient.getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 5L, 101L), true))))
            .thenReturn(List.of(WAREHOUSE_FROM, SORTING_CENTER_WAREHOUSE_TO, pickupPoint));

        when(tarifficatorClient.getTariff(tariffId)).thenReturn(TariffDto.builder().partnerId(5L).build());

        mockDeliveryOption(defaultDeliverySearchRequestBuilder().tariffId(tariffId).build());

        createOrder(createOrderThroughSortingCenter().andThen(
                    o -> o.getDeliveryOption()
                        .setPartnerId(110L)
                        .setTariffId(tariffId)
                )
                .andThen(d -> d.getRecipient().setPickupPointId(101L))
                .andThen(items())
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(captor.capture());
        CombinatorOuterClass.DeliveryRouteFromPointRequest request = captor.getValue();
        softly.assertThat(request.getItemsList()).isEqualTo(List.of(
            CombinatorOuterClass.DeliveryRequestPackage.newBuilder()
                .setRequiredCount(1)
                .setWeight(4000)
                .addAllDimensions(List.of(1, 2, 3))
                .setPrice(190)
                .build(),
            CombinatorOuterClass.DeliveryRequestPackage.newBuilder()
                .setRequiredCount(1)
                .setWeight(1000)
                .addAllDimensions(List.of(4, 3, 2))
                .setPrice(10)
                .build()
        ));

        softly.assertThat(request.getDestination().getRegionId()).isEqualTo(213);

        verifyLomOrderCreate(orderDto);
        verify(tarifficatorClient).getTariff(tariffId);
    }

    @Test
    @DisplayName("Успешное создание заказа через МК при неудачной попытке построения через комбинатор")
    void createOrderWithCombinatorRouteRetry() throws Exception {
        when(featureProperties.isEnableCombinatorRoute()).thenReturn(true);
        when(featureProperties.isEnableCombinatorRouteFallback()).thenReturn(false);

        doThrow(
            new StatusRuntimeException(
                Status.Code.UNKNOWN.toStatus()
                    .withDescription("no courier route, ds:0")
            )
        )
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(
                argThat(request -> request.getDestination().getRegionId() == 120542)
            );

        doReturn(getCourierRoute())
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(
                argThat(request -> request.getDestination().getRegionId() == 20279)
            );

        mockVirtualDeliveryService();
        mockGeoService();
        mockCourierSchedule(lmsClient, Set.of(213, 20279, 120542), Set.of(5L));

        mockSearchSortingCenter(List.of(
            LmsFactory.createPartner(SORTING_CENTER_ID, PartnerType.SORTING_CENTER),
            createMkPartner()
        ));
        when(lmsClient.getLogisticsPoints(
            refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 5L), true))
        ))
            .thenReturn(List.of(
                WAREHOUSE_FROM,
                SORTING_CENTER_WAREHOUSE_TO
            ));
        doReturn(List.of(LOCAL_SORTING_CENTER))
            .when(lmsClient).getLogisticsPoints(refEq(LmsFactory.createWarehousesFilter(Set.of(123L))));
        mockDeliveryOption(
            defaultDeliverySearchRequestBuilder()
                .pickupPoints(null)
                .deliveryServiceIds(Set.of(420L, 5L, 421L, 45L))
                .locationsTo(Set.of(213, 20279, 120542))
                .build()
        );

        createOrder("order_with_coordinates", 1L)
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        WaybillOrderRequestDto expectedOrder = sortingCenterOrder(OrderDtoFactory.createLocation(5L));
        expectedOrder.setWaybill(expectedCourierWaybill());
        setExpectedItems(expectedOrder, true);

        // тк партнёр МК, то заполняются координаты
        expectedOrder.setRecipient(
            createRecipientBuilder()
                .address(getAddress())
                .build()
        );

        verify(combinatorGrpcClient, times(2)).getDeliveryRouteFromPoint(captor.capture());
        CombinatorOuterClass.DeliveryRouteFromPointRequest request = captor.getValue();

        softly.assertThat(request.getDestination().getRegionId()).isEqualTo(20279);

        verifyLomOrderCreate(expectedOrder);
        verify(lmsClient, times(0)).getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(SORTING_CENTER_ID))
            .active(true)
            .build()));

    }

    private void mockGeoService() {
        String locationCoordinates = "37.587092522460836 55.73397404565889";

        when(geoClient.find(locationCoordinates))
            .thenReturn(List.of(
                geoObjectWithParameters(
                    locationCoordinates, "213",
                    List.of(
                        new Component("Россия", List.of(Kind.COUNTRY)),
                        new Component("Центральный федеральный округ", List.of(Kind.PROVINCE)),
                        new Component("Москва", List.of(Kind.PROVINCE)),
                        new Component("Москва", List.of(Kind.LOCALITY)),
                        new Component("улица Льва Толстого", List.of(Kind.STREET))
                    )
                ),
                geoObjectWithParameters(
                    locationCoordinates, "120542",
                    List.of(
                        new Component("Россия", List.of(Kind.COUNTRY)),
                        new Component("Центральный федеральный округ", List.of(Kind.PROVINCE)),
                        new Component("Москва", List.of(Kind.PROVINCE)),
                        new Component("Москва", List.of(Kind.LOCALITY)),
                        new Component("Центральный административный округ", List.of(Kind.DISTRICT)),
                        new Component("район Хамовники", List.of(Kind.DISTRICT)),
                        new Component("квартал Красная Роза", List.of(Kind.DISTRICT))
                    )
                ),
                geoObjectWithParameters(
                    locationCoordinates, "20279",
                    List.of(
                        new Component("Россия", List.of(Kind.COUNTRY)),
                        new Component("Центральный федеральный округ", List.of(Kind.PROVINCE)),
                        new Component("Москва", List.of(Kind.PROVINCE)),
                        new Component("Москва", List.of(Kind.LOCALITY)),
                        new Component("Центральный административный округ", List.of(Kind.DISTRICT))
                    )
                ),
                geoObjectWithParameters(
                    locationCoordinates, "213",
                    List.of(
                        new Component("Россия", List.of(Kind.COUNTRY)),
                        new Component("Центральный федеральный округ", List.of(Kind.PROVINCE)),
                        new Component("Москва", List.of(Kind.PROVINCE)),
                        new Component("Москва", List.of(Kind.LOCALITY))
                    )
                )
            ));
    }

    private OrderRecipientAddress getRecipient() {
        return OrderRecipientAddress.builder()
            .geoId(213)
            .latitude(new BigDecimal("55.73397404565889"))
            .longitude(new BigDecimal("37.587092522460836"))
            .country("Россия")
            .locality("Москва")
            .street("улица Льва Толстого")
            .house("16")
            .build();
    }

    @Nonnull
    private AddressDto getAddress() {
        return AddressDto.builder()
            .geoId(213)
            .country("Россия")
            .region("Москва и Московская область")
            .locality("Москва")
            .street("улица Льва Толстого")
            .house("16")
            .latitude(new BigDecimal("55.73397404565889"))
            .longitude(new BigDecimal("37.587092522460836"))
            .build();
    }

    @Nonnull
    private GeoObject geoObjectWithParameters(String point, String geoId, List<Component> components) {
        return defaultGeoObjectWithParameters(Kind.HOUSE, geoId, point, components);
    }

    @Nonnull
    private CombinatorOuterClass.DeliveryRoute getCourierRoute() {
        CombinatorOuterClass.Route.Point handing = CombinatorOuterClass.Route.Point.newBuilder()
            .setIds(getIds(5))
            .setPartnerType(PartnerType.DELIVERY.name())
            .setSegmentType("handing")
            .build();

        return getRoute(handing);
    }

    @Nonnull
    private CombinatorOuterClass.DeliveryRoute getPvzRoute() {
        CombinatorOuterClass.Route.Point pickup = CombinatorOuterClass.Route.Point.newBuilder()
            .setIds(getIds(4, 101))
            .setPartnerType(PartnerType.DELIVERY.name())
            .setSegmentType("pickup")
            .addAllServices(List.of(getService("INBOUND", START.plusHours(5))))
            .build();

        return getRoute(pickup);
    }

    @Nonnull
    private CombinatorOuterClass.DeliveryRoute getRoute(CombinatorOuterClass.Route.Point lastPoint) {
        List<CombinatorOuterClass.Route.Point> points = getCommonPoints();
        points.add(lastPoint);

        return CombinatorOuterClass.DeliveryRoute.newBuilder()
            .setRoute(
                CombinatorOuterClass.Route.newBuilder()
                    .addAllPoints(points)
                    .addAllPaths(getPath(points.size()))
                    .setDateFrom(CombinatorConverterUtils.getDate(INITIAL_SHIPMENT_DATE.plusDays(5)))
                    .setDateTo(CombinatorConverterUtils.getDate(INITIAL_SHIPMENT_DATE.plusDays(5)))
                    .build()
            )
            .build();
    }

    @Nonnull
    private List<CombinatorOuterClass.Route.Point> getCommonPoints() {
        return new ArrayList<>(List.of(
            createSortingCenterPoint(SORTING_CENTER_ID, 5),
            createMovementPoint(7, START),
            createSortingCenterPoint(8, 6),
            createMovementPoint(9, START.plusHours(1)),
            createSortingCenterPoint(9, 7),
            createMovementPoint(10, START.plusHours(2)),
            createSortingCenterPoint(11, 8),
            createMovementPoint(11, START.plusHours(3)),
            createSortingCenterPoint(12, 9),
            createMovementPoint(5, START.plusHours(4)),
            createLinehaulPoint(5)
        ));
    }

    @Nonnull
    private CombinatorOuterClass.Route.Point createSortingCenterPoint(long partnerId, long logisticPointId) {
        return CombinatorOuterClass.Route.Point.newBuilder()
            .setIds(getIds(partnerId, logisticPointId))
            .setPartnerType(PartnerType.SORTING_CENTER.name())
            .setSegmentType("warehouse")
            .build();
    }

    @Nonnull
    private CombinatorOuterClass.Route.Point createMovementPoint(long partnerId, OffsetDateTime dateTime) {
        return CombinatorOuterClass.Route.Point.newBuilder()
            .setIds(getIds(partnerId))
            .setPartnerType(PartnerType.SORTING_CENTER.name())
            .setSegmentType("movement")
            .addAllServices(List.of(getMovementService(dateTime)))
            .build();
    }

    @Nonnull
    private CombinatorOuterClass.Route.Point createLinehaulPoint(long partnerId) {
        return CombinatorOuterClass.Route.Point.newBuilder()
            .setIds(getIds(partnerId))
            .setPartnerType(PartnerType.DELIVERY.name())
            .setSegmentType("linehaul")
            .build();
    }

    @Nonnull
    private CombinatorOuterClass.PointIds getIds(long partnerId) {
        return CombinatorOuterClass.PointIds.newBuilder()
            .setPartnerId(partnerId)
            .build();
    }

    @Nonnull
    private CombinatorOuterClass.PointIds getIds(long partnerId, long logisticPointId) {
        return CombinatorOuterClass.PointIds.newBuilder()
            .setPartnerId(partnerId)
            .setLogisticPointId(logisticPointId)
            .build();
    }

    @Nonnull
    private CombinatorOuterClass.DeliveryService getMovementService(OffsetDateTime dateTime) {
        return getService("MOVEMENT", dateTime);
    }

    @Nonnull
    private CombinatorOuterClass.DeliveryService getService(String code, OffsetDateTime dateTime) {
        return CombinatorOuterClass.DeliveryService.newBuilder()
            .setCode(code)
            .setTzOffset(dateTime.getOffset().getTotalSeconds())
            .setStartTime(getTimestamp(dateTime.toInstant()))
            .setScheduleEndTime(getTimestamp(dateTime.plusHours(1).toInstant()))
            .setLogisticDate(CombinatorConverterUtils.getDate(dateTime.toLocalDate()))
            .build();
    }

    @Nonnull
    private Timestamp getTimestamp(Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }

    @Nonnull
    private List<CombinatorOuterClass.Route.Path> getPath(int size) {
        List<CombinatorOuterClass.Route.Path> path = new ArrayList<>(size - 1);
        for (int i = size - 1; i > 0; i--) {
            path.add(
                CombinatorOuterClass.Route.Path.newBuilder()
                    .setPointFrom(i - 1)
                    .setPointTo(i)
                    .build()
            );
        }

        return path;
    }

    @Nonnull
    private List<WaybillSegmentDto> expectedCourierWaybill() {
        List<WaybillSegmentDto> waybill = getCommonExpectedWaybill();

        waybill.add(createWaybillSegment(
            5,
            SegmentType.COURIER,
            WaybillSegmentDto.ShipmentDto.builder()
                .type(ShipmentType.WITHDRAW)
                .date(INITIAL_SHIPMENT_DATE)
                .dateTime(START.plusHours(5))
                .tzOffset(OFFSET.getTotalSeconds())
                .locationFrom(OrderDtoFactory.createLocation(9L))
                .build()
        ));

        return waybill;
    }

    @Nonnull
    private List<WaybillSegmentDto> expectedPvzWaybill() {
        List<WaybillSegmentDto> waybill = getCommonExpectedWaybill();

        waybill.add(createWaybillSegment(
            5,
            SegmentType.MOVEMENT,
            WaybillSegmentDto.ShipmentDto.builder()
                .type(ShipmentType.WITHDRAW)
                .date(INITIAL_SHIPMENT_DATE)
                .dateTime(START.plusHours(5))
                .tzOffset(OFFSET.getTotalSeconds())
                .locationFrom(OrderDtoFactory.createLocation(9L))
                .locationTo(OrderDtoFactory.createLocation(101L, LocationType.PICKUP))
                .build()
        ));
        waybill.add(createWaybillSegment(
            4,
            SegmentType.PICKUP,
            WaybillSegmentDto.ShipmentDto.builder()
                .date(INITIAL_SHIPMENT_DATE)
                .locationFrom(OrderDtoFactory.createLocation(9L))
                .locationTo(OrderDtoFactory.createLocation(101L, LocationType.PICKUP))
                .build()
        ));

        return waybill;
    }

    @Nonnull
    private List<WaybillSegmentDto> getCommonExpectedWaybill() {
        return new ArrayList<>(List.of(
            createWaybillSegment(
                6,
                SegmentType.SORTING_CENTER,
                WaybillSegmentDto.ShipmentDto.builder()
                    .type(ShipmentType.WITHDRAW)
                    .date(INITIAL_SHIPMENT_DATE)
                    .locationFrom(OrderDtoFactory.createLocation(3L))
                    .locationTo(OrderDtoFactory.createLocation(5L))
                    .build()
            ),
            createWaybillSegment(
                8,
                SegmentType.SORTING_CENTER,
                WaybillSegmentDto.ShipmentDto.builder()
                    .type(ShipmentType.WITHDRAW)
                    .date(INITIAL_SHIPMENT_DATE)
                    .dateTime(START.plusHours(2))
                    .tzOffset(OFFSET.getTotalSeconds())
                    .locationFrom(OrderDtoFactory.createLocation(5L))
                    .locationTo(OrderDtoFactory.createLocation(6L))
                    .build()
            ),
            createWaybillSegment(
                9,
                SegmentType.SORTING_CENTER,
                WaybillSegmentDto.ShipmentDto.builder()
                    .type(ShipmentType.WITHDRAW)
                    .date(INITIAL_SHIPMENT_DATE)
                    .dateTime(START.plusHours(3))
                    .tzOffset(OFFSET.getTotalSeconds())
                    .locationFrom(OrderDtoFactory.createLocation(6L))
                    .locationTo(OrderDtoFactory.createLocation(7L))
                    .build()
            ),
            createWaybillSegment(
                11,
                SegmentType.SORTING_CENTER,
                WaybillSegmentDto.ShipmentDto.builder()
                    .date(INITIAL_SHIPMENT_DATE)
                    .dateTime(START.plusHours(4))
                    .tzOffset(OFFSET.getTotalSeconds())
                    .locationFrom(OrderDtoFactory.createLocation(8L))
                    .locationTo(OrderDtoFactory.createLocation(9L))
                    .build()
            ),
            createWaybillSegment(
                12,
                SegmentType.SORTING_CENTER,
                WaybillSegmentDto.ShipmentDto.builder()
                    .type(ShipmentType.IMPORT)
                    .date(INITIAL_SHIPMENT_DATE)
                    .dateTime(START.plusHours(5))
                    .tzOffset(OFFSET.getTotalSeconds())
                    .locationFrom(OrderDtoFactory.createLocation(8L))
                    .locationTo(OrderDtoFactory.createLocation(9L))
                    .build()
            )
        ));
    }

    @Nonnull
    private WaybillSegmentDto createWaybillSegment(
        long partnerId,
        SegmentType segmentType,
        WaybillSegmentDto.ShipmentDto shipment
    ) {
        return WaybillSegmentDto.builder()
            .requisiteId(200L)
            .partnerId(partnerId)
            .shipment(shipment)
            .segmentType(segmentType)
            .rootStorageUnitExternalId("generated-0")
            .build();
    }

    @Nonnull
    private Consumer<OrderDraft> multiplaceItems() {
        return order -> {
            order.setDimensions(
                new Dimensions()
                    .setHeight(15)
                    .setWidth(30)
                    .setLength(45)
                    .setWeight(new BigDecimal(50))
            );

            order.setItems(
                List.of(
                    createItem(10, 10, 100),
                    createItem(90, 1, 100),
                    createItem(5, 2, 100)
                )
            );

            order.setPlaces(
                List.of(
                    createPlace(1, 2, 3, 4, null),
                    createPlace(4, 3, 2, 1, null)
                )
            );
        };
    }

    @Nonnull
    private Consumer<OrderDraft> items() {
        return order -> {
            order.setDimensions(
                new Dimensions()
                    .setHeight(15)
                    .setWidth(30)
                    .setLength(45)
                    .setWeight(new BigDecimal(50))
            );

            order.setPlaces(
                List.of(
                    createPlace(
                        1, 2, 3, 4,
                        List.of(
                            createItem(10, 10, 100).setSupplierInn("inn"),
                            createItem(90, 1, 100).setSupplierInn("inn")
                        )
                    ),
                    createPlace(
                        4, 3, 2, 1,
                        List.of(createItem(5, 2, 100).setSupplierInn("inn"))
                    )
                )
            );
        };
    }

    private void setExpectedItems(WaybillOrderRequestDto expectedOrder, boolean isMultiplace) {
        expectedOrder.setItems(
            List.of(
                OrderDtoFactory.createLomItemBuilder()
                    .count(10)
                    .supplierInn("inn")
                    .price(createMonetary(BigDecimal.valueOf(10)))
                    .boxes(List.of(
                        OrderDtoFactory.createItemBoxBuilder()
                            .storageUnitExternalIds(isMultiplace ? null : Set.of("ext_place_id"))
                            .storageUnitIndexes(isMultiplace ? null : List.of(0))
                            .build()
                    ))
                    .build(),
                OrderDtoFactory.createLomItemBuilder()
                    .count(1)
                    .supplierInn("inn")
                    .price(createMonetary(BigDecimal.valueOf(90)))
                    .boxes(List.of(
                        OrderDtoFactory.createItemBoxBuilder()
                            .storageUnitExternalIds(isMultiplace ? null : Set.of("ext_place_id"))
                            .storageUnitIndexes(isMultiplace ? null : List.of(0))
                            .build()
                    ))
                    .build(),
                OrderDtoFactory.createLomItemBuilder()
                    .count(2)
                    .supplierInn("inn")
                    .price(createMonetary(BigDecimal.valueOf(5)))
                    .boxes(List.of(
                        OrderDtoFactory.createItemBoxBuilder()
                            .storageUnitExternalIds(isMultiplace ? null : Set.of("ext_place_id"))
                            .storageUnitIndexes(isMultiplace ? null : List.of(1))
                            .build()
                    ))
                    .build()
            )
        );

        expectedOrder.setUnits(
            List.of(
                OrderDtoFactory.createPlaceUnitBuilder()
                    .dimensions(OrderDtoFactory.createKorobyte(3, 2, 1, 4))
                    .build(),
                OrderDtoFactory.createPlaceUnitBuilder()
                    .dimensions(OrderDtoFactory.createKorobyte(2, 3, 4, 1))
                    .build(),
                OrderDtoFactory.createRootUnit()
            )
        );
    }
}
