package ru.yandex.autotests.market.partner.api.campaigns.pull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.ProjectConfig;
import ru.yandex.autotests.market.partner.api.data.b2b.BaseRequestData;
import ru.yandex.autotests.market.partner.api.steps.PartnerApiCompareSteps;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.beans.api.pullapi.Status;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;

import static ru.yandex.autotests.market.partner.api.data.PullApiRequestsData.*;

/**
 * Created
 * by strangelet
 * on 24.06.15.
 */
@Feature("pull-api")
@Aqua.Test(title = "Тесты  валидации ручек GET pull-api")
@RunWith(Parameterized.class)
@Stories("GET /campaigns/(campaignId)/orders/ (orderId)")
public class BadRequestCampaignOrderTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private PartnerApiRequestData request;
    private PartnerApiCompareSteps tester = new PartnerApiCompareSteps();


    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                getCampaignOrdersWithFilterPagerRequest(0, 0),
                getCampaignOrdersWithFilterPagerRequest(1, 0),
                getCampaignOrdersWithFilterPagerRequest(1, 51),
                getCampaignOrdersWithFilterDatesRequest("-", ""),
                getCampaignOrdersWithFilterDatesRequest("31-12-3020", null),
                getCampaignOrdersWithFilterStatusRequest(Status.CANCELLED + "1"),
                getCampaignOrdersWithOrderRequest(1),
                getCampaignOrdersWithOrderRequest(10155),
                getCampaignOrdersWithFakeRequest("-"),
                getCampaignOrderWithInvalidOrderRequest(),
                getCampaignOrderWithForeignOrderRequest(),
                getCampaignOrderWithForeignCampaignRequest()
        );
    }


    @Before
    public void saveExpected() {
        if (new ProjectConfig().isOverwriteResponse()) {
            tester.saveExpectedToStorage(request);
            tester.saveExpectedToStorage(request.withFormat(Format.JSON));
        }
    }

    public BadRequestCampaignOrderTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
    }


    @Test
    public void compareWithStoredResult() {
        tester.compareWithStoredResult(request);
    }

    @Test
    public void compareWithJsonStoredResult() {
        tester.compareWithJsonStoredResult(request.withFormat(Format.JSON));
    }

}
