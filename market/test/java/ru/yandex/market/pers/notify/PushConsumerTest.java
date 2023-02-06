package ru.yandex.market.pers.notify;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.external.checkouter.CheckouterService;
import ru.yandex.market.pers.notify.mock.MarketMailerMockFactory;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.model.push.PushTemplateType;
import ru.yandex.market.pers.notify.push.MobileAppInfoDAO;
import ru.yandex.market.pers.notify.push.PushReplacingConsumer;
import ru.yandex.market.pers.notify.push.PushSimpleConsumer;
import ru.yandex.market.pers.notify.push.PusherService;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 *         created on 17.05.16.
 */
public abstract class PushConsumerTest extends MarketMailerMockedDbTest {
    protected static final Long ORDER_ID = 123123L;
    private static final String SHOP_ORDER_ID = "\"123121\"";

    @Autowired
    private CheckouterService checkouterService;
    @Autowired
    public PushReplacingConsumer pushReplacingConsumer;
    @Autowired
    public PushSimpleConsumer pushSimpleConsumer;
    @Autowired
    private PusherService marketPusherService;
    @Autowired
    protected NotificationEventService notificationEventService;

    @BeforeEach
    public void initConsumer() {
        MobileAppInfoDAO mobileAppInfoDAO = mock(MobileAppInfoDAO.class);
        when(mobileAppInfoDAO.getByUid(anyLong())).thenReturn(generateApps());
        when(mobileAppInfoDAO.getByUuid(anyString())).thenReturn(generateApp());
        pushReplacingConsumer.setMobileAppInfoDAO(mobileAppInfoDAO);
        pushReplacingConsumer.setPusherService(marketPusherService);
    }

    private List<MobileAppInfo> generateApps() {
        List<MobileAppInfo> apps = new ArrayList<>();
        int count = RND.nextInt(10) + 1;
        for (int i = 0; i < count; i++) {
            apps.add(generateApp());
        }
        return apps;
    }

    private MobileAppInfo generateApp() {
        return new MobileAppInfo(RND.nextLong(),
                UUID.randomUUID().toString(),
            "app",
            UUID.randomUUID().toString(),
                MobilePlatform.ANY,
                RND.nextBoolean(),
                new Date());
    }

    protected NotificationEventStatus createCancelledEventAndProcess(PushTemplateType templateType) {
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
                .pushFromUid(1L, templateType)
                .addTemplateParam(NotificationEventDataName.ORDER_ID, ORDER_ID.toString())
                .build());

        return pushSimpleConsumer.processEvent(event).getStatus();
    }

    protected Order createOrder() {
        Order order = MarketMailerMockFactory.generateOrder();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.CANCELLED);
        order.setShopOrderId(SHOP_ORDER_ID);
        order.setRgb(Color.WHITE);
        return order;
    }

    protected void setupCheckouterClient(Order order) {
        when(checkouterService.getOrder(anyLong(), any(ClientRole.class), anyLong(), anyBoolean())).thenReturn(null);
        when(checkouterService.getOrder(eq(order.getId()), eq(ClientRole.SYSTEM), anyLong(), anyBoolean())).thenReturn(order);
    }
}
