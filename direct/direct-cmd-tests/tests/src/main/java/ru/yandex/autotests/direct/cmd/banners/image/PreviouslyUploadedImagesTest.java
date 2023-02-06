package ru.yandex.autotests.direct.cmd.banners.image;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Получение списка загруженных через графические баннеры картинки")
@Stories(TestFeatures.Banners.IMAGE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class PreviouslyUploadedImagesTest {
    public static final String CLIENT = "at-direct-banner-image-1";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private NewImagesUploadHelper imagesUploadHelper1 = (NewImagesUploadHelper) new NewImagesUploadHelper()
            .withBannerImageSteps(cmdRule.cmdSteps().bannerImagesSteps())
            .withClient(CLIENT)
            .withImageParams(
                    new ImageParams()
                            .withFormat(ImageUtils.ImageFormat.JPG)
                            .withWidth(300)
                            .withHeight(250)
            );
    private NewImagesUploadHelper imagesUploadHelper2 = (NewImagesUploadHelper) new NewImagesUploadHelper()
            .withBannerImageSteps(cmdRule.cmdSteps().bannerImagesSteps())
            .withClient(CLIENT)
            .withImageParams(
                    new ImageParams()
                            .withFormat(ImageUtils.ImageFormat.JPG)
                            .withWidth(240)
                            .withHeight(400)
            );


    @Before
    public void uploadImages() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannerImagesPoolSteps()
                .deleteBannerImagesPool(Long.valueOf(User.get(CLIENT).getClientID()));
        imagesUploadHelper1.upload();
        imagesUploadHelper2.upload();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9243")
    public void previouslyUploadedImagesOnCampaign() {
        Campaign campaign = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(CLIENT,
                bannersRule.getCampaignId()).getCampaign();

        assertThat("в кампании 2 картинки", campaign.getImageAds(), hasSize(2));
    }

}
