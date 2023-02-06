package ru.yandex.market.checkout.checkouter.pay;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.application.AbstractWebTestHelper;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.util.Has;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;

/**
 * @author mkasumov
 */
public abstract class AbstractPaymentTestHelper extends AbstractWebTestHelper {

    @Autowired
    protected OrderService orderService;
    @Autowired
    protected OrderUpdateService orderUpdateService;
    @Autowired
    protected TrustMockConfigurer trustMockConfigurer;
    @Autowired
    protected CheckouterFeatureReader checkouterFeatureReader;

    protected Has<Order> order;
    protected Has<ShopMetaData> shopMetaData;

    public AbstractPaymentTestHelper(AbstractWebTestBase test,
                                     Has<Order> order, Has<ShopMetaData> shopMetaData) {
        super(test);
        this.order = order;
        this.shopMetaData = shopMetaData;
    }

    protected Order order() {
        return order.get();
    }

    protected Long uid() {
        if (Boolean.TRUE.equals(order().isNoAuth())) {
            return null;
        }
        return order().getBuyer().getUid();
    }

    protected Order refreshOrder() {
        return order.set(orderService.getOrder(order().getId(), ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES)));
    }

    protected ShopMetaData shopMetaData() {
        return shopMetaData.get();
    }

    protected boolean isNewPrepayType() {
        return isNewPrepayType(order().getPayment());
    }

    public static boolean isNewPrepayType(Payment payment) {
        return payment.getPrepayType() == PrepayType.YANDEX_MARKET;
    }
}
