package ru.yandex.market.loyalty.admin.test;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.loyalty.admin.config.AdminTestConfig;
import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdminMockConfigurer.BlackboxClientConfig;
import ru.yandex.market.loyalty.admin.config.TestAuthorizationContext;
import ru.yandex.market.loyalty.core.service.SecurityService;
import ru.yandex.market.loyalty.core.service.datacamp.DataCampStrollerClient;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.test.TestCoverageRule;

import static ru.yandex.market.loyalty.core.model.security.AdminRole.SUPERUSER_ROLE;

/**
 * @author dinyat
 * 16/06/2017
 */
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = AdminTestConfig.class, properties = {
        "promo3p.notifications.enabled=true",
        "market.loyalty.yt.promo.bundles.urls=hahn:",
        "market.loyalty.yt.promo.personal.urls=",
        "market.loyalty.yt.promo.anaplan.urls=",
        "market.loyalty.yt.promo.autosets.urls=",
        "market.loyalty.yt.promo.promostorage.urls=",
        "market.loyalty.yt.fast.promos.urls="
})
public abstract class MarketLoyaltyAdminMockedDbTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Rule
    @Autowired
    public TestCoverageRule testCoverageRule;
    @Autowired
    protected SecurityService securityService;
    @Autowired
    protected TestAuthorizationContext authorizationContext;

    @Before
    public void initAuthorizationContext() {
        authorizationContext.setUserName(TestAuthorizationContext.DEFAULT_USER_NAME);
    }

    @Before
    public void initSuperUser() {
        securityService.addRole(BlackboxClientConfig.SUPERUSER_USER, SUPERUSER_ROLE);
    }
}
