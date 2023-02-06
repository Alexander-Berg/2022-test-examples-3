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
import ru.yandex.autotests.directapi.beans.images.ImagesFormats;
import ru.yandex.autotests.directapi.darkside.model.ImageType;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImageUtils.getImagesFormatsMatcher;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка сохранение форматов картинки в таблице banner_images_formats")
@Stories(TestFeatures.BannerImages.RESIZE_BANNER_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@RunWith(Parameterized.class)
@Tag(CmdTag.AJAX_RESIZE_BANNER_IMAGE)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.TEXT)
@Tag(SmokeTag.YES)
public class BannerImageFormatsSaveTest {

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

    public BannerImageFormatsSaveTest(int sourceWidth, int sourceHeight,
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
                withUlogin(CLIENT);

        cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Картинка: {0}x{1}; resizeX1: {2}, resizeX2: {3}, resizeY1: {4}, resizeY2: {5}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                /* regular min */
                {450, 450, 0, 450, 0, 450, ImageType.REGULAR},
                {600, 450, 0, 600, 0, 450, ImageType.REGULAR},
                {450, 600, 0, 450, 0, 600, ImageType.REGULAR},
                {700, 800, 150, 600, 100, 700, ImageType.REGULAR},
                /* regular medium */
                {1200, 900, 0, 1200, 0, 900, ImageType.REGULAR},
                {1200, 900, 0, 800, 0, 600, ImageType.REGULAR},
                {1080, 607, 0, 600, 0, 607, ImageType.REGULAR},
                /* regular max */
                {3750, 5000, 0, 3750, 0, 5000, ImageType.REGULAR},

                /* wide min */
                {1080, 607, 0, 1080, 0, 607, ImageType.WIDE},
                {1080, 608, 0, 1080, 0, 608, ImageType.WIDE},
                {1200, 900, 0, 1080, 0, 607, ImageType.WIDE},
                {1200, 900, 0, 1080, 0, 608, ImageType.WIDE},
                {1200, 900, 100, 1180, 100, 707, ImageType.WIDE},
                {1200, 900, 100, 1180, 100, 708, ImageType.WIDE},
                /* wide medium */
                {1200, 900, 0, 1200, 225, 900, ImageType.WIDE},
        });
    }

    @Before
    public void before() {

        imageUploadHelper = bannersRule.getImageUploadHelper();
        getShard();
    }

    @Test
    @Description("Проверка сохранение форматов картинки в таблице banner_images_formats")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9882")
    public void testBannerImagesFormats() {
        BannerImagesFormatsRecord record = TestEnvironment.newDbSteps().useShard(shard).imagesSteps()
                .getBannerImagesFormatsRecords(imageUploadHelper.getResizeResponse().getImage());

        ImagesFormats actualImagesFormats = new Gson().fromJson(record.getFormats(), ImagesFormats.class);
        Matcher<ImagesFormats> imagesFormatsMatcher = getImagesFormatsMatcher(imageType);

        // два ассерта в одном тесте из соображений производительности и скорости выполнения тестов
        // (загрузка картинок - долгая операция)
        assertThat("в БД записаны правильные форматы данного типа картинки типа " + imageType.getName(),
                actualImagesFormats, imagesFormatsMatcher);
        assertThat("в БД записан правильный тип картинки",
                record.getImageType().getLiteral(), equalTo(imageType.getName()));
    }

    private void getShard() {
        shard = TestEnvironment.newDbSteps().shardingSteps().getShardByCid(bannersRule.getCampaignId());
    }
}
