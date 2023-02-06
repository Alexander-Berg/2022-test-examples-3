package ru.yandex.travel.orders.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.InvalidProtocolBufferException;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.bus.service.BusesService;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.orders.entities.GenericOrder;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.proto.ERefundPartType;
import ru.yandex.travel.orders.proto.TCalculateRefundReqV2;
import ru.yandex.travel.orders.proto.TRefundCalculation;
import ru.yandex.travel.orders.proto.TRefundPartContext;
import ru.yandex.travel.orders.services.buses.BusesServiceProvider;
import ru.yandex.travel.orders.services.orders.RefundPartsService;
import ru.yandex.travel.orders.services.train.ImClientProvider;
import ru.yandex.travel.orders.workflow.order.generic.proto.TGenericRefundToken;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.train.proto.TTrainRefundPassenger;
import ru.yandex.travel.orders.workflow.train.proto.TTrainRefundToken;
import ru.yandex.travel.orders.workflows.orderitem.bus.BusProperties;
import ru.yandex.travel.orders.workflows.orderitem.expedia.ExpediaProperties;
import ru.yandex.travel.orders.workflows.orderitem.train.TrainWorkflowProperties;
import ru.yandex.travel.train.model.InsuranceStatus;
import ru.yandex.travel.train.partners.im.ImClient;
import ru.yandex.travel.train.partners.im.model.RailwayReturnAmountRequest;
import ru.yandex.travel.train.partners.im.model.RailwayReturnAmountResponse;
import ru.yandex.travel.train.partners.im.model.RailwayReturnBlankResponse;
import ru.yandex.travel.train.partners.im.model.ReturnAmountRequest;
import ru.yandex.travel.train.partners.im.model.ReturnAmountResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RefundCalculationServiceTest {
    private RefundCalculationService service;
    private ImClient imClient;
    private BusesService busesService;

    @Before
    public void setUp() {
        var trainWorkflowProperties = new TrainWorkflowProperties();
        trainWorkflowProperties.setRefund(new TrainWorkflowProperties.RefundProperties());
        trainWorkflowProperties.getRefund().setCalculationExpireTime(Duration.ofMinutes(20));
        trainWorkflowProperties.getRefund().setReturnFeeTime(Duration.ofDays(1));
        var busProperties = new BusProperties();
        busProperties.setRefundCalculationExpireTime(Duration.ofMinutes(20));
        imClient = mock(ImClient.class);
        busesService = mock(BusesService.class);
        ImClientProvider provider = orderItem -> imClient;
        BusesServiceProvider busProvider = orderItem -> busesService;
        service = new RefundCalculationService(new ExpediaProperties(), trainWorkflowProperties, busProperties,
                provider, busProvider, Clock.systemDefaultZone());
    }

    @Test
    public void testTrainCalculateRefundFull() throws InvalidProtocolBufferException {
        GenericOrder order = createTrainOrder(Instant.now().minusSeconds(60), InsuranceStatus.CHECKED_OUT);
        var serviceResult = calculateRefundFull(order);

        assertThat(serviceResult.getOrderId()).isEqualTo(order.getId().toString());
        assertThat(serviceResult.getRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(60 + 36 + 156 - (12 + 12 + 11), ProtoCurrencyUnit.RUB)));
        assertThat(serviceResult.getPenaltyAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(606 - 60 + (12 + 12 + 11), ProtoCurrencyUnit.RUB)));
        TGenericRefundToken genericToken =
                TGenericRefundToken.parseFrom(BaseEncoding.base64Url().decode(serviceResult.getToken()));
        TTrainRefundToken token = genericToken.getService(0).getTrainRefundToken();
        assertThat(token.getPassengerCount()).isEqualTo(3);
        TTrainRefundPassenger refundPassenger2 = token.getPassengerList().stream()
                .filter(p -> p.getBlankId() == 200002).findFirst().orElseThrow();
        assertThat(refundPassenger2.getCustomerId()).isEqualTo(220002);
        assertThat(refundPassenger2.getTicketRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(20, ProtoCurrencyUnit.RUB)));
        assertThat(refundPassenger2.getFeeRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(0, ProtoCurrencyUnit.RUB)));
        assertThat(refundPassenger2.getInsuranceRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(52, ProtoCurrencyUnit.RUB)));
    }

    private TRefundCalculation calculateRefundFull(GenericOrder order) {
        var b1 = new RailwayReturnBlankResponse();
        b1.setPurchaseOrderItemBlankId(200001);
        b1.setAmount(BigDecimal.valueOf(10));
        var b2 = new RailwayReturnBlankResponse();
        b2.setPurchaseOrderItemBlankId(200002);
        b2.setAmount(BigDecimal.valueOf(20));
        var b3 = new RailwayReturnBlankResponse();
        b3.setPurchaseOrderItemBlankId(200003);
        b3.setAmount(BigDecimal.valueOf(30));

        var imRsp1 = new ReturnAmountResponse();
        imRsp1.setServiceReturnResponse(new RailwayReturnAmountResponse());
        imRsp1.getServiceReturnResponse().setBlanks(List.of(b1));
        var imReq1 = new ReturnAmountRequest();
        imReq1.setServiceReturnAmountRequest(new RailwayReturnAmountRequest());
        imReq1.getServiceReturnAmountRequest().setOrderItemId(70000007);
        imReq1.getServiceReturnAmountRequest().setCheckDocumentNumber("1414656565");
        imReq1.getServiceReturnAmountRequest().setOrderItemBlankIds(null);
        when(imClient.getReturnAmount(eq(imReq1))).thenReturn(imRsp1);

        var imRsp2 = new ReturnAmountResponse();
        imRsp2.setServiceReturnResponse(new RailwayReturnAmountResponse());
        imRsp2.getServiceReturnResponse().setBlanks(List.of(b2, b3));
        var imReq2 = new ReturnAmountRequest();
        imReq2.setServiceReturnAmountRequest(new RailwayReturnAmountRequest());
        imReq2.getServiceReturnAmountRequest().setOrderItemId(70000008);
        imReq2.getServiceReturnAmountRequest().setCheckDocumentNumber("1414656522");
        imReq2.getServiceReturnAmountRequest().setOrderItemBlankIds(null);
        when(imClient.getReturnAmount(eq(imReq2))).thenReturn(imRsp2);

        var serviceResult = service.calculateRefundV2(order, TCalculateRefundReqV2.newBuilder()
                .setOrderId(order.getId().toString())
                .addContext(RefundPartsService.partContextToString(TRefundPartContext.newBuilder()
                        .setServiceId(order.getOrderItems().get(0).getId().toString())
                        .setType(ERefundPartType.RPT_SERVICE)
                        .build()))
                .build());

        verify(imClient).getReturnAmount(eq(imReq1));
        verify(imClient).getReturnAmount(eq(imReq2));
        return serviceResult;
    }

    @Test
    public void testTrainCalculateRefundOneTicket() throws InvalidProtocolBufferException {
        GenericOrder order = createTrainOrder(Instant.now().minusSeconds(60), InsuranceStatus.CHECKED_OUT);
        var imRsp = new ReturnAmountResponse();
        imRsp.setServiceReturnResponse(new RailwayReturnAmountResponse());
        var b2 = new RailwayReturnBlankResponse();
        b2.setPurchaseOrderItemBlankId(200002);
        b2.setAmount(BigDecimal.valueOf(20));
        imRsp.getServiceReturnResponse().setBlanks(List.of(b2));
        when(imClient.getReturnAmount(any())).thenReturn(imRsp);
        var imReq = new ReturnAmountRequest();
        imReq.setServiceReturnAmountRequest(new RailwayReturnAmountRequest());
        imReq.getServiceReturnAmountRequest().setOrderItemId(70000008);
        imReq.getServiceReturnAmountRequest().setCheckDocumentNumber("1414656522");
        imReq.getServiceReturnAmountRequest().setOrderItemBlankIds(List.of(200002));

        var serviceResult = service.calculateRefundV2(order, TCalculateRefundReqV2.newBuilder()
                .setOrderId(order.getId().toString())
                .addContext(RefundPartsService.partContextToString(TRefundPartContext.newBuilder()
                        .setServiceId(order.getOrderItems().get(0).getId().toString())
                        .setType(ERefundPartType.RPT_SERVICE_PART)
                        .setTrainTicketPartContext(ru.yandex.travel.orders.workflow.orderitem.train.ticketrefund.proto.TRefundPartContext.newBuilder()
                                .setBlankId(200002).setCustomerId(220002).build())
                        .build()))
                .build());

        verify(imClient).getReturnAmount(eq(imReq));
        assertThat(serviceResult.getOrderId()).isEqualTo(order.getId().toString());
        assertThat(serviceResult.getRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(20 + 12 + 52 - (6 + 6), ProtoCurrencyUnit.RUB)));
        assertThat(serviceResult.getPenaltyAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(202 - 20 + (6 + 6), ProtoCurrencyUnit.RUB)));
        TGenericRefundToken genericToken =
                TGenericRefundToken.parseFrom(BaseEncoding.base64Url().decode(serviceResult.getToken()));
        TTrainRefundToken token = genericToken.getService(0).getTrainRefundToken();
        assertThat(token.getPassengerCount()).isEqualTo(1);
        assertThat(token.getPassenger(0).getBlankId()).isEqualTo(200002);
        assertThat(token.getPassenger(0).getCustomerId()).isEqualTo(220002);
        assertThat(token.getPassenger(0).getTicketRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(20, ProtoCurrencyUnit.RUB)));
        assertThat(token.getPassenger(0).getFeeRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(12 - (6 + 6), ProtoCurrencyUnit.RUB)));
        assertThat(token.getPassenger(0).getInsuranceRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(52, ProtoCurrencyUnit.RUB)));
    }

    @Test
    public void testTrainCalculateRefundWithoutFeeAndInsurance() throws InvalidProtocolBufferException {
        GenericOrder order = createTrainOrder(Instant.now().minus(99, ChronoUnit.DAYS),
                InsuranceStatus.CHECKOUT_FAILED);

        var serviceResult = calculateRefundFull(order);

        assertThat(serviceResult.getOrderId()).isEqualTo(order.getId().toString());
        assertThat(serviceResult.getRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(60, ProtoCurrencyUnit.RUB)));
        assertThat(serviceResult.getPenaltyAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(606 + 36 - 60, ProtoCurrencyUnit.RUB)));

        TGenericRefundToken genericToken =
                TGenericRefundToken.parseFrom(BaseEncoding.base64Url().decode(serviceResult.getToken()));
        TTrainRefundToken token = genericToken.getService(0).getTrainRefundToken();
        assertThat(token.getPassengerCount()).isEqualTo(3);
        TTrainRefundPassenger refundPassenger2 = token.getPassengerList().stream()
                .filter(p -> p.getBlankId() == 200002).findFirst().orElseThrow();
        assertThat(refundPassenger2.getCustomerId()).isEqualTo(220002);
        assertThat(refundPassenger2.getTicketRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(20, ProtoCurrencyUnit.RUB)));
        assertThat(refundPassenger2.getFeeRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(0, ProtoCurrencyUnit.RUB)));
        assertThat(refundPassenger2.getInsuranceRefundAmount()).isEqualTo(
                ProtoUtils.toTPrice(Money.of(0, ProtoCurrencyUnit.RUB)));
    }

    private GenericOrder createTrainOrder(Instant confirmedAt, InsuranceStatus insuranceStatus) {
        GenericOrder order = new GenericOrder();
        order.setCurrency(ProtoCurrencyUnit.RUB);
        order.setId(UUID.randomUUID());
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_CONFIRMED);
        var orderItem = factory.createTrainOrderItem();
        order.addOrderItem(orderItem);
//        orderItem.getPayload().setPartnerBuyOperationId(70000007);
        orderItem.getPayload().setInsuranceStatus(insuranceStatus);
        orderItem.setConfirmedAt(confirmedAt);

        var p1 = factory.createTrainPassenger();
        p1.setDocumentNumber("1414656565");
        p1.getTicket().setBlankId(200001);
        p1.setCustomerId(220001);
        p1.getTicket().setPartnerBuyOperationId(70000007);
        p1.getTicket().setTariffAmount(Money.of(101, ProtoCurrencyUnit.RUB));
        p1.getTicket().setServiceAmount(Money.of(100, ProtoCurrencyUnit.RUB));
        p1.getTicket().setFeeAmount(Money.of(11, ProtoCurrencyUnit.RUB));
        p1.getTicket().setPartnerFee(Money.of(6, ProtoCurrencyUnit.RUB));
        p1.getTicket().setPartnerRefundFee(Money.of(6, ProtoCurrencyUnit.RUB));
        p1.getInsurance().setAmount(Money.of(51, ProtoCurrencyUnit.RUB));

        var p2 = factory.createTrainPassenger();
        p2.setDocumentNumber("1414656522");
        p2.getTicket().setBlankId(200002);
        p2.setCustomerId(220002);
        p2.getTicket().setPartnerBuyOperationId(70000008);
        p2.getTicket().setTariffAmount(Money.of(102, ProtoCurrencyUnit.RUB));
        p2.getTicket().setServiceAmount(Money.of(100, ProtoCurrencyUnit.RUB));
        p2.getTicket().setFeeAmount(Money.of(12, ProtoCurrencyUnit.RUB));
        p2.getTicket().setPartnerFee(Money.of(6, ProtoCurrencyUnit.RUB));
        p2.getTicket().setPartnerRefundFee(Money.of(6, ProtoCurrencyUnit.RUB));
        p2.getInsurance().setAmount(Money.of(52, ProtoCurrencyUnit.RUB));

        var p3 = factory.createTrainPassenger();
        p3.getTicket().setBlankId(200003);
        p3.setCustomerId(220003);
        p3.getTicket().setPartnerBuyOperationId(70000008);
        p3.getTicket().setTariffAmount(Money.of(103, ProtoCurrencyUnit.RUB));
        p3.getTicket().setServiceAmount(Money.of(100, ProtoCurrencyUnit.RUB));
        p3.getTicket().setFeeAmount(Money.of(13, ProtoCurrencyUnit.RUB));
        p3.getTicket().setPartnerFee(Money.of(6, ProtoCurrencyUnit.RUB));
        p3.getTicket().setPartnerRefundFee(Money.of(6, ProtoCurrencyUnit.RUB));
        p3.getInsurance().setAmount(Money.of(53, ProtoCurrencyUnit.RUB));

        orderItem.getPayload().setPassengers(List.of(p1, p2, p3));
        return order;
    }
}
