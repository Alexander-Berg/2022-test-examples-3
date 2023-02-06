package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.billing.BillingProduct;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.chemodan.app.djfs.core.user.Organization;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class QuotaManagerTest extends DjfsSingleUserTestBase {
    @Test
    public void freeConnectOrganization() {
        userDao.setB2bKey(UID, "key");
        organizationDao.insert(Organization.builder().id("key").isPaid(false).quotaFree(1000).build());
        quotaManager.setLimit(UID, 100);
        diskInfoDao.setTotalUsed(UID, 2);
        Assert.equals(98L, quotaManager.getFree(UID, Option.empty()));
    }

    @Test
    public void paidConnectOrganization() {
        userDao.setB2bKey(UID, "key");
        organizationDao.insert(Organization.builder().id("key").isPaid(true).quotaFree(1000).build());
        quotaManager.setLimit(UID, 100);
        diskInfoDao.setTotalUsed(UID, 2);
        Assert.equals(1000L, quotaManager.getFree(UID, Option.empty()));
    }

    @Test
    public void paidConnectOrganizationWithNegativeFreeSpace() {
        userDao.setB2bKey(UID, "key");
        organizationDao.insert(Organization.builder().id("key").isPaid(true).quotaFree(-1000).build());
        quotaManager.setLimit(UID, 100);
        diskInfoDao.setTotalUsed(UID, 2);
        Assert.equals(0L, quotaManager.getFree(UID, Option.empty()));
    }

    @Test
    public void paidConnectOrganizationAndPaidServices() {
        userDao.setB2bKey(UID, "key");
        organizationDao.insert(Organization.builder().id("key").isPaid(true).quotaFree(1000).build());
        quotaManager.setLimit(UID, 100);
        diskInfoDao.setTotalUsed(UID, 2);
        billingManager.addProduct(UID, BillingProduct.builder().id("id").productId("test_1kb_eternal").uid(UID).build());
        Assert.equals(2022L, quotaManager.getFree(UID, Option.empty()));
    }

    @Test
    public void paidConnectOrganizationWithNegativeFreeSpaceAndPaidServices() {
        userDao.setB2bKey(UID, "key");
        organizationDao.insert(Organization.builder().id("key").isPaid(true).quotaFree(-1000).build());
        quotaManager.setLimit(UID, 100);
        diskInfoDao.setTotalUsed(UID, 2);
        billingManager.addProduct(UID, BillingProduct.builder().id("id").productId("test_1kb_eternal").uid(UID).build());
        Assert.equals(1022L, quotaManager.getFree(UID, Option.empty()));
    }
}
