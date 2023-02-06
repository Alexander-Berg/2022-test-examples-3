package ru.yandex.market.crm.triggers.services.bpm.messages;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.DeliveryDeadlineStatus;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryDatesChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.PaymentChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.services.trigger.variables.OrderDeliveryInfo;
import ru.yandex.market.crm.triggers.domain.system.MessageInfo;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author vtarasoff
 * @since 08.10.2020
 */
public class ChangeRequestStatusUpdatedOrderEventBpmMessageFactoryTest extends OrderEventBpmMessageFactoryTestBase {

    @Value("classpath:ru/yandex/market/crm/triggers/services/bpm.factories/ORDER_CHANGE_REQUEST_STATUS_UPDATED.json")
    private Resource changeRequestStatusUpdatedJson;

    @Test
    public void shouldCreateBpmMessageIfStatusChanged() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();

        List<UidBpmMessage> messages = factory.from(event);
        Delivery delivery = order.getDelivery();
        DeliveryDatesChangeRequestPayload payload = deliveryDatesPayload(order);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getType(), equalTo(MessageTypes.ORDER_USER_DELIVERY_CHANGE_APPLIED));
        assertThat(message.getUid().getType(), is(UidType.PUID));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(order.getBuyer().getUid())));

        assertThat(message.getCorrelationVariables().size(), is(1));
        assertThat(message.getCorrelationVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));

        Map<String, Object> vars = message.getVariables();
        assertThat(vars.size(), equalTo(17));
        assertThat(vars.get(ProcessVariablesNames.CLIENT_NAME), equalTo(CLIENT_NAME));
        assertThat(vars.get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));
        assertThat(vars.get(ProcessVariablesNames.PHONE_NUMBER), equalTo(order.getBuyer().getNormalizedPhone()));
        assertThat(vars.get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE), equalTo(
                new OrderDeliveryInfo("09-09-2020", "09-09-2020", "08:00", "08:00", null)));
        assertThat(vars.get(ProcessVariablesNames.REGION_ID), equalTo(delivery.getRegionId()));
        assertThat(vars.get(ProcessVariablesNames.ORDER_EVENT_REASON), equalTo(payload.getReason().name()));
        assertThat(vars.get(ProcessVariablesNames.DELIVERY_SERVICE_ID), equalTo(delivery.getDeliveryServiceId()));
        assertThat(vars.get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS), equalTo("PROGRESS"));
        assertThat(vars.get(ProcessVariablesNames.DELIVERY_FEATURES), equalTo(Set.of()));
        assertThat(vars.get(ProcessVariablesNames.EXPERIMENTS), equalTo(Map.of()));

        MessageInfo messageInfo = (MessageInfo) vars.get(ProcessVariablesNames.MESSAGE_INFO);
        assertNotNull(messageInfo);
        assertEquals(MessageTypes.ORDER_USER_DELIVERY_CHANGE_APPLIED, messageInfo.getType());

        long expectedTimestamp = ZonedDateTime.of(
                LocalDateTime.of(2021, 1, 13, 11, 50, 53),
                ZoneId.of("Europe/Moscow")
        ).toEpochSecond() * 1000;

        assertEquals(expectedTimestamp, messageInfo.getTimestamp());
    }

    @Test
    public void shouldNotCreateBpmMessageIfRequestStatusNotAccepted() {
        Set<ChangeRequestStatus> accepted =
                Set.of(ChangeRequestStatus.APPLIED, ChangeRequestStatus.REJECTED, ChangeRequestStatus.INVALID);

        Map<ChangeRequestStatus, String> messageTypes = new HashMap<>();

        Set<ChangeRequestStatus> actual = Stream.of(ChangeRequestStatus.values())
                .filter(status -> {
                    OrderHistoryEvent event = loadOrderEvent(testedJson());
                    setChangeRequestField(deliveryDatesChangeRequest(event.getOrderAfter()), "status", status);

                    List<UidBpmMessage> messages = factory.from(event);
                    if (messages.isEmpty()) {
                        return false;
                    }

                    messageTypes.put(status, messages.get(0).getType());
                    return true;
                })
                .collect(Collectors.toUnmodifiableSet());

        assertThat(actual, equalTo(accepted));

        assertThat(messageTypes.get(ChangeRequestStatus.APPLIED),
                equalTo(MessageTypes.ORDER_USER_DELIVERY_CHANGE_APPLIED));
        assertThat(messageTypes.get(ChangeRequestStatus.REJECTED),
                equalTo(MessageTypes.ORDER_USER_DELIVERY_CHANGE_REJECTED));
        assertThat(messageTypes.get(ChangeRequestStatus.INVALID),
                equalTo(MessageTypes.ORDER_USER_DELIVERY_CHANGE_INVALID));
    }

    @Test
    public void shouldNotCreateBpmMessageIfNullChangeRequests() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().setChangeRequests(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfEmptyChangeRequests() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().setChangeRequests(List.of());

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfChangeRequestsSizeChanged() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().setChangeRequests(List.of(event.getOrderAfter().getChangeRequests().get(0)));

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfChangeRequestNotFoundById() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        setChangeRequestField(deliveryDatesChangeRequest(event.getOrderAfter()), "id", 7L);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfChangeRequestNotFoundByType() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        setChangeRequestField(
                deliveryDatesChangeRequest(event.getOrderAfter()),
                "payload",
                new PaymentChangeRequestPayload(PaymentMethod.GOOGLE_PAY, false)
        );

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfChangeRequestStatusNotChanged() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        setChangeRequestField(
                deliveryDatesChangeRequest(event.getOrderAfter()),
                "status",
                deliveryDatesChangeRequest(event.getOrderBefore()).getStatus()
        );

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfChangeRequestTypeNotDeliveryDates() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        setChangeRequestField(
                deliveryDatesChangeRequest(event.getOrderAfter()),
                "payload",
                new PaymentChangeRequestPayload(PaymentMethod.GOOGLE_PAY, false)
        );
        setChangeRequestField(
                deliveryDatesChangeRequest(event.getOrderBefore()),
                "payload",
                new PaymentChangeRequestPayload(PaymentMethod.GOOGLE_PAY, false)
        );

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldCreateBpmMessageIfEmptyOptionalParameters() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();
        DeliveryDatesChangeRequestPayload payload = deliveryDatesPayload(order);

        payload.setReason(null);

        payload.setFromDate(null);
        payload.setToDate(null);
        payload.getTimeInterval().setFromTime(null);
        payload.getTimeInterval().setToTime(null);

        order.getDelivery().setParcels(null);
        order.getDelivery().setRegionId(null);
        order.getDelivery().setDeliveryServiceId(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_EVENT_REASON), equalTo("NO_REASON"));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS), equalTo("NO_DEADLINE"));
        assertThat(message.getVariables().get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE),
                equalTo(new OrderDeliveryInfo("", "", "", "", null)));
        assertThat(message.getVariables().get(ProcessVariablesNames.REGION_ID), equalTo(0L));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_SERVICE_ID), equalTo(0L));
    }

    @Test
    public void shouldCreateBpmMessageIfUnknownReason() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());

        DeliveryDatesChangeRequestPayload payload = deliveryDatesPayload(event.getOrderAfter());
        payload.setReason(HistoryEventReason.UNKNOWN);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_EVENT_REASON), equalTo("NO_REASON"));
    }

    @Test
    public void shouldCreateBpmMessageIfNullTime() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        DeliveryDatesChangeRequestPayload payload = deliveryDatesPayload(event.getOrderAfter());

        payload.setTimeInterval(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE),
                equalTo(new OrderDeliveryInfo("09-09-2020", "09-09-2020", "", "", null)));
    }

    @Test
    public void shouldCreateBpmMessageIfEmptyTime() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        DeliveryDatesChangeRequestPayload payload = deliveryDatesPayload(event.getOrderAfter());

        payload.getTimeInterval().setFromTime(null);
        payload.getTimeInterval().setToTime(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE),
                equalTo(new OrderDeliveryInfo("09-09-2020", "09-09-2020", "", "", null)));
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
        return changeRequestStatusUpdatedJson;
    }

    private ChangeRequest deliveryDatesChangeRequest(Order order) {
        return order.getChangeRequests().stream()
                .filter(request -> request.getPayload() instanceof DeliveryDatesChangeRequestPayload)
                .findFirst()
                .get();
    }

    private DeliveryDatesChangeRequestPayload deliveryDatesPayload(Order order) {
        return (DeliveryDatesChangeRequestPayload) deliveryDatesChangeRequest(order).getPayload();
    }

    private void setChangeRequestField(ChangeRequest request, String name, Object value) {
        Field field = ReflectionUtils.findField(ChangeRequest.class, name);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, request, value);
    }
}
