package ru.yandex.autotests.direct.cmd.images.resize;

import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.images.AjaxResizeBannerImageResponse;
import ru.yandex.autotests.direct.cmd.data.images.UploadBannerImageResponse;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Ресайз картинки с неправильными размерами (cmd ajaxResizeBannerImage)")
@Stories(TestFeatures.BannerImages.RESIZE_BANNER_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.AJAX_RESIZE_BANNER_IMAGE)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.TEXT)
public class ResizeBannerImageInvalidTest extends BaseResizeBannerImageTest {

    @Parameterized.Parameters(name = "Картинка: {0}x{1}; resizeX1: {2}, resizeX2: {3}, resizeY1: {4}, resizeY2: {5}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                /* regular - small width || height */
                {1200, 900, 0, 449, 0, 598},
                {1200, 900, 0, 598, 0, 449},
                {1200, 900, 10, 459, 10, 510},
                {1200, 900, 0, 449, 0, 449},
                /* regular - invalid x/y */
                {1200, 900, 0, 450, 0, 601},
                {1200, 900, 0, 601, 0, 450},
                {1200, 900, 100, 701, 0, 450},
                {1200, 900, 0, 601, 100, 550},
                /* wide - small width */
                {1079, 607, 0, 1079, 0, 607},
                {1200, 900, 0, 1079, 0, 607},
                {1200, 900, 0, 1079, 0, 608},
                {1200, 900, 100, 1179, 100, 707},
                {1200, 900, 100, 1179, 100, 708},
                /* wide - small height */
                {1080, 606, 0, 1080, 0, 606},
                {1200, 900, 0, 1080, 0, 606},
                {1200, 900, 100, 1180, 100, 706},
                /* wide - invalid x/y min width */
                {1200, 900, 0, 1080, 0, 609},
        });
    }

    @Test
    @Description("Ресайз картинки с неправильными размерами (cmd ajaxResizeBannerImage)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9884")
    public void testImageResizeForInvalidSize() {
        super.test();
    }

    @Override
    protected void testResponse(UploadBannerImageResponse uploadImageResponse,
                                AjaxResizeBannerImageResponse resizeImageResponse) {
        AjaxResizeBannerImageResponse expectedResponse = new AjaxResizeBannerImageResponse()
                .withError(AjaxResizeBannerImageResponse.ERROR_INVALID_IMG_SIZE)
                .withImage(null)
                .withName(null);
        assertThat("на неправильный размер картинки " + getCurrentImgSizeAsString() + " ручка ответила ошибкой",
                resizeImageResponse, beanDiffer(expectedResponse));
    }

    @Override
    protected ImageUploadHelper.UploadType getUploadType() {
        return ImageUploadHelper.UploadType.FILE;
    }
}
