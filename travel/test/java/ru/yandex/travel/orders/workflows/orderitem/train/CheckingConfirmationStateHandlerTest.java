package ru.yandex.travel.orders.workflows.orderitem.train;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.management.StarTrekService;
import ru.yandex.travel.orders.services.train.TrainDiscountService;
import ru.yandex.travel.orders.workflow.order.proto.TServiceCancelled;
import ru.yandex.travel.orders.workflow.order.proto.TServiceConfirmed;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.EErrorCode;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TCancelInsurance;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TConfirmationFailed;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TConfirmationSuccess;
import ru.yandex.travel.orders.workflows.orderitem.train.handlers.CheckingConfirmationStateHandler;
import ru.yandex.travel.train.model.ErrorCode;
import ru.yandex.travel.train.model.Insurance;
import ru.yandex.travel.train.model.InsuranceStatus;
import ru.yandex.travel.train.model.TrainPassenger;
import ru.yandex.travel.train.partners.im.model.ImBlankStatus;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationType;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOrderItemType;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderItemResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;
import static ru.yandex.travel.orders.workflows.orderitem.train.HandlerTestHelper.createOrderInfoResponse;

@SuppressWarnings("FieldCanBeLocal")
public class CheckingConfirmationStateHandlerTest {
    private final int blankId = 12345;
    private final int insuranceId = 77777;

    private StarTrekService starTrekService;
    private CheckingConfirmationStateHandler handler;
    private TrainOrderItemFactory orderItemFactory;
    private TrainDiscountService trainDiscountService;

    @Before
    public void setUp() {
        starTrekService = mock(StarTrekService.class);
        trainDiscountService = mock(TrainDiscountService.class);
        handler = new CheckingConfirmationStateHandler(starTrekService, trainDiscountService);

        orderItemFactory = new TrainOrderItemFactory();
        orderItemFactory.setOrderItemState(EOrderItemState.IS_CONFIRMING);
        orderItemFactory.setBlankId(blankId);
    }

    @Test
    public void testHandleConfirmationDoneSuccessfully() {
        var orderInfoResponse = createOrderInfoResponse(ImOperationStatus.OK, blankId);
        TrainOrderItem orderItem = orderItemFactory.createTrainOrderItem();
        orderItem.getPayload().getPassengers().get(0).getTicket().setBlankId(blankId);

        var ctx = testMessagingContext(orderItem);

        handler.handleEvent(TConfirmationSuccess.newBuilder()
                .setPartnerResult(ProtoUtils.toTJson(orderInfoResponse)).build(), ctx);

        assertThat(orderItem.getItemState()).isEqualTo(EOrderItemState.IS_CONFIRMED);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceConfirmed.class);

        assertThat(orderItem.getPayload().getReservationNumber()).isNotNull();
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket()
                .getCanChangeElectronicRegistrationTill())
                .isEqualTo(Instant.from(ZonedDateTime.of(2019, 7, 30, 9, 0, 0, 0, ZoneId.of("UTC"))));
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket()
                .getCanReturnTill())
                .isEqualTo(Instant.from(ZonedDateTime.of(2019, 7, 30, 8, 0, 0, 0, ZoneId.of("UTC"))));
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket().getImBlankStatus()).isEqualTo(
                ImBlankStatus.REMOTE_CHECK_IN);

        verify(starTrekService, never()).createIssueForTrainInsuranceNotConfirmed(any(), any());
    }

    @Test
    public void testHandleConfirmationInsuranceFailed() {
        var orderInfoResponse = createOrderInfoResponse(ImOperationStatus.OK, blankId);
        OrderItemResponse buyInsuranceItem = new OrderItemResponse();
        buyInsuranceItem.setOrderItemId(insuranceId);
        buyInsuranceItem.setOperationType(ImOperationType.BUY);
        buyInsuranceItem.setType(ImOrderItemType.INSURANCE);
        buyInsuranceItem.setSimpleOperationStatus(ImOperationStatus.FAILED);
        buyInsuranceItem.setOrderItemBlanks(new ArrayList<>());
        orderInfoResponse.getOrderItems().add(buyInsuranceItem);

        TrainOrderItem orderItem = orderItemFactory.createTrainOrderItem();
        orderItem.getPayload().setInsuranceStatus(InsuranceStatus.CHECKED_OUT);
        TrainPassenger p1 = orderItem.getPayload().getPassengers().get(0);
        p1.getTicket().setBlankId(blankId);
        p1.setInsurance(new Insurance());
        p1.getInsurance().setAmount(Money.of(BigDecimal.valueOf(100), ProtoCurrencyUnit.RUB));
        p1.getInsurance().setPartnerOperationId(insuranceId);

        var ctx = testMessagingContext(orderItem);

        handler.handleEvent(TConfirmationSuccess.newBuilder()
                .setPartnerResult(ProtoUtils.toTJson(orderInfoResponse)).build(), ctx);

        assertThat(orderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLING_INSURANCE);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TCancelInsurance.class);
        assertThat(p1.getInsurance().getPartnerOperationStatus()).isEqualTo(ImOperationStatus.FAILED);

        verify(starTrekService, never()).createIssueForTrainInsuranceNotConfirmed(any(), any());
    }

    @Test
    public void testHandleConfirmationInsurancePending() {
        var orderInfoResponse = createOrderInfoResponse(ImOperationStatus.OK, blankId);
        OrderItemResponse buyInsuranceItem = new OrderItemResponse();
        buyInsuranceItem.setOrderItemId(insuranceId);
        buyInsuranceItem.setOperationType(ImOperationType.BUY);
        buyInsuranceItem.setType(ImOrderItemType.INSURANCE);
        buyInsuranceItem.setSimpleOperationStatus(ImOperationStatus.IN_PROCESS);
        buyInsuranceItem.setOrderItemBlanks(new ArrayList<>());
        orderInfoResponse.getOrderItems().add(buyInsuranceItem);

        TrainOrderItem orderItem = orderItemFactory.createTrainOrderItem();
        orderItem.getPayload().setInsuranceStatus(InsuranceStatus.CHECKED_OUT);
        TrainPassenger p1 = orderItem.getPayload().getPassengers().get(0);
        p1.getTicket().setBlankId(blankId);
        p1.setInsurance(new Insurance());
        p1.getInsurance().setAmount(Money.of(BigDecimal.valueOf(100), ProtoCurrencyUnit.RUB));
        p1.getInsurance().setPartnerOperationId(insuranceId);

        var ctx = testMessagingContext(orderItem);

        handler.handleEvent(TConfirmationSuccess.newBuilder()
                .setPartnerResult(ProtoUtils.toTJson(orderInfoResponse)).build(), ctx);

        assertThat(orderItem.getItemState()).isEqualTo(EOrderItemState.IS_CONFIRMED);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceConfirmed.class);

        verify(starTrekService).createIssueForTrainInsuranceNotConfirmed(any(), any());
    }

    @Test
    public void testHandleConfirmationFailed() {
        var orderInfoResponse = createOrderInfoResponse(ImOperationStatus.FAILED, blankId);
        TrainOrderItem orderItem = orderItemFactory.createTrainOrderItem();
        orderItem.getPayload().getPassengers().get(0).getTicket().setBlankId(blankId);

        var ctx = testMessagingContext(orderItem);

        handler.handleEvent(TConfirmationFailed.newBuilder()
                .setPartnerResult(ProtoUtils.toTJson(orderInfoResponse))
                .setErrorCode(EErrorCode.EM_TRY_LATER)
                .build(), ctx);

        assertThat(orderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLED);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceCancelled.class);

        assertThat(orderItem.getPayload().getReservationNumber()).isNull();
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket()
                .getCanChangeElectronicRegistrationTill()).isNull();
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket().getImBlankStatus()).isEqualTo(
                ImBlankStatus.CANCELLED);
        assertThat(orderItem.getPayload().getErrorInfo().getCode()).isEqualTo(ErrorCode.TRY_LATER);
    }

    @Test
    public void testHandleConfirmationPending() {
        var orderInfoResponse = createOrderInfoResponse(ImOperationStatus.IN_PROCESS, blankId);
        TrainOrderItem orderItem = orderItemFactory.createTrainOrderItem();
        orderItem.getPayload().getPassengers().get(0).getTicket().setBlankId(blankId);

        var ctx = testMessagingContext(orderItem);

        handler.handleEvent(TConfirmationFailed.newBuilder()
                .setPartnerResult(ProtoUtils.toTJson(orderInfoResponse))
                .build(), ctx);

        assertThat(orderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLED);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceCancelled.class);
        assertThat(orderItem.getPayload().getReservationNumber()).isNull();
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket()
                .getCanChangeElectronicRegistrationTill()).isNull();
    }
}
