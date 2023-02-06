package ru.yandex.market.mboc.app.offers;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mboc.app.pipeline.BasePipelineTest;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.masterdata.model.MasterDataAsJsonDTO;
import ru.yandex.market.mboc.common.masterdata.model.MasterDataDto;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class MasterDataRepositoryTest extends BasePipelineTest {
    private final int SUPPLIER_ID1 = 123456;
    private final String SHOP_SKU1 = "shop-sku-1";

    @Before
    public void startup() {
        supplierRepository.insert(new Supplier(SUPPLIER_ID1, "test-supplier"));
    }

    @Test
    public void whenInsertOrUpdateConflictThenUpdates() {
        MasterDataAsJsonDTO masterData = new MasterDataAsJsonDTO();
        masterData.setSupplierId(SUPPLIER_ID1);
        masterData.setShopSku(SHOP_SKU1);
        masterData.setMasterData(buildMasterData());

        masterDataFor1pRepository.insertOrUpdateAll(List.of(masterData));

        var mdFromRepository = masterDataFor1pRepository.findByIds(List.of(new ShopSkuKey(SUPPLIER_ID1, SHOP_SKU1)));
        Assertions.assertThat(mdFromRepository.get(0).getMasterData().getBoxCount()).isEqualTo(13);

        masterData.getMasterData().setBoxCount(10);
        masterDataFor1pRepository.insertOrUpdateAll(List.of(masterData));
        mdFromRepository = masterDataFor1pRepository.findByIds(List.of(new ShopSkuKey(SUPPLIER_ID1, SHOP_SKU1)));
        Assertions.assertThat(mdFromRepository.get(0).getMasterData().getBoxCount()).isEqualTo(10);
        Assertions.assertThat(mdFromRepository).hasSize(1);
    }

    @Test
    public void whenTryingToInsertConflictingMDThenThrows() {
        MasterDataAsJsonDTO masterData = new MasterDataAsJsonDTO();
        masterData.setSupplierId(SUPPLIER_ID1);
        masterData.setShopSku(SHOP_SKU1);
        masterData.setMasterData(buildMasterData());

        masterDataFor1pRepository.insert(masterData);

        var mdFromRepository = masterDataFor1pRepository.findByIds(List.of(new ShopSkuKey(SUPPLIER_ID1, SHOP_SKU1)));
        Assertions.assertThat(mdFromRepository).hasSize(1);
        Assertions.assertThatThrownBy(() -> masterDataFor1pRepository.insert(masterData))
            .isInstanceOf(DuplicateKeyException.class);

        mdFromRepository = masterDataFor1pRepository.findByIds(List.of(new ShopSkuKey(SUPPLIER_ID1, SHOP_SKU1)));
        Assertions.assertThat(mdFromRepository).hasSize(1);
    }

    private MasterDataDto buildMasterData() {
        var masterData = new MasterDataDto();
        masterData.setBoxCount(13);
        masterData.setDeliveryTime(150);
        masterData.setLifeTime(new TimeInUnits(321, TimeInUnits.TimeUnit.DAY));
        masterData.setBoxDimensionLengthInUm(10L);
        masterData.setBoxDimensionWidthInUm(10L);
        masterData.setBoxDimensionHeightInUm(10L);
        masterData.setCustomsCommodityCode("custom code");
        masterData.setDangerousGood(true);
        masterData.setGtins(List.of("gtin1", "gtin2"));
        masterData.setGuaranteePeriod(new TimeInUnits(150, TimeInUnits.TimeUnit.DAY));
        masterData.setDatacampMasterDataVersion(1L);
        masterData.setLifeTimeComment("lifetime comment");
        masterData.setHeavyGood(true);
        masterData.setUseInMercury(true);
        masterData.setGrossWeight(1000L);
        masterData.setVatId(VatRate.NO_VAT.getId());
        masterData.setNetWeight(1500L);
        masterData.setVetisGuids(List.of("vetis1", "vetis2"));
        masterData.setQualityDocumentsNumbers(List.of("AB-15"));
        return masterData;
    }
}
