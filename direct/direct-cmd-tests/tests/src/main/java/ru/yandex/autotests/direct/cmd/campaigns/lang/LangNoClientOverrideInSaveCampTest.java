package ru.yandex.autotests.direct.cmd.campaigns.lang;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.Lang;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Не-сбрасывание параметра content_lang при сохранении клиентом параметров кампании")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
@Ignore
public class LangNoClientOverrideInSaveCampTest {

    private static final String CLIENT = "at-direct-backend-c";
    private static final String CONTENT_LANG_SUPER = Lang.EN.toString();
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public String clientContentLang;
    @Rule
    public DirectCmdRule cmdRule;
    private BannersRule bannersRule;

    public LangNoClientOverrideInSaveCampTest(CampaignTypeEnum campType, String clientContentLang) {
        this.clientContentLang = clientContentLang;
        this.bannersRule = BannersRuleFactory.
                getBannersRuleBuilderByCampType(campType).
                withUlogin(CLIENT);
        this.bannersRule.overrideCampTemplate(
                new SaveCampRequest().withContentLang(CONTENT_LANG_SUPER));
        this.cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Тип кампании: {0}; Язык: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT, Lang.DE.toString()},
                {CampaignTypeEnum.TEXT, ""},
                {CampaignTypeEnum.TEXT, null},

                {CampaignTypeEnum.DTO, Lang.DE.toString()},
                {CampaignTypeEnum.DTO, ""},
                {CampaignTypeEnum.DTO, null},

                {CampaignTypeEnum.MOBILE, Lang.DE.toString()},
                {CampaignTypeEnum.MOBILE, ""},
                {CampaignTypeEnum.MOBILE, null}
        });
    }

    @Test
    @Description("Не-сбрасывание параметра content_lang при сохранении клиентом параметров текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9413")
    public void testCampaignLangNoClientOverride() {
        User client = User.get(CLIENT);
        cmdRule.cmdSteps().authSteps().authenticate(client);
        SaveCampRequest saveCampRequest = getSaveCampRequest();
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);

        EditCampResponse campResponse = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
        String actualLang = campResponse.getCampaign().getContentLang();
        String expectedLang = CONTENT_LANG_SUPER;
        assertThat("язык на уровне кампании не изменился", actualLang, equalTo(expectedLang));
    }

    protected SaveCampRequest getSaveCampRequest() {
        SaveCampRequest request = bannersRule.getSaveCampRequest();
        request.setCid(bannersRule.getCampaignId().toString());
        request.setContentLang(clientContentLang);
        request.setGeo(Geo.AUSTRIA.toString());
        return request;
    }
}
