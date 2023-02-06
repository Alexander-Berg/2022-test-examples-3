package ru.yandex.market.admin.service.remote;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.contact.UIContact;
import ru.yandex.market.admin.ui.model.contact.UIContactLinkBusiness;
import ru.yandex.market.admin.ui.model.contact.UIContactLinkCampaign;
import ru.yandex.market.admin.ui.model.contact.UIContactRole;
import ru.yandex.market.admin.ui.model.shop.UIContactEmail;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;

class RemoteContactUIServiceTest extends FunctionalTest {
    @Autowired
    private RemoteContactUIService remoteContactUIService;
    @Autowired
    private PassportService passportService;

    @Test
    @DbUnitDataSet(
            before = "RemoteContactUIServiceTest.csv",
            after = "RemoteContactUIServiceTest.testUpdateRolesWithExistedLink.after.csv"
    )
    void testUpdateRolesWithExistedLink() {
        remoteContactUIService.updateContactRoles(100L, 10L, Set.of(UIContactRole.ADMIN), true);
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteContactUIServiceTest.csv",
            after = "RemoteContactUIServiceTest.updateRolesWithoutExistedLink.after.csv"
    )
    void testUpdateRolesWithoutExistedLink() {
        remoteContactUIService.updateContactRoles(100L, 20L, Set.of(UIContactRole.ADMIN), false);
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteContactUIServiceTest.csv",
            after = "RemoteContactUIServiceTest.csv"
    )
    void testUpdateOwnerException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> remoteContactUIService.updateContactRoles(100L, 20L, Set.of(UIContactRole.BUSINESS_OWNER),
                        false));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "RemoteContactUIServiceTest.csv",
                    "RemoteContactUIServiceTest.testUpdateOwner.before.csv"
            },
            after = "RemoteContactUIServiceTest.testUpdateOwner.after.csv"
    )
    void testUpdateOwner() {
        remoteContactUIService.updateContactRoles(110L, 20L,
                Set.of(UIContactRole.BUSINESS_OWNER, UIContactRole.BUSINESS_ADMIN),
                false);
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteContactUIServiceTest.csv",
            after = "RemoteContactUIServiceTest.csv"
    )
    void testImportPassportAlreadyImported() {
        UIContact expected = new UIContact();
        expected.setField(UIContact.ID, 10L);
        expected.setField(UIContact.USER_ID, 100L);
        expected.setField(UIContact.BALANCE_ACCESS, true);
        expected.setField(UIContact.LOGIN, "NEW_LOGIN");
        expected.setField(UIContact.NAME, "NEW");
        expected.setField(UIContact.EMAILS, new HashSet<>());
        expected.setField(UIContact.BALANCE_CLIENT_ID, null);
        expected.setField(UIContact.MARKET_CLIENT_IDS, new HashSet<>());
        UIContactLinkCampaign link = new UIContactLinkCampaign();
        link.setCampaignId(100L);
        link.setRoles(new HashSet<>(Set.of(UIContactRole.TECHNICAL)));
        expected.setField(UIContact.LINKS, new HashSet<>(Set.of(link)));

        UIContact contact = remoteContactUIService.importContactFromPassport(100L);
        ReflectionAssert.assertReflectionEquals(expected, contact);
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteContactUIServiceTest.csv",
            after = "RemoteContactUIServiceTest.csv"
    )
    void testImportPassportBusiness() {
        UIContact expected = new UIContact();
        expected.setField(UIContact.ID, 30L);
        expected.setField(UIContact.USER_ID, 300L);
        expected.setField(UIContact.BALANCE_ACCESS, true);
        expected.setField(UIContact.LOGIN, "NEW_LOGIN_BUSINESS");
        expected.setField(UIContact.NAME, "NEW_BUSINESS");
        expected.setField(UIContact.EMAILS, new HashSet<>());
        expected.setField(UIContact.BALANCE_CLIENT_ID, null);
        expected.setField(UIContact.MARKET_CLIENT_IDS, new HashSet<>());
        UIContactLinkBusiness link = new UIContactLinkBusiness();
        link.setBusinessId(1000L);
        link.setRoles(new HashSet<>(Set.of(UIContactRole.BUSINESS_OWNER)));
        link.setCampaignId(300L);
        expected.setField(UIContact.LINKS, new HashSet<>(Set.of(link)));

        UIContact contact = remoteContactUIService.importContactFromPassport(300L);
        ReflectionAssert.assertReflectionEquals(expected, contact);
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteContactUIServiceTest.csv",
            after = "RemoteContactUIServiceTest.importPassportNotImported.after.csv"
    )
    void testImportPassportNotImported() {
        UIContact expected = new UIContact();
        expected.setField(UIContact.ID, 1L);
        expected.setField(UIContact.USER_ID, 400L);
        expected.setField(UIContact.BALANCE_ACCESS, false);
        expected.setField(UIContact.LOGIN, "testLogin");
        expected.setField(UIContact.NAME, "- test");
        expected.setField(UIContact.BALANCE_CLIENT_ID, null);
        expected.setField(UIContact.MARKET_CLIENT_IDS, new HashSet<>());
        expected.setField(UIContact.LINKS, new HashSet<>());
        UIContactEmail email = new UIContactEmail();
        email.setField(UIContactEmail.ID, 1);
        email.setField(UIContactEmail.EMAIL, "testLogin@yandex.ru");
        email.setField(UIContactEmail.ACTIVE, true);
        expected.setField(UIContact.EMAILS, new HashSet<>(Set.of(email)));

        Mockito.when(passportService.getUserInfo(ArgumentMatchers.eq(400L)))
                .thenReturn(new UserInfo(400L, "test", "test@ya.ru", "testLogin"));
        UIContact contact = remoteContactUIService.importContactFromPassport(400L);
        ReflectionAssert.assertReflectionEquals(expected, contact);
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteContactUIServiceTest.businessOwner.before.csv",
            after = "RemoteContactUIServiceTest.businessOwner.after.csv"
    )
    void testUnbindLastBusinessOwner() {
        remoteContactUIService.unbindContactFromCampaigns(400L, List.of(10200L));
    }

    @Test
    @DbUnitDataSet(before = "RemoteContactUIServiceTest.businessOwner.before.csv")
    void testCanUnbindBusinessOwner_singleBusiness() {
        boolean actual = remoteContactUIService.canUnbindBusinessOwner(400L, List.of(200L));
        Assertions.assertTrue(actual);
    }

    @Test
    @DbUnitDataSet(before = "RemoteContactUIServiceTest.businessOwner.before.csv")
    void testCanUnbindBusinessOwner_notOwner() {
        boolean actual = remoteContactUIService.canUnbindBusinessOwner(20L, List.of(200L));
        Assertions.assertFalse(actual);
    }

    @Test
    @DbUnitDataSet(before = {
            "RemoteContactUIServiceTest.businessOwner.before.csv",
            "RemoteContactUIServiceTest.canUnbind.extraContact.before.csv"
    })
    void testCanUnbindBusinessOwner_withAdmin() {
        boolean actual = remoteContactUIService.canUnbindBusinessOwner(400L, List.of(200L));
        Assertions.assertFalse(actual);
    }

    @Test
    @DbUnitDataSet(before = {
            "RemoteContactUIServiceTest.businessOwner.before.csv",
            "RemoteContactUIServiceTest.canUnbind.withShops.before.csv"
    })
    void testCanUnbindBusinessOwner_withShops() {
        boolean actual = remoteContactUIService.canUnbindBusinessOwner(400L, List.of(200L));
        Assertions.assertFalse(actual);
    }
}
