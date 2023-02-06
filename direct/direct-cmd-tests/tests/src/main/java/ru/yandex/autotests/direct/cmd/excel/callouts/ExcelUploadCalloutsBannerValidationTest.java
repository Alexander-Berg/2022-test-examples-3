package ru.yandex.autotests.direct.cmd.excel.callouts;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AdditionsItemCalloutsRecord;
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
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка загрузки уточнений к баннерам через эксель")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.CALLOUTS)
@Tag(BusinessProcessTag.EXCEL)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@RunWith(Parameterized.class)
public class ExcelUploadCalloutsBannerValidationTest {

    private static final String EXCEL_PATH = "excel/callouts/";
    private static final String DEFAULT_EXCEL_FILE = EXCEL_PATH + "16627219_callouts_default_file.xlsx";
    private static final String CLIENT = "direct-test-xls-callouts-2";
    private static final Integer CAMPAIGN_ID = 16627219;
    private static final Long BANNER_ID = 1701686286L;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(0)
    public List<String> additions;
    @Parameterized.Parameter(1)
    public String newExcelFileName;


    @Parameterized.Parameters(name = "Уточнения: {0}, имя файла: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Arrays.asList("bmw"), EXCEL_PATH + "16627219_callouts_delete_one.xlsx"},
                {Arrays.asList("bmw", "opel"), EXCEL_PATH + "16627219_callouts_change_one.xlsx"},
                {emptyList(), EXCEL_PATH + "16627219_callouts_without_additions.xlsx"},
        });
    }

    @Before
    public void before() {


        Long clientId = Long.valueOf(User.get(CLIENT).getClientID());
        cmdRule.apiAggregationSteps().unArchiveCampaign(CLIENT, (long)CAMPAIGN_ID);
        TestEnvironment.newDbSteps(CLIENT).bannerAdditionsSteps().clearCalloutsForClient(clientId);

        File campFile = ResourceUtils.getResourceAsFile(DEFAULT_EXCEL_FILE);
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                campFile, CLIENT, CAMPAIGN_ID.toString(), ImportCampXlsRequest.DestinationCamp.OLD);
    }

    @Test
    @Description("Загрузка уточнений через эксель")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9729")
    public void saveAdditionsTest() {
        File campFile = ResourceUtils.getResourceAsFile(newExcelFileName);
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                campFile, CLIENT, CAMPAIGN_ID.toString(), ImportCampXlsRequest.DestinationCamp.OLD);

        List<AdditionsItemCalloutsRecord> newBannerAdditions = TestEnvironment.newDbSteps(CLIENT).
                bannerAdditionsSteps().getBannerCallouts(BANNER_ID);

        List<String> newAdditionsTexts = newBannerAdditions
                .stream()
                .map(AdditionsItemCalloutsRecord::getCalloutText)
                .collect(toList());

        assertThat("Загруженные уточнения соответствуют ожидаемым", newAdditionsTexts, beanDiffer(additions));
    }
}
