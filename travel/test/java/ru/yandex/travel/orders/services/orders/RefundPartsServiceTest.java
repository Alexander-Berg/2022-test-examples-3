package ru.yandex.travel.orders.services.orders;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.entities.GenericOrder;
import ru.yandex.travel.orders.entities.GenericOrderUserRefund;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.entities.TrainTicketRefund;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.proto.EOrderRefundState;
import ru.yandex.travel.orders.proto.ERefundPartState;
import ru.yandex.travel.orders.proto.TRefundPartInfo;
import ru.yandex.travel.orders.workflow.order.generic.proto.EOrderState;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.ticketrefund.proto.ETrainTicketRefundState;
import ru.yandex.travel.testing.time.SettableClock;
import ru.yandex.travel.train.model.InsuranceStatus;
import ru.yandex.travel.train.model.TrainPassenger;
import ru.yandex.travel.train.model.TrainTicketRefundStatus;
import ru.yandex.travel.train.model.refund.PassengerRefundInfo;
import ru.yandex.travel.train.partners.im.model.ImBlankStatus;
import ru.yandex.travel.train.partners.im.model.orderinfo.BoardingSystemType;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;
import ru.yandex.travel.train.service.TrainPartKeyService;

import static org.assertj.core.api.Assertions.assertThat;

public class RefundPartsServiceTest {
    private SettableClock settableClock;
    private RefundPartsService refundPartsService;

    @Before
    public void init() {
        settableClock = new SettableClock();
        refundPartsService = new RefundPartsService(settableClock);
    }

    @Test
    public void testAllRefunded() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_REFUNDED);
        var order = new GenericOrder();
        order.setId(UUID.randomUUID());
        order.setState(EOrderState.OS_REFUNDED);
        order.setCurrentInvoice(TrustInvoice.createEmptyInvoice());
        var orderItem = factory.createTrainOrderItem();
        orderItem.getPayload().getPassengers().get(0).getTicket().setRefundStatus(TrainTicketRefundStatus.REFUNDED);
        order.addOrderItem(orderItem);
        var orderItem2 = factory.createTrainOrderItem();
        orderItem2.getPayload().getPassengers().get(0).getTicket().setRefundStatus(TrainTicketRefundStatus.REFUNDED);
        order.addOrderItem(orderItem2);
        var orderRefund = GenericOrderUserRefund.createForOrder(order);
        orderRefund.setState(EOrderRefundState.RS_REFUNDED);
        var refundItem1 = createPassengerRefundInfo(orderItem);
        var ticketRefund1 = TrainTicketRefund.createRefund(orderItem, List.of(refundItem1), orderRefund);
        var refundItem2 = createPassengerRefundInfo(orderItem2);
        var ticketRefund2 = TrainTicketRefund.createRefund(orderItem2, List.of(refundItem2), orderRefund);
        orderRefund.setTrainTicketRefunds(List.of(ticketRefund1, ticketRefund2));

        List<TRefundPartInfo> refundParts = refundPartsService.getRefundParts(order, null);
        assertThat(refundParts).isNotNull().isNotEmpty();
        assertThat(refundParts.stream().allMatch(x -> x.getState() == ERefundPartState.RPS_REFUNDED)).isTrue();

        List<TRefundPartInfo> refundedParts = refundParts.stream().filter(TRefundPartInfo::hasRefund)
                .collect(Collectors.toList());
        assertThat(refundedParts.size()).isEqualTo(2);
        assertThat(refundedParts.stream().allMatch(x -> x.getState() == ERefundPartState.RPS_REFUNDED &&
                x.getRefund().getState() == EOrderRefundState.RS_REFUNDED)).isTrue();
    }

    @Test
    public void testLastTicketRefundFailed() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_CONFIRMED);
        var order = new GenericOrder();
        order.setId(UUID.randomUUID());
        order.setState(EOrderState.OS_CONFIRMED);
        order.setCurrentInvoice(TrustInvoice.createEmptyInvoice());
        var orderItem = factory.createTrainOrderItem();
        orderItem.setState(EOrderItemState.IS_REFUNDED);
        orderItem.getPayload().getPassengers().get(0).getTicket().setRefundStatus(TrainTicketRefundStatus.REFUNDED);
        order.addOrderItem(orderItem);
        var orderItem2 = factory.createTrainOrderItem();
        order.addOrderItem(orderItem2);
        var orderRefund = GenericOrderUserRefund.createForOrder(order);
        orderRefund.setState(EOrderRefundState.RS_REFUNDED);
        orderRefund.setCreatedAt(Instant.now().minus(Duration.ofDays(1)));
        var refundItem1 = createPassengerRefundInfo(orderItem);
        var ticketRefund1 = TrainTicketRefund.createRefund(orderItem, List.of(refundItem1), orderRefund);
        orderRefund.setTrainTicketRefunds(List.of(ticketRefund1));
        var orderRefund2 = GenericOrderUserRefund.createForOrder(order);
        orderRefund2.setState(EOrderRefundState.RS_FAILED);
        orderRefund2.setCreatedAt(Instant.now());
        var refundItem2 = createPassengerRefundInfo(orderItem2);
        var ticketRefund2 = TrainTicketRefund.createRefund(orderItem2, List.of(refundItem2), orderRefund);
        ticketRefund2.setState(ETrainTicketRefundState.RS_FAILED);
        ticketRefund2.getPayload().getItems().forEach(x -> x.setRefundOperationStatus(ImOperationStatus.FAILED));
        orderRefund2.setTrainTicketRefunds(List.of(ticketRefund2));

        List<TRefundPartInfo> refundParts = refundPartsService.getRefundParts(order, null);
        assertThat(refundParts).isNotNull().isNotEmpty();

        List<TRefundPartInfo> refundedParts = refundParts.stream().filter(TRefundPartInfo::hasRefund)
                .collect(Collectors.toList());
        assertThat(refundedParts.size()).isEqualTo(2);
        assertThat(refundedParts.stream().filter(x -> x.getState() == ERefundPartState.RPS_REFUNDED &&
                x.getRefund().getState() == EOrderRefundState.RS_REFUNDED).count()).isEqualTo(1);
        assertThat(refundedParts.stream().filter(x -> x.getState() == ERefundPartState.RPS_ENABLED &&
                x.getRefund().getState() == EOrderRefundState.RS_FAILED).count()).isEqualTo(1);
    }

    @Test
    public void testFirstTicketRefundFailed() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_CONFIRMED);
        var order = new GenericOrder();
        order.setId(UUID.randomUUID());
        order.setState(EOrderState.OS_CONFIRMED);
        order.setCurrentInvoice(TrustInvoice.createEmptyInvoice());
        var orderItem = factory.createTrainOrderItem();
        orderItem.getPayload().getPassengers().get(0).getTicket().setRefundStatus(TrainTicketRefundStatus.REFUNDED);
        order.addOrderItem(orderItem);
        var orderItem2 = factory.createTrainOrderItem();
        order.addOrderItem(orderItem2);
        var orderRefund = GenericOrderUserRefund.createForOrder(order);
        orderRefund.setState(EOrderRefundState.RS_REFUNDED);
        orderRefund.setCreatedAt(Instant.now());
        var refundItem1 = createPassengerRefundInfo(orderItem);
        var ticketRefund1 = TrainTicketRefund.createRefund(orderItem, List.of(refundItem1), orderRefund);
        orderRefund.setTrainTicketRefunds(List.of(ticketRefund1));
        var orderRefund2 = GenericOrderUserRefund.createForOrder(order);
        orderRefund2.setState(EOrderRefundState.RS_FAILED);
        orderRefund2.setCreatedAt(Instant.now().minus(Duration.ofDays(1)));
        var refundItem2 = createPassengerRefundInfo(orderItem2);
        var ticketRefund2 = TrainTicketRefund.createRefund(orderItem2, List.of(refundItem2), orderRefund);
        ticketRefund2.setState(ETrainTicketRefundState.RS_FAILED);
        ticketRefund2.getPayload().getItems().forEach(x -> x.setRefundOperationStatus(ImOperationStatus.FAILED));
        orderRefund2.setTrainTicketRefunds(List.of(ticketRefund2));

        List<TRefundPartInfo> refundParts = refundPartsService.getRefundParts(order, null);
        assertThat(refundParts).isNotNull().isNotEmpty();

        List<TRefundPartInfo> refundedParts = refundParts.stream().filter(TRefundPartInfo::hasRefund)
                .collect(Collectors.toList());
        assertThat(refundedParts.size()).isEqualTo(1);
        assertThat(refundedParts.stream().filter(x -> x.getState() == ERefundPartState.RPS_REFUNDED &&
                x.getRefund().getState() == EOrderRefundState.RS_REFUNDED).count()).isEqualTo(1);
    }

    @Test
    public void testCanReturnTill() {
        AtomicReference<Map<String, TRefundPartInfo>> refundPartsByKey = new AtomicReference<>();
        Consumer<List<TRefundPartInfo>> loadRefundParts = (List<TRefundPartInfo> refundParts) -> {
            assertThat(refundParts).isNotNull().isNotEmpty();
            refundPartsByKey.set(refundParts.stream().collect(Collectors.toMap(TRefundPartInfo::getKey,
                    Function.identity())));
        };
        Function<TrainOrderItem, ERefundPartState> getRefundState = (TrainOrderItem orderItem) ->
                refundPartsByKey.get().get(TrainPartKeyService.getServiceKey(orderItem.getId())).getState();

        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_CONFIRMED);
        factory.setDepartureTime(Instant.parse("2020-01-01T18:00:00Z"));
        var order = new GenericOrder();
        order.setId(UUID.randomUUID());
        order.setState(EOrderState.OS_CONFIRMED);
        order.setCurrentInvoice(TrustInvoice.createEmptyInvoice());
        var orderItemWithoutCanReturnTill = factory.createTrainOrderItem();
        order.addOrderItem(orderItemWithoutCanReturnTill);
        var orderItemWithCanReturnTill = factory.createTrainOrderItem();
        orderItemWithCanReturnTill.getPayload().getPassengers().get(0).getTicket()
                .setCanReturnTill(Instant.parse("2020-01-01T12:00:00Z"));
        order.addOrderItem(orderItemWithCanReturnTill);
        var orderItemWithPassengerBoardingControl = factory.createTrainOrderItem();
        orderItemWithPassengerBoardingControl.getPayload().setBoardingSystemType(BoardingSystemType.PASSENGER_BOARDING_CONTROL);
        order.addOrderItem(orderItemWithPassengerBoardingControl);
        var orderItemWithProviderP2 = factory.createTrainOrderItem();
        orderItemWithProviderP2.getPayload().setProvider("P2");
        order.addOrderItem(orderItemWithProviderP2);

        settableClock.setCurrentTime(Instant.parse("2020-01-01T06:00:00Z"));
        loadRefundParts.accept(refundPartsService.getRefundParts(order, null));
        assertThat(getRefundState.apply(orderItemWithoutCanReturnTill)).isEqualTo(ERefundPartState.RPS_ENABLED);
        assertThat(getRefundState.apply(orderItemWithCanReturnTill)).isEqualTo(ERefundPartState.RPS_ENABLED);
        assertThat(getRefundState.apply(orderItemWithPassengerBoardingControl)).isEqualTo(ERefundPartState.RPS_ENABLED);
        assertThat(getRefundState.apply(orderItemWithProviderP2)).isEqualTo(ERefundPartState.RPS_ENABLED);

        settableClock.setCurrentTime(Instant.parse("2020-01-01T12:01:00Z"));
        loadRefundParts.accept(refundPartsService.getRefundParts(order, null));
        assertThat(getRefundState.apply(orderItemWithoutCanReturnTill)).isEqualTo(ERefundPartState.RPS_ENABLED);
        assertThat(getRefundState.apply(orderItemWithCanReturnTill)).isEqualTo(ERefundPartState.RPS_OFFLINE_ENABLED);
        assertThat(getRefundState.apply(orderItemWithPassengerBoardingControl)).isEqualTo(ERefundPartState.RPS_ENABLED);
        assertThat(getRefundState.apply(orderItemWithProviderP2)).isEqualTo(ERefundPartState.RPS_ENABLED);

        settableClock.setCurrentTime(Instant.parse("2020-01-01T17:01:00Z"));
        loadRefundParts.accept(refundPartsService.getRefundParts(order, null));
        assertThat(getRefundState.apply(orderItemWithoutCanReturnTill)).isEqualTo(ERefundPartState.RPS_ENABLED);
        assertThat(getRefundState.apply(orderItemWithCanReturnTill)).isEqualTo(ERefundPartState.RPS_OFFLINE_ENABLED);
        assertThat(getRefundState.apply(orderItemWithPassengerBoardingControl)).isEqualTo(ERefundPartState.RPS_ENABLED);
        assertThat(getRefundState.apply(orderItemWithProviderP2)).isEqualTo(ERefundPartState.RPS_DISABLED);

        settableClock.setCurrentTime(Instant.parse("2020-01-01T18:01:00Z"));
        loadRefundParts.accept(refundPartsService.getRefundParts(order, null));
        assertThat(getRefundState.apply(orderItemWithoutCanReturnTill)).isEqualTo(ERefundPartState.RPS_ENABLED);
        assertThat(getRefundState.apply(orderItemWithCanReturnTill)).isEqualTo(ERefundPartState.RPS_OFFLINE_ENABLED);
        assertThat(getRefundState.apply(orderItemWithPassengerBoardingControl)).isEqualTo(ERefundPartState.RPS_OFFLINE_ENABLED);
        assertThat(getRefundState.apply(orderItemWithProviderP2)).isEqualTo(ERefundPartState.RPS_DISABLED);

        settableClock.setCurrentTime(Instant.parse("2020-01-22T00:00:00Z"));
        loadRefundParts.accept(refundPartsService.getRefundParts(order, null));
        assertThat(getRefundState.apply(orderItemWithoutCanReturnTill)).isEqualTo(ERefundPartState.RPS_ENABLED);
        assertThat(getRefundState.apply(orderItemWithCanReturnTill)).isEqualTo(ERefundPartState.RPS_DISABLED);
        assertThat(getRefundState.apply(orderItemWithPassengerBoardingControl)).isEqualTo(ERefundPartState.RPS_DISABLED);
        assertThat(getRefundState.apply(orderItemWithProviderP2)).isEqualTo(ERefundPartState.RPS_DISABLED);

        settableClock.setCurrentTime(Instant.parse("2020-01-11T11:59:00Z"));
        loadRefundParts.accept(refundPartsService.getRefundParts(order, null));
        assertThat(getRefundState.apply(orderItemWithoutCanReturnTill)).isEqualTo(ERefundPartState.RPS_ENABLED);
        assertThat(getRefundState.apply(orderItemWithCanReturnTill)).isEqualTo(ERefundPartState.RPS_OFFLINE_ENABLED);
        assertThat(getRefundState.apply(orderItemWithPassengerBoardingControl)).isEqualTo(ERefundPartState.RPS_OFFLINE_ENABLED);
        assertThat(getRefundState.apply(orderItemWithProviderP2)).isEqualTo(ERefundPartState.RPS_DISABLED);

        settableClock.setCurrentTime(Instant.parse("2020-01-11T12:01:00Z"));
        loadRefundParts.accept(refundPartsService.getRefundParts(order, null));
        assertThat(getRefundState.apply(orderItemWithoutCanReturnTill)).isEqualTo(ERefundPartState.RPS_ENABLED);
        assertThat(getRefundState.apply(orderItemWithCanReturnTill)).isEqualTo(ERefundPartState.RPS_DISABLED);
        assertThat(getRefundState.apply(orderItemWithPassengerBoardingControl)).isEqualTo(ERefundPartState.RPS_OFFLINE_ENABLED);
        assertThat(getRefundState.apply(orderItemWithProviderP2)).isEqualTo(ERefundPartState.RPS_DISABLED);
    }

    private PassengerRefundInfo createPassengerRefundInfo(TrainOrderItem orderItem) {
        var refundItem = new PassengerRefundInfo();
        TrainPassenger passenger = orderItem.getPayload().getPassengers().get(0);
        refundItem.setBlankId(passenger.getTicket().getBlankId());
        refundItem.setActualRefundTicketAmount(passenger.calculateTotalCostWithInsurance()
                .subtract(passenger.getTicket().getPartnerFee())
                .subtract(passenger.getTicket().getPartnerRefundFee()));
        refundItem.setCalculatedRefundFeeAmount(refundItem.getActualRefundTicketAmount());
        if (orderItem.getPayload().getInsuranceStatus() == InsuranceStatus.CHECKED_OUT) {
            refundItem.setCalculatedRefundInsuranceAmount(passenger.getInsurance().getAmount());
        } else {
            refundItem.setCalculatedRefundInsuranceAmount(Money.zero(passenger.getTicket().getTariffAmount().getCurrency()));
        }
        refundItem.setBlankStatus(ImBlankStatus.REFUNDED);
        refundItem.setCustomerId(passenger.getCustomerId());
        refundItem.setPassengerCategory(passenger.getCategory());
        refundItem.setRefundBlankId(passenger.getTicket().getBlankId() + 1000);
        Integer partnerBuyOperationId = passenger.getTicket().getPartnerBuyOperationId() != null ?
                passenger.getTicket().getPartnerBuyOperationId() : orderItem.getPayload().getPartnerBuyOperationId();
        refundItem.setRefundOperationId(partnerBuyOperationId + 1000);
        refundItem.setRefundOperationStatus(ImOperationStatus.OK);
        return refundItem;
    }
}
