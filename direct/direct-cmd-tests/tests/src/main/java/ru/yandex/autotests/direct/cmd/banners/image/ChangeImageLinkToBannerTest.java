package ru.yandex.autotests.direct.cmd.banners.image;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.BannerType;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ImagesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;


@Aqua.Test
@Description("Изменение изображения графического баннера в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.IMAGE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class ChangeImageLinkToBannerTest {

    private static final String ULOGIN = "at-backend-image-banner";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    private ImageBannerRule imageBannerRule = new ImageBannerRule(CampaignTypeEnum.TEXT)
            .withImageUploader(new NewImagesUploadHelper())
            .withUlogin(ULOGIN);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(imageBannerRule);

    private NewImagesUploadHelper imagesUploadHelper = (NewImagesUploadHelper) new NewImagesUploadHelper()
            .withBannerImageSteps(cmdRule.cmdSteps().bannerImagesSteps())
            .withClient(ULOGIN)
            .withImageParams(
                    new ImageParams()
                            .withWidth(300)
                            .withHeight(250)
                            .withFormat(ImageUtils.ImageFormat.JPG)
            );


    @Before
    public void changeImage() {
        imagesUploadHelper.upload();
        Group group = imageBannerRule.getCurrentGroup().withTags(emptyMap());
        imagesUploadHelper.fillBannerByUploadedImage(group.getBanners().get(0));

        GroupsParameters request = GroupsParameters.forExistingCamp(ULOGIN, imageBannerRule.getCampaignId(), group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(request);
    }


    @Test
    @Description("Должна замениться картинка для баннера после пересохранения с новой")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9223")
    public void pictureChanged() {
        Group group = imageBannerRule.getCurrentGroup();
        assumeThat("В группе 1 баннер", group.getBanners(), hasSize(1));

        Banner actualBanner = group.getBanners().get(0);
        Banner expectedBanner = NewImagesUploadHelper
                .fromUploadPictureResponse(imagesUploadHelper.getUploadResponse())
                .withAdType(BannerType.IMAGE_AD.toString());

        assertThat("Сохранена новая картинка на баннере", actualBanner, beanDiffer(expectedBanner)
                .useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Хеш картинки в базе заменен на новую")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9224")
    public void linkInDataBaseShouldBeReplaced() {
        ImagesRecord actualRecord = TestEnvironment.newDbSteps().useShardForLogin(ULOGIN).imagesSteps()
                .getImagesRecords(imageBannerRule.getCampaignId(), imageBannerRule.getGroupId(), imageBannerRule.getBannerId());

        assertThat("хеш картинки изменился", actualRecord.getImageHash(),
                equalTo(imagesUploadHelper.getUploadResponse().getHash()));
    }
}
