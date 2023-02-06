package ru.yandex.autotests.direct.cmd.campaigns.lang;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.Lang;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Не-сохранение параметра content_lang при создании кампании под клиентом")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Ignore
public class DontSaveLangInSaveNewCampUnderClientTest {

    private static final String CLIENT = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private BannersRule bannersRule = (BannersRule) BannersRuleFactory.
            getBannersRuleBuilderByCampType(CampaignTypeEnum.TEXT).
            overrideCampTemplate(new SaveCampRequest().withContentLang(Lang.EN.toString())).
            withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannersRule);

    @Test
    @Description("Не-сохранение параметра content_lang при создании кампании под клиентом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9408")
    public void testDontSaveLangInSaveNewCampUnderClient() {
        EditCampResponse campResponse = cmdRule.cmdSteps().campaignSteps()
                .getEditCamp(bannersRule.getCampaignId(), CLIENT);
        String actualLang = campResponse.getCampaign().getContentLang();
        assertThat("язык на уровне кампании сохранился", actualLang, nullValue());
    }
}
