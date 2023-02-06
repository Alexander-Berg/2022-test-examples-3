package ru.yandex.autotests.direct.httpclient.banners.editgroups.roles;

import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by shmykov on 13.05.15.
 * TESTIRT-4953
 */
@Aqua.Test
@Description("Вызов saveTextAdGroups для ролей, имеющих к нему доступ")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class AllowedRolesSaveTextAdGroupsTest extends GroupsRolesTestBase {

    public AllowedRolesSaveTextAdGroupsTest() {
        super(CMD.SAVE_TEXT_ADGROUPS);
    }

    @Parameterized.Parameters(name = "Роль: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"Пользователь", CLIENT_LOGIN, SELF_CAMPAIGN},
                {"Менеджер", Logins.MANAGER, AG_CAMPAIGN},
                {"Агенство", Logins.ADGROUPS_AGENCY, AG_CAMPAIGN},
                {"Супер", Logins.SUPER, SELF_CAMPAIGN},
                {"Представитель", CLIENT_REPRESENTATIVE, SELF_CAMPAIGN},
        };
        return Arrays.asList(data);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10112")
    public void checkRedirectTest() {
        AllureUtils.changeTestCaseTitle(description + ", проверка редиректа после сохранения объявления");
        cmdRule.oldSteps().commonSteps().checkRedirect(response, CMD.SHOW_CAMP);
    }
}
