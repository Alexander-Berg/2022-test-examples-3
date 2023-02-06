package ru.yandex.autotests.direct.cmd.excel.callouts;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.data.excel.errors.CalloutsErrors;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка ошибок при загрузке уточнений к баннерам через эксель")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.CALLOUTS)
@Tag(BusinessProcessTag.EXCEL)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@RunWith(Parameterized.class)
public class ExcelUploadCalloutsErrorsTest {

    private static final String CLIENT = "direct-test-xls-callouts";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(0)
    public String error;
    @Parameterized.Parameter(1)
    public String excelFileName;


    @Parameterized.Parameters(name = "Уточнения: {0}, имя файла: {1}")
    public static Collection<Object[]> testData() {
        String path = "excel/callouts/";
        return Arrays.asList(new Object[][]{
                {CalloutsErrors.MAX_CALLOUT_LENGTH_EXCEEDED_ERROR.getErrorText(),
                        path + "callouts_more_than_25_symbols.xlsx"},
                {CalloutsErrors.CALLOUTS_ARE_THE_SAME_ERROR.getErrorText(),
                        path + "callouts_the_same.xlsx"},
                {CalloutsErrors.WRONG_SYMBOLS_ERROR.getErrorText(),
                        path + "callouts_wrong_symbols.xlsx"},
                {CalloutsErrors.DIFFERENT_CALLOUTS_IN_ROWS_ERROR.getErrorText(),
                        path + "callouts_different_in_rows.xlsx"},
        });
    }

    @Before
    public void before() {

        TestEnvironment.newDbSteps(CLIENT).bannerAdditionsSteps().clearCalloutsForClient(
                Long.valueOf(User.get(CLIENT).getClientID()));
    }

    @Test
    @Description("Проверка ошибок при загрузке уточнений к баннерам через эксель")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9731")
    public void errorsAtPreImportAdditionsTest() {
        File campFile = ResourceUtils.getResourceAsFile(excelFileName);

        PreImportCampXlsRequest preImportRequest = new PreImportCampXlsRequest()
                .withImportFormat(PreImportCampXlsRequest.ImportFormat.XLS)
                .withJson(true)
                .withXls(campFile.toString())
                .withUlogin(CLIENT);
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps().
                preImportCampaign(preImportRequest);

        assertThat("В ответе содержится ошибка",
                preImportResponse.getErrors(), hasItem(containsString(error)));
    }
}
