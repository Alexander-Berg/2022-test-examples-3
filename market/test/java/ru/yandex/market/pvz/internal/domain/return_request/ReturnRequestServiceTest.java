package ru.yandex.market.pvz.internal.domain.return_request;

import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.returns.ReturnableItemsResponse;
import ru.yandex.market.checkout.checkouter.viewmodel.NonReturnableItemViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.NonReturnableReasonType;
import ru.yandex.market.checkout.checkouter.viewmodel.ReturnableItemViewModel;
import ru.yandex.market.pvz.client.logistics.dto.ReturnErrorDto;
import ru.yandex.market.pvz.client.logistics.dto.ReturnRequestResponseDto;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderItem;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.returns.ReturnRequestFilterData;
import ru.yandex.market.pvz.core.domain.returns.ReturnRequestRepository;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnClientType;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequest;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnStatus;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.pvz.core.test.factory.mapper.ReturnRequestTestParamsMapper;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.return_request.dto.action.ReturnActionDto;
import ru.yandex.market.pvz.internal.controller.return_request.dto.action.ReturnActionType;
import ru.yandex.market.pvz.internal.domain.return_request.mapper.ReturnRequestDtoMapper;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.PARTIAL_RETURN_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.SEND_RECEIVE_TO_OW_ENABLED;
import static ru.yandex.market.pvz.core.domain.returns.model.ReturnRequest.DEFAULT_EXPIRATION_DAYS;
import static ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory.BARCODE_PREFIX;


@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReturnRequestServiceTest {

    private final DbQueueTestUtil dbQueueTestUtil;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final TestReturnRequestFactory returnRequestFactory;
    private final TestPickupPointFactory pickupPointFactory;

    private final TestOrderFactory orderFactory;
    private final OrderRepository orderRepository;

    private final ReturnRequestService returnRequestService;
    private final ReturnRequestRepository returnRequestRepository;

    private final ReturnRequestTestParamsMapper requestTestParamsMapper;
    private final ReturnRequestDtoMapper returnRequestDtoMapper;

    @MockBean
    private CheckouterClient checkouterClient;

    @MockBean
    private CheckouterReturnClient checkouterReturnClient;

    @BeforeEach
    void setup() {
        configurationGlobalCommandService.setValue(SEND_RECEIVE_TO_OW_ENABLED, true);
        configurationGlobalCommandService.setValue(PARTIAL_RETURN_ENABLED, true);
    }

    @Test
    public void partialReturnDisabled() {
        configurationGlobalCommandService.setValue(PARTIAL_RETURN_ENABLED, false);
        var pickupPoint =
                pickupPointFactory.createPickupPoint(
                        TestPickupPointFactory.CreatePickupPointBuilder.builder()
                                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                        .partialReturnAllowed(true).build()).build()
                );
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .params(TestOrderFactory.OrderParams.builder()
                                .paymentStatus(OrderPaymentStatus.PAID)
                                .isDropShip(false)
                                .fbs(false)
                                .externalId("1234567890").build())
                        .pickupPoint(pickupPoint)
                        .build()
        );
        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);

        var returnableItems = returnRequestService.getReturnableItems(order.getId(),
                new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), null, null, 3, 7));

        assertThat(returnableItems.isPartialReturnAllowed()).isFalse();
    }

    @Test
    public void partialReturnNotAllowedDueToOrderType() {
        var pickupPoint =
                pickupPointFactory.createPickupPoint(
                        TestPickupPointFactory.CreatePickupPointBuilder.builder()
                                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                        .partialReturnAllowed(true).build()).build()
                );
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .params(TestOrderFactory.OrderParams.builder()
                                .isClickAndCollect(true)
                                .externalId("1234567890").build())
                        .pickupPoint(pickupPoint)
                        .build()
        );
        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);

        var returnableItems = returnRequestService.getReturnableItems(order.getId(),
                new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), null, null, 3, 7));

        assertThat(returnableItems.isPartialReturnAllowed()).isFalse();
    }

    @Test
    public void partialReturnDisabledOnPvz() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .params(TestOrderFactory.OrderParams.builder()
                                .externalId("1234567890").build())
                        .pickupPoint(pickupPoint)
                        .build()
        );
        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);

        var returnableItems = returnRequestService.getReturnableItems(order.getId(),
                new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), null, null, 3, 7));

        assertThat(returnableItems.isPartialReturnAllowed()).isFalse();
    }

    @Test
    public void returnRequestMappingTest() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var returnRequest = returnRequestFactory.createReturnRequest(
                TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        var pickupPointRequestData = new PickupPointRequestData(pickupPoint.getId(), null, "", 1L, 3, 7);
        var returnRequestFilterData = new ReturnRequestFilterData(null, null, ReturnClientType.CLIENT, null, null);
        var returnRequestBy = returnRequestService.getReturnRequestBy(
                returnRequestFilterData, pickupPointRequestData, Pageable.unpaged()
        );
        assertThat(returnRequestBy).isNotEmpty();

        var returnDto = StreamEx.of(returnRequestBy.get()).toList().get(0);
        assertThat(returnDto.getClientType()).isEqualTo(returnRequest.getClientType());
        assertThat(returnDto.getBuyerName()).isEqualTo(returnRequest.getBuyerName());
        assertThat(returnDto.getExpirationDate()).isEqualTo(returnRequest.getExpirationDate());
        assertThat(returnDto.getActions()).contains(new ReturnActionDto(ReturnActionType.DOWNLOAD_APP));
    }

    @Test
    void getReturnByIdDtoCheck() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var returnRequest = returnRequestFactory.createReturnRequest(
                TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );

        var pickupPointData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), "Катарсис", 123L, 3, 7);
        var returnDto = returnRequestService.getReturnRequestById(returnRequest.getReturnId(), pickupPointData);
        assertThat(returnDto).isNotNull();
        assertThat(returnDto).isEqualTo(returnRequestDtoMapper.mapWithItems(returnRequest));
        assertThat(returnDto.getItems()).isNotEmpty();
        assertThat(returnDto.getItems().get(0).getName()).isEqualTo(returnRequest.getItems().get(0).getName());
    }

    @ParameterizedTest()
    @EnumSource(value = PvzOrderStatus.class, names = {"TRANSMITTED_TO_RECIPIENT", "TRANSPORTATION_RECIPIENT"})
    void createReturnForOrderFromPvzToCommitDeliverCheckpoint(PvzOrderStatus status) {
        var returnRequest = createRequest(true, status);
        var returnRequestCreateDto = returnRequestDtoMapper.mapToDto(returnRequest);
        var actualReturnRequest = returnRequestService.createReturn(returnRequestCreateDto);
        var expectedDto = returnRequestDtoMapper.mapWithItems(returnRequestDtoMapper.map(returnRequest));
        expectedDto.setStatus(ru.yandex.market.pvz.client.logistics.model.ReturnStatus.NEW);
        expectedDto.setExpirationDate(expectedDto.getRequestDate().plusDays(DEFAULT_EXPIRATION_DAYS));
        expectedDto.getItems().get(0).setId(actualReturnRequest.getReturnRequest().getItems().get(0).getId());
        var expectedResponse = ReturnRequestResponseDto.builder().returnRequest(expectedDto).build();
        assertThat(actualReturnRequest).isEqualTo(expectedResponse);

        var order = orderRepository.findByExternalIdAndPickupPointIdOrThrow(returnRequest.getExternalOrderId(),
                returnRequest.getPickupPointId());
        assertThat(order.getStatus()).isEqualTo(PvzOrderStatus.DELIVERED_TO_RECIPIENT);
    }

    @ParameterizedTest()
    @EnumSource(value = PvzOrderStatus.class, mode = EnumSource.Mode.EXCLUDE,
            names = {"TRANSMITTED_TO_RECIPIENT", "TRANSPORTATION_RECIPIENT"})
    void createReturnForOrderFromPvzOtherCheckpoint(PvzOrderStatus status) {
        var returnRequest = createRequest(true, status);
        var returnRequestCreateDto = returnRequestDtoMapper.mapToDto(returnRequest);
        var actualReturnRequest = returnRequestService.createReturn(returnRequestCreateDto);
        var expectedDto = returnRequestDtoMapper.mapWithItems(returnRequestDtoMapper.map(returnRequest));
        expectedDto.setStatus(ru.yandex.market.pvz.client.logistics.model.ReturnStatus.NEW);
        expectedDto.setExpirationDate(expectedDto.getRequestDate().plusDays(DEFAULT_EXPIRATION_DAYS));
        expectedDto.getItems().get(0).setId(actualReturnRequest.getReturnRequest().getItems().get(0).getId());
        var expectedResponse = ReturnRequestResponseDto.builder().returnRequest(expectedDto).build();
        assertThat(actualReturnRequest).isEqualTo(expectedResponse);

        var order = orderRepository.findByExternalIdAndPickupPointIdOrThrow(returnRequest.getExternalOrderId(),
                returnRequest.getPickupPointId());
        assertThat(order.getStatus()).isEqualTo(status);
    }

    @Test
    void createReturnForRandomOrder() {
        var returnRequest = createRequest();
        var returnRequestCreateDto = returnRequestDtoMapper.mapToDto(returnRequest);
        var actualReturnRequest = returnRequestService.createReturn(returnRequestCreateDto);
        var expectedDto = returnRequestDtoMapper.mapWithItems(returnRequestDtoMapper.map(returnRequest));
        expectedDto.setStatus(ru.yandex.market.pvz.client.logistics.model.ReturnStatus.NEW);
        expectedDto.setExpirationDate(expectedDto.getRequestDate().plusDays(DEFAULT_EXPIRATION_DAYS));
        expectedDto.getItems().get(0).setId(actualReturnRequest.getReturnRequest().getItems().get(0).getId());
        var expectedResponse = ReturnRequestResponseDto.builder().returnRequest(expectedDto).build();
        assertThat(actualReturnRequest).isEqualTo(expectedResponse);
    }

    @Test
    void cancelReturn() {
        var returnRequest = returnRequestFactory.createReturnRequest();
        var returnRequestResponseDto =
                returnRequestService.cancelReturn(returnRequest.getReturnId());
        var updatedReturnRequest = returnRequestRepository.findByReturnId(returnRequest.getReturnId());

        assertThat(updatedReturnRequest.get().getStatus()).isEqualTo(ReturnStatus.CANCELLED);
        assertThat(returnRequestResponseDto.getReturnRequest().getStatus())
                .isEqualTo(ru.yandex.market.pvz.client.logistics.model.ReturnStatus.CANCELLED);
    }

    @Test
    void cancelAlreadyReceivedReturn() {
        var returnRequest = returnRequestFactory.createReturnRequest();
        returnRequest = returnRequestFactory.receiveReturnRequest(returnRequest.getReturnId());
        var returnRequestResponseDto =
                returnRequestService.cancelReturn(returnRequest.getReturnId());
        var updatedReturnRequest = returnRequestRepository.findByReturnId(returnRequest.getReturnId());

        assertThat(updatedReturnRequest.get().getStatus()).isEqualTo(ReturnStatus.RECEIVED);
        assertThat(returnRequestResponseDto.getErrorCodes()).contains(ReturnErrorDto.RETURN_IN_RECEIVED_STATUS);
        assertThat(returnRequestResponseDto.getReturnRequest().getStatus())
                .isEqualTo(ru.yandex.market.pvz.client.logistics.model.ReturnStatus.RECEIVED);
    }

    @Test
    void cancelReturnWithNoReturnFoundError() {
        assertThat(returnRequestService.cancelReturn("fake-123456").getErrorCodes())
                .contains(ReturnErrorDto.NO_RETURN_FOUND);
    }

    public ReturnRequest createRequest() {
        return createRequest(false, null);
    }

    public ReturnRequest createRequest(boolean withOrder, PvzOrderStatus status) {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var params = TestReturnRequestFactory.ReturnRequestTestParams.builder().build();
        if (withOrder) {
            params.setExternalOrderId(createOrderForRequest(pickupPoint, status).getExternalId());
        }
        params.setBarcode(BARCODE_PREFIX + params.getReturnId());
        var requestReturn = requestTestParamsMapper.mapToEntity(params);
        requestReturn.setPickupPoint(pickupPoint);
        requestReturn.setPickupPointId(pickupPoint.getId());
        return requestReturn;
    }

    private Order createOrderForRequest(PickupPoint pickupPoint, PvzOrderStatus status) {
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build());
        orderFactory.setStatusAndCheckpoint(order.getId(), status);
        return order;
    }

    @Test
    void receiveReturn() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var returnRequest = returnRequestFactory.createReturnRequest(
                TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        var pickupPointData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), "Катарсис", 123L, 3, 7);

        returnRequestService.receiveReturn(returnRequest.getReturnId(), pickupPointData);
        returnRequestService.receiveReturn(returnRequest.getReturnId(), pickupPointData);

        var returnRequestQueue = dbQueueTestUtil.getQueue(PvzQueueType.RETURN_REQUEST);
        assertThat(returnRequestQueue).hasSize(1);
        dbQueueTestUtil.executeSingleQueueItem(PvzQueueType.RETURN_REQUEST);
        assertThat(returnRequestQueue.get(0)).isEqualTo(returnRequest.getReturnId());

        var sendReceiveToOwQueue = dbQueueTestUtil.getQueue(PvzQueueType.SEND_RECEIVE_TO_OW);
        assertThat(sendReceiveToOwQueue).hasSize(1);
        dbQueueTestUtil.executeSingleQueueItem(PvzQueueType.SEND_RECEIVE_TO_OW);
        assertThat(sendReceiveToOwQueue.get(0)).isEqualTo(returnRequest.getReturnId());
    }

    @Test
    void tryReceiveCancelReturn() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var returnRequest = returnRequestFactory.createReturnRequest(
                TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        var pickupPointData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), "Катарсис", 123L, 3, 7);
        returnRequestService.cancelReturn(returnRequest.getReturnId());

        assertThatThrownBy(() -> returnRequestService.receiveReturn(returnRequest.getReturnId(), pickupPointData))
                .hasMessageContaining("Невозможно принять возврат");
    }

    @Test
    void getReturnableItemsWhereOrderIsUnpaid() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .params(
                                TestOrderFactory.OrderParams.builder()
                                        .externalId("1234567890")
                                        .isDropShip(false)
                                        .fbs(false)
                                        .paymentStatus(OrderPaymentStatus.UNPAID)
                                        .build())
                        .pickupPoint(pickupPoint)
                        .build()
        );
        var response = createResponse(order, Collections.emptyList());
        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);

        when(checkouterReturnClient.getReturnableItems(anyLong(), any(ClientRole.class), anyLong()))
                .thenReturn(response);

        var returnableItems = returnRequestService.getReturnableItems(order.getId(),
                new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), null, null, 3, 7));

        assertThat(returnableItems.isPartialReturnAllowed()).isFalse();
    }

    @Test
    void getReturnableItemsAndNonReturnableItems() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .partialReturnAllowed(true)
                        .build());
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .params(TestOrderFactory.OrderParams.builder()
                                .paymentStatus(OrderPaymentStatus.PAID)
                                .isDropShip(false)
                                .fbs(false)
                                .externalId("1234567890").build())
                        .pickupPoint(pickupPoint)
                        .build()
        );
        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);
        var nonReturnableItem = createNonReturnableItem(order, NonReturnableReasonType.NON_RETURNABLE_DIGITAL_ITEM);

        var response = createResponse(order, List.of(nonReturnableItem));
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setNoAuth(false);
        checkouterOrder.setBuyer(new Buyer(99L));
        when(checkouterClient.getOrder(any(), any())).thenReturn(checkouterOrder);
        when(checkouterReturnClient.getReturnableItems(Long.parseLong(order.getExternalId()), ClientRole.USER, 99L))
                .thenReturn(response);

        var returnableItems = returnRequestService.getReturnableItems(order.getId(),
                new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), null, null, 3, 7));

        assertThat(returnableItems.isPartialReturnAllowed()).isTrue();
        assertThat(returnableItems.getReturnableItems()).size().isEqualTo(2);
        assertThat(returnableItems.getNonReturnableItems()).size().isEqualTo(1);
    }

    @Test
    void getReturnableItems() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .partialReturnAllowed(true)
                        .build());
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .params(TestOrderFactory.OrderParams.builder()
                                .paymentStatus(OrderPaymentStatus.PAID)
                                .isDropShip(false)
                                .fbs(false)
                                .externalId("1234567890").build())
                        .pickupPoint(pickupPoint)
                        .build()
        );
        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);
        var response = createResponse(order, Collections.emptyList());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setNoAuth(false);
        checkouterOrder.setBuyer(new Buyer(99L));
        when(checkouterClient.getOrder(any(), any())).thenReturn(checkouterOrder);
        when(checkouterReturnClient.getReturnableItems(Long.parseLong(order.getExternalId()), ClientRole.USER, 99L))
                .thenReturn(response);

        var returnableItems = returnRequestService.getReturnableItems(order.getId(),
                new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), null, null, 3, 7));

        assertThat(returnableItems.getReturnableItems()).size().isEqualTo(2);
        assertThat(returnableItems.getNonReturnableItems()).isEmpty();
        assertThat(returnableItems.isPartialReturnAllowed()).isTrue();
        assertThat(returnableItems.getReturnableItems().get(0))
                .hasFieldOrPropertyWithValue("name", order.getItems().get(0).getName());
        assertThat(returnableItems.getReturnableItems().get(0))
                .hasFieldOrPropertyWithValue("count", (long) order.getItems().get(0).getCount());
    }

    @Test
    void whenOrderIsKgtThenPartialReturnIsNotAllowed() {
        var pickupPoint =
                pickupPointFactory.createPickupPoint(
                        TestPickupPointFactory.CreatePickupPointBuilder.builder()
                                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                        .partialReturnAllowed(true).build()).build()
                );
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .params(TestOrderFactory.OrderParams.builder()
                                .paymentStatus(OrderPaymentStatus.PAID)
                                .isDropShip(false)
                                .fbs(false)
                                .isKgt(true)
                                .externalId("1234567890").build())
                        .pickupPoint(pickupPoint)
                        .build()
        );
        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);
        var response = createResponse(order, Collections.emptyList());
        when(checkouterReturnClient.getReturnableItems(anyLong(), any(ClientRole.class), anyLong()))
                .thenReturn(response);

        var returnableItems = returnRequestService.getReturnableItems(order.getId(),
                new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), null, null, 3, 7));

        assertThat(returnableItems.isPartialReturnAllowed()).isFalse();
    }

    private ReturnableItemsResponse createResponse(Order order, List<NonReturnableItemViewModel> itemViewModels) {
        var returnableItems = StreamEx.of(order.getItems()).map(this::createItem).toList();
        return new ReturnableItemsResponse(Collections.emptySet(), returnableItems, itemViewModels);
    }

    private NonReturnableItemViewModel createNonReturnableItem(Order order, NonReturnableReasonType type) {
        var items = StreamEx.of(order.getItems()).map(this::createItem).toList();
        var nonReturnableItem = new NonReturnableItemViewModel();
        nonReturnableItem.setItem(items.get(0));
        nonReturnableItem.setNonReturnableReason(type);
        return nonReturnableItem;
    }

    private ReturnableItemViewModel createItem(OrderItem orderItem) {
        var returnableItem = new ReturnableItemViewModel();
        returnableItem.setItemId(orderItem.getId());
        returnableItem.setItemTitle(orderItem.getName());
        returnableItem.setCount(orderItem.getCount());
        returnableItem.setBuyerPrice(orderItem.getPrice());
        return returnableItem;
    }

}
