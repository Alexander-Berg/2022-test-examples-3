package ru.yandex.autotests.direct.cmd.images.resize;

import java.util.Arrays;
import java.util.Collection;

import com.google.gson.Gson;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannerImagesFormatsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.beans.images.ImagesFormats;
import ru.yandex.autotests.directapi.darkside.model.ImageType;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.steps.images.BannerImageUtils.getImagesFormatsMatcher;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка сохранение форматов картинки в таблице banner_images_formats повторно")
@Stories(TestFeatures.BannerImages.UPLOAD_BANNER_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@RunWith(Parameterized.class)
@Tag(CmdTag.AJAX_RESIZE_BANNER_IMAGE)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.TEXT)
@Tag(SmokeTag.YES)
public class BannerImageFormatsDoubleSaveTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final ImageUtils.ImageFormat FORMAT = ImageUtils.ImageFormat.JPG;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private ImageUploadHelper imageUploadHelper;
    private ImageType imageType;
    private int shard;
    private TextBannersRule bannersRule;

    public BannerImageFormatsDoubleSaveTest(int sourceWidth, int sourceHeight,
                                            int resizeX1, int resizeX2,
                                            int resizeY1, int resizeY2,
                                            ImageType imageType) {
        this.imageType = imageType;
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
                /* regular min */
                {450, 450, 0, 450, 0, 450, ImageType.REGULAR},
                /* wide min */
                {1080, 607, 0, 1080, 0, 607, ImageType.WIDE},
        });
    }

    @Before
    public void before() {

        imageUploadHelper = bannersRule.getImageUploadHelper();
        bannersRule.createGroup();
        getShard();
    }

    @Test
    @Description("Проверка сохранение форматов картинки в таблице banner_images_formats повторно")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9881")
    public void testBannerImagesFormatsDouble() {
        ImagesFormats actualImagesFormats = getSavedImagesFormats();
        Matcher<ImagesFormats> imagesFormatsMatcher = getImagesFormatsMatcher(imageType);

        assertThat("в БД записаны правильные форматы данного типа картинки типа " + imageType.getName(),
                actualImagesFormats, imagesFormatsMatcher);
    }

    private void getShard() {
        shard = TestEnvironment.newDbSteps().shardingSteps().getShardByCid(bannersRule.getCampaignId());
    }

    private ImagesFormats getSavedImagesFormats() {
        BannerImagesFormatsRecord record = TestEnvironment.newDbSteps().useShard(shard).imagesSteps()
                .getBannerImagesFormatsRecords(imageUploadHelper.getResizeResponse().getImage());
        return new Gson().fromJson(record.getFormats(), ImagesFormats.class);
    }
}
