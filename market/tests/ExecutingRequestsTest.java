package ru.yandex.autotests.market.checkouter.tests;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.autotests.market.checkouter.beans.DataForLoad;
import ru.yandex.autotests.market.checkouter.beans.Delivery;
import ru.yandex.autotests.market.checkouter.beans.OrderEvent;
import ru.yandex.autotests.market.checkouter.beans.OrderHistory;
import ru.yandex.autotests.market.checkouter.beans.Status;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataItem;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;
import ru.yandex.autotests.market.checkouter.dao.CheckouterStorageJdbcDao;
import ru.yandex.autotests.market.notifier.beans.api.notify.delivery.NotificationDelivery;
import ru.yandex.autotests.market.notifier.beans.api.notify.inbox.NotificationInbox;

import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * Created with IntelliJ IDEA.
 * User: suok
 * Date: 29.07.13
 * Time: 15:18
 * To change this template use File | Settings | File Templates.
 */
public class ExecutingRequestsTest {

    private static final Logger LOG = Logger.getLogger(ExecutingRequestsTest.class);

    private CheckouterStorageJdbcDao storageDao;

    @Before
    public void init() {
        storageDao = CheckouterStorageJdbcDao.getInstance();
    }

    @Test
    public void testGetRealPayments() {
        List<Integer> payments = storageDao.getRealPayments();
        Assert.assertThat(payments, hasSize(greaterThan(0)));
    }

    @Test
    public void testGetFakePayments() {
        List<Integer> payments = storageDao.getFakePayments();
        Assert.assertThat(payments, hasSize(greaterThan(0)));
    }

    @Test
    public void testGetUnpaidOrders() {
        List<DataForLoad> orders = storageDao.getUnpaidOrdersWithoutPayment();
        Assert.assertThat(orders, hasSize(greaterThan(0)));
    }

    @Test
    public void testGetOrderBuyer() {
        storageDao.getOrderBuyers(42l);
    }

    @Test
    public void testGetOrderDelivery() {
        List<Delivery> deliveries = storageDao.getOrderDelivery(144l);
        LOG.debug(deliveries);
    }

    @Test
    public void testGetDeliveryAddress() {
        storageDao.getDeliveryAddress(42l);
    }

    @Test
    public void testGetOrderItem() {
        List<TestDataItem> items = storageDao.getOrderItems(42l);
    }

    @Test
    public void testGetNotificationInbox() {
        storageDao.getNotificationsInbox(37615);
    }

    @Test
    public void testGetNotificationsDelivery() {
        List<NotificationDelivery> deliveries = storageDao.getNotificationsDelivery(39481);
        LOG.debug(deliveries);
    }

    @Test
    public void testGetNotificationsInboxAfterTime() {
        List<NotificationInbox> inboxes = storageDao.getNotificationsInboxAfterTime("05-09-2013 16:11:00");
        LOG.debug(inboxes);
    }

    @Test
    public void testOrdersQuery() {
        List<TestDataOrder> orders = storageDao.getOrders(144l);
        LOG.debug(orders);
    }

    @Test
    public void testGetOrdersHistory() {
        List<OrderHistory> orders = storageDao.getOrderHistory(144l, 162l);
        LOG.debug(orders);
    }

    @Test
    public void testGetOrdersHistoryWithEventId() {
        List<OrderHistory> orders = storageDao.getOrderHistoryWithEventId(144);
        LOG.debug(orders);
    }

    @Test
    public void testGetLastOrdersHistory() {
        List<OrderHistory> orders = storageDao.getLastOrderHistory(600, 108l);
        //LOG.debug(orders);
    }

    //205, 206
    @Test
    public void testGetHistoryOrders() {
        List<TestDataOrder> orders = storageDao.getHistoryOrdersSqlQuery(206, 205);
        LOG.debug(orders);
    }

    @Test
    public void testGetOrderEvent() {
        List<OrderEvent> events = storageDao.getOrderEvent();
        LOG.debug(events);
    }

    @Test
    public void testGetOrderEventWithIdMoreThan() {
        List<OrderEvent> events = storageDao.getOrderEventWithIdMoreThan(207, 10);
        LOG.debug(events);
    }

    @Test
    public void testGetOrdersBetweenDates() {
        String fromDate = "2013-08-28 16:36:01";
        String toDate = "2013-08-28 20:28:00";
        List<TestDataOrder> orders = storageDao.getOrdersBetweenDates( fromDate, toDate );
        LOG.debug(orders);
    }

    @Test
    public void testGetOrdersByShopIdBetweenDates() {
        String fromDate = "2013-08-28 16:36:01";
        String toDate = "2013-08-28 20:28:00";
        long shopId = 3828l;
        List<TestDataOrder> orders = storageDao.
                getOrdersByShopIdBetweenDates( fromDate, toDate, shopId);
        LOG.debug(orders);
        Status status = Status.PICKUP;
        orders = storageDao.
                getOrdersByShopIdBetweenDates(fromDate, toDate, shopId, status);
        LOG.debug(orders);
    }

    @Test
    public void testGetOrdersByUserIdBetweenDates() {
        String fromDate = "2013-08-28 16:36:01";
        String toDate = "2013-08-28 20:28:00";
        long userId = 666;
        List<TestDataOrder> orders = storageDao.
                getOrdersByUserIdBetweenDates(fromDate, toDate, userId);
        LOG.debug(orders);
    }

    @Test
    public void testGetOrdersWithStatusBetweenDates() {
        String fromDate = "29-08-2013";
        String toDate = "30-08-2013";
        Status status = Status.PROCESSING;
        List<TestDataOrder> orders = storageDao.
                getOrdersBetweenDates( fromDate, toDate);
        LOG.debug(orders);
    }

}
