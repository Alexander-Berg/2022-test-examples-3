package ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.option;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryOption;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.integration.service.DeliveryScoringService;
import ru.yandex.market.delivery.mdbapp.integration.service.RedeliveryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RedeliveryOptionChangeTest extends AllMockContextualTest {
    private static final long CHECKOUTER_ORDER_ID = 28340891;
    private static final LocalDate FIXED_TIME_FIRST = LocalDate.of(2021, 1, 1);
    private static final LocalDate FIXED_TIME_SECOND = LocalDate.of(2021, 1, 2);
    private static final long OLD_DELIVERY_SERVICE_ID = 1L;
    private static final long NEW_DELIVERY_SERVICE_ID = 2L;

    @Autowired
    RedeliveryService redeliveryService;

    @MockBean
    private CheckouterServiceClient checkouterServiceClient;

    @MockBean
    private CheckouterOrderService checkouterOrderService;

    @MockBean
    private DeliveryScoringService deliveryScoringService;

    @Test
    public void redeliveryOtherOption() {
        when(checkouterServiceClient.getOrderWithChangeRequests(CHECKOUTER_ORDER_ID)).thenReturn(createOrder());

        DeliveryOption optionFirst = createDeliveryOption(OLD_DELIVERY_SERVICE_ID, FIXED_TIME_FIRST);
        DeliveryOption optionSecond = createDeliveryOption(NEW_DELIVERY_SERVICE_ID, FIXED_TIME_SECOND);

        OrderEditOptions orderEditOptions = createOrderEditOptions(Set.of(optionFirst, optionSecond));
        when(checkouterOrderService
            .getNewDeliveryOptions(CHECKOUTER_ORDER_ID, HistoryEventReason.DELIVERY_SERVICE_PROBLEM))
            .thenReturn(orderEditOptions);

        when(deliveryScoringService.getBestDeliveryOption(argThat(arg -> arg.equals(Set.of(optionSecond))), any()))
            .thenReturn(Set.of(optionSecond));

        redeliveryService.redelivery(CHECKOUTER_ORDER_ID);

        verify(checkouterServiceClient).getOrderWithChangeRequests(CHECKOUTER_ORDER_ID);
        verify(checkouterOrderService)
            .getNewDeliveryOptions(CHECKOUTER_ORDER_ID, HistoryEventReason.DELIVERY_SERVICE_PROBLEM);

        ArgumentCaptor<DeliveryEditRequest> captor = ArgumentCaptor.forClass(DeliveryEditRequest.class);
        verify(checkouterOrderService).updateDelivery(eq(CHECKOUTER_ORDER_ID), captor.capture());

        DeliveryEditRequest deliveryEditRequest = captor.getValue();

        softly.assertThat(deliveryEditRequest.getDeliveryServiceId()).isEqualTo(NEW_DELIVERY_SERVICE_ID);
    }

    @Nonnull
    private Order createOrder() {
        Order order = new Order();
        order.setId(CHECKOUTER_ORDER_ID);
        order.setDelivery(createDelivery());
        return order;
    }

    @Nonnull
    private Delivery createDelivery() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setParcels(List.of(createParcel()));
        delivery.setDeliveryServiceId(OLD_DELIVERY_SERVICE_ID);
        delivery.setDeliveryDates(new DeliveryDates());
        return delivery;
    }

    @Nonnull
    private Parcel createParcel() {
        Parcel parcel = new Parcel();
        parcel.setStatus(ParcelStatus.ERROR);
        parcel.setTracks(List.of(new Track()));
        return parcel;
    }

    @Nonnull
    private DeliveryOption createDeliveryOption(Long serviceId, LocalDate deliveryDate) {
        DeliveryOption option = new DeliveryOption();
        option.setDeliveryServiceId(serviceId);
        option.setFromDate(deliveryDate);
        option.setToDate(deliveryDate);
        option.setTimeIntervalOptions(Set.of(new TimeInterval(
            LocalTime.of(15, 30),
            LocalTime.of(17, 28)
        )));
        return option;
    }

    @Nonnull
    private OrderEditOptions createOrderEditOptions(Set<DeliveryOption> deliveryOptions) {
        OrderEditOptions orderEditOptions = new OrderEditOptions();
        orderEditOptions.setDeliveryOptions(deliveryOptions);
        return orderEditOptions;
    }
}
