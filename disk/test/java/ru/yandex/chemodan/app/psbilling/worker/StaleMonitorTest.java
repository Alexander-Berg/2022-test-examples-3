package ru.yandex.chemodan.app.psbilling.worker;

import java.math.BigDecimal;
import java.util.Currency;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.PsBillingBalanceFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingOrdersFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.config.Settings;
import ru.yandex.chemodan.app.psbilling.core.dao.cards.TrustCardBindingDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.OrderDao;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardBindingStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureScope;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderType;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.worker.monitor.PsBillingStaleRecordsMonitor;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class StaleMonitorTest extends AbstractWorkerTest {
    @Autowired
    protected Settings settings;
    @Autowired
    protected PsBillingBalanceFactory psBillingBalanceFactory;
    @Autowired
    protected PsBillingStaleRecordsMonitor psBillingStaleRecordsMonitor;
    @Autowired
    protected ClientBalanceDao clientBalanceDao;
    @Autowired
    protected TrustCardBindingDao trustCardBindingDao;
    @Autowired
    protected PsBillingOrdersFactory psBillingOrdersFactory;

    @Autowired
    protected OrderDao orderDao;

    @Test
    public void groupService() {
        GroupService groupService = psBillingGroupsFactory
                .createGroupService(psBillingGroupsFactory.createGroup(),
                        psBillingProductsFactory.createGroupProduct());
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleGroupServices());

        DateUtils.shiftTime(PsBillingStaleRecordsMonitor.STALE_PERIOD.plus(1));

        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(1, psBillingStaleRecordsMonitor.getStaleGroupServices());

        groupServiceDao.setStatusActual(Cf.list(groupService.getId()), Target.ENABLED);

        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleGroupServices());

        groupServiceDao.setTargetToDisabled(groupService.getId(), Target.ENABLED);
        DateUtils.shiftTime(PsBillingStaleRecordsMonitor.STALE_PERIOD.plus(1));
        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(1, psBillingStaleRecordsMonitor.getStaleGroupServices());

        psBillingStaleRecordsMonitor.resetState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleGroupServices());
    }

    @Test
    public void groupServiceMember() {
        GroupService groupService = psBillingGroupsFactory
                .createGroupService(psBillingGroupsFactory.createGroup(),
                        psBillingProductsFactory.createGroupProduct());
        psBillingGroupsFactory.createGroupServiceMember(groupService);
        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleGroupServiceMembers());

        DateUtils.shiftTime(PsBillingStaleRecordsMonitor.STALE_PERIOD.plus(1));

        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(1, psBillingStaleRecordsMonitor.getStaleGroupServiceMembers());

        psBillingStaleRecordsMonitor.resetState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleGroupServiceMembers());
    }

    @Test
    public void userService() {
        UserProductEntity product = psBillingProductsFactory.createUserProduct();
        psBillingUsersFactory.createUserService(product);

        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleUserServices());

        DateUtils.shiftTime(PsBillingStaleRecordsMonitor.STALE_PERIOD.plus(1));

        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(1, psBillingStaleRecordsMonitor.getStaleUserServices());

        psBillingStaleRecordsMonitor.resetState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleUserServices());
    }

    @Test
    public void userServiceFeature() {
        ProductFeatureEntity productFeature = psBillingProductsFactory.createProductFeature(
                psBillingProductsFactory.createUserProduct().getId(),
                psBillingProductsFactory.createFeature(FeatureType.ADDITIVE));
        psBillingUsersFactory.createUserServiceFeature(productFeature);

        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleUserServiceFeatures());

        DateUtils.shiftTime(PsBillingStaleRecordsMonitor.STALE_PERIOD.plus(1));

        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(1, psBillingStaleRecordsMonitor.getStaleUserServiceFeatures());

        psBillingStaleRecordsMonitor.resetState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleUserServiceFeatures());
    }

    @Test
    public void groupServiceFeatures() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        ProductFeatureEntity productFeature = psBillingProductsFactory.createProductFeature(
                product.getUserProductId(),
                psBillingProductsFactory.createFeature(FeatureType.ADDITIVE), x -> x.scope(FeatureScope.GROUP));
        psBillingGroupsFactory.createGroupServiceFeature(group, productFeature);

        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleGroupServiceFeatures());

        DateUtils.shiftTime(PsBillingStaleRecordsMonitor.STALE_PERIOD.plus(1));

        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(1, psBillingStaleRecordsMonitor.getStaleGroupServiceFeatures());

        psBillingStaleRecordsMonitor.resetState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleGroupServiceFeatures());
    }

    @Test
    public void staleCardBindings() {
        trustCardBindingDao.insert(TrustCardBindingDao.InsertData.builder()
                .operatorUid(PassportUid.cons(42L))
                .status(CardBindingStatus.INIT)
                .build());

        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleCardBindings());

        DateUtils.shiftTime(settings.getStaleCardBindingIntervalHours().plus(Duration.standardHours(1)));

        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(1, psBillingStaleRecordsMonitor.getStaleCardBindings());

        psBillingStaleRecordsMonitor.resetState();
        Assert.equals(0, psBillingStaleRecordsMonitor.getStaleCardBindings());
    }

    @Test
    public void staleBalances() {
        assertBalanceStaleCount(0);

        createBalance(1L, "RUB");
        createBalance(1L, "USD");
        createBalance(2L, "RUB");
        createBalance(3L, "RUB");
        assertBalanceStaleCount(0);

        DateUtils.shiftTime(settings.getBalanceStaleIntervalHours()
                .plus(Duration.standardMinutes(psBillingStaleRecordsMonitor.staleBalanceDelayMinutes.get())).plus(1));
        assertBalanceStaleCount(4);
    }

    private void createUserService(OrderType subscription, UserProductEntity userProduct) {
        Order orderSub = psBillingOrdersFactory.createOrder(PsBillingUsersFactory.UID, subscription);
        UserServiceEntity userServiceSub = psBillingUsersFactory.createUserService(userProduct.getId(), builder -> {
            builder.paidByOrderId(Option.of(orderSub.getId()))
                    .uid(orderSub.getUid())
                    .nextCheckDate(Option.of(Instant.now()));
            return builder;
        }, Target.ENABLED);
        orderDao.onSuccessfulOrderPurchase(orderSub.getId(), Option.of(userServiceSub.getId()), 1);
    }

    private void createBalance(Long clientId, String currency) {
        clientBalanceDao.createOrUpdate(clientId, Currency.getInstance(currency), BigDecimal.valueOf(0),
                Option.empty());
    }

    private void assertBalanceStaleCount(int count) {
        psBillingStaleRecordsMonitor.updateState();
        Assert.equals(count, psBillingStaleRecordsMonitor.getStaleBalances());
    }
}
