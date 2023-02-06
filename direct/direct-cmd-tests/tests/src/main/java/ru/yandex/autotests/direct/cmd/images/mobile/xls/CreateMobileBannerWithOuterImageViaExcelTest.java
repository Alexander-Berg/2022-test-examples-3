package ru.yandex.autotests.direct.cmd.images.mobile.xls;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.clients.s3.S3Helper;
import ru.yandex.autotests.directapi.darkside.model.ImageType;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.File;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.direct.cmd.util.ImageUtils.createImageInTempFile;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Создание мобильной кампании с картинкой со стороннего ресурса через excel")
@Stories(TestFeatures.BannerImages.CREATE_BANNER_IMAGE_VIA_EXCEL)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.PRE_IMPORT_CAMP_XLS)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@Tag(CmdTag.CONFIRM_SAVE_CAMP_XLS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.MOBILE)
public class CreateMobileBannerWithOuterImageViaExcelTest extends UploadMobileBannerWithImageViaExcelBase {

    private static final ImageParams WIDE_IMAGE_PARAMS = new ImageParams().
            withFormat(ImageUtils.ImageFormat.JPG).
            withWidth(1080).
            withHeight(607);

    private S3Helper s3Helper;

    private File imageFile;
    private String storageImagePath;
    private String storageImageUrl;

    @Before
    public void before() {
        s3Helper = S3Helper.getInstance();
        createImageAtDropbox();
        super.before();
    }

    @After
    public void after() {
        if (imageFile != null) {
            imageFile.delete();
        }
        if (storageImagePath != null) {
            s3Helper.deleteTemporaryObject(storageImagePath);
        }
        super.after();
    }

    @Test
    @Description("Создание мобильной кампании с картинкой со стороннего ресурса через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9877")
    public void importMobileCampWithImageFromOuterUrlViaExcel() {
        super.test();
    }

    @Override
    protected DirectCmdRule createDirectCmdRule() {
        return DirectCmdRule.defaultRule();
    }

    private void createImageAtDropbox() {
        imageFile = createImageInTempFile(WIDE_IMAGE_PARAMS.getWidth(),
                WIDE_IMAGE_PARAMS.getHeight(), WIDE_IMAGE_PARAMS.getFormat());
        storageImagePath = imageFile.getName();
        storageImageUrl = s3Helper.putTemporaryObject(storageImagePath, imageFile);
    }

    @Override
    protected Matcher<Banner> getBannerMatcher() {
        Banner expectedBanner = new Banner().
                withImageType(ImageType.WIDE.getName()).
                withImageName(new File(storageImagePath).getName());

        CompareStrategy compareStrategy = onlyExpectedFields().
                forFields(newPath("imageName")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("image")).useMatcher(not(isEmptyOrNullString()));

        return beanDiffer(expectedBanner).useCompareStrategy(compareStrategy);
    }

    @Override
    protected String getImageUrlToSendInXls() {
        return storageImageUrl;
    }

    @Override
    protected String getUploadedImageToSendInXls() {
        throw new IllegalStateException("this method must not be used in this test");
    }

    @Override
    protected String getUploadedImageGroupToSendInXls() {
        throw new IllegalStateException("this method must not be used in this test");
    }
}
