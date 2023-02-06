package ru.yandex.market.pers.notify.external;


import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.external.checkouter.CheckouterService;
import ru.yandex.market.pers.notify.external.sender.SenderClient;
import ru.yandex.market.pers.notify.external.sender.SenderHttpClientProperties;
import ru.yandex.market.pers.notify.mock.MarketMailerMockFactory;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.SenderAccount;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.test.MailProcessorInvoker;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

// This "test" can be used to send real mails via Sendr test instance. It mocks all external dependencies of market-mailer
// (just like all our tests) except Sendr.
// It is intentionally marked as @Ignored because it is not actually a test, but rather a tool to verify mailer - sendr
// integration manually. For the same reason it doesn't contain any assertions.
@Disabled
@ActiveProfiles({"production", "sender-testing"})
@ContextConfiguration(classes = SenderTemplatesTest.SenderIntegrationTestConfig.class, inheritLocations = false)
public class SenderTemplatesTest extends MarketMailerMockedDbTest {
    private static final String BIND_KEY = "BIND_KEY";
    private static final long ORDER_ID = 12345L;
    private static final String EMAIL = "valter@yandex-team.ru";

    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private CheckouterService checkouterService;
    @Autowired
    private MailProcessorInvoker mailProcessorInvoker;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void noAuthPrepaidProcessing() throws Exception {
        sendOrderMail(true, PaymentType.PREPAID, NotificationSubtype.ORDER_PROCESSING, OrderStatus.PROCESSING);
    }

    @Test
    public void noAuthPrepaidPending() throws Exception {
        sendOrderMail(true, PaymentType.PREPAID, NotificationSubtype.ORDER_PENDING, OrderStatus.PENDING);
    }

    @Test
    public void noAuthPostpaidProcessing() throws Exception {
        sendOrderMail(true, PaymentType.POSTPAID, NotificationSubtype.ORDER_PROCESSING, OrderStatus.PROCESSING);
    }

    @Test
    public void noAuthPostpaidPending() throws Exception {
        sendOrderMail(true, PaymentType.POSTPAID, NotificationSubtype.ORDER_PENDING, OrderStatus.PENDING);
    }

    @Test
    public void authPrepaidProcessing() throws Exception {
        sendOrderMail(false, PaymentType.PREPAID, NotificationSubtype.ORDER_PROCESSING, OrderStatus.PROCESSING
        );
    }

    @Test
    public void authPrepaidPending() throws Exception {
        sendOrderMail(false, PaymentType.PREPAID, NotificationSubtype.ORDER_PENDING, OrderStatus.PENDING);
    }

    @Test
    public void authPrepaidPendingNoName() throws Exception {
        sendOrderMail(false, PaymentType.PREPAID, NotificationSubtype.ORDER_PENDING, OrderStatus.PENDING, null, null);
    }



    private void sendOrderMail(boolean noAuth, PaymentType paymentType, NotificationSubtype notificationSubtype,
                               OrderStatus orderStatus) throws IOException {
        sendOrderMail(noAuth, paymentType, notificationSubtype, orderStatus,
                paymentType.name() + " " + (noAuth ? "HEAВТОРИЗОВАННЫЙ" : "АВТОРИЗОВАННЫЙ"), orderStatus.name());

    }

    private void sendOrderMail(boolean noAuth, PaymentType paymentType, NotificationSubtype notificationSubtype,
                               OrderStatus orderStatus, String firstName, String name) {
        Order order = MarketMailerMockFactory.generateOrder(orderStatus, noAuth, paymentType,
                firstName, name);
        order.getBuyer().setBindKey(BIND_KEY);
        when(checkouterService.getOrder(eq(ORDER_ID), any(ClientRole.class), anyLong(), anyBoolean())).thenReturn(order);

        notificationEventService.addEvent(NotificationEventSource
                .fromEmail(EMAIL, notificationSubtype)
                .setSourceId(ORDER_ID)
                .build());

        mailProcessorInvoker.processAllMail();
    }

    @Configuration
    @ImportResource({"classpath:test-bean.xml", "classpath:market-mailer-test-bean.xml"})
    static class SenderIntegrationTestConfig {

        @Bean
        public SenderClient senderClient() {
            return new SenderClient(new SenderHttpClientProperties(Collections.singletonMap(SenderAccount.MARKET, "")),
                    "https://test.sender.yandex-team.ru",
                    3000, 3000, 3000, 10);
        }

    }
}
