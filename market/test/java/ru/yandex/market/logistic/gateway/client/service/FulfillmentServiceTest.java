package ru.yandex.market.logistic.gateway.client.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.common.util.collections.Triple;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayValidationException;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.InboundService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.InboundXDocService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.MovementService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.OrderService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.OutboundService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.ShipmentService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.TransferService;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.request.CancelMovementRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Inbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundDetailsXDoc;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Order;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderItems;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Outbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegisterType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegisterUnit;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegisterUnitType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnInboundDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Transfer;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferDetailsItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransportationRegister;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.CancelInboundRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.CancelOutboundRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.CreateInboundRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.CreateOrderRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.CreateOutboundRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.CreateTransferRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetInboundDetailsRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetInboundDetailsXDocRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetInboundHistoryRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetInboundsStatusRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetOrderHistoryRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetOrderRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetOrdersStatusRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetOutboundDetailsRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetOutboundHistoryRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetReturnInboundDetailsRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetTransferDetailsRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetTransferHistoryRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetTransfersStatusRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.PutRegisterRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.UpdateInboundRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.UpdateOrderItemsRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetInboundDetailsResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetInboundDetailsXDocResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetInboundHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetInboundsStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetOrdersStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetOutboundDetailsResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetOutboundHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetReturnInboundDetailsResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetTransferDetailsResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetTransferHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetTransfersStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistic.gateway.utils.FulfillmentDtoFactory;
import ru.yandex.market.logistics.util.client.HttpTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.refEq;

@RunWith(MockitoJUnitRunner.class)
public class FulfillmentServiceTest {

    static {
        Locale.setDefault(Locale.ENGLISH);
    }

    private static final String PROCESS_ID = "processIdABC123";

    private static final ClientRequestMeta CLIENT_REQUEST_META = new ClientRequestMeta(PROCESS_ID);

    private static final CreateOrderRestrictedData RESTRICTED_DATA = FulfillmentDtoFactory.createOrderRestrictedData();

    @Mock
    private SqsSenderService sqsSenderService;

    @Mock
    private HttpTemplate httpTemplate;

    @InjectMocks
    private InboundService inboundService;

    @InjectMocks
    private OutboundService outboundService;

    @InjectMocks
    private TransferService transferService;

    @InjectMocks
    private InboundXDocService inboundXDocService;

    @InjectMocks
    private OrderService orderService;

    @InjectMocks
    private ShipmentService shipmentService;

    @InjectMocks
    private MovementService movementService;

    private SoftAssertions assertions = new SoftAssertions();

    @After
    public void tearDown() {
        assertions.assertAll();
    }

    @Test
    public void testCreateOrderSuccessSend() throws GatewayApiException {
        Order order = FulfillmentDtoFactory.createOrder();
        Partner partner = FulfillmentDtoFactory.createPartner();
        orderService.createOrder(order, partner, RESTRICTED_DATA, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
        Mockito.verify(sqsSenderService).sendClientTask(eq(RequestFlow.FF_CREATE_ORDER),
            refEq(new CreateOrderRequest(order, RESTRICTED_DATA, partner)), eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB));
    }

    @Test
    public void testCreateOrderWithoutProcessIdSuccessSend() throws GatewayApiException {
        Order order = FulfillmentDtoFactory.createOrder();
        Partner partner = FulfillmentDtoFactory.createPartner();
        orderService.createOrder(order, partner, RESTRICTED_DATA, null, TaskResultConsumer.MDB);
        Mockito.verify(sqsSenderService).sendClientTask(eq(RequestFlow.FF_CREATE_ORDER),
            refEq(new CreateOrderRequest(order, RESTRICTED_DATA, partner)), isNull(), eq(TaskResultConsumer.MDB));
    }

    @Test(expected = GatewayValidationException.class)
    public void testCreateOrderFailedValidation() throws GatewayApiException {
        Order order = FulfillmentDtoFactory.createInvalidOrder();
        Partner partner = FulfillmentDtoFactory.createPartner();
        orderService.createOrder(order, partner, RESTRICTED_DATA, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
    }

    @Test
    public void testCreateOrderInvalidAutoRemoveParamsValidation() {
        assertOrderValidation(
            builder -> builder.setMaxAbsentItemsPricePercent(null),
            "Order.maxAbsentItemsPricePercent must be set when any item has removableIfAbsent flag set"
        );
    }

    @Test
    public void testCreateOrderTooLargeMaxAbsentItemsPricePercent() {
        assertOrderValidation(
            builder -> builder.setMaxAbsentItemsPricePercent(new BigDecimal("120")),
            "interpolatedMessage='must be less than or equal to 100', propertyPath=order.maxAbsentItemsPricePercent"
        );
    }

    @Test
    public void testCreateOrderTooSmallMaxAbsentItemsPricePercent() {
        assertOrderValidation(
            builder -> builder.setMaxAbsentItemsPricePercent(new BigDecimal("-20")),
            "must be greater than or equal to 0', propertyPath=order.maxAbsentItemsPricePercent"
        );
    }

    private void assertOrderValidation(Consumer<Order.OrderBuilder> mutator, String errorMessage) {
        Order.OrderBuilder builder = FulfillmentDtoFactory.createOrderBuilder();
        mutator.accept(builder);

        Partner partner = FulfillmentDtoFactory.createPartner();

        assertThatThrownBy(() -> orderService.createOrder(
            builder.build(),
            partner,
            RESTRICTED_DATA,
            CLIENT_REQUEST_META,
            TaskResultConsumer.MDB
        ))
            .isInstanceOf(GatewayValidationException.class)
            .hasMessageContaining(errorMessage);

    }

    @Test
    public void testCreateInboundSuccessSend() throws GatewayApiException {
        Inbound inbound = FulfillmentDtoFactory.createInbound();
        Partner partner = FulfillmentDtoFactory.createPartner();
        inboundService.createInbound(inbound, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
        Mockito.verify(sqsSenderService).sendClientTask(Mockito.eq(RequestFlow.FF_CREATE_INBOUND),
            Mockito.refEq(new CreateInboundRequest(inbound, partner)), eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB));
    }

    @Test(expected = GatewayValidationException.class)
    public void testCreateInboundFailedValidation() throws GatewayApiException {
        Inbound inbound = FulfillmentDtoFactory.createInvalidInbound();
        Partner partner = FulfillmentDtoFactory.createPartner();
        inboundService.createInbound(inbound, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
    }

    @Test
    public void testUpdateInboundSuccessSend() throws GatewayApiException {
        Inbound inbound = FulfillmentDtoFactory.createInbound();
        Partner partner = FulfillmentDtoFactory.createPartner();
        inboundService.updateInbound(inbound, partner, CLIENT_REQUEST_META, TaskResultConsumer.FF_WF_API);
        Mockito.verify(sqsSenderService).sendClientTask(Mockito.eq(RequestFlow.FF_UPDATE_INBOUND),
            Mockito.refEq(new UpdateInboundRequest(inbound, partner)), eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.FF_WF_API));
    }

    @Test(expected = GatewayValidationException.class)
    public void testUpdateInboundFailedValidation() throws GatewayApiException {
        Inbound inbound = FulfillmentDtoFactory.createInvalidInbound();
        Partner partner = FulfillmentDtoFactory.createPartner();
        inboundService.updateInbound(inbound, partner, CLIENT_REQUEST_META, TaskResultConsumer.FF_WF_API);
    }

    @Test
    public void testCreateTransferSuccessSend() throws GatewayApiException {
        Transfer transfer = FulfillmentDtoFactory.createTransfer();
        Partner partner = FulfillmentDtoFactory.createPartner();
        transferService.createTransfer(transfer, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
        Mockito.verify(sqsSenderService).sendClientTask(Mockito.eq(RequestFlow.FF_CREATE_TRANSFER),
            Mockito.refEq(new CreateTransferRequest(transfer, partner)), eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB));
    }

    @Test(expected = GatewayValidationException.class)
    public void testCreateTransferFailedValidation() throws GatewayApiException {
        Partner partner = FulfillmentDtoFactory.createPartner();
        transferService.createTransfer(null, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
    }

    @Test
    public void testGetOrderSuccessSend() throws GatewayApiException {
        ResourceId orderId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();
        orderService.getOrder(orderId, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
        Mockito.verify(sqsSenderService).sendClientTask(Mockito.eq(RequestFlow.FF_GET_ORDER),
            Mockito.refEq(new GetOrderRequest(orderId, partner)), eq(CLIENT_REQUEST_META), eq(TaskResultConsumer.MDB));
    }

    @Test(expected = GatewayValidationException.class)
    public void testGetOrderFailedValidation() throws GatewayApiException {
        Partner partner = FulfillmentDtoFactory.createPartner();
        orderService.getOrder(null, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
    }

    @Test
    public void testGetTransferDetails() {
        ResourceId transferId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();

        List<TransferDetailsItem> transferDetailsItems = new ArrayList<>();
        transferDetailsItems.add(new TransferDetailsItem(FulfillmentDtoFactory.createUnitId(), 4, 4));
        TransferDetails expectedTransferDetails = new TransferDetails(transferId, transferDetailsItems);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetTransferDetailsRequest(transferId, partner)),
            Mockito.eq(GetTransferDetailsResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getTransferDetails")))
            .thenReturn(new GetTransferDetailsResponse(expectedTransferDetails));

        TransferDetails transferDetails = transferService.getTransferDetails(transferId, partner);
        Assert.assertEquals(expectedTransferDetails, transferDetails);
    }

    @Test
    public void testGetTransferHistory() {
        ResourceId transferId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();

        TransferStatusHistory exceptedStatusHistory = new TransferStatusHistory(null, transferId);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetTransferHistoryRequest(transferId, partner)),
            Mockito.eq(GetTransferHistoryResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getTransferHistory")))
            .thenReturn(new GetTransferHistoryResponse(exceptedStatusHistory));

        TransferStatusHistory transferHistory = transferService.getTransferHistory(transferId, partner);
        Assert.assertEquals(exceptedStatusHistory, transferHistory);
    }

    @Test
    public void testGetTransfersStatus() {
        ResourceId transferId = FulfillmentDtoFactory.createResourceId();
        List<ResourceId> transfersId = Collections.singletonList(transferId);
        Partner partner = FulfillmentDtoFactory.createPartner();
        List<TransferStatus> exceptedTransfersStatus =
            Collections.singletonList(new TransferStatus(transferId, null));

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetTransfersStatusRequest(transfersId, partner)),
            Mockito.eq(GetTransfersStatusResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getTransfersStatus")))
            .thenReturn(new GetTransfersStatusResponse(exceptedTransfersStatus));

        List<TransferStatus> transfersStatus = transferService.getTransfersStatus(transfersId, partner);
        Assert.assertEquals(exceptedTransfersStatus, transfersStatus);
    }

    @Test
    public void testGetInboundDetailsXDoc() {
        ResourceId inboundId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();

        InboundDetailsXDoc expectedInboundDetailsXDoc = new InboundDetailsXDoc(inboundId, 10, 15);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetInboundDetailsXDocRequest(inboundId, partner)),
            Mockito.eq(GetInboundDetailsXDocResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getInboundDetailsXDoc")))
            .thenReturn(new GetInboundDetailsXDocResponse(expectedInboundDetailsXDoc));

        InboundDetailsXDoc inboundDetailsXDoc = inboundXDocService.getInboundDetailsXDoc(inboundId, partner);
        Assert.assertEquals(expectedInboundDetailsXDoc, inboundDetailsXDoc);
    }

    @Test
    public void testCancelInboundSuccessSend() throws Exception {
        ResourceId inboundId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();
        inboundService.cancelInbound(inboundId, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);

        Mockito.verify(sqsSenderService).sendClientTask(Mockito.eq(RequestFlow.FF_CANCEL_INBOUND),
            Mockito.refEq(new CancelInboundRequest(inboundId, partner)), eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB));
    }

    @Test
    public void testGetInboundDetails() throws Exception {
        ResourceId inboundId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();

        InboundDetails expectedInboundDetails = new InboundDetails(inboundId, null);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetInboundDetailsRequest(inboundId, partner)),
            Mockito.eq(GetInboundDetailsResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getInboundDetails")))
            .thenReturn(new GetInboundDetailsResponse(expectedInboundDetails));

        InboundDetails inboundDetails = inboundService.getInboundDetails(inboundId, partner);
        Assert.assertEquals(expectedInboundDetails, inboundDetails);
    }

    @Test
    public void testGetReturnInboundDetails() {
        ResourceId inboundId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();

        ReturnInboundDetails expectedInboundDetails = new ReturnInboundDetails(inboundId, null, null);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetReturnInboundDetailsRequest(inboundId, partner)),
            Mockito.eq(GetReturnInboundDetailsResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getReturnInboundDetails")))
            .thenReturn(new GetReturnInboundDetailsResponse(expectedInboundDetails));

        ReturnInboundDetails returnInboundDetails = inboundService.getReturnInboundDetails(inboundId, partner);
        Assert.assertEquals(expectedInboundDetails, returnInboundDetails);
    }

    @Test
    public void testGetInboundHistory() throws Exception {
        ResourceId inboundId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();

        InboundStatusHistory exceptedStatusHistory = new InboundStatusHistory(null, inboundId);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetInboundHistoryRequest(inboundId, partner)),
            Mockito.eq(GetInboundHistoryResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getInboundHistory")))
            .thenReturn(new GetInboundHistoryResponse(exceptedStatusHistory));

        InboundStatusHistory inboundHistory = inboundService.getInboundHistory(inboundId, partner);
        Assert.assertEquals(exceptedStatusHistory, inboundHistory);
    }

    @Test
    public void testGetInboundsStatus() throws Exception {
        ResourceId inboundId = FulfillmentDtoFactory.createResourceId();
        List<ResourceId> inboundsId = Collections.singletonList(inboundId);
        Partner partner = FulfillmentDtoFactory.createPartner();
        List<InboundStatus> exceptedInboundsStatuses =
            Collections.singletonList(new InboundStatus(inboundId, null));

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetInboundsStatusRequest(inboundsId, partner)),
            Mockito.eq(GetInboundsStatusResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getInboundsStatus")))
            .thenReturn(new GetInboundsStatusResponse(exceptedInboundsStatuses));

        List<InboundStatus> inboundsStatus = inboundService.getInboundsStatus(inboundsId, partner);
        Assert.assertEquals(exceptedInboundsStatuses, inboundsStatus);
    }

    @Test
    public void testCreateOutboundSuccessSend() throws GatewayApiException {
        Outbound outbound = FulfillmentDtoFactory.createOutbound();
        Partner partner = FulfillmentDtoFactory.createPartner();

        outboundService.createOutbound(outbound, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);

        Mockito.verify(sqsSenderService).sendClientTask(Mockito.eq(RequestFlow.FF_CREATE_OUTBOUND),
            Mockito.refEq(new CreateOutboundRequest(outbound, partner)), eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB));
    }

    @Test(expected = GatewayValidationException.class)
    public void testCreateOutboundFailedValidation() throws GatewayApiException {
        Outbound outbound = FulfillmentDtoFactory.createInvalidOutbound();
        Partner partner = FulfillmentDtoFactory.createPartner();

        outboundService.createOutbound(outbound, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);
    }

    @Test
    public void testCancelOutboundSuccessSend() throws Exception {
        ResourceId outboundId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();
        outboundService.cancelOutbound(outboundId, partner, CLIENT_REQUEST_META, TaskResultConsumer.MDB);

        Mockito.verify(sqsSenderService).sendClientTask(Mockito.eq(RequestFlow.FF_CANCEL_OUTBOUND),
            Mockito.refEq(new CancelOutboundRequest(outboundId, partner)), eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.MDB));
    }

    @Test
    public void testGetOutboundHistory() throws Exception {
        ResourceId outboundId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();

        OutboundStatusHistory exceptedOutboundHistory = new OutboundStatusHistory(outboundId, null);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetOutboundHistoryRequest(outboundId, partner)),
            Mockito.eq(GetOutboundHistoryResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getOutboundHistory")))
            .thenReturn(new GetOutboundHistoryResponse(exceptedOutboundHistory));

        OutboundStatusHistory outboundHistory = outboundService.getOutboundHistory(outboundId, partner);
        Assert.assertEquals(exceptedOutboundHistory, outboundHistory);
    }

    @Test
    public void testGetOutboundDetails() throws Exception {
        ResourceId outboundId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();

        OutboundDetails exceptedOutboundDetails = new OutboundDetails(outboundId, null);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetOutboundDetailsRequest(outboundId, partner)),
            Mockito.eq(GetOutboundDetailsResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getOutboundDetails")))
            .thenReturn(new GetOutboundDetailsResponse(exceptedOutboundDetails));

        OutboundDetails outboundDetails = outboundService.getOutboundDetails(outboundId, partner);
        Assert.assertEquals(exceptedOutboundDetails, outboundDetails);
    }

    @Test
    public void testGetOrderHistory() throws Exception {
        ResourceId orderId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();

        OrderStatusHistory expectedOrderStatusHistory = new OrderStatusHistory(null, orderId);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetOrderHistoryRequest(orderId, partner)),
            Mockito.eq(GetOrderHistoryResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getOrderHistory")
        )).thenReturn(new GetOrderHistoryResponse(expectedOrderStatusHistory));

        OrderStatusHistory orderStatusHistory = orderService.getOrderHistory(orderId, partner);
        assertions.assertThat(orderStatusHistory)
            .as("Asserting that order status history wrapper is correct")
            .isEqualToComparingFieldByFieldRecursively(expectedOrderStatusHistory);
    }

    @Test
    public void testGetOrdersStatus() throws Exception {
        ResourceId orderId = FulfillmentDtoFactory.createResourceId();
        Partner partner = FulfillmentDtoFactory.createPartner();

        OrderStatusHistory expectedOrderStatusHistory = new OrderStatusHistory(null, orderId);

        Mockito.when(httpTemplate.executePost(
            Mockito.refEq(new GetOrdersStatusRequest(Collections.singletonList(orderId), partner)),
            Mockito.eq(GetOrdersStatusResponse.class),
            Mockito.eq("fulfillment"), Mockito.eq("getOrdersStatus")
        )).thenReturn(new GetOrdersStatusResponse(Collections.singletonList(expectedOrderStatusHistory)));

        List<OrderStatusHistory> orderStatusHistories =
            orderService.getOrdersStatus(Collections.singletonList(orderId), partner);
        assertions.assertThat(orderStatusHistories)
            .as("Asserting that order status history wrapper is correct")
            .hasSameElementsAs(Collections.singletonList(expectedOrderStatusHistory));
    }

    @Test
    public void putRegisterSuccess() throws GatewayApiException {
        TransportationRegister transportationRegister = FulfillmentDtoFactory.createTransportationRegister();
        Partner partner = FulfillmentDtoFactory.createPartner();
        shipmentService.putRegister(
                transportationRegister,
                partner,
                CLIENT_REQUEST_META,
                TaskResultConsumer.FF_WF_API
        );

        Mockito.verify(sqsSenderService).sendClientTask(
            eq(RequestFlow.FF_PUT_REGISTER),
            refEq(new PutRegisterRequest(transportationRegister, partner)),
            eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.FF_WF_API))
        ;
    }

    @Test
    public void putRegisterValidationFailed() {
        Partner partner = FulfillmentDtoFactory.createPartner();
        Stream.of(
            new Triple<>(
                new TransportationRegister(
                    null,
                    RegisterType.YANDEX,
                    null,
                    List.of(FulfillmentDtoFactory.createRegisterUnit(null))
                ),
                "transportationRequestId",
                "must not be null"
            ),
            new Triple<>(
                new TransportationRegister(
                    FulfillmentDtoFactory.createResourceId(),
                    null,
                    null,
                    List.of(FulfillmentDtoFactory.createRegisterUnit(null))
                ),
                "registerType",
                "must not be null"
            ),
            new Triple<>(
                new TransportationRegister(
                    FulfillmentDtoFactory.createResourceId(),
                    RegisterType.YANDEX,
                    null,
                    null
                ),
                "registerUnits",
                "must not be empty"
            ),
            new Triple<>(
                new TransportationRegister(
                    FulfillmentDtoFactory.createResourceId(),
                    RegisterType.YANDEX,
                    null,
                    List.of()
                ),
                "registerUnits",
                "must not be empty"
            ),
            new Triple<>(
                new TransportationRegister(
                    FulfillmentDtoFactory.createResourceId(),
                    RegisterType.YANDEX,
                    null,
                    List.of()
                ),
                "registerUnits",
                "must not be empty"
            ),
            new Triple<>(
                new TransportationRegister(
                    FulfillmentDtoFactory.createResourceId(),
                    RegisterType.YANDEX,
                    null,
                    List.of(new RegisterUnit(null, null, null, null, null, null))
                ),
                "registerUnits[0].registerUnitType",
                "must not be null"
            ),
            new Triple<>(
                new TransportationRegister(
                    FulfillmentDtoFactory.createResourceId(),
                    RegisterType.YANDEX,
                    null,
                    List.of(new RegisterUnit(RegisterUnitType.BOX, null, null, null, null, -1))
                ),
                "registerUnits[0].amount",
                "must be greater than 0"
            )
        ).forEach(triple ->
            assertions.assertThatThrownBy(() -> shipmentService.putRegister(
                triple.first,
                partner,
                CLIENT_REQUEST_META,
                TaskResultConsumer.FF_WF_API
            ))
                .isInstanceOf(GatewayValidationException.class)
                .hasMessageContaining("propertyPath=transportationRegister." + triple.second + ",")
                .hasMessageContaining("interpolatedMessage='" + triple.third)
        );
    }

    @Test
    public void updateOrderItemsSuccess() throws GatewayApiException {
        OrderItems orderItems = FulfillmentDtoFactory.createOrderItemsBuilder().build();
        Partner partner = FulfillmentDtoFactory.createPartner();
        orderService.updateOrderItems(
            orderItems,
            partner,
            CLIENT_REQUEST_META,
            TaskResultConsumer.FF_WF_API
        );

        Mockito.verify(sqsSenderService).sendClientTask(
            eq(RequestFlow.FF_UPDATE_ORDER_ITEMS),
            refEq(new UpdateOrderItemsRequest(orderItems, partner)),
            eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.FF_WF_API))
        ;
    }

    @Test
    public void updateOrderItemsValidationFailed()  {
        Partner partner = FulfillmentDtoFactory.createPartner();
        Stream.of(
            new Triple<Function<OrderItems.OrderItemsBuilder, OrderItems.OrderItemsBuilder>, String, String>(
                builder -> builder.setOrderId(null),
                "orderId",
                "must not be null"
            ),
            new Triple<Function<OrderItems.OrderItemsBuilder, OrderItems.OrderItemsBuilder>, String, String>(
                builder -> builder.setKorobyte(null),
                "korobyte",
                "must not be null"
            ),
            new Triple<Function<OrderItems.OrderItemsBuilder, OrderItems.OrderItemsBuilder>, String, String>(
                builder -> builder.setAssessedCost(null),
                "assessedCost",
                "must not be null"
            ),
            new Triple<Function<OrderItems.OrderItemsBuilder, OrderItems.OrderItemsBuilder>, String, String>(
                builder -> builder.setAssessedCost(BigDecimal.valueOf(-1L)),
                "assessedCost",
                "must be greater than or equal to 0"
            ),
            new Triple<Function<OrderItems.OrderItemsBuilder, OrderItems.OrderItemsBuilder>, String, String>(
                builder -> builder.setTotal(null),
                "total",
                "must not be null"
            ),
            new Triple<Function<OrderItems.OrderItemsBuilder, OrderItems.OrderItemsBuilder>, String, String>(
                builder -> builder.setTotal(BigDecimal.valueOf(-1L)),
                "total",
                "must be greater than or equal to 0"
            ),
            new Triple<Function<OrderItems.OrderItemsBuilder, OrderItems.OrderItemsBuilder>, String, String>(
                builder -> builder.setDeliveryCost(null),
                "deliveryCost",
                "must not be null"
            ),
            new Triple<Function<OrderItems.OrderItemsBuilder, OrderItems.OrderItemsBuilder>, String, String>(
                builder -> builder.setDeliveryCost(BigDecimal.valueOf(-1L)),
                "deliveryCost",
                "must be greater than or equal to 0"
            ),
            new Triple<Function<OrderItems.OrderItemsBuilder, OrderItems.OrderItemsBuilder>, String, String>(
                builder -> builder.setItems(null),
                "items",
                "must not be empty"
            ),
            new Triple<Function<OrderItems.OrderItemsBuilder, OrderItems.OrderItemsBuilder>, String, String>(
                builder -> builder.setItems(List.of()),
                "items",
                "must not be empty"
            ),
            new Triple<Function<OrderItems.OrderItemsBuilder, OrderItems.OrderItemsBuilder>, String, String>(
                builder -> builder.setItems(Collections.singletonList(null)),
                "items[0].<list element>",
                "must not be null"
            )
        )
            .forEach(
                triple ->
                    assertions.assertThatThrownBy(
                        () -> orderService.updateOrderItems(
                            triple.first.apply(FulfillmentDtoFactory.createOrderItemsBuilder()).build(),
                            partner,
                            CLIENT_REQUEST_META,
                            TaskResultConsumer.LOM
                        )
                    )
                        .isInstanceOf(GatewayValidationException.class)
                        .hasMessageContaining("propertyPath=orderItems." + triple.second + ",")
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
            eq(RequestFlow.FF_CANCEL_MOVEMENT),
            refEq(new CancelMovementRequest(movementId, partner)),
            eq(CLIENT_REQUEST_META),
            eq(TaskResultConsumer.TM))
        ;
    }

    @Test
    public void cancelMovementValidationFailed()  {
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

}
