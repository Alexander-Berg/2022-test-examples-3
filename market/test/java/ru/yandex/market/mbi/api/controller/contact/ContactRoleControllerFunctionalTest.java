package ru.yandex.market.mbi.api.controller.contact;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.mbi.api.config.FunctionalTest;

/**
 * Функциональный тест для {@link ContactRoleController}.
 */
public class ContactRoleControllerFunctionalTest extends FunctionalTest {

    private static final long UID = 1248L;
    private static final long PARTNER_ID = 774L;

    @Test
    @DbUnitDataSet(before = "csv/ContactRoleControllerFunctionalTest.checkContactRoleSuccess.csv")
    void checkContactRoleSuccess() {
        boolean hasAdminRole = mbiApiClient.checkContactRole(UID, PARTNER_ID, InnerRole.SHOP_ADMIN);
        boolean hasTechnicalRole = mbiApiClient.checkContactRole(UID, PARTNER_ID, InnerRole.SHOP_TECHNICAL);
        boolean hasOperatorRole = mbiApiClient.checkContactRole(UID, PARTNER_ID, InnerRole.SHOP_OPERATOR);
        Assertions.assertFalse(hasAdminRole);
        Assertions.assertTrue(hasTechnicalRole);
        Assertions.assertFalse(hasOperatorRole);
    }

    @Test
    void checkContactRoleWrongClient() {
        boolean hasAdminRole = mbiApiClient.checkContactRole(UID, PARTNER_ID, InnerRole.SHOP_ADMIN);
        boolean hasTechnicalRole = mbiApiClient.checkContactRole(UID, PARTNER_ID, InnerRole.SHOP_TECHNICAL);
        boolean hasOperatorRole = mbiApiClient.checkContactRole(UID, PARTNER_ID, InnerRole.SHOP_OPERATOR);
        Assertions.assertFalse(hasAdminRole);
        Assertions.assertFalse(hasTechnicalRole);
        Assertions.assertFalse(hasOperatorRole);
    }

    @Test
    @DbUnitDataSet(before = "csv/ContactRoleControllerFunctionalTest.checkContactRoleWrongCampaign.csv")
    void checkContactRoleWrongCampaign() {
        boolean hasAdminRole = mbiApiClient.checkContactRole(UID, PARTNER_ID, InnerRole.SHOP_ADMIN);
        boolean hasTechnicalRole = mbiApiClient.checkContactRole(UID, PARTNER_ID, InnerRole.SHOP_TECHNICAL);
        boolean hasOperatorRole = mbiApiClient.checkContactRole(UID, PARTNER_ID, InnerRole.SHOP_OPERATOR);
        Assertions.assertFalse(hasAdminRole);
        Assertions.assertFalse(hasTechnicalRole);
        Assertions.assertFalse(hasOperatorRole);
    }
}
