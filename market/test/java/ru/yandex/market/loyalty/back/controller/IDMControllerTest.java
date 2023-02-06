package ru.yandex.market.loyalty.back.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.back.idm.IDMController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.security.AdminRole;
import ru.yandex.market.loyalty.core.service.SecurityService;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.back.idm.IDMController.SLUG;
import static ru.yandex.market.loyalty.core.model.security.AdminRole.COUPON_PROMO_EDITOR_ROLE;
import static ru.yandex.market.loyalty.core.model.security.AdminRole.PROMO_3P_EDITOR_ROLE;
import static ru.yandex.market.loyalty.core.model.security.AdminRole.VIEWER_ROLE;

/**
 * Doesn't use objectMapper 'cause need to known exact formation of response.
 * For example we must return "roles": [ {"group": "admin"}, {"group": "manager"}, ] structure in get-all-roles
 */
@TestFor(IDMController.class)
public class IDMControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SecurityService securityService;
    private static final String LOGIN = "frodo";

    @Test
    public void shouldReturnListOfAvailableRoles() throws Exception {
        String response = mockMvc
                .perform(get("/idm/info/"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(response, startsWith("{" +
                "\"code\":0," +
                "\"roles\":{" +
                "\"slug\":\"" + SLUG + "\"," +
                "\"name\":\"Группа\"," +
                "\"values\":{" +
                "\"" + VIEWER_ROLE.getCode() + "\":\"" + VIEWER_ROLE.getDescription() + "\""
        ));
    }

    @Test
    public void shouldAddRole() throws Exception {
        addRole(LOGIN, VIEWER_ROLE);

        assertThat(
                securityService.getUser(LOGIN).getAdminRoles(),
                hasItem(VIEWER_ROLE)
        );
    }

    @Test
    public void shouldAddSecondRole() throws Exception {
        addRole(LOGIN, COUPON_PROMO_EDITOR_ROLE);

        assertThat(
                securityService.getUser(LOGIN).getAdminRoles(),
                allOf(
                        hasItem(COUPON_PROMO_EDITOR_ROLE),
                        not(hasItem(PROMO_3P_EDITOR_ROLE))
                )
        );

        addRole(LOGIN, PROMO_3P_EDITOR_ROLE);

        assertThat(
                securityService.getUser(LOGIN).getAdminRoles(),
                allOf(
                        hasItem(COUPON_PROMO_EDITOR_ROLE),
                        hasItem(PROMO_3P_EDITOR_ROLE)
                )
        );
    }

    @Test
    public void shouldRemoveRole() throws Exception {
        addRole(LOGIN, COUPON_PROMO_EDITOR_ROLE);
        addRole(LOGIN, PROMO_3P_EDITOR_ROLE);
        removeRole(LOGIN, PROMO_3P_EDITOR_ROLE);

        assertThat(
                securityService.getUser(LOGIN).getAdminRoles(),
                allOf(
                        hasItem(COUPON_PROMO_EDITOR_ROLE),
                        not(hasItem(PROMO_3P_EDITOR_ROLE))
                )
        );
    }

    @Test
    public void shouldReturnAllUsers() throws Exception {
        addRole(LOGIN, VIEWER_ROLE);
        addRole(LOGIN, PROMO_3P_EDITOR_ROLE);

        String response = mockMvc
                .perform(get("/idm/get-all-roles/"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(response, equalTo("{" +
                "\"code\":0," +
                "\"users\":[{" +
                "\"login\":\"" + LOGIN + "\"," +
                "\"roles\":[" +
                "{\"" + SLUG + "\":\"" + VIEWER_ROLE.getCode() + "\"}," +
                "{\"" + SLUG + "\":\"" + PROMO_3P_EDITOR_ROLE.getCode() + "\"}" +
                "]" +
                "}]}"
        ));
    }

    @Test
    public void shouldRemoveUserIfLastRoleWasRemoved() throws Exception {
        addRole(LOGIN, PROMO_3P_EDITOR_ROLE);
        removeRole(LOGIN, PROMO_3P_EDITOR_ROLE);

        assertNull(securityService.getUser(LOGIN));
    }

    private void addRole(String login, AdminRole role) throws Exception {
        String response = mockMvc
                .perform(
                        post("/idm/add-role/")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .content(
                                        "login=" + login +
                                                "&role={\"" + SLUG + "\": \"" + role.getCode() + "\"}" +
                                                "&path=/" + SLUG + "/" + role.getCode() + "/"
                                )
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(response, equalTo("{\"code\":0}"));
    }

    private void removeRole(String login, AdminRole role) throws Exception {
        String response = mockMvc
                .perform(
                        post("/idm/remove-role/")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .content(
                                        "login=" + login +
                                                "&role={\"" + SLUG + "\": \"" + role.getCode() + "\"}" +
                                                "&path=/" + SLUG + "/" + role.getCode() + "/"
                                )
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(response, equalTo("{\"code\":0}"));
    }

}
