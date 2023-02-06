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

import static org.hamcrest.Matchers.equalTo;

/**
 * Created by shmykov on 29.04.15.
 * TESTIRT-4974
 */
@Aqua.Test
@Description("Вызов showCampMultiEdit для ролей, имеющих к нему доступ")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class AllowedRolesShowCampMultiEditTest extends GroupsRolesTestBase {

    public AllowedRolesShowCampMultiEditTest() {
        super(CMD.SHOW_CAMP_MULTI_EDIT);
    }

    @Parameterized.Parameters(name = "Роль: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"Пользователь", CLIENT_LOGIN, SELF_CAMPAIGN},
                {"Менеджер", Logins.MANAGER, AG_CAMPAIGN},
                {"Агенство", Logins.ADGROUPS_AGENCY, AG_CAMPAIGN},
                {"Супер", Logins.SUPER, SELF_CAMPAIGN},
                {"Представитель", CLIENT_REPRESENTATIVE, SELF_CAMPAIGN},
                {"Суперридер", Logins.SUPER_READER, SELF_CAMPAIGN},
        };
        return Arrays.asList(data);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10114")
    public void NoErrorsTest() {
        AllureUtils.changeTestCaseTitle(description + ", проверка отстутсвия ошибок");
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(null));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10113")
    public void correctVarDumpTest() {
        AllureUtils.changeTestCaseTitle(description + ", проверка корректного cmd в ответе");
        cmdRule.oldSteps().commonSteps().checkDirectResponseCmdField(response, equalTo(cmd.getName()));
    }
}
