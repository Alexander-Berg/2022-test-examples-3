package ru.yandex.autotests.directintapi.tests.dostup.removerole;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;


import org.junit.Test;
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

import static ch.lambdaj.Lambda.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * User: omaz
 * Date: 15.08.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1411
 */

@Aqua.Test(title = "DostupRemoveRole - неправильный паспортный логин")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_REMOVE_ROLE)
public class DostupRemoveRoleAnotherLoginTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    DarkSideSteps darkSideSteps = new DarkSideSteps();
    Role role = Role.MANAGER;
    String anotherLogin = Logins.LOGIN_MAIN;

    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void dostupRemoveRoleAnotherLoginTest() {
        darkSideSteps.getDostupSteps().clearRoles(DostupSteps.STAFF_LOGIN);
        darkSideSteps.getDostupSteps().addRoleNoErrors(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(Logins.getRoleLogin(role))
                        .withRole(role)
        );

        log.info("Пытаемся удалить роль, которая назначена на другой логин");
        darkSideSteps.getDostupSteps().removeRoleNoErrors(
                new DostupRemoveRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(anotherLogin)
                        .withRole(role)
        );
        log.info("Проверяем, что роль не удалилась");
        DostupGetUserRolesResponse getUserRolesResponse =
                darkSideSteps.getDostupSteps().getUserRolesNoErrors(DostupSteps.STAFF_LOGIN);
        RoleResponseData roleResponseData =
                selectFirst(getUserRolesResponse.getRoles(),
                        having(on(RoleResponseData.class).getRoleName(), equalTo(role.getRoleName())));
        assertThat("Роль удалилась",
                roleResponseData,
                notNullValue()
        );
    }

}
