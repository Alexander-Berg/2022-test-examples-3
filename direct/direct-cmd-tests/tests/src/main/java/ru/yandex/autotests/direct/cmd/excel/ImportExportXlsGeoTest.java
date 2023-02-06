package ru.yandex.autotests.direct.cmd.excel;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.excel.ExportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.api5.bids.BidSetItemMap;
import ru.yandex.autotests.directapi.model.api5.bids.SetRequestMap;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Сохранение корректного региона при экспорте/импорте между российским и украинским клиентами")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class ImportExportXlsGeoTest {

    private static final String CLIENT_RU = "at-direct-excel-rus-1";
    private static final String CLIENT_UA = "at-direct-excel-ua-1";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    public String firstExcelClient;
    public String secondExcelClient;
    public String firstClientGeo;
    public String secondClientGeo;
    @Rule
    public DirectCmdRule cmdRule;
    private TextBannersRule bannersRule;
    private File exportedCamp;
    private Long campaignID;
    private Long newCampaignID;

    public ImportExportXlsGeoTest(String firstExcelClient, String secondExcelClient, String firstClientGeo,
            String secondClientGeo)
    {
        this.firstExcelClient = firstExcelClient;
        this.secondExcelClient = secondExcelClient;
        this.firstClientGeo = firstClientGeo;
        this.secondClientGeo = secondClientGeo;
        bannersRule = new TextBannersRule()
                .overrideGroupTemplate(new Group().withGeo(firstClientGeo))
                .withUlogin(firstExcelClient);
        cmdRule = DirectCmdRule.defaultRule().as(Logins.SUPER).withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Первый клиент: {0}, второй клиент: {1}, " +
            "регион у первого клиента: {2}, регион у второго клиента: {3}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CLIENT_RU, CLIENT_UA, "225,977", "225"},
                {CLIENT_RU, CLIENT_UA, "225", "225"},
                {CLIENT_RU, CLIENT_UA, "187", "187"},
                {CLIENT_RU, CLIENT_UA, "187,977", "187"},

                {CLIENT_UA, CLIENT_RU, "225,977", "225"},
                {CLIENT_UA, CLIENT_RU, "225", "225"},
                {CLIENT_UA, CLIENT_RU, "187", "187"},
                {CLIENT_UA, CLIENT_RU, "187,977", "187"}
        });
    }

    @Before
    public void before() {

        campaignID = bannersRule.getCampaignId();

        cmdRule.apiSteps().bidsSteps().bidsSet(
                new SetRequestMap()
                        .withBids(new BidSetItemMap()
                                .withCampaignId(campaignID)
                                .withBid(3200000L)),
                firstExcelClient);
    }

    @After
    public void delete() {
        if (exportedCamp != null) {
            exportedCamp.delete();
        }
        if (newCampaignID != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(secondExcelClient, newCampaignID);
        }
    }

    @Test
    @Description("Сохранение корректного региона при экспорте/импорте между российским и украинским клиентами")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9682")
    public void excelDownloadAndUploadWithGeoTest() {
        ExportCampXlsRequest request = new ExportCampXlsRequest()
                .withCid(campaignID.toString())
                .withSkipArch(true)
                .withXlsFormat(ExportCampXlsRequest.ExcelFormat.XLSX)
                .withUlogin(firstExcelClient);
        exportedCamp = cmdRule.cmdSteps().excelSteps().exportCampaignIgnoringLock(request);

        newCampaignID = cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                exportedCamp,
                secondExcelClient,
                campaignID.toString(),
                ImportCampXlsRequest.DestinationCamp.NEW).getLocationParamAsInteger(LocationParam.CID).longValue();

        check(newCampaignID, secondClientGeo);
    }

    private void check(Long newCampaignId, String newGeo) {
        List<Group> groups = cmdRule.cmdSteps().groupsSteps().getGroups(secondExcelClient, newCampaignId);

        assumeThat("в кампании 1на группа", groups, hasSize(1));
        assumeThat("загрузился только один баннер", groups.get(0).getBanners(), hasSize(1));
        assertThat("регион совпадает с ожидаемым", groups.get(0).getGeo(), equalTo(newGeo));
    }
}
