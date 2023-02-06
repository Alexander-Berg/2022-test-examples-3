package ru.yandex.autotests.market.partner.api.campaigns;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.data.b2b.BaseRequestData;
import ru.yandex.autotests.market.partner.api.steps.response.ModelsResponseSteps;
import ru.yandex.autotests.market.partner.beans.ApiVersion;
import ru.yandex.autotests.market.partner.beans.Currency;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.beans.api.models.search.byid.Models;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;

import static ru.yandex.autotests.market.partner.api.data.b2b.ModelsRequestData.modelPricesRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.ModelsRequestData.pricesForMultipleModelsJsonRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.ModelsRequestData.pricesForMultipleModelsXmlRequest;
import static ru.yandex.autotests.market.partner.api.steps.compare.ModelsCompareSteps.compareModelsWithReportResponse;

/**
 * Created by poluektov on 25.05.16.
 */
@Feature("Models resources")
@Stories("Model prices")
@Aqua.Test(title = " GetModelsPricesTest проверка выдачи /models")
@RunWith(Parameterized.class)
@Issues({
        @Issue("https://st.yandex-team.ru/AUTOTESTMARKET-3976")
})
public class GetModelsPricesTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private final String REGION = "2";
    private final Currency currency = Currency.RUR;

    private PartnerApiRequestData request;
    private Models modelList;

    private ModelsResponseSteps modelsSteps = new ModelsResponseSteps();

    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                modelPricesRequest(),
                pricesForMultipleModelsXmlRequest(),
                pricesForMultipleModelsJsonRequest()
        );
    }

    public GetModelsPricesTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
    }

    @Test
    public void testResponseV1() {
        modelList = modelsSteps.getResponse(request.withVersion(ApiVersion.V1).withCurrency(currency))
                               .getModels();
        compareModelsWithReportResponse(modelList, REGION, currency);
    }

    @Test
    public void testResponseV2() {
        modelList = modelsSteps.getResponse(request.withVersion(ApiVersion.V2).withCurrency(currency))
                               .getModels();
        compareModelsWithReportResponse(modelList, REGION, currency);
    }

    @Test
    public void testJSONResponseV1() {
        modelList = modelsSteps.getJsonResponse(request.withVersion(ApiVersion.V1)
                                                       .withFormat(Format.JSON)
                                                       .withCurrency(currency))
                               .getModels();
        compareModelsWithReportResponse(modelList, REGION, currency);
    }

    @Test
    public void testJSONResponseV2() {
        modelList = modelsSteps.getJsonResponse(request.withVersion(ApiVersion.V2)
                                                       .withFormat(Format.JSON)
                                                       .withCurrency(currency))
                               .getModels();
        compareModelsWithReportResponse(modelList, REGION, currency);
    }
}