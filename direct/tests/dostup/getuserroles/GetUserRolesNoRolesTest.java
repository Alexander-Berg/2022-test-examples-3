package ru.yandex.autotests.directintapi.tests.dostup.getuserroles;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.DostupGetUserRolesResponse;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.DostupSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.directintapi.utils.HazelcastLockNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.HazelcastAnnotations;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by omaz on 30.01.14.
 * https://jira.yandex-team.ru/browse/TESTIRT-1413
 */
@Aqua.Test(title = "get_user_roles - нет ролей")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_GET_USER_ROLES)
public class GetUserRolesNoRolesTest {
    DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected LogSteps log = LogSteps.getLogger(this.getClass());


    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void getUserRolesNoRolesTest() {
        darkSideSteps.getDostupSteps().clearRoles(DostupSteps.STAFF_LOGIN);
        DostupGetUserRolesResponse response =
                darkSideSteps.getDostupSteps().getUserRolesNoErrors(DostupSteps.STAFF_LOGIN);
        assertThat("У пользователя " + DostupSteps.STAFF_LOGIN + " не должно быть ролей",
                response.getRoles(),
                empty());
    }
}
