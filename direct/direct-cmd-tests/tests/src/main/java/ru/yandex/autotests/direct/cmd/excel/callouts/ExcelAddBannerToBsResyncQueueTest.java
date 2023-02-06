package ru.yandex.autotests.direct.cmd.excel.callouts;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
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
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Попадание баннера в очередь на ленивую переотправку после модификации списка уточнений через эксель")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class ExcelAddBannerToBsResyncQueueTest {

    private static final String EXCEL_PATH = "excel/callouts/";
    private static final String FILE_WITHOUT_CALLOUTS = EXCEL_PATH + "16757746_without_callouts.xlsx";
    private static final String FILE_WITH_ONE_CALLOUT = EXCEL_PATH + "16757746_with_one_callout.xlsx";
    private static final String FILE_WITH_TWO_CALLOUTS = EXCEL_PATH + "16757746_with_2_callouts.xlsx";
    private static final String CLIENT = "direct-test-xls-callouts-4";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(0)
    public String excelState0;
    @Parameterized.Parameter(1)
    public String excelState1;
    @Parameterized.Parameter(2)
    public String desc;

    private Long campaignId = 16757746L;
    private Long bannerId = 1728412683L;

    @Parameterized.Parameters(name = "Попадание баннера в очередь на ленивую переотправку " +
            "после {2} списка уточнений через эксель")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {FILE_WITHOUT_CALLOUTS, FILE_WITH_ONE_CALLOUT, "добавления"},
                {FILE_WITH_ONE_CALLOUT, FILE_WITH_TWO_CALLOUTS, "изменения"},
                {FILE_WITH_TWO_CALLOUTS, FILE_WITHOUT_CALLOUTS, "удаления"}
        });
    }

    @Before
    public void before() {

        Long clientId = Long.valueOf(User.get(CLIENT).getClientID());
        cmdRule.apiAggregationSteps().unArchiveCampaign(CLIENT, campaignId);
        TestEnvironment.newDbSteps(CLIENT).bannerAdditionsSteps().clearCalloutsForClient(clientId);

        TestEnvironment.newDbSteps(CLIENT).bsResyncQueueSteps().deleteCampaignFromBsResyncQueueByCid(campaignId);
        uploadCampaign(CLIENT, campaignId, excelState0);
        cmdRule.darkSideSteps().getCampaignFakeSteps().makeCampaignActive(campaignId);
        cmdRule.darkSideSteps().getBannersFakeSteps().makeBannerActive(bannerId);
    }

    @After
    public void delete() {
        TestEnvironment.newDbSteps(CLIENT).bsResyncQueueSteps().deleteCampaignFromBsResyncQueueByCid(campaignId);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9727")
    public void testAddBannerToBsResyncQueueAfterModifyCalloutsByExcel() {

        uploadCampaign(CLIENT, campaignId, excelState1);
        String statusBsSynced = TestEnvironment.newDbSteps(CLIENT).bannersSteps().getBanner(bannerId).getStatusbssynced().toString();
        assertThat("у баннера сбросился bsSynced", statusBsSynced, equalTo("No"));
    }

    private void uploadCampaign(String login, Long campaignId, String excelFileName) {
        File campFile = ResourceUtils.getResourceAsFile(excelFileName);
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(campFile, login, campaignId.toString(),
                ImportCampXlsRequest.DestinationCamp.OLD);
    }
}
