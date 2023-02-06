package ru.yandex.market.core.contact.db;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

/**
 * Тесты для {@link BusinessOwnerDao}
 *
 * @author Daniil Ivanov storabic@yandex-team.ru
 */
@DbUnitDataSet(before = "BusinessOwnerDaoTest.before.csv")
class BusinessOwnerDaoTest extends FunctionalTest {

    private static final Long PARTNER_ID = 30L;
    private static final Long BUSINESS_OWNER_UID = 99L;
    private static final Long BUSINESS_ID = 10L;

    private static final Long TPL_PARTNER_ID = 32L;
    private static final Long TPL_SHOP_ADMIN_UID = 19L;

    @Autowired
    private BusinessOwnerDao businessOwnerDao;

    @Test
    void testPartnerIdToOwnerUid() {
        Map<Long, Long> result = businessOwnerDao.getPartnerIdToOwnerUid(List.of(PARTNER_ID));
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(BUSINESS_OWNER_UID, result.get(PARTNER_ID));
    }

    @Test
    void testOwnerUidByPartnerId() {
        Assertions.assertEquals(BUSINESS_OWNER_UID, businessOwnerDao.getOwnerUidByPartnerId(PARTNER_ID));
    }

    @Test
    void testOwnerContactIdByPartnerId() {
        Assertions.assertEquals(1L, businessOwnerDao.getOwnerContactIdByPartnerId(PARTNER_ID));
    }

    @Test
    void testOwnerWithoutBusiness() {
        Assertions.assertEquals(TPL_SHOP_ADMIN_UID, businessOwnerDao.getOwnerUidByPartnerId(TPL_PARTNER_ID));
    }

    @Test
    void testGetBusinessOwnerUid() {
        Assertions.assertEquals(BUSINESS_OWNER_UID, businessOwnerDao.getBusinessOwnerUid(BUSINESS_ID));
    }

    @Test
    void testGetBusinessOwnerUidWrongBusinessId() {
        Assertions.assertNull(businessOwnerDao.getBusinessOwnerUid(TPL_PARTNER_ID));
    }
}
