package ru.yandex.autotests.direct.cmd.banners.imagecreative.geo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
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
@Description("Создание группы с геотергетингом, соответствующим языку баннеров " +
        "(автоопределенному) (позитивные кейсы)")
@Stories(TestFeatures.Banners.CANVAS_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class GroupGeoInCreativeImageBannerMobileAppCampPositiveTest extends GroupGeoInCreativeImageBannerTestBase {

    public GroupGeoInCreativeImageBannerMobileAppCampPositiveTest(CampaignTypeEnum campaignType, String bannerText, Geo geo) {
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
    @Description("Создание группы с геотергетингом, соответствующим языку баннера (тексту креатива в ГО) (позитивные кейсы)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9284")
    public void testGeoBannerLangValidationPositive() {
        Group group = getGroup();
        GroupsParameters request = GroupsParameters.forNewCamp(CLIENT, campaignRule.getCampaignId(), group);

        try {
            cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(request);
        } catch (HttpClientLiteException e) {
            Assert.fail("не удалось создать группу с геотергетингом, соответствующим языку кампании");
        }
    }
}
