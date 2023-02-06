package ru.yandex.autotests.market.partner.api.campaigns.campaign;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.data.b2b.BaseRequestData;
import ru.yandex.autotests.market.partner.api.steps.response.CampaignInfoResponseStep;
import ru.yandex.autotests.market.partner.beans.ApiVersion;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;

import java.util.Collection;

import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignsListRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.Unauthorized401ErrorRequestData.emptyLoginRequest;

/**
 * User: jkt
 * Date: 05.06.13
 * Time: 13:25
 */
@Feature("Campaign offers resource")
@Aqua.Test(title = "BackToBack тесты сравнения с сохраненным результатом")
@RunWith(Parameterized.class)
public class CampaignResourceTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private CampaignInfoResponseStep tester = new CampaignInfoResponseStep();

    private PartnerApiRequestData request;

    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                emptyLoginRequest()   // согласно MBI-12599 логин не требуется, ошибки не будет
                , campaignsListRequest()

        );
    }

    public CampaignResourceTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
    }


    @Test
    public void checkCampaigns() {
        tester.checkCampaignsResponse(request);
    }

    @Test
    public void checkCampaignsV1() {
        tester.checkCampaignsResponse(request.withVersion(ApiVersion.V1));
    }

    @Test
    public void checkCampaignsJson() {
        tester.checkCampaignsResponseJSON(request.withFormat(Format.JSON));
    }

    @Test
    public void checkCampaignsJsonV1() {
        tester.checkCampaignsResponseJSON(request.withVersion(ApiVersion.V1).withFormat(Format.JSON));
    }

}
