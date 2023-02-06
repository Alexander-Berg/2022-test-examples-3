package ru.yandex.market.loyalty.core.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.security.AdminRole;
import ru.yandex.market.loyalty.core.model.security.AdminUser;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.test.SameCollection.sameCollectionInAnyOrder;

/**
 * @author dinyat
 * 11/09/2017
 */
public class SecurityDaoTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private SecurityDao securityDao;

    @Test
    public void testSaveAndGetUser() {
        String userLogin = "user_login";
        securityDao.addRole(userLogin, AdminRole.COUPON_GENERATOR_ROLE);

        AdminUser user = securityDao.getUserByLogin(userLogin);

        assertEquals(userLogin, user.getLogin());
        assertThat(user.getEffectiveRoles(), sameCollectionInAnyOrder(Arrays.asList(
                AdminRole.COUPON_GENERATOR_ROLE, AdminRole.VIEWER_ROLE
        )));
    }

    @Test
    public void testSaveAndGetSuperuser() {
        String userLogin = "user_login";
        securityDao.addRole(userLogin, AdminRole.SUPERUSER_ROLE);

        AdminUser user = securityDao.getUserByLogin(userLogin);

        assertEquals(userLogin, user.getLogin());
        assertThat(user.getEffectiveRoles(), sameCollectionInAnyOrder(Arrays.asList(AdminRole.values())));
    }
}
