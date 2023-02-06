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
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
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

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение параметра content_lang при редактировании кампании")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
@Ignore
public class LangInSaveCampTest {

    private static final String CLIENT = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    public String contentLang;
    public String geo;
    private BannersRule bannersRule;

    @SuppressWarnings("unused")
    public LangInSaveCampTest(CampaignTypeEnum campType, Lang contentLang, Geo geo, String geoName) {
        this.contentLang = contentLang != null ? contentLang.toString() : null;
        this.geo = geo.toString();
        bannersRule = BannersRuleFactory.
                getBannersRuleBuilderByCampType(campType).
                overrideGroupTemplate(new Group().withGeo(geo.getGeo())).
                withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    /**
     * Все типы кроме перфоманс
     */
    @Parameterized.Parameters(name = "Тип кампании: {0}; Язык: {1}; Гео: {3}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT, Lang.EN, Geo.AUSTRIA, Geo.AUSTRIA.getName()},
                {CampaignTypeEnum.TEXT, Lang.UA, Geo.UKRAINE, Geo.UKRAINE.getName()},
                {CampaignTypeEnum.TEXT, Lang.KZ, Geo.KAZAKHSTAN, Geo.KAZAKHSTAN.getName()},
                {CampaignTypeEnum.TEXT, Lang.TR, Geo.TURKEY, Geo.TURKEY.getName()},
                {CampaignTypeEnum.TEXT, Lang.DE, Geo.GERMANY, Geo.GERMANY.getName()},
                {CampaignTypeEnum.TEXT, null, Geo.RUSSIA, Geo.RUSSIA.getName()},

                {CampaignTypeEnum.DTO, Lang.EN, Geo.AUSTRIA, Geo.AUSTRIA.getName()},
                {CampaignTypeEnum.DTO, Lang.KZ, Geo.KAZAKHSTAN, Geo.KAZAKHSTAN.getName()},
                {CampaignTypeEnum.DTO, null, Geo.KAZAKHSTAN, Geo.KAZAKHSTAN.getName()},

                {CampaignTypeEnum.MOBILE, Lang.EN, Geo.AUSTRIA, Geo.AUSTRIA.getName()},
                {CampaignTypeEnum.MOBILE, Lang.KZ, Geo.KAZAKHSTAN, Geo.KAZAKHSTAN.getName()},
                {CampaignTypeEnum.MOBILE, null, Geo.KAZAKHSTAN, Geo.KAZAKHSTAN.getName()}
        });
    }

    @Test
    @Description("Сохранение параметра content_lang при редактировании кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9411")
    public void testCampaignLangInSaveCamp() {
        SaveCampRequest saveCampRequest = getSaveCampRequest();
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);

        EditCampResponse campResponse = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
        String actualLang = campResponse.getCampaign().getContentLang();
        String expectedLang = contentLang;
        assertThat("язык на уровне кампании сохранился", actualLang, equalTo(expectedLang));
    }

    protected SaveCampRequest getSaveCampRequest() {
        SaveCampRequest request = bannersRule.getSaveCampRequest();
        request.setCid(bannersRule.getCampaignId().toString());
        request.setContentLang(this.contentLang);
        request.setGeo(geo);
        return request;
    }
}
