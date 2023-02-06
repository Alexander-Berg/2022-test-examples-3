package ru.yandex.autotests.direct.cmd.banners.imagecreative.geo;

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
        "(автоопределенному) (негативные кейсы)")
@Stories(TestFeatures.Banners.CANVAS_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class GroupGeoInCreativeImageBannerTextCampNegativeTest extends GroupGeoInCreativeImageBannerTestBase {

    public GroupGeoInCreativeImageBannerTextCampNegativeTest(CampaignTypeEnum campaignType, String bannerText, Geo geo) {
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
    @Description("Создание группы с геотергетингом, не соответствующим языку баннера (тексту креатива в ГО) (негативные сценарии)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9285")
    public void testGeoBannerLangValidationNegative() {
        Group group = getGroup();
        GroupsParameters request = GroupsParameters.forNewCamp(CLIENT, campaignRule.getCampaignId(), group);

        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(request);
    }
}
