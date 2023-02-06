package ru.yandex.autotests.direct.cmd.images.mobile.xls;

import org.hamcrest.Matcher;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.model.ImageType;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Создание мобильной кампании с картинкой данного клиента через excel")
@Stories(TestFeatures.BannerImages.CREATE_BANNER_IMAGE_VIA_EXCEL)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.PRE_IMPORT_CAMP_XLS)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@Tag(CmdTag.CONFIRM_SAVE_CAMP_XLS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.MOBILE)
public class CreateMobileBannerWithThisClientImageViaExcelTest extends UploadMobileBannerWithImageViaExcelBase {

    @Test
    @Description("Создание мобильной кампании с картинкой данного клиента через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9878")
    public void importMobileCampWithUploadedImageOfThisClientViaExcel() {
        super.test();
    }

    @Override
    protected Matcher<Banner> getBannerMatcher() {
        String sourceImageName = imageUploader.getResizeResponse().getName();

        return beanDiffer(new Banner().
                withImage(getUploadedImageToSendInXls()).
                withImageType(ImageType.WIDE.getName())).useCompareStrategy(onlyExpectedFields());
    }
}
