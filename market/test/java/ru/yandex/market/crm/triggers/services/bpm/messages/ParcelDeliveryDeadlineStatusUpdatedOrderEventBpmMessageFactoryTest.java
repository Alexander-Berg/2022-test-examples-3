package ru.yandex.market.crm.triggers.services.bpm.messages;

import java.util.HashMap;
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
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.services.trigger.variables.OrderDeliveryInfo;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author vtarasoff
 * @since 09.10.2020
 */
public class ParcelDeliveryDeadlineStatusUpdatedOrderEventBpmMessageFactoryTest
        extends OrderEventBpmMessageFactoryTestBase {

    @Value("classpath:ru/yandex/market/crm/triggers/services/bpm.factories/PARCEL_DELIVERY_DEADLINE_STATUS_UPDATED.json")
    private Resource deliveryDeadlineStatusUpdatedJson;

    @Test
    public void shouldCreateBpmMessageIfDeliveryDatesChanged() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());

        List<UidBpmMessage> messages = factory.from(event);

        Order order = event.getOrderAfter();
        Delivery delivery = order.getDelivery();

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getType(), equalTo(MessageTypes.ORDER_DEADLINE_STATUS_UPDATED_NOW));
        assertThat(message.getUid().getType(), is(UidType.PUID));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(order.getBuyer().getUid())));

        assertThat(message.getCorrelationVariables().size(), is(1));
        assertThat(message.getCorrelationVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));

        assertThat(message.getVariables().size(), is(17));
        assertThat(message.getVariables().get(ProcessVariablesNames.CLIENT_NAME), equalTo(CLIENT_NAME));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));
        assertThat(message.getVariables().get(ProcessVariablesNames.PHONE_NUMBER),
                equalTo(order.getBuyer().getNormalizedPhone()));
        assertThat(message.getVariables().get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE), equalTo(
                new OrderDeliveryInfo("09-09-2020", "09-09-2020", "08:00", "08:00", null)));
        assertThat(message.getVariables().get(ProcessVariablesNames.REGION_ID), equalTo(delivery.getRegionId()));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_EVENT_REASON),
                equalTo(delivery.getParcels().get(0).getDeliveryDeadlineStatus().name()));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_SERVICE_ID),
                equalTo(delivery.getDeliveryServiceId()));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS),
                equalTo(DeliveryDeadlineStatus.DELIVERY_DATES_DEADLINE_NOW.name()));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_FEATURES),
                equalTo(Set.of("EXPRESS_DELIVERY")));
        assertThat(message.getVariables().get(ProcessVariablesNames.EXPERIMENTS), equalTo(Map.of()));
    }

    @Test
    public void shouldNotCreateBpmMessageIfEmptyDelivery() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().setDelivery(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfNullDeliveryPercels() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().setParcels(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfEmptyDeliveryPercels() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().setParcels(List.of());

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfEmptyDeadlineStatus() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().getParcels().get(0).setDeliveryDeadlineStatus(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfDeadlineStatusNotAccepted() {
        Set<DeliveryDeadlineStatus> accepted = Set.of(DeliveryDeadlineStatus.DELIVERY_DATES_DEADLINE_NOW,
                                                      DeliveryDeadlineStatus.DELIVERY_DATES_DEADLINE_REJECT);

        Map<DeliveryDeadlineStatus, String> messageTypes = new HashMap<>();

        Set<DeliveryDeadlineStatus> actual = Stream.of(DeliveryDeadlineStatus.values())
                .filter(status -> {
                    OrderHistoryEvent event = loadOrderEvent(testedJson());
                    event.getOrderAfter().getDelivery().getParcels().get(0).setDeliveryDeadlineStatus(status);

                    List<UidBpmMessage> messages = factory.from(event);
                    if (messages.isEmpty()) {
                        return false;
                    }

                    messageTypes.put(status, messages.get(0).getType());
                    return true;
                })
                .collect(Collectors.toUnmodifiableSet());

        assertThat(actual, equalTo(accepted));

        assertThat(messageTypes.get(DeliveryDeadlineStatus.DELIVERY_DATES_DEADLINE_NOW),
                equalTo(MessageTypes.ORDER_DEADLINE_STATUS_UPDATED_NOW));
        assertThat(messageTypes.get(DeliveryDeadlineStatus.DELIVERY_DATES_DEADLINE_REJECT),
                equalTo(MessageTypes.ORDER_DEADLINE_STATUS_UPDATED_REJECT));
    }

    @Test
    public void shouldCreateBpmMessageIfEmptyOptionalParameters() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().setDeliveryDates(new DeliveryDates());
        event.getOrderAfter().getDelivery().setRegionId(null);
        event.getOrderAfter().getDelivery().setDeliveryServiceId(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE),
                equalTo(new OrderDeliveryInfo("", "", "", "", null)));
        assertThat(message.getVariables().get(ProcessVariablesNames.REGION_ID), equalTo(0L));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_SERVICE_ID), equalTo(0L));
    }

    @Test
    public void shouldCreateBpmMessageWithFirstDeliveryDeadlineStatus() {
        Parcel deadlineNow = new Parcel();
        deadlineNow.setDeliveryDeadlineStatus(DeliveryDeadlineStatus.DELIVERY_DATES_DEADLINE_NOW);

        Parcel deadlineReject = new Parcel();
        deadlineReject.setDeliveryDeadlineStatus(DeliveryDeadlineStatus.DELIVERY_DATES_DEADLINE_REJECT);

        List<Parcel> parcels = List.of(deadlineNow, deadlineReject);

        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().setParcels(parcels);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS),
                equalTo(DeliveryDeadlineStatus.DELIVERY_DATES_DEADLINE_NOW.name()));
    }

    @Override
    protected Resource testedJson() {
        return deliveryDeadlineStatusUpdatedJson;
    }
}
