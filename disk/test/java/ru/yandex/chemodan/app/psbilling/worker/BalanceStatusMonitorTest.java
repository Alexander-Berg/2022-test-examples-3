package ru.yandex.chemodan.app.psbilling.worker;

import java.math.BigDecimal;
import java.util.Currency;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.PsBillingBalanceFactory;
import ru.yandex.chemodan.app.psbilling.core.config.Settings;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.worker.monitor.BalanceStatusMonitor;
import ru.yandex.misc.test.Assert;

public class BalanceStatusMonitorTest extends AbstractWorkerTest {
    @Autowired
    protected Settings settings;
    @Autowired
    protected PsBillingBalanceFactory psBillingBalanceFactory;
    @Autowired
    protected BalanceStatusMonitor balanceStatusMonitor;
    @Autowired
    protected ClientBalanceDao clientBalanceDao;

    @Test
    public void prepaidDebtsCount() {
        balanceStatusMonitor.updateState();
        Assert.equals(0, balanceStatusMonitor.getPrepaidDebtsCount());

        GroupProduct postpaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));
        GroupProduct prepaidProductRub =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID));
        GroupProduct prepaidProductUsd =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .priceCurrency(Currency.getInstance("USD")));
        Group group1 = psBillingGroupsFactory.createGroup(1L);
        Group group2 = psBillingGroupsFactory.createGroup(2L);
        Group group3 = psBillingGroupsFactory.createGroup(3L);
        Group group4 = psBillingGroupsFactory.createGroup(4L);

        psBillingGroupsFactory.createGroupService(group1, prepaidProductRub);
        psBillingGroupsFactory.createGroupService(group1, prepaidProductUsd);

        psBillingGroupsFactory.createGroupService(group2, prepaidProductRub);
        psBillingGroupsFactory.createGroupService(group2, prepaidProductUsd);

        psBillingGroupsFactory.createGroupService(group3, prepaidProductRub);
        psBillingGroupsFactory.createGroupService(group4, postpaidProduct);

        psBillingBalanceFactory.createBalance(1, "RUB", 0);
        psBillingBalanceFactory.createBalance(1, "USD", 1);
        psBillingBalanceFactory.createBalance(2, "RUB", -0.1);
        psBillingBalanceFactory.createBalance(2, "USD", -1);
        psBillingBalanceFactory.createBalance(3, "RUB", -1);
        psBillingBalanceFactory.createBalance(4, "RUB", 1);

        balanceStatusMonitor.updateState();
        Assert.equals(3, balanceStatusMonitor.getPrepaidDebtsCount());
    }

    @Test
    public void mixpaidClientsCount() {
        balanceStatusMonitor.updateState();
        Assert.equals(0, balanceStatusMonitor.getMixpaidClientsCount());

        GroupProduct postpaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));
        GroupProduct prepaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID));

        Group group1 = psBillingGroupsFactory.createGroup(1L);
        Group group2 = psBillingGroupsFactory.createGroup(1L);
        Group group3 = psBillingGroupsFactory.createGroup(2L);

        psBillingGroupsFactory.createGroupService(group1, prepaidProduct);
        psBillingGroupsFactory.createGroupService(group2, postpaidProduct);

        balanceStatusMonitor.updateState();
        Assert.equals(1, balanceStatusMonitor.getMixpaidClientsCount());

        psBillingGroupsFactory.createGroupService(group3, prepaidProduct);
        psBillingGroupsFactory.createGroupService(group3, postpaidProduct);

        balanceStatusMonitor.updateState();
        Assert.equals(2, balanceStatusMonitor.getMixpaidClientsCount());
    }

    @Test
    public void mistakenlyActiveServiceCount() {
        Instant now = Instant.now();
        assertMistakenlyActiveServiceCount(0);

        Group group = psBillingGroupsFactory.createGroup(1L);
        GroupProduct prepaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID));
        GroupProduct postpaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));

        psBillingGroupsFactory.createGroupService(group, prepaidProduct);
        // no client balance
        assertMistakenlyActiveServiceCount(0);

        clientBalanceDao.createOrUpdate(1L, prepaidProduct.getPriceCurrency(), BigDecimal.ZERO, Option.empty());
        clientBalanceDao.createOrUpdate(1L, Currency.getInstance("USD"), BigDecimal.ZERO, Option.of(now));
        // balance void too soon
        assertMistakenlyActiveServiceCount(0);

        clientBalanceDao.createOrUpdate(1L, prepaidProduct.getPriceCurrency(), BigDecimal.ZERO, Option.of(now));
        DateUtils.freezeTime(now.plus(BalanceStatusMonitor.MISTAKENLY_ACTIVE_SERVICE_THRESHOLD.minus(1)));
        // balance void too soon
        assertMistakenlyActiveServiceCount(0);

        DateUtils.freezeTime(now.plus(BalanceStatusMonitor.MISTAKENLY_ACTIVE_SERVICE_THRESHOLD));
        // 1 prepaid service with rub product
        assertMistakenlyActiveServiceCount(1);

        DateUtils.freezeTime(now);
        psBillingGroupsFactory.createGroupService(group, prepaidProduct); // +1
        GroupService service = psBillingGroupsFactory.createGroupService(group, prepaidProduct);
        groupServiceDao.updateSkipTransactionsExport(service.getId(), true);
        psBillingGroupsFactory.createGroupService(group, postpaidProduct);

        DateUtils.freezeTime(now.plus(BalanceStatusMonitor.MISTAKENLY_ACTIVE_SERVICE_THRESHOLD));
        assertMistakenlyActiveServiceCount(2);
    }

    private void assertMistakenlyActiveServiceCount(int expectedCount) {
        balanceStatusMonitor.updateState();
        Assert.equals(expectedCount, balanceStatusMonitor.getMistakenlyActiveServiceCount());
    }
}
