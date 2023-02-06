package ru.yandex.autotests.market.partner.api.campaigns;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.data.b2b.BaseRequestData;
import ru.yandex.autotests.market.partner.api.steps.OfferStatisticsResourcesSteps;
import ru.yandex.autotests.market.partner.api.steps.PartnerApiCompareSteps;
import ru.yandex.autotests.market.partner.beans.ApiVersion;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;

import java.util.Collection;

import static ru.yandex.autotests.market.partner.api.data.b2b.StatisticsRequestData.overdueOfferStatisticsRequest;

/**
 * User: strangelet
 * Date: 28.01.14
 * Time: 11:50
 */
@Feature("Statistics resources")
@Aqua.Test(title = "Проверка выдчи stats/offers ")
@RunWith(Parameterized.class)
@Issues({@Issue("MBI-22969"),
        @Issue("MSTAT-4685")})
public class StatisticsOffersResourcesTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private PartnerApiCompareSteps tester = new PartnerApiCompareSteps();
    private OfferStatisticsResourcesSteps statisticSteps = new OfferStatisticsResourcesSteps();

    private PartnerApiRequestData request;
    private String caseName;

    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                //Выключено из-за преределок статистик до https://st.yandex-team.ru/MSTAT-4685
//                 actualOfferStatisticsRequest(),
                overdueOfferStatisticsRequest()

        );
    }


    public StatisticsOffersResourcesTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
        this.caseName = caseName;
    }

    @Before
    public void getResponse() {
        statisticSteps.takeResponseWithOkStatus(request);
    }

    @Test
    public void checkMainV1Statistic() {
        statisticSteps.checkStatisticV1Response(request
                .withVersion(ApiVersion.V1));
    }

    @Test
    public void checkMainV2Statistic() {
        statisticSteps.checkStatisticV2Response(request
                .withVersion(ApiVersion.V2));
    }

    @Test
    public void checkMainStatisticV1JSON() {
        statisticSteps.checkStatisticJSONResponseV1(request
                .withFormat(Format.JSON)
                .withVersion(ApiVersion.V1));
    }

    @Test
    public void checkMainStatisticV2JSON() {
        statisticSteps.checkStatisticJSONResponseV2(request
                .withFormat(Format.JSON)
                .withVersion(ApiVersion.V2));
    }

}
