package ru.yandex.autotests.direct.cmd.excel;

import java.io.File;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.excel.ExportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка расширенного геотаргетинга при создании кампании через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Features(TestFeatures.EXCEL)
@Tag(TrunkTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class ImportNewCampExtendedGeotargetingTest {

    private static final String EXCEL_CLIENT = "at-direct-excel-rus";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule()
            .overrideCampTemplate(new SaveCampRequest().withExtendedGeotargeting(1))
            .withUlogin(EXCEL_CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private File exportedCamp;
    private Long newCampaignId;


    @After
    public void delete() {
        if (exportedCamp != null) {
            exportedCamp.delete();
        }
        if (newCampaignId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(EXCEL_CLIENT, newCampaignId);
        }
    }

    @Test
    @Description("Проверка расширенного геотаргетинга при создании кампании через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9684")
    public void excelDownloadAndUploadExtendedGeotargetingTest() {
        ExportCampXlsRequest request = new ExportCampXlsRequest()
                .withCid(bannersRule.getCampaignId().toString())
                .withSkipArch(true)
                .withXlsFormat(ExportCampXlsRequest.ExcelFormat.XLSX)
                .withUlogin(EXCEL_CLIENT);
        exportedCamp = cmdRule.cmdSteps().excelSteps().exportCampaignIgnoringLock(request);

        newCampaignId = cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                exportedCamp, EXCEL_CLIENT, bannersRule.getCampaignId().toString(),
                ImportCampXlsRequest.DestinationCamp.NEW)
                .getLocationParamAsInteger(LocationParam.CID).longValue();

        EditCampResponse actualResponse =
                cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), EXCEL_CLIENT);
        assertThat("расширенный геотаргетинг включен", actualResponse.getCampaign().getNoExtendedGeotargeting(),
                equalTo(0));
    }
}
