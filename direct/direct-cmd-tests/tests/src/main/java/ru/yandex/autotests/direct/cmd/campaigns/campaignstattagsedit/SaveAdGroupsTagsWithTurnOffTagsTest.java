package ru.yandex.autotests.direct.cmd.campaigns.campaignstattagsedit;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.autotests.direct.cmd.data.clients.SettingsModel;
import ru.yandex.autotests.direct.cmd.data.clients.TagsAllowedEnum;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorsResponse;
import ru.yandex.autotests.direct.cmd.data.saveadgrouptags.SaveAdGroupTagsRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

//TESTIRT-8419
@Stories(TestFeatures.Tags.SAVE_ADGROUP_TAGS)
@Features(TestFeatures.TAGS)
@Tag(CmdTag.SAVE_ADGROUP_TAGS)
@Tag(ObjectTag.TAGS)
@Tag(CampTypeTag.TEXT)
public class SaveAdGroupsTagsWithTurnOffTagsTest {
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private String CLIENT = "at-direct-backend-c";
    TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    @Before
    public void before() {
        cmdRule.apiSteps().campaignFakeSteps().setOrderID(bannersRule.getCampaignId().intValue(),
                bannersRule.getCampaignId().intValue());
        SettingsModel userSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        userSettings.setUlogin(CLIENT);
        userSettings.withTagsAllowed(TagsAllowedEnum.OFF);
        cmdRule.cmdSteps().userSettingsSteps().postSaveSettings(userSettings);
    }

    @Test
    @Description("Пытаемся сохранить тэги")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9398")
    public void showCampStat() {
        SaveAdGroupTagsRequest saveTagsParams = new SaveAdGroupTagsRequest().withCid(bannersRule.getCampaignId().toString())
                .withAdgroupIds(bannersRule.getGroupId().toString())
                .withNewTags("tags")
                .withUlogin(CLIENT);
        ErrorsResponse actualError = cmdRule.cmdSteps().groupsSteps().postSaveAdgroupTagsInvalidData(saveTagsParams);
        assertThat("Получили ошибку", actualError.getCommonErrors().get(0),
                equalTo("Невозможно установить метки"));
    }

}
