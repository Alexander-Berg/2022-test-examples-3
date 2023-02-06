package ru.yandex.autotests.direct.cmd.banners.imagecreative.geo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
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
        "(автоопределенному) (позитивные кейсы)")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class CampGeoInCreativeImageBannerMobileAppCampPositiveTest extends CampGeoInCreativeImageBannerTestBase {

    public CampGeoInCreativeImageBannerMobileAppCampPositiveTest(CampaignTypeEnum campaignType, String bannerText, Geo geo) {
        super(campaignType, bannerText, geo);
    }

    @Parameterized.Parameters(name = "Тип кампании: {0}; текст баннера: {1}; Гео: {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.MOBILE, "русский текст", Geo.RUSSIA},
                {CampaignTypeEnum.MOBILE, "русский текст", Geo.UKRAINE},
                {CampaignTypeEnum.MOBILE, "русский текст", Geo.KAZAKHSTAN},

                {CampaignTypeEnum.MOBILE, "английский текст", Geo.AUSTRIA},
                {CampaignTypeEnum.MOBILE, "английский текст", Geo.RUSSIA},
                {CampaignTypeEnum.MOBILE, "английский текст", Geo.UKRAINE},

                {CampaignTypeEnum.MOBILE, "deutsch-text äß", Geo.GERMANY},
                {CampaignTypeEnum.MOBILE, "deutsch-text äß", Geo.RUSSIA},

                {CampaignTypeEnum.MOBILE, "український текст", Geo.UKRAINE},
                {CampaignTypeEnum.MOBILE, "Қазақ мәтін", Geo.KAZAKHSTAN},
                {CampaignTypeEnum.MOBILE, "Türk şarkı sözleri", Geo.TURKEY}
        });
    }

    @Test
    @Description("Изменение геотергетинга кампании, соответствующим языку баннера (тексту креатива в ГО) (позитивные кейсы)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9280")
    public void testGeoBannerLangValidationPositive() {
        SaveCampRequest saveCampRequest = getSaveCampRequest();

        try {
            cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
        } catch (HttpClientLiteException e) {
            Assert.fail("не удалось изменить геотергетинг кампании, соответствующим языку кампании");
        }
    }
}
