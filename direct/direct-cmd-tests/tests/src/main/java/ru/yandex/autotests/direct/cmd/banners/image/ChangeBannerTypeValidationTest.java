package ru.yandex.autotests.direct.cmd.banners.image;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.BannerType;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ImageAd;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collections;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Невозможность изменения типа баннера в ТГО кампании")
@Stories(TestFeatures.Banners.IMAGE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class ChangeBannerTypeValidationTest {

    public static final String CLIENT = "at-backend-image-banner";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    private CampaignRule campaignRule = new CampaignRule().withUlogin(CLIENT).withMediaType(CampaignTypeEnum.TEXT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(Logins.SUPER).withRules(campaignRule);

    private NewImagesUploadHelper imagesUploadHelper = (NewImagesUploadHelper) new NewImagesUploadHelper()
            .withClient(CLIENT)
            .withBannerImageSteps(
                    cmdRule.cmdSteps().bannerImagesSteps()
            );

    @Before
    public void uploadPicture() {
        imagesUploadHelper.upload();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9221")
    public void changeTextToImageBanner() {
        GroupsParameters parameters = GroupsParameters.forExistingCamp(CLIENT, campaignRule.getCampaignId(),
                GroupsFactory.getDefaultTextGroup());
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(parameters);

        Group group = cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, campaignRule.getCampaignId()).get(0)
                .withTags(emptyMap());
        group.getBanners().get(0)
                .withTitle("")
                .withBody("")
                .withAdType(BannerType.IMAGE_AD.toString())
                .withImageAd(new ImageAd().withHash(imagesUploadHelper.getUploadResponse().getHash()));

        parameters = GroupsParameters.forExistingCamp(CLIENT, campaignRule.getCampaignId(), group);

        ErrorResponse error = cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(parameters);

        assertThat("Получили ошибку", error.getError(), equalTo(CommonErrorsResource.WRONG_INPUT_DATA.toString()));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9222")
    public void changeImageToTextBanner() {
        Banner newBanner = BannersFactory.getDefaultTextImageBanner();
        Banner defaultBanner = BannersFactory.getDefaultTextBanner();
        imagesUploadHelper.fillBannerByUploadedImage(newBanner);

        GroupsParameters parameters = GroupsParameters.forExistingCamp(CLIENT, campaignRule.getCampaignId(),
                GroupsFactory.getDefaultTextGroup()
                        .withBanners(Collections.singletonList(newBanner)));


        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(parameters);

        Group group = cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, campaignRule.getCampaignId()).get(0);
        group.getBanners().get(0)
                .withTitle(defaultBanner.getTitle())
                .withBody(defaultBanner.getBody())
                .withImageAd(null)
                .withAdType("text");

        parameters = GroupsParameters.forExistingCamp(CLIENT, campaignRule.getCampaignId(), group);

        ErrorResponse error = cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(parameters);

        assertThat("Получили ошибку", error.getError(), equalTo(CommonErrorsResource.WRONG_INPUT_DATA.toString()));
    }


}
