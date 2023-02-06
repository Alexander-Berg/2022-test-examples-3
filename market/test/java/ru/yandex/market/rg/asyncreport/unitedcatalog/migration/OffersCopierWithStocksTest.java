package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.util.List;

import Market.DataCamp.DataCampOfferMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.supplier.service.PartnerFulfillmentLinkService;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "FbsToFbsMigration.before.csv")
class OffersCopierWithStocksTest extends AbstractMigrationTaskProcessorTest {

    @Autowired
    private PartnerFulfillmentLinkService partnerFulfillmentLinkService;

    private OffersCopierWithStocks offersCopierWithStocks;

    @BeforeEach
    void setUp() {
        offersCopierWithStocks = new OffersCopierWithStocks(reportsMdsStorage, dataCampMigrationClient,
                mboMappingService, offerConversionService, reportsService, migrationTaskStateService,
                assortmentReportWriteService,
                environmentService, clock, distributedMigrators, partnerFulfillmentLinkService);
    }

    @Test
    @DisplayName("Нет оферов на целевом магазине")
    void emptyOfferSourcePartner() {
        doReturn(SearchBusinessOffersResult.builder().build())
                .when(dataCampMigrationClient).searchBusinessOffers(any());
        offersCopierWithStocks.generate("23", new FbsMigrationParams(123L, 234L, 99L, 345L, 987L));
        verify(dataCampMigrationClient, times(1)).searchBusinessOffers(any());
        verify(dataCampMigrationClient, times(0)).changeBusinessUnitedOffers(any(), any(), any());
    }

    @Test
    @DisplayName("Копирование офера")
    void oneOfferCopy() {
        var offerMeta = DataCampOfferMeta.OfferMeta.newBuilder()
                .setScope(DataCampOfferMeta.OfferScope.SERVICE)
                .build();
        mockDataCamp(123, 234, 1, new DataCampMockRequest(null, List.of()),
                DataCampMockResponse.ofOffers(null, List.of(
                        UnitedOfferBuilder.offerBuilder(123, 234, "1000", 567, true, false, null)
                .withActualService(234, 567, true, null).build()))
        );
        offersCopierWithStocks.generate("23",
                new FbsMigrationParams(123L, 234L, 567L, 345L, 987L));
        verify(dataCampMigrationClient, times(1)).searchBusinessOffers(any());
        verify(dataCampMigrationClient).changeBusinessUnitedOffers(123L, 345L,
                List.of(UnitedOfferBuilder.offerBuilder(123, 345, "1000", 456, true, false, offerMeta)
                        .withActualService(345, 456, true, offerMeta).build()));
    }
}
