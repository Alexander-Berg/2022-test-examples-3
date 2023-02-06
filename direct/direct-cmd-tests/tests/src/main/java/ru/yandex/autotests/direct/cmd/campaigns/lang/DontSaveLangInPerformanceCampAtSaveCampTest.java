package ru.yandex.autotests.direct.cmd.campaigns.lang;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.Lang;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Не-сохранение параметра content_lang при редактировании перфоманс-кампании")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.PERFORMANCE)
@Ignore
public class DontSaveLangInPerformanceCampAtSaveCampTest {

    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Test
    @Description("Не-сохранение параметра content_lang при редактировании перфоманс-кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9406")
    public void testNoCampLangSaveAtSaveCamp() {
        SaveCampRequest saveCampRequest = getSaveCampRequest();
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);

        EditCampResponse campResponse = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
        String actualLang = campResponse.getCampaign().getContentLang();
        assertThat("язык на уровне кампании не сохранился", actualLang, nullValue());
    }

    protected SaveCampRequest getSaveCampRequest() {
        SaveCampRequest request = bannersRule.getSaveCampRequest();
        request.setCid(bannersRule.getCampaignId().toString());
        request.setContentLang(Lang.EN.toString());
        request.setGeo(Geo.AUSTRIA.toString());
        return request;
    }
}
