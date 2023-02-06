package ru.yandex.chemodan.app.psbilling.core.groups;

import java.math.BigDecimal;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.SendLocalizedEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.inside.passport.PassportUid;

public class GroupServicesManagerTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupServicesManager groupServicesManager;
    @Autowired
    private ProductSetDao productSetDao;
    @Autowired
    private ProductLineDao productLineDao;
    @Autowired
    private BazingaTaskManagerMock bazingaTaskManagerMock;

    @Test
    public void sendB2bUpsaleEmailsProductsChoice() {
        // Current product
        GroupProduct product1 = psBillingProductsFactory.createProductWithCommonEmailFeatures(BigDecimal.valueOf(1_000L), 1024);
        // Same disk space, but higher price - pass
        GroupProduct product2 = psBillingProductsFactory.createProductWithCommonEmailFeatures(BigDecimal.valueOf(10_000L), 1024);
        // Higher disk space, higher price - just what the doctor ordered
        GroupProduct product3 = psBillingProductsFactory.createProductWithCommonEmailFeatures(BigDecimal.valueOf(15_000L), 2048);
        // Higher disk space, higher price, but higher than product3 - pass
        GroupProduct product4 = psBillingProductsFactory.createProductWithCommonEmailFeatures(BigDecimal.valueOf(20_000L), 4096);

        psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product1, product2, product3, product4));
        Group group = psBillingGroupsFactory.createGroup(b -> b.ownerUid(PassportUid.cons(1)));
        psBillingGroupsFactory.createGroupService(group, product1);

        groupServicesManager.sendB2bUpsaleEmails(Instant.now());

        AssertHelper.assertSize(bazingaTaskManagerMock.findTasks(SendLocalizedEmailTask.class), 1);
        SendLocalizedEmailTask task = bazingaTaskManagerMock.findTasks(SendLocalizedEmailTask.class).first()._1;
        Assert.assertEquals(product3.getUserProductId(), task.getParametersTyped().getContext().getUserProductId().get());
    }

    @Test
    public void sendNoB2bUpsaleEmailWhenTimeIsUnappropriated() {
        GroupProduct product1 = psBillingProductsFactory.createProductWithCommonEmailFeatures(BigDecimal.valueOf(42_000_000L), 1);
        GroupProduct product2 = psBillingProductsFactory.createProductWithCommonEmailFeatures(BigDecimal.valueOf(84_000_000L), 2);

        psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product1, product2));
        Group group = psBillingGroupsFactory.createGroup(b -> b.ownerUid(PassportUid.cons(1)));
        psBillingGroupsFactory.createGroupService(group, product1);

        // It's too late
        groupServicesManager.sendB2bUpsaleEmails(Instant.now().minus(Duration.standardMinutes(2)));
        AssertHelper.assertSize(bazingaTaskManagerMock.findTasks(SendLocalizedEmailTask.class), 0);

        // It's too early
        groupServicesManager.sendB2bUpsaleEmails(Instant.now().plus(Duration.standardHours(4)));
        AssertHelper.assertSize(bazingaTaskManagerMock.findTasks(SendLocalizedEmailTask.class), 0);
    }
}
