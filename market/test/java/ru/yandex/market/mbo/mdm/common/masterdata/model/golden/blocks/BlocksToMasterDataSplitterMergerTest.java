package ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.model.TransportUnit;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class BlocksToMasterDataSplitterMergerTest extends MdmBaseDbTestClass {
    private static final ShopSkuKey SSKU_KEY = new ShopSkuKey(45, "aloha");

    @Autowired
    private MdmParamCache mdmParamCache;

    private MasterDataIntoBlocksSplitter splitter;
    private BlocksToMasterDataMerger merger;

    @Before
    public void setUp() throws Exception {
        merger = new BlocksToMasterDataMergerImpl();
        splitter = new MasterDataIntoBlocksSplitterImpl(mdmParamCache);
    }


    @Test
    public void testSplitThanMergeEmptyMasterData() {
        MasterData emptyMasterData = new MasterData();
        emptyMasterData.setShopSkuKey(SSKU_KEY);

        List<ItemBlock> blocks = splitter.splitIntoBlocks(emptyMasterData);

        MasterData mergedMasterData = merger.mergeFromBlocks(SSKU_KEY, blocks);

        Assertions.assertThat(mergedMasterData).isEqualTo(emptyMasterData);
    }

    @Test
    public void testAllSupportedValuesSplitThanMerge() {
        MasterData existingMasterData = new MasterData()
            .setShopSkuKey(SSKU_KEY)
            .setShelfLife(TimeInUnits.UNLIMITED)
            .setShelfLifeComment("Это комментарий к сроку годности")
            .setLifeTime(new TimeInUnits(10, TimeInUnits.TimeUnit.YEAR))
            .setLifeTimeComment("Это комментарий к сроку службы")
            .setGuaranteePeriod(new TimeInUnits(3, TimeInUnits.TimeUnit.YEAR))
            .setGuaranteePeriodComment("Это комментарий к гарантийному сроку")
            .setCustomsCommodityCode("0123456789")
            .setVat(VatRate.VAT_20)
            .setTransportUnit(new TransportUnit(1, 10))
            .setBoxCount(10)
            .setDeliveryTime(7)
            .setMinShipment(10)
            .setGtins(List.of("these", "are", "gtins"))
            .setQuantumOfSupply(10)
            .setVetisGuids(List.of("these", "are", "vetis", "guids"))
            .setManufacturerCountries(List.of("Russia", "USA", "DPRK"))
            .setRegNumbers(List.of("these", "are", "reg", "numbers", "(net)"))
            .setManufacturer("МИТ")
            .setDatacampMasterDataVersion(1L)
            .setTraceable(true)
            .setUseInMercury(true);

        List<ItemBlock> blocks = splitter.splitIntoBlocks(existingMasterData);

        MasterData mergedMasterData = merger.mergeFromBlocks(SSKU_KEY, blocks);

        Assertions.assertThat(mergedMasterData).isEqualTo(existingMasterData);
    }

    @Test
    public void testPartialTransportUnitSplitAndMerge() {
        MasterData existingMasterData = new MasterData();

        existingMasterData.setShopSkuKey(SSKU_KEY)
            .setTransportUnit(new TransportUnit(10, 0));

        List<ItemBlock> blocks = splitter.splitIntoBlocks(existingMasterData);

        MasterData mergedMasterData = merger.mergeFromBlocks(SSKU_KEY, blocks);

        Assertions.assertThat(mergedMasterData).isEqualTo(existingMasterData);
    }
}
