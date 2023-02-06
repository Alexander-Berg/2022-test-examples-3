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
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.beans.api.campaign.response.Campaign;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;
import java.util.List;

import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignsByLoginRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignsListRequest;
import static ru.yandex.autotests.market.partner.api.steps.compare.CampaignsCompareSteps.compareCampaignsListWithDB;
import static ru.yandex.autotests.market.partner.beans.ApiVersion.V1;
import static ru.yandex.autotests.market.partner.beans.ApiVersion.V2;
import static ru.yandex.autotests.market.partner.beans.Format.JSON;
import static ru.yandex.autotests.market.partner.beans.Format.XML;

/**
 * Created by poluektov on 31.05.16.
 */
@Feature("Campaign resources")
@Stories("Campaigns info")
@Aqua.Test(title = "Сравнение информации о компании в партнерском апи с данными из биллинга")
@RunWith(Parameterized.class)
public class GetCampaignsTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private final long CLIENT_ID = 5835538;
    private PartnerApiRequestData request;

    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                campaignsListRequest(),
                campaignsByLoginRequest()
        );
    }

    public GetCampaignsTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
    }

    @Test
    public void testV1() {
        final List<Campaign> campaignsFromApi = new CampaignInfoResponseStep()
                .getResponse(request.withTvmAuth(false).withFormat(XML).withVersion(V1))
                .getCampaigns().getCampaign();

        compareCampaignsListWithDB(campaignsFromApi, CLIENT_ID, ApiVersion.V1);
    }

    @Test
    public void testV2() {
        final List<Campaign> campaignsFromApi = new CampaignInfoResponseStep()
                .getResponse(request.withTvmAuth(false).withFormat(XML).withVersion(V2))
                .getCampaigns().getCampaign();

        compareCampaignsListWithDB(campaignsFromApi, CLIENT_ID, ApiVersion.V2);
    }

    @Test
    public void testV1Json() {
        final List<Campaign> campaignsFromApi = new CampaignInfoResponseStep()
                .getJsonResponse(request.withTvmAuth(false).withFormat(JSON).withVersion(V1))
                .getCampaigns().getCampaign();

        compareCampaignsListWithDB(campaignsFromApi, CLIENT_ID, ApiVersion.V1);
    }

    @Test
    public void testV2Json() {
        final List<Campaign> campaignsFromApi = new CampaignInfoResponseStep()
                .getJsonResponse(request.withTvmAuth(false).withFormat(JSON).withVersion(V2))
                .getCampaigns().getCampaign();

        compareCampaignsListWithDB(campaignsFromApi, CLIENT_ID, ApiVersion.V2);
    }

    @Test
    public void testV1Tvm() {
        final List<Campaign> campaignsFromApi = new CampaignInfoResponseStep()
                .getResponse(request.withTvmAuth(true).withFormat(XML).withVersion(V1))
                .getCampaigns().getCampaign();

        compareCampaignsListWithDB(campaignsFromApi, CLIENT_ID, ApiVersion.V1);
    }

    @Test
    public void testV2JsonTvm() {
        final List<Campaign> campaignsFromApi = new CampaignInfoResponseStep()
                .getJsonResponse(request.withTvmAuth(true).withFormat(JSON).withVersion(V2))
                .getCampaigns().getCampaign();

        compareCampaignsListWithDB(campaignsFromApi, CLIENT_ID, ApiVersion.V2);
    }

}
