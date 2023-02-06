package ru.yandex.market.crm.triggers.services.bpm.messages;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.DeliveryDeadlineStatus;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.crm.core.domain.trigger.fmcg.OrderItemInfo;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.services.trigger.variables.OrderDeliveryInfo;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.variables.Address;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author vtarasoff
 * @since 08.10.2020
 */
public class StatusUpdatedOrderEventBpmMessageFactoryTest extends OrderEventBpmMessageFactoryTestBase {

    private static final String SCHEDULE = "пн. — пт. с 10:00 до 20:00; сб. с 10:00 до 17:00";

    @Value("classpath:ru/yandex/market/crm/triggers/services/bpm.factories/ORDER_STATUS_UPDATED.json")
    private Resource statusUpdatedJson;

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToPickup() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();
        Delivery delivery = order.getDelivery();

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_PICKUP)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getType(), equalTo(MessageTypes.ORDER_PICKUP));
        assertThat(message.getUid().getType(), is(UidType.PUID));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(order.getBuyer().getUid())));

        assertThat(message.getCorrelationVariables().size(), is(1));
        assertThat(message.getCorrelationVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));

        assertThat(message.getVariables().size(), is(25));
        assertThat(message.getVariables().get(ProcessVariablesNames.CLIENT_NAME), equalTo(CLIENT_NAME));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));
        assertThat(message.getVariables().get(ProcessVariablesNames.PHONE_NUMBER),
                equalTo(order.getBuyer().getNormalizedPhone()));
        assertThat(message.getVariables().get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE), equalTo(
                new OrderDeliveryInfo("09-09-2020", "09-09-2020", "08:00", "08:00", null)));
        assertThat(message.getVariables().get(ProcessVariablesNames.REGION_ID), equalTo(delivery.getRegionId()));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_EVENT_REASON),
                equalTo(event.getReason().name()));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_SERVICE_ID),
                equalTo(delivery.getDeliveryServiceId()));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS), equalTo("PROGRESS"));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_PARTNER_TYPE), equalTo("YANDEX_MARKET"));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_ID), equalTo(delivery.getOutletId()));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_CODE), equalTo(delivery.getOutletCode()));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_ADDRESS), equalTo(testAddress()));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_SCHEDULE), equalTo(SCHEDULE));

        LocalDateTime dateTime = LocalDateTime.of(2020, Month.SEPTEMBER, 9, 23, 0, 0);
        Long eventFromDateTs = dateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli();
        LocalDate date = dateTime.toLocalDate();

        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_PICKUP_EVENT_DATE), equalTo(date.toString()));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_STORAGE_PERIOD), equalTo("1"));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_FEATURES), equalTo(Set.of()));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_EVENT_FROM_DATE_TS), equalTo(eventFromDateTs));
        assertThat(message.getVariables().get(ProcessVariablesNames.EXPERIMENTS), equalTo(Map.of()));
        assertThat(message.getVariables().get(ProcessVariablesNames.PARCEL_BOXES_COUNT), equalTo(3));
    }

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToCancelled() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();
        order.setStatus(OrderStatus.CANCELLED);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_CANCELLED)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getType(), equalTo(MessageTypes.ORDER_CANCELLED));
        assertThat(message.getUid().getType(), is(UidType.PUID));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(event.getOrderAfter().getBuyer().getUid())));

        assertThat(message.getCorrelationVariables().size(), is(1));
        assertThat(message.getCorrelationVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));

        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));
        var eventFromDateTimeTs = event.getFromDate().getTime();
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_EVENT_FROM_DATE_TS), equalTo(eventFromDateTimeTs));
        var lastStatus = event.getOrderBefore().getStatus().name();
        assertThat(message.getVariables().get(ProcessVariablesNames.LAST_ORDER_STATUS), equalTo(lastStatus));
        var lastSubstatus = event.getOrderBefore().getSubstatus().name();
        assertThat(message.getVariables().get(ProcessVariablesNames.LAST_ORDER_SUBSTATUS), equalTo(lastSubstatus));
        var authorRole = event.getAuthor().getRole().name();
        assertThat(message.getVariables().get(ProcessVariablesNames.AUTHOR_ROLE), equalTo(authorRole));
    }

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToDelivered() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();
        order.setStatus(OrderStatus.DELIVERED);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_DELIVERED)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getType(), equalTo(MessageTypes.ORDER_DELIVERED));
        assertThat(message.getUid().getType(), is(UidType.PUID));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(event.getOrderAfter().getBuyer().getUid())));

        assertThat(message.getCorrelationVariables().size(), is(1));
        assertThat(message.getCorrelationVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));

        assertThat(message.getVariables().size(), is(5));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));
    }

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToProcessing() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();
        order.setStatus(OrderStatus.PROCESSING);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.CLIENT_ORDER_PROCESSING)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getType(), equalTo(MessageTypes.CLIENT_ORDER_PROCESSING));
        assertThat(message.getUid().getType(), is(UidType.PUID));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(event.getOrderAfter().getBuyer().getUid())));

        assertTrue(message.getCorrelationVariables().isEmpty());

        assertThat(message.getVariables().size(), is(4));
        assertThat(message.getVariables().get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));

        assertThat((List<OrderItemInfo>) message.getVariables().get(ProcessVariablesNames.ORDER_ITEM_INFO),
                contains(new OrderItemInfo(87654321, 123456789, "123456789012", 1)));
    }

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToProcessingForEstimated() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();
        order.setStatus(OrderStatus.PROCESSING);
        order.setProperty(OrderPropertyType.ESTIMATED_ORDER, true);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ESTIMATED_ORDER_PROCESSING)).collect(Collectors.toList());

        assertThat(messages, hasSize(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getType(), equalTo(MessageTypes.ESTIMATED_ORDER_PROCESSING));
        assertThat(message.getUid().getType(), is(UidType.PUID));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(event.getOrderAfter().getBuyer().getUid())));

        assertFalse(message.getCorrelationVariables().isEmpty());

        Map<String, Object> variables = message.getVariables();

        assertThat(variables.size(), is(7));
        assertThat(variables.get(ProcessVariablesNames.ORDER_ID), equalTo(order.getId()));

        assertEquals("09-09-2020", variables.get(ProcessVariablesNames.ORDER_DELIVERY_DATE_EXPIRE_INFO));
        assertEquals(16L, variables.get(ProcessVariablesNames.REGION_ID));
        assertNotNull(variables.get(ProcessVariablesNames.BUYER));
        assertEquals(CLIENT_NAME, variables.get(ProcessVariablesNames.CLIENT_NAME));
        assertEquals("example@example.com", ((Buyer) variables.get(ProcessVariablesNames.BUYER)).getEmail());
    }

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToPickupAndBuyerHasNoUid() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();

        Buyer buyer = order.getBuyer();
        buyer.setUid(null);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_PICKUP)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getUid().getType(), is(UidType.EMAIL));
        assertThat(message.getUid().getValue(), equalTo(buyer.getEmail()));
    }

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToPickupAndEmptyDelivery() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().setDelivery(null);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_PICKUP)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.REGION_ID), equalTo(0L));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_SERVICE_ID), equalTo(0L));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS), equalTo("NO_DEADLINE"));
        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_PARTNER_TYPE), equalTo(""));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_ID), equalTo(0L));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_CODE), equalTo(""));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_ADDRESS), equalTo(new Address()));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_SCHEDULE), equalTo(""));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_STORAGE_PERIOD), equalTo("0"));
    }

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToPickupAndEmptyOptionalParameters() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();
        Delivery delivery = order.getDelivery();

        event.setReason(null);
        delivery.setDeliveryDates(null);
        delivery.setParcels(null);
        delivery.setDeliveryPartnerType(null);
        delivery.setOutletCode(null);
        delivery.setOutlet(null);
        delivery.setRegionId(null);
        delivery.setDeliveryServiceId(null);
        delivery.setOutletId(null);
        delivery.setOutletStoragePeriod(null);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_PICKUP)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        Map<String, Object> vars = message.getVariables();
        assertThat(vars.get(ProcessVariablesNames.ORDER_EVENT_REASON), equalTo("NO_REASON"));
        assertThat(vars.get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS), equalTo("NO_DEADLINE"));
        assertThat(vars.get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE),
                equalTo(new OrderDeliveryInfo("", "", "", "", null)));
        assertThat((String) vars.get(ProcessVariablesNames.DELIVERY_PARTNER_TYPE), isEmptyString());
        assertThat((String) vars.get(ProcessVariablesNames.OUTLET_CODE), isEmptyString());
        assertThat(vars.get(ProcessVariablesNames.OUTLET_ADDRESS), equalTo(new Address()));
        assertThat(vars.get(ProcessVariablesNames.OUTLET_SCHEDULE), equalTo(""));
        assertThat(vars.get(ProcessVariablesNames.ORDER_PICKUP_EVENT_DATE), equalTo("2020-09-09"));
        assertThat(vars.get(ProcessVariablesNames.REGION_ID), equalTo(0L));
        assertThat(vars.get(ProcessVariablesNames.DELIVERY_SERVICE_ID), equalTo(0L));
        assertThat(vars.get(ProcessVariablesNames.OUTLET_ID), equalTo(0L));
        assertThat(vars.get(ProcessVariablesNames.REGION_ID), equalTo(0L));
        assertThat(vars.get(ProcessVariablesNames.OUTLET_STORAGE_PERIOD), equalTo("0"));
    }

    @Test
    public void shouldNotCreateBpmMessageIfStatusChangedToPickupAndEmptyDeliveryDates() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().setDeliveryDates(new DeliveryDates());

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_PICKUP)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.CHANGED_EXPECTED_DELIVERY_DATE),
                equalTo(new OrderDeliveryInfo("", "", "", "", null)));
    }

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToPickupAndUnknownPartnerType() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().setDeliveryPartnerType(DeliveryPartnerType.UNKNOWN);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_PICKUP)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat((String) message.getVariables().get(ProcessVariablesNames.DELIVERY_PARTNER_TYPE), isEmptyString());
    }

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToPickupAndDeliveryAddressInPostOutlet() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());

        Delivery delivery = event.getOrderAfter().getDelivery();
        delivery.setPostOutlet(delivery.getOutlet());
        delivery.setOutlet(null);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_PICKUP)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_ADDRESS), equalTo(testAddress()));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_SCHEDULE), equalTo(SCHEDULE));
    }

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToPickupAndEmptyAddress() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());

        event.getOrderAfter().getDelivery().getOutlet().setCity(null);
        event.getOrderAfter().getDelivery().getOutlet().setKm(null);
        event.getOrderAfter().getDelivery().getOutlet().setStreet(null);
        event.getOrderAfter().getDelivery().getOutlet().setHouse(null);
        event.getOrderAfter().getDelivery().getOutlet().setBlock(null);
        event.getOrderAfter().getDelivery().getOutlet().setBuilding(null);
        event.getOrderAfter().getDelivery().getOutlet().setPostcode(null);
        event.getOrderAfter().getDelivery().getOutlet().setScheduleString(null);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_PICKUP)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_ADDRESS), equalTo(emptyAddress()));
        assertThat(message.getVariables().get(ProcessVariablesNames.OUTLET_SCHEDULE), equalTo(""));
    }

    @Test
    public void shouldCreateBpmMessageIfStatusChangedToProcessingAndEmptyItems() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        Order order = event.getOrderAfter();
        order.setStatus(OrderStatus.PROCESSING);
        order.setItems(List.of());

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.CLIENT_ORDER_PROCESSING)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat((List<OrderItemInfo>) message.getVariables().get(ProcessVariablesNames.ORDER_ITEM_INFO), empty());
    }

    @Test
    public void shouldCreateBpmMessageIfNullDeliveryDeadlineStatus() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getDelivery().getParcels().get(0).setDeliveryDeadlineStatus(null);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_PICKUP)).collect(Collectors.toList());

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

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_PICKUP)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables().get(ProcessVariablesNames.DELIVERY_DEADLINE_STATUS), equalTo("NO_DEADLINE"));
    }

    @Override
    @Test
    public void shouldCreateBpmMessageWithEmailUidIfMuidExists() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getBuyer().setMuid(123L);

        List<UidBpmMessage> messages = factory.from(event).stream()
                .filter(x -> x.getType().equals(MessageTypes.ORDER_PICKUP)).collect(Collectors.toList());

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getUid().getType(), is(UidType.EMAIL));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(event.getOrderAfter().getBuyer().getEmail())));
    }

    @Override
    protected Resource testedJson() {
        return statusUpdatedJson;
    }

    private Address testAddress() {
        Address address = new Address();
        address.setCity("Екатеринбург");
        address.setKm("5");
        address.setStreet("Ленина");
        address.setHouse("1");
        address.setBlock("2");
        address.setBuilding("1");
        address.setPostcode("10");
        address.setScheduleString(
                "<WorkingTime>" +
                    "<WorkingDaysFrom>1</WorkingDaysFrom>" +
                    "<WorkingDaysTill>1</WorkingDaysTill>" +
                    "<WorkingHoursFrom>10:00</WorkingHoursFrom>" +
                    "<WorkingHoursTill>20:00</WorkingHoursTill>" +
                "</WorkingTime>" +
                "<WorkingTime>" +
                    "<WorkingDaysFrom>2</WorkingDaysFrom>" +
                    "<WorkingDaysTill>2</WorkingDaysTill>" +
                    "<WorkingHoursFrom>10:00</WorkingHoursFrom>" +
                    "<WorkingHoursTill>20:00</WorkingHoursTill>" +
                "</WorkingTime>" +
                "<WorkingTime>" +
                    "<WorkingDaysFrom>3</WorkingDaysFrom>" +
                    "<WorkingDaysTill>3</WorkingDaysTill>" +
                    "<WorkingHoursFrom>10:00</WorkingHoursFrom>" +
                    "<WorkingHoursTill>20:00</WorkingHoursTill>" +
                "</WorkingTime>" +
                "<WorkingTime>" +
                    "<WorkingDaysFrom>4</WorkingDaysFrom>" +
                    "<WorkingDaysTill>4</WorkingDaysTill>" +
                    "<WorkingHoursFrom>10:00</WorkingHoursFrom>" +
                    "<WorkingHoursTill>20:00</WorkingHoursTill>" +
                "</WorkingTime>" +
                "<WorkingTime>" +
                    "<WorkingDaysFrom>5</WorkingDaysFrom>" +
                    "<WorkingDaysTill>5</WorkingDaysTill>" +
                    "<WorkingHoursFrom>10:00</WorkingHoursFrom>" +
                    "<WorkingHoursTill>20:00</WorkingHoursTill>" +
                "</WorkingTime>" +
                "<WorkingTime>" +
                    "<WorkingDaysFrom>6</WorkingDaysFrom>" +
                    "<WorkingDaysTill>6</WorkingDaysTill>" +
                    "<WorkingHoursFrom>10:00</WorkingHoursFrom>" +
                    "<WorkingHoursTill>17:00</WorkingHoursTill>" +
                "</WorkingTime>");
        address.setEstate("");
        address.setEntrance("");
        address.setEntryPhone("");
        address.setFloor("");
        address.setApartment("");
        address.setCountry("");
        return address;
    }

    private Address emptyAddress() {
        Address address = new Address();
        address.setCountry("");
        address.setCity("");
        address.setKm("");
        address.setStreet("");
        address.setHouse("");
        address.setBuilding("");
        address.setBlock("");
        address.setPostcode("");
        address.setScheduleString("");
        address.setEstate("");
        address.setEntrance("");
        address.setEntryPhone("");
        address.setFloor("");
        address.setApartment("");
        return address;
    }
}
