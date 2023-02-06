package ru.yandex.autotests.direct.httpclient.banners.editgroups.commonvalidation;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.groups.EditGroupsErrors;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;

/**
 * Created by shmykov on 13.05.15.
 * TESTIRT-4953
 */
@Aqua.Test
@Description("Вызов saveTextAdGroups с неверными параметрами запроса")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CampTypeTag.TEXT)
public class SaveTextAdGroupsWrongRequestParametersTest extends WrongGroupRequestParametersTestBase {

    @Test
    @Description("Проверка ошибки при вызове контроллера для архивного баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10099")
    public void archivedGroupErrorTest() {
        cmdRule.cmdSteps().bannerSteps().archiveBanner(CLIENT_LOGIN, bannersRule.getCampaignId(),
                bannersRule.getGroupId(), bannersRule.getBannerId());
        cmdRule.apiSteps().campaignStepsV5().campaignsSuspend(CLIENT_LOGIN, bannersRule.getCampaignId());
        cmdRule.apiSteps().campaignStepsV5().campaignsArchive(CLIENT_LOGIN, bannersRule.getCampaignId());
        doRequest();
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response,
                equalTo(TextResourceFormatter.resource(EditGroupsErrors.SAVE_ARCHIVED_CAMPAIGN2)
                        .args(bannersRule.getCampaignId()).toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера для архивной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10100")
    public void archivedCampaignErrorTest() {
        cmdRule.apiSteps().campaignStepsV5().campaignsSuspend(CLIENT_LOGIN, bannersRule.getCampaignId());
        cmdRule.apiSteps().campaignStepsV5().campaignsArchive(CLIENT_LOGIN, bannersRule.getCampaignId());
        doRequest();
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response,
                equalTo(TextResourceFormatter.resource(EditGroupsErrors.SAVE_ARCHIVED_CAMPAIGN2)
                        .args(bannersRule.getCampaignId()).toString()));
    }

    @Override
    protected void doRequest() {
        response = cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, requestParams);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10098")
    public void wrongCidTest() {
        super.wrongCidTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10101")
    public void withoutCidTest() {
        super.withoutCidTest();
    }
}
