package ru.yandex.autotests.directintapi.tests.dostup.addrole;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;


import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.DostupAddRoleRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.DostupAddRemoveRoleResponse;
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


/**
 * User: omaz
 * Date: 15.08.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1144
 */

@Aqua.Test(title = "DostupAddRole - нельзя добавить две роли на один паспортный логин")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_ADD_ROLE)
public class DostupAddRoleTwoRolesAtOneLoginTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    DarkSideSteps darkSideSteps = new DarkSideSteps();
    Role firstRole = Role.MANAGER;
    Role secondRole = Role.SUPER;
    String passportLogin = Logins.getRoleLogin(firstRole);

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void dostupAddRoleNonexistPassportLoginTest() {
        darkSideSteps.getDostupSteps().clearRoles(DostupSteps.STAFF_LOGIN);
        log.info("Добавляем первую роль");
        darkSideSteps.getDostupSteps().addRoleNoErrors(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(passportLogin)
                        .withRole(firstRole)
        );
        log.info("Пытаемся добавить вторую роль на тот же логин");
        darkSideSteps.getDostupSteps().addRoleExpectError(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(passportLogin)
                        .withRole(secondRole),
                new DostupAddRemoveRoleResponse().withFatal(
                        "логин " + passportLogin + " уже имеет роль: " + firstRole.getRoleName()
                )
        );

    }


}
