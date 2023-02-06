package ru.yandex.market.core.offer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author stani on 28.05.18.
 */

@DbUnitDataSet(
        before = "PapiMarketSkuOfferMetaDataServiceTest.before.csv"
)
class PapiMarketSkuOfferMetaDataServiceTest extends FunctionalTest {

    @Autowired
    PapiMarketSkuOfferMetaDataService papiMarketSkuOfferMetaDataService;

    @Autowired
    MboMappingsService mboMappingsService;

    private static MboMappings.ApprovedMappingInfo mapping(int supplierId, String shopSku, long marketSku) {
        return MboMappings.ApprovedMappingInfo.newBuilder()
                .setSupplierId(supplierId)
                .setShopSku(shopSku)
                .setMarketSkuId(marketSku)
                .setShopTitle("Title of " + shopSku)
                .setMarketCategoryId(20L)
                .build();
    }

    @Test
    @DbUnitDataSet(
            after = "PapiMarketSkuOfferMetaDataServiceTest.insert.after.csv"
    )
    void testInsert() {
        MboMappings.SearchApprovedMappingsResponse mappingResponse = MboMappings.SearchApprovedMappingsResponse.newBuilder()
                .addMapping(mapping(1001, "S1001SKU2", 127))
                .addMapping(mapping(1001, "S1001SKU4", 128))
                .addMapping(mapping(1001, "S1001SKU5", 126))
                .addMapping(mapping(1001, "S1001SKU6", 124))
                .addMapping(mapping(1001, "S1001SKU7", 123))
                .addMapping(mapping(1002, "S1002SKU1", 126))
                .build();

        when(mboMappingsService.searchApprovedMappingsByMarketSkuId(any())).thenReturn(mappingResponse);

        papiMarketSkuOfferMetaDataService.addMissingShopSkus();
    }

    @Test
    @DbUnitDataSet(
            after = "PapiMarketSkuOfferMetaDataServiceTest.delete.after.csv"
    )
    void testDeleteUreferenced() {
        papiMarketSkuOfferMetaDataService.deleteUnreferenced();
    }

    @Test
    @DbUnitDataSet(
            after = "PapiMarketSkuOfferMetaDataServiceTest.update.after.csv"
    )
    void testUpdate() {
        MboMappings.SearchApprovedMappingsResponse mappingResponse = MboMappings.SearchApprovedMappingsResponse.newBuilder()
                .addMapping(mapping(1001, "S1001SKU1", 124))
                .addMapping(mapping(1001, "S1001SKU2", 123))
                .addMapping(mapping(1001, "S1001SKU7", 125))
                .build();

        when(mboMappingsService.searchApprovedMappingsByMarketSkuId(any())).thenReturn(mappingResponse);
        papiMarketSkuOfferMetaDataService.updateMarketSkus();
    }
}
