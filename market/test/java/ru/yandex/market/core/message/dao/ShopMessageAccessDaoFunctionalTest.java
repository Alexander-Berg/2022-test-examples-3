package ru.yandex.market.core.message.dao;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.core.message.model.AgencyMessageAccess;
import ru.yandex.market.core.message.model.ContactShopMessageRoles;

/**
 * Тесты для {@link ru.yandex.market.core.message.dao.impl.ShopMessageAccessDaoImpl}
 */
@DbUnitDataSet(before = "ShopMessageAccessDaoFunctionalTest.before.csv")
public class ShopMessageAccessDaoFunctionalTest extends FunctionalTest {

    private static final long AGENCY_SHOP_ID = 32L;
    private static final long AGENCY_ID = 10L;
    private static final long CLIENT_ID = 25L;
    private static final long SHOP_ID_1 = 31L;
    private static final long SHOP_ID_2 = 33L;
    private static final long SHOP_ID_3 = 34L;

    @Autowired
    ShopMessageAccessDao shopMessageAccessDao;

    @Test
    void testGetMessageForShopAdminContact() {
        Assertions.assertEquals(
                Set.of(
                        new ContactShopMessageRoles(SHOP_ID_1,
                                Set.of(InnerRole.SHOP_TECHNICAL.getCode(), InnerRole.SHOP_ADMIN.getCode()))
                ),
                shopMessageAccessDao.getMessageRolesForContact(3L)
        );
    }

    @Test
    void testGetMessageForBusinessOwnerContact() {
        Assertions.assertEquals(
                Set.of(
                        new ContactShopMessageRoles(SHOP_ID_2,
                                Set.of(InnerRole.SHOP_ADMIN.getCode())),
                        new ContactShopMessageRoles(SHOP_ID_3,
                                Set.of(InnerRole.SHOP_ADMIN.getCode()))
                ),
                shopMessageAccessDao.getMessageRolesForContact(5L)
        );
    }

    @Test
    void testGetMessageForBusinessAdminContact() {
        Assertions.assertEquals(
                Set.of(
                        new ContactShopMessageRoles(SHOP_ID_2,
                                Set.of(InnerRole.SHOP_ADMIN.getCode())),
                        new ContactShopMessageRoles(SHOP_ID_3,
                                Set.of(InnerRole.SHOP_ADMIN.getCode()))
                ),
                shopMessageAccessDao.getMessageRolesForContact(6L)
        );
    }

    @Test
    void testGetMessageForBusinessAndShopAdminContact() {
        Assertions.assertEquals(
                Set.of(
                        new ContactShopMessageRoles(SHOP_ID_2,
                                Set.of(InnerRole.SHOP_ADMIN.getCode())),
                        new ContactShopMessageRoles(SHOP_ID_3,
                                Set.of(InnerRole.SHOP_ADMIN.getCode()))
                ),
                shopMessageAccessDao.getMessageRolesForContact(7L)
        );
    }

    @Test
    void testGetMessageAccessForAgency() {
        Assertions.assertEquals(Set.of(
                new AgencyMessageAccess(AGENCY_SHOP_ID, CLIENT_ID)
        ), shopMessageAccessDao.getMessageAccessForAgency(AGENCY_ID));
    }
}
