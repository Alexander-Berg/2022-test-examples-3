package ru.yandex.autotests.market.partner.api.campaigns;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.ProjectConfig;
import ru.yandex.autotests.market.partner.api.data.b2b.BaseRequestData;
import ru.yandex.autotests.market.partner.api.steps.MainStatisticsResourcesSteps;
import ru.yandex.autotests.market.partner.api.steps.PartnerApiCompareSteps;
import ru.yandex.autotests.market.partner.beans.ApiVersion;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;

import java.util.Collection;

import static ru.yandex.autotests.market.partner.api.data.b2b.StatisticsRequestData.*;

/**
 * User: jkt
 * Date: 01.11.12
 * Time: 11:50
 */
@Feature("Statistics resources")
@Aqua.Test(title = "Проверка выдчи stats/main")
@RunWith(Parameterized.class)
public class StatisticsResourcesTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private PartnerApiCompareSteps tester = new PartnerApiCompareSteps();
    private MainStatisticsResourcesSteps statisticSteps = new MainStatisticsResourcesSteps();

    private PartnerApiRequestData request;
    private String caseName;

    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                mainStatisticsRequest()
                , dailyStatisticsRequest()
                , weeklyStatisticsRequest()
                , monthlyStatisticsRequest()
        );
    }


    public StatisticsResourcesTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
        this.caseName = caseName;
    }

    @Before
    public void getResponse() {
        if (new ProjectConfig().isOverwriteResponse()) {
            // save result to elliptics
            tester.saveExpectedToStorage(request);
            tester.saveExpectedToStorage(request.withFormat(Format.JSON));
            tester.saveExpectedToStorage(request.withVersion(ApiVersion.V1));
            tester.saveExpectedToStorage(request.withFormat(Format.JSON).withVersion(ApiVersion.V1));
        }
//*/
    }

    @Test
    public void checkMainV1Statistic() {
        statisticSteps.checkStatisticResponse(request
                .withVersion(ApiVersion.V1));
    }

    @Test
    public void checkMainV2Statistic() {
        statisticSteps.checkStatisticResponse(request
                .withVersion(ApiVersion.V2));
    }

    @Test
    public void checkMainStatisticV1JSON() {
        statisticSteps.checkStatisticJSONResponse(request
                .withFormat(Format.JSON)
                .withVersion(ApiVersion.V1));
    }

    @Test
    public void checkMainStatisticV2JSON() {
        statisticSteps.checkStatisticJSONResponse(request
                .withFormat(Format.JSON)
                .withVersion(ApiVersion.V2));
    }
  /*

     b2b не подходит, тк ответы разные.

   @Test
    public void testCheckBasicResponse() {
        tester.compareWithStoredResult(request);
    }

    @Test
    public void compareWithJsonStoredResult() {
        tester.compareWithJsonStoredResult(request.withFormat(Format.JSON));
    }

    @Test
    public void compareWithV1StoredResult() {
        tester.compareWithStoredResult(request.withVersion(ApiVersion.V1));
    }

    @Test
    public void compareWithV1JsonStoredResult() {
        tester.compareWithJsonStoredResult(request.withFormat(Format.JSON).withVersion(ApiVersion.V1));
    }//*/
}
