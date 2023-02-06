package ru.yandex.travel.orders.workflows.orderitem.train;

import java.math.BigDecimal;
import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.management.StarTrekService;
import ru.yandex.travel.orders.services.train.ImClientProvider;
import ru.yandex.travel.orders.services.train.TrainMeters;
import ru.yandex.travel.orders.workflow.order.proto.TServiceReserved;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TInsuranceReservationCommit;
import ru.yandex.travel.orders.workflows.orderitem.train.handlers.ReservingInsuranceStateHandler;
import ru.yandex.travel.train.model.Insurance;
import ru.yandex.travel.train.model.InsuranceStatus;
import ru.yandex.travel.train.partners.im.ImClient;
import ru.yandex.travel.train.partners.im.ImClientException;
import ru.yandex.travel.train.partners.im.model.insurance.InsuranceCheckoutResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class ReservingInsuranceStateHandlerTest {

    private ImClient imClient;
    private ImClientProvider imClientProvider;
    private ReservingInsuranceStateHandler handler;
    private StarTrekService starTrekService;
    private TrainWorkflowProperties trainWorkflowProperties;
    private TrainMeters trainMeters;

    @Before
    public void setUp() {
        imClient = mock(ImClient.class);
        imClientProvider = mock(ImClientProvider.class);
        when(imClientProvider.getImClientForOrderItem(any())).thenReturn(imClient);
        starTrekService = mock(StarTrekService.class);
        trainWorkflowProperties = new TrainWorkflowProperties();
        trainWorkflowProperties.setBilling(new TrainWorkflowProperties.BillingProperties());
        trainWorkflowProperties.getBilling().setInsuranceFiscalTitle("Insurance");
        trainWorkflowProperties.getBilling().setPartnerInn("222222222");
        trainMeters = new TrainMeters();
        handler = new ReservingInsuranceStateHandler(imClientProvider, starTrekService, trainWorkflowProperties, trainMeters);
    }

    @Test
    public void testInsuranceReserved() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVING_INSURANCE);
        var passenger1 = factory.createTrainPassenger();
        passenger1.setInsurance(createInsurance());
        var passenger2 = factory.createTrainPassenger();
        passenger2.setInsurance(createInsurance());
        factory.setPassengers(List.of(passenger1, passenger2));
        var trainOrderItem = factory.createTrainOrderItem();
        FiscalItem fiscalItem = new FiscalItem();
        fiscalItem.setMoneyAmount(Money.of(1000, ProtoCurrencyUnit.RUB));
        fiscalItem.setInternalId(0);
        trainOrderItem.addFiscalItem(fiscalItem);
        var response1 = new InsuranceCheckoutResponse();
        response1.setOrderItemId(30000001);
        response1.setAmount(BigDecimal.valueOf(100));
        var response2 = new InsuranceCheckoutResponse();
        response2.setOrderItemId(30000002);
        response2.setAmount(BigDecimal.valueOf(100));
        when(imClient.insuranceCheckout(any())).thenReturn(response1).thenReturn(response2);

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TInsuranceReservationCommit.getDefaultInstance(), ctx);

        verify(imClient, times(2)).insuranceCheckout(any());
        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_RESERVED);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceReserved.class);
        assertThat(trainOrderItem.getPayload().getInsuranceStatus()).isEqualTo(InsuranceStatus.CHECKED_OUT);
        assertThat(passenger1.getInsurance().getPartnerOperationId()).isEqualTo(30000001);
        assertThat(passenger2.getInsurance().getPartnerOperationId()).isEqualTo(30000002);

        verify(starTrekService, never()).createIssueForTrainInsuranceInvalidAmount(
                any(), any(), any(BigDecimal.class), any(BigDecimal.class));
        verify(starTrekService, never()).createIssueForTrainInsuranceNotAdded(any(), any());
    }

    private Insurance createInsurance() {
        var result =  new Insurance();
        result.setProductPackage("Random");
        result.setProvider("IM");
        result.setCompany("Yandex");
        result.setCompensation(Money.of(100500, ProtoCurrencyUnit.RUB));
        result.setAmount(Money.of(100, ProtoCurrencyUnit.RUB));
        return result;
    }

    @Test
    public void testInsuranceReservationImError() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVING_INSURANCE);
        var passenger1 = factory.createTrainPassenger();
        passenger1.setInsurance(createInsurance());
        factory.setPassengers(List.of(passenger1));
        var trainOrderItem = factory.createTrainOrderItem();
        FiscalItem fiscalItem = new FiscalItem();
        fiscalItem.setMoneyAmount(Money.of(1000, ProtoCurrencyUnit.RUB));
        fiscalItem.setInternalId(0);
        trainOrderItem.addFiscalItem(fiscalItem);
        when(imClient.insuranceCheckout(any())).thenThrow(new ImClientException(1, "Fake"));

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TInsuranceReservationCommit.getDefaultInstance(), ctx);

        verify(imClient).insuranceCheckout(any());
        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_RESERVED);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceReserved.class);
        assertThat(trainOrderItem.getPayload().getInsuranceStatus()).isEqualTo(InsuranceStatus.CHECKOUT_FAILED);
        assertThat(passenger1.getInsurance().getPartnerOperationId()).isNull();

        verify(starTrekService).createIssueForTrainInsuranceNotAdded(any(), any());
    }

    @Test
    public void testInsuranceInvalidAmount() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVING_INSURANCE);
        var passenger1 = factory.createTrainPassenger();
        passenger1.setInsurance(createInsurance());
        factory.setPassengers(List.of(passenger1));
        var trainOrderItem = factory.createTrainOrderItem();
        FiscalItem fiscalItem = new FiscalItem();
        fiscalItem.setMoneyAmount(Money.of(1000, ProtoCurrencyUnit.RUB));
        fiscalItem.setInternalId(0);
        trainOrderItem.addFiscalItem(fiscalItem);
        var response1 = new InsuranceCheckoutResponse();
        response1.setOrderItemId(30000001);
        response1.setAmount(BigDecimal.valueOf(101));
        when(imClient.insuranceCheckout(any())).thenReturn(response1);

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TInsuranceReservationCommit.getDefaultInstance(), ctx);

        verify(imClient).insuranceCheckout(any());
        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_RESERVED);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceReserved.class);
        assertThat(trainOrderItem.getPayload().getInsuranceStatus()).isEqualTo(InsuranceStatus.CHECKED_OUT);
        assertThat(passenger1.getInsurance().getPartnerOperationId()).isEqualTo(30000001);

        verify(starTrekService).createIssueForTrainInsuranceInvalidAmount(
                any(), any(), any(BigDecimal.class), any(BigDecimal.class));
    }
}
