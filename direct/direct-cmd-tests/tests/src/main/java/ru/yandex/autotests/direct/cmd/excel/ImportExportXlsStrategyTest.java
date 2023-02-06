package ru.yandex.autotests.direct.cmd.excel;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.data.excel.ExportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Сохранение стратегий при выгрузке и загрузке эксель-файла")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class ImportExportXlsStrategyTest {
    private static final String CLIENT = "at-direct-excel-rus";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public Strategies strategy;
    public TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(Logins.SUPER).withRules(bannersRule);
    private Long campaignId;
    private File exportedCamp;
    private CampaignStrategy campaignStrategy;

    @Parameterized.Parameters(name = "Стратегия: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Strategies.AVERAGE_CPA_OPTIMIZATION_DEFAULT},
                {Strategies.ROI_OPTIMIZATION_DEFAULT},
                {Strategies.WEEKLY_BUDGET_MAX_CLICKS_DEFAULT},
                {Strategies.SHOWS_DISABLED_AVERAGE_PRICE},
                {Strategies.HIGHEST_POSITION_MAX_COVERAGE}
        });
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        Long goalId = MetrikaCountersData.DEFAULT_COUNTER.getFirstGoalId();
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campMetrikaGoalsSteps().
                addOrUpdateMetrikaGoals(campaignId, goalId, 50L, 50L);
        campaignStrategy = CmdStrategyBeans.getStrategyBean(strategy);
        if (strategy == Strategies.AVERAGE_CPA_OPTIMIZATION_DEFAULT
                || strategy == Strategies.ROI_OPTIMIZATION_DEFAULT
        ) {
            campaignStrategy.getSearch().setGoalId(goalId.toString());
//            campaignStrategy.getNet().goa
        }
        SaveCampRequest saveCampRequest = bannersRule.getSaveCampRequest();
        saveCampRequest.setCid(campaignId.toString());
        saveCampRequest.setJsonStrategy(campaignStrategy);
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
    }


    @Test
    @Description("Сохранение стратегии после выгрузки и загрузки кампании через эксель")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9683")
    public void excelDownloadAndUploadWithStrategyTest() {
        ExportCampXlsRequest request = new ExportCampXlsRequest()
                .withCid(campaignId.toString())
                .withSkipArch(true)
                .withXlsFormat(ExportCampXlsRequest.ExcelFormat.XLSX)
                .withUlogin(CLIENT);
        exportedCamp = cmdRule.cmdSteps().excelSteps().exportCampaignIgnoringLock(request);

        Long importedCampId = cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                exportedCamp, CLIENT, campaignId.toString(),
                ImportCampXlsRequest.DestinationCamp.OLD).getLocationParamAsLong(LocationParam.CID);
        assumeThat("загрузка произошла в ту же кампанию", importedCampId, equalTo(campaignId));

        Campaign campaign = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, bannersRule.getCampaignId());

        if (campaignStrategy.getSearch().getGoalId() != null && campaignStrategy.getSearch().getGoalId().isEmpty()) {
            campaignStrategy.getSearch().setGoalId(null);
        }
        assertThat("Параметры стратегии совпадают с ожидаемыми", campaign.getStrategy(),
                beanDiffer(campaignStrategy));

    }
}

