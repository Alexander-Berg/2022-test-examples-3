package ru.yandex.autotests.direct.cmd.images.upload;

import java.util.Arrays;
import java.util.Collection;

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
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

@Aqua.Test
@Description("Загрузка картинки для графического баннера из файла и по ссылке (cmd uploadImage)")
@Stories(TestFeatures.BannerImages.UPLOAD_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@RunWith(Parameterized.class)
@Tag(CmdTag.UPLOAD_IMAGE)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
public class UploadImageValidTest extends BaseUploadImageTest {
    private static final ImageUtils.ImageFormat JPG = ImageUtils.ImageFormat.JPG;
    private static final ImageUtils.ImageFormat PNG = ImageUtils.ImageFormat.PNG;
    private static final ImageUtils.ImageFormat GIF = ImageUtils.ImageFormat.GIF;

    @Parameterized.Parameters(name = "Картинка: {1}x{2} {0} загрузка из {3}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {JPG, 336, 280, AbstractImageUploadHelper.UploadType.FILE},
                {GIF, 240, 400, AbstractImageUploadHelper.UploadType.FILE},
                {PNG, 336, 280, AbstractImageUploadHelper.UploadType.FILE},

                {JPG, 970, 250, AbstractImageUploadHelper.UploadType.URL},
                {GIF, 336, 280, AbstractImageUploadHelper.UploadType.URL},
                {PNG, 240, 400, AbstractImageUploadHelper.UploadType.URL},
        });
    }

    @Test
    @Description("Загрузка картинки с правильными форматами и размерами (cmd uploadImage)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9894")
    public void validUploadImage() {
        UploadImageResponse expectedResponse = (UploadImageResponse) new UploadImageResponse().
                withName(imageUploadHelper.getFileImageName()).
                withHeight(height).
                withWidth(width);

        DefaultCompareStrategy strategy = DefaultCompareStrategies.allFields().
                forFields(newPath("hash")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("groupId")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("result")).useMatcher(equalTo(1)).
                forFields(newPath("error")).useMatcher(equalTo(null)).
                forFields(newPath("name")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("scale")).useMatcher(not(isEmptyOrNullString()));

        assertThat("ответ ручки uploadImage соответствует ожидаемому",
                imageUploadHelper.getUploadResponse(),
                BeanDifferMatcher.beanDiffer(expectedResponse).useCompareStrategy(strategy));
    }

}
