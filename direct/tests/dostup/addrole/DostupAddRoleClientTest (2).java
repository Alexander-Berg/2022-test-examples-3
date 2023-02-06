package ru.yandex.autotests.directintapi.tests.dostup.addrole;

import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.DostupAddRoleRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.DostupAddRemoveRoleResponse;
import ru.yandex.autotests.directapi.darkside.model.Role;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.DostupSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.directintapi.utils.HazelcastLockNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.HazelcastAnnotations;
import ru.yandex.qatools.hazelcast.SemaphoreRule;


/**
 * User: omaz
 * Date: 15.08.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1144
 */

@Aqua.Test(title = "DostupAddRole - нельзя добавить роль клиенту")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_ADD_ROLE)
public class DostupAddRoleClientTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    DarkSideSteps darkSideSteps = new DarkSideSteps();
    String login = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void dostupAddRoleClientTest() {
        log.info("Пытаемся добавить роль логину, который уже является клиентом");
        darkSideSteps.getDostupSteps().addRoleExpectError(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(login)
                        .withRole(Role.SUPER),
                new DostupAddRemoveRoleResponse().withFatal(
                        "логин " + login + " уже имеет роль: client"
                )
        );

    }


}
