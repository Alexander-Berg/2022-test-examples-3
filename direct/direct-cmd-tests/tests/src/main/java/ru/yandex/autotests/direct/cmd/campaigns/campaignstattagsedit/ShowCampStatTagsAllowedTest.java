package ru.yandex.autotests.direct.cmd.campaigns.campaignstattagsedit;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.data.clients.SettingsModel;
import ru.yandex.autotests.direct.cmd.data.clients.TagsAllowedEnum;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

//TESTIRT-8419
@Stories(TestFeatures.Campaigns.SHOW_CAMP_STAT)
@Features(TestFeatures.STAT)
@Tag(CmdTag.SHOW_CAMP_STAT)
@Tag(ObjectTag.STAT)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
@Tag("sb_test")
public class ShowCampStatTagsAllowedTest {
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public TagsAllowedEnum tagsAllowed;
    @Parameterized.Parameter(1)
    public String expectedStatus;
    private String CLIENT = "at-direct-backend-c";
    TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Parameterized.Parameters(name = "Настройка tags_allowed в положении {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {TagsAllowedEnum.ON, "Yes"},
                {TagsAllowedEnum.OFF, "No"},
        });
    }

    @Before
    public void before() {
        cmdRule.apiSteps().campaignFakeSteps().setOrderID(bannersRule.getCampaignId().intValue(),
                bannersRule.getCampaignId().intValue());
        SettingsModel userSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        userSettings.setUlogin(CLIENT);
        userSettings.withTagsAllowed(tagsAllowed);
        cmdRule.cmdSteps().userSettingsSteps().postSaveSettings(userSettings);
    }

    @Test
    @Description("Открываем статистику кампании при разрешенных тэгах")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9399")
    public void showCampStatWithAllowedTags() {
        ShowCampStatResponse actualResponse = cmdRule.cmdSteps().campaignSteps().getShowCampStat(ShowCampStatRequest
                .defaultCampStatRequest(bannersRule.getCampaignId(), CLIENT));
        assertThat("Флаг tags_allowed есть и соотвествует ожиданиям", actualResponse.getTagsAllowed(), equalTo(expectedStatus));
    }


}
