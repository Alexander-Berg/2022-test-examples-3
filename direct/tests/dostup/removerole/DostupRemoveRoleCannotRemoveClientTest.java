package ru.yandex.autotests.directintapi.tests.dostup.removerole;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;


import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.DostupRemoveRoleRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.DostupAddRemoveRoleResponse;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.DostupSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.directintapi.utils.HazelcastLockNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.HazelcastAnnotations;


/**
 * User: omaz
 * Date: 15.08.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1411
 */

@Aqua.Test(title = "DostupRemoveRole - нельзя удалить роль клиента")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_REMOVE_ROLE)
public class DostupRemoveRoleCannotRemoveClientTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    DarkSideSteps darkSideSteps = new DarkSideSteps();
    String login = "at-intapi-role-mediaplanner";

    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void dostupRemoveRoleClientTest() {
        log.info("Пытаемся удалить роль клиента");
        darkSideSteps.getDostupSteps().removeRoleExpectError(
                new DostupRemoveRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(login)
                        .withRole("client"),
                new DostupAddRemoveRoleResponse().withFatal(
                        "ошибка в названии роли: 'client'"
                )
        );

    }


}
