package ru.yandex.autotests.direct.httpclient.banners.editgroups.roles;

import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by shmykov on 29.04.15.
 * TESTIRT-4974
 */
@Aqua.Test
@Description("Вызов showCampMultiEdit для ролей, не имеющих к нему доступ, часть 1")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class UnAllowedRolesPart1ShowCampMultiEditTest extends GroupsRolesTestBase {


    public UnAllowedRolesPart1ShowCampMultiEditTest() {
        super(CMD.SHOW_CAMP_MULTI_EDIT);
    }

    @Parameterized.Parameters(name = "Роль: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"Другой пользователь", ANOTHER_CLIENT_LOGIN, SELF_CAMPAIGN},
                {"Чужой менеджер", Logins.TRANSFER_MANAGER, SELF_CAMPAIGN},
                {"Чужое агенство", Logins.AGENCY, SELF_CAMPAIGN},
        };
        return Arrays.asList(data);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10117")
    public void noRightsErrorTest() {
        AllureUtils.changeTestCaseTitle(description);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }
}
