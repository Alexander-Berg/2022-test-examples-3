package ru.yandex.travel.orders.workflows.orderitem.train;

import java.math.BigDecimal;
import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.repository.TrainInsuranceRefundRepository;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TCancelInsurance;
import ru.yandex.travel.orders.workflow.train.proto.TServiceConfirmedWithoutInsurance;
import ru.yandex.travel.orders.workflows.orderitem.train.handlers.CancellingInsuranceStateHandler;
import ru.yandex.travel.train.model.Insurance;
import ru.yandex.travel.train.model.InsuranceStatus;
import ru.yandex.travel.train.model.TrainPassenger;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class CancellingInsuranceStateHandlerTest {
    private CancellingInsuranceStateHandler handler;
    private TrainInsuranceRefundRepository trainInsuranceRefundRepository;
    private WorkflowRepository workflowRepository;
    private TrainOrderItemFactory factory;

    @Before
    public void setUp() {
        trainInsuranceRefundRepository = mock(TrainInsuranceRefundRepository.class);
        workflowRepository = mock(WorkflowRepository.class);
        handler = new CancellingInsuranceStateHandler(trainInsuranceRefundRepository, workflowRepository);
        factory = new TrainOrderItemFactory();
    }

    @Test
    public void testHandleCancelInsurance() {
        when(trainInsuranceRefundRepository.save(any())).thenAnswer(x -> x.getArguments()[0]);
        when(workflowRepository.save(any())).thenAnswer(x -> x.getArguments()[0]);
        factory.setOrderItemState(EOrderItemState.IS_CONFIRMING);
        TrainOrderItem orderItem = factory.createTrainOrderItem();
        TrainPassenger p1 = createTrainPassengerWithInsurance(900001, ImOperationStatus.FAILED);
        TrainPassenger p2 = createTrainPassengerWithInsurance(900002, ImOperationStatus.IN_PROCESS);
        TrainPassenger p3 = createTrainPassengerWithInsurance(900003, ImOperationStatus.OK);
        orderItem.getPayload().setPassengers(List.of(p1, p2, p3));

        var ctx = testMessagingContext(orderItem);
        handler.handleCancelInsurance(TCancelInsurance.getDefaultInstance(), ctx);

        assertThat(orderItem.getPayload().getInsuranceStatus()).isEqualTo(InsuranceStatus.AUTO_RETURN);
        assertThat(ctx.getState()).isEqualTo(EOrderItemState.IS_CONFIRMED);
        assertThat(ctx.getScheduledEvents().get(1).getMessage()).isInstanceOf(TServiceConfirmedWithoutInsurance.class);

        verify(trainInsuranceRefundRepository).save(argThat(x ->
                x.getPayload().getItems().size() == 2
                        && x.getPayload().getItems().get(0).getBuyOperationId() == 900002
                        && x.getPayload().getItems().get(1).getBuyOperationId() == 900003
        ));
        verify(workflowRepository).save(argThat(x -> x.getSupervisorId() == orderItem.getWorkflow().getId()));
    }

    private TrainPassenger createTrainPassengerWithInsurance(int operationId, ImOperationStatus status) {
        var p1 = factory.createTrainPassenger();
        p1.setInsurance(new Insurance());
        p1.getInsurance().setAmount(Money.of(BigDecimal.valueOf(100), ProtoCurrencyUnit.RUB));
        p1.getInsurance().setPartnerOperationId(operationId);
        p1.getInsurance().setPartnerOperationStatus(status);
        return p1;
    }

    @Test
    public void testHandleCancelInsuranceAllFailed() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_CONFIRMING);
        TrainOrderItem orderItem = factory.createTrainOrderItem();
        TrainPassenger p1 = createTrainPassengerWithInsurance(90001, ImOperationStatus.FAILED);
        TrainPassenger p2 = createTrainPassengerWithInsurance(90002, ImOperationStatus.FAILED);
        orderItem.getPayload().setPassengers(List.of(p1, p2));

        var ctx = testMessagingContext(orderItem);
        handler.handleCancelInsurance(TCancelInsurance.getDefaultInstance(), ctx);

        assertThat(orderItem.getPayload().getInsuranceStatus()).isEqualTo(InsuranceStatus.AUTO_RETURN);
        assertThat(ctx.getState()).isEqualTo(EOrderItemState.IS_CONFIRMED);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceConfirmedWithoutInsurance.class);

        verify(trainInsuranceRefundRepository, times(0)).save(any());
        verify(workflowRepository, times(0)).save(any());
    }
}
