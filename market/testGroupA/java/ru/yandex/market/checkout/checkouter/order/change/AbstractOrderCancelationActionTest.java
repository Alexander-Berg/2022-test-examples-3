package ru.yandex.market.checkout.checkouter.order.change;

import java.util.Collections;
import java.util.Stack;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.service.StockStorageServiceImpl;
import ru.yandex.market.checkout.checkouter.storage.OrderEntityGroup;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.storage.Storage;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.ParcelItemProvider;
import ru.yandex.market.checkout.test.providers.ParcelProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageFreezeNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;


public abstract class AbstractOrderCancelationActionTest extends AbstractWebTestBase {

    @Autowired
    protected StockStorageServiceImpl stockStorageService;
    @Autowired
    protected StockStorageOrderClient stockStorageOrderClient;
    @Autowired
    protected OrderWritingDao writingDao;
    @Autowired
    protected Storage storage;
    protected Stack<Order> created = new Stack<>();
    @Autowired
    private OrderInsertHelper orderInsertHelper;

    @Override
    @AfterEach
    public void clean() {
        stockStorageService.setStockStorageOrderClient(stockStorageOrderClient);
        storage.deleteEntityGroup(new OrderEntityGroup(ClientInfo.SYSTEM.getId()), () -> {
            created.forEach(o -> writingDao.dropOrderUnfreezeStocks(o.getId()));
            return null;
        });
    }

    StockStorageOrderClient mockStockServiceClient(BiConsumer<String, Boolean> unfreezeHandler)
            throws StockStorageFreezeNotFoundException {
        StockStorageOrderClient service = mock(StockStorageOrderClient.class);
        doAnswer(invocation -> {
            unfreezeHandler.accept(invocation.getArgument(0), invocation.getArgument(1));
            return true;
        }).when(service).unfreezeStocks(any(String.class), anyBoolean());
        return service;
    }

    Order generateOrder(OrderStatus initialStatus) {
        Order order = OrderProvider.getFulfilmentOrder();
        OrderItem item = OrderItemProvider.buildOrderItem("qwerty", 1);
        item.setFitFreezed(1);
        item.setFulfilmentWarehouseId(DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED);
        order.setContext(Context.MARKET);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        order.setItems(Collections.singletonList(item));
        created.add(order);
        if (OrderStatus.PROCESSING == initialStatus) {
            Parcel parcel = ParcelProvider.createParcel(ParcelItemProvider.buildParcelItem(134, 2));
            parcel.setStatus(ParcelStatus.ERROR);
            Track track = TrackProvider.createTrack();
            track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
            parcel.addTrack(track);
            order.getDelivery().addParcel(parcel);
        }
        orderInsertHelper.insertOrder(order, initialStatus);
        return order;
    }
}
