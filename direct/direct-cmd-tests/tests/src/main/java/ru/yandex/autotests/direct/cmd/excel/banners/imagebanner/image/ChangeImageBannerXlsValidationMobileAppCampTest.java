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
@Tag(CampTypeTag.MOBILE)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
public class ChangeImageBannerXlsValidationMobileAppCampTest extends ChangeImageBannerXlsValidationTestBase {

    public ChangeImageBannerXlsValidationMobileAppCampTest() {
        super(CampaignTypeEnum.MOBILE);
    }

    @Test
    @Description("Удаление ссылки на приложение ГО баннера в группе через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9713")
    public void deleteImageViaXls() {
        ExcelUtils.setCellValue(tempExcel, excelToUpload, ExcelColumnsEnum.APP_HREF, 0, "");
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload.toString(), PreImportCampXlsRequest.ImportFormat.XLS, CLIENT);

        assertThat("ошибка соответсвует ожиданию", preImportResponse.getErrors(),
                hasItem(containsString("Строка 12: " + ImageAdErrors.NEED_APP_HREF.getErrorText())));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9712")
    public void changeImageAnotherSizeViaXls() {
        super.changeImageAnotherSizeViaXls();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9714")
    public void changeImageToCreativeViaXls() {
        super.changeImageToCreativeViaXls();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9715")
    public void changeBannerTypeViaXls() {
        super.changeBannerTypeViaXls();
    }


}
