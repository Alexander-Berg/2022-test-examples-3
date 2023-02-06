package ru.yandex.market.checkout.checkouter.delivery.dropship;

import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.ParcelBoxHelper;
import ru.yandex.market.checkout.test.providers.ParcelBoxItemProvider;
import ru.yandex.market.checkout.test.providers.ParcelBoxProvider;
import ru.yandex.market.checkout.util.matching.Matchers;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class DropshipOrderFreezeTest extends AbstractWebTestBase {

    @Autowired
    private DropshipDeliveryHelper dropshipDeliveryHelper;
    @Autowired
    private ParcelBoxHelper parcelBoxHelper;
    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    private Order order;
    private OrderItem item;
    private long parcelId;

    @BeforeEach
    public void setUp() throws Exception {
        stockStorageConfigurer.mockOkForUnfreeze();
        order = dropshipDeliveryHelper.createDropshipOrder();
        parcelId = order.getDelivery().getParcels().iterator().next().getId();
        item = order.getItems().iterator().next();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        List<ParcelBox> boxes = parcelBoxHelper.putBoxes(
                order.getId(), parcelId,
                Collections.singletonList(
                        ParcelBoxProvider.buildBox(
                                ParcelBoxItemProvider.parcelBoxItem(item.getId(), item.getCount())
                        )
                ), new ClientInfo(ClientRole.SHOP, order.getShopId())
        );

        assertThat(boxes, hasSize(1));
        assertThat(boxes.get(0).getItems(), hasSize(1));
    }

    @Test
    public void shouldRunUnfreezeOrder() {
        stockStorageConfigurer.resetRequests();
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PROCESSING, OrderSubstatus.SHIPPED);

        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();
        assertThat(events, IsCollectionWithSize.hasSize(1));
        ServeEvent serveEvent = events.get(0);
        assertThat(serveEvent.getRequest().getMethod(), CoreMatchers.is(RequestMethod.DELETE));
        assertThat(serveEvent.getRequest().getUrl(), Matchers.matchesPattern("/order/\\d+\\?cancel=false"));
    }

    @Test
    public void shouldRunUnfreezeOrderOnCancelFromProcessingStarted() {
        stockStorageConfigurer.resetRequests();
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.SHOP_FAILED);

        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();
        assertThat(events, IsCollectionWithSize.hasSize(1));
        ServeEvent serveEvent = events.get(0);
        assertThat(serveEvent.getRequest().getMethod(), CoreMatchers.is(RequestMethod.DELETE));
        assertThat(serveEvent.getRequest().getUrl(), Matchers.matchesPattern("/order/\\d+\\?cancel=false"));
    }

    @Test
    public void shouldRunUnfreezeOrderOnCancelFromAnySubstatus() {
        stockStorageConfigurer.resetRequests();
        client.updateOrderStatus(order.getId(), ClientRole.SYSTEM, null, null, OrderStatus.PROCESSING,
                OrderSubstatus.PACKAGING);

        order = orderService.getOrder(order.getId());
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.SHOP_FAILED);


        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();
        assertThat(events, IsCollectionWithSize.hasSize(1));
        ServeEvent serveEvent = events.get(0);
        assertThat(serveEvent.getRequest().getMethod(), CoreMatchers.is(RequestMethod.DELETE));
        assertThat(serveEvent.getRequest().getUrl(), Matchers.matchesPattern("/order/\\d+\\?cancel=false"));
    }
}
