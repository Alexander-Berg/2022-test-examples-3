package ru.yandex.market.checkout.helpers;

import java.util.Date;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.checkouter.storage.OrderEntityGroup;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.storage.Storage;

/**
 * Вспомогательный класс для создания заказов в БД, минуя вызов стандартного /checkout.
 * Использовать в прямых наследниках AbstractServicesTest.
 * Использовать с умом и осторожно.
 */
public class OrderInsertHelper {

    private final OrderWritingDao orderWritingDao;
    private final Storage storage;

    public OrderInsertHelper(Storage storage, OrderWritingDao orderWritingDao) {
        this.storage = storage;
        this.orderWritingDao = orderWritingDao;
    }

    public void insertOrder(final long orderId, Order order) {
        insertOrder(orderId, order, OrderStatus.PROCESSING);
    }

    public long insertOrder(Order order) {
        return insertOrder(order, OrderStatus.PROCESSING);
    }

    public long insertOrder(Order order, OrderStatus status) {
        final long orderId = System.currentTimeMillis();
        insertOrder(orderId, order, status);
        return orderId;
    }

    private void insertOrder(final long orderId, Order order, OrderStatus status) {
        order.setId(orderId);
        order.setUserGroup(UserGroup.DEFAULT);
        order.setStatus(status);
        final Date now = new Date();
        order.setCreationDate(now);
        order.setUpdateDate(now);
        order.setStatusUpdateDate(now);
        order.setSubstatusUpdateDate(now);
        order.setStatusExpiryDate(now);
        storage.createEntityGroup(new OrderEntityGroup(orderId), () ->
                orderWritingDao.insertOrder(order, ClientInfo.SYSTEM));
    }
}
