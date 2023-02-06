package ru.yandex.market.checkout.storage.shipment;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.checkouter.storage.shipment.ParcelWritingDao;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.storage.Storage;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParcelDaoTest extends AbstractServicesTestBase {

    @Autowired
    private ParcelWritingDao parcelWritingDao;
    @Autowired
    private OrderWritingDao orderWritingDao;
    @Autowired
    private OrderReadingDao orderReadingDao;
    @Autowired
    private Storage storage;

    private OrderInsertHelper orderInsertHelper;

    @PostConstruct
    void init() {
        orderInsertHelper = new OrderInsertHelper(storage, orderWritingDao);
    }

    @Test
    void parcelWithItemsPersistenceTest() {
        LocalDateTime shipmentDateTimeBySupplier = LocalDateTime.parse("2020-04-04T12:00:00");
        LocalDateTime receptionDateTimeByWarehouse = LocalDateTime.parse("2020-04-04T20:00:00");

        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(shipmentDateTimeBySupplier);
        parcel.setReceptionDateTimeByWarehouse(receptionDateTimeByWarehouse);

        ParcelItem item = new ParcelItem();
        item.setItemId(1L);
        item.setCount(1);
        item.setShipmentDateTimeBySupplier(shipmentDateTimeBySupplier);
        item.setReceptionDateTimeByWarehouse(receptionDateTimeByWarehouse);
        parcel.addParcelItem(item);

        Parcel createdParcel = saveParcel(parcel);
        ParcelItem createdItem = CollectionUtils.extractSingleton(createdParcel.getParcelItems());

        assertEquals(shipmentDateTimeBySupplier, createdParcel.getShipmentDateTimeBySupplier());
        assertEquals(receptionDateTimeByWarehouse, createdParcel.getReceptionDateTimeByWarehouse());
        assertEquals(shipmentDateTimeBySupplier, createdItem.getShipmentDateTimeBySupplier());
        assertEquals(receptionDateTimeByWarehouse, createdItem.getReceptionDateTimeByWarehouse());
    }

    private Parcel saveParcel(@Nonnull Parcel parcel) {
        Order order = OrderProvider.getBlueOrder();
        long orderId = orderInsertHelper.insertOrder(order);
        transactionTemplate.execute(t -> {
            parcelWritingDao.insertParcels(List.of(parcel), orderId, getDeliveryId(orderId), 1);
            return null;
        });
        order = orderReadingDao.getOrder(orderId, ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return CollectionUtils.extractSingleton(order.getDelivery().getParcels());
    }

    // Не нашел лучшего способа достать deliveryId для теста
    private long getDeliveryId(long orderId) {
        return (long) CollectionUtils.extractSingleton(
                masterJdbcTemplate.queryForList("SELECT id FROM order_delivery WHERE order_id = " + orderId))
                .get("id");
    }
}
