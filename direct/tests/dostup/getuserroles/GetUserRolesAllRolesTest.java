package ru.yandex.autotests.directintapi.tests.dostup.getuserroles;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.hamcrest.Matcher;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.DostupAddRoleRequest;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static org.hamcrest.Matchers.hasItems;

/**
 * Created by omaz on 30.01.14.
 * https://jira.yandex-team.ru/browse/TESTIRT-1413
 */
@Aqua.Test(title = "get_user_roles - все роли")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_GET_USER_ROLES)
public class GetUserRolesAllRolesTest {
    DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected LogSteps log = LogSteps.getLogger(this.getClass());

    @Test
    @HazelcastAnnotations.Lock(HazelcastLockNames.DOSTUP)
    public void getUserRolesAllRolesTest() {
        darkSideSteps.getDostupSteps().clearRoles(DostupSteps.STAFF_LOGIN);
        log.info("Сначала добавим на аккаунты для тимлидеров роли менеджера, чтобы роли тимлидеров добавились успешно");
        darkSideSteps.getDostupSteps().addRoleNoErrors(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withRole(Role.MANAGER)
                        .withPassportLogin(Logins.getRoleLogin(Role.TEAMLEADER))
        );
        darkSideSteps.getDostupSteps().addRoleNoErrors(
                new DostupAddRoleRequest()
                        .withLogin(DostupSteps.STAFF_LOGIN)
                        .withRole(Role.MANAGER)
                        .withPassportLogin(Logins.getRoleLogin(Role.SUPERTEAMLEADER))
        );
        log.info("Добавляем все роли");
        for (Role role : Role.values()) {
            darkSideSteps.getDostupSteps().addRoleNoErrors(
                    new DostupAddRoleRequest()
                            .withLogin(DostupSteps.STAFF_LOGIN)
                            .withRole(role)
                            .withPassportLogin(Logins.getRoleLogin(role))
            );
        }

        DostupGetUserRolesResponse response =
                darkSideSteps.getDostupSteps().getUserRolesNoErrors(DostupSteps.STAFF_LOGIN);

        List<String> responseRoles =
                extract(response.getRoles(), on(RoleResponseData.class).getRoleName());
        List<String> allRoles = new ArrayList<>();
        for (Role role : Role.values())
            allRoles.add(role.getRoleName());
        assertThat("Список ролей из ответа не совпадает со списком всех ролей",
                responseRoles,
                (Matcher) hasItems(allRoles.toArray()));

        List<String> responseLogins =
                extract(response.getRoles(), on(RoleResponseData.class).getPassportLogin());
        Collection<String> allLogins =
                Logins.roleLogins.values();
        assertThat("Список логинов из ответа не совпадает со списком всех ролей",
                responseLogins,
                (Matcher) hasItems(allLogins.toArray()));

    }
}
