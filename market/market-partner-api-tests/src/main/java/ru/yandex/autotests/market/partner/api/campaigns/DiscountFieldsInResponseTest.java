package ru.yandex.autotests.market.partner.api.campaigns;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.common.request.rest.HttpRequestSteps;
import ru.yandex.autotests.common.request.rest.RequestStepsFactory;
import ru.yandex.autotests.market.partner.api.ProjectConfig;
import ru.yandex.autotests.market.partner.api.data.b2b.CampaignOffersRequestData;
import ru.yandex.autotests.market.partner.api.data.b2b.ModelsRequestData;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.beans.modeloffers.ModelOffer;
import ru.yandex.autotests.market.partner.beans.modeloffers.ModelsOffersResponse;
import ru.yandex.autotests.market.partner.beans.offers.OffersReport;
import ru.yandex.autotests.market.report.beans.json.places.prime.OfferResult;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.autotests.market.report.util.RequestFactory;
import ru.yandex.autotests.market.report.util.offers.OffersParser;
import ru.yandex.autotests.market.report.util.query.Places;
import ru.yandex.autotests.market.report.util.query.ReportRequest;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.market.common.steps.AssertSteps.assertStep;

/**
 * Created by poluektov on 06.05.16.
 */
@Aqua.Test(title = "Наличие полей скидок у офферов модели")
@Feature("Discounts")
@Stories("Discount fields in offers resources")
public class DiscountFieldsInResponseTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private ProjectConfig config = new ProjectConfig();
    private HttpRequestSteps rest = RequestStepsFactory.getHttpRequestStepsWithAllureLogger();

    private final String REGION = "2";
    private final long MODEL_ID = 8350595;
    private final String OFFER_WITH_DISCOUNT = "Бинокли, Телескопы Nikon Aculon T11 8-24x25 (черный)";
    private final long FEED_ID = 375216;
    private final String SHOP_OFFER_ID = "1636288";
    private final long CAMPAIGN_ID = config.getBidsByTitleCampaignId();

    private OfferResult offerWithDiscount;

    @Before
    public void takeDiscountValueFromReport() {
        ReportRequest request = RequestFactory
                .primeFesh(config.getBidsByTitleShopId())
                .withPlace(Places.MIPRIME)
                .withNumOfResults(0);
        int offersCount = OffersParser.getOffersCount(request);
        offerWithDiscount = OffersParser
                .parseOfferResultsFrom(request.withNumOfResults(offersCount))
                .stream()
                .filter(x -> Objects.equals(x.getShop().getFeed().getId(), FEED_ID)
                        && Objects.equals(x.getShop().getFeed().getOfferId(), SHOP_OFFER_ID))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Couldn't find any offers in report by query: "
                        + request.toString())
                );
        if (offerWithDiscount.getOfferPrices().getDiscount() == null) {
            throw new IllegalStateException("No discount for offer " + offerWithDiscount.toString());
        }
    }

    @Test
    @Title("GET /campaign/../offers")
    public void testGetCampaignOffers() {
        PartnerApiRequestData request = addOAuthToken(CampaignOffersRequestData.campaignOffersRequestWithQuery(OFFER_WITH_DISCOUNT)
                .withCampaignId(CAMPAIGN_ID));
        OffersReport response = rest.getResponseAs(OffersReport.class, request);

        assumeThat(response.getOffers().getOffer(), not(empty()));
        assertStep("pre-discount-price", response.getOffers().getOffer().get(0).getPreDiscountPrice(),
                equalTo(Long.valueOf(offerWithDiscount.getOfferPrices().getDiscount().getOldMin())));
        assertStep("discount", response.getOffers().getOffer().get(0).getDiscount(),
                equalTo(offerWithDiscount.getOfferPrices().getDiscount().getPercent()));
    }

    @Test
    @Title("POST /model/offers")
    public void testPostModelsOffers() {
        PartnerApiRequestData request = addOAuthToken(ModelsRequestData.offersForMultipleModelsXmlRequest(MODEL_ID));

        checkModelOffersResponse(request);
    }

    @Test
    @Title("GET /model/../offers")
    public void testGetModelOffers() {
        PartnerApiRequestData request = addOAuthToken(ModelsRequestData.modelOffersRequest(MODEL_ID));

        checkModelOffersResponse(request);
    }

    private void checkModelOffersResponse(PartnerApiRequestData request) {
        ModelsOffersResponse response = rest.getResponseAs(ModelsOffersResponse.class, request);

        assumeThat(response.getModels().getModel(), not(empty()));
        List<ModelOffer> offersList = response.getModels().getModel().get(0).getOffers().getOffer();
        ModelOffer testOffer = offersList.stream()
                .filter(modelOffer ->
                        Objects.equals(modelOffer.getName(), offerWithDiscount.getTitles().getRaw()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No offers with discount in PAPI response"));

        assertStep("old price is correct", testOffer.getPreDiscountPrice(),
                equalTo(Long.valueOf(offerWithDiscount.getOfferPrices().getDiscount().getOldMin())));
        assertStep("discount percent is correct", testOffer.getDiscount(),
                equalTo(offerWithDiscount.getOfferPrices().getDiscount().getPercent()));
    }

    private PartnerApiRequestData addOAuthToken(PartnerApiRequestData requestData) {
        return requestData.withOAuth(config.getBidsClientId(), config.getBidsToken());
    }
}
