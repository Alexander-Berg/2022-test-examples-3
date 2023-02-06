package ru.yandex.autotests.direct.cmd.images;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.images.BannerImageFormats;
import ru.yandex.autotests.direct.cmd.data.images.Formats;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.BannerImagesFormats;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannerImagesFormatsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.direct.cmd.util.ImageUtils.ImageFormat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка сохранение картинки в баннере")
@Stories(TestFeatures.BannerImages.UPLOAD_BANNER_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@RunWith(Parameterized.class)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.TEXT)
@Tag(SmokeTag.YES)
public class BannerImageSaveTest {

    private static final String CLIENT = "at-direct-backend-c";
    private static final ImageFormat FORMAT = ImageFormat.JPG;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;

    private TextBannersRule bannersRule;

    private Group createdGroup;
    private Banner createdBanner;
    private Banner expectedBanner;

    private DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps();

    public BannerImageSaveTest(int sourceWidth, int sourceHeight,
                               int resizeX1, int resizeX2,
                               int resizeY1, int resizeY2) {
        ImageParams imageParams = new ImageParams().
                withFormat(FORMAT).
                withWidth(sourceWidth).
                withHeight(sourceHeight).
                withResizeX1(resizeX1).
                withResizeX2(resizeX2).
                withResizeY1(resizeY1).
                withResizeY2(resizeY2);

        bannersRule = new TextBannersRule().
                withImageUploader((ImageUploadHelper) new ImageUploadHelper()
                        .withImageParams(imageParams)
                        .withClient(CLIENT)
                        .withUploadType(ImageUploadHelper.UploadType.FILE)).
                withMediaType(CampaignTypeEnum.TEXT).
                withUlogin(CLIENT);

        cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Картинка: {0}x{1}; resizeX1: {2}, resizeX2: {3}, resizeY1: {4}, resizeY2: {5}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {450, 450, 0, 450, 0, 450},
                {1080, 607, 0, 1080, 0, 607},
        });
    }

    @Before
    public void before() {
        dbSteps.useShardForLogin(CLIENT);

        createdGroup = getCreatedGroup();
        createdBanner = createdGroup.getBanners().get(0);
        expectedBanner = new Banner();

        expectedBanner.setImageModel(null);
        expectedBanner.setImageSource(null);
        expectedBanner.setImageSourceUrl(null);
        expectedBanner.setImageStatusModerate("New");

    }

    @Test
    @Description("Проверка сохранения картинки в баннере")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9870")
    public void testBannerImagesFormats() {
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("в сохраненном баннере присутствует картинка",
                createdBanner,
                BeanDifferMatcher.beanDiffer(expectedBanner).useCompareStrategy(strategy));
    }

    @Test
    @Description("Проверка формата данных для namespace=direct аватарницы по smart-center в таблице banner_images_formats")
    @TestCaseId("11017")
    public void testBannerImagesFormatsForSmartCenterFeature() {
        BannerImagesFormatsRecord record = dbSteps.imagesSteps().
                getBannerImagesFormatsRecords(createdBanner.getImage().toString());
        assertThat("В таблице banner_images_formats есть запись про smart-center",
                record.getFormats().toString().contains("smart-center"), notNullValue());
    }

    private Group getCreatedGroup() {
        ShowCampMultiEditRequest request = ShowCampMultiEditRequest.forSingleBanner(
                CLIENT, bannersRule.getCampaignId(), bannersRule.getGroupId(), bannersRule.getBannerId());
        ShowCampMultiEditResponse response = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(request);
        return response.getCampaign().getGroups().get(0);
    }
}
