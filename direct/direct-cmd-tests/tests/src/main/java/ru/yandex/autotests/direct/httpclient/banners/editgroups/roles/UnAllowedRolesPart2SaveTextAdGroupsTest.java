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
 * Created by shmykov on 13.05.15.
 * TESTIRT-4953
 */
@Aqua.Test
@Description("Вызов saveTextAdGroups для ролей, не имеющих к нему доступ, часть 2")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class UnAllowedRolesPart2SaveTextAdGroupsTest extends GroupsRolesTestBase {


    public UnAllowedRolesPart2SaveTextAdGroupsTest() {
        super(CMD.SAVE_TEXT_ADGROUPS);
    }

    @Parameterized.Parameters(name = "Роль: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"Пользователь, агентская кампания", CLIENT_LOGIN, AG_CAMPAIGN},
                {"Менеджер, клиентская кампания", Logins.MANAGER, SELF_CAMPAIGN},
                {"Агенство, клиентская кампания", Logins.ADGROUPS_AGENCY, SELF_CAMPAIGN},
                /*{"Вешальщик", Logins.PLACER, SELF_CAMPAIGN},*/
                //по DIRECT-42207 у вешальшика есть права на редактирование, было DIRECT-41646
        };
        return Arrays.asList(data);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10119")
    public void noRightsErrorTest() {
        AllureUtils.changeTestCaseTitle(description);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }
}
