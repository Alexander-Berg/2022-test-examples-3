package ru.yandex.autotests.directintapi.tests.dostup.info;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.dostup.DostupInfoResponse;
import ru.yandex.autotests.directapi.darkside.model.Role;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by omaz on 30.01.14.
 * https://jira.yandex-team.ru/browse/TESTIRT-1412
 */
@Aqua.Test(title = "info")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.DOSTUP_INFO)
public class InfoTest {
    private DarkSideSteps darkSideSteps = new DarkSideSteps();

    @Test
    public void infoTest() {
        DostupInfoResponse response = darkSideSteps.getDostupSteps().infoNoErrors();
        Collection<String> responseRoleNames = response.getRoles().getValues().keySet();
        List<String> allRoleNames = new ArrayList<>();
        for (Role role : Role.values()) {
            allRoleNames.add(role.getRoleName());
        }
        // Роли для Тирной схемы доступов DIRECT-92406
        allRoleNames.add("manager_for_client");
        allRoleNames.add("main_manager_for_client");

        // Роли для пинкодной схемы доступов DIRECT-101826
        allRoleNames.add("limited_support");
        allRoleNames.add("support_for_client");

        assertThat("получен верный список ролей", responseRoleNames.stream().sorted().collect(Collectors.toList()),
                beanDiffer(allRoleNames.stream().sorted().collect(Collectors.toList())));
    }

}
