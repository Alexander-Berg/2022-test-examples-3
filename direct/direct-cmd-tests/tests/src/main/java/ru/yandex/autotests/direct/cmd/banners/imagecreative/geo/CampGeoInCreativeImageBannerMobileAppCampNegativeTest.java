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
public class CampGeoInCreativeImageBannerMobileAppCampNegativeTest extends CampGeoInCreativeImageBannerTestBase {

    public CampGeoInCreativeImageBannerMobileAppCampNegativeTest(CampaignTypeEnum campaignType, String bannerText, Geo geo) {
        super(campaignType, bannerText, geo);
    }

    @Parameterized.Parameters(name = "Тип кампании: {0}; текст баннера: {1}; Гео: {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.MOBILE, "український текст", Geo.RUSSIA},
                {CampaignTypeEnum.MOBILE, "український текст", Geo.KAZAKHSTAN},
                {CampaignTypeEnum.MOBILE, "український текст", Geo.TURKEY},
                {CampaignTypeEnum.MOBILE, "український текст", Geo.AUSTRIA},

                {CampaignTypeEnum.MOBILE, "Қазақ мәтін", Geo.RUSSIA},
                {CampaignTypeEnum.MOBILE, "Қазақ мәтін", Geo.UKRAINE},
                {CampaignTypeEnum.MOBILE, "Қазақ мәтін", Geo.TURKEY},
                {CampaignTypeEnum.MOBILE, "Қазақ мәтін", Geo.AUSTRIA},

                {CampaignTypeEnum.MOBILE, "Türk şarkı sözleri", Geo.RUSSIA},
                {CampaignTypeEnum.MOBILE, "Türk şarkı sözleri", Geo.UKRAINE},
                {CampaignTypeEnum.MOBILE, "Türk şarkı sözleri", Geo.KAZAKHSTAN},
                {CampaignTypeEnum.MOBILE, "Türk şarkı sözleri", Geo.AUSTRIA}
        });
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Изменение гео кампании на не соответствующий языку баннера (тексту креатива в ГО) (негативные сценарии)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9279")
    public void testGeoBannerLangValidationNegative() {
        SaveCampRequest saveCampRequest = getSaveCampRequest();

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
    }
}
