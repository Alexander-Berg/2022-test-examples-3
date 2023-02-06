package ru.yandex.market.api.partner.controllers.price;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncSearch;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.core.offer.PapiMarketSkuOfferService;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerServiceStub;
import ru.yandex.market.mbi.datacamp.saas.impl.SaasDatacampService;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.DataCampSearchAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = {"AbstractOfferPriceControllerTest.csv"})
abstract class AbstractOfferPriceControllerTest extends FunctionalTest {
    static final int CAMPAIGN_ID = 10774;
    static final long SUPPLIER_CAMPAIGN_ID = 10661;
    static final long ONE_P_SUPPLIER_CAMPAIGN_ID = 10662;
    static final long SMB_CAMPAIGN_ID = 10997;
    static final long PUSH_SHOP_CAMPAIGN_ID = 10998;
    static final long DIRECT_CAMPAIGN_ID = 10300;
    static final long SPBTESTER_UID_6 = 67282296;
    static final long SPBTESTER_UID_5 = 67282295;

    @Autowired
    OfferPriceControllerTestConfig.TestClock offerPriceControllerClock;

    @Autowired
    @Qualifier("papiMarketSkuOfferSeparatePoolService")
    PapiMarketSkuOfferService papiMarketSkuOfferService;

    @Autowired
    private UltraControllerServiceStub ultraControllerClient;

    @Autowired
    DataCampService dataCampService;

    @Autowired
    @Qualifier("dataCampShopClient")
    DataCampClient dataCampShopClient;

    @Autowired
    private SaasDatacampService saasService;

    @BeforeEach
    public void restartClock() {
        offerPriceControllerClock.restart();
    }

    HttpUriRequest patchOfferPricesRequest(
            long campaignId,
            String format,
            HttpEntity entity
    ) {
        return patchOfferPricesRequest(campaignId, format, entity, 67282295);
    }

    HttpPost patchOfferPricesRequest(
            long campaignId,
            String format,
            HttpEntity entity,
            long uid) {
        HttpPost request = new HttpPost(patchOfferPricesURI(campaignId, format));
        request.setHeader("X-AuthorizationService", "Mock");
        request.setHeader("Cookie", String.format("yandexuid = %d;", uid));
        request.setEntity(entity);
        return request;
    }

    HttpGet getOfferPricesRequest(
            long campaignId,
            String format,
            Map<String, String> queryParameters,
            long uid) {
        HttpGet request = new HttpGet(offerPricesURI(campaignId, format, queryParameters));
        request.setHeader("X-AuthorizationService", "Mock");
        request.setHeader("Cookie", String.format("yandexuid = %d;", uid));
        return request;
    }

    HttpGet getOfferPricesRequest(
            long campaignId,
            String format,
            Map<String, String> queryParameters) {
        return getOfferPricesRequest(campaignId, format, queryParameters, 67282295);
    }

    HttpPost removeAllOfferPricesRequest(
            long campaignId,
            String format,
            HttpEntity entity) {
        HttpPost request = new HttpPost(removeAllOfferPricesURI(campaignId, format));
        request.setHeader("X-AuthorizationService", "Mock");
        request.setHeader("Cookie", String.format("yandexuid = %d;", 67282295));
        request.setEntity(entity);
        return request;
    }

    protected URI patchOfferPricesURI(
            long campaignId,
            String format) {
        try {
            URIBuilder uriBuilder =
                    new URIBuilder(
                            String.format(Locale.US, "%s/campaigns/%d/offer-prices/updates.%s",
                                    urlBasePrefix, campaignId, format));
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Expecting to be valid URI", e);
        }
    }

    protected URI removeAllOfferPricesURI(
            long campaignId,
            String format) {
        try {
            URIBuilder uriBuilder =
                    new URIBuilder(
                            String.format(Locale.US, "%s/campaigns/%d/offer-prices/removals.%s",
                                    urlBasePrefix, campaignId, format));
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Expecting to be valid URI", e);
        }
    }

    protected URI offerPricesURI(
            long campaignId,
            String format,
            Map<String, String> queryParams) {
        try {
            URIBuilder uriBuilder =
                    new URIBuilder(
                            String.format(Locale.US, "%s/campaigns/%d/offer-prices.%s",
                                    urlBasePrefix, campaignId, format));
            for (Map.Entry<String, String> argument : queryParams.entrySet()) {
                uriBuilder.addParameter(argument.getKey(), argument.getValue());
            }
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Expecting to be valid URI", e);
        }
    }

    protected void mockUltraControllerClient() {
        when(ultraControllerClient.getShopSKU(any())).thenAnswer(a -> {
            UltraController.ShopSKURequest request = a.getArgument(0);
            UltraController.SKUMappingResponse.Builder builder = UltraController.SKUMappingResponse.newBuilder();
            for (Long marketSku : request.getMarketSkuIdList()) {
                builder.addSkuMapping(
                        UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                .addShopSku(marketSku.toString())
                                .setMarketSkuId(marketSku)
                                .build());
            }
            return builder.build();
        });
    }

    protected static SyncGetOffer.GetUnitedOffersResponse getUnitedDatacampResponse(final String jsonFile) {
        return getDatacampResponse(
                jsonFile,
                SyncGetOffer.GetUnitedOffersResponse.class
        );
    }

    protected static SyncSearch.SearchResponse getDatacampResponse(final String jsonFile) {
        return getDatacampResponse(
                jsonFile,
                SyncSearch.SearchResponse.class
        );
    }

    protected static SyncGetOffer.GetOfferResponse getDatacampGetOfferResponse(final String jsonFile) {
        return getDatacampResponse(
                jsonFile,
                SyncGetOffer.GetOfferResponse.class
        );
    }

    protected static DataCampOffer.Offer getDatacampPostOfferResponse(final String jsonFile) {
        return getDatacampResponse(
                jsonFile,
                DataCampOffer.Offer.class
        );
    }

    protected static <T extends GeneratedMessageV3> T getDatacampResponse(final String jsonFile,
                                                                          final Class<T> clazz) {
        return ProtoTestUtil.getProtoMessageByJson(
                clazz,
                "datacamp/" + jsonFile,
                AbstractOfferPriceControllerTest.class
        );
    }

    @Nonnull
    protected String fileToString(@Nonnull String test, @Nonnull String testFolder) {
        return StringTestUtil.getString(this.getClass(),
                "OfferPriceController/" + testFolder + "/xml/" + test + ".xml");
    }

    protected void mockSaasService(int total, long partnerId) {
        SaasSearchResult result = SaasSearchResult.builder()
                .setTotalCount(total)
                .build();
        Mockito.doReturn(result)
                .when(saasService)
                .searchBusinessOffers(Mockito.argThat(filter ->
                        filter.getFiltersMap()
                                .get(DataCampSearchAttribute.SEARCH_SHOP_ID)
                                .remove(Long.toString(partnerId))
                ));
    }
}
