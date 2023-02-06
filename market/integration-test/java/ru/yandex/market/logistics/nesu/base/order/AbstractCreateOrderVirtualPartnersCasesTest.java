package ru.yandex.market.logistics.nesu.base.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.geocoder.model.response.Component;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto.ShipmentDto;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftDeliveryOption;
import ru.yandex.market.logistics.nesu.enums.PartnerSubtype;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.service.combinator.CombinatorGrpcClient;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffDto;
import ru.yandex.market.tpl.internal.client.TplInternalClient;
import ru.yandex.market.tpl.internal.client.model.DeliveryServiceByLocationRequestDto;
import ru.yandex.market.tpl.internal.client.model.DeliveryServiceByLocationResponseDto;
import ru.yandex.market.tpl.internal.client.model.GeoCoordinates;
import ru.yandex.market.tpl.internal.client.model.SortingCenterInfoDto;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.lom.model.enums.LocationType.PICKUP;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.defaultDeliverySearchRequestBuilder;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.mockCourierSchedule;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createAddressBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createLomOrderCost;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createRecipientBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createWithdrawBuilder;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponseBuilder;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartnerResponseBuilder;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createScheduleDayDto;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractCreateOrderVirtualPartnersCasesTest extends AbstractCreateOrderBaseCasesTest {

    @Autowired
    protected TarifficatorClient tarifficatorClient;

    @Autowired
    protected TplInternalClient tplInternalClient;

    @Autowired
    protected CombinatorGrpcClient combinatorGrpcClient;

    @AfterEach
    protected final void tearDownVirtualPartnersCases() {
        verifyNoMoreInteractions(tplInternalClient);
    }

    @Test
    @DisplayName(
        "Тариф, указанный в опциях доставки, является тарифом тарификатора, " +
            "и в опциях доставки указан виртуальный партнер"
    )
    void virtualPartner() throws Exception {
        long tariffId = 100_033L;

        WaybillOrderRequestDto orderDto = createLomOrderRequest();
        orderDto.setCost(createLomOrderCost().tariffId(tariffId).build());
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

        when(tarifficatorClient.getTariff(tariffId)).thenReturn(TariffDto.builder().partnerId(5L).build());

        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(
            Set.of(5L, 110L),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        )))
            .thenReturn(List.of(
                LmsFactory.createPartnerResponseBuilder(5L, PartnerType.DELIVERY, 100L)
                    .subtype(PartnerSubtypeResponse.newBuilder().id(2).build())
                    .build(),
                LmsFactory.createPartner(110L, PartnerType.DELIVERY)
            ));

        mockDeliveryOption(defaultDeliverySearchRequestBuilder().tariffId(tariffId).build());

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(
            o -> {
                OrderDraftDeliveryOption deliveryOption = o.getDeliveryOption();
                deliveryOption.setTariffId(tariffId);
                deliveryOption.setPartnerId(110L);
            }
        ))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(orderDto);
        verify(tarifficatorClient).getTariff(tariffId);
    }

    @Test
    @DisplayName(
        "Тариф, указанный в опциях доставки, является тарифом тарификатора, " +
            "и в опциях доставки указан виртуальный партнер для постаматов (нужен поход в 3pl за средней милей)"
    )
    void virtualPartnerAndMiddleMile() throws Exception {
        doReturn(new SortingCenterInfoDto(123L))
            .when(tplInternalClient).getSortingCenter(777L);
        long tariffId = 100_033L;

        WaybillOrderRequestDto orderDto = createLomOrderRequest();
        orderDto.setCost(
            createLomOrderCost()
                .services(addLomSortService(OrderDtoFactory.defaultLomDeliveryServices("3.86", "0.75")))
                .tariffId(tariffId)
                .build()
        );
        orderDto.setWaybill(waybillWithMiddleMileFromMk());

        doReturn(List.of(
            LmsFactory.createPartner(SORTING_CENTER_ID, PartnerType.SORTING_CENTER),
            LmsFactory.createPartner(110L, PartnerType.DELIVERY),
            createPartnerResponseBuilder(5L, PartnerType.DELIVERY, 100L)
                .subtype(PartnerSubtypeResponse.newBuilder().id(5).build())
                .build()
        ))
            .when(lmsClient)
            .searchPartners(refEq(LmsFactory.createPartnerFilter(
                Set.of(SORTING_CENTER_ID, 5L, 110L),
                null,
                Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
            )));

        doReturn(List.of(WAREHOUSE_FROM, SORTING_CENTER_WAREHOUSE_TO, PICKUP_POINT))
            .when(lmsClient)
            .getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 5L, 101L), true)));
        doReturn(List.of(LOCAL_SORTING_CENTER))
            .when(lmsClient).getLogisticsPoints(refEq(LmsFactory.createWarehousesFilter(Set.of(123L))));

        when(tarifficatorClient.getTariff(tariffId)).thenReturn(TariffDto.builder().partnerId(5L).build());

        mockDeliveryOption(defaultDeliverySearchRequestBuilder().tariffId(tariffId).build());

        DeliveryServiceByLocationRequestDto tplRequest = DeliveryServiceByLocationRequestDto.builder()
            .geoCoordinates(
                GeoCoordinates.builder()
                    .longitude(new BigDecimal(2))
                    .latitude(new BigDecimal(1))
                    .build()
            )
            .build();
        when(tplInternalClient.getDeliveryServiceByLocation(tplRequest))
            .thenReturn(DeliveryServiceByLocationResponseDto.builder().deliveryServiceId(777L).build());

        createOrder(createOrderThroughSortingCenter().andThen(
                    o -> o.getDeliveryOption()
                        .setPartnerId(110L)
                        .setTariffId(tariffId)
                )
                .andThen(d -> d.getRecipient().setPickupPointId(101L))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(orderDto);
        verify(tarifficatorClient).getTariff(tariffId);
        verify(tplInternalClient).getDeliveryServiceByLocation(tplRequest);
        verify(tplInternalClient).getSortingCenter(777L);
    }

    @Test
    @DisplayName(
        "Тариф, указанный в опциях доставки, является тарифом тарификатора, " +
            "и в опциях доставки указан виртуальный партнер для постаматов (нужен поход в 3pl за средней милей)." +
            "У ПВЗ отсутствуют координаты"
    )
    void virtualPartnerAndMiddleMilePickupCoordinatesDoNotExist() throws Exception {
        long tariffId = 100_033L;

        doReturn(List.of(
            LmsFactory.createPartner(SORTING_CENTER_ID, PartnerType.SORTING_CENTER),
            LmsFactory.createPartner(110L, PartnerType.DELIVERY),
            createPartnerResponseBuilder(5L, PartnerType.DELIVERY, 100L)
                .subtype(PartnerSubtypeResponse.newBuilder().id(5).build())
                .build()
        ))
            .when(lmsClient)
            .searchPartners(refEq(LmsFactory.createPartnerFilter(
                Set.of(SORTING_CENTER_ID, 5L, 110L),
                null,
                Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
            )));

        when(lmsClient.getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 5L, 101L), true))))
            .thenReturn(List.of(
                WAREHOUSE_FROM,
                SORTING_CENTER_WAREHOUSE_TO,
                createLogisticsPointResponseBuilder(
                    101L,
                    5L,
                    "pick",
                    PointType.PICKUP_POINT
                )
                    .address(Address.newBuilder().build())
                    .build()
            ));

        when(tarifficatorClient.getTariff(tariffId)).thenReturn(TariffDto.builder().partnerId(5L).build());

        mockDeliveryOption(defaultDeliverySearchRequestBuilder().tariffId(tariffId).build());

        createOrder(createOrderThroughSortingCenter().andThen(
                    o -> o.getDeliveryOption()
                        .setPartnerId(110L)
                        .setTariffId(tariffId)
                )
                .andThen(d -> d.getRecipient().setPickupPointId(101L))
        )
            .andExpect(status().isInternalServerError())
            .andExpect(content().json(
                "{\"message\":\"Order without pickup point gps coordinates, orderExternalId = a\\\\Bc-12356. " +
                    "Middle mile delivery service id cannot be determined\",\"type\":\"UNKNOWN\"}"
            ));

        verify(tarifficatorClient).getTariff(tariffId);
    }

    @Test
    @DisplayName(
        "Тариф, указанный в опциях доставки, является ПВЗ-тарифом тарификатора, " +
            "и в опциях доставки указан виртуальный партнер для курьерки (не нужен поход в 3pl за средней милей)"
    )
    void virtualPartnerNewFlowAndMiddleMilePickupCoordinatesDoNotExist()
        throws Exception {
        doReturn(new SortingCenterInfoDto(123L))
            .when(tplInternalClient).getSortingCenter(777L);
        long tariffId = 100_033L;

        WaybillOrderRequestDto orderDto = createLomOrderRequest();
        orderDto.setCost(
            createLomOrderCost()
                .services(addLomSortService(OrderDtoFactory.defaultLomDeliveryServices("3.86", "0.75")))
                .tariffId(tariffId)
                .build()
        );
        orderDto.setWaybill(waybillWithMiddleMilePickup());
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

        doReturn(List.of(
            LmsFactory.createPartner(SORTING_CENTER_ID, PartnerType.SORTING_CENTER),
            LmsFactory.createPartner(110L, PartnerType.DELIVERY),
            createPartnerResponseBuilder(5L, PartnerType.DELIVERY, 100L)
                .subtype(PartnerSubtypeResponse.newBuilder().id(5).build())
                .build(),
            createPartnerResponseBuilder(777L, PartnerType.DELIVERY, 100L)
                .subtype(PartnerSubtypeResponse.newBuilder().id(2).build())
                .build()
        ))
            .when(lmsClient)
            .searchPartners(refEq(LmsFactory.createPartnerFilter(
                Set.of(SORTING_CENTER_ID, 5L, 110L, 777L),
                null,
                Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
            )));

        when(lmsClient.getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 5L, 101L), true))))
            .thenReturn(List.of(WAREHOUSE_FROM, SORTING_CENTER_WAREHOUSE_TO, PICKUP_POINT));

        doReturn(List.of(LOCAL_SORTING_CENTER))
            .when(lmsClient).getLogisticsPoints(refEq(LmsFactory.createWarehousesFilter(Set.of(123L))));
        when(tarifficatorClient.getTariff(tariffId)).thenReturn(TariffDto.builder().partnerId(777L).build());

        mockDeliveryOption(defaultDeliverySearchRequestBuilder().tariffId(tariffId).build());

        createOrder(createOrderThroughSortingCenter().andThen(
                    o -> o.getDeliveryOption()
                        .setPartnerId(110L)
                        .setTariffId(tariffId)
                )
                .andThen(d -> d.getRecipient().setPickupPointId(101L))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(orderDto);
        verify(tarifficatorClient).getTariff(tariffId);
        verify(tplInternalClient).getSortingCenter(777L);
    }

    @Test
    @DisplayName("Валидация опций доставки, виртуальный партнер")
    void deliveryOptionVirtualPartner() throws Exception {
        mockVirtualDeliveryService();

        String locationCoordinates = "37.5846221554295 55.7513100141919";

        String searchHouseRequest = "Россия, " +
            "Республика Мордовия, " +
            "Городской округ Саранск, " +
            "Саранск, " +
            "recipient_street, " +
            "recipient_house, " +
            "recipient_housing, " +
            "recipient_building";
        mockGeoService(searchHouseRequest, locationCoordinates);

        mockDeliveryOption(
            defaultDeliverySearchRequestBuilder()
                .locationsTo(Set.of(217991))
                .deliveryServiceIds(Set.of(420L, 5L, 421L, 45L))
                .build()
        );

        mockCourierSchedule(lmsClient, 217991, Set.of(5L));

        createOrder(
            OrderDtoFactory.defaultOrderDraft()
                .andThen(order -> order.getCost().setFullyPrepaid(true))
                .andThen(order -> order.getDeliveryOption().setServices(
                    filterCashService(order.getDeliveryOption().getServices())
                ))
        )
            .andExpect(status().isOk());

        verify(geoClient).find(searchHouseRequest);
        verify(geoClient).find(locationCoordinates);
        verifyNoMoreInteractions(geoClient);
    }

    @Nonnull
    private GeoObject geoObjectWithParameters(String point, List<Component> components) {
        return defaultGeoObjectWithParameters(Kind.HOUSE, "217991", point, components);
    }

    @Test
    @DisplayName("Найден не скрытый под виртуальным партнером партнер в статусе TESTING для ЯДо")
    void invalidTestingPartner() throws Exception {
        SearchPartnerFilter partnerFilter = LmsFactory.createPartnerFilter(
            Set.of(5L),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        );

        when(lmsClient.searchPartners(partnerFilter)).thenReturn(List.of(
            PartnerResponse.newBuilder()
                .id(5)
                .platformClients(List.of(PlatformClientDto.newBuilder().id(3L).status(PartnerStatus.TESTING).build()))
                .build()
        ));

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [5]"));

        verify(lmsClient).searchPartners(partnerFilter);
    }

    @Test
    @DisplayName("Найден скрытый под виртуальным партнером партнер в статусе TESTING")
    void validTestingPartner() throws Exception {
        SearchPartnerFilter partnerFilter = LmsFactory.createPartnerFilter(
            Set.of(5L),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        );

        when(lmsClient.searchPartners(partnerFilter)).thenReturn(List.of(
            PartnerResponse.newBuilder()
                .id(5)
                .platformClients(List.of(PlatformClientDto.newBuilder().id(3L).status(PartnerStatus.TESTING).build()))
                .partnerType(PartnerType.DELIVERY)
                .subtype(PartnerSubtypeResponse.newBuilder().id(2).build())
                .build()
        ));

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isOk())
            .andExpect(content().json("1"));

        verify(lmsClient).searchPartners(partnerFilter);
    }

    @Test
    @DisplayName("Не найден скрытый под виртуальным партнером партнер в статусе TESTING")
    void testingPartnerNotFound() throws Exception {
        long tariffId = 100_033L;

        WaybillOrderRequestDto orderDto = createLomOrderRequest();
        orderDto.setCost(createLomOrderCost().tariffId(tariffId).build());

        when(tarifficatorClient.getTariff(tariffId)).thenReturn(TariffDto.builder().partnerId(5L).build());

        SearchPartnerFilter partnerFilter = LmsFactory.createPartnerFilter(
            Set.of(5L, 110L),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        );

        when(lmsClient.searchPartners(partnerFilter)).thenReturn(List.of(
            PartnerResponse.newBuilder()
                .id(110)
                .platformClients(List.of(PlatformClientDto.newBuilder().id(3L).status(PartnerStatus.ACTIVE).build()))
                .build()
        ));

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(
            o -> {
                OrderDraftDeliveryOption deliveryOption = o.getDeliveryOption();
                deliveryOption.setTariffId(tariffId);
                deliveryOption.setPartnerId(110L);
            }
        ))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [110]"));

        verify(lmsClient).searchPartners(partnerFilter);
    }

    @Test
    @DisplayName("Виртуальный партнер в статусе TESTING")
    void virtualPartnerInTesting() throws Exception {

        long tariffId = 100_033L;

        WaybillOrderRequestDto orderDto = createLomOrderRequest();
        orderDto.setCost(createLomOrderCost().tariffId(tariffId).build());

        when(tarifficatorClient.getTariff(tariffId)).thenReturn(TariffDto.builder().partnerId(5L).build());

        SearchPartnerFilter partnerFilter = LmsFactory.createPartnerFilter(
            Set.of(5L, 110L),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        );

        when(lmsClient.searchPartners(partnerFilter)).thenReturn(List.of(
            PartnerResponse.newBuilder()
                .id(110)
                .platformClients(List.of(PlatformClientDto.newBuilder().id(3L).status(PartnerStatus.TESTING).build()))
                .build(),
            PartnerResponse.newBuilder()
                .id(5)
                .platformClients(List.of(PlatformClientDto.newBuilder().id(3L).status(PartnerStatus.TESTING).build()))
                .build()
        ));

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(
            o -> {
                OrderDraftDeliveryOption deliveryOption = o.getDeliveryOption();
                deliveryOption.setTariffId(tariffId);
                deliveryOption.setPartnerId(110L);
            }
        ))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [110]"));

        verify(lmsClient).searchPartners(partnerFilter);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @SneakyThrows
    @MethodSource
    @DisplayName("Создание заказа с МК: СЦ в маршруте используется, будет построено 3 сегмента : "
        + "СЦ + локальный СЦ МК + МК")
    void createOrderWithMKSegment(
        @SuppressWarnings("unused") String displayName,
        @Nullable Long localSortingCenterId,
        boolean hasLocalSc,
        boolean tplCalling
    ) {
        //ищем локальный сц только для мк
        doThrow(new RuntimeException())
            .when(tplInternalClient).getSortingCenter(SORTING_CENTER_ID);
        doReturn(new SortingCenterInfoDto(localSortingCenterId))
            .when(tplInternalClient).getSortingCenter(5L);

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
        mockDeliveryOptionNoPickupPoint();

        createOrder(createOrderThroughSortingCenter())
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        WaybillOrderRequestDto expectedOrder = sortingCenterOrder(OrderDtoFactory.createLocation(5L));
        if (hasLocalSc) {
            expectedOrder.setWaybill(waybillWithSc());
        }
        // тк партнёр МК, то заполняются координаты
        expectedOrder.setRecipient(
            createRecipientBuilder()
                .address(
                    createAddressBuilder()
                        .longitude(BigDecimal.valueOf(37.5846221554295))
                        .latitude(BigDecimal.valueOf(55.7513100141919))
                        .build()
                )
                .build()
        );

        verifyLomOrderCreate(expectedOrder);
        verify(lmsClient, times(0)).getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(SORTING_CENTER_ID))
            .active(true)
            .build()));
        if (tplCalling) {
            verify(tplInternalClient).getSortingCenter(5L);
        }
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание заказа с МК: СЦ в маршруте используется, добавление локального сц включено, "
        + "сц не добавлен, так как совпадает с основным сц")
    void createOrderWithMKSegmentLocalScAndFirstCsAreSame() {
        //ищем локальный сц только для мк
        doThrow(new RuntimeException())
            .when(tplInternalClient).getSortingCenter(SORTING_CENTER_ID);
        doReturn(new SortingCenterInfoDto(3L))
            .when(tplInternalClient).getSortingCenter(5L);

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
        doReturn(List.of(SORTING_CENTER_WAREHOUSE_TO))
            .when(lmsClient).getLogisticsPoints(refEq(LmsFactory.createWarehousesFilter(Set.of(3L))));
        mockDeliveryOptionNoPickupPoint();

        createOrder(createOrderThroughSortingCenter())
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        WaybillOrderRequestDto expectedOrder = sortingCenterOrder(OrderDtoFactory.createLocation(5L));
        // тк партнёр МК, то заполняются координаты
        expectedOrder.setRecipient(
            createRecipientBuilder()
                .address(
                    createAddressBuilder()
                        .longitude(BigDecimal.valueOf(37.5846221554295))
                        .latitude(BigDecimal.valueOf(55.7513100141919))
                        .build()
                )
                .build()
        );

        verifyLomOrderCreate(expectedOrder);
        verify(lmsClient, times(0)).getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(SORTING_CENTER_ID))
            .active(true)
            .build()));
        verify(tplInternalClient).getSortingCenter(5L);
    }

    @Test
    @DisplayName("Создание заказа в постамат партнера GO")
    void createOrderToGoPartnerLocker() throws Exception {
        when(featureProperties.isEnableCombinatorRoute()).thenReturn(true);
        doReturn(new SortingCenterInfoDto(123L))
            .when(tplInternalClient).getSortingCenter(777L);
        long tariffId = 100_033L;

        WaybillOrderRequestDto orderDto = createLomOrderRequest();
        orderDto.setCost(
            createLomOrderCost()
                .services(addLomSortService(OrderDtoFactory.defaultLomDeliveryServices("3.86", "0.75")))
                .tariffId(tariffId)
                .build()
        );
        orderDto.setWaybill(waybillWithMiddleMilePickup());
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

        doReturn(List.of(
            LmsFactory.createPartner(SORTING_CENTER_ID, PartnerType.SORTING_CENTER),
            LmsFactory.createPartner(110L, PartnerType.DELIVERY),
            createPartnerResponseBuilder(5L, PartnerType.DELIVERY, 100L)
                .subtype(PartnerSubtypeResponse.newBuilder().id(PartnerSubtype.GO_PARTNER_LOCKER.getId()).build())
                .build(),
            createPartnerResponseBuilder(777L, PartnerType.DELIVERY, 100L)
                .subtype(PartnerSubtypeResponse.newBuilder().id(PartnerSubtype.MARKET_COURIER.getId()).build())
                .build()
        ))
            .when(lmsClient)
            .searchPartners(refEq(LmsFactory.createPartnerFilter(
                Set.of(SORTING_CENTER_ID, 5L, 110L, 777L),
                null,
                Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
            )));

        when(lmsClient.getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 5L, 101L), true))))
            .thenReturn(List.of(WAREHOUSE_FROM, SORTING_CENTER_WAREHOUSE_TO, PICKUP_POINT));

        doReturn(List.of(LOCAL_SORTING_CENTER))
            .when(lmsClient).getLogisticsPoints(refEq(LmsFactory.createWarehousesFilter(Set.of(123L))));
        when(tarifficatorClient.getTariff(tariffId)).thenReturn(TariffDto.builder().partnerId(777L).build());

        mockDeliveryOption(defaultDeliverySearchRequestBuilder().tariffId(tariffId).build());

        createOrder(createOrderThroughSortingCenter().andThen(
                    o -> o.getDeliveryOption()
                        .setPartnerId(110L)
                        .setTariffId(tariffId)
                )
                .andThen(d -> d.getRecipient().setPickupPointId(101L))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(orderDto);
        verify(tarifficatorClient).getTariff(tariffId);
        verify(tplInternalClient).getSortingCenter(777L);
        verifyNoMoreInteractions(combinatorGrpcClient);
    }

    @Nonnull
    private static Stream<Arguments> createOrderWithMKSegment() {
        return Stream.of(
            Arguments.of(
                "Построение маршрута с добавлением локального СЦ",
                LOCAL_SORTING_CENTER_PARTNER_ID,
                true,
                true
            ),
            Arguments.of(
                "В tpl нет найден СЦ по СД, локальный СЦ не добавляется",
                null,
                false,
                true
            )
        );
    }

    @Nonnull
    protected List<WaybillSegmentDto> waybillWithSc() {
        return List.of(
            OrderDtoFactory.createWaybillSegmentBuilder(
                    INITIAL_SHIPMENT_DATE,
                    OrderDtoFactory.createLocation(3L),
                    OrderDtoFactory.createLocation(5L),
                    SORTING_CENTER_ID,
                    ShipmentType.WITHDRAW
                )
                .segmentType(SegmentType.SORTING_CENTER)
                .build(),
            OrderDtoFactory.createWaybillSegmentBuilder(
                    INITIAL_SHIPMENT_DATE,
                    OrderDtoFactory.createLocation(5L),
                    OrderDtoFactory.createLocation(LOCAL_SORTING_CENTER_ID),
                    LOCAL_SORTING_CENTER_PARTNER_ID,
                    ShipmentType.WITHDRAW
                )
                .segmentType(SegmentType.SORTING_CENTER)
                .build(),
            OrderDtoFactory.createWaybillSegmentBuilder(
                    INITIAL_SHIPMENT_DATE,
                    OrderDtoFactory.createLocation(LOCAL_SORTING_CENTER_ID),
                    null,
                    5L,
                    ShipmentType.WITHDRAW
                )
                .segmentType(SegmentType.COURIER)
                .build()
        );
    }

    @Nonnull
    protected PartnerResponse createMkPartner() {
        return createPartnerResponseBuilder(5L, PartnerType.DELIVERY, 100L)
            .subtype(PartnerSubtypeResponse.newBuilder().id(2).name("Маркет Курьер").build())
            .build();
    }

    protected void mockVirtualDeliveryService() {
        doReturn(List.of(
            createPartnerResponseBuilder(420, PartnerType.DELIVERY, 100L)
                .intakeSchedule(List.of(createScheduleDayDto(1)))
                .subtype(PartnerSubtypeResponse.newBuilder().id(2L).name("Маркет Курьер").build())
                .build(),
            createPartnerResponseBuilder(421, PartnerType.DELIVERY, 100L)
                .intakeSchedule(List.of(createScheduleDayDto(1)))
                .subtype(PartnerSubtypeResponse.newBuilder().id(2L).name("Маркет Курьер").build())
                .build()
        )).when(lmsClient).searchPartners(
            SearchPartnerFilter.builder()
                .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
                .setPlatformClientStatuses(EnumSet.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING))
                .setStatuses(Set.of(PartnerStatus.ACTIVE))
                .setPartnerSubTypeIds(Set.of(2L))
                .build()
        );
    }

    private void mockGeoService(String searchHouseRequest, String locationCoordinates) {
        when(geoClient.find(searchHouseRequest))
            .thenReturn(List.of(geoObjectWithParameters(locationCoordinates, List.of(
                new Component("Russia", List.of(Kind.COUNTRY)),
                new Component("Novosibirsk Area", List.of(Kind.PROVINCE)),
                new Component("Novosibirsk", List.of(Kind.LOCALITY)),
                new Component("Akademgorodok", List.of(Kind.DISTRICT)),
                new Component("recipient_street", List.of(Kind.STREET)),
                new Component("recipient_housing", List.of(Kind.HOUSE))
            ))));

        when(geoClient.find(locationCoordinates))
            .thenReturn(List.of(geoObjectWithParameters("", List.of(
                    new Component("Russia", List.of(Kind.COUNTRY)),
                    new Component("Novosibirsk Area", List.of(Kind.PROVINCE)),
                    new Component("Novosibirsk", List.of(Kind.LOCALITY)),
                    new Component("Akademgorodok", List.of(Kind.DISTRICT))
                )
            )));
    }

    @Nonnull
    private List<WaybillSegmentDto> waybillWithMiddleMilePickup() {
        return List.of(
            createWithdrawBuilder()
                .partnerId(6L)
                .segmentType(SegmentType.SORTING_CENTER)
                .shipment(
                    ShipmentDto.builder()
                        .type(ShipmentType.WITHDRAW)
                        .date(LocalDate.of(2019, 8, 1))
                        .locationFrom(OrderDtoFactory.createLocation(3L))
                        .locationTo(OrderDtoFactory.createLocation(5L))
                        .build()
                )
                .build(),
            createWithdrawBuilder()
                .partnerId(LOCAL_SORTING_CENTER_PARTNER_ID)
                .segmentType(SegmentType.SORTING_CENTER)
                .shipment(
                    ShipmentDto.builder()
                        .type(ShipmentType.WITHDRAW)
                        .date(LocalDate.of(2019, 8, 1))
                        .locationFrom(OrderDtoFactory.createLocation(5L))
                        .locationTo(OrderDtoFactory.createLocation(LOCAL_SORTING_CENTER_ID))
                        .build()
                )
                .build(),
            createWithdrawBuilder()
                .partnerId(777L)
                .segmentType(SegmentType.MOVEMENT)
                .shipment(
                    ShipmentDto.builder()
                        .type(ShipmentType.WITHDRAW)
                        .date(LocalDate.of(2019, 8, 1))
                        .locationFrom(OrderDtoFactory.createLocation(LOCAL_SORTING_CENTER_ID))
                        .locationTo(OrderDtoFactory.createLocation(101L, PICKUP))
                        .build()
                )
                .build(),
            createWithdrawBuilder()
                .partnerId(5L)
                .segmentType(SegmentType.PICKUP)
                .shipment(
                    ShipmentDto.builder()
                        .date(LocalDate.of(2019, 8, 1))
                        .locationFrom(OrderDtoFactory.createLocation(LOCAL_SORTING_CENTER_ID))
                        .locationTo(OrderDtoFactory.createLocation(101L, PICKUP))
                        .build()
                )
                .build()
        );
    }

    @Nonnull
    private List<WaybillSegmentDto> waybillWithMiddleMileFromMk() {
        return List.of(
            createWithdrawBuilder()
                .partnerId(6L)
                .segmentType(SegmentType.SORTING_CENTER)
                .shipment(
                    ShipmentDto.builder()
                        .type(ShipmentType.WITHDRAW)
                        .date(LocalDate.of(2019, 8, 1))
                        .locationFrom(OrderDtoFactory.createLocation(3L))
                        .locationTo(OrderDtoFactory.createLocation(5L))
                        .build()
                )
                .build(),
            createWithdrawBuilder()
                .partnerId(LOCAL_SORTING_CENTER_PARTNER_ID)
                .segmentType(SegmentType.SORTING_CENTER)
                .shipment(
                    ShipmentDto.builder()
                        .type(ShipmentType.WITHDRAW)
                        .date(LocalDate.of(2019, 8, 1))
                        .locationFrom(OrderDtoFactory.createLocation(5L))
                        .locationTo(OrderDtoFactory.createLocation(LOCAL_SORTING_CENTER_ID))
                        .build()
                )
                .build(),
            createWithdrawBuilder()
                .partnerId(777L)
                .segmentType(SegmentType.MOVEMENT)
                .shipment(
                    ShipmentDto.builder()
                        .type(ShipmentType.WITHDRAW)
                        .date(LocalDate.of(2019, 8, 1))
                        .locationFrom(OrderDtoFactory.createLocation(LOCAL_SORTING_CENTER_ID))
                        .locationTo(OrderDtoFactory.createLocation(101L, PICKUP))
                        .build()
                )
                .build(),
            createWithdrawBuilder()
                .partnerId(5L)
                .segmentType(SegmentType.PICKUP)
                .shipment(
                    ShipmentDto.builder()
                        .date(LocalDate.of(2019, 8, 1))
                        .locationFrom(OrderDtoFactory.createLocation(LOCAL_SORTING_CENTER_ID))
                        .locationTo(OrderDtoFactory.createLocation(101L, PICKUP))
                        .build()
                )
                .build()
        );
    }
}
