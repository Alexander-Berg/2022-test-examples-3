package ru.yandex.chemodan.app.psbilling.web.validation;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupPartnerDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.web.BaseWebTest;
import ru.yandex.chemodan.util.exception.A3ExceptionWithStatus;
import ru.yandex.misc.test.Assert;

public class GroupValidatorTest extends BaseWebTest {
    @Autowired
    private GroupValidator groupValidator;

    @Autowired
    private GroupPartnerDao groupPartnerDao;


    @Test
    public void testOrganizationIsNotPartnerClientGroupNotFound() {
        groupValidator.checkOrganizationIsNotPartnerClient(uid, "123");
    }

    @Test
    public void testOrganizationIsNotPartnerClient() {
        Group group = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(1, uid)));

        groupValidator.checkOrganizationIsNotPartnerClient(uid, group.getExternalId());
    }

    @Test
    public void testGroupIsNotPartnerClient() {
        Group group = psBillingGroupsFactory.createGroup();

        groupValidator.checkIsNotPartnerClient(group);
    }

    @Test
    public void testOrganizationIsPartnerClient() {
        Group group = createPartnerClient();

        Assert.assertThrows(
                () -> groupValidator.checkOrganizationIsNotPartnerClient(uid,
                        group.getExternalId()),
                A3ExceptionWithStatus.class
        );
    }

    @Test
    public void testGroupIsPartnerClient() {
        Group group = createPartnerClient();

        Assert.assertThrows(
                () -> groupValidator.checkIsNotPartnerClient(group),
                A3ExceptionWithStatus.class
        );
    }

    private Group createPartnerClient() {
        Group group = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(1, uid)));
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), "123");
        return group;
    }
}
