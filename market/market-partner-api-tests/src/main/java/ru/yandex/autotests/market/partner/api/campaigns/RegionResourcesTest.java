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

import static ru.yandex.autotests.market.partner.api.data.b2b.RegionsRequestData.*;

/**
 * User: jkt
 * Date: 01.11.12
 * Time: 11:50
 */
@Feature("Region resources")
@Aqua.Test(title = "BackToBack тесты сравнения с сохраненным результатом")
@RunWith(Parameterized.class)
public class RegionResourcesTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private PartnerApiCompareSteps tester = new PartnerApiCompareSteps();

    private PartnerApiRequestData request;

    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                regionInfoRequest()
                , regionsRequest()
                , regionInfoRequestForUkraine()
        );
    }

    public RegionResourcesTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
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
    }

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
    }
}
