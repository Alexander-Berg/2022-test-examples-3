package ru.yandex.chemodan.app.psbilling.core;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.users.OrderDao;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderType;
import ru.yandex.inside.passport.PassportUid;

public class PsBillingOrdersFactory {
    @Autowired
    private OrderDao orderDao;
    @Autowired
    private PsBillingProductsFactory productsFactory;

    public Order createOrder(PassportUid uid, OrderType type) {
        UserProductPriceEntity userProductPrices = productsFactory.createUserProductPrice();
        return createOrUpdateOrder(uid, userProductPrices.getId(), (int) (Math.random() * 10000) + "",
                builder -> builder
                        .status(Option.of(OrderStatus.PAID))
                        .type(type));
    }

    public Order createOrder(PassportUid uid) {
        return createOrder(uid, OrderStatus.PAID);
    }

    public Order createOrder(PassportUid uid, OrderStatus status) {
        UserProductPriceEntity userProductPrices = productsFactory.createUserProductPrice();
        return createOrUpdateOrder(uid, userProductPrices.getId(), (int) (Math.random() * 10000) + "",
                builder -> builder.status(Option.of(status)));
    }

    public Order createOrUpdateOrder(PassportUid userId, UUID priceId, String trustOrderId) {
        return createOrUpdateOrder(userId, priceId, trustOrderId, x -> x);
    }

    public Order createOrUpdateInappOrder(PassportUid userId, UUID priceId, String trustOrderId) {
        return createOrUpdateInappOrder(userId, priceId, trustOrderId, x -> x);
    }

    public Order createOrUpdateOrder(PassportUid userId, UUID priceId, String trustOrderId,
                                     java.util.function.Function<OrderDao.InsertData.InsertDataBuilder,
                                             OrderDao.InsertData.InsertDataBuilder> customizer) {
        return orderDao.createOrUpdate(
                customizer.apply(OrderDao.InsertData.builder()
                                .uid(userId.toString())
                                .trustServiceId(116)
                                .trustOrderId(trustOrderId)
                                .userProductPriceId(priceId)
                                .type(OrderType.SUBSCRIPTION)
                                .packageName(Option.empty()))
                        .build());
    }

    public Order createOrUpdateInappOrder(PassportUid userId, UUID priceId, String trustOrderId,
                                          java.util.function.Function<OrderDao.InsertData.InsertDataBuilder,
                                                  OrderDao.InsertData.InsertDataBuilder> customizer) {
        return orderDao.createOrUpdate(
                customizer.apply(OrderDao.InsertData.builder()
                                .uid(userId.toString())
                                .trustServiceId(116)
                                .trustOrderId(trustOrderId)
                                .userProductPriceId(priceId)
                                .type(OrderType.INAPP_SUBSCRIPTION)
                                .packageName(Option.of("ru.yandex.disk")))
                        .build());
    }

    public Order createOrUpdateOrder(Order order, java.util.function.Function<OrderDao.InsertData.InsertDataBuilder,
            OrderDao.InsertData.InsertDataBuilder> customizer) {
        return orderDao.createOrUpdate(
                customizer.apply(OrderDao.InsertData.builder()
                                .uid(order.getUid())
                                .trustServiceId(order.getTrustServiceId())
                                .trustOrderId(order.getTrustOrderId())
                                .userProductPriceId(order.getUserProductPriceId())
                                .type(order.getType())
                                .packageName(order.getPackageName()))
                        .build());
    }
}
