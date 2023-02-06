package ru.yandex.autotests.direct.cmd.excel.callouts;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
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

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Уточнения не загружаются в РМП кампанию через эксель")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.CALLOUTS)
@Tag(BusinessProcessTag.EXCEL)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@Tag(CampTypeTag.MOBILE)
public class ExcelDontUploadCallouts {

    private static final String EXCEL_FILE_NAME = "excel/callouts/callouts_rmp.xlsx";
    private static final String CLIENT = "direct-test-xls-callouts-5";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private Integer newCampId;
    private Long clientId;

    @Before
    public void before() {

        clientId = Long.valueOf(User.get(CLIENT).getClientID());
        TestEnvironment.newDbSteps(CLIENT).bannerAdditionsSteps().clearCalloutsForClient(clientId);
    }

    @After
    public void delete() {
        if (newCampId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCampId.longValue());
        }
    }

    @Test
    @Description("Уточнения не загружаются в РМП кампанию через эксель")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9728")
    public void dontUploadCalloutsToRmpTest() {
        File campFile = ResourceUtils.getResourceAsFile(EXCEL_FILE_NAME);
        newCampId = cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(campFile, CLIENT, "",
                ImportCampXlsRequest.DestinationCamp.NEW).getLocationParamAsInteger(LocationParam.CID);

        List<AdditionsItemCalloutsRecord> clientAdditions = TestEnvironment.newDbSteps(CLIENT)
                .bannerAdditionsSteps().getClientCallouts(clientId);

        assertThat("список уточнений клиента пуст", clientAdditions, empty());
    }
}
