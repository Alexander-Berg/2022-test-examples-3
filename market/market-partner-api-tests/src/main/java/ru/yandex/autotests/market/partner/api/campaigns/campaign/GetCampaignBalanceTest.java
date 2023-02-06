package ru.yandex.autotests.market.partner.api.campaigns.campaign;

import com.github.alkedr.matchers.reporting.ReportingMatcher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.campaign.CampaignBalance;
import ru.yandex.autotests.market.billing.backend.steps.BillingDaoSteps;
import ru.yandex.autotests.market.common.deserialization.json.CampaignBalanceResponseJsonDeserializer;
import ru.yandex.autotests.market.partner.api.ProjectConfig;
import ru.yandex.autotests.market.partner.api.data.b2b.BaseRequestData;
import ru.yandex.autotests.market.partner.api.steps.BaseObjectResponseSteps;
import ru.yandex.autotests.market.partner.beans.ApiVersion;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.beans.api.pullapi.balance.Balance;
import ru.yandex.autotests.market.partner.beans.api.pullapi.balance.CampaignBalanceV1Response;
import ru.yandex.autotests.market.partner.beans.api.pullapi.balance.CampaignBalanceV2Response;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;

import static com.github.alkedr.matchers.reporting.ReportingMatchers.field;
import static com.github.alkedr.matchers.reporting.ReportingMatchers.merge;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.common.request.rest.RequestStepsFactory.getHttpRequestStepsWithAllureLogger;
import static ru.yandex.autotests.market.common.steps.AssertSteps.reportingAssertThat;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignBalanceRequest;

/**
 * Created by poluektov on 07.06.16.
 */
@Feature("Campaign resources")
@Stories("Campaign balance")
@Aqua.Test(title = "Сравнение баланса кампании в партнерском апи с данными из биллинга")
@RunWith(Parameterized.class)
public class GetCampaignBalanceTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private PartnerApiRequestData request;
    private CampaignBalance actualBalance;

    private final Double BALANCE_MULTIPLIER = 100.0;

    private BaseObjectResponseSteps<CampaignBalanceV2Response> responseSteps = new BaseObjectResponseSteps<CampaignBalanceV2Response>() {
        @Override
        public Class<CampaignBalanceV2Response> getXType() {
            return CampaignBalanceV2Response.class;
        }

        @Override
        public Gson createGson() {
            return new GsonBuilder()
                    .registerTypeAdapter(getXType(), new CampaignBalanceResponseJsonDeserializer())
                    .create();
        }
    };


    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                campaignBalanceRequest()
        );
    }

    public GetCampaignBalanceTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
    }

    @Before
    public void getActualBalance() {
        actualBalance = BillingDaoSteps.getInstance().getCampaignBalance((long) new ProjectConfig().getCampaignId());
    }

    @Test
    public void testV1() {
        CampaignBalanceV1Response response = getHttpRequestStepsWithAllureLogger()
                .getResponseAs(CampaignBalanceV1Response.class, request.withVersion(ApiVersion.V1).withFormat(Format.XML));
        reportingAssertThat(response.getBalance().getBalance(), isBalanceCorrect());
    }

    @Test
    public void testV2() {
        CampaignBalanceV2Response response = responseSteps.getResponse(request.withVersion(ApiVersion.V2).withFormat(Format.XML));
        reportingAssertThat(response.getBalance(), isBalanceCorrect());
    }

    @Test
    public void testV1Json() {
        CampaignBalanceV2Response response = responseSteps.getJsonResponse(request.withVersion(ApiVersion.V1).withFormat(Format.JSON));
        reportingAssertThat(response.getBalance(), isBalanceCorrect());
    }

    @Test
    public void testV2Json() {
        CampaignBalanceV2Response response = responseSteps.getJsonResponse(request.withVersion(ApiVersion.V1).withFormat(Format.JSON));
        reportingAssertThat(response.getBalance(), isBalanceCorrect());
    }

    private ReportingMatcher<? super Balance> isBalanceCorrect() {
        return merge(
                field("balance", equalTo(actualBalance.getTotalBalance() / BALANCE_MULTIPLIER)),
                field("daysLeft", equalTo(365)),
                field("recommendedPayment", equalTo(actualBalance.getSumPaid() / BALANCE_MULTIPLIER))
        );
    }
}
