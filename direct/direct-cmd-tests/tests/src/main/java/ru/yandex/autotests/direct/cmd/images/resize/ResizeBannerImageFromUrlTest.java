package ru.yandex.autotests.direct.cmd.images.resize;

import org.hamcrest.Matcher;
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
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

@Aqua.Test
@Description("Ресайз картинки после загрузки по урлу")
@Stories(TestFeatures.BannerImages.RESIZE_BANNER_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.AJAX_RESIZE_BANNER_IMAGE)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.TEXT)
public class ResizeBannerImageFromUrlTest extends BaseResizeBannerImageTest {

    @Parameterized.Parameters(name = "Картинка: {0}x{1}; resizeX1: {2}, resizeX2: {3}, resizeY1: {4}, resizeY2: {5}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                /* regular min */
                {450, 450, 0, 450, 0, 450},
                /* regular medium */
                {1200, 900, 0, 1200, 0, 900},
                /* regular big */
                // doesn't work:
                // DIRECT-101068: Не работает загрузка большого изображения
                // CV-1353: [cbird] Проблема с картинками от direct
                // {5000, 5000, 0, 5000, 0, 5000},

                /* wide min */
                {1080, 607, 0, 1080, 0, 607},
                /* wide medium */
                {1200, 900, 0, 1200, 225, 900},
                /* wide max */
                 {5000, 5000, 0, 5000, 0, 2813}
        });
    }

    @Test
    @Description("Ресайз картинки после загрузки по урлу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9883")
    public void testImageResizeAfterUploadingByUrl() {
        super.test();
    }

    @Override
    protected void testResponse(UploadBannerImageResponse uploadImageResponse,
                                AjaxResizeBannerImageResponse resizeImageResponse) {
        assertThat("для картинки " + getCurrentImgSizeAsString() + " ответ соответствует ожидаемому",
                resizeImageResponse, getResponseMatcher());
    }

    @Override
    protected ImageUploadHelper.UploadType getUploadType() {
        return ImageUploadHelper.UploadType.URL;
    }

    private Matcher<AjaxResizeBannerImageResponse> getResponseMatcher() {
        AjaxResizeBannerImageResponse expectedResponse = new AjaxResizeBannerImageResponse()
                .withName(imageUploadHelper.getStorageImageName());

        DefaultCompareStrategy strategy = DefaultCompareStrategies.allFields().
                forFields(newPath("error")).useMatcher(nullValue()).
                forFields(newPath("image")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("name")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("mdsGroupId")).useMatcher(not(nullValue()));

        return beanDiffer(expectedResponse).useCompareStrategy(strategy);
    }
}
