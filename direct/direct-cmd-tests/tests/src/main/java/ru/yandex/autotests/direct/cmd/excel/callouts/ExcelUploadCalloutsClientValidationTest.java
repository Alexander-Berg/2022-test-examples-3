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
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
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

import static java.util.stream.Collectors.toList;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Загрузка уточнений к баннерам через эксель")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class ExcelUploadCalloutsClientValidationTest {

    private static final String EXCEL_PATH = "excel/callouts/";
    private static final String DEFAULT_EXCEL_FILE = EXCEL_PATH + "16757868_callouts_default_file.xlsx";
    private static final String CLIENT = "direct-test-xls-callouts-3";
    private static final Long CAMPAIGN_ID = 16757868L;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(0)
    public List<String> additions;
    @Parameterized.Parameter(1)
    public String newExcelFileName;

    private Long clientId;

    @Parameterized.Parameters(name = "Уточнения: {0}, имя файла: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Arrays.asList("audi", "bmw"), EXCEL_PATH + "16757868_callouts_delete_one.xlsx"},
                {Arrays.asList("audi", "bmw", "opel"), EXCEL_PATH + "16757868_callouts_change_one.xlsx"},
                {Arrays.asList("audi", "bmw"), EXCEL_PATH + "16757868_callouts_without_additions.xlsx"},
        });
    }

    @Before
    public void before() {

        clientId = Long.valueOf(User.get(CLIENT).getClientID());

        TestEnvironment.newDbSteps(CLIENT).bannerAdditionsSteps().clearCalloutsForClient(clientId);
        cmdRule.apiAggregationSteps().unArchiveCampaign(CLIENT, CAMPAIGN_ID);

        File campFile = ResourceUtils.getResourceAsFile(DEFAULT_EXCEL_FILE);
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(campFile, CLIENT, CAMPAIGN_ID.toString(),
                ImportCampXlsRequest.DestinationCamp.OLD);
    }

    @Test
    @Description("Загрузка уточнений к баннерам через эксель")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9730")
    public void saveAdditionsTest() {
        File campFile = ResourceUtils.getResourceAsFile(newExcelFileName);
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(campFile, CLIENT, CAMPAIGN_ID.toString(),
                ImportCampXlsRequest.DestinationCamp.OLD);

        List<AdditionsItemCalloutsRecord> clientAdditions = TestEnvironment.newDbSteps(CLIENT).
                bannerAdditionsSteps().getClientCallouts(clientId);

        List<String> newAdditionsTexts = clientAdditions
                .stream()
                .map(AdditionsItemCalloutsRecord::getCalloutText)
                .collect(toList());

        assertThat("уточнения клиента соответствуют ожидаемым", newAdditionsTexts, beanDiffer(additions));
    }
}
