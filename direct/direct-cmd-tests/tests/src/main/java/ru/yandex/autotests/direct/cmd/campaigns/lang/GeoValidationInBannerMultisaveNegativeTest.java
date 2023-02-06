package ru.yandex.autotests.direct.cmd.campaigns.lang;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.Lang;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

@Aqua.Test
@Description("Создание группы с геотергетингом, не соответствующим языку баннеров " +
        "(автоопределенному или выставленному в campaign.content_lang) (негативные кейсы)")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
@Ignore
public class GeoValidationInBannerMultisaveNegativeTest {

    private static final String CLIENT = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private CampaignRule campaignRule;
    private Geo geo;
    private String bannerText;

    @SuppressWarnings("unused")
    public GeoValidationInBannerMultisaveNegativeTest(Lang contentLang, String bannerText,
                                                      Geo geo, String geoName) {
        this.geo = geo;
        this.bannerText = bannerText;
        String contentLangStr = contentLang != null ? contentLang.toString() : null;
        campaignRule = new CampaignRule().
                withMediaType(CampaignTypeEnum.TEXT).
                withUlogin(CLIENT).
                overrideCampTemplate(new SaveCampRequest().withContentLang(contentLangStr));
        cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);
    }

    @Parameterized.Parameters(name = "campaign.content_lang: {0}; Заголовок/текст баннера: {1}; Гео: {3}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                /*
                    выставлен язык на уровне кампании
                 */
                {Lang.UA, "русский текст", Geo.RUSSIA, Geo.RUSSIA.getName()},
                {Lang.UA, "Қазақ мәтін", Geo.KAZAKHSTAN, Geo.KAZAKHSTAN.getName()},
                {Lang.UA, "Türk şarkı sözleri", Geo.TURKEY, Geo.TURKEY.getName()},
                {Lang.UA, "english text", Geo.AUSTRIA, Geo.AUSTRIA.getName()},

                {Lang.KZ, "русский текст", Geo.RUSSIA, Geo.RUSSIA.getName()},
                {Lang.KZ, "український текст", Geo.UKRAINE, Geo.UKRAINE.getName()},
                {Lang.KZ, "Türk şarkı sözleri", Geo.TURKEY, Geo.TURKEY.getName()},
                {Lang.KZ, "english text", Geo.AUSTRIA, Geo.AUSTRIA.getName()},

                {Lang.TR, "русский текст", Geo.RUSSIA, Geo.RUSSIA.getName()},
                {Lang.TR, "український текст", Geo.UKRAINE, Geo.UKRAINE.getName()},
                {Lang.TR, "Қазақ мәтін", Geo.KAZAKHSTAN, Geo.KAZAKHSTAN.getName()},
                {Lang.TR, "english text", Geo.AUSTRIA, Geo.AUSTRIA.getName()},

                /*
                    язык определяется на уровне баннера
                 */
                {null, "український текст", Geo.RUSSIA, Geo.RUSSIA.getName()},
                {null, "український текст", Geo.KAZAKHSTAN, Geo.KAZAKHSTAN.getName()},
                {null, "український текст", Geo.TURKEY, Geo.TURKEY.getName()},
                {null, "український текст", Geo.AUSTRIA, Geo.AUSTRIA.getName()},

                {null, "Қазақ мәтін", Geo.RUSSIA, Geo.RUSSIA.getName()},
                {null, "Қазақ мәтін", Geo.UKRAINE, Geo.UKRAINE.getName()},
                {null, "Қазақ мәтін", Geo.TURKEY, Geo.TURKEY.getName()},
                {null, "Қазақ мәтін", Geo.AUSTRIA, Geo.AUSTRIA.getName()},

                {null, "Türk şarkı sözleri", Geo.RUSSIA, Geo.RUSSIA.getName()},
                {null, "Türk şarkı sözleri", Geo.UKRAINE, Geo.UKRAINE.getName()},
                {null, "Türk şarkı sözleri", Geo.KAZAKHSTAN, Geo.KAZAKHSTAN.getName()},
                {null, "Türk şarkı sözleri", Geo.AUSTRIA, Geo.AUSTRIA.getName()},
        });
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Создание группы с геотергетингом, не соответствующим языку баннеров (негативные сценарии)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9409")
    public void testGeoCampaignLangValidationNegative() {
        Group group = getGroup();
        GroupsParameters request = GroupsParameters.forNewCamp(CLIENT, campaignRule.getCampaignId(), group);

        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(request);
    }

    private Group getGroup() {
        Group group = GroupsFactory.getDefaultTextGroup();
        group.withCampaignID(campaignRule.getCampaignId().toString()).
                withGeo(geo.getGeo());
        group.getBanners().get(0).withCid(campaignRule.getCampaignId());
        group.getBanners().get(0).setTitle(bannerText);
        group.getBanners().get(0).setBody(bannerText);
        return group;
    }
}
