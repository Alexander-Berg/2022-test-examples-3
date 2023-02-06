package ru.yandex.autotests.directintapi.tests.dostup.removerole;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.DostupAddRoleRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.DostupRemoveRoleRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.DostupGetUserRolesResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.RoleResponseData;
import ru.yandex.autotests.directapi.darkside.model.Role;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.DostupSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.directintapi.utils.HazelcastLockNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.HazelcastAnnotations;

import java.util.Arrays;

import static ch.lambdaj.Lambda.*;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * User: omaz
 * Date: 15.08.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1411
 */

@Aqua.Test(title = "DostupRemoveRole - удаление ролей тимлидеров")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_REMOVE_ROLE)
@RunWith(Parameterized.class)
public class DostupRemoveRoleTeamlidersTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    DarkSideSteps darkSideSteps = new DarkSideSteps();
    String passportLogin;

    @Parameterized.Parameter
    public Role role;

    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {Role.TEAMLEADER},
                {Role.SUPERTEAMLEADER},};
        return Arrays.asList(data);
    }

    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void dostupRemoveRoleTeamleaderTest() {
        darkSideSteps.getDostupSteps().clearRoles(DostupSteps.STAFF_LOGIN);
        passportLogin = Logins.getRoleLogin(role);
        log.info("Добавляем роль менеджера");
        darkSideSteps.getDostupSteps().addRoleNoErrors(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(passportLogin)
                        .withRole(Role.MANAGER)
        );

        log.info("Добавляем роль тимлидера");
        darkSideSteps.getDostupSteps().addRoleNoErrors(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(passportLogin)
                        .withRole(role)
        );
        log.info("Проверяем, что можно удалить роль тимлидера");
        darkSideSteps.getDostupSteps().removeRoleNoErrors(
                new DostupRemoveRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(passportLogin)
                        .withRole(role)
        );

        log.info("Проверяем, что роль удалилась");
        DostupGetUserRolesResponse getUserRolesResponse =
                darkSideSteps.getDostupSteps().getUserRolesNoErrors(DostupSteps.STAFF_LOGIN);
        RoleResponseData roleResponseData =
                selectFirst(getUserRolesResponse.getRoles(),
                        having(on(RoleResponseData.class).getRoleName(), equalTo(role.getRoleName())));
        assertThat("Роль не удалилась",
                roleResponseData,
                nullValue()
        );
    }

    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void dostupRemoveRoleManagerWithTeamleaderTest() {
        darkSideSteps.getDostupSteps().clearRoles(DostupSteps.STAFF_LOGIN);
        passportLogin = Logins.getRoleLogin(role);
        log.info("Добавляем роль менеджера");
        darkSideSteps.getDostupSteps().addRoleNoErrors(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(passportLogin)
                        .withRole(Role.MANAGER)
        );

        log.info("Добавляем роль тимлидера");
        darkSideSteps.getDostupSteps().addRoleNoErrors(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(passportLogin)
                        .withRole(role)
        );

        log.info("Проверяем, что нельзя удалить роль менеджера, если есть роль тимлидера");
        darkSideSteps.getDostupSteps().removeRoleNoErrors(
                new DostupRemoveRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(passportLogin)
                        .withRole(Role.MANAGER)
        );

        log.info("Проверяем, что роль не удалилась");
        DostupGetUserRolesResponse getUserRolesResponse =
                darkSideSteps.getDostupSteps().getUserRolesNoErrors(DostupSteps.STAFF_LOGIN);
        RoleResponseData roleResponseData =
                selectFirst(getUserRolesResponse.getRoles(),
                        having(on(RoleResponseData.class).getRoleName(), equalTo(role.getRoleName())));
        assertThat("Роль не удалилась",
                roleResponseData,
                notNullValue()
        );
    }

}
