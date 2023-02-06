package ru.yandex.autotests.direct.cmd.excel;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ExportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.data.excel.errors.ImageAdErrors;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.File;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Построчная валидация при обработке xls и xlsx файлов")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(CampTypeTag.TEXT)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
public class ImportXlsxErrorsByLineTest {

    private static final String CLIENT = "at-direct-excel-line-error";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private File exportedCamp;
    private File excelToUpload;
    private Long campaignId;

    private TextBannersRule bannerRule = new TextBannersRule()
            .overrideGroupTemplate(BeanLoadHelper.loadCmdBean(
                    CmdBeans.COMMON_REQUEST_GROUP_TEXT_WITH_TWO_BANNERS, Group.class))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannerRule);

    @Before
    public void before() {
        campaignId = bannerRule.getCampaignId();
        ExportCampXlsRequest request = new ExportCampXlsRequest()
                .withCid(campaignId.toString())
                .withSkipArch(true)
                .withXlsFormat(ExportCampXlsRequest.ExcelFormat.XLSX)
                .withUlogin(CLIENT);
        exportedCamp = cmdRule.cmdSteps().excelSteps().exportCampaignIgnoringLock(request);
        try {
            excelToUpload = File.createTempFile(RandomUtils.getString(10), ".xlsx");
        } catch (Exception e) {
            throw new IllegalStateException("ошибка создания временного excel-файла", e);
        }
    }

    @After
    public void after() {
        FileUtils.deleteQuietly(exportedCamp);
    }

    @Test
    @Description("Загружаем из файла текстовое объявление без ссылки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10661")
    public void ImportXlsWithoutHref() {
        ExcelUtils.setCellValue(exportedCamp, excelToUpload, ExcelColumnsEnum.HREF, 0, "");
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload.toString(), PreImportCampXlsRequest.ImportFormat.XLSX, CLIENT);
        assertThat("ошибка соответсвует ожиданию", preImportResponse.getErrors(),
                hasItem(containsString("Строка 12: " + ImageAdErrors.NEED_HREF.getErrorText())));
    }

    @Test
    @Description("Загружаем из файла текстовое объявление с неправильной ссылкой")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10663")
    public void ImportXlsWithInvalidHref() {
        ExcelUtils.setCellValue(exportedCamp, excelToUpload, ExcelColumnsEnum.HREF, 0, "1234");
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload.toString(), PreImportCampXlsRequest.ImportFormat.XLSX, CLIENT);
        assertThat("ошибка соответсвует ожиданию", preImportResponse.getErrors(),
                hasItem(containsString("Строка 12: " + ImageAdErrors.WRONG_HREF.getErrorText())));
    }

    @Test
    @Description("Загружаем из файла текстовое объявление без текста")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10662")
    public void ImportXlsWithoutText() {
        ExcelUtils.setCellValue(exportedCamp, excelToUpload, ExcelColumnsEnum.TEXT, 0, "");
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload.toString(), PreImportCampXlsRequest.ImportFormat.XLSX, CLIENT);
        assertThat("ошибка соответсвует ожиданию", preImportResponse.getErrors(),
                hasItem(containsString("Строка 12: " + ImageAdErrors.NEED_TEXT.getErrorText())));
    }

}

