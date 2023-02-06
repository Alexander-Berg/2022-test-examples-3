package ru.yandex.market.crm.campaign.http.controller;

import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.crm.campaign.domain.page.PageContext;
import ru.yandex.market.crm.campaign.domain.security.Permission;
import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;
import ru.yandex.market.crm.http.security.BlackboxProfile;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
public class PageContextControllerTest extends AbstractControllerMediumTest {

    private static final String LOGIN = "user";

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private UsersRolesDao usersRolesDao;

    @Test
    public void testGetPermissionsForUserWithOperatorRole() throws Exception {
        BlackboxProfile profile = SecurityUtils.profile(LOGIN);

        usersRolesDao.addRole(profile.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.ADMIN));

        SecurityUtils.setAuthentication(profile);

        MvcResult result = mockMvc.perform(get("/api/page/context"))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        PageContext pageContext = jsonDeserializer.readObject(
                PageContext.class,
                result.getResponse().getContentAsByteArray()
        );

        Assertions.assertEquals(profile.getLogin(), pageContext.getLogin());

        Set<Permission> permissions = pageContext.getPermissions();
        Assertions.assertNotNull(permissions);
        Assertions.assertTrue(permissions.contains(Permission.ADMINISTERING));
    }
}
