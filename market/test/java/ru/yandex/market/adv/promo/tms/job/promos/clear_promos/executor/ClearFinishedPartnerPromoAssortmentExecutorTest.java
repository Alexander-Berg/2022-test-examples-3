package ru.yandex.market.adv.promo.tms.job.promos.clear_promos.executor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import com.google.protobuf.Timestamp;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.datacamp.utils.DateTimeUtils;
import ru.yandex.market.adv.promo.logbroker.model.DatacampMessageLogbrokerEvent;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerBusinessDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnersBusinessResponse;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;

import static Market.DataCamp.DataCampPromo.PromoMechanics.PartnerCustomCashback.CreationTab.DYNAMIC_GROUPS;
import static Market.DataCamp.DataCampPromo.PromoMechanics.PartnerCustomCashback.CreationTab.FILE;
import static Market.DataCamp.DataCampPromo.PromoType.PARTNER_CUSTOM_CASHBACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createBasicOffer;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_CASHBACK_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_CASHBACK_PROMOS_META;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_PROMOS_META;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_IDENTIFIERS;

public class ClearFinishedPartnerPromoAssortmentExecutorTest extends FunctionalTest {

    @Autowired
    private ClearFinishedPartnerPromoAssortmentExecutor clearPartnerPromoAssortmentExecutor;

    @Autowired
    @Qualifier("dataCampClient")
    private DataCampClient dataCampClient;

    @Autowired
    private LogbrokerEventPublisher<DatacampMessageLogbrokerEvent> logbrokerService;

    @Autowired
    private SaasService saasDataCampShopService;

    @Autowired
    private MbiApiClient mbiApiClient;

    @BeforeEach
    public void beforeEach() {
        SaasOfferInfo saasOfferInfo = SaasOfferInfo.newBuilder()
                .addShopId(774L)
                .addOfferId("0516465165")
                .build();
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(List.of(saasOfferInfo))
                .setTotalCount(1)
                .build();
        doReturn(saasSearchResult)
                .doReturn(
                        SaasSearchResult.builder()
                                .setOffers(Collections.emptyList())
                                .setTotalCount(0)
                                .build()
                )
                .when(saasDataCampShopService).searchBusinessOffers(any());
    }

    @Test
    @DbUnitDataSet(
            before = "ClearFinishedPartnerPromosExecutorTest/before.csv",
            after = "ClearFinishedPartnerPromosExecutorTest/after.csv")
    public void deletePromosWithoutOffersTest() {
        mockDescriptions();
        OffersBatch.UnitedOffersBatchResponse unitedResponse = OffersBatch.UnitedOffersBatchResponse.newBuilder()
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .build())
                .build();
        doReturn(unitedResponse).when(dataCampClient).getBusinessUnitedOffers(anyLong(), anyCollection(), any());
        Set<Long> partnerIds = Set.of(11111L);
        PartnersBusinessResponse response = new PartnersBusinessResponse(
                List.of(
                        new PartnerBusinessDTO(11111, 100L)
                )
        );
        doReturn(response).when(mbiApiClient).getBusinessesForPartners(partnerIds);

        clearPartnerPromoAssortmentExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "ClearFinishedPartnerPromosExecutorTest/before_graphqlQuery.csv",
            after = "ClearFinishedPartnerPromosExecutorTest/after.csv")
    public void deletePromosWithoutOffersGraphqlQueryTest() {
        mockDescriptions();
        OffersBatch.UnitedOffersBatchResponse unitedResponse = OffersBatch.UnitedOffersBatchResponse.newBuilder()
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .build())
                .build();
        doReturn(unitedResponse)
                .when(dataCampClient).getBusinessUnitedOffers(
                        anyLong(),
                        anyCollection(),
                        any(),
                        argThat(offerQuery -> CollectionUtils.isEqualCollection(
                                offerQuery.getFields(), EnumSet.of(
                                        OFFER_IDENTIFIERS,
                                        ALL_PARTNER_PROMOS,
                                        ALL_PARTNER_PROMOS_META,
                                        ALL_PARTNER_CASHBACK_PROMOS,
                                        ALL_PARTNER_CASHBACK_PROMOS_META
                                )))
                );
        Set<Long> partnerIds = Set.of(11111L);
        PartnersBusinessResponse response = new PartnersBusinessResponse(
                List.of(
                        new PartnerBusinessDTO(11111, 100L)
                )
        );
        doReturn(response).when(mbiApiClient).getBusinessesForPartners(partnerIds);

        clearPartnerPromoAssortmentExecutor.doJob(null);
    }


    @Test
    @DbUnitDataSet(before = "ClearFinishedPartnerPromosExecutorTest/before.csv")
    public void promoFinishedTest() {
        DataCampPromo.PromoDescription promo = createPromoDescription("11111_aaaaa", 1, 2, true);
        checkOffersClearing("11111_aaaaa", promo);
    }

    @Test
    @DbUnitDataSet(before = "ClearFinishedPartnerPromosExecutorTest/before_graphqlQuery.csv")
    public void promoFinishedGraphqlQueryTest() {
        DataCampPromo.PromoDescription promo = createPromoDescription("11111_aaaaa", 1, 2, true);
        checkOffersClearingGraphqlQuery("11111_aaaaa", promo);
    }

    @Test
    @DbUnitDataSet(before = "ClearFinishedPartnerPromosExecutorTest/before.csv")
    public void cashbackFinishedTest() {
        DataCampPromo.PromoDescription promo = createCashback("11111_aaaaa", 1, 2, true, FILE);
        checkOffersClearing("11111_aaaaa", promo);
    }

    @Test
    @DbUnitDataSet(before = "ClearFinishedPartnerPromosExecutorTest/before_graphqlQuery.csv")
    public void cashbackFinishedGraphqlQueryTest() {
        DataCampPromo.PromoDescription promo = createCashback("11111_aaaaa", 1, 2, true, FILE);
        checkOffersClearingGraphqlQuery("11111_aaaaa", promo);
    }

    @Test
    @DbUnitDataSet(before = "ClearFinishedPartnerPromosExecutorTest/before.csv")
    public void dynamicGroupCashbackTest() {
        long offset = DateTimeUtils.getDateTimeInSeconds(LocalDateTime.now());
        DataCampPromo.PromoDescription promo = createCashback("11111_aaaaa", 1, offset + 2, true, DYNAMIC_GROUPS);
        checkOffersClearing("11111_aaaaa", promo);
    }

    @Test
    @DbUnitDataSet(before = "ClearFinishedPartnerPromosExecutorTest/before_graphqlQuery.csv")
    public void dynamicGroupCashbackGraphqlQueryTest() {
        long offset = DateTimeUtils.getDateTimeInSeconds(LocalDateTime.now());
        DataCampPromo.PromoDescription promo = createCashback("11111_aaaaa", 1, offset + 2, true, DYNAMIC_GROUPS);
        checkOffersClearingGraphqlQuery("11111_aaaaa", promo);
    }

    @Test
    @DbUnitDataSet(before = "ClearFinishedPartnerPromosExecutorTest/before.csv")
    public void promoDescriptionIsNullTest() {
        checkOffersClearing("11111_aaaaa", null);
    }

    @Test
    @DbUnitDataSet(before = "ClearFinishedPartnerPromosExecutorTest/before_graphqlQuery.csv")
    public void promoDescriptionIsNullGraphqlQueryTest() {
        checkOffersClearingGraphqlQuery("11111_aaaaa", null);
    }

    private void checkOffersClearing(
            String promoForClearingId,
            DataCampPromo.PromoDescription promoForClearing
    ) {
        boolean promoIsCashback = false;
        if (promoForClearing != null) {
            promoIsCashback = promoForClearing.getPromoGeneralInfo().getPromoType() == PARTNER_CUSTOM_CASHBACK;
        }
        List<String> promoIds = Collections.emptyList();
        List<String> cashbackPromoIds = Collections.emptyList();
        String promoWithoutClearingId = "11111_bbbbb";
        if (promoIsCashback) {
            cashbackPromoIds = List.of(promoForClearingId, promoWithoutClearingId);
        } else {
            promoIds = List.of(promoForClearingId, promoWithoutClearingId);
        }

        doReturn(createUnitedResponse(promoIds, cashbackPromoIds))
                .when(dataCampClient).getBusinessUnitedOffers(anyLong(), anyCollection(), any());

        checkOffersClearing(promoForClearing, promoWithoutClearingId, promoIsCashback);
    }

    private void checkOffersClearingGraphqlQuery(
            String promoForClearingId,
            DataCampPromo.PromoDescription promoForClearing
    ) {
        boolean promoIsCashback = false;
        if (promoForClearing != null) {
            promoIsCashback = promoForClearing.getPromoGeneralInfo().getPromoType() == PARTNER_CUSTOM_CASHBACK;
        }
        List<String> promoIds = Collections.emptyList();
        List<String> cashbackPromoIds = Collections.emptyList();
        String promoWithoutClearingId = "11111_bbbbb";
        if (promoIsCashback) {
            cashbackPromoIds = List.of(promoForClearingId, promoWithoutClearingId);
        } else {
            promoIds = List.of(promoForClearingId, promoWithoutClearingId);
        }

        doReturn(createUnitedResponse(promoIds, cashbackPromoIds))
                .when(dataCampClient).getBusinessUnitedOffers(
                        anyLong(),
                        anyCollection(),
                        any(),
                        argThat(offerQuery -> CollectionUtils.isEqualCollection(
                                offerQuery.getFields(), EnumSet.of(
                                        OFFER_IDENTIFIERS,
                                        ALL_PARTNER_PROMOS,
                                        ALL_PARTNER_PROMOS_META,
                                        ALL_PARTNER_CASHBACK_PROMOS,
                                        ALL_PARTNER_CASHBACK_PROMOS_META
                                )))
                        );

        checkOffersClearing(promoForClearing, promoWithoutClearingId, promoIsCashback);
    }

    private void checkOffersClearing(
            DataCampPromo.PromoDescription promoForClearing,
            String promoWithoutClearingId,
            boolean promoIsCashback
    ) {
        DataCampPromo.PromoDescription promoWithoutClearing =
                createPromoDescription(promoWithoutClearingId,
                        DateTimeUtils.getDateTimeInSeconds(LocalDateTime.now()),
                        DateTimeUtils.getDateTimeInSeconds(LocalDateTime.now().plus(5, ChronoUnit.DAYS)),
                        true);

        DataCampPromo.PromoDescriptionBatch.Builder promoDescriptionBatchBuilder =
                DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promoWithoutClearing);

        if (promoForClearing != null) {
            promoDescriptionBatchBuilder.addPromo(promoForClearing);
        }

        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(promoDescriptionBatchBuilder.build())
                .build())
                .when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));

        Set<Long> partnerIds = Set.of(11111L);
        PartnersBusinessResponse response = new PartnersBusinessResponse(
                List.of(
                        new PartnerBusinessDTO(11111, 100L)
                )
        );
        doReturn(response).when(mbiApiClient).getBusinessesForPartners(partnerIds);

        clearPartnerPromoAssortmentExecutor.doJob(null);

        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(logbrokerService, times(1)).publishEvent(eventCaptor.capture());
        DatacampMessageLogbrokerEvent event = eventCaptor.getValue();
        DatacampMessageOuterClass.DatacampMessage msg = event.getPayload();
        List<DataCampOffer.Offer> offers = msg.getOffersList().get(0).getOfferList();
        assertEquals(3, offers.size());
        Assertions.assertEquals("offer1", offers.get(0).getIdentifiers().getOfferId());
        if (promoIsCashback) {
            checkOfferHasCashbackPromo(promoWithoutClearingId, offers.get(0));
        } else {
            checkOfferHasPartnerPromo(promoWithoutClearingId, offers.get(0));
        }
        Assertions.assertEquals("offer2", offers.get(1).getIdentifiers().getOfferId());
        if (promoIsCashback) {
            checkOfferHasCashbackPromo(promoWithoutClearingId, offers.get(1));
        } else {
            checkOfferHasPartnerPromo(promoWithoutClearingId, offers.get(1));
        }

    }

    private OffersBatch.UnitedOffersBatchResponse createUnitedResponse(
            List<String> promoIds,
            List<String> cashbackPromoIds
    ) {
        DataCampOffer.Offer basicOffer1 = createBasicOffer("offer1", 11111, 100);
        DataCampOffer.Offer serviceOffer1 =
                createServiceOffer(basicOffer1, 100, 1, promoIds, cashbackPromoIds);
        DataCampOffer.Offer basicOffer2 = createBasicOffer("offer2", 11111, 100);
        DataCampOffer.Offer serviceOffer2 =
                createServiceOffer(basicOffer2, 100, 1, promoIds, cashbackPromoIds);
        DataCampOffer.Offer basicOffer3 = createBasicOffer("offer3", 11111, 100);
        DataCampOffer.Offer serviceOffer3 =
                createServiceOffer(basicOffer3, 100, 1, promoIds, cashbackPromoIds);

        return OffersBatch.UnitedOffersBatchResponse.newBuilder()
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .setUnitedOffer(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(basicOffer1)
                                .putActual(1, DataCampUnitedOffer.ActualOffers.newBuilder()
                                        .putWarehouse(1, basicOffer1)
                                        .build())
                                .putService(1, serviceOffer1)
                                .build())
                        .build())
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .setUnitedOffer(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(basicOffer2)
                                .putActual(1, DataCampUnitedOffer.ActualOffers.newBuilder()
                                        .putWarehouse(1, basicOffer2)
                                        .build())
                                .putService(1, serviceOffer2)
                                .build())
                        .build())
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .setUnitedOffer(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(basicOffer3)
                                .putActual(1, DataCampUnitedOffer.ActualOffers.newBuilder()
                                        .putWarehouse(1, basicOffer3)
                                        .build())
                                .putService(1, serviceOffer3)
                                .build())
                        .build())
                .build();
    }

    private void checkOfferHasPartnerPromo(String promoId, DataCampOffer.Offer offer) {
        assertTrue(offer.getPromos().hasPartnerPromos());
        assertFalse(offer.getPromos().hasAnaplanPromos());
        assertEquals(1, offer.getPromos().getPartnerPromos().getPromosCount());
        DataCampOfferPromos.Promo promo1 = offer.getPromos().getPartnerPromos().getPromos(0);
        Assertions.assertEquals(promoId, promo1.getId());
    }

    private void checkOfferHasCashbackPromo(String promoId, DataCampOffer.Offer offer) {
        assertTrue(offer.getPromos().hasPartnerCashbackPromos());
        assertFalse(offer.getPromos().hasAnaplanPromos());
        assertEquals(1, offer.getPromos().getPartnerCashbackPromos().getPromosCount());
        DataCampOfferPromos.Promo promo1 = offer.getPromos().getPartnerCashbackPromos().getPromos(0);
        Assertions.assertEquals(promoId, promo1.getId());
    }

    private void mockDescriptions() {
        long farFromNow = LocalDate.now().toEpochDay() + 1000000000000L;
        DataCampPromo.PromoDescription promoA = createPromoDescription("11111_aaaaa", 1, 2, true);

        DataCampPromo.PromoDescription promoC = createPromoDescription("11111_bbbbb", 1, farFromNow, true);

        DataCampPromo.PromoDescription promoB = createPromoDescription("11111_ccccc", 1, 2, false);

        DataCampPromo.PromoDescription promoD = createPromoDescription("11111_ddddd", 1, farFromNow, false);

        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promoA)
                        .addPromo(promoB)
                        .addPromo(promoC)
                        .addPromo(promoD)
                        .build())
                .build())
                .when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
    }

    private DataCampPromo.PromoDescription createPromoDescription(
            String promoId,
            long startDate,
            long endDate,
            boolean enabled
    ) {
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId(promoId)
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .setEnabled(enabled)
                        .build())
                .build();
    }

    private DataCampPromo.PromoDescription createCashback(
            String promoId,
            long startDate,
            long endDate,
            boolean enabled,
            DataCampPromo.PromoMechanics.PartnerCustomCashback.CreationTab creationTab
    ) {
        DataCampPromo.PromoMechanics.PartnerCustomCashback customCashback =
                DataCampPromo.PromoMechanics.PartnerCustomCashback.newBuilder()
                        .setSource(creationTab)
                        .build();

        DataCampPromo.PromoMechanics.newBuilder()
                .setPartnerCustomCashback(
                        customCashback
                )
                .build();

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId(promoId)
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .setEnabled(enabled)
                        .build())
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(PARTNER_CUSTOM_CASHBACK)
                                .build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setPartnerCustomCashback(
                                customCashback
                        )
                        .build())
                .build();
    }

    private DataCampOffer.Offer createServiceOffer(
            DataCampOffer.Offer basicOffer,
            long seconds,
            int nanos,
            List<String> promoIds,
            List<String> cashbackPromoIds
    ) {
        return DataCampOffer.Offer.newBuilder()
                .setIdentifiers(basicOffer.getIdentifiers())
                .setPromos(DataCampOfferPromos.OfferPromos.newBuilder()
                        .setPartnerPromos(DataCampOfferPromos.Promos.newBuilder()
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setTimestamp(Timestamp.newBuilder()
                                                .setSeconds(seconds)
                                                .setNanos(nanos)
                                                .build())
                                        .build())
                                .addAllPromos(promoIds.stream().map(promoId ->
                                        DataCampOfferPromos.Promo.newBuilder()
                                                .setId(promoId)
                                                .build()).collect(Collectors.toList())
                                )
                                .build())
                        .setPartnerCashbackPromos(
                                DataCampOfferPromos.Promos.newBuilder()
                                        .addAllPromos(cashbackPromoIds.stream().map(promoId ->
                                                DataCampOfferPromos.Promo.newBuilder()
                                                        .setId(promoId)
                                                        .build()).collect(Collectors.toList())
                                        )

                        )
                        .setAnaplanPromos(
                                DataCampOfferPromos.MarketPromos.newBuilder()
                                        .setAllPromos(DataCampOfferPromos.Promos.newBuilder()
                                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                        .setTimestamp(Timestamp.newBuilder()
                                                                .setSeconds(seconds)
                                                                .setNanos(nanos)
                                                                .build())
                                                        .build())
                                                .addAllPromos(List.of(
                                                        DataCampOfferPromos.Promo.newBuilder()
                                                                .setId("another_promo")
                                                                .build())
                                                )
                                                .build())
                                        .build()
                        )
                        .build())
                .build();

    }
}
