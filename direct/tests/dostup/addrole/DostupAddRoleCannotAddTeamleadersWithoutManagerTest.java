package ru.yandex.autotests.directintapi.tests.dostup.addrole;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;


import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.DostupAddRoleRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.DostupAddRemoveRoleResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.DostupGetUserRolesResponse;
import ru.yandex.autotests.directapi.darkside.model.Role;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.DostupSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.directintapi.utils.HazelcastLockNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.HazelcastAnnotations;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static org.hamcrest.Matchers.equalTo;


/**
 * User: omaz
 * Date: 15.08.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1144
 */

@Aqua.Test(title = "DostupAddRole - нельзя добавить тимлидера без менеджера")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_ADD_ROLE)
@RunWith(Parameterized.class)
public class DostupAddRoleCannotAddTeamleadersWithoutManagerTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    DarkSideSteps darkSideSteps = new DarkSideSteps();

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameter
    public Role role;

    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {Role.SUPERTEAMLEADER},
                {Role.TEAMLEADER}};
        return Arrays.asList(data);
    }


    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void dostupAddRoleCannotAddTeamleadersWithoutManagerTest() {
        String passportLogin = Logins.getRoleLogin(role);
        darkSideSteps.getDostupSteps().clearRoles(DostupSteps.STAFF_LOGIN);
        darkSideSteps.getDostupSteps().addRoleExpectError(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(passportLogin)
                        .withRole(role),
                new DostupAddRemoveRoleResponse().withFatal(
                        "логин " + passportLogin + " не имеет роли менеджера (текущая роль: empty)"
                )
        );

        log.info("Проверяем, что роль не добавилась");
        DostupGetUserRolesResponse getUserRolesResponse =
                darkSideSteps.getDostupSteps().getUserRolesNoErrors(DostupSteps.STAFF_LOGIN);
        assertThat("В ответе должна была быть две роли(менеджера и тимлидера)",
                getUserRolesResponse.getRoles().size(), equalTo(0));
    }

}
