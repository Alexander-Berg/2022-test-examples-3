package ru.yandex.chemodan.app.psbilling.core.dao.groups.impl;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupPartnerDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;

public class GroupPartnerDaoImplTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupPartnerDao groupPartnerDao;

    @Test
    public void testInsert() {
        String partnerExternalId = "5678";

        Group group = psBillingGroupsFactory.createGroup();

        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), partnerExternalId);
        Assert.assertEquals(partnerExternalId, groupPartnerDao.getPartnerId(group.getId()).get());
    }

    @Test
    public void testSecondInsert() {
        String partnerExternalId = "5678";
        String partnerExternalIdUpdated = "9876";

        Group group = psBillingGroupsFactory.createGroup();

        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), partnerExternalId);
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), partnerExternalIdUpdated);
        Assert.assertEquals(partnerExternalId, groupPartnerDao.getPartnerId(group.getId()).get());
    }

    @Test
    public void testUpdate() {
        String partnerExternalId = "5678";
        String partnerExternalIdUpdated = "9876";

        Group group = psBillingGroupsFactory.createGroup();

        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), partnerExternalId);
        groupPartnerDao.updatePartnerInfo(group.getId(), partnerExternalIdUpdated);
        Assert.assertEquals(partnerExternalIdUpdated, groupPartnerDao.getPartnerId(group.getId()).get());
    }
}
