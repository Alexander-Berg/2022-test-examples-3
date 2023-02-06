package ru.yandex.market.logistics.logistics4go.controller.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4go.client.model.CancelOrderResponse;
import ru.yandex.market.logistics.logistics4go.client.model.CancellationRequestReason;
import ru.yandex.market.logistics.logistics4go.client.model.CancellationRequestStatus;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeOrderRequestDto;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeRequestReason;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeRequestStatus;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeRequestType;
import ru.yandex.market.logistics.logistics4go.client.model.ContactDto;
import ru.yandex.market.logistics.logistics4go.client.model.Cost;
import ru.yandex.market.logistics.logistics4go.client.model.Dimensions;
import ru.yandex.market.logistics.logistics4go.client.model.GetOrderResponse;
import ru.yandex.market.logistics.logistics4go.client.model.Interval;
import ru.yandex.market.logistics.logistics4go.client.model.Item;
import ru.yandex.market.logistics.logistics4go.client.model.ItemInstanceDto;
import ru.yandex.market.logistics.logistics4go.client.model.ItemSupplier;
import ru.yandex.market.logistics.logistics4go.client.model.OrderAvailableActionsDto;
import ru.yandex.market.logistics.logistics4go.client.model.OrderStatus;
import ru.yandex.market.logistics.logistics4go.client.model.OrderStatusHistoryDto;
import ru.yandex.market.logistics.logistics4go.client.model.PartnerType;
import ru.yandex.market.logistics.logistics4go.client.model.PaymentMethod;
import ru.yandex.market.logistics.logistics4go.client.model.PersonName;
import ru.yandex.market.logistics.logistics4go.client.model.Phone;
import ru.yandex.market.logistics.logistics4go.client.model.Place;
import ru.yandex.market.logistics.logistics4go.client.model.ReturnInfoDto;
import ru.yandex.market.logistics.logistics4go.client.model.VatRate;
import ru.yandex.market.logistics.logistics4go.client.model.WaybillSegmentDto;
import ru.yandex.market.logistics.logistics4go.client.model.WaybillSegmentStatus;
import ru.yandex.market.logistics.logistics4go.client.model.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.logistics4go.client.model.WaybillSegmentType;
import ru.yandex.market.logistics.logistics4go.utils.LomFactory;
import ru.yandex.market.logistics.logistics4go.utils.OrderFactory;
import ru.yandex.market.logistics.lom.model.dto.OrderActionsDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4go.utils.LomFactory.unit;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.YANDEX_INN;

@DisplayName("Ручка получения информации о заказе")
class GetOrderTest extends AbstractOrderTest {

    @Test
    @DisplayName("Успех (все поддерживаемые поля в заказе)")
    @DatabaseSetup("/controller/order/get/cancelled_order_event.xml")
    void successFull() throws Exception {
        try (var ignored = mockLomClientSearchOrders(LomFactory.order(false))) {
            String lomOrderAsString = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .asString();
            IntegrationTestUtils.assertJson("controller/order/get/lom_get_order.json", lomOrderAsString);
        }
    }

    @Test
    @DisplayName("Успех (минимальный набор полей в заказе)")
    void successRequired() throws Exception {
        try (var ignored = mockLomClientSearchOrders(LomFactory.order(true))) {
            GetOrderResponse response = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly
                .assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse(true));
        }
    }

    @Test
    @DisplayName("Успех (все поддерживаемые поля в заказе) с удалением персональных данных")
    @DatabaseSetup("/controller/order/get/cancelled_order_event.xml")
    @DatabaseSetup("/controller/order/get/remove_recipient_from_get_order.xml")
    void successFullWithoutRecipient() throws Exception {
        try (var ignored = mockLomClientSearchOrders(LomFactory.order(false))) {
            String lomOrderAsString = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .asString();
            IntegrationTestUtils.assertJson("controller/order/get/lom_get_order_no_recipient.json", lomOrderAsString);
        }
    }

    @Test
    @DisplayName("Успешное получение заказа с возвратным дропоффом, который отличается от дропоффа в прямом маршруте")
    @DatabaseSetup("/controller/order/get/cancelled_order_event.xml")
    void successWithReturnDropoff() throws Exception {
        try (var ignored = mockLomClientSearchOrders(LomFactory.orderWithAdditionalReturnSegments())) {
            GetOrderResponse response = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly
                .assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponseWithReturnDropoff());
        }
    }

    @Test
    @DisplayName("Несуществующий заказ")
    void noSuchOrder() throws Exception {
        try (var ignored = mockLomClientSearchOrders(null)) {
            apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .executeAs(validatedWith(shouldBeCode(SC_NOT_FOUND)));
        }
    }

    @Test
    @DisplayName("Типы запросов на изменения, которых нет в модели L4G фильтруются + проходят все, которые есть")
    void changeRequestTypeFilter() throws Exception {
        OrderDto lomOrder = LomFactory.order(true)
            .setChangeOrderRequests(
                Arrays.stream(ChangeOrderRequestType.values())
                    .map(
                        type -> ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto.builder()
                            .status(ChangeOrderRequestStatus.CREATED)
                            .reason(ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_RECIPIENT)
                            .requestType(type)
                            .id(1L)
                            .build()
                    )
                    .collect(Collectors.toList())
            );

        try (var ignored = mockLomClientSearchOrders(lomOrder)) {
            GetOrderResponse response = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            GetOrderResponse expected = expectedResponse(true)
                .changeRequests(
                    Arrays.stream(ChangeRequestType.values())
                        .map(
                            type -> new ChangeOrderRequestDto()
                                .id(1L)
                                .status(ChangeRequestStatus.CREATED)
                                .reason(ChangeRequestReason.DELIVERY_DATE_UPDATED_BY_RECIPIENT)
                                .requestType(type)
                        )
                        .collect(Collectors.toList())
                );

            softly
                .assertThat(response)
                .usingRecursiveComparison()
                .ignoringFields("availableActions")
                .isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("Грузоместа без указанных размеров игнорируются")
    @DatabaseSetup("/controller/order/get/cancelled_order_event.xml")
    void noPlaceDimensions() throws Exception {
        OrderDto withNullDimensionsUnit = LomFactory.order(false)
            .setUnits(
                List.of(
                    unit(),
                    unit().toBuilder().dimensions(null).build()
                )
            );

        try (var ignored = mockLomClientSearchOrders(withNullDimensionsUnit)) {
            GetOrderResponse response = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly
                .assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse(false));
        }
    }

    @Test
    @DisplayName("В заявки на отмену не ставится статус SUCCESS, если в последней миле не было отмены")
    void cancellationSuccessNotSetUntilLastMileCancelled() throws Exception {
        try (var ignored = mockLomClientSearchOrders(LomFactory.orderLastMileNotCancelled())) {
            GetOrderResponse response = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly
                .assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponseLastMileNotCancelled());
        }
    }

    @Test
    @DisplayName("В товаре не проставлен КИЗ")
    @DatabaseSetup("/controller/order/get/cancelled_order_event.xml")
    void cisNotFoundInOrder() throws Exception {
        OrderDto order = LomFactory.order(false)
            .setItems(List.of(
                LomFactory.item(false)
                    .toBuilder()
                    .instances(List.of(Map.of("key", "val")))
                    .build()
            ));
        try (var ignored = mockLomClientSearchOrders(order)) {
            String lomOrderAsString = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .asString();
            IntegrationTestUtils.assertJson("controller/order/get/lom_get_order_no_cis.json", lomOrderAsString);
        }
    }

    @Test
    @DisplayName("Для заказа доступно обновление маркировок")
    void updateItemsInstancesAvailable() throws Exception {
        try (var ignored = mockLomClientSearchOrders(LomFactory.orderUpdateItemsInstancesAvailable())) {
            GetOrderResponse response = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly
                .assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(
                    expectedResponseLastMileNotCancelled()
                        .status(OrderStatus.CREATED)
                        .statusHistory(List.of(
                            orderStatusHistoryDto(OrderStatus.DRAFT, Instant.parse("2022-01-01T12:00:10Z")),
                            orderStatusHistoryDto(OrderStatus.VALIDATING, Instant.parse("2022-01-01T12:00:20Z")),
                            orderStatusHistoryDto(OrderStatus.CREATED, Instant.parse("2022-01-01T12:00:30Z"))
                        ))
                        .segments(expectedSegmentsUpdateItemsInstancesAvailable())
                        .availableActions(
                            new OrderAvailableActionsDto()
                                .updateItemsInstances(true)
                                .updateRecipient(true)
                                .updatePlaces(false)
                        )
                );
        }
    }

    @Test
    @DisplayName("Для заказа недоступно обновление получателя")
    void updateRecipientUnavailable() throws Exception {
        try (var ignored = mockLomClientSearchOrders(LomFactory.orderUpdateRecipientUnavailable())) {
            GetOrderResponse response = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly
                .assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(
                    expectedResponseLastMileNotCancelled()
                        .changeRequests(List.of(
                            new ChangeOrderRequestDto()
                                .requestType(ChangeRequestType.RECIPIENT)
                                .status(ChangeRequestStatus.CREATED)
                        ))
                        .availableActions(
                            new OrderAvailableActionsDto()
                                .updateItemsInstances(false)
                                .updateRecipient(false)
                                .updatePlaces(false)
                        )
                );
        }
    }

    @Test
    @DisplayName("Для заказа доступно обновление грузомест")
    void updatePlacesAvailable() throws Exception {
        OrderDto order = LomFactory.order(true)
            .setAvailableActions(
                OrderActionsDto.builder()
                    .updatePlaces(true)
                    .build()
            );

        try (var ignored = mockLomClientSearchOrders(order)) {
            GetOrderResponse response = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly
                .assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(
                    expectedResponse(true)
                        .availableActions(
                            new OrderAvailableActionsDto()
                                .updateItemsInstances(false)
                                .updatePlaces(true)
                                .updateRecipient(true)
                        )
                );
        }
    }

    @Test
    @DisplayName("Для заказа недоступно обновление грузомест")
    void updatePlacesUnavailable() throws Exception {
        OrderDto order = LomFactory.order(true)
            .setAvailableActions(
                OrderActionsDto.builder()
                    .updatePlaces(false)
                    .build()
            );

        try (var ignored = mockLomClientSearchOrders(order)) {
            GetOrderResponse response = apiClient.orders()
                .getOrder()
                .orderIdPath(13L)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly
                .assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(
                    expectedResponse(true)
                        .availableActions(
                            new OrderAvailableActionsDto()
                                .updateItemsInstances(false)
                                .updatePlaces(false)
                                .updateRecipient(true)
                        )
                );
        }
    }

    @Nonnull
    private GetOrderResponse expectedResponseWithReturnDropoff() {
        return expectedResponse(false)
            .returnInfo(new ReturnInfoDto().logisticPointId(3006L))
            .segments(expectedSegmentsWithReturn());
    }

    @Nonnull
    private GetOrderResponse expectedResponseLastMileNotCancelled() {
        return expectedResponse(false)
            .cancellationRequests(
                List.of(
                    new CancelOrderResponse()
                        .id(100L)
                        .status(CancellationRequestStatus.CREATED)
                        .reason(CancellationRequestReason.SHOP_CANCELLED)
                        .cancelledTimestamp(null),
                    new CancelOrderResponse()
                        .id(101L)
                        .status(CancellationRequestStatus.CREATED)
                        .reason(CancellationRequestReason.DELIVERY_SERVICE_LOST)
                        .cancelledTimestamp(null)
                )
            )
            .segments(expectedSegmentsLastMileNotCancelled());
    }

    @Nonnull
    private GetOrderResponse expectedResponse(boolean isOnlyRequired) {
        List<OrderStatusHistoryDto> statusHistory = new ArrayList<>();
        statusHistory.add(orderStatusHistoryDto(OrderStatus.DRAFT, Instant.parse("2022-01-01T12:00:10Z")));
        statusHistory.add(orderStatusHistoryDto(OrderStatus.VALIDATING, Instant.parse("2022-01-01T12:00:20Z")));
        statusHistory.add(orderStatusHistoryDto(OrderStatus.CREATED, Instant.parse("2022-01-01T12:00:30Z")));
        if (!isOnlyRequired) {
            statusHistory.add(
                orderStatusHistoryDto(OrderStatus.SORTING_CENTER_AT_START, Instant.parse("2022-02-01T01:11:03Z"))
            );
        }
        return new GetOrderResponse()
            .id(13L)
            .cost(
                new Cost()
                    .paymentMethod(PaymentMethod.PREPAID)
                    .assessedValue(BigDecimal.valueOf(999.90).setScale(2, RoundingMode.HALF_UP))
                    .deliveryForCustomer(BigDecimal.valueOf(249).setScale(2, RoundingMode.HALF_UP))
            )
            .deliveryInterval(
                isOnlyRequired ?
                    null :
                    new Interval()
                        .start(LocalTime.of(12, 13, 14))
                        .end(LocalTime.of(15, 16, 17))
            )
            .recipient(
                new ContactDto()
                    .name(
                        new PersonName()
                            .firstName("recipient.firstName")
                            .middleName(isOnlyRequired ? null : "recipient.middleName")
                            .lastName("recipient.lastName")
                    )
                    .phone(
                        new Phone()
                            .number("+7 999 888 7766")
                            .extension(isOnlyRequired ? null : "12345")
                    )
                    .email(isOnlyRequired ? null : "recipient@email.com")
            )
            .items(List.of(
                new Item()
                    .name("item[0].name")
                    .assessedValue(isOnlyRequired ? null : BigDecimal.valueOf(99.90).setScale(2, RoundingMode.HALF_UP))
                    .supplier(new ItemSupplier().inn(YANDEX_INN))
                    .externalId("item[0].externalId")
                    .price(BigDecimal.valueOf(999.90).setScale(2, RoundingMode.HALF_UP))
                    .tax(VatRate.VAT_20)
                    .count(1)
                    .cargoTypes(isOnlyRequired ? null : List.of(300, 301, 302))
                    .dimensions(
                        isOnlyRequired ?
                            null :
                            new Dimensions()
                                .height(30)
                                .length(40)
                                .width(50)
                                .weight(BigDecimal.valueOf(1.234).setScale(3, RoundingMode.HALF_UP))
                    )
                    .instances(isOnlyRequired ? null : List.of(new ItemInstanceDto().cis(OrderFactory.CIS_FULL)))
                    .placesExternalIds(isOnlyRequired ? null : List.of("place[0].externalId"))
            ))
            .cancellationRequests(
                isOnlyRequired ?
                    null :
                    List.of(
                        new CancelOrderResponse()
                            .id(100L)
                            .status(CancellationRequestStatus.CREATED)
                            .reason(CancellationRequestReason.SHOP_CANCELLED)
                            .cancelledTimestamp(null),
                        new CancelOrderResponse()
                            .id(101L)
                            .status(CancellationRequestStatus.SUCCESS)
                            .reason(CancellationRequestReason.DELIVERY_SERVICE_LOST)
                            .cancelledTimestamp(Instant.parse("2022-03-01T01:10:03Z"))
                    )
            )
            .changeRequests(
                isOnlyRequired ?
                    null :
                    List.of(
                        new ChangeOrderRequestDto()
                            .id(200L)
                            .status(ChangeRequestStatus.ERROR)
                            .reason(ChangeRequestReason.DELIVERY_DATE_UPDATED_BY_DELIVERY)
                            .requestType(ChangeRequestType.DELIVERY_DATE)
                    )
            )
            .status(isOnlyRequired ? OrderStatus.CREATED : OrderStatus.SORTING_CENTER_AT_START)
            .statusHistory(statusHistory)
            .deliveryDateMin(isOnlyRequired ? null : LocalDate.of(2022, 2, 3))
            .deliveryDateMax(isOnlyRequired ? null : LocalDate.of(2022, 2, 4))
            .places(
                isOnlyRequired ?
                    null :
                    List.of(
                        new Place()
                            .dimensions(
                                new Dimensions()
                                    .height(30)
                                    .length(40)
                                    .width(50)
                                    .weight(BigDecimal.valueOf(1.234).setScale(3, RoundingMode.HALF_UP))
                            )
                            .externalId("place[0].externalId")
                    )
            )
            .deliveryServiceId(isOnlyRequired ? null : 2001L)
            .segments(isOnlyRequired ? null : expectedSegments())
            .returnInfo(isOnlyRequired ? null : new ReturnInfoDto().logisticPointId(3001L))
            .availableActions(
                new OrderAvailableActionsDto()
                    .updateItemsInstances(false)
                    .updateRecipient(true)
                    .updatePlaces(false)
            );
    }

    private OrderStatusHistoryDto orderStatusHistoryDto(OrderStatus orderStatus, Instant datetime) {
        return new OrderStatusHistoryDto().status(orderStatus).datetime(datetime);
    }

    @Nonnull
    private List<WaybillSegmentDto> expectedSegmentsWithReturn() {
        List<WaybillSegmentDto> segments = new ArrayList<>(expectedSegments());
        segments.add(
            new WaybillSegmentDto()
                .id(1005L)
                .partnerId(2005L)
                .partnerName("return-sc-partner-name")
                .partnerLegalName("return-sc-partner-legal-name")
                .partnerAddress("return-sc-partner-address")
                .partnerType(PartnerType.SORTING_CENTER)
                .segmentType(WaybillSegmentType.SORTING_CENTER)
                .logisticsPointId(3005L)
                .waybillSegmentStatusHistory(List.of())
        );
        segments.add(
            new WaybillSegmentDto()
                .id(1006L)
                .partnerId(2001L)
                .partnerName("return-dropoff-partner-name")
                .partnerLegalName("return-dropoff-partner-legal-name")
                .partnerAddress("return-dropoff-partner-address")
                .partnerType(PartnerType.DELIVERY)
                .segmentType(WaybillSegmentType.SORTING_CENTER)
                .logisticsPointId(3006L)
                .waybillSegmentStatusHistory(List.of())
        );
        return segments;
    }

    @Nonnull
    private List<WaybillSegmentDto> expectedSegments() {
        return List.of(
            yandexGoShopSegment(),
            dropoffSegment(),
            scSegment(),
            movementSegment(),
            pickupSegment()
        );
    }

    @Nonnull
    private List<WaybillSegmentDto> expectedSegmentsLastMileNotCancelled() {
        return List.of(
            yandexGoShopSegment(),
            dropoffSegment(),
            scSegment(),
            movementSegment(),
            pickupSegment()
                .waybillSegmentStatusHistory(
                    List.of(
                        new WaybillSegmentStatusHistoryDto()
                            .id(10000L)
                            .created(Instant.parse("2022-02-01T01:11:03Z"))
                            .date(Instant.parse("2022-02-01T01:10:03Z"))
                            .status(WaybillSegmentStatus.IN)
                            .trackerStatus("wssh-tracker-status")
                    )
                )
        );
    }

    @Nonnull
    private List<WaybillSegmentDto> expectedSegmentsUpdateItemsInstancesAvailable() {
        return List.of(
            yandexGoShopSegment(),
            dropoffSegment().waybillSegmentStatusHistory(List.of()),
            scSegment(),
            movementSegment(),
            pickupSegment().waybillSegmentStatusHistory(List.of())
        );
    }

    @Nonnull
    private WaybillSegmentDto yandexGoShopSegment() {
        return new WaybillSegmentDto()
            .id(1000L)
            .partnerId(2000L)
            .partnerName("go-shop-partner-name")
            .partnerLegalName("go-shop-partner-legal-name")
            .partnerAddress("go-shop-partner-address")
            .partnerType(PartnerType.YANDEX_GO_SHOP)
            .segmentType(WaybillSegmentType.NO_OPERATION)
            .waybillSegmentStatusHistory(List.of());
    }

    @Nonnull
    private WaybillSegmentDto dropoffSegment() {
        return new WaybillSegmentDto()
            .id(1001L)
            .partnerId(2001L)
            .partnerName("dropoff-partner-name")
            .partnerLegalName("dropoff-partner-legal-name")
            .partnerAddress("dropoff-partner-address")
            .partnerType(PartnerType.DELIVERY)
            .segmentType(WaybillSegmentType.SORTING_CENTER)
            .logisticsPointId(3001L)
            .segmentStatus(WaybillSegmentStatus.OUT)
            .waybillSegmentStatusHistory(List.of(
                new WaybillSegmentStatusHistoryDto()
                    .id(10000L)
                    .created(Instant.parse("2022-02-01T01:11:03Z"))
                    .date(Instant.parse("2022-02-01T01:10:03Z"))
                    .status(WaybillSegmentStatus.IN)
                    .trackerStatus("wssh-tracker-status")
            ));
    }

    @Nonnull
    private WaybillSegmentDto scSegment() {
        return new WaybillSegmentDto()
            .id(1002L)
            .partnerId(2002L)
            .partnerName("sc-partner-name")
            .partnerLegalName("sc-partner-legal-name")
            .partnerAddress("sc-partner-address")
            .partnerType(PartnerType.SORTING_CENTER)
            .segmentType(WaybillSegmentType.SORTING_CENTER)
            .logisticsPointId(3002L)
            .waybillSegmentStatusHistory(List.of());
    }

    @Nonnull
    private WaybillSegmentDto movementSegment() {
        return new WaybillSegmentDto()
            .id(1003L)
            .partnerId(2003L)
            .partnerName("market-courier-partner-name")
            .partnerLegalName("market-courier-partner-legal-name")
            .partnerAddress("market-courier-partner-address")
            .partnerType(PartnerType.DELIVERY)
            .segmentType(WaybillSegmentType.MOVEMENT)
            .waybillSegmentStatusHistory(List.of());
    }

    @Nonnull
    private WaybillSegmentDto pickupSegment() {
        return new WaybillSegmentDto()
            .id(1004L)
            .partnerId(2001L)
            .partnerName("dropoff-partner-name")
            .partnerLegalName("dropoff-partner-legal-name")
            .partnerAddress("dropoff-partner-address")
            .partnerType(PartnerType.DELIVERY)
            .segmentType(WaybillSegmentType.PICKUP)
            .waybillSegmentStatusHistory(
                List.of(
                    new WaybillSegmentStatusHistoryDto()
                        .id(10000L)
                        .created(Instant.parse("2022-02-01T01:11:03Z"))
                        .date(Instant.parse("2022-02-01T01:10:03Z"))
                        .status(WaybillSegmentStatus.IN)
                        .trackerStatus("wssh-tracker-status"),
                    new WaybillSegmentStatusHistoryDto()
                        .id(10001L)
                        .created(Instant.parse("2022-02-01T01:11:03Z"))
                        .date(Instant.parse("2022-03-01T01:10:03Z"))
                        .status(WaybillSegmentStatus.CANCELLED)
                        .trackerStatus("wssh-tracker-status")
                )
            );
    }

    @Nonnull
    private AutoCloseable mockLomClientSearchOrders(@Nullable OrderDto response) {
        List<OrderDto> orders = Stream.ofNullable(response).filter(Objects::nonNull).collect(Collectors.toList());
        mockSearchLomOrder(13L, orders);
        return () -> verifySearchLomOrder(13L);
    }
}
