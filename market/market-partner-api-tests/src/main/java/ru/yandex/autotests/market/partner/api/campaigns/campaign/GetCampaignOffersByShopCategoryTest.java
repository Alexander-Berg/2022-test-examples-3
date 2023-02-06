package ru.yandex.autotests.market.partner.api.campaigns.campaign;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.ProjectConfig;
import ru.yandex.autotests.market.partner.api.steps.CompareWithReportSteps;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.beans.offers.CampaignOffer;
import ru.yandex.autotests.market.report.beans.json.places.prime.OfferResult;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.autotests.market.report.util.offers.OffersParser;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.Objects;

import static ru.yandex.autotests.market.common.data.ReportRequests.reportRequestWithTestFeed;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignOffersRequestData.campaignOffersForCategoryRequest;
import static ru.yandex.autotests.market.partner.api.steps.compare.CampaignOffersCompareSteps.compareCampaignOffersWithReportV1;
import static ru.yandex.autotests.market.partner.beans.ApiVersion.V2;
import static ru.yandex.autotests.market.partner.beans.Format.JSON;
import static ru.yandex.autotests.market.partner.beans.Format.XML;

/**
 * Created by poluektov on 09.06.16.
 */

//Тест аналогичен GetCampaignOffersTest, здесь кейсы требующие специфических фильтров, которых нет в репорте.
//Вынесен отдельно - фильтр по shopCategoryId досутпен только в v2
@Feature("Campaign resources")
@Stories("Campaign Offers")
@Aqua.Test(title = "проверка фильтров выдачи GET campaign/.../offers/ShopCategoryId=")
public class GetCampaignOffersByShopCategoryTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private PartnerApiRequestData partnerRequest = campaignOffersForCategoryRequest();
    private CompareWithReportSteps responseSteps = new CompareWithReportSteps();
    private List<CampaignOffer> offersFromApi;
    private List<OfferResult> offersFromReport;
    private ProjectConfig config = new ProjectConfig();

    @Before
    public void getOffersFromReport() {
        offersFromReport = OffersParser.parseOfferResultsFrom(reportRequestWithTestFeed());
    }

    @Test
    @Title("V2 XML: GET CampaignOffers by ShopCategoryId")
    public void testResponseV2() {
        offersFromApi = responseSteps.parseOffersFromApi(partnerRequest.withFormat(XML).withVersion(V2))
                .getOffers().getOffer();
        offersFromReport.removeIf(reportOffer -> !Objects.equals(reportOffer.getShop().getFeed().getCategoryId(),
                String.valueOf(config.getOffersShopCategoryId())));
        compareCampaignOffersWithReportV1(offersFromApi, offersFromReport);
    }

    @Test
    @Title("V2 JSON: GET CampaignOffers by ShopCategoryId")
    public void testResponseV2JSON() {
        offersFromApi = responseSteps.getJsonResponse(partnerRequest.withFormat(JSON).withVersion(V2))
                .getOffers().getOffer();
        offersFromReport.removeIf(reportOffer -> !Objects.equals(reportOffer.getShop().getFeed().getCategoryId(),
                String.valueOf(config.getOffersShopCategoryId())));
        compareCampaignOffersWithReportV1(offersFromApi, offersFromReport);
    }
}
