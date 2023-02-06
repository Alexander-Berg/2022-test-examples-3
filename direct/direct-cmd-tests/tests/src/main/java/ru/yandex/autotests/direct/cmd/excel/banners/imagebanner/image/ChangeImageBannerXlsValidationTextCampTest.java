package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.image;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.data.excel.errors.ImageAdErrors;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Ошибки при изменении параметров ГО баннера в группе через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(CampTypeTag.TEXT)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
public class ChangeImageBannerXlsValidationTextCampTest extends ChangeImageBannerXlsValidationTestBase {

    public ChangeImageBannerXlsValidationTextCampTest() {
        super(CampaignTypeEnum.TEXT);
    }

    @Test
    @Description("Удаление ссылки ГО баннера в группе через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9716")
    public void deleteBannerHrefViaXls() {
        ExcelUtils.setCellValue(tempExcel, excelToUpload, ExcelColumnsEnum.HREF, 0, "");
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload.toString(), PreImportCampXlsRequest.ImportFormat.XLS, CLIENT);

        assertThat("ошибка соответсвует ожиданию", preImportResponse.getErrors(),
                hasItem(containsString("Строка 12: " + ImageAdErrors.NEED_HREF.getErrorText())));
    }

    @Test
    @Description("Изменение ссылки на невалидную ГО баннера в группе через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9716")
    public void changeToInvalidBannerHrefViaXls() {
        ExcelUtils.setCellValue(tempExcel, excelToUpload, ExcelColumnsEnum.HREF, 0, "1223");
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload.toString(), PreImportCampXlsRequest.ImportFormat.XLS, CLIENT);

        assertThat("ошибка соответсвует ожиданию", preImportResponse.getErrors(),
                hasItem(containsString("Строка 12: " + ImageAdErrors.WRONG_HREF.getErrorText())));
    }

    @Test
    @Description("Изменение c обычного на мобильное ГО баннера в группе через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9718")
    public void changeToMobileBannerTypeViaXls() {
        ExcelUtils.setCellValue(tempExcel, excelToUpload, ExcelColumnsEnum.IS_MOBILE, 1, "+");
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload.toString(), PreImportCampXlsRequest.ImportFormat.XLS, CLIENT);

        assertThat("предупреждение соответсвует ожиданию", preImportResponse.getWarnings(),
                hasItem(containsString("Строка 13: " + ImageAdErrors.MOBILE_IMAGE_AD_WARNING.getErrorText())));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9717")
    public void changeImageAnotherSizeViaXls() {
        super.changeImageAnotherSizeViaXls();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9720")
    public void changeImageToCreativeViaXls() {
        super.changeImageToCreativeViaXls();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9721")
    public void changeBannerTypeViaXls() {
        super.changeBannerTypeViaXls();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9719")
    public void deleteImageViaXls() {
        super.deleteImageViaXls();
    }
}
