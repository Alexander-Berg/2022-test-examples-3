package ru.yandex.autotests.market.partner.api.campaigns;

import org.apache.log4j.Logger;
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

import static ru.yandex.autotests.market.partner.api.data.b2b.Forbidden403ErrorRequestData.*;

/**
 * User: jkt
 * Date: 04.06.13
 * Time: 14:44
 */
@Feature("403 Forbidden errors")
@Aqua.Test(title = "BackToBack тесты сравнения с сохраненным результатом")
@RunWith(Parameterized.class)
public class Forbidden403ErrorTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private static final Logger LOG = Logger.getLogger(Forbidden403ErrorTest.class);

    private PartnerApiCompareSteps tester = new PartnerApiCompareSteps();

    private PartnerApiRequestData request;
    private String caseName;

    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                invalidClientIdRequest()
             //   , invalidLoginRequest() проверка в рамках CampaignsInformationResourcesTest
                , invalidTokenRequest()
                , otherUserCampaignInfoRequest()
                , otherUserCampaignBalanceRequest()
                , otherUserCampaignLoginsRequest()
                , otherUserCampaignRegionRequest()
                , otherUserCampaignSettingsRequest()

        );
    }

    public Forbidden403ErrorTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
        this.caseName = caseName;
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
    public void testForbiddenErrors() {
        tester.compareWithStoredResult(request);
    }

    @Test
    public void compareDifferentVersionsForbiddenErrors() {
        // костыль для  некоторых кейсов в разных версиях разный результат
        // надо вынести в отдельный тест
        if (!(
                caseName.equals("otherUserCampaignSettingsRequest")
                        || caseName.equals("invalidLoginRequest")
        )) {
            tester.compareWithV1Output(request);
        } else {
            LOG.info("Skip test for " + caseName);
        }

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
