package ru.yandex.market.mbo.mdm.common.masterdata.services.ssku;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSilverItem;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MasterDataIntoBlocksSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MasterDataIntoBlocksSplitterImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class TraceableSskuSilverSplitterTest {
    private TraceableSskuSilverSplitter splitter;
    private MdmParamCache paramCache;
    private Random random;

    @Before
    public void setUp() throws Exception {
        random = new Random("Ave @dmserebr".hashCode());
        paramCache = TestMdmParamUtils.createParamCacheMock();
        MasterDataIntoBlocksSplitter masterDataIntoBlocksSplitter = new MasterDataIntoBlocksSplitterImpl(paramCache);
        splitter = new TraceableSskuSilverSplitter(paramCache, masterDataIntoBlocksSplitter);
    }

    @Test
    public void whenSplitNullReturnEmptySet() {
        Assertions.assertThat(splitter.splitIntoBlocks(null)).isEmpty();
    }

    @Test
    public void whenSplitUnsupportedSilverThrowException() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> splitter.splitIntoBlocks(GoldComputationContext.EMPTY_CONTEXT))
            .withMessageStartingWith("Got silver item of unsupported type");
    }

    @Test
    public void testSplittingCommonMsku() {
        // given
        long mskuId = 123;
        MskuParamValue traceableParamValue = new MskuParamValue().setMskuId(mskuId);
        TestMdmParamUtils.createRandomMdmParamValue(random, paramCache.get(KnownMdmParams.IS_TRACEABLE))
            .setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT)
            .copyTo(traceableParamValue);
        MskuParamValue ignoredParamValue = new MskuParamValue().setMskuId(mskuId);
        TestMdmParamUtils.createRandomMdmParamValue(random, paramCache.get(KnownMdmParams.EXPIR_DATE))
            .setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT)
            .copyTo(ignoredParamValue);
        CommonMsku msku = new CommonMsku(mskuId, List.of(traceableParamValue, ignoredParamValue));

        // when
        Set<? extends ItemBlock> blocks = splitter.splitIntoBlocks(msku);

        // then
        Assertions.assertThat(blocks).hasSize(1);
        Assertions.assertThat(blocks)
            .flatMap(ItemBlock::getMdmParamValues)
            .containsExactlyInAnyOrder(traceableParamValue);
    }

    @Test
    public void whenNotMskuInheritSourceTypeOnMskuThrowException() {
        // given
        long mskuId = 123;
        MskuParamValue traceableParamValue = new MskuParamValue().setMskuId(mskuId);
        TestMdmParamUtils.createRandomMdmParamValue(random, paramCache.get(KnownMdmParams.IS_TRACEABLE))
            .setMasterDataSourceType(MasterDataSourceType.AUTO)
            .copyTo(traceableParamValue);
        CommonMsku msku = new CommonMsku(mskuId, List.of(traceableParamValue));

        // then
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> splitter.splitIntoBlocks(msku))
            .withMessageStartingWith("Msku must have all params with source MSKU_INHERIT, but got AUTO.");
    }

    @Test
    public void testSplittingSilver() {
        // given
        SilverSskuKey silverKey = new SilverSskuKey(1, "2", MasterDataSourceType.MDM_OPERATOR, "213");
        SilverServiceSsku silver = (SilverServiceSsku) new SilverServiceSsku(silverKey)
            .addParamValue(
                TestMdmParamUtils.createRandomMdmParamValue(random, paramCache.get(KnownMdmParams.IS_TRACEABLE)))
            .addParamValue(
                TestMdmParamUtils.createRandomMdmParamValue(random, paramCache.get(KnownMdmParams.EXPIR_DATE)));

        // when
        Set<? extends ItemBlock> blocks = splitter.splitIntoBlocks(silver);

        // then
        Assertions.assertThat(blocks).hasSize(1);
        Assertions.assertThat(blocks)
            .flatMap(ItemBlock::getMdmParamValues)
            .containsExactlyInAnyOrder(silver.getParamValue(KnownMdmParams.IS_TRACEABLE).orElseThrow());
    }

    @Test
    public void testSplittingMasterData() {
        // given
        Instant updatedTs = Instant.parse("2007-12-03T10:15:30.00Z");
        final boolean traceable = true;
        MasterData masterData = new MasterData()
            .setShopSkuKey(new ShopSkuKey(123, "456"))
            .setManufacturer("ignored")
            .setTraceable(traceable)
            .setModifiedTimestamp(TimestampUtil.toLocalDateTime(updatedTs));

        // when
        Set<? extends ItemBlock> blocks = splitter.splitIntoBlocks(new MasterDataSilverItem(masterData));

        // then
        Assertions.assertThat(blocks).hasSize(1);
        Assertions.assertThat(blocks)
            .flatMap(ItemBlock::getMdmParamValues)
            .containsExactly(
                new MdmParamValue()
                    .setMdmParamId(KnownMdmParams.IS_TRACEABLE)
                    .setXslName(paramCache.get(KnownMdmParams.IS_TRACEABLE).getXslName())
                    .setBool(traceable)
                    .setUpdatedTs(updatedTs)
                    .setMasterDataSourceType(MasterDataSourceType.MDM_DEFAULT)
                    .setMasterDataSourceId("")
            );
    }
}
