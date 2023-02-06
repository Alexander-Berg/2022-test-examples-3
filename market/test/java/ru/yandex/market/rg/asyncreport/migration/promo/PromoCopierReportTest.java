package ru.yandex.market.rg.asyncreport.migration.promo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeGenerationResponse;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.rg.asyncreport.unitedcatalog.migration.UnitedOfferBuilder;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromoCopierReportTest extends FunctionalTest {

    private static final Long BUSINESS_ID = 200L;

    private static final Long NEW_WAREHOUSE_ID = 400L;

    private static final Long NEW_PARTNER_ID = 101L;

    private PromoCopierReport promoCopierReport;

    @Autowired
    private DataCampPromoCopier dataCampPromoCopier;

    @Autowired
    private DataCampClient dataCampShopClient;

    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;

    @Autowired
    private DataCampService dataCampService;

    @Autowired
    private EnvironmentService environmentService;

    private DataCampService spyDataCampService;

    @BeforeEach
    void setUp() {
        spyDataCampService = spy(dataCampService);
        OfferPromoUpdater offerPromoUpdater = new OfferPromoUpdater(spyDataCampService, environmentService);
        promoCopierReport = new PromoCopierReport(offerPromoUpdater, dataCampPromoCopier);
    }

    @Test
    void noPromoToUpdate() {
        var params = new PromoCopierParams(100,
                NEW_PARTNER_ID,
                300,
                NEW_WAREHOUSE_ID,
                BUSINESS_ID);
        var result = promoCopierReport.generate("someId", params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());
        verify(marketLoyaltyClient, never()).generatePromocode();
        verify(dataCampShopClient, never())
                .addPromo(any(SyncGetPromo.UpdatePromoBatchRequest.class), anyLong());
        verify(dataCampShopClient, never())
                .changeBusinessUnitedOffers(anyLong(), anyLong(), anyCollection());
    }

    @Test
    @DbUnitDataSet(before = "DataCampPromoCopierCreatePromoTest.before.csv",
    after = "PromoCopierReportTest.after.csv")
    void allPromoTypeUpdated() throws NoSuchFieldException, IllegalAccessException {
        var params = new PromoCopierParams(100,
                NEW_PARTNER_ID,
                300,
                NEW_WAREHOUSE_ID,
                BUSINESS_ID);
        var pageSize = 2;
        var pageCount = 2;
        initMock(pageCount, pageSize, 0, DataCampPromo.PromoType.MARKET_PROMOCODE);
        initMock(1, 1, 4, DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK);
        initMock(pageCount, pageSize, 5, DataCampPromo.PromoType.PARTNER_CUSTOM_CASHBACK);
        mockUUID(5, dataCampPromoCopier);
        var result = promoCopierReport.generate("someId", params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());
        verify(dataCampShopClient, times(1))
                .addPromo(any(SyncGetPromo.UpdatePromoBatchRequest.class), anyLong());
        verify(dataCampShopClient, times(1))
                .changeBusinessUnitedOffers(anyLong(), anyLong(), anyCollection());
    }

    private void mockUUID(int uuidCount, Object testedClass) throws NoSuchFieldException, IllegalAccessException {
        if (uuidCount > 9) {
            throw new IllegalStateException("Can mock only 10 uuids");
        }
        Supplier<UUID> uuidSupplier = mock(Supplier.class);
        var uuidSupplierFiled =  DataCampPromoCopier.class.getDeclaredField("uuidSupplier");
        uuidSupplierFiled.setAccessible(true);
        uuidSupplierFiled.set(testedClass, uuidSupplier);
        List<UUID> uuids = new ArrayList<>();
        String template = "6cb4775e-97a9-4a2f-98d8-ba99ae1f12a";
        for (int i = 0; i < uuidCount; i++) {
            uuids.add(UUID.fromString(template + i));
        }
        when(uuidSupplier.get()).thenReturn(uuids.stream().findFirst().get(),
                uuids.stream().skip(1).toArray(UUID[]::new));
    }

    private void initMock(int pageCount, int pageSize, int startId, DataCampPromo.PromoType type) {
        var req = new PromoDatacampRequest.Builder(BUSINESS_ID)
                .withPartnerId(100L)
                .withOnlyUnfinished(true)
                .withLimit(100)
                .withPromoType(Set.of(type));
        String prevPageToken = null;
        int startPagePromoId = startId;
        int endPagePromoId = startId + pageSize;
        for (int i = 0; i < pageCount; i++) {
            String nextPageToken = i + 1 == pageCount ? null : String.valueOf(i);
            doReturn(generatePromoResponse(startPagePromoId, endPagePromoId, nextPageToken, type))
                    .when(dataCampShopClient).getPromos(eq(req.withPosition(prevPageToken).build()));
            prevPageToken = nextPageToken;
            startPagePromoId = endPagePromoId;
            endPagePromoId += pageSize;
        }
        if (type == DataCampPromo.PromoType.MARKET_PROMOCODE) {
            var promoCodesResponse = IntStream.rangeClosed(1000, pageCount * pageSize + 999)
                    .mapToObj(v -> PromocodeGenerationResponse.of(String.valueOf(v)));
            when(marketLoyaltyClient.generatePromocode()).thenReturn(
                    PromocodeGenerationResponse.of(String.valueOf(1200)),
                    promoCodesResponse.toArray(PromocodeGenerationResponse[]::new));
            doReturn(generateUnitedOfferStream(Set.of("4", "7")))
                    .when(spyDataCampService).streamDataCampOffers(any(SearchBusinessOffersRequest.class));
        }
    }

    private Stream<DataCampUnitedOffer.UnitedOffer> generateUnitedOfferStream(Set<String> promoIds) {
        return promoIds.stream().map(promoId ->
                UnitedOfferBuilder.offerBuilder(BUSINESS_ID.intValue(), NEW_PARTNER_ID.intValue(), promoId,
                        NEW_WAREHOUSE_ID.intValue(), true, false, null)
                        .withPromo(DataCampPromo.PromoType.MARKET_PROMOCODE, promoId)
                        .build());
    }

    private SyncGetPromo.GetPromoBatchResponse generatePromoResponse(int startId,
                                                                     int endId,
                                                                     String nextPageHash,
                                                                     DataCampPromo.PromoType type) {
        var response = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(generatePromo(startId, endId, type));
        if (nextPageHash != null) {
            response.setNextPagePosition(nextPageHash);
        }
        return response.build();
    }

    private DataCampPromo.PromoDescriptionBatch generatePromo(int startId, int endId, DataCampPromo.PromoType type) {

        var promoDesc = new ArrayList<DataCampPromo.PromoDescription>();
        for (int promoId = startId; promoId < endId; promoId++) {
            var promo = DataCampPromo.PromoDescription.newBuilder()
                    .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                            .setPromoId(String.valueOf(promoId)))
                    .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                            .setPromoType(type)
                            .build())
                    .setLoyaltyKey(DataCampPromo.LoyaltyKey.newBuilder()
                            .setLoyaltyPromoKey("someLoyalty")
                            .setLoyaltyPromoId(100500)
                            .build())
                    .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                            .setStartDate(1500100)
                            .setEndDate(1501100)
                            .setEnabled(true)
                            .addAllOffersMatchingRules(generateRestrictions(100, 300))
                            .buildPartial());
            if (type == DataCampPromo.PromoType.MARKET_PROMOCODE) {
                promo.setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                .setPromoCode(String.valueOf(promoId))
                                .build())
                        .build());
                promo.setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName(promoId + "someName")
                        .build());
            }
            promoDesc.add(promo.build());
        }
        return DataCampPromo.PromoDescriptionBatch.newBuilder().addAllPromo(promoDesc).build();
    }

    private List<DataCampPromo.PromoConstraints.OffersMatchingRule> generateRestrictions(int supplierId,
                                                                                         int warehouseId) {
        return List.of(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                .setSupplierRestriction(
                        DataCampPromo.PromoConstraints.OffersMatchingRule.SupplierRestriction.newBuilder()
                                .setSuppliers(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                        .addId(supplierId)
                                        .build())
                                .build())
                .setWarehouseRestriction(
                        DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                                .setWarehouse(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                        .addId(warehouseId)
                                        .build()))
                .setBrandTriggerRestriction(
                        DataCampPromo.PromoConstraints.OffersMatchingRule.BrandTriggerRestriction.newBuilder()
                                .setBrandTrigger(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                        .addId(100500L)
                                        .build())
                                .build())
                .build());
    }
}
