package ru.yandex.autotests.direct.cmd.images.upload;

import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.images.UploadBannerImageResponse;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

@Aqua.Test
@Description("Позитивные сценарии загрузки картинки для баннера из файла (cmd uploadBannerImage)")
@Stories(TestFeatures.BannerImages.UPLOAD_BANNER_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.AJAX_RESIZE_BANNER_IMAGE)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.TEXT)
public class UploadBannerImageFromFileValidTest extends BaseUploadBannerImageTest {

    private static final ImageUtils.ImageFormat JPG = ImageUtils.ImageFormat.JPG;
    private static final ImageUtils.ImageFormat PNG = ImageUtils.ImageFormat.PNG;
    private static final ImageUtils.ImageFormat GIF = ImageUtils.ImageFormat.GIF;

    @Parameterized.Parameters(name = "Картинка: {1}x{2} {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {JPG, 450, 450},
                {JPG, 600, 450},
                {JPG, 450, 600},
                {JPG, 5000, 3750},
                {JPG, 3750, 5000},
                {JPG, 842, 621},      // отношение сторон не равно 1/1, 3/4, 4/3

                {JPG, 1080, 607},
                {JPG, 5000, 2813},
                {JPG, 5000, 2814},

                {JPG, 450, 450},
                {PNG, 450, 450},
                {GIF, 450, 450}
        });
    }

    @Test
    @Description("Загрузка правильной картинки для баннера из файла (cmd uploadBannerImage)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9888")
    public void testValidUploadImageByFile() {
        super.test();
    }

    @Override
    protected void testResponse(UploadBannerImageResponse uploadResponse) {
        UploadBannerImageResponse expectedResponse = new UploadBannerImageResponse().
                withName(imageUploadHelper.getFileImageName()).
                withOrigWidth(width).
                withOrigHeight(height);

        DefaultCompareStrategy strategy = DefaultCompareStrategies.allFields().
                forFields(newPath("image")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("uploadId")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("width")).useMatcher(lessThanOrEqualTo(width)).
                forFields(newPath("height")).useMatcher(lessThanOrEqualTo(height));

        assertThat("ответ ручки uploadBannerImage соответствует ожидаемому",
                uploadResponse, BeanDifferMatcher.beanDiffer(expectedResponse).useCompareStrategy(strategy));
    }

    @Override
    protected ImageUploadHelper.UploadType getUploadType() {
        return ImageUploadHelper.UploadType.FILE;
    }
}
