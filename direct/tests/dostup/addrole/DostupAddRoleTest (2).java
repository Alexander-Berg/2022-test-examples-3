package ru.yandex.autotests.directintapi.tests.dostup.addrole;

import java.util.Arrays;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.DostupAddRoleRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.DostupGetUserRolesResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.RoleResponseData;
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

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * User: omaz
 * Date: 15.08.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1144
 */

@Aqua.Test(title = "DostupAddRole - добавление ролей")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_ADD_ROLE)
@RunWith(Parameterized.class)
public class DostupAddRoleTest {
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
                {Role.MANAGER},
                {Role.MEDIAPLANNER},
                {Role.PLACER},
                {Role.SUPER},
                {Role.INTERNAL_ADMIN},
                {Role.INTERNAL_MANAGER}
        };
        return Arrays.asList(data);
    }

    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void dostupAddRoleTest() {
        String passportLogin = Logins.getRoleLogin(role);
        darkSideSteps.getDostupSteps().clearRoles(DostupSteps.STAFF_LOGIN);
        darkSideSteps.getDostupSteps().addRoleNoErrors(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(passportLogin)
                        .withRole(role)
        );

        log.info("Проверяем, что роль добавилась");
        DostupGetUserRolesResponse getUserRolesResponse =
                darkSideSteps.getDostupSteps().getUserRolesNoErrors(DostupSteps.STAFF_LOGIN);
        assertThat("В ответе должна была быть одна роль",
                getUserRolesResponse.getRoles().size(), equalTo(1));
        RoleResponseData roleResponseData =
                selectFirst(getUserRolesResponse.getRoles(),
                        having(on(RoleResponseData.class).getRoleName(), equalTo(role.getRoleName())));
        assertThat("Не добавилась нужная роль, или добавилась с неправильным паспортым логином",
                roleResponseData,
                allOf(
                        notNullValue(),
                        having(on(RoleResponseData.class).getPassportLogin(), equalTo(passportLogin))
                ));
    }

}
