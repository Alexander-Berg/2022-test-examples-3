package ru.yandex.chemodan.directory.client;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

@Ignore
@ContextConfiguration(classes = {DirectoryClientIntegrationTestConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class DirectoryClientIntegrationTest {
    @Autowired
    private DirectoryClient directoryClient;

    @Test
    public void testUsersInOrganizationWithDefaultPerPage() {
        Assert.equals(
                Cf.set(1130000001389650L, 1130000001389652L),
                directoryClient.usersInOrganization("108", true, 20, Option.empty()).unique()
        );
    }

    @Test
    public void testUsersInOrganizationWithSmallPerPage() {
        Assert.equals(
                55,
                directoryClient.usersInOrganization("73", true, 100, Option.empty()).unique().size()
        );
        Assert.equals(
                55,
                directoryClient.usersInOrganization("73", true, 10, Option.empty()).unique().size()
        );
    }

    @Test
    public void testFilterRobots() {
        Assert.equals(
                60,
                directoryClient.usersInOrganization("73", false, 20, Option.empty()).unique().size()
        );
        Assert.equals(
                55,
                directoryClient.usersInOrganization("73", true, 20, Option.empty()).unique().size()
        );
    }

    @Test
    public void testGetOrganizationsWhereUserIsAdmin() {
        Assert.equals(
                Cf.set("103981", "103982"),
                directoryClient.organizationsWhereUserIsAdmin("4037336209", 10).unique()
        );
    }

    @Test
    public void testGetOrganizationsWhereUserIsAdminWithSmallPerPage() {
        Assert.equals(
                Cf.set("103981", "103982"),
                directoryClient.organizationsWhereUserIsAdmin("4037336209", 1).unique()
        );
    }

    @Test
    public void testGetOrganizationsFeaturesActive() {
        DirectoryOrganizationFeaturesResponse features = directoryClient.getOrganizationFeatures("108");
        Assert.isTrue(
                features.isEdu360Active()
        );
        Assert.isTrue(
                features.isDisablePsbillingProcessingActive()
        );
    }

    @Test
    public void testGetOrganizationsFeaturesDisabled() {
        DirectoryOrganizationFeaturesResponse features = directoryClient.getOrganizationFeatures("73");
        Assert.isFalse(
                features.isEdu360Active()
        );
        Assert.isFalse(
                features.isDisablePsbillingProcessingActive()
        );
    }

    @Test
    public void testGetOrganizationsFeaturesNotFound() {
        Assert.assertThrows(
                () -> directoryClient.getOrganizationFeatures("-1"),
                OrganizationNotFoundException.class
        );
    }

    @Test
    public void testGetOrganizationById() {
        DirectoryOrganizationByIdResponse org = directoryClient.getOrganizationById("108");
        Assert.equals(
                "common",
                org.getOrganizationType()
        );
    }

    @Test
    public void testGetUserInfo() {
        PassportUid uid = PassportUid.cons(4093541324L);
        DirectoryUsersInfoResponse userInfo = directoryClient.getUserInfo(uid, "110021").get();
        Assert.equals(uid.getUid(), userInfo.getId());
        Assert.isTrue(userInfo.isAdmin());
    }
}
