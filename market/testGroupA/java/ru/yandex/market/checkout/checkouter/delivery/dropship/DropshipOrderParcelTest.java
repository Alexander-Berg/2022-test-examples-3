package ru.yandex.market.checkout.checkouter.delivery.dropship;

import java.util.Collections;
import java.util.List;

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
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.ParcelBoxHelper;
import ru.yandex.market.checkout.test.providers.ParcelBoxItemProvider;
import ru.yandex.market.checkout.test.providers.ParcelBoxProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

public class DropshipOrderParcelTest extends AbstractWebTestBase {

    @Autowired
    private DropshipDeliveryHelper dropshipDeliveryHelper;
    @Autowired
    private ParcelBoxHelper parcelBoxHelper;

    private Order order;
    private OrderItem item;

    private long shopId;
    private long parcelId;

    @BeforeEach
    public void setUp() throws Exception {
        order = dropshipDeliveryHelper.createDropshipOrder();
        assertThat(OrderTypeUtils.isFulfilment(order), is(false));
        assertThat(OrderTypeUtils.isMarketDelivery(order), is(true));
        shopId = order.getShopId();
        item = order.getItems().iterator().next();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        parcelId = order.getDelivery().getParcels().get(0).getId();
    }

    @Test
    public void shouldAllowToAddParcelBox() throws Exception {
        List<ParcelBox> boxes = parcelBoxHelper.putBoxes(
                order.getId(), parcelId, Collections.singletonList(
                        ParcelBoxProvider.buildBox(
                                ParcelBoxItemProvider.parcelBoxItem(item.getId(), item.getCount())
                        )
                ), new ClientInfo(ClientRole.SHOP, shopId)
        );

        assertThat(boxes, hasSize(1));
        assertThat(boxes.get(0).getItems(), hasSize(1));
    }

    @Test
    public void shouldAllowToUpdateParcelBox() throws Exception {
        List<ParcelBox> boxes = parcelBoxHelper.putBoxes(
                order.getId(), parcelId, Collections.singletonList(
                        ParcelBoxProvider.buildBox(
                                ParcelBoxItemProvider.parcelBoxItem(item.getId(), item.getCount())
                        )
                ), new ClientInfo(ClientRole.SHOP, shopId)
        );

        assertThat(boxes, hasSize(1));
        assertThat(boxes.get(0).getItems(), hasSize(1));

        List<ParcelBox> updated = parcelBoxHelper.putBoxes(
                order.getId(), parcelId, Collections.singletonList(
                        ParcelBoxProvider.buildBox(
                                ParcelBoxItemProvider.parcelBoxItem(item.getId(), item.getCount() - 1)
                        )
                ), new ClientInfo(ClientRole.SHOP, shopId)
        );

        assertThat(updated, hasSize(1));
        assertThat(updated.get(0).getItems(), hasSize(1));
        assertThat(updated.get(0).getItems().get(0).getCount(), is(item.getCount() - 1));
    }
}
