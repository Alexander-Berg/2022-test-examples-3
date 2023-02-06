package ru.yandex.autotests.direct.cmd.excel.callouts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.banners.additions.callouts.CalloutsTestHelper;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ExportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Загрузка уточнений к баннерам через эксель: правильное сохранение порядка дополнений")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.CALLOUTS)
@Tag(BusinessProcessTag.EXCEL)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@Feature(TestFeatures.EXCEL)
public class ExcelUploadCalloutsOrderTest {

    private static final String CLIENT = "direct-test-xls-callouts2";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    String[] expectedCallouts = {"callout1", "callout2", "callout3"};

    protected BannersRule bannersRule = new TextBannersRule()
            .overrideBannerTemplate(new Banner()
                    .withCallouts(
                            Arrays.stream(expectedCallouts)
                                    .map(c -> new Callout().withCalloutText(c))
                                    .collect(Collectors.toList())))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private Long newCampId;

    private File tempExcel;

    private File excelToUpload;

    @Before
    public void before() {
        DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps().useShardForLogin(CLIENT);
        dbSteps.bannerAdditionsSteps().clearCalloutsForClient(
                Long.valueOf(User.get(CLIENT).getClientID()));
    }

    @After
    public void delete() {
        FileUtils.deleteQuietly(tempExcel);
        FileUtils.deleteQuietly(excelToUpload);
        if (newCampId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCampId);
        }
    }

    @Test
    @Description("Сохранение порядка уточнений")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9732")
    public void importAdditionsToNewCamp() {
        newCampId = uploadCampaign(ImportCampXlsRequest.DestinationCamp.NEW, "");
        check();
    }

    @Test
    @Description("Изменение порядка уточнений")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9733")
    public void importAdditionsToExistingCamp() {
        expectedCallouts = new String[]{"callout3", "callout1", "callout2"};
        newCampId = uploadCampaign(ImportCampXlsRequest.DestinationCamp.OLD, bannersRule.getCampaignId().toString());
        check();
    }

    private Long uploadCampaign(ImportCampXlsRequest.DestinationCamp dest, String newCid) {
        tempExcel = cmdRule.cmdSteps().excelSteps().exportCampaign(new ExportCampXlsRequest().
                withCid(bannersRule.getCampaignId().toString()).
                withSkipArch(true).
                withXlsFormat(ExportCampXlsRequest.ExcelFormat.XLSX).
                withUlogin(CLIENT));

        try {
            excelToUpload = File.createTempFile(RandomUtils.getString(10), ".xlsx");
        } catch (Exception e) {
            throw new IllegalStateException("ошибка создания временного excel-файла", e);
        }

        ExcelUtils.setCellValue(tempExcel, excelToUpload, ExcelColumnsEnum.CALLOUTS, 0,
                StringUtils.join(expectedCallouts, "||"));

        return cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(excelToUpload, CLIENT, newCid,
                dest).getLocationParamAsLong(LocationParam.CID);
    }

    private void check() {
        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, newCampId.toString());

        CalloutsTestHelper helper = new CalloutsTestHelper(CLIENT, cmdRule.cmdSteps(), newCampId.toString());
        List<String> callouts = helper.getCalloutsList(response);

        assertThat("в баннере сохранились дополнения", callouts, beanDiffer(Arrays.asList(expectedCallouts)));
    }
}
