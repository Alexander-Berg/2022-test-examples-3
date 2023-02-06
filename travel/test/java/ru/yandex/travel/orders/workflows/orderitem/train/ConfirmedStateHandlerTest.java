package ru.yandex.travel.orders.workflows.orderitem.train;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.repository.OrderRefundRepository;
import ru.yandex.travel.orders.repository.TrainInsuranceRefundRepository;
import ru.yandex.travel.orders.repository.TrainTicketRefundRepository;
import ru.yandex.travel.orders.services.train.ImClientProvider;
import ru.yandex.travel.orders.services.train.TrainDiscountService;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TChangeRegistrationStatus;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TOfficeRefundOccured;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TPartnerInfoUpdated;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TUpdateTickets;
import ru.yandex.travel.orders.workflow.train.proto.TServiceOfficeRefunded;
import ru.yandex.travel.orders.workflows.orderitem.train.handlers.ConfirmedStateHandler;
import ru.yandex.travel.train.model.InsuranceStatus;
import ru.yandex.travel.train.model.TrainTicketRefundStatus;
import ru.yandex.travel.train.partners.im.ImClient;
import ru.yandex.travel.train.partners.im.model.ElectronicRegistrationResponse;
import ru.yandex.travel.train.partners.im.model.ImBlankStatus;
import ru.yandex.travel.train.partners.im.model.PendingElectronicRegistration;
import ru.yandex.travel.train.partners.im.model.UpdateBlanksResponse;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;
import static ru.yandex.travel.orders.workflows.orderitem.train.HandlerTestHelper.createOrderInfoResponse;

@SuppressWarnings("FieldCanBeLocal")
public class ConfirmedStateHandlerTest {
    private final int blankId = 1000001;
    private ConfirmedStateHandler handler;
    private TrainOrderItemFactory orderItemFactory;
    private TrainTicketRefundRepository trainTicketRefundRepository;
    private WorkflowRepository workflowRepository;
    private OrderRefundRepository orderRefundRepository;
    private ImClient imClient;
    private ImClientProvider imClientProvider;
    private TrainWorkflowProperties trainWorkflowProperties;
    private TrainInsuranceRefundRepository trainInsuranceRefundRepository;

    @Before
    public void setUp() {
        trainTicketRefundRepository = mock(TrainTicketRefundRepository.class);
        workflowRepository = mock(WorkflowRepository.class);
        orderRefundRepository = mock(OrderRefundRepository.class);
        imClient = mock(ImClient.class);
        imClientProvider = mock(ImClientProvider.class);
        trainInsuranceRefundRepository = mock(TrainInsuranceRefundRepository.class);
        when(imClientProvider.getImClientForOrderItem(any())).thenReturn(imClient);
        trainWorkflowProperties = new TrainWorkflowProperties();
        trainWorkflowProperties.setRefund(new TrainWorkflowProperties.RefundProperties());
        trainWorkflowProperties.getRefund().setReturnFeeTime(Duration.ofHours(24));
        handler = new ConfirmedStateHandler(trainTicketRefundRepository, workflowRepository, orderRefundRepository,
                imClientProvider, trainWorkflowProperties, mock(TrainDiscountService.class),
                trainInsuranceRefundRepository);
        orderItemFactory = new TrainOrderItemFactory();
        orderItemFactory.setOrderItemState(EOrderItemState.IS_CONFIRMED);
        orderItemFactory.setBlankId(blankId);
    }

    @Test
    public void testHandlePartnerInfoUpdated() {
        var orderInfoResponse = createOrderInfoResponse(ImOperationStatus.OK, blankId);
        var orderItem = orderItemFactory.createTrainOrderItem();
        var ctx = testMessagingContext(orderItem);
        var ticket = orderItem.getPayload().getPassengers().get(0).getTicket();
        ticket.setImBlankStatus(null);
        ticket.setPendingElectronicRegistration(false);
        var blank = orderInfoResponse.findBuyRailwayItems().get(0).getIdToBlankMap().get(blankId);
        blank.setBlankStatus(ImBlankStatus.REFUNDED);
        blank.setPendingElectronicRegistration(PendingElectronicRegistration.TO_CANCEL);

        var event = TPartnerInfoUpdated.newBuilder().setPayload(ProtoUtils.toTJson(orderInfoResponse)).build();
        handler.handleEvent(event, ctx);

        assertThat(ticket.getImBlankStatus()).isEqualByComparingTo(ImBlankStatus.REFUNDED);
        assertThat(ticket.isPendingElectronicRegistration()).isEqualTo(true);
    }

    @Test
    public void testHandleRefreshServiceInfo() {
        var orderInfoResponse = createOrderInfoResponse(ImOperationStatus.OK, blankId);
        var orderItem = orderItemFactory.createTrainOrderItem();
        var ctx = testMessagingContext(orderItem);
        var ticket = orderItem.getPayload().getPassengers().get(0).getTicket();
        ticket.setImBlankStatus(null);
        ticket.setPendingElectronicRegistration(false);
        var blank = orderInfoResponse.findBuyRailwayItems().get(0).getIdToBlankMap().get(blankId);
        blank.setBlankStatus(ImBlankStatus.REFUNDED);
        blank.setPendingElectronicRegistration(PendingElectronicRegistration.TO_CANCEL);

        when(imClient.updateBlanks(orderItem.getPayload().getPartnerBuyOperationIds().get(0))).thenReturn(new UpdateBlanksResponse());
        when(imClient.orderInfo(orderItem.getPayload().getPartnerOrderId())).thenReturn(orderInfoResponse);
        var event = TUpdateTickets.newBuilder().build();
        handler.handleEvent(event, ctx);

        assertThat(ticket.getImBlankStatus()).isEqualByComparingTo(ImBlankStatus.REFUNDED);
        assertThat(ticket.isPendingElectronicRegistration()).isEqualTo(true);
    }

    @Test
    public void testHandleChangeRegistrationStatus() {
        var orderItem = orderItemFactory.createTrainOrderItem();
        var ctx = testMessagingContext(orderItem);
        var ticket = orderItem.getPayload().getPassengers().get(0).getTicket();
        var electronicRegistrationResponse = new ElectronicRegistrationResponse();

        ticket.setImBlankStatus(ImBlankStatus.REMOTE_CHECK_IN);
        ticket.setPendingElectronicRegistration(false);
        electronicRegistrationResponse.setExpirationElectronicRegistrationDateTime(LocalDateTime.now());

        when(imClient.changeElectronicRegistration(any())).thenReturn(electronicRegistrationResponse);

        var eventToSuccess = TChangeRegistrationStatus.newBuilder()
                .setEnabled(false).addBlankIds(ticket.getBlankId()).build();
        handler.handleEvent(eventToSuccess, ctx);

        assertThat(ticket.getImBlankStatus()).isEqualTo(ImBlankStatus.NO_REMOTE_CHECK_IN);
        assertThat(ticket.isPendingElectronicRegistration()).isEqualTo(false);

        electronicRegistrationResponse.setExpirationElectronicRegistrationDateTime(null);

        var eventToFail = TChangeRegistrationStatus.newBuilder()
                .setEnabled(true).addBlankIds(ticket.getBlankId()).build();
        handler.handleEvent(eventToFail, ctx);

        assertThat(ticket.getImBlankStatus()).isEqualTo(ImBlankStatus.NO_REMOTE_CHECK_IN);
        assertThat(ticket.isPendingElectronicRegistration()).isEqualTo(true);
    }

    @Test
    public void testHandleOfficeRefund() {
        var orderItem = orderItemFactory.createTrainOrderItem();
        var refundOperationId = 99999123;
        var refundInsuranceOperationId = 99999124;
        orderItem.getPayload().setInsuranceStatus(InsuranceStatus.CHECKED_OUT);
        orderItem.setOrder(new TrainOrder());
        orderItem.getOrder().setCurrency(ProtoCurrencyUnit.RUB);
        var confirmedAt = LocalDateTime.of(2019, 12, 19, 17, 0, 0);
        orderItem.setConfirmedAt(ImHelpers.fromLocalDateTime(confirmedAt, ImHelpers.MSK_TZ));
        orderItemFactory.fillFiscalItems(orderItem);
        var orderInfoResponse = createOrderInfoResponse(ImOperationStatus.OK, blankId);
        var orderInsurance = orderItem.getPayload().getPassengers().get(0).getInsurance();
        var insuranceBuyOperation = orderInsurance.getPartnerOperationId();
        HandlerTestHelper.addInsuranceBuyOperation(orderInfoResponse, insuranceBuyOperation);
        HandlerTestHelper.addInsuranceRefundOperation(orderInfoResponse, insuranceBuyOperation,
                refundInsuranceOperationId);
        HandlerTestHelper.addTicketRefundOperation(orderInfoResponse, blankId, refundOperationId,
                confirmedAt.plusHours(1));
        when(imClient.orderInfo(orderItem.getPayload().getPartnerOrderId())).thenReturn(orderInfoResponse);

        var ctx = testMessagingContext(orderItem);
        handler.handleEvent(
                TOfficeRefundOccured.newBuilder()
                        .addAllRefundOperationIds(List.of(refundOperationId, refundInsuranceOperationId))
                        .build(),
                ctx
        );

        verify(trainTicketRefundRepository).save(argThat(x -> {
            var refundItem = x.getPayload().getItems().get(0);
            return refundItem.getBlankId() == blankId &&
                    refundItem.getRefundOperationId() == refundOperationId &&
                    refundItem.getActualRefundTicketAmount().isEqualTo(Money.of(1555, ProtoCurrencyUnit.RUB)) &&
                    refundItem.getCalculatedRefundFeeAmount().isEqualTo(Money.of(233.3, ProtoCurrencyUnit.RUB)) &&
                    refundItem.getCalculatedRefundInsuranceAmount().isEqualTo(orderInsurance.getAmount());
        }));
        assertThat(orderItem.getState()).isEqualTo(EOrderItemState.IS_REFUNDED);
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket().getRefundStatus()).isEqualTo(TrainTicketRefundStatus.REFUNDED);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceOfficeRefunded.class);
    }

    @Test
    public void testHandleOfficeRefundTicketOnly() {
        var orderItem = orderItemFactory.createTrainOrderItem();
        var refundOperationId = 99999123;
        var refundInsuranceOperationId = 99999124;
        orderItem.getPayload().setInsuranceStatus(InsuranceStatus.AUTO_RETURN);
        orderItem.setOrder(new TrainOrder());
        orderItem.getOrder().setCurrency(ProtoCurrencyUnit.RUB);
        orderItemFactory.fillFiscalItems(orderItem);
        var confirmedAt = LocalDateTime.of(2019, 12, 19, 17, 0, 0);
        orderItem.setConfirmedAt(ImHelpers.fromLocalDateTime(confirmedAt, ImHelpers.MSK_TZ));
        var orderInfoResponse = createOrderInfoResponse(ImOperationStatus.OK, blankId);
        var orderInsurance = orderItem.getPayload().getPassengers().get(0).getInsurance();
        var insuranceBuyOperation = orderInsurance.getPartnerOperationId();
        HandlerTestHelper.addInsuranceBuyOperation(orderInfoResponse, insuranceBuyOperation);
        HandlerTestHelper.addInsuranceRefundOperation(orderInfoResponse, insuranceBuyOperation,
                refundInsuranceOperationId);
        HandlerTestHelper.addTicketRefundOperation(orderInfoResponse, blankId, refundOperationId,
                confirmedAt.plusHours(25));
        when(imClient.orderInfo(orderItem.getPayload().getPartnerOrderId())).thenReturn(orderInfoResponse);

        var ctx = testMessagingContext(orderItem);
        handler.handleEvent(TOfficeRefundOccured.newBuilder().addRefundOperationIds(refundOperationId).build(), ctx);

        verify(trainTicketRefundRepository).save(argThat(x -> {
            var refundItem = x.getPayload().getItems().get(0);
            return refundItem.getBlankId() == blankId &&
                    refundItem.getRefundOperationId() == refundOperationId &&
                    refundItem.getActualRefundTicketAmount().isEqualTo(Money.of(1555, ProtoCurrencyUnit.RUB)) &&
                    Money.zero(ProtoCurrencyUnit.RUB).equals(refundItem.getCalculatedRefundTicketAmount()) &&
                    Money.zero(ProtoCurrencyUnit.RUB).equals(refundItem.getCalculatedRefundFeeAmount()) &&
                    Money.zero(ProtoCurrencyUnit.RUB).equals(refundItem.getCalculatedRefundInsuranceAmount());
        }));
        assertThat(orderItem.getState()).isEqualTo(EOrderItemState.IS_REFUNDED);
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket().getRefundStatus()).isEqualTo(TrainTicketRefundStatus.REFUNDED);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceOfficeRefunded.class);
    }
}
