package ru.yandex.market.pers.notify.ems.consumer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.pers.notify.ems.MailAttachment;
import ru.yandex.market.pers.notify.external.sender.SenderClient;
import ru.yandex.market.pers.notify.mock.MarketMailerMockFactory;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.SenderTemplate;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.service.AboService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

abstract public class CancelledOrderSenderTestsCommon extends SendNotificationTestCommons {

    public static final String EMAIL_TEMPLATE = "semin-serg%s@yandex-team.ru";

    public static final String ENCODED_SECRET_KEY;

    static {
        try {
            ENCODED_SECRET_KEY = URLEncoder.encode(MarketMailerMockFactory.SECRET_KEY, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException unexpected) {
            throw new RuntimeException(
                "unexpected exception when encoding secretKey + \"" + MarketMailerMockFactory.SECRET_KEY +
                    "with UTF-8",
                unexpected
            );
        }
    }

    public static final String SECRET_KEY_PARAM_NAME = "sKey";

    public static final String SUBSTATUS_PARAM_NAME = "substatus";

    static final MockUtil MOCK_UTIL = new MockUtil();

    @Autowired
    SenderClient senderClient;

    @Autowired
    AboService aboService;

    Order order;

    static NotificationEventSource createNotificationEventSource(
        long orderId,
        OrderSubstatus orderSubstatus,
        ClientRole authorRole,
        String secretKey
    ) {
        NotificationEventSource.Builder builder = NotificationEventSource
            .fromEmail(
                String.format(
                    EMAIL_TEMPLATE,
                    //знаки после "+" не воспринимаются почтовым сервисом яндекса, всё равно придёт
                    //на адрес, предшествующий "+". Можно передать через знаки после "+" отладочную
                    //информацию.
                    "+substatus=" + orderSubstatus.name() + (
                        (authorRole != null) ? ("&role=" + authorRole) : ""
                    )
                ),
                NotificationSubtype.ORDER_CANCELLED
            )
            .setSourceId(randomLong())
            .addDataParam(NotificationEventDataName.ORDER_ID, String.valueOf(orderId));
        if (secretKey != null) {
            builder.addDataParam(NotificationEventDataName.ABO_SECRET_KEY, secretKey);
        }
        if (authorRole != null) {
            builder.addDataParam(NotificationEventDataName.AUTHOR_ROLE, authorRole.name());
        }
        return builder.build();
    }

    @BeforeEach
    public void setUp() {
        order = MarketMailerMockFactory.generateOrder();
        order.setStatus(OrderStatus.CANCELLED);
        when(
            checkouterService.getOrder(
                eq(order.getId()),
                eq(ClientRole.SYSTEM),
                eq(0L),
                eq(Boolean.FALSE)
            )
        ).thenReturn(order);
        when(aboService.generateSkFeedback(order.getId())).thenReturn(MarketMailerMockFactory.SECRET_KEY);
    }

    protected void test(OrderSubstatus orderSubstatus, String orderSubstatusForSenderExpected) throws Exception {
        test(orderSubstatus, orderSubstatusForSenderExpected, (ClientRole) null);
    }

    protected void test(OrderSubstatus orderSubstatus, String orderSubstatusForSenderExpected, ClientRole authorRole) throws Exception {
        test(orderSubstatus, orderSubstatusForSenderExpected, authorRole, SkSource.ABO);
    }

    protected void test(OrderSubstatus orderSubstatus, String orderSubstatusForSenderExpected, SkSource skSource) throws Exception {
        test(orderSubstatus, orderSubstatusForSenderExpected, null, skSource);
    }

    protected void test(
        OrderSubstatus orderSubstatus,
        String orderSubstatusForSenderExpected,
        ClientRole authorRole,
        SkSource skSource
    ) throws Exception {
        test(
            orderSubstatus,
            orderSubstatusForSenderExpected,
            authorRole,
            skSource,
            NotificationEventStatus.SENT
        );
    }


    protected void test(
        OrderSubstatus orderSubstatus,
        String orderSubstatusForSenderExpected,
        ClientRole authorRole,
        SkSource skSource,
        NotificationEventStatus resultStatusExpected
    ) throws Exception {
        order.setSubstatus(orderSubstatus);
        NotificationEventSource eventSource = createNotificationEventSource(
            order.getId(),
            orderSubstatus,
            authorRole,
            (skSource != SkSource.ABO) ? MarketMailerMockFactory.SECRET_KEY : null
        );
        long id = scheduleAndCheck(eventSource);
        sendAndCheck(id, checkoutOrderSenderConsumer, resultStatusExpected);
        verify(checkouterService).getOrder(order.getId(), ClientRole.SYSTEM, 0L, false);
        if (skSource == SkSource.ABO) {
            verify(aboService).generateSkFeedback(order.getId());
        } else {
            verify(aboService, times(0)).generateSkFeedback(anyLong());
        }
        if ((resultStatusExpected != NotificationEventStatus.SENT) || !MOCK_UTIL.isMock(senderClient)) {
            //Если senderClient не является mock-объектом или если в тесте не ожидали отправку сообщения
            //Полезное сравнение, чтобы можно было запускать данный тест с настоящим sender-ом и генерировать настоящие письма
            return;
        }
        ArgumentCaptor<Map<String, Object>> modelCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
        verify(senderClient).sendTransactionalMail(
            eq(eventSource.getEmail()),
            eq(SenderTemplate.ORDER_CANCELLED),
            anyBoolean(),
            modelCaptor.capture(),
            anyListOf(MailAttachment.class)
        );
        Map<String, Object> model = modelCaptor.getValue();
        assertTrue(model.containsKey(SECRET_KEY_PARAM_NAME), "Model should contain secretKey");
        assertEquals(
            ENCODED_SECRET_KEY,
            model.get(SECRET_KEY_PARAM_NAME),
            "Secret should be encoded with UTF-8"
        );
        assertTrue(model.containsKey(SUBSTATUS_PARAM_NAME), "Model should contain substatus");
        assertEquals(
            orderSubstatusForSenderExpected,
            model.get(SUBSTATUS_PARAM_NAME)
        );
    }

    protected enum SkSource {
        ABO, EVENT_SOURCE_DATA
    }

}
