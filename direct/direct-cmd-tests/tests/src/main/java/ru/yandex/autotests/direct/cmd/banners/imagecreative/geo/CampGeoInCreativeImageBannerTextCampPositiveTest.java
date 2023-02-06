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
public class CampGeoInCreativeImageBannerTextCampPositiveTest extends CampGeoInCreativeImageBannerTestBase {

    public CampGeoInCreativeImageBannerTextCampPositiveTest(CampaignTypeEnum campaignType, String bannerText, Geo geo) {
        super(campaignType, bannerText, geo);
    }

    @Parameterized.Parameters(name = "Тип кампании: {0}; текст баннера: {1}; Гео: {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT, "русский текст", Geo.RUSSIA},
                {CampaignTypeEnum.TEXT, "русский текст", Geo.UKRAINE},
                {CampaignTypeEnum.TEXT, "русский текст", Geo.KAZAKHSTAN},

                {CampaignTypeEnum.TEXT, "английский текст", Geo.AUSTRIA},
                {CampaignTypeEnum.TEXT, "английский текст", Geo.RUSSIA},
                {CampaignTypeEnum.TEXT, "английский текст", Geo.UKRAINE},

                {CampaignTypeEnum.TEXT, "deutsch-text äß", Geo.GERMANY},
                {CampaignTypeEnum.TEXT, "deutsch-text äß", Geo.RUSSIA},

                {CampaignTypeEnum.TEXT, "український текст", Geo.UKRAINE},
                {CampaignTypeEnum.TEXT, "Қазақ мәтін", Geo.KAZAKHSTAN},
                {CampaignTypeEnum.TEXT, "Türk şarkı sözleri", Geo.TURKEY}
        });
    }

    @Test
    @Description("Изменение геотергетинга кампании, соответствующим языку баннера (тексту креатива в ГО) (позитивные кейсы)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9282")
    public void testGeoBannerLangValidationPositive() {
        SaveCampRequest saveCampRequest = getSaveCampRequest();

        try {
            cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
        } catch (HttpClientLiteException e) {
            Assert.fail("не удалось изменить геотергетинг кампании, соответствующим языку кампании");
        }
    }
}
