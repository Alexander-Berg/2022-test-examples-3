package ru.yandex.market.logistics.nesu.base.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.Component;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.util.collections.Triple;
import ru.yandex.market.logistics.delivery.calculator.client.DeliveryCalculatorSearchEngineClient;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOption;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchRequest;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchResponse;
import ru.yandex.market.logistics.delivery.calculator.client.model.TariffType;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.RecipientDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto.ShipmentDto;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.model.enums.tags.OrderTag;
import ru.yandex.market.logistics.lom.model.error.FieldError;
import ru.yandex.market.logistics.lom.model.error.ValidationError;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.base.OrderTestUtils;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.dto.order.OrdersSubmitRequest;
import ru.yandex.market.logistics.nesu.jobs.producer.RegisterOrderCapacityProducer;
import ru.yandex.market.logistics.nesu.model.GeoSearchFactory;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;
import ru.yandex.market.logistics.nesu.service.sender.SenderService;
import ru.yandex.market.logistics.nesu.utils.SenderAvailableDeliveriesUtils;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createDefaultLomPlace;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createRootUnit;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultLomOrderService;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartnerResponseBuilder;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createScheduleDayDto;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractSubmitOrderTest extends AbstractContextualTest {

    protected static final long SHOP_ID = 2;
    private static final int MAX_DELIVERY_DAYS = 5;
    private static final Instant INSTANT = Instant.parse("2019-02-02T12:00:00.00Z");

    @Autowired
    protected LomClient lomClient;
    @Autowired
    private SenderService senderService;
    @Autowired
    protected RegisterOrderCapacityProducer registerOrderCapacityProducer;
    @Autowired
    protected DeliveryCalculatorSearchEngineClient deliveryCalculatorSearchEngineClient;
    @Autowired
    protected LMSClient lmsClient;
    @Autowired
    private GeoClient geoClient;
    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setupProducer() {
        doNothing().when(registerOrderCapacityProducer).produceTask(anyLong());
        clock.setFixed(INSTANT, ZoneId.systemDefault());
    }

    @AfterEach
    void verifyProducer() {
        verifyNoMoreInteractions(registerOrderCapacityProducer);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("requestValidationProvider")
    @DisplayName("Валидация запроса")
    void requestValidation(ValidationErrorData error, List<Long> orderIds) throws Exception {
        submitOrders(orderIds)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    private static Stream<Arguments> requestValidationProvider() {
        return Stream.of(
            Arguments.of(fieldError("orderIds", "must not be null", "ordersSubmitRequest", "NotNull"), null),
            Arguments.of(
                fieldError(
                    "orderIds",
                    "size must be between 1 and 100",
                    "ordersSubmitRequest",
                    "Size",
                    Map.of("min", 1, "max", 100)
                ),
                List.of()
            ),
            Arguments.of(
                fieldError(
                    "orderIds",
                    "size must be between 1 and 100",
                    "ordersSubmitRequest",
                    "Size",
                    Map.of("min", 1, "max", 100)
                ),
                LongStream.range(0, 101).boxed().collect(Collectors.toList())
            ),
            Arguments.of(
                fieldError("orderIds", "must not contain nulls", "ordersSubmitRequest", "NotNullElements"),
                Collections.singletonList(null)
            )
        );
    }

    @Test
    @DisplayName("Неизвестный заказ")
    void unknownOrder() throws Exception {
        long orderId = 100L;

        mockGetOrder(orderId, null);

        submitOrders(List.of(orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_order_not_found.json"));

        verify(lomClient, never()).commitOrder(orderId);
    }

    @Test
    @DisplayName("Неправильный идентификатор клиента платформы")
    void invalidPlatformClientId() throws Exception {
        long orderId = 100L;

        mockGetOrder(orderId, new OrderDto().setId(orderId).setPlatformClientId(2L));

        submitOrders(List.of(orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_order_not_found.json"));

        verify(lomClient, never()).commitOrder(orderId);
    }

    @Test
    @DisplayName("Заказ, принадлежащий неизвестному сендеру")
    void unknownSender() throws Exception {
        long orderId = 101L;

        mockGetOrder(orderId, order(orderId, 10));

        submitOrders(List.of(orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_sender_not_found.json"));

        verify(lomClient, never()).commitOrder(orderId);
    }

    @Test
    @DisplayName("Заказ, принадлежащий отключенному магазину")
    @DatabaseSetup(value = "/repository/shop/before/disabled_shop.xml", type = DatabaseOperation.UPDATE)
    void disabledShop() throws Exception {
        long orderId = 100L;

        mockGetOrder(orderId, order(orderId, 11));

        submitOrders(List.of(orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_order_not_found.json"));

        verify(lomClient, never()).commitOrder(orderId);
    }

    @Test
    @DisplayName("Заказ в неправильном статусе")
    void invalidStatus() throws Exception {
        long orderId = 101L;

        mockGetOrder(orderId, order(orderId, 12).setStatus(OrderStatus.CANCELLED));

        submitOrders(List.of(orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_invalid_status.json"));

        verify(lomClient, never()).commitOrder(orderId);
    }

    @Test
    @DisplayName("Ошибка первичной валидации")
    void validationError() throws Exception {
        long orderId = 103L;

        mockSuccessfulOrder(orderId, 12, ValidationError.builder()
            .fieldErrors(List.of(
                FieldError.builder()
                    .propertyPath("testField")
                    .message("test message")
                    .build()
            ))
            .build());
        mockGetSortingCenterWarehouse();
        mockGetSenderAvailableDeliveries();

        submitOrders(List.of(orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_validation_error.json"));
    }

    @Test
    @DisplayName("Ошибка валидации, null в fieldErrors")
    void validationErrorNullInFieldErrors() throws Exception {
        long orderId = 103L;

        mockSuccessfulOrder(orderId, 12, ValidationError.builder().fieldErrors(null).build());
        mockGetSortingCenterWarehouse();
        mockGetSenderAvailableDeliveries();

        submitOrders(List.of(orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_validation_error_null_in_field_errors.json"));
    }

    @Test
    @DisplayName("Успех")
    void success() throws Exception {
        long orderId = 104L;

        mockSuccessfulOrder(orderId, 12);
        mockGetSortingCenterWarehouse();
        mockGetSenderAvailableDeliveries();

        submitOrders(List.of(orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_success.json"));

        verify(registerOrderCapacityProducer).produceTask(orderId);
    }

    @Test
    @DisplayName("Успех сабмита заказа из нескольких коробок")
    void successSeveralBoxes() throws Exception {
        long orderId = 104L;
        long senderId = 12L;

        mockGetOrder(
            orderId,
            orderWithOptions(orderId, senderId).setUnits(
                List.of(
                    createRootUnit(),
                    createDefaultLomPlace(10, 100, 40, "externalId-1"),
                    createDefaultLomPlace(80, 10, 50, "externalId-2"),
                    createDefaultLomPlace(20, 60, 60, "externalId-3")
                )
            )
        );
        OrderTestUtils.mockDeliveryOptionValidation(
            MAX_DELIVERY_DAYS,
            deliveryCalculatorSearchEngineClient,
            lmsClient,
            DeliverySearchRequest.builder()
                .locationFrom(213)
                .locationsTo(Set.of(213))
                .weight(BigDecimal.valueOf(50))
                .length(45)
                .width(30)
                .height(15)
                .deliveryServiceIds(Set.of(5L))
                .tariffId(42L)
                .senderId(senderId)
                .build()
        );
        mockGetSortingCenterWarehouse();
        mockGetSenderAvailableDeliveries();
        OrderTestUtils.mockCourierSchedule(lmsClient, 213, Set.of(5L));
        when(lomClient.commitOrder(orderId)).thenReturn(Optional.empty());

        submitOrders(List.of(orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_success.json"));

        verify(registerOrderCapacityProducer).produceTask(orderId);
    }

    @Test
    @DisplayName("Несколько заказов от одного сендера, один запрос сендера в БД")
    void sameSenderSingleCall() throws Exception {
        mockSuccessfulOrder(105, 12);
        mockSuccessfulOrder(106, 12);
        mockGetSortingCenterWarehouse();
        mockGetSenderAvailableDeliveries();

        submitOrders(List.of(105L, 106L))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_success_multiple.json"));

        verify(senderService).findActiveSender(12L);
        verifyNoMoreInteractions(senderService);

        verify(registerOrderCapacityProducer).produceTask(105L);
        verify(registerOrderCapacityProducer).produceTask(106L);
    }

    @Test
    @DisplayName("Валидация опций доставки, не найдена опция")
    void deliveryOptionValidationNotFound() throws Exception {
        when(deliveryCalculatorSearchEngineClient.deliverySearch(any()))
            .thenReturn(DeliverySearchResponse.builder().deliveryOptions(List.of()).build());

        mockGetOrder(104L, orderWithOptions(104, 12));

        mockGetSortingCenterWarehouse();
        mockGetSenderAvailableDeliveries();
        when(lmsClient.getPartner(6L)).thenReturn(
            Optional.of(LmsFactory.createPartner(6L, PartnerType.SORTING_CENTER))
        );

        submitOrders(List.of(104L))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_delivery_option_not_found.json"));
    }

    @Test
    @DisplayName("Валидация опций доставки")
    void deliveryOptionValidation() throws Exception {
        long warehouseFromId = 500;
        OrderDto order = orderWithOptions(104, 12)
            .setCost(
                CostDto.builder()
                    .tariffId(42L)
                    .assessedValue(BigDecimal.TEN)
                    .services(List.of(
                        defaultLomOrderService()
                            .code(ShipmentOption.WAIT_20)
                            .customerPay(false)
                            .cost(BigDecimal.valueOf(42))
                            .build(),
                        defaultLomOrderService()
                            .code(ShipmentOption.INSURANCE)
                            .customerPay(true)
                            .cost(new BigDecimal("0.006"))
                            .build(),
                        defaultLomOrderService().code(ShipmentOption.SORT).cost(BigDecimal.valueOf(27)).build(),
                        defaultLomOrderService().code(ShipmentOption.RETURN)
                            .customerPay(false)
                            .cost(BigDecimal.valueOf(0.75))
                            .build(),
                        defaultLomOrderService().code(ShipmentOption.RETURN_SORT)
                            .customerPay(false)
                            .cost(BigDecimal.valueOf(20))
                            .build()
                    ))
                    .build()
            )
            .setDeliveryInterval(DeliveryIntervalDto.builder().build())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerId(6L)
                    .shipment(
                        ShipmentDto.builder()
                            .type(ShipmentType.WITHDRAW)
                            .locationFrom(LocationDto.builder().warehouseId(warehouseFromId).build())
                            .build()
                    )
                    .build()
            ));
        mockGetOrder(104L, order);
        OrderTestUtils.mockDeliveryOptionValidation(
            MAX_DELIVERY_DAYS,
            deliveryCalculatorSearchEngineClient,
            lmsClient,
            deliveryOptionSearchRequest(12L)
        );
        OrderTestUtils.mockCourierSchedule(lmsClient, 213, Set.of(5L));
        when(lmsClient.getLogisticsPoint(warehouseFromId))
            .thenReturn(Optional.of(
                LmsFactory.createLogisticsPointResponse(
                    warehouseFromId,
                    41L,
                    null,
                    "Shop warehouse",
                    PointType.WAREHOUSE
                )
            ));
        mockGetSortingCenterWarehouse();
        mockGetSenderAvailableDeliveries();
        when(lomClient.searchShipments(any(), any())).thenAnswer(i -> PageResult.empty(i.getArgument(1)));

        submitOrders(List.of(104L))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_delivery_option_validation.json"));

        ArgumentCaptor<ShipmentSearchFilter> filterCaptor = ArgumentCaptor.forClass(ShipmentSearchFilter.class);
        // поиск заявок на сегодня + поиск уже созданных отгрузок
        verify(lomClient, times(2)).searchShipments(filterCaptor.capture(), any());
        softly.assertThat(filterCaptor.getAllValues())
            .extracting(ShipmentSearchFilter::getFromDate)
            .containsExactly(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 2, 4));
    }

    @Test
    @DisplayName("Валидация опций доставки, найдено более одной опции")
    void deliveryOptionValidationMultipleOption() throws Exception {
        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(Set.of(5L), null, Set.of(PartnerStatus.ACTIVE))))
            .thenReturn(List.of(LmsFactory.createPartner(5L, PartnerType.DELIVERY)));
        mockGetSortingCenterWarehouse();
        mockGetSenderAvailableDeliveries();
        when(lmsClient.getPartner(6L))
            .thenReturn(Optional.of(LmsFactory.createPartner(6L, 101L, PartnerType.SORTING_CENTER)));

        OrderTestUtils.mockCourierSchedule(lmsClient, 213, Set.of(5L));

        DeliveryOption.DeliveryOptionBuilder optionBuilder = DeliveryOption.builder()
            .deliveryServiceId(5L)
            .tariffType(TariffType.COURIER)
            .services(OrderTestUtils.defaultDeliveryOptionServices(100))
            .maxDays(MAX_DELIVERY_DAYS);
        when(deliveryCalculatorSearchEngineClient.deliverySearch(any()))
            .thenReturn(DeliverySearchResponse.builder().deliveryOptions(
                List.of(optionBuilder.build(), optionBuilder.cost(44L).tariffId(38L).build())
            ).build());

        mockGetOrder(104L, orderWithOptions(104, 12));

        submitOrders(List.of(104L))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("message")
                .value(containsString("При валидации опций доставки было получено более одной опции")));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("incompleteOrderValidationSource")
    @DisplayName("Валидация опций доставки, недостаточно данных в заказе")
    void deliveryOptionValidationIncompleteOrder(
        @SuppressWarnings("unused") String displayName,
        Consumer<OrderDto> orderModifier,
        String responsePath
    ) throws Exception {
        OrderDto orderDto = orderWithOptions(104, 12);
        orderModifier.accept(orderDto);
        mockGetOrder(104L, orderDto);

        submitOrders(List.of(104L))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> incompleteOrderValidationSource() {
        return Stream.<Triple<String, Consumer<OrderDto>, String>>of(
            Triple.of(
                "Не хватает размеров заказа или размеров грузомест",
                o -> o.setUnits(null),
                "controller/order/submit/submit_incomplete_order_places.json"
            ),
            Triple.of(
                "Не указан получатель",
                o -> o.setRecipient(null),
                "controller/order/submit/submit_incomplete_order_recipient.json"
            ),
            Triple.of(
                "Не указан тариф",
                o -> o.setCost(o.getCost().toBuilder().tariffId(null).build()),
                "controller/order/submit/submit_incomplete_order_tariff_id.json"
            ),
            Triple.of(
                "Не указаны параметры отгрузки",
                o -> {
                    WaybillSegmentDto segment = o.getWaybill().get(0);
                    o.setWaybill(List.of(
                        segment.toBuilder()
                            .partnerId(null)
                            .shipment(segment.getShipment().toBuilder().type(null).build())
                            .build()
                    ));
                },
                "controller/order/submit/submit_incomplete_order_tariff_shipment.json"

            )
        )
            .map(t -> Arguments.of(t.first, t.second, t.third));
    }

    @Test
    @DisplayName("Успешный сабмит курьерского заказа магазина GO")
    @DatabaseSetup(
        value = "/controller/order/submit/submit_market_courier.xml",
        type = DatabaseOperation.UPDATE
    )
    void successGoShopMkOrder() throws Exception {
        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(List.of(2L));
        long orderId = 104L;
        long senderId = 12;
        mockVirtualDeliveryService();
        mockGetOrder(orderId, courierOrderWithOptions(orderId, senderId));
        OrderTestUtils.mockDeliveryOptionValidation(
            MAX_DELIVERY_DAYS,
            deliveryCalculatorSearchEngineClient,
            lmsClient,
            marketCourierDeliveryOptionSearchRequest(senderId)
        );
        mockGeoService();
        when(lmsClient.getLogisticsPoints(LmsFactory.createWarehousesFilter(Set.of(5L, 6L, 53916L))))
            .thenReturn(List.of(LmsFactory.createLogisticsPointResponse(
                60L,
                6L,
                "Sorting center warehouse",
                PointType.WAREHOUSE
            )));
        OrderTestUtils.mockCourierSchedule(lmsClient, 213, Set.of(5L));
        when(lomClient.commitOrder(orderId, getTag())).thenReturn(Optional.empty());
        mockGetSortingCenterWarehouse();
        mockGetSenderAvailableDeliveriesMarketCourier();

        submitOrders(List.of(orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_success.json"));

        verify(registerOrderCapacityProducer).produceTask(orderId);
    }

    protected void mockSuccessfulOrder(long orderId, long senderId) {
        mockSuccessfulOrder(orderId, senderId, null);
    }

    protected void mockSuccessfulOrder(
        long orderId,
        long senderId,
        @Nullable ValidationError errors
    ) {
        mockGetOrder(orderId, orderWithOptions(orderId, senderId));
        OrderTestUtils.mockDeliveryOptionValidation(
            MAX_DELIVERY_DAYS,
            deliveryCalculatorSearchEngineClient,
            lmsClient,
            deliveryOptionSearchRequest(senderId)
        );
        OrderTestUtils.mockCourierSchedule(lmsClient, 213, Set.of(5L));
        when(lomClient.commitOrder(orderId, getTag())).thenReturn(Optional.ofNullable(errors));
    }

    protected void mockGetOrder(long orderId, @Nullable OrderDto orderDto) {
        when(lomClient.getOrder(orderId, Set.of())).thenReturn(Optional.ofNullable(orderDto));
    }

    protected void mockGetSortingCenterWarehouse() {
        when(lmsClient.getLogisticsPoint(60L))
            .thenReturn(Optional.of(
                LmsFactory.createLogisticsPointResponse(60L, 6L, "Sorting center warehouse", PointType.WAREHOUSE)
            ));
    }

    protected void mockGetSenderAvailableDeliveries() {
        SenderAvailableDeliveriesUtils.mockGetSenderAvailableDeliveries(
            lmsClient,
            LmsFactory.createPartner(6L, PartnerType.SORTING_CENTER),
            List.of(LmsFactory.createPartner(5L, PartnerType.DELIVERY)),
            List.of(LmsFactory.createLogisticsPointResponse(60L, 6L, "SC warehouse", PointType.WAREHOUSE))
        );
    }

    protected void mockGetSenderAvailableDeliveriesMarketCourier() {
        SenderAvailableDeliveriesUtils.mockGetSenderAvailableDeliveries(
            lmsClient,
            LmsFactory.createPartner(6L, PartnerType.SORTING_CENTER),
            List.of(LmsFactory.createPartner(53916L, PartnerType.DELIVERY)),
            List.of(LmsFactory.createLogisticsPointResponse(60L, 6L, "SC warehouse", PointType.WAREHOUSE))
        );
    }

    @Nonnull
    protected OrderDto order(long id, long senderId) {
        return new OrderDto()
            .setPlatformClientId(3L)
            .setId(id)
            .setSenderId(senderId)
            .setStatus(OrderStatus.DRAFT);
    }

    @Nonnull
    private OrderDto orderWithOptions(long id, long senderId) {
        return orderWithOptions(id, senderId, ShipmentType.WITHDRAW);
    }

    @Nonnull
    private OrderDto orderWithOptions(long id, long senderId, ShipmentType shipmentType) {
        return order(id, senderId)
            .setUnits(List.of(
                StorageUnitDto.builder()
                    .type(StorageUnitType.ROOT)
                    .dimensions(
                        KorobyteDto.builder()
                            .weightGross(BigDecimal.valueOf(10L))
                            .length(10)
                            .width(3)
                            .height(44)
                            .build()
                    )
                    .build(),
                StorageUnitDto.builder()
                    .type(StorageUnitType.PLACE)
                    .dimensions(
                        KorobyteDto.builder()
                            .weightGross(BigDecimal.valueOf(10L))
                            .length(10)
                            .width(3)
                            .height(44)
                            .build()
                    )
                    .build()
            ))
            .setRecipient(RecipientDto.builder().address(AddressDto.builder().geoId(213).build()).build())
            .setCost(
                CostDto.builder()
                    .tariffId(42L)
                    .manualDeliveryForCustomer(BigDecimal.valueOf(10000))
                    .assessedValue(BigDecimal.TEN)
                    .services(List.of(
                        defaultLomOrderService()
                            .code(ShipmentOption.CASH_SERVICE)
                            .cost(BigDecimal.valueOf(170))
                            .customerPay(false)
                            .build(),
                        defaultLomOrderService()
                            .code(ShipmentOption.INSURANCE)
                            .cost(new BigDecimal("0.06"))
                            .build(),
                        defaultLomOrderService()
                            .code(ShipmentOption.SORT)
                            .cost(BigDecimal.valueOf(27))
                            .customerPay(false)
                            .build(),
                        defaultLomOrderService()
                            .code(ShipmentOption.RETURN)
                            .cost(BigDecimal.valueOf(0.75))
                            .customerPay(false)
                            .build(),
                        defaultLomOrderService()
                            .code(ShipmentOption.RETURN_SORT)
                            .cost(BigDecimal.valueOf(20))
                            .customerPay(false)
                            .build()
                    ))
                    .build()
            )
            .setDeliveryInterval(
                DeliveryIntervalDto.builder()
                    .deliveryDateMin(LocalDate.of(2019, 2, 5))
                    .deliveryDateMax(LocalDate.of(2019, 2, 12))
                    .build()
            )
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerId(6L)
                    .shipment(
                        ShipmentDto.builder()
                            .type(shipmentType)
                            .date(LocalDate.of(2019, 2, 4))
                            .build()
                    )
                    .build()
            ));
    }

    @Nonnull
    private OrderDto courierOrderWithOptions(long id, long senderId) {
        return orderWithOptions(id, senderId, ShipmentType.IMPORT)
            .setDeliveryType(DeliveryType.COURIER)
            .setRecipient(
                RecipientDto.builder()
                    .address(
                        AddressDto.builder()
                            .geoId(213)
                            .country("Россия")
                            .locality("Москва")
                            .street("Толстого")
                            .house("16")
                            .latitude(new BigDecimal("55.73397404565889"))
                            .longitude(new BigDecimal("37.587092522460836"))
                            .build()
                    )
                    .build()
            );
    }

    @Nonnull
    private DeliverySearchRequest deliveryOptionSearchRequest(Long senderId) {
        return DeliverySearchRequest.builder()
            .locationFrom(213)
            .locationsTo(Set.of(213))
            .weight(BigDecimal.TEN)
            .length(10)
            .width(3)
            .height(44)
            .deliveryServiceIds(Set.of(5L))
            .tariffId(42L)
            .senderId(senderId)
            .build();
    }

    @Nonnull
    private DeliverySearchRequest marketCourierDeliveryOptionSearchRequest(Long senderId) {
        return DeliverySearchRequest.builder()
            .locationFrom(213)
            .locationsTo(Set.of(213))
            .weight(BigDecimal.TEN)
            .length(10)
            .width(3)
            .height(44)
            .deliveryServiceIds(Set.of(5L))
            .tariffType(TariffType.COURIER)
            .tariffId(42L)
            .senderId(senderId)
            .build();
    }

    @Nonnull
    protected ResultActions submitOrders(List<Long> orderIds) throws Exception {
        OrdersSubmitRequest request = new OrdersSubmitRequest();
        request.setOrderIds(orderIds);
        return submitOrders(request);
    }

    @Nonnull
    protected abstract ResultActions submitOrders(OrdersSubmitRequest request) throws Exception;

    @Nonnull
    protected abstract OrderTag getTag();

    protected void mockVirtualDeliveryService() {
        doReturn(List.of(
            createPartnerResponseBuilder(5, PartnerType.DELIVERY, 100L)
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

    private void mockGeoService() {
        String locationCoordinates = "37.587092522460836 55.73397404565889";
        when(geoClient.find(locationCoordinates))
            .thenReturn(List.of(
                GeoSearchFactory.geoObject(
                    Kind.LOCALITY,
                    "213",
                    locationCoordinates,
                    List.of(new Component("Москва", List.of(Kind.LOCALITY)))
                )
            ));
    }
}
