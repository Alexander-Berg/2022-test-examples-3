package ru.yandex.chemodan.app.psbilling.core.dao.users;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderType;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.lang.impl.DefaultObjectReflectionCache;
import ru.yandex.misc.test.Assert;

public class OrderDaoTests extends AbstractPsBillingCoreTest {
    @Autowired
    private OrderDao orderDao;

    private UserProductPrice price;

    @Test
    public void findByUid() {
        PassportUid uid1 = PassportUid.cons(1);
        PassportUid uid2 = PassportUid.cons(2);

        ListF<Order> orders = orderDao.findByUid(uid1);
        Assert.assertTrue(orders.isEmpty());

        createOrder(uid1);
        createOrder(uid1);
        createOrder(uid2);

        orders = orderDao.findByUid(uid1);
        Assert.assertEquals(2, orders.length());

        orders = orderDao.findByUid(uid2);
        Assert.assertEquals(1, orders.length());
    }

    @Test
    public void createWithSameTrustOrderIdIgnoreConflict() {
        String trustOrderId = UUID.randomUUID().toString();

        Order o1 = orderDao.create(
                OrderDao.InsertData.builder()
                        .uid(uid.toString())
                        .trustServiceId(116)
                        .trustOrderId(trustOrderId)
                        .userProductPriceId(price.getId())
                        .type(OrderType.SUBSCRIPTION)
                        .build());

        Assert.assertThrows(
                () -> orderDao.create(OrderDao.InsertData.builder()
                        .uid(uid.toString())
                        .trustServiceId(116)
                        .trustOrderId(trustOrderId)
                        .userProductPriceId(price.getId())
                        .type(OrderType.SUBSCRIPTION)
                        .build()),
                DataIntegrityViolationException.class);
    }


    @Test
    public void createWithOtherTrustOrderIdIgnoreConflict() {
        Order o1 = orderDao.create(
                OrderDao.InsertData.builder()
                        .uid(uid.toString())
                        .trustServiceId(116)
                        .trustOrderId(UUID.randomUUID().toString())
                        .userProductPriceId(price.getId())
                        .type(OrderType.SUBSCRIPTION)
                        .build());

        Order o2 = orderDao.create(OrderDao.InsertData.builder()
                .uid(uid.toString())
                .trustServiceId(116)
                .trustOrderId(UUID.randomUUID().toString())
                .userProductPriceId(price.getId())
                .type(OrderType.SUBSCRIPTION)
                .build());


        Assert.assertNotEquals(o1.getId(), o2.getId());
        Assert.assertNotEquals(o1.getTrustOrderId(), o2.getTrustOrderId());

        Assert.assertEquals(o1.getUid(), o2.getUid());
        Assert.assertEquals(o1.getTrustServiceId(), o2.getTrustServiceId());
        Assert.assertEquals(o1.getUserProductPriceId(), o2.getUserProductPriceId());
        Assert.assertEquals(o1.getType(), o2.getType());
    }

    @Test
    public void createIfNotExistsWithSameTrustOrderIdIgnoreConflict() {
        String trustOrderId = UUID.randomUUID().toString();

        Order o1 = orderDao.createIfNotExists(
                OrderDao.InsertData.builder()
                        .uid(uid.toString())
                        .trustServiceId(116)
                        .trustOrderId(trustOrderId)
                        .userProductPriceId(price.getId())
                        .type(OrderType.SUBSCRIPTION)
                        .build());

        Order o2 = orderDao.createIfNotExists(OrderDao.InsertData.builder()
                .uid(uid.toString())
                .trustServiceId(116)
                .trustOrderId(trustOrderId)
                .userProductPriceId(price.getId())
                .type(OrderType.SUBSCRIPTION)
                .build());

        Assert.assertTrue(DefaultObjectReflectionCache.get(Order.class).equals(o1, o2));
    }

    @Test
    public void createIfNotExistsWithOtherTrustOrderIdIgnoreConflict() {
        Order o1 = orderDao.createIfNotExists(
                OrderDao.InsertData.builder()
                        .uid(uid.toString())
                        .trustServiceId(116)
                        .trustOrderId(UUID.randomUUID().toString())
                        .userProductPriceId(price.getId())
                        .type(OrderType.SUBSCRIPTION)
                        .build());

        Order o2 = orderDao.createIfNotExists(OrderDao.InsertData.builder()
                .uid(uid.toString())
                .trustServiceId(116)
                .trustOrderId(UUID.randomUUID().toString())
                .userProductPriceId(price.getId())
                .type(OrderType.SUBSCRIPTION)
                .build());

        Assert.assertFalse(DefaultObjectReflectionCache.get(Order.class).equals(o1, o2));
    }

    @Before
    public void Setup() {
        UserProductEntity product = psBillingProductsFactory.createUserProduct(builder -> {
            builder.billingType(BillingType.TRUST);
            builder.code("TEST");
            builder.allowAutoProlong(true);
            builder.titleTankerKeyId(psBillingTextsFactory.create().getId());
            builder.singleton(false);
            builder.trustServiceId(Option.of(111));
            builder.trustSubsChargingRetryDelay(Option.of("1D"));
            builder.trustSubsChargingRetryLimit(Option.of("2D"));
            builder.trustSubsGracePeriod(Option.of("2D"));

            return builder;
        });

        price = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(product, CustomPeriodUnit.TEN_MINUTES).getId());
    }

    private Order createOrder(PassportUid uid) {
        return orderDao.createOrUpdate(OrderDao.InsertData.builder()
                .uid(uid.toString())
                .trustServiceId(116)
                .trustOrderId(UUID.randomUUID().toString())
                .userProductPriceId(price.getId())
                .type(OrderType.SUBSCRIPTION)
                .build());
    }
}
