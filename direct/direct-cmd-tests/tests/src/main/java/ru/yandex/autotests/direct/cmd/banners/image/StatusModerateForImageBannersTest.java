package ru.yandex.autotests.direct.cmd.banners.image;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ImagesStatusmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Статус модерации графического баннера в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.IMAGE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class StatusModerateForImageBannersTest {

    private static final String CLIENT = "at-direct-image-banner71";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private ImageBannerRule bannerRule;
    private CampaignTypeEnum campaignType;
    private Long campaignId;
    private Long bannerId;

    public StatusModerateForImageBannersTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannerRule = new ImageBannerRule(campaignType)
                .withImageUploader(new NewImagesUploadHelper())
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannerRule);
    }

    @Parameterized.Parameters(name = "Статус модерации графического баннера. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        campaignId = bannerRule.getCampaignId();
        bannerId = bannerRule.getBannerId();
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT);
    }

    @Test
    @Description("картинка * баннер No")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9250")
    public void statusModerateNoForBanner() {
        TestEnvironment.newDbSteps().bannersSteps().setBannerStatusModerate(bannerId, BannersStatusmoderate.No);

        checkBannerStatusModerate(Arrays.asList(StatusModerate.NO.toString()));
    }

    @Test
    @Description("картинка No баннер *")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9251")
    public void statusModerateNoForImage() {
        TestEnvironment.newDbSteps().imagesSteps().setImageStatusModerate(
                bannerRule.getBannerId(),
                bannerRule.getBanner().getImageAd().getHash(),
                ImagesStatusmoderate.No);

        checkBannerStatusModerate(Arrays.asList(StatusModerate.NO.toString()));
    }

    @Test
    @Description("картинка * баннер RSS")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9252")
    public void statusModerateReadyForBanner() {
        TestEnvironment.newDbSteps().bannersSteps().setBannerStatusModerate(bannerId, BannersStatusmoderate.Ready);

        checkBannerStatusModerate(Arrays.asList(
                StatusModerate.READY.toString(), StatusModerate.SENDING.toString(), StatusModerate.SENT.toString()));
    }

    @Test
    @Description("картинка RSS баннер *")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9253")
    public void statusModerateReadyForImage() {
        TestEnvironment.newDbSteps().imagesSteps().setImageStatusModerate(
                bannerRule.getBannerId(),
                bannerRule.getBanner().getImageAd().getHash(),
                ImagesStatusmoderate.Ready);

        checkBannerStatusModerate(Arrays.asList(
                StatusModerate.READY.toString(), StatusModerate.SENDING.toString(), StatusModerate.SENT.toString()));
    }

    @Test
    @Description("картинка New баннер New")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9254")
    public void statusModerateNewForBannerAndImage() {
        TestEnvironment.newDbSteps().bannersSteps().setBannerStatusModerate(bannerId, BannersStatusmoderate.New);

        TestEnvironment.newDbSteps().imagesSteps().setImageStatusModerate(
                bannerRule.getBannerId(),
                bannerRule.getBanner().getImageAd().getHash(),
                ImagesStatusmoderate.New);

        checkBannerStatusModerate(Arrays.asList(StatusModerate.NEW.toString()));
    }

    private void checkBannerStatusModerate(List expStatuses) {
        Banner banner = cmdRule.cmdSteps().groupsSteps().getBanner(CLIENT, campaignId, bannerId);
        assertThat(
                "статус модерации баннера соответствует ожиданиям",
                expStatuses,
                hasItem(banner.getStatusModerate()));
    }

}
