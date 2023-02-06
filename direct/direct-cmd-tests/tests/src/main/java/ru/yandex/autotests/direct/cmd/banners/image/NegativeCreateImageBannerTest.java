package ru.yandex.autotests.direct.cmd.banners.image;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Негативные сценарии сохранения графического баннера в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.IMAGE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
public class NegativeCreateImageBannerTest extends NegativeImageBannerBaseTest {

    public NegativeCreateImageBannerTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        imagesUploadHelper = (NewImagesUploadHelper) new NewImagesUploadHelper()
                .withImageParams(
                        new ImageParams()
                                .withWidth(300)
                                .withHeight(250)
                                .withFormat(ImageUtils.ImageFormat.JPG))
                .withClient(CLIENT);
        campaignRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().as(Logins.SUPER).withRules(campaignRule);
        imagesUploadHelper.withBannerImageSteps(
                cmdRule.cmdSteps().bannerImagesSteps());
    }

    @Before
    public void before() {
        super.before();
        imagesUploadHelper.upload();
        imagesUploadHelper.fillBannerByUploadedImage(newBanner);
    }

    @Override
    public GroupsParameters getMobileGroupParameters() {
        Group group = GroupsFactory.getCommonMobileAppGroup().withBanners(Collections.singletonList(newBanner));
        return GroupsParameters.forNewCamp(CLIENT, campaignRule.getCampaignId(), group);
    }

    @Override
    public GroupsParameters getTextGroupParameters() {
        Group group = GroupsFactory.getDefaultTextGroup().withBanners(Collections.singletonList(newBanner));
        return GroupsParameters.forNewCamp(CLIENT, campaignRule.getCampaignId(), group);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9238")
    public void imageBannerWithoutPictureHash() {
        super.imageBannerWithoutPictureHash();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9239")
    public void imageBannerWithInvalidPictureHash() {
        super.imageBannerWithInvalidPictureHash();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9240")
    public void imageBannerWithoutImageAd() {
        super.imageBannerWithoutImageAd();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9241")
    public void imageBannerWithoutAdType() {
        super.imageBannerWithoutAdType();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9242")
    public void imageBannerWithAnotherLoginImageHash() {
        super.imageBannerWithAnotherLoginImageHash();
    }
}
