package ru.yandex.market.crm.campaign.http.controller;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumWithoutYtTest;
import ru.yandex.market.crm.campaign.test.utils.AccountsTeslHelper;
import ru.yandex.market.crm.core.domain.mobile.MetricaMobileApp;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.mobile.features.Feature;
import ru.yandex.market.crm.core.test.utils.MobileAppsTestHelper;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
class MobileApplicationControllerTest extends AbstractControllerMediumWithoutYtTest {

    @Inject
    private MobileAppsTestHelper mobileAppsTestHelper;

    @Inject
    private AccountsTeslHelper accountsTeslHelper;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private UsersRolesDao usersRolesDao;

    @Test
    void testGetAllApplications() throws Exception {
        var testAppId = "test_app";
        var accountId = "accountId";
        mobileAppsTestHelper.insertApplication(testAppId, 111, YPath.cypressRoot(), List.of());
        accountsTeslHelper.prepareAccount(accountId, Set.of(testAppId));

        var user = SecurityUtils.profile("operator_profile");
        usersRolesDao.addRole(user.getUid(), new CompositeUserRole(accountId, Roles.OPERATOR));
        usersRolesDao.addRole(user.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR));

        SecurityUtils.setAuthentication(user);

        var result = mockMvc.perform(get("/api/mobile-applications"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        var apps = jsonDeserializer.readObject(
                new TypeReference<List<MobileApplication>>() {},
                result.getResponse().getContentAsString()
        );

        assertThat(apps, hasSize(2));

        var market = apps.get(0);
        assertEquals(MobileApplication.MARKET_APP, market.getId());
        assertNotNull(market.getName());
        assertEquals(MetricaMobileApp.BERU.getId(), market.getMetricaAppId());
        assertTrue(market.hasFeature(Feature.SUBSCRIPTIONS));
        assertTrue(market.hasFeature(Feature.GLOBAL_CONTROL));
        assertTrue(market.hasFeature(Feature.FREQUENCY_THROTTLING));
    }

    /**
     * Ручка получения мобильных приложений возвращает только те приложения, которые доступны в аккаунте пользователя
     */
    @Test
    void getAccessibleApplications() throws Exception {
        var testAppId = "test_app";
        var metricaAppId = 111;
        var accountId = "accountId";
        mobileAppsTestHelper.insertApplication(testAppId, metricaAppId, YPath.cypressRoot(), List.of());
        accountsTeslHelper.prepareAccount(accountId, Set.of(testAppId));

        var user = SecurityUtils.profile("operator_profile");
        usersRolesDao.addRole(user.getUid(), new CompositeUserRole(accountId, Roles.OPERATOR));

        SecurityUtils.setAuthentication(user);

        var result = mockMvc.perform(get("/api/mobile-applications"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        var apps = jsonDeserializer.readObject(
                new TypeReference<List<MobileApplication>>() {},
                result.getResponse().getContentAsString()
        );

        assertThat(apps, hasSize(1));

        var app = apps.get(0);
        assertEquals(testAppId, app.getId());
        assertNotNull(app.getName());
        assertEquals(metricaAppId, app.getMetricaAppId());
    }
}
