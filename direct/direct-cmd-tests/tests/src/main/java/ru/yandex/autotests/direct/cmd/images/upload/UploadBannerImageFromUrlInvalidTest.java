package ru.yandex.autotests.direct.cmd.images.upload;

import org.junit.Test;
import org.junit.runner.RunWith;
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
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Негативные сценарии загрузки картинки для баннера по ссылке (cmd uploadBannerImage)")
@Stories(TestFeatures.BannerImages.UPLOAD_BANNER_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@RunWith(Parameterized.class)
@Tag(CmdTag.AJAX_RESIZE_BANNER_IMAGE)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.TEXT)
public class UploadBannerImageFromUrlInvalidTest extends BaseUploadBannerImageTest {

    private static final ImageUtils.ImageFormat JPG = ImageUtils.ImageFormat.JPG;
    private static final ImageUtils.ImageFormat BMP = ImageUtils.ImageFormat.BMP;
    @Parameterized.Parameter(3)
    public String expectedError;

    @Parameterized.Parameters(name = "Картинка: {1}x{2} {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
//                {JPG, 449, 450, UploadBannerImageResponse.ERROR_IMG_SIZE_TOO_SMALL},
//                {JPG, 450, 449, UploadBannerImageResponse.ERROR_IMG_SIZE_TOO_SMALL},
                {JPG, 449, 449, UploadBannerImageResponse.ERROR_IMG_SIZE_TOO_SMALL},
//                {JPG, 449, 598, UploadBannerImageResponse.ERROR_IMG_SIZE_TOO_SMALL},
//                {JPG, 598, 449, UploadBannerImageResponse.ERROR_IMG_SIZE_TOO_SMALL},
//                {JPG, 2814, 5001, UploadBannerImageResponse.ERROR_IMG_SIZE_TOO_BIG},
//                {JPG, 5001, 2814, UploadBannerImageResponse.ERROR_IMG_SIZE_TOO_BIG},
                {JPG, 5001, 5001, UploadBannerImageResponse.ERROR_IMG_SIZE_TOO_BIG},
                {BMP, 450, 450, UploadBannerImageResponse.ERROR_IMG_FORMAT_INVALID}
        });
    }

    @Test
    @Description("Загрузка неправильной картинки для баннера по урлу (cmd uploadBannerImage)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9889")
    public void testInvalidUploadImageByUrl() {
        super.test();
    }

    @Override
    protected void testResponse(UploadBannerImageResponse uploadResponse) {
        UploadBannerImageResponse expectedResponse = new UploadBannerImageResponse().withError(expectedError);
        assertThat("ответ ручки uploadBannerImage содержит только сообщение об ошибке",
                uploadResponse, BeanDifferMatcher.beanDiffer(expectedResponse));
    }

    @Override
    protected ImageUploadHelper.UploadType getUploadType() {
        return ImageUploadHelper.UploadType.URL;
    }
}
