package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.core.services.phone.PhoneService;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;
import ru.yandex.market.crm.triggers.services.bpm.variables.TplEvent;
import ru.yandex.market.crm.triggers.services.bpm.variables.TplEvent.DeliveryInterval;
import ru.yandex.market.crm.triggers.services.bpm.variables.TplEvent.ReceiptInfo;
import ru.yandex.market.crm.triggers.services.bpm.variables.TplEvent.Recipient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.MULTI_ORDER_ID;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.ORDER_IDS;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.PHONE_NUMBER;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.RETURN_IDS;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.CourierPlatformConsumer.CANCEL_REASON_TYPE_VARIABLE;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.CourierPlatformConsumer.CODE_VALIDATION_TYPE_VARIABLE;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.CourierPlatformConsumer.DELIVERED_TYPE_VARIABLE;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.CourierPlatformConsumer.DELIVERY_INTERVAL_VARIABLE;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.CourierPlatformConsumer.EVENT_VARIABLE;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.CourierPlatformConsumer.RECEIPT_INFO_VARIABLE;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.CourierPlatformConsumer.RECEIPT_KEY_VARIABLE;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.CourierPlatformConsumer.RECIPIENT_VARIABLE;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.CourierPlatformConsumer.RESCHEDULE_TYPE_VARIABLE;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.CourierPlatformConsumer.TIME_TO_DELIVERY_VARIABLE;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.CourierPlatformConsumer.UNPAID_PART_VARIABLE;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.MessageMatchers.messagesMatcher;

@RunWith(MockitoJUnitRunner.class)
public class CourierPlatformConsumerTest {

    @Mock
    private MessageSender messageSender;
    @Mock
    private LogTypesResolver logTypes;
    @Mock
    private PhoneService phoneService;

    private CourierPlatformConsumer consumer;

    @Before
    public void setUp() {
        when(logTypes.getLogIdentifier("courier_platform.events"))
                .thenReturn(new LogIdentifier("null/null", LBInstallation.LBKX));
        consumer = new CourierPlatformConsumer(messageSender, logTypes, phoneService);
    }

    @Test
    public void checkDeliveryStartedEvent() {
        String line = """
                {
                    "eventType":"ORDER_STARTED_DELIVERY_EVENT",
                    "recipientYandexUid":56737,
                    "recipientPhone":"+71234567890",
                    "recipientEmail":"devnull@email.ru",
                    "multiOrderId":"1233321",
                    "yandexOrderIds":[
                        "98765", "12347"
                    ],
                    "externalReturnIds":[
                        "34673", "235783"
                    ],
                    "trackingId":"cc2ce24f8aa64e9cb24025285f5219b3",
                    "unpaidPart":6735.4664,
                    "payload":{
                        "payload_key":"someValue"
                    },
                    "deliveryIntervalFrom":"10:15",
                    "deliveryIntervalTo":"11:00"
                }
                """;
        TplEvent event = new TplEvent().setType("ORDER_STARTED_DELIVERY_EVENT")
                .setOrderIds(List.of("98765", "12347"))
                .setReturnIds(List.of("34673", "235783"))
                .setMultiOrderId("1233321")
                .setTrackingId("cc2ce24f8aa64e9cb24025285f5219b3")
                .setPayload(Map.of("payload_key", "someValue"));
        UidBpmMessage message = new UidBpmMessage(
                MessageTypes.TPL_DELIVERY_STARTED,
                Uid.asPuid(56737L),
                Map.of(MULTI_ORDER_ID, "1233321"),
                Map.of(
                        MULTI_ORDER_ID, "1233321",
                        ORDER_IDS, "98765,12347",
                        RETURN_IDS, "34673,235783",
                        EVENT_VARIABLE, event,
                        RECIPIENT_VARIABLE, new Recipient().setPuid(56737L).setEmail("devnull@email.ru").setPhone(
                                "71234567890"),
                        DELIVERY_INTERVAL_VARIABLE,
                        new DeliveryInterval().setFrom(LocalTime.of(10, 15)).setTo(LocalTime.of(11, 0)),
                        UNPAID_PART_VARIABLE, new BigDecimal("6735.4664"),
                        PHONE_NUMBER, "71234567890"
                )
        );
        assertMessages(line, message);
    }

    @Test
    public void checkDeliveryReschedulledEvent() {
        String line = """
                {
                    "eventType":"ORDER_RESCHEDULED_EVENT",
                    "recipientYandexUid":56737,
                    "multiOrderId":"1233321",
                    "recipientPhone":"+7 (123) 456-78-90",
                    "recipientEmail":"devnull@email.ru",
                    "yandexOrderIds":[
                        "98765", "12347"
                    ],
                    "trackingId":"cc2ce24f8aa64e9cb24025285f5219b3",
                    "payload":{
                        "payload_key":"someValue"
                    },
                    "deliveryDate":"2021-09-17",
                    "deliveryIntervalFrom":"10:15",
                    "deliveryIntervalTo":"11:00",
                    "rescheduleType":"NEED_CONFIRMATION"
                }
                """;
        TplEvent event = new TplEvent().setType("ORDER_RESCHEDULED_EVENT")
                .setOrderIds(List.of("98765", "12347"))
                .setMultiOrderId("1233321")
                .setTrackingId("cc2ce24f8aa64e9cb24025285f5219b3")
                .setPayload(Map.of("payload_key", "someValue"));
        UidBpmMessage message = new UidBpmMessage(
                MessageTypes.TPL_DELIVERY_RESCHEDULED,
                Uid.asPuid(56737L),
                Map.of(MULTI_ORDER_ID, "1233321"),
                Map.of(
                        MULTI_ORDER_ID, "1233321",
                        ORDER_IDS, "98765,12347",
                        EVENT_VARIABLE, event,
                        RECIPIENT_VARIABLE, new Recipient().setPuid(56737L).setEmail("devnull@email.ru").setPhone(
                                "71234567890"),
                        DELIVERY_INTERVAL_VARIABLE,
                        new DeliveryInterval().setDate(LocalDate.of(2021, 9, 17)).setFrom(LocalTime.of(10, 15)).setTo(LocalTime.of(11, 0)),
                        RESCHEDULE_TYPE_VARIABLE, "NEED_CONFIRMATION",
                        PHONE_NUMBER, "71234567890"
                )
        );
        assertMessages(line, message);
    }

    @Test
    public void checkCanelledEvent() {
        String line = """
                {
                    "eventType":"ORDER_CANCELLED_EVENT",
                    "recipientPhone":"71234567890",
                    "recipientEmail":"devnull@email.ru",
                    "multiOrderId":"1233321",
                    "yandexOrderIds":[
                        "98765",
                        "12347"
                    ],
                    "externalReturnIds":[
                        "34673", "235783"
                    ],
                    "trackingId":"cc2ce24f8aa64e9cb24025285f5219b3",
                    "payload":{
                        "payload_key":"someValue"
                    },
                    "cancelReasonType":"NOT_CALLED_MORE_3_DAY"
                }
                """;
        TplEvent event = new TplEvent().setType("ORDER_CANCELLED_EVENT")
                .setOrderIds(List.of("98765", "12347"))
                .setReturnIds(List.of("34673", "235783"))
                .setMultiOrderId("1233321")
                .setTrackingId("cc2ce24f8aa64e9cb24025285f5219b3")
                .setPayload(Map.of("payload_key", "someValue"));
        UidBpmMessage message = new UidBpmMessage(
                MessageTypes.TPL_ORDER_CANCELLED,
                Uid.asEmail("devnull@email.ru"),
                Map.of(MULTI_ORDER_ID, "1233321"),
                Map.of(
                        MULTI_ORDER_ID, "1233321",
                        ORDER_IDS, "98765,12347",
                        RETURN_IDS, "34673,235783",
                        EVENT_VARIABLE, event,
                        RECIPIENT_VARIABLE, new Recipient().setEmail("devnull@email.ru").setPhone("71234567890"),
                        CANCEL_REASON_TYPE_VARIABLE, "NOT_CALLED_MORE_3_DAY",
                        PHONE_NUMBER, "71234567890"
                )
        );
        assertMessages(line, message);
    }

    @Test
    public void checkConfirmationDeliveryEvent() {
        String line = """
                {
                    "eventType":"CONFIRMATION_DELIVERY_DURING_THE_DAY_EVENT",
                    "recipientPhone":"+71234567890",
                    "recipientEmail":"devnull@email.ru",
                    "multiOrderId":"1233321",
                    "yandexOrderIds":[
                        "98765", "12347"
                    ],
                    "trackingId":"cc2ce24f8aa64e9cb24025285f5219b3",
                    "payload":{
                        "payload_key":"someValue"
                    }
                }
                """;
        TplEvent event = new TplEvent().setType("CONFIRMATION_DELIVERY_DURING_THE_DAY_EVENT")
                .setOrderIds(List.of("98765", "12347"))
                .setMultiOrderId("1233321")
                .setTrackingId("cc2ce24f8aa64e9cb24025285f5219b3")
                .setPayload(Map.of("payload_key", "someValue"));
        UidBpmMessage message = new UidBpmMessage(
                MessageTypes.TPL_DELIVERY_CONFIRMATION,
                Uid.asEmail("devnull@email.ru"),
                Map.of(MULTI_ORDER_ID, "1233321"),
                Map.of(
                        MULTI_ORDER_ID, "1233321",
                        ORDER_IDS, "98765,12347",
                        EVENT_VARIABLE, event,
                        RECIPIENT_VARIABLE, new Recipient().setEmail("devnull@email.ru").setPhone("71234567890"),
                        PHONE_NUMBER, "71234567890"
                )
        );
        assertMessages(line, message);
    }

    @Test
    public void checkDeliverySoonEvent() {
        String line = """
                {
                    "eventType":"ORDER_WILL_BE_DELIVERED_SOON_EVENT",
                    "recipientYandexUid":56737,
                    "recipientPhone":"+71234567890",
                    "recipientEmail":"devnull@email.ru",
                    "multiOrderId":"1233321",
                    "yandexOrderIds":[
                        "98765", "12347"
                    ],
                    "trackingId":"cc2ce24f8aa64e9cb24025285f5219b3",
                    "payload":{
                        "payload_key":"someValue"
                    },
                    "timeToDelivery":"PT27M"
                }
                """;
        TplEvent event = new TplEvent().setType("ORDER_WILL_BE_DELIVERED_SOON_EVENT")
                .setOrderIds(List.of("98765", "12347"))
                .setMultiOrderId("1233321")
                .setTrackingId("cc2ce24f8aa64e9cb24025285f5219b3")
                .setPayload(Map.of("payload_key", "someValue"));
        UidBpmMessage message = new UidBpmMessage(
                MessageTypes.TPL_DELIVERY_SOON,
                Uid.asPuid(56737L),
                Map.of(MULTI_ORDER_ID, "1233321"),
                Map.of(
                        MULTI_ORDER_ID, "1233321",
                        ORDER_IDS, "98765,12347",
                        EVENT_VARIABLE, event,
                        RECIPIENT_VARIABLE, new Recipient().setPuid(56737L).setEmail("devnull@email.ru").setPhone(
                                "71234567890"),
                        TIME_TO_DELIVERY_VARIABLE, Duration.ofMinutes(27),
                        PHONE_NUMBER, "71234567890"
                )
        );
        assertMessages(line, message);
    }

    @Test
    public void checkDeliveredEvent() {
        String line = """
                {
                    "eventType":"ORDER_DELIVERED_EVENT",
                    "recipientPhone":"+71234567890",
                    "recipientEmail":"devnull@email.ru",
                    "multiOrderId":"1233321",
                    "yandexOrderIds":[
                        "98765"
                    ],
                    "externalReturnIds":[
                        "34673", "235783"
                    ],
                    "trackingId":"cc2ce24f8aa64e9cb24025285f5219b3",
                    "payload":{
                        "payload_key":"someValue"
                    },
                    "deliveredType":"TO_THE_DOOR"
                }
                """;
        TplEvent event = new TplEvent().setType("ORDER_DELIVERED_EVENT")
                .setOrderIds(List.of("98765"))
                .setReturnIds(List.of("34673", "235783"))
                .setMultiOrderId("1233321")
                .setTrackingId("cc2ce24f8aa64e9cb24025285f5219b3")
                .setPayload(Map.of("payload_key", "someValue"));
        UidBpmMessage message = new UidBpmMessage(
                MessageTypes.TPL_ORDER_DELIVERED,
                Uid.asEmail("devnull@email.ru"),
                Map.of(MULTI_ORDER_ID, "1233321"),
                Map.of(
                        MULTI_ORDER_ID, "1233321",
                        ORDER_IDS, "98765",
                        RETURN_IDS, "34673,235783",
                        EVENT_VARIABLE, event,
                        RECIPIENT_VARIABLE, new Recipient().setEmail("devnull@email.ru").setPhone("71234567890"),
                        DELIVERED_TYPE_VARIABLE, "TO_THE_DOOR",
                        PHONE_NUMBER, "71234567890"
                )
        );
        assertMessages(line, message);
    }

    @Test
    public void checkReceiptEvent() {
        String line = """
                {
                    "eventType":"RECEIPT_EVENT",
                    "recipientPhone":"+71234567890",
                    "recipientEmail":"devnull@email.ru",
                    "receiptType":"SOME_RECEIPT_TYPE",
                    "receiptDate":"2021-05-17T13:19:23",
                    "receiptTotalSum":23017.4564,
                    "receiptOfdUrl":"http://ofd.ru/cc2ce24f8aa64e9cb24025285f5219b3/pdf",
                    "payload":{
                        "payload_key":"someValue"
                    }
                }
                """;
        TplEvent event = new TplEvent().setType("RECEIPT_EVENT")
                .setPayload(Map.of("payload_key", "someValue"));
        UidBpmMessage message = new UidBpmMessage(
                MessageTypes.TPL_RECEIPT_FISCALIZED,
                Uid.asEmail("devnull@email.ru"),
                Map.of(RECEIPT_KEY_VARIABLE, "http://ofd.ru/cc2ce24f8aa64e9cb24025285f5219b3/pdf"),
                Map.of(
                        RECEIPT_KEY_VARIABLE, "http://ofd.ru/cc2ce24f8aa64e9cb24025285f5219b3/pdf",
                        EVENT_VARIABLE, event,
                        RECIPIENT_VARIABLE, new Recipient().setEmail("devnull@email.ru").setPhone("71234567890"),
                        RECEIPT_INFO_VARIABLE,
                        new ReceiptInfo().setType("SOME_RECEIPT_TYPE").setTotalSum(new BigDecimal("23017.4564"))
                                .setDateTime(LocalDateTime.of(2021, 5, 17, 13, 19, 23))
                                .setOfdUrl("http://ofd.ru/cc2ce24f8aa64e9cb24025285f5219b3/pdf"),
                        PHONE_NUMBER, "71234567890"
                )
        );
        assertMessages(line, message);
    }

    @Test
    public void testCheckCodeValidationResendEvent() {
        String line = """
                {
                    "eventType":"CODE_VALIDATION_RESEND_EVENT",
                    "recipientYandexUid":56737,
                    "recipientPhone":"+71234567890",
                    "recipientEmail":"devnull@email.ru",
                    "multiOrderId":"1233321",
                    "yandexOrderIds":[
                        "12347"
                    ],
                    "trackingId":"cc2ce24f8aa64e9cb24025285f5219b3",
                    "payload":{
                        "payload_key":"someValue"
                    },
                    "code":"123456"
                }
                """;
        TplEvent event = new TplEvent().setType("CODE_VALIDATION_RESEND_EVENT")
                .setOrderIds(List.of("12347"))
                .setMultiOrderId("1233321")
                .setTrackingId("cc2ce24f8aa64e9cb24025285f5219b3")
                .setPayload(Map.of("payload_key", "someValue"));
        UidBpmMessage message = new UidBpmMessage(
                MessageTypes.TPL_CODE_VALIDATION_RESEND,
                Uid.asPuid(56737L),
                Map.of(MULTI_ORDER_ID, "1233321"),
                Map.of(
                        MULTI_ORDER_ID, "1233321",
                        ORDER_IDS, "12347",
                        EVENT_VARIABLE, event,
                        RECIPIENT_VARIABLE, new Recipient().setPuid(56737L).setEmail("devnull@email.ru").setPhone(
                                "71234567890"),
                        CODE_VALIDATION_TYPE_VARIABLE, "123456",
                        PHONE_NUMBER, "71234567890"
                )
        );
        assertMessages(line, message);
    }


    private void assertMessages(String line, UidBpmMessage... expected) {
        List<Map<String, Object>> rows = consumer.transform(line.getBytes());
        Preconditions.checkArgument(rows != null);
        consumer.accept(rows);

        verify(messageSender).send(argThat(messagesMatcher(expected)));
    }

}
