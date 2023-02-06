package ru.yandex.autotests.direct.cmd.banners.image;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.data.images.UploadImageResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Негативные сценарии изменения графического баннера в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.IMAGE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(CmdTag.UPLOAD_IMAGE)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class NegativeChangeImageBannerTest extends NegativeImageBannerBaseTest {

    public NegativeChangeImageBannerTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        imagesUploadHelper = (NewImagesUploadHelper) new NewImagesUploadHelper()
                .withImageParams(
                        new ImageParams()
                                .withWidth(300)
                                .withHeight(250)
                                .withFormat(ImageUtils.ImageFormat.JPG))
                .withClient(CLIENT);
        campaignRule = new ImageBannerRule(campaignType)
                .withImageUploader(imagesUploadHelper)
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);
        imagesUploadHelper.withBannerImageSteps(
                cmdRule.cmdSteps().bannerImagesSteps());

    }

    @Test
    @Description("Нельзя изменить на картинку с другим размером")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9231")
    public void uploadImageBannerWithAnotherSize() {
        List<Banner> bannerList = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, campaignId);
        assumeThat("вернулся список из 1 баннера", bannerList, hasSize(1));
        String bannerId = bannerList.get(0).getBid().toString();
        imagesUploadHelper
                .withBannerId(bannerId)
                .withImageParams(new ImageParams()
                        .withWidth(300)
                        .withHeight(600)
                        .withFormat(ImageUtils.ImageFormat.JPG));
        imagesUploadHelper.upload();
        assertThat("Получили ошибку", imagesUploadHelper.getUploadResponse().getError(),
                equalTo(UploadImageResponse.ERROR_IMG_SIZE_MUST_BE_THE_SAME));
    }

    @Test
    @Description("Нельзя изменить на ранее загруженную картинку с другим размером")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9232")
    public void changeImageToPreviouslyUnloadedAnotherSize() {
        imagesUploadHelper.withImageParams(new ImageParams()
                .withWidth(300)
                .withHeight(500)
                .withFormat(ImageUtils.ImageFormat.JPG));
        imagesUploadHelper.upload();
        ErrorResponse response = saveGroup();
        assertThat("Получили ошибку", response.getError(), equalTo(CommonErrorsResource.WRONG_INPUT_DATA.toString()));
    }

    @Override
    public GroupsParameters getMobileGroupParameters() {
        Group group = GroupsFactory.getCommonMobileAppGroup().withBanners(Collections.singletonList(newBanner));
        group.setAdGroupID(TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps()
                .getPhrasesByCid(campaignId).get(0)
                .getPid().toString());
        return GroupsParameters.forExistingCamp(CLIENT, campaignId, group);
    }

    @Override
    public GroupsParameters getTextGroupParameters() {
        Group group = GroupsFactory.getDefaultTextGroup().withBanners(Collections.singletonList(newBanner));
        group.setAdGroupID(TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps()
                .getPhrasesByCid(campaignId).get(0)
                .getPid().toString());
        return GroupsParameters.forExistingCamp(CLIENT, campaignId, group);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9233")
    public void imageBannerWithoutPictureHash() {
        super.imageBannerWithoutPictureHash();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9234")
    public void imageBannerWithInvalidPictureHash() {
        super.imageBannerWithInvalidPictureHash();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9235")
    public void imageBannerWithoutImageAd() {
        super.imageBannerWithoutImageAd();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9236")
    public void imageBannerWithoutAdType() {
        super.imageBannerWithoutAdType();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9237")
    public void imageBannerWithAnotherLoginImageHash() {
        super.imageBannerWithAnotherLoginImageHash();
    }
}
