package ru.yandex.autotests.direct.cmd.banners.imagecreative.geo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
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
@Description("Изменение геотергетинга кампании на соответствующий языку баннеров " +
        "(автоопределенному) (негативные кейсы)")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class CampGeoInCreativeImageBannerTextCampNegativeTest extends CampGeoInCreativeImageBannerTestBase {

    public CampGeoInCreativeImageBannerTextCampNegativeTest(CampaignTypeEnum campaignType, String bannerText, Geo geo) {
        super(campaignType, bannerText, geo);
    }

    @Parameterized.Parameters(name = "Тип кампании: {0}; текст баннера: {1}; Гео: {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT, "український текст", Geo.RUSSIA},
                {CampaignTypeEnum.TEXT, "український текст", Geo.KAZAKHSTAN},
                {CampaignTypeEnum.TEXT, "український текст", Geo.TURKEY},
                {CampaignTypeEnum.TEXT, "український текст", Geo.AUSTRIA},

                {CampaignTypeEnum.TEXT, "Қазақ мәтін", Geo.RUSSIA},
                {CampaignTypeEnum.TEXT, "Қазақ мәтін", Geo.UKRAINE},
                {CampaignTypeEnum.TEXT, "Қазақ мәтін", Geo.TURKEY},
                {CampaignTypeEnum.TEXT, "Қазақ мәтін", Geo.AUSTRIA},

                {CampaignTypeEnum.TEXT, "Türk şarkı sözleri", Geo.RUSSIA},
                {CampaignTypeEnum.TEXT, "Türk şarkı sözleri", Geo.UKRAINE},
                {CampaignTypeEnum.TEXT, "Türk şarkı sözleri", Geo.KAZAKHSTAN},
                {CampaignTypeEnum.TEXT, "Türk şarkı sözleri", Geo.AUSTRIA}
        });
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Изменение гео кампании на не соответствующий языку баннера (тексту креатива в ГО) (негативные сценарии)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9281")
    public void testGeoBannerLangValidationNegative() {
        SaveCampRequest saveCampRequest = getSaveCampRequest();

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
    }
}
