package ru.yandex.market.crm.triggers.services.bpm.messages;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.shipment.DeliveryDeadlineStatus;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.services.trigger.variables.OrderDeliveryInfo;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author vtarasoff
 * @since 08.10.2020
 */
public class DeliveryUpdatedOrderEventBpmMessageFactoryTest extends OrderEventBpmMessageFactoryTestBase {

    @Value("classpath:ru/yandex/market/crm/triggers/services/bpm.factories/ORDER_DELIVERY_UPDATED.json")
    private Resource deliveryUpdatedJson;

    @Test
    public void shouldCreateBpmMessageIfDeliveryDatesChanged() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());

        List<UidBpmMessage> messages = factory.from(event);

        Order order = event.getOrderAfter();
        Delivery delivery = order.getDelivery();

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getType(), equalTo(MessageTypes.ORDER_EXPECTED_DELIVERY_DATE_CHANGED));
        assertThat(message.getUid().getType(), is(UidType.PUID));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(order.getBuyer().getUid())));

        assertThat(message.getCorrelationVariables().size(), is(1));
        assertThat(message.getCorrelationVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));

        assertThat(message.getVariables().size(), is(18));
        assertThat(message.getVariables().get(ProcessVariablesNames.CLIENT_NAME), equalTo(CLIENT_NAME));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));
        assertThat(message.getVariables().get(ProcessVariablesNames.PHONE_NUMBER),
                equalTo(order.getBuyer().getNormalizedPhone()));
        assertThat(message.getVariables().get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE), equalTo(
                new OrderDeliveryInfo("09-09-2020", "10-09-2020", "08:00", "08:08", null)));
        assertThat(message.getVariables().get(ProcessVariablesNames.REGION_ID), equalTo(delivery.getRegionId()));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_EVENT_REASON),
                equalTo(event.getReason().name()));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_SERVICE_ID),
                equalTo(delivery.getDeliveryServiceId()));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS), equalTo("PROGRESS"));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DATES_BEFORE), equalTo(
                new OrderDeliveryInfo("09-09-2020", "09-09-2020", "08:00", "08:00", null)));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_FEATURES), equalTo(Set.of()));
        assertThat(message.getVariables().get(ProcessVariablesNames.EXPERIMENTS), equalTo(Map.of()));
    }

    @Test
    public void shouldCreateBpmMessageIfDeliveryDateDaysChanged() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());

        DeliveryDates after = event.getOrderAfter().getDelivery().getDeliveryDates();
        after.setFromTime((LocalTime) null);
        after.setToTime((LocalTime) null);

        DeliveryDates before = event.getOrderBefore().getDelivery().getDeliveryDates();
        before.setFromTime((LocalTime) null);
        before.setToTime((LocalTime) null);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), is(1));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE), equalTo(
                new OrderDeliveryInfo("09-09-2020", "10-09-2020", "", "", null)));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DATES_BEFORE), equalTo(
                new OrderDeliveryInfo("09-09-2020", "09-09-2020", "", "", null)));
    }

    @Test
    public void shouldCreateBpmMessageIfDeliveryDateDaysNotChanged() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());

        DeliveryDates after = event.getOrderAfter().getDelivery().getDeliveryDates();
        DeliveryDates before = event.getOrderBefore().getDelivery().getDeliveryDates();
        after.setFromDate(before.getFromDate());
        after.setToDate(before.getToDate());

        List<UidBpmMessage> messages = factory.from(event);

        assertFalse(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfDeliveryAfterIsEmpty() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().setDelivery(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfDeliveryBeforeIsEmpty() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderBefore().setDelivery(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfDeliveryDatesAreEmpty() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().setDeliveryDates(null);
        event.getOrderBefore().getDelivery().setDeliveryDates(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldCreateBpmMessageIfDeliveryDatesAfterIsEmpty() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().setDeliveryDates(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), is(1));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE),
                equalTo(new OrderDeliveryInfo("", "", "", "", null)));
    }

    @Test
    public void shouldCreateBpmMessageIfDeliveryDatesBeforeIsEmpty() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderBefore().getDelivery().setDeliveryDates(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), is(1));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DATES_BEFORE),
                equalTo(new OrderDeliveryInfo("", "", "", "", null)));
    }

    @Test
    public void shouldNotCreateBpmMessageIfDeliveryDatesNotChanged() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().setDeliveryDates(event.getOrderBefore().getDelivery().getDeliveryDates());

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfDeliveryDateDaysNotChanged() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());

        DeliveryDates after = event.getOrderAfter().getDelivery().getDeliveryDates();
        after.setFromTime((LocalTime) null);
        after.setToTime((LocalTime) null);

        DeliveryDates before = event.getOrderBefore().getDelivery().getDeliveryDates();
        before.setFromTime((LocalTime) null);
        before.setToTime((LocalTime) null);

        after.setFromDate(before.getFromDate());
        after.setToDate(before.getToDate());

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfOrderStatusNotAccepted() {
        Set<OrderStatus> accepted = Set.of(OrderStatus.PROCESSING, OrderStatus.DELIVERY, OrderStatus.PICKUP);

        Set<OrderStatus> actual = Stream.of(OrderStatus.values())
                .filter(status -> {
                    OrderHistoryEvent event = loadOrderEvent(testedJson());
                    event.getOrderAfter().setStatus(status);
                    return !factory.from(event).isEmpty();
                })
                .collect(Collectors.toUnmodifiableSet());

        assertThat(actual, equalTo(accepted));
    }

    @Test
    public void shouldCreateBpmMessageIfDeliveryServiceDelayedAndStatusDelivery() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.setReason(HistoryEventReason.DELIVERY_SERVICE_DELAYED);
        event.getOrderAfter().setStatus(OrderStatus.DELIVERY);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), is(1));
    }

    @Test
    public void shouldCreateBpmMessageIfBuyerHasNoUid() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getBuyer().setUid(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getUid().getType(), is(UidType.EMAIL));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(event.getOrderAfter().getBuyer().getEmail())));
    }

    @Test
    public void shouldCreateBpmMessageIfEmptyOptionalParameters() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();

        event.setReason(null);
        event.getOrderBefore().getDelivery().setDeliveryDates(new DeliveryDates());
        order.getDelivery().setParcels(null);
        order.getDelivery().setRegionId(null);
        order.getDelivery().setDeliveryServiceId(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_EVENT_REASON), equalTo("NO_REASON"));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS), equalTo("NO_DEADLINE"));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DATES_BEFORE),
                equalTo(new OrderDeliveryInfo("", "", "", "", null)));
        assertThat(message.getVariables().get(ProcessVariablesNames.REGION_ID), equalTo(0L));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_SERVICE_ID), equalTo(0L));
    }

    @Test
    public void shouldCreateBpmMessageIfUnknownReason() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.setReason(HistoryEventReason.UNKNOWN);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_EVENT_REASON), equalTo("NO_REASON"));
    }

    @Test
    public void shouldCreateBpmMessageIfNullDeliveryDeadlineStatus() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().getParcels().get(0).setDeliveryDeadlineStatus(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS), equalTo("NO_DEADLINE"));
    }

    @Test
    public void shouldCreateBpmMessageIfUnknownDeliveryDeadlineStatus() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().getParcels().get(0)
                .setDeliveryDeadlineStatus(DeliveryDeadlineStatus.UNKNOWN);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS), equalTo("NO_DEADLINE"));
    }

    @Override
    protected Resource testedJson() {
        return deliveryUpdatedJson;
    }
}
