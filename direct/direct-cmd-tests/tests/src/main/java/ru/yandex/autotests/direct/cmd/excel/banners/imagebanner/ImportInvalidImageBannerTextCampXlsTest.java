package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.data.excel.errors.ImageAdErrors;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Загрузка невалидниго xls файла с графическим объявлением")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
public class ImportInvalidImageBannerTextCampXlsTest extends ImportInvalidImageBannerXlsTestBase {

    private static final String DEFAULT_TEXT_XLS = "excel/image/one_text-image_banner.xls";

    @Override
    protected String getDefaultXls() {
        return DEFAULT_TEXT_XLS;
    }

    @Override
    protected String getImageErrorText() {
        return ImageAdErrors.NEED_IMAGE.getErrorText();
    }

    @Test
    @Description("Загрузка невалидниго xls файла с графическим объявлением без ссылки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9689")
    public void createNewCampImageBannerWithoutHrefTest() {
        ExcelUtils.setCellValue(defaultFile, excelToUpload, ExcelColumnsEnum.HREF, 0, "");

        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload.toString(), PreImportCampXlsRequest.ImportFormat.XLS, CLIENT);

        assertThat("ошибка соответсвует ожиданию", preImportResponse.getErrors(),
                hasItem(containsString(ImageAdErrors.NEED_MAIN_HREF.getErrorText())));
    }

    @Test
    @Override
    @Description("Загрузка невалидниго xls файла с графическим объявлением без картинки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9690")
    public void createNewCampImageBannerWithoutImageTest() {
        super.createNewCampImageBannerWithoutImageTest();
    }
}
