package ru.yandex.market.crm.campaign.http.controller;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;
import ru.yandex.market.crm.http.security.BlackboxProfile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
public class AdminControllerTest extends AbstractControllerMediumTest {

    @Inject
    private UsersRolesDao usersRolesDao;

    /**
     * Пользователь с ролью "Администратор" может получить настройки
     * мониторингов триггерных сообщений
     */
    @Test
    public void testUserWithAdminRoleCanGetMessageMonitoring() throws Exception {
        requestMonitorings()
                .andExpect(status().isOk());
    }

    /**
     * Пользователь, не обладающий ролью администратора, не может получить настройки
     * мониторингов триггерных сообщений
     * <p>
     * Проверка аннотации {@link ru.yandex.market.crm.campaign.http.security.PermissionsRequired},
     * повешенной на класс
     */
    @Test
    public void testUserWithoutAdminRoleCannotGetMessageMonitorings() throws Exception {
        BlackboxProfile profile = SecurityUtils.profile("operator_user");
        usersRolesDao.addRole(profile.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR));
        SecurityUtils.setAuthentication(profile);

        requestMonitorings()
                .andExpect(status().isForbidden());
    }

    private ResultActions requestMonitorings() throws Exception {
        return mockMvc.perform(get("/api/admin/message/monitoring/configs"))
                .andDo(print());
    }
}
