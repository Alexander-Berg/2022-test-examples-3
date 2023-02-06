package ru.yandex.market.pers.notify.ems.consumer;

import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource.PushBuilder;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.push.MobileAppInfoDAO;

/**
 * @author valter
 */
public class SendPushTest extends SendNotificationTestCommons {
    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;

    @BeforeEach
    public void addMobileAppInfo() {
        Order cancelledOrder = getCancelledOrder(OrderSubstatus.SHOP_FAILED);
        setCancellationChangeRequest(cancelledOrder);

        MobileAppInfo info = new MobileAppInfo(
                UID, UUID_STR,  "ru.yandex.market", "push_token", MobilePlatform.ANDROID, false
        );
        info.setLoginTime(new Date());
        mobileAppInfoDAO.add(info);
    }

    @Test
    public void testPUSH_STORE_COIN_ACTIVATED() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_STORE_COIN_ACTIVATED);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_STORE_LAVKA() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_STORE_LAVKA);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_STORE_PREPAID_PENDING() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_STORE_PREPAID_PENDING);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_STORE_PREPAID_DELAY() throws Exception {
        testWithShopDelivery(NotificationSubtype.PUSH_STORE_PREPAID_DELAY);
    }

    @Test
    public void testPUSH_STORE_POSTPAID_PENDING() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_STORE_POSTPAID_PENDING);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_STORE_PICKUP_PREPAID() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_STORE_PICKUP_PREPAID);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_STORE_PICKUP_POSTPAID() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_STORE_PICKUP_POSTPAID);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_STORE_DELIVERY_PREPAID() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_STORE_DELIVERY_PREPAID);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_STORE_DELIVERY_POSTPAID() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_STORE_DELIVERY_POSTPAID);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_STORE_DELIVERY_READY_FOR_LAST_MILE() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_STORE_DELIVERY_READY_FOR_LAST_MILE);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_STORE_DELIVERED() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_STORE_DELIVERED);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_STORE_CANCELLED_USER_REFUSED_PREPAID() throws Exception {
        testWithShopDelivery(NotificationSubtype.PUSH_STORE_CANCELLED_USER_REFUSED_PREPAID);
    }

    @Test
    public void testPUSH_STORE_CANCELLED_USER_REFUSED_POSTPAID() throws Exception {
        testWithShopDelivery(NotificationSubtype.PUSH_STORE_CANCELLED_USER_REFUSED_POSTPAID);
    }

    @Test
    public void testPUSH_STORE_CANCELLED_USER_NOT_PAID() throws Exception {
        testWithShopDelivery(NotificationSubtype.PUSH_STORE_CANCELLED_USER_NOT_PAID);
    }

    @Test
    public void testPUSH_STORE_CANCELLED_SHOP_FAILED_PREPAID() throws Exception {
        testWithShopDelivery(NotificationSubtype.PUSH_STORE_CANCELLED_SHOP_FAILED_PREPAID);
    }

    @Test
    public void testPUSH_STORE_CANCELLED_SHOP_FAILED_POSTPAID() throws Exception {
        testWithShopDelivery(NotificationSubtype.PUSH_STORE_CANCELLED_SHOP_FAILED_POSTPAID);
    }

    @Test
    public void testPUSH_STORE_CANCELLED_PENDING_CANCELLED_PREPAID() throws Exception {
        testWithShopDelivery(NotificationSubtype.PUSH_STORE_CANCELLED_PENDING_CANCELLED_PREPAID);
    }

    @Test
    public void testPUSH_STORE_CANCELLED_PENDING_CANCELLED_POSTPAID() throws Exception {
        testWithShopDelivery(NotificationSubtype.PUSH_STORE_CANCELLED_PENDING_CANCELLED_POSTPAID);
    }

    @Test
    public void testPUSH_TRANSBOUNDARY_TRADING_GENERIC() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_GENERIC);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_TRANSBOUNDARY_TRADING_ADVERTISING() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_ADVERTISING);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_TRANSBOUNDARY_TRADING_TRANSACTION() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_TRANSACTION);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_TRANSBOUNDARY_TRADING_TRIGGER() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_TRIGGER);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_YAPLUS_DELIVERED_CASH_BACK() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_YAPLUS_DELIVERED_CASH_BACK);
        sendAndCheck(id);
    }

    @Test
    public void testPUSH_STORE_UNPAID_WAITING_USER_DELIVERY_INPUT() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.PUSH_STORE_UNPAID_WAITING_USER_DELIVERY_INPUT);
        sendAndCheck(id);
    }

    private void testWithShopDelivery(NotificationSubtype notificationSubtype) throws Exception {
        testWithShopDelivery(notificationSubtype, null);
        testWithShopDelivery(notificationSubtype, false);
        testWithShopDelivery(notificationSubtype, true);
    }

    private void testWithShopDelivery(NotificationSubtype notificationSubtype, Boolean isShopDelivery) throws Exception {
        long id = scheduleAndCheck(notificationSubtype, isShopDelivery);
        sendAndCheck(id);
    }

    protected long scheduleAndCheck(NotificationSubtype type, Boolean isShopDelivery) {
        NotificationEventSource source = schedulers.get(type).get();

        if (isShopDelivery != null) {
            source.getData().put(
                PushBuilder.getTemplateParamName(NotificationEventDataName.SHOP_DELIVERY),
                String.valueOf(isShopDelivery)
            );
        }
        return scheduleAndCheck(source);
    }
}
