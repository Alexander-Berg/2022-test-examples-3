package ru.yandex.market.marketpromo.core.test.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent.MarketContent;
import Market.DataCamp.DataCampOfferContent.OfferContent;
import Market.DataCamp.DataCampOfferIdentifiers.OfferExtraIdentifiers;
import Market.DataCamp.DataCampOfferIdentifiers.OfferIdentifiers;
import Market.DataCamp.DataCampOfferPrice.OfferPrice;
import Market.DataCamp.DataCampOfferPrice.PriceBundle;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampOfferPromos.OfferPromos;
import Market.DataCamp.DataCampPartnerInfo;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import NMarketIndexer.Common.Common.PriceExpression;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import ru.yandex.market.marketpromo.core.application.context.OfferStorage;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.OfferStorageSaasClient;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.OfferStorageStrollerClient;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.SaasOfferProperty;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.SearchOffersRequest;
import ru.yandex.market.marketpromo.model.BusinessOfferKey;
import ru.yandex.market.marketpromo.model.BusinessShopKey;
import ru.yandex.market.marketpromo.model.DatacampOffer;
import ru.yandex.market.marketpromo.model.SupplierType;
import ru.yandex.market.saas.search.DocumentsConverter;
import ru.yandex.market.saas.search.SaasSearchException;
import ru.yandex.market.saas.search.SaasSearchService;
import ru.yandex.market.saas.search.response.SaasSearchDocument;
import ru.yandex.market.saas.search.response.SaasSearchResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.OFFER_STORAGE_ACTIVE;

@Component
public class OfferStorageTestHelper {

    private final ApplicationContext applicationContext;
    private final OfferStorageSaasClient saasClient;
    private final OfferStorageStrollerClient strollerClient;
    private final SaasSearchService saasSearchService;
    private final CloseableHttpClient httpClient;

    public OfferStorageTestHelper(ApplicationContext applicationContext, OfferStorageSaasClient saasClient,
                                  OfferStorageStrollerClient strollerClient, SaasSearchService saasSearchService,
                                  @OfferStorage HttpClientBuilder clientBuilder) {
        this.applicationContext = applicationContext;
        this.saasClient = saasClient;
        this.strollerClient = strollerClient;
        this.saasSearchService = saasSearchService;
        this.httpClient = clientBuilder.build();
    }

    public void mockSaasSearchServiceResponse(@Nonnull List<String> sskus) {
        if (isIntegrationContext()) {
            return;
        }

        try {
            SaasSearchResponse saasSearchResponse = Mockito.mock(SaasSearchResponse.class);
            List<SaasSearchDocument> documents = sskus.stream()
                    .map(ssku -> {
                        SaasSearchDocument saasSearchDocument = Mockito.mock(SaasSearchDocument.class);
                        when(saasSearchDocument.getProperty(eq(SaasOfferProperty.SHOP_SKU))).thenReturn(ssku);
                        return saasSearchDocument;
                    }).collect(Collectors.toUnmodifiableList());

            when(saasSearchResponse.getDocuments(any())).thenAnswer(invocation ->
                    invocation.getArgument(0, DocumentsConverter.class).convert(documents));
            when(saasSearchResponse.getTotal()).thenReturn(documents.size());
            when(saasSearchService.search(any()))
                    .thenReturn(saasSearchResponse);

            when(saasClient.searchOffersBy(any())).thenCallRealMethod();
        } catch (SaasSearchException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public void mockStrollerSearchOffersByShopResponse(long shopId, long warehousId, @Nonnull List<String> sskus) {
        if (isIntegrationContext()) {
            return;
        }

        try {
            SyncChangeOffer.FullOfferResponse offersResponse = getOfferResponse(shopId, warehousId, sskus);

            HttpResponse response = Mockito.mock(HttpResponse.class);
            HttpEntity entity = mock(HttpEntity.class);

            when(response.getEntity()).thenReturn(entity);
            StatusLine statusLine = mock(StatusLine.class);
            when(response.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
            when(entity.getContent()).thenReturn(new ByteArrayInputStream(offersResponse.toByteArray()));
            when(httpClient.execute(
                    any(HttpUriRequest.class),
                    any(ResponseHandler.class)
            )).thenAnswer(invocation ->
                    invocation.getArgument(1, ResponseHandler.class).handleResponse(response));

            when(strollerClient.searchOffersByShop(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            )).thenCallRealMethod();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void mockStrollerSearchOffersResponse(@Nonnull List<DatacampOffer> offersToMock) {
        if (isIntegrationContext()) {
            return;
        }

        final Map<BusinessShopKey, List<DatacampOffer>> offersByShop = offersToMock.stream()
                .collect(Collectors.groupingBy(o -> BusinessOfferKey.of(o).toBusinessShopKey()));

        for (Map.Entry<BusinessShopKey, List<DatacampOffer>> entry : offersByShop.entrySet()) {
            final BusinessShopKey shopKey = entry.getKey();
            final List<DatacampOffer> offers = entry.getValue();

            final SearchOffersRequest searchOffersRequest = SearchOffersRequest.builder()
                    .limit(offers.size())
                    .businessId(shopKey.getBusinessId())
                    .shopId(shopKey.getShopId())
                    .shopSkus(offers.stream()
                            .map(DatacampOffer::getShopSku)
                            .collect(Collectors.toUnmodifiableSet()))
                    .build();

            when(strollerClient.searchOffersByBusiness(eq(searchOffersRequest)))
                    .thenReturn(offers);
        }
    }

    @NotNull
    private SyncChangeOffer.FullOfferResponse getOfferResponse(long shopId,
                                                               long warehouseId,
                                                               @Nonnull List<String> sskus) {
        return SyncChangeOffer.FullOfferResponse.newBuilder()
                .addAllOffer(sskus.stream()
                        .map(ssku -> DataCampOffer.Offer.newBuilder()
                                .setIdentifiers(OfferIdentifiers.newBuilder()
                                        .setOfferId(ssku)
                                        .setShopId((int) shopId)
                                        .setFeedId(123)
                                        .setWarehouseId((int) warehouseId)
                                        .setExtra(OfferExtraIdentifiers.newBuilder()
                                                .setMarketSkuId(ssku.hashCode())
                                                .setShopSku(ssku)
                                                .setWareMd5(DigestUtils.md5Hex(ssku))
                                                .build())
                                        .build())
                                .setContent(OfferContent.newBuilder()
                                        .setMarket(MarketContent.newBuilder()
                                                .setCategoryId(123)
                                                .setProductName("Some offer" + ssku)
                                                .build()))
                                .setPartnerInfo(DataCampPartnerInfo.PartnerInfo.newBuilder()
                                        .setSupplierId((int) shopId)
                                        .setSupplierType(SupplierType._1P.getReportCode())
                                        .build())
                                .setPromos(OfferPromos.newBuilder()
                                        .addPromo(DataCampOfferPromos.Promo.newBuilder()
                                                .setId("#21098")
                                                .setActive(true)
                                                .build())
                                        .build())
                                .setPrice(OfferPrice.newBuilder()
                                        .setBasic(PriceBundle.newBuilder()
                                                .setBinaryPrice(PriceExpression.newBuilder()
                                                        .setPrice(1000_0_000_000L)
                                                        .build())
                                                .setBinaryOldprice(PriceExpression.newBuilder()
                                                        .setPrice(1500_0_000_000L)
                                                        .build())
                                                .build())
                                        .build())

                                .build())

                        .collect(Collectors.toUnmodifiableList()))
                .build();
    }

    public void reset() {
        if (isIntegrationContext()) {
            return;
        }
        Mockito.reset(saasClient, saasSearchService, strollerClient, httpClient);
    }

    private boolean isIntegrationContext() {
        return applicationContext.getEnvironment().acceptsProfiles(Profiles.of(OFFER_STORAGE_ACTIVE));
    }

}
