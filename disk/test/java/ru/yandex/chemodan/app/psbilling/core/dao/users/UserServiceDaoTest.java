package ru.yandex.chemodan.app.psbilling.core.dao.users;

import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingOrdersFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceBillingStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.users.UserService;
import ru.yandex.chemodan.app.psbilling.core.users.UserServiceManager;
import ru.yandex.inside.passport.PassportUid;

public class UserServiceDaoTest extends AbstractPsBillingCoreTest {
    private static PassportUid userId = PassportUid.MAX_VALUE;

    @Autowired
    PsBillingOrdersFactory psBillingOrdersFactory;
    @Autowired
    PsBillingUsersFactory psBillingUsersFactory;
    @Autowired
    UserServiceDao userServiceDao;
    @Autowired
    UserServiceManager userServiceManager;

    @Test
    public void findNotSynchronizedAt_ReturnsNeverSynchronized() {
        findNotSynchronizedAtImpl(null, true);
    }

    @Test
    public void findNotSynchronizedAt_ReturnsLateSynchronized() {
        findNotSynchronizedAtImpl(new Instant(0), true);
    }

    @Test
    public void findNotSynchronizedAt_DontReturnRecentlySynchronized() {
        findNotSynchronizedAtImpl(Instant.now(), false);
    }

    private void findNotSynchronizedAtImpl(Instant syncTime, boolean foundExpected) {
        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct(x -> x.billingType(BillingType.INAPP_GOOGLE));
        UserProductPrice price = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(userProduct, CustomPeriodUnit.TEN_MINUTES).getId());
        Order order = psBillingOrdersFactory.createOrUpdateOrder(userId, price.getId(), "innapp_order");
        UserService userService = userServiceManager.createUserService(order, Instant.now(), UserServiceBillingStatus.PAID);
        orderDao.updateInappSyncDate(order.getId(), syncTime);
        ListF<UserServiceEntity> services = userServiceDao.findNotSynchronizedAt(Option.empty(), 10, Instant.now());
        if (foundExpected) {
            Assert.assertEquals(1, services.size());
            Assert.assertEquals(services.get(0).getId(), userService.getId());
        } else {
            Assert.assertTrue(services.isEmpty());
        }
    }
}
