package ru.yandex.market.logistic.gateway.client.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.common.util.collections.Quadruple;
import ru.yandex.common.util.collections.Triple;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayValidationException;
import ru.yandex.market.logistic.gateway.client.service.delivery.DataExchangeService;
import ru.yandex.market.logistic.gateway.client.service.delivery.MovementService;
import ru.yandex.market.logistic.gateway.client.service.delivery.OrderService;
import ru.yandex.market.logistic.gateway.common.model.common.Courier;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.request.CancelMovementRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.AttachedDocsData;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.ExternalOrderStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.delivery.ExternalResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.ItemInstances;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location.LocationBuilder;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderDeliveryDate;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderItems;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderParcelId;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.delivery.Recipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.SelfExport;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.Transaction;
import ru.yandex.market.logistic.gateway.common.model.delivery.UnitId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.CreateOrderRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetAttachedDocsRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetCourierRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetExternalOrderHistoryRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetExternalOrdersStatusRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetLabelsRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetOrderHistoryRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetOrdersDeliveryDateRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetOrdersStatusRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetReferenceTimetableCouriersRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetReferenceWarehousesRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetTransactionsOrdersRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.PutReferenceWarehousesRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.UpdateItemsInstancesRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.UpdateOrderDeliveryDateRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.UpdateOrderItemsRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.UpdateOrderRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.UpdateOrderTransferCodesRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.UpdateRecipientRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetCourierResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetExternalOrderHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetExternalOrdersStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetOrdersDeliveryDateResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetOrdersStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetReferenceWarehousesResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetTransactionsOrdersResponse;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistic.gateway.utils.CommonDtoFactory;
import ru.yandex.market.logistic.gateway.utils.DeliveryDtoFactory;
import ru.yandex.market.logistic.gateway.utils.FulfillmentDtoFactory;
import ru.yandex.market.logistics.util.client.HttpTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.refEq;

@RunWith(MockitoJUnitRunner.class)
public class DeliveryServiceTest {

    static {
        Locale.setDefault(Locale.ENGLISH);
    }

    private static final String PROCESS_ID = "processIdABC123";

    private static final ClientRequestMeta CLIENT_REQUEST_META = new ClientRequestMeta(PROCESS_ID);

    @Mock
    private SqsSenderService sqsSenderService;

    @Mock
    private HttpTemplate httpTemplate;

    @InjectMocks
    private OrderService orderService;

    @InjectMocks
    private DataExchangeService dataExchangeService;

    @InjectMocks
    private MovementService movementService;

    private SoftAssertions assertions = new SoftAssertions();

    @After
    public void tearDown() {
        assertions.assertAll();
    }

    @Test
    public void testCreateOrderSuccessSend() throws GatewayApiException {
        Order order = DeliveryDtoFactory.createOrder();
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.createOrder(order, partner, null, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
        Mockito.verify(sqsSenderService).sendClientTask(eq(RequestFlow.DS_CREATE_ORDER),
            refEq(new CreateOrderRequest(order, partner, null)), eq(CLIENT_REQUEST_META), eq(TaskResultConsumer.MDB));
    }

    @Test
    public void testCreateOrderWithRestrictedDataSuccessSend() throws GatewayApiException {
        Order order = DeliveryDtoFactory.createOrder();
        Partner partner = DeliveryDtoFactory.createPartner();
        final CreateOrderRestrictedData restrictedData = DeliveryDtoFactory.createOrderRestrictedData();

        orderService.createOrder(order, partner, restrictedData, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
        Mockito.verify(sqsSenderService).sendClientTask(
            eq(RequestFlow.DS_CREATE_ORDER),
            refEq(new CreateOrderRequest(order, partner, restrictedData)),
            eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB)
        );
    }

    @Test
    public void testCreateOrderWithEmptyRestrictedDataSuccessSend() throws GatewayApiException {
        Order order = DeliveryDtoFactory.createOrder();
        Partner partner = DeliveryDtoFactory.createPartner();
        final CreateOrderRestrictedData restrictedData = DeliveryDtoFactory.createOrderEmptyRestrictedData();

        orderService.createOrder(order, partner, restrictedData, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
        Mockito.verify(sqsSenderService).sendClientTask(
            eq(RequestFlow.DS_CREATE_ORDER),
            refEq(new CreateOrderRequest(order, partner, restrictedData)),
            eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB)
        );
    }

    @Test
    public void testCreateOrderWithoutProcessIdSuccessSend() throws GatewayApiException {
        Order order = DeliveryDtoFactory.createOrder();
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.createOrder(order, partner, null, null, TaskResultConsumer.MDB);
        Mockito.verify(sqsSenderService).sendClientTask(eq(RequestFlow.DS_CREATE_ORDER),
            refEq(new CreateOrderRequest(order, partner, null)), isNull(), eq(TaskResultConsumer.MDB));
    }

    @Test(expected = GatewayValidationException.class)
    public void testCreateOrderFailedValidation() throws GatewayApiException {
        Order order = DeliveryDtoFactory.createInvalidOrder();
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.createOrder(order, partner, null, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
    }

    @Test
    public void testCreateOrderWithRestrictedYandexGoDataFailedValidation() {
        DeliveryDtoFactory.createInvalidRestrictedYandexGoDataCollection()
            .forEach(this::doTestCreateOrderWithRestrictedDataFailedValidation);
    }

    @SneakyThrows
    private void doTestCreateOrderWithRestrictedDataFailedValidation(
        Triple<CreateOrderRestrictedData, String, String> dataWithExceptionMessage
    ) {
        Order order = DeliveryDtoFactory.createOrder();
        Partner partner = DeliveryDtoFactory.createPartner();

        assertThatThrownBy(() -> orderService.createOrder(order, partner, dataWithExceptionMessage.first, CLIENT_REQUEST_META, TaskResultConsumer.MDB))
            .isInstanceOfAny(GatewayApiException.class, GatewayValidationException.class)
            .hasMessageContaining("propertyPath=" + dataWithExceptionMessage.second)
            .hasMessageContaining("interpolatedMessage='" + dataWithExceptionMessage.third);
    }

    @Test
    public void testUpdateOrderSuccessSend() throws GatewayApiException {
        Order order = DeliveryDtoFactory.createOrder();
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.updateOrder(order, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
        Mockito.verify(sqsSenderService).sendClientTask(eq(RequestFlow.DS_UPDATE_ORDER),
            refEq(new UpdateOrderRequest(order, partner)), eq(CLIENT_REQUEST_META), eq(TaskResultConsumer.MDB));
    }

    @Test(expected = GatewayValidationException.class)
    public void testUpdateOrderFailedValidation() throws GatewayApiException {
        Order order = DeliveryDtoFactory.createInvalidOrder();
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.updateOrder(order, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
    }

    @Test
    public void testGetLabelsSuccessSend() throws GatewayApiException {
        List<OrderParcelId> ordersId = Collections.singletonList(DeliveryDtoFactory.createOrderParcelId());
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.getLabels(ordersId, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
        Mockito.verify(sqsSenderService).sendClientTask(eq(RequestFlow.DS_GET_LABELS),
            refEq(new GetLabelsRequest(ordersId, partner)), eq(CLIENT_REQUEST_META), eq(TaskResultConsumer.MDB));
    }

    @Test(expected = GatewayValidationException.class)
    public void testGetLabelsFailedValidation() throws GatewayApiException {
        List<OrderParcelId> ordersId = null;
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.getLabels(ordersId, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
    }

    @Test
    public void testUpdateRecipientSuccessSendWithNullParcelId() throws GatewayApiException {
        ResourceId orderId = DeliveryDtoFactory.createResourceId();
        Recipient recipient = DeliveryDtoFactory.createRecipient();
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.updateRecipient(
            orderId,
            null,
            recipient,
            null,
            partner,
            111L,
            CLIENT_REQUEST_META,
            TaskResultConsumer.MDB
        );
        Mockito.verify(sqsSenderService).sendClientTask(eq(RequestFlow.DS_UPDATE_RECIPIENT),
            refEq(new UpdateRecipientRequest(orderId, null, recipient, partner, null, 111L)),
            eq(CLIENT_REQUEST_META), eq(TaskResultConsumer.MDB));
    }

    @Test
    public void testUpdateRecipientSuccessSendWithNullOrderId() throws GatewayApiException {
        ResourceId parcelId = DeliveryDtoFactory.createResourceId();
        Recipient recipient = DeliveryDtoFactory.createRecipient();
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.updateRecipient(
            null,
            parcelId,
            recipient,
            null,
            partner,
            111L,
            CLIENT_REQUEST_META,
            TaskResultConsumer.MDB
        );
        Mockito.verify(sqsSenderService).sendClientTask(eq(RequestFlow.DS_UPDATE_RECIPIENT),
            refEq(new UpdateRecipientRequest(null, parcelId, recipient, partner, null, 111L)), eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB));
    }

    @Test(expected = GatewayValidationException.class)
    public void testUpdateRecipientFailedValidationWithNullParcelId() throws GatewayApiException {
        ResourceId orderId = DeliveryDtoFactory.createResourceId();
        ResourceId parcelId = null;
        Recipient recipient = null;
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.updateRecipient(
            orderId,
            parcelId,
            recipient,
            null,
            partner,
            111L,
            CLIENT_REQUEST_META,
            TaskResultConsumer.MDB
        );
    }

    @Test(expected = GatewayValidationException.class)
    public void testUpdateRecipientFailedValidationWithNullOrderId() throws GatewayApiException {
        ResourceId orderId = null;
        ResourceId parcelId = DeliveryDtoFactory.createResourceId();
        Recipient recipient = null;
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.updateRecipient(
            orderId,
            parcelId,
            recipient,
            null,
            partner,
            111L,
            CLIENT_REQUEST_META,
            TaskResultConsumer.MDB
        );
    }

    @Test
    public void testUpdateOrderItemsValidationSuccess() throws GatewayApiException {
        OrderItems orderItems = DeliveryDtoFactory.createOrderItemsBuilder().build();
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.updateOrderItems(
            orderItems,
            partner,
            CLIENT_REQUEST_META,
            TaskResultConsumer.LOM
        );
        Mockito.verify(sqsSenderService).sendClientTask(eq(RequestFlow.DS_UPDATE_ORDER_ITEMS),
            refEq(new UpdateOrderItemsRequest(orderItems, partner)),
            eq(CLIENT_REQUEST_META), eq(TaskResultConsumer.LOM));
    }

    @Test
    public void testUpdateOrderItemsValidationWithInvalid() {
        Partner partner = DeliveryDtoFactory.createPartner();
        Stream.of(
            new Triple<>(
                new OrderItems.OrderItemsBuilder(null).build(),
                "orderId",
                "must not be null"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setTotal(null).build(),
                "total",
                "must not be null"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setTotal(BigDecimal.valueOf(-1)).build(),
                "total",
                "must be greater than or equal to 0"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setAssessedCost(null).build(),
                "assessedCost",
                "must not be null"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setAssessedCost(BigDecimal.valueOf(-1)).build(),
                "assessedCost",
                "must be greater than or equal to 0"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setDeliveryCost(null).build(),
                "deliveryCost",
                "must not be null"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setDeliveryCost(BigDecimal.valueOf(-1)).build(),
                "deliveryCost",
                "must be greater than or equal to 0"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setWeight(null).build(),
                "weight",
                "must not be null"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setWeight(BigDecimal.valueOf(-1)).build(),
                "weight",
                "must be greater than or equal to 0"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setWidth(null).build(),
                "width",
                "must not be null"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setLength(null).build(),
                "length",
                "must not be null"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setHeight(null).build(),
                "height",
                "must not be null"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setItems(null).build(),
                "item",
                "must not be empty"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setItems(Collections.emptyList()).build(),
                "item",
                "must not be empty"
            ),
            new Triple<>(
                DeliveryDtoFactory.createOrderItemsBuilder().setItems(Collections.singletonList(null)).build(),
                "item",
                "must not be null"
            )
        ).forEach(triple -> {
            assertThatThrownBy(() ->
                orderService.updateOrderItems(
                    triple.first,
                    partner,
                    CLIENT_REQUEST_META,
                    TaskResultConsumer.LOM
                ))
                .isInstanceOf(GatewayValidationException.class)
                .hasMessageContaining("propertyPath=orderItems." + triple.second)
                .hasMessageContaining("interpolatedMessage='" + triple.third);
        });
    }

    @Test
    public void testUpdateOrderItemsValidationWithNull() {
        Partner partner = DeliveryDtoFactory.createPartner();
        assertThatThrownBy(() ->
            orderService.updateOrderItems(
                null,
                partner,
                CLIENT_REQUEST_META,
                TaskResultConsumer.LOM
            ))
            .isInstanceOf(GatewayValidationException.class)
            .hasMessageContaining("propertyPath=orderItems")
            .hasMessageContaining("interpolatedMessage='must not be null'");
    }

    @Test
    public void testGetOrderHistory() {
        ResourceId orderId = DeliveryDtoFactory.createResourceId();
        Partner partner = DeliveryDtoFactory.createPartner();

        OrderStatusHistory expectedOrderStatusHistory = new OrderStatusHistory(null, orderId);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetOrderHistoryRequest(orderId, partner)),
            Mockito.eq(GetOrderHistoryResponse.class),
            Mockito.eq("delivery"), Mockito.eq("getOrderHistory")
        )).thenReturn(new GetOrderHistoryResponse(expectedOrderStatusHistory));

        OrderStatusHistory orderStatusHistory = orderService.getOrderHistory(orderId, partner);
        assertions.assertThat(orderStatusHistory)
            .as("Asserting that order status history wrapper is correct")
            .isEqualToComparingFieldByFieldRecursively(expectedOrderStatusHistory);
    }

    @Test
    public void testGetOrdersStatus() {
        ResourceId orderId = DeliveryDtoFactory.createResourceId();
        Partner partner = DeliveryDtoFactory.createPartner();

        OrderStatusHistory expectedOrderStatusHistory = new OrderStatusHistory(null, orderId);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetOrdersStatusRequest(Collections.singletonList(orderId), partner)),
            Mockito.eq(GetOrdersStatusResponse.class),
            Mockito.eq("delivery"), Mockito.eq("getOrdersStatus")
        )).thenReturn(new GetOrdersStatusResponse(Collections.singletonList(expectedOrderStatusHistory)));

        List<OrderStatusHistory> orderStatusHistories =
            orderService.getOrdersStatus(Collections.singletonList(orderId), partner);
        assertions.assertThat(orderStatusHistories)
            .as("Asserting that order status history wrapper is correct")
            .hasSameElementsAs(Collections.singletonList(expectedOrderStatusHistory));
    }

    @Test
    public void testGetExternalOrderHistory() {
        ExternalResourceId orderId = DeliveryDtoFactory.createExternalResourceId();
        Partner partner = DeliveryDtoFactory.createPartner();

        ExternalOrderStatusHistory expectedExternalOrderStatusHistory = new ExternalOrderStatusHistory(null, orderId);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetExternalOrderHistoryRequest(orderId, partner)),
            Mockito.eq(GetExternalOrderHistoryResponse.class),
            Mockito.eq("delivery"), Mockito.eq("getExternalOrderHistory")
        ))
            .thenReturn(new GetExternalOrderHistoryResponse(expectedExternalOrderStatusHistory));

        ExternalOrderStatusHistory externalOrderStatusHistory = orderService.getExternalOrderHistory(orderId, partner);
        assertions.assertThat(externalOrderStatusHistory)
            .as("Asserting that external order status history wrapper is correct")
            .isEqualToComparingFieldByFieldRecursively(expectedExternalOrderStatusHistory);
    }

    @Test
    public void testGetExternalOrdersStatus() {
        ExternalResourceId orderId = DeliveryDtoFactory.createExternalResourceId();
        Partner partner = DeliveryDtoFactory.createPartner();

        ExternalOrderStatusHistory expectedOrderStatusHistory = new ExternalOrderStatusHistory(null, orderId);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetExternalOrdersStatusRequest(Collections.singletonList(orderId), partner)),
            Mockito.eq(GetExternalOrdersStatusResponse.class),
            Mockito.eq("delivery"), Mockito.eq("getExternalOrdersStatus")
        ))
            .thenReturn(new GetExternalOrdersStatusResponse(Collections.singletonList(expectedOrderStatusHistory)));

        List<ExternalOrderStatusHistory> orderStatusHistories =
            orderService.getExternalOrdersStatus(Collections.singletonList(orderId), partner);
        assertions.assertThat(orderStatusHistories)
            .as("Asserting that external order status history wrapper is correct")
            .hasSameElementsAs(Collections.singletonList(expectedOrderStatusHistory));
    }

    @Test
    public void testGetOrdersDeliveryDate() {
        ResourceId orderId = DeliveryDtoFactory.createResourceId();
        Partner partner = DeliveryDtoFactory.createPartner();

        OrderDeliveryDate expectedOrderDeliveryDate = new OrderDeliveryDate(orderId,
            DateTime.fromLocalDateTime(LocalDateTime.of(2018, 10, 10, 0, 0)),
            new TimeInterval("03:00:00+03:00/02:59:00+03:00"),
            "message"

        );

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetOrdersDeliveryDateRequest(Collections.singletonList(orderId), partner)),
            Mockito.eq(GetOrdersDeliveryDateResponse.class),
            Mockito.eq("delivery"), Mockito.eq("getOrdersDeliveryDate")
        )).thenReturn(new GetOrdersDeliveryDateResponse(Collections.singletonList(expectedOrderDeliveryDate)));

        List<OrderDeliveryDate> orderStatusHistories =
            orderService.getOrdersDeliveryDate(Collections.singletonList(orderId), partner);
        assertions.assertThat(orderStatusHistories)
            .as("Asserting that order status history wrapper is correct")
            .hasSameElementsAs(Collections.singletonList(expectedOrderDeliveryDate));
    }

    @Test
    public void testGetReferenceWarehouses() {
        Partner partner = DeliveryDtoFactory.createPartner();

        Warehouse expectedWarehouse = DeliveryDtoFactory.createWarehouse();

        Mockito.when(
            httpTemplate.executePost(
                Mockito.refEq(new GetReferenceWarehousesRequest(partner)),
                Mockito.eq(GetReferenceWarehousesResponse.class),
                Mockito.eq("delivery"), Mockito.eq("getReferenceWarehouses")
            )
        ).thenReturn(new GetReferenceWarehousesResponse(Collections.singletonList(expectedWarehouse)));

        List<Warehouse> warehouses = dataExchangeService.getReferenceWarehouses(partner);
        assertions.assertThat(warehouses)
            .as("Asserting that warehouses are correct")
            .hasSameElementsAs(Collections.singleton(expectedWarehouse));
    }

    @Test
    public void testUpdateOrderDeliveryDateSuccessSend() throws GatewayApiException {
        OrderDeliveryDate orderDeliveryDate = DeliveryDtoFactory.createOrderDeliveryDate();
        Partner partner = DeliveryDtoFactory.createPartner();
        Long updateRequestId = 111L;

        orderService.updateOrderDeliveryDate(orderDeliveryDate, partner, updateRequestId, CLIENT_REQUEST_META,
            TaskResultConsumer.MDB);

        Mockito.verify(sqsSenderService).sendClientTask(eq(RequestFlow.DS_UPDATE_ORDER_DELIVERY_DATE),
            refEq(new UpdateOrderDeliveryDateRequest(orderDeliveryDate, partner, updateRequestId)), eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB));
    }

    @Test(expected = GatewayValidationException.class)
    public void testUpdateOrderDeliveryDateFailedOrderIdValidation() throws GatewayApiException {
        OrderDeliveryDate orderDeliveryDate = DeliveryDtoFactory.createInvalidOrderDeliveryDate();

        Partner partner = DeliveryDtoFactory.createPartner();

        orderService.updateOrderDeliveryDate(orderDeliveryDate, partner, 111L, CLIENT_REQUEST_META,
            TaskResultConsumer.MDB);
    }

    @Test
    public void testCreateSelfExport() throws GatewayApiException {

        Partner partner = DeliveryDtoFactory.createPartner();
        SelfExport selfExport = DeliveryDtoFactory.createSelfExport();

        orderService.createSelfExport(selfExport, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);

    }

    @Test(expected = GatewayValidationException.class)
    public void testCreateSelfExportFailedValidation() throws GatewayApiException {

        Partner partner = DeliveryDtoFactory.createPartner();
        SelfExport selfExport = DeliveryDtoFactory.createInvalidSelfExport();

        orderService.createSelfExport(selfExport, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);

    }

    @Test
    public void testGetAttachedDocs() throws GatewayApiException {
        Partner partner = DeliveryDtoFactory.createPartner();
        AttachedDocsData data = DeliveryDtoFactory.createAttachedDocsData();

        orderService.getAttachedDocs(
            data,
            partner,
            CLIENT_REQUEST_META,
            TaskResultConsumer.MDB
        );

        Mockito.verify(sqsSenderService).sendClientTask(
            eq(RequestFlow.DS_GET_ATTACHED_DOCS),
            refEq(new GetAttachedDocsRequest(data, partner)),
            eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB)
        );
    }

    @Test(expected = GatewayValidationException.class)
    public void testGetAttachedDocsFailedValidation() throws GatewayApiException {
        Partner partner = DeliveryDtoFactory.createPartner();
        AttachedDocsData data = DeliveryDtoFactory.createInvalidAttachedDocsData();

        orderService.getAttachedDocs(
            data,
            partner,
            CLIENT_REQUEST_META,
            TaskResultConsumer.MDB
        );
    }

    @Test
    public void testGetTransactionsOders() {
        Partner partner = DeliveryDtoFactory.createPartner();
        List<Transaction> expectedTransactions = Collections.singletonList(DeliveryDtoFactory.createTransaction());

        Mockito.when(
            httpTemplate.executePost(
                Mockito.any(GetTransactionsOrdersRequest.class),
                Mockito.eq(GetTransactionsOrdersResponse.class),
                Mockito.eq("delivery"), Mockito.eq("getTransactionsOrders")
            )
        ).thenReturn(new GetTransactionsOrdersResponse(expectedTransactions));

        List<Transaction> transactions = orderService.getTransactionsOrders(
            DeliveryDtoFactory.createResourceId(),
            DeliveryDtoFactory.createDateTimeInterval(),
            0,
            0,
            partner
        );

        assertions.assertThat(transactions)
            .as("GetTransactionsOrders response is correct")
            .hasSameElementsAs(expectedTransactions);
    }

    @Test
    public void testPutReferenceWarehouses() throws GatewayApiException {
        Partner partner = DeliveryDtoFactory.createPartner();
        Warehouse warehouse = DeliveryDtoFactory.createWarehouse();

        dataExchangeService.putReferenceWarehouses(
            partner,
            Collections.singletonList(warehouse),
            false,
            CLIENT_REQUEST_META,
            TaskResultConsumer.MDB
        );

        PutReferenceWarehousesRequest expectedRequest =
            new PutReferenceWarehousesRequest(partner, Collections.singletonList(warehouse), false);

        Mockito.verify(sqsSenderService)
            .sendClientTask(
                eq(RequestFlow.DS_PUT_REFERENCE_WAREHOUSES),
                refEq(expectedRequest),
                eq(CLIENT_REQUEST_META),
                eq(TaskResultConsumer.MDB)
            );
    }

    @Test(expected = GatewayValidationException.class)
    public void testPutReferenceWarehousesValidation() throws GatewayApiException {
        dataExchangeService.putReferenceWarehouses(
            DeliveryDtoFactory.createPartner(),
            Collections.EMPTY_LIST,
            false,
            CLIENT_REQUEST_META,
            TaskResultConsumer.MDB
        );
    }

    @Test
    public void getReferenceTimetableCouriersSuccess() throws GatewayApiException {
        List<Location> locations = List.of(DeliveryDtoFactory.createLocation());
        Partner partner = DeliveryDtoFactory.createPartner();
        dataExchangeService.getReferenceTimetableCouriers(
            locations,
            partner,
            CLIENT_REQUEST_META,
            TaskResultConsumer.MDB
        );
        Mockito.verify(sqsSenderService).sendClientTask(
            eq(RequestFlow.DS_GET_REFERENCE_TIMETABLE_COURIERS),
            refEq(new GetReferenceTimetableCouriersRequest(partner, locations)),
            eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB)
        );
    }

    @Test
    public void getReferenceTimetableCouriersValidationFailed() {
        Partner partner = DeliveryDtoFactory.createPartner();
        Stream.of(
            new Triple<>(
                new LocationBuilder(null, "Новосибирск", "Новосибирская область").build(),
                "country",
                "must not be blank"
            ),
            new Triple<>(
                new LocationBuilder("", "Новосибирск", "Новосибирская область").build(),
                "country",
                "must not be blank"
            ),
            new Triple<>(
                new LocationBuilder("Россия", null, "Новосибирская область").build(),
                "locality",
                "must not be blank"
            ),
            new Triple<>(
                new LocationBuilder("Россия", "", "Новосибирская область").build(),
                "locality",
                "must not be blank"
            ),
            new Triple<>(
                new LocationBuilder("Россия", "Новосибирск", null).build(),
                "region",
                "must not be blank"
            ),
            new Triple<>(
                new LocationBuilder("Россия", "Новосибирск", "").build(),
                "region",
                "must not be blank"
            )
        )
            .forEach(
                triple ->
                    assertions.assertThatThrownBy(
                        () -> dataExchangeService.getReferenceTimetableCouriers(
                            List.of(triple.first),
                            partner,
                            CLIENT_REQUEST_META,
                            TaskResultConsumer.MDB
                        )
                    )
                        .isInstanceOf(GatewayValidationException.class)
                        .hasMessageContaining("propertyPath=searchByLocations[0]." + triple.second + ",")
                        .hasMessageContaining("interpolatedMessage='" + triple.third)
            );
    }

    @Test
    public void cancelMovementSuccess() throws GatewayApiException {
        ru.yandex.market.logistic.gateway.common.model.common.ResourceId movementId =
            FulfillmentDtoFactory.createCommonResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();
        movementService.cancelMovement(movementId, partner, CLIENT_REQUEST_META, TaskResultConsumer.TM);

        Mockito.verify(sqsSenderService).sendClientTask(
            eq(RequestFlow.DS_CANCEL_MOVEMENT),
            refEq(new CancelMovementRequest(movementId, partner)),
            eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.TM))
        ;
    }

    @Test
    public void cancelMovementValidationFailed() {
        Partner partner = FulfillmentDtoFactory.createPartner();
        Stream.of(
            new Triple<>(
                ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder().build(),
                "movementId.partnerId",
                "must not be blank"
            ),
            new Triple<ru.yandex.market.logistic.gateway.common.model.common.ResourceId, String, String>(
                null,
                "movementId",
                "must not be null"
            )
        )
            .forEach(
                triple ->
                    assertions.assertThatThrownBy(
                        () -> movementService.cancelMovement(
                            triple.first,
                            partner,
                            CLIENT_REQUEST_META,
                            TaskResultConsumer.TM
                        )
                    )
                        .isInstanceOf(GatewayValidationException.class)
                        .hasMessageContaining("propertyPath=" + triple.second + ",")
                        .hasMessageContaining("interpolatedMessage='" + triple.third)
            );
    }

    @Test
    public void updateItemsInstancesSuccess() throws GatewayApiException {
        ResourceId orderId = DeliveryDtoFactory.createResourceId();
        List<ItemInstances> itemsInstances = DeliveryDtoFactory.createItemsInstances();
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.updateItemsInstances(
            orderId,
            itemsInstances,
            partner,
            CLIENT_REQUEST_META,
            TaskResultConsumer.LOM
        );
        Mockito.verify(sqsSenderService).sendClientTask(
            eq(RequestFlow.DS_UPDATE_ITEMS_INSTANCES),
            refEq(new UpdateItemsInstancesRequest(partner, orderId, itemsInstances)),
            eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.LOM)
        );
    }

    @Test
    public void updateItemsInstancesValidationFailed() {
        Partner partner = DeliveryDtoFactory.createPartner();
        Stream.<Quadruple<ResourceId, List<ItemInstances>, String, String>>of(
            new Quadruple<>(
                null,
                DeliveryDtoFactory.createItemsInstances(),
                "orderId",
                "must not be null"
            ),
            new Quadruple<>(
                DeliveryDtoFactory.createResourceId(),
                Collections.emptyList(),
                "itemsInstances",
                "size must be between 1 and 2147483647"
            ),
            new Quadruple<>(
                DeliveryDtoFactory.createResourceId(),
                Collections.singletonList(
                    new ItemInstances.ItemInstancesBuilder()
                        .setInstances(DeliveryDtoFactory.createItemInstances())
                        .build()
                ),
                "itemsInstances[0].unitId",
                "must not be null"
            ),
            new Quadruple<>(
                DeliveryDtoFactory.createResourceId(),
                Collections.singletonList(
                    new ItemInstances.ItemInstancesBuilder()
                        .setUnitId(new UnitId.UnitIdBuilder(null, "article").build())
                        .setInstances(DeliveryDtoFactory.createItemInstances())
                        .build()
                ),
                "itemsInstances[0].unitId.vendorId",
                "must not be null"
            ),
            new Quadruple<>(
                DeliveryDtoFactory.createResourceId(),
                Collections.singletonList(
                    new ItemInstances.ItemInstancesBuilder()
                        .setUnitId(new UnitId.UnitIdBuilder(48905L, "").build())
                        .setInstances(DeliveryDtoFactory.createItemInstances())
                        .build()
                ),
                "itemsInstances[0].unitId.article",
                "must not be blank"
            ),
            new Quadruple<>(
                DeliveryDtoFactory.createResourceId(),
                Collections.singletonList(
                    new ItemInstances.ItemInstancesBuilder()
                        .setUnitId(new UnitId.UnitIdBuilder(48905L, "article").build())
                        .setInstances(Collections.emptyList())
                        .build()
                ),
                "itemsInstances[0].instances",
                "size must be between 1 and 2147483647"
            )
        )
            .forEach(
                quadruple ->
                    assertions.assertThatThrownBy(
                        () -> orderService.updateItemsInstances(
                            quadruple.getFirst(),
                            quadruple.getSecond(),
                            partner,
                            CLIENT_REQUEST_META,
                            TaskResultConsumer.MDB
                        )
                    )
                        .isInstanceOf(GatewayValidationException.class)
                        .hasMessageContaining("propertyPath=" + quadruple.getThird())
                        .hasMessageContaining("interpolatedMessage='" + quadruple.getFourth())
            );
    }

    @Test
    public void updateOrderTransferCodesSuccess() throws GatewayApiException {
        ResourceId orderId = DeliveryDtoFactory.createResourceId();
        OrderTransferCodes transferCodes = DeliveryDtoFactory.createValidOrderTransferCodes();
        Partner partner = DeliveryDtoFactory.createPartner();
        orderService.updateOrderTransferCodes(
            orderId,
            transferCodes,
            partner,
            CLIENT_REQUEST_META,
            TaskResultConsumer.LOM
        );
        Mockito.verify(sqsSenderService).sendClientTask(
            eq(RequestFlow.DS_UPDATE_ORDER_TRANSFER_CODES),
            refEq(new UpdateOrderTransferCodesRequest(partner, orderId, transferCodes)),
            eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.LOM)
        );
    }

    @Test
    public void updateOrderTransferCodesFailed() {
        Partner partner = DeliveryDtoFactory.createPartner();
        Stream.<Quadruple<ResourceId, OrderTransferCodes, String, String>>of(
            new Quadruple<>(
                null,
                DeliveryDtoFactory.createValidOrderTransferCodes(),
                "orderId",
                "must not be null"
            ),
            new Quadruple<>(
                DeliveryDtoFactory.createResourceId(),
                null,
                "transferCodes",
                "must not be null"
            )
        )
            .forEach(
                quadruple ->
                    assertions.assertThatThrownBy(
                        () -> orderService.updateOrderTransferCodes(
                            quadruple.getFirst(),
                            quadruple.getSecond(),
                            partner,
                            CLIENT_REQUEST_META,
                            TaskResultConsumer.LOM
                        )
                    )
                        .isInstanceOf(GatewayValidationException.class)
                        .hasMessageContaining("propertyPath=" + quadruple.getThird())
                        .hasMessageContaining("interpolatedMessage='" + quadruple.getFourth())
            );
    }

    @Test
    public void testGetCourier() {
        ResourceId orderId = DeliveryDtoFactory.createResourceId();
        Partner partner = DeliveryDtoFactory.createPartner();
        Courier courier = CommonDtoFactory.createCourier();
        OrderTransferCodes codes = CommonDtoFactory.createOrderTransferCodes();

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetCourierRequest(orderId, partner)),
            Mockito.eq(GetCourierResponse.class),
            Mockito.eq("delivery"), Mockito.eq("getCourier")
        )).thenReturn(new GetCourierResponse(courier, codes));

        GetCourierResponse courierResponse =
            dataExchangeService.getCourier(orderId, partner);
        Assert.assertEquals(courierResponse.getCourier(), courier);
        Assert.assertEquals(courierResponse.getCodes(), codes);
    }
}
