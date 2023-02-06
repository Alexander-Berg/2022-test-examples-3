package ru.yandex.autotests.market.partner.api.campaigns;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.data.b2b.BaseRequestData;
import ru.yandex.autotests.market.partner.api.steps.PartnerApiCompareSteps;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;

import java.util.Collection;

import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignRegionRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.ModelsRequestData.*;
import static ru.yandex.autotests.market.partner.api.data.b2b.RegionsRequestData.regionInfoRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.RegionsRequestData.regionsRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.StatisticsRequestData.*;

/**
 * User: jkt
 * Date: 30.05.13
 * Time: 13:54
 */
@Feature("Second version resources same as in first version")
@Aqua.Test(title = "BackToBack тесты сравнения ответов в первой и второй версии")
@RunWith(Parameterized.class)
public class CompareFirstAndSecondVersionOutputsTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private PartnerApiCompareSteps tester = new PartnerApiCompareSteps();

    private PartnerApiRequestData requestData;

    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
              /* campaignsListRequest()    //   ответы v1 и v2 различаются согласно документации*/
           /* ,*/ campaignRegionRequest()
              /*, campaignsByLoginRequest()//   ответы v1 и v2 различаются согласно документации*/
              //  , modelSearchRequest()   //   ModelsSearchResourcesTest */
                , modelPricesRequest()
                , pricesForMultipleModelsXmlRequest()
                , pricesForMultipleModelsJsonRequest()
                , modelOffersRequest()
                , offersForMultipleModelsXmlRequest()
                , offersForMultipleModelsJsonRequest()
                , mainStatisticsRequest()
                , dailyStatisticsRequest()
                , weeklyStatisticsRequest()
                , monthlyStatisticsRequest()
                , regionInfoRequest()
                , regionsRequest()

        );
    }

    public CompareFirstAndSecondVersionOutputsTest(PartnerApiRequestData requestData, String caseName) {
        this.requestData = requestData;
    }

    @Test
    public void testCheckBasicResponse() {
        tester.compareWithV1Output(requestData);
    }
}
