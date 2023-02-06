package ru.yandex.autotests.directintapi.tests.dostup.getallroles;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.DostupAddRoleRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.DostupRemoveRoleRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.DostupGetAllRolesResponse;
import ru.yandex.autotests.directapi.darkside.model.Role;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.DostupSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.directintapi.utils.HazelcastLockNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.HazelcastAnnotations;

import static ch.lambdaj.Lambda.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by omaz on 30.01.14.
 * https://jira.yandex-team.ru/browse/TESTIRT-1414
 */
@Aqua.Test(title = "get_all_roles - удаление роли")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_GET_ALL_ROLES)
public class GetAllRolesRemoveRoleTest {
    DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    Role role = Role.MANAGER;

    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void getAllRolesRemoveRoleTest() {
        darkSideSteps.getDostupSteps().clearRoles(DostupSteps.STAFF_LOGIN);
        log.info("Добавляем новую роль");
        darkSideSteps.getDostupSteps().addRoleNoErrors(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withRole(role)
                        .withPassportLogin(Logins.getRoleLogin(role))
        );
        log.info("Удаляем роль");
        darkSideSteps.getDostupSteps().removeRoleNoErrors(
                new DostupRemoveRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withRole(role)
                        .withPassportLogin(Logins.getRoleLogin(role))
        );

        DostupGetAllRolesResponse response =
                darkSideSteps.getDostupSteps().getAllRolesNoErrors();
        log.info("Проверяем, что удаленной роли нет в ответе");
        DostupGetAllRolesResponse.User userData =
                selectFirst(response.getUsers(),
                        having(on(DostupGetAllRolesResponse.User.class).getLogin(), equalTo(DostupSteps.STAFF_LOGIN)));
        assertThat("У логина в ответе не должно быть ни одной роли",
                userData,
                nullValue());

    }
}
