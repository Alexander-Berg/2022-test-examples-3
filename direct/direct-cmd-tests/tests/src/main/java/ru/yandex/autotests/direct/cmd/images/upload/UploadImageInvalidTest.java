package ru.yandex.autotests.direct.cmd.images.upload;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.images.UploadImageResponse;
import ru.yandex.autotests.direct.cmd.steps.images.AbstractImageUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Негативные сценарии загрузки картинки для графического баннера из файла и по ссылке (cmd uploadImage)")
@Stories(TestFeatures.BannerImages.UPLOAD_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@RunWith(Parameterized.class)
@Tag(CmdTag.UPLOAD_IMAGE)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
public class UploadImageInvalidTest extends BaseUploadImageTest {
    private static final ImageUtils.ImageFormat JPG = ImageUtils.ImageFormat.JPG;
    private static final ImageUtils.ImageFormat PNG = ImageUtils.ImageFormat.PNG;
    private static final ImageUtils.ImageFormat GIF = ImageUtils.ImageFormat.GIF;
    private static final ImageUtils.ImageFormat BMP = ImageUtils.ImageFormat.BMP;

    @Parameterized.Parameter(4)
    public String expectedError;

    @Parameterized.Parameters(name = "Картинка: {1}x{2} {0} загрузка из {3}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{

                //Неверный размер изображения
                {JPG, 400, 240, AbstractImageUploadHelper.UploadType.FILE, UploadImageResponse.ERROR_IMG_SIZE_INVALID},
                {GIF, 239, 400, AbstractImageUploadHelper.UploadType.FILE, UploadImageResponse.ERROR_IMG_SIZE_INVALID},
                //Неверный формат изображения
                {BMP, 240, 400, AbstractImageUploadHelper.UploadType.FILE, UploadImageResponse.ERROR_IMG_FORMAT_INVALID},
                //Неверный размер изображения
                {PNG, 639, 960, AbstractImageUploadHelper.UploadType.URL, UploadImageResponse.ERROR_IMG_SIZE_TOO_BIG},
                {GIF, 500, 300, AbstractImageUploadHelper.UploadType.URL, UploadImageResponse.ERROR_IMG_SIZE_INVALID},
                //Неверный формат изображения
                {BMP, 970, 250, AbstractImageUploadHelper.UploadType.URL, UploadImageResponse.ERROR_IMG_SIZE_TOO_BIG},
        });
    }

    @Test
    @Description("Загрузка картинки с неправильными форматами и размерами (cmd uploadImage)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9893")
    public void testValidUploadImageByFile() {
        UploadImageResponse expectedResponse = new UploadImageResponse()
                .withError(expectedError);
        assertThat("ответ ручки uploadBannerImage содержит сообщение об ошибке",
                imageUploadHelper.getUploadResponse(), beanDiffer(expectedResponse)
                        .useCompareStrategy(onlyExpectedFields()));
    }

}
