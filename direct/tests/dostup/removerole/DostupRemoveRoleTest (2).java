package ru.yandex.autotests.directintapi.tests.dostup.removerole;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
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
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.HazelcastAnnotations;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * User: omaz
 * Date: 15.08.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1411
 */

@Aqua.Test(title = "DostupRemoveRole - удаление ролей")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_REMOVE_ROLE)
@RunWith(Parameterized.class)
public class DostupRemoveRoleTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    DarkSideSteps darkSideSteps = new DarkSideSteps();
    String passportLogin;

    @Parameterized.Parameter
    public Role role;

    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {Role.MANAGER},
                {Role.MEDIAPLANNER},
                {Role.PLACER},
                {Role.SUPER},
                {Role.SUPERREADER},
                {Role.SUPPORT},
                {Role.INTERNAL_MANAGER},
                {Role.INTERNAL_ADMIN}};
        return Arrays.asList(data);
    }


    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void dostupRemoveRoleTest() {
        darkSideSteps.getDostupSteps().clearRoles(DostupSteps.STAFF_LOGIN);
        passportLogin = Logins.getRoleLogin(role);
        log.info("Добавляем роль, которую будем удалять");
        darkSideSteps.getDostupSteps().addRoleNoErrors(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withPassportLogin(passportLogin)
                        .withRole(role)
        );
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

}
