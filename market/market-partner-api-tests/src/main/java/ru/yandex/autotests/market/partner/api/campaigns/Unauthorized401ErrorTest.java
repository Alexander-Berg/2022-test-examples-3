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
import ru.yandex.autotests.market.partner.api.steps.PartnerApiCompareSteps;
import ru.yandex.autotests.market.partner.beans.ApiVersion;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;

import java.util.Collection;

import static ru.yandex.autotests.market.partner.api.data.b2b.Unauthorized401ErrorRequestData.*;

/**
 * User: jkt
 * Date: 04.06.13
 * Time: 12:53
 */
@Feature("401 Unauthorized errors")
@Aqua.Test(title = "BackToBack тесты сравнения с сохраненным результатом")
@RunWith(Parameterized.class)
public class Unauthorized401ErrorTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private PartnerApiCompareSteps tester = new PartnerApiCompareSteps();

    private PartnerApiRequestData request;

    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                requestWithoutCredentials()
           // , emptyLoginRequest()  // согласно MBI-12599 логин не требуется, ошибки не будет. Перенесено в CampaignOffersResourceTest
                , emptyClientRequest()
                , emptyTokenRequest()
        );
    }

    public Unauthorized401ErrorTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
    }

    @Before
    public void saveExpected() {
        if (new ProjectConfig().isOverwriteResponse()) {
            // save result to elliptics
            tester.saveExpectedToStorage(request);
            tester.saveExpectedToStorage(request.withFormat(Format.JSON));
            tester.saveExpectedToStorage(request.withVersion(ApiVersion.V1));
            tester.saveExpectedToStorage(request.withFormat(Format.JSON).withVersion(ApiVersion.V1));
        }
    }

    @Test
    public void testAuthorizationErrors() {
        tester.compareWithStoredResult(request);
        tester.saveExpectedToStorage(request.withFormat(Format.JSON));
    }

    @Test
    public void compareDifferentVersionsAuthorizationErrors() {
        tester.compareWithV1Output(request);
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
    }
}
