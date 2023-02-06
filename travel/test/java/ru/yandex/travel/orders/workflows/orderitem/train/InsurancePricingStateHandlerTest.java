package ru.yandex.travel.orders.workflows.orderitem.train;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.services.train.ImClientProvider;
import ru.yandex.travel.orders.services.train.TrainMeters;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TFeeCalculationCommit;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TInsurancePricingCommit;
import ru.yandex.travel.orders.workflows.orderitem.train.handlers.InsurancePricingStateHandler;
import ru.yandex.travel.train.model.InsuranceStatus;
import ru.yandex.travel.train.partners.im.ImClient;
import ru.yandex.travel.train.partners.im.ImClientException;
import ru.yandex.travel.train.partners.im.model.insurance.InsurancePricingResponse;
import ru.yandex.travel.train.partners.im.model.insurance.RailwayTravelPricingResult;
import ru.yandex.travel.train.partners.im.model.insurance.RailwayTravelProductPricingInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class InsurancePricingStateHandlerTest {

    private ImClient imClient;
    private ImClientProvider imClientProvider;
    private InsurancePricingStateHandler handler;
    private TrainMeters trainMeters;

    @Before
    public void setUp() {
        imClient = mock(ImClient.class);
        imClientProvider = mock(ImClientProvider.class);
        trainMeters = new TrainMeters();
        when(imClientProvider.getImClientForOrderItem(any())).thenReturn(imClient);
        handler = new InsurancePricingStateHandler(imClientProvider, trainMeters);
    }

    @Test
    public void testPricingSuccess() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVED);
        var passenger1 = factory.createTrainPassenger();
        passenger1.setCustomerId(2001);
        var passenger2 = factory.createTrainPassenger();
        passenger2.setCustomerId(2002);
        factory.setPassengers(List.of(passenger1, passenger2));
        var trainOrderItem = factory.createTrainOrderItem();
        InsurancePricingResponse response = createInsurancePricingResponse();
        when(imClient.insurancePricing(any())).thenReturn(response);

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TInsurancePricingCommit.getDefaultInstance(), ctx);

        verify(imClient).insurancePricing(any());
        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CALCULATING_FEE_TRAINS);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TFeeCalculationCommit.class);
        assertThat(trainOrderItem.getPayload().getInsuranceStatus()).isEqualTo(InsuranceStatus.PRICED);
        assertThat(passenger1.getInsurance().getAmount()).isEqualTo(Money.of(100, ProtoCurrencyUnit.RUB));
        assertThat(passenger2.getInsurance().getAmount()).isEqualTo(Money.of(200, ProtoCurrencyUnit.RUB));
    }

    private InsurancePricingResponse createInsurancePricingResponse() {
        var response = new InsurancePricingResponse();
        response.setPricingResult(new RailwayTravelPricingResult());
        List<RailwayTravelProductPricingInfo> productPricingInfoList = new ArrayList<>();
        response.getPricingResult().setProductPricingInfoList(productPricingInfoList);

        var product = new RailwayTravelProductPricingInfo();
        productPricingInfoList.add(product);
        product.setAmount(BigDecimal.valueOf(100));
        product.setOrderCustomerId(2001);
        product.setCompensation(BigDecimal.valueOf(101));

        product = new RailwayTravelProductPricingInfo();
        productPricingInfoList.add(product);
        product.setAmount(BigDecimal.valueOf(200));
        product.setOrderCustomerId(2002);
        product.setCompensation(BigDecimal.valueOf(201));
        return response;
    }

    @Test
    public void testPricingError() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVED);
        var passenger1 = factory.createTrainPassenger();
        passenger1.setCustomerId(2001);
        passenger1.setInsurance(null);
        factory.setPassengers(List.of(passenger1));
        var trainOrderItem = factory.createTrainOrderItem();
        when(imClient.insurancePricing(any())).thenThrow(new ImClientException(1, "Fake"));

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TInsurancePricingCommit.getDefaultInstance(), ctx);

        verify(imClient).insurancePricing(any());
        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CALCULATING_FEE_TRAINS);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TFeeCalculationCommit.class);
        assertThat(trainOrderItem.getPayload().getInsuranceStatus()).isEqualTo(InsuranceStatus.PRICING_FAILED);
        assertThat(passenger1.getInsurance()).isNull();
    }
}
