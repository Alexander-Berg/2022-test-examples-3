package ru.yandex.market.ff4shops.tms.jobs.datacamp;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.mbi.feature.model.FeatureStatus;
import ru.yandex.market.ff4shops.partner.PartnerOffersImporterService;
import ru.yandex.market.ff4shops.partner.service.PartnerService;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOfferStocksRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOfferStocksResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ImportDataCampOffersTest extends FunctionalTest {

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private DataCampClient dataCampShopClient;

    @Autowired
    private PartnerOffersImporterService unitedOffersImporterService;

    @Test
    @DbUnitDataSet(before = "ImportPartnerOffersTest.before.csv", after = "ImportPartnerOffersTest.after.csv")
    void doJobUnited() {
        DataCampOffer.Offer offer1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("offer_id_1")
                                .setShopId(10281764)
                                .build()
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(
                                DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(
                                                DataCampOfferMapping.Mapping.newBuilder().setMarketSkuId(100500)
                                        )
                        )
                )
                .setStatus(
                        DataCampOfferStatus.OfferStatus.newBuilder()
                                .setPublishByPartner(DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE)
                )
                .build();
        DataCampOffer.Offer offer2 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("offer_id_2")
                                .setShopId(10281764)
                                .build()
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(
                                DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(
                                                DataCampOfferMapping.Mapping.newBuilder().setMarketSkuId(100500)
                                        )
                        )
                )
                .setStatus(
                        DataCampOfferStatus.OfferStatus.newBuilder()
                                .setPublishByPartner(DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE)
                )
                .build();

        List<DataCampOffer.Offer> offers = Arrays.asList(offer1, offer2);

        SearchBusinessOfferStocksResult result = SearchBusinessOfferStocksResult.builder()
                .setOffers(offers)
                .setOffersCount(offers.size())
                .build();

        ArgumentCaptor<SearchBusinessOfferStocksRequest> captor =
                ArgumentCaptor.forClass(SearchBusinessOfferStocksRequest.class);
        when(dataCampShopClient.searchBusinessOfferIdentifiers(captor.capture())).thenReturn(result);

        ForkJoinPool forkJoinPool = new ForkJoinPool(5);
        ImportDataCampOffersJob job = new ImportDataCampOffersJob(
                partnerService, dataCampShopClient, unitedOffersImporterService,
                List.of(FeatureType.MARKETPLACE_SELF_DELIVERY, FeatureType.DROPSHIP, FeatureType.CROSSDOCK),
                List.of(FeatureStatus.SUCCESS, FeatureStatus.NEW),
                forkJoinPool);
        job.doJob(null);
    }

    @Test
    @DbUnitDataSet(before = "ImportDataCampOffersTest.jobException.before.csv")
    void doJobWithExceptions() {
        when(dataCampShopClient.searchBusinessOfferIdentifiers(any()))
                .thenAnswer(invocation -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        ForkJoinPool forkJoinPool = new ForkJoinPool(5);
        ImportDataCampOffersJob job = new ImportDataCampOffersJob(
                partnerService, dataCampShopClient, unitedOffersImporterService,
                List.of(FeatureType.MARKETPLACE_SELF_DELIVERY),
                List.of(FeatureStatus.SUCCESS, FeatureStatus.NEW),
                forkJoinPool);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> job.doJob(null)
        );

        List<String> messages = Stream.concat(Stream.of(exception), Arrays.stream(exception.getSuppressed()))
                .map(Throwable::getMessage)
                .collect(Collectors.toList());

        assertTrue(messages.contains("Failed to import offers for businessId=123, partnerId=10270530"));
        assertTrue(messages.contains("Failed to import offers for businessId=124, partnerId=10270531"));
        assertTrue(messages.contains("Failed to import offers for businessId=125, partnerId=10270532"));
    }
}
