package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.util.List;

import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.model.unitedcatalog.CopyOffersParams;
import ru.yandex.market.rg.asyncreport.unitedcatalog.migration.delete.UnmigratedOffersRemover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.rg.asyncreport.unitedcatalog.migration.UnitedOfferBuilder.offerBuilder;

@DbUnitDataSet(before = "OffersToEkatCopierTest.before.csv")
public class UnmigratedStocksRemoverTest extends AbstractMigrationTaskProcessorTest {

    public static final int PARTNER_ID = 101;
    public static final long BUSINESS_ID = 99L;
    private static final CopyOffersParams COPY_OFFERS_PARAMS =
            new CopyOffersParams(BUSINESS_ID, BUSINESS_ID, PARTNER_ID);

    UnmigratedOffersRemover unmigratedStocksRemover;

    @BeforeEach
    void setUp() {
        environmentService.setValue("business.migration.batch.size", "10");
        if (unmigratedStocksRemover == null) {
            unmigratedStocksRemover = new UnmigratedOffersRemover(
                    reportsMdsStorage, dataCampMigrationClient, reportsService, migrationTaskStateService,
                    assortmentReportWriteService, mboMappingService, offerConversionService,
                    environmentService, clock
            );
        }
    }

    @Test
    void testRemoval() {
        mockDataCamp(99, PARTNER_ID, 2, new DataCampMockRequest(null, List.of()),
                DataCampMockResponse.ofOffers(null, List.of(
                        offerBuilder(99, (int) PARTNER_ID, "1000")
                                .withMapping(10000)
                                .withName("test1")
                                .withActualService(PARTNER_ID, 123, true, null)
                                .withActualService(PARTNER_ID, 222, false, null)
                                .build(),
                        offerBuilder(99, (int) PARTNER_ID, "1001")
                                .withActualService(PARTNER_ID, 123, false, null)
                                .build(),
                        offerBuilder(99, (int) PARTNER_ID, "1002", true)
                                .withMapping(10002)
                                .withName("good-name")
                                .withActualService(PARTNER_ID, 123, true, null)
                                .build()))
        );
        willReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .given(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());

        unmigratedStocksRemover.generate("1", COPY_OFFERS_PARAMS);

        //noinspection unchecked
        ArgumentCaptor<List<DataCampUnitedOffer.UnitedOffer>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataCampMigrationClient).changeBusinessUnitedOffers(eq(BUSINESS_ID), eq(Long.valueOf(PARTNER_ID)),
                captor.capture());


        var actual = captor.getValue();
        assertEquals(2, actual.size());
        //Проверим, что удаляем сервисную часть
        assertTrue(actual.stream()
                .filter(o -> o.getBasic().getIdentifiers().getOfferId().equals("1000"))
                .map(o -> o.getServiceMap().get(PARTNER_ID))
                .allMatch(o -> o.getStatus().getRemoved().getFlag()));
        //Проверим, что удаляем базовую часть если под ней удалили все сервисные
        assertTrue(actual.stream()
                .filter(o -> o.getBasic().getIdentifiers().getOfferId().equals("1000"))
                .allMatch(o -> o.getBasic().getStatus().getRemoved().getFlag()));

        //Проверили что не тронули ничего
        assertTrue(actual.stream()
                .filter(o -> o.getBasic().getIdentifiers().getOfferId().equals("1002"))
                .map(o -> o.getServiceMap().get(PARTNER_ID))
                .noneMatch(o -> o.getStatus().getRemoved().getFlag()));
        assertTrue(actual.stream()
                .filter(o -> o.getBasic().getIdentifiers().getOfferId().equals("1002"))
                .noneMatch(o -> o.getBasic().getStatus().getRemoved().getFlag()));
    }
}
