package ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.DimensionsBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class MskuSilverSplitterTest extends MdmBaseDbTestClass {

    @Autowired
    private MdmParamCache mdmParamCache;

    private MskuSilverSplitter mskuSilverSplitter;
    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        mskuSilverSplitter = new MskuSilverSplitter(mdmParamCache, Mockito.mock(SskuGoldenParamUtil.class));
        random = TestDataUtils.defaultRandom(9428567);
    }

    @Test
    public void testSplitIntoBlocksWithGoldenParamValues() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(12, "winterfir");
        SskuGoldenParamValue sskuGoldenParamValue1 = generateSskuGoldenParam();
        sskuGoldenParamValue1.setMasterDataSourceType(MasterDataSourceType.AUTO);
        sskuGoldenParamValue1.setMdmParamId(KnownMdmParams.SSKU_LENGTH);
        SskuGoldenParamValue sskuGoldenParamValue2 = generateSskuGoldenParam();
        sskuGoldenParamValue2.setMasterDataSourceType(MasterDataSourceType.AUTO);
        sskuGoldenParamValue2.setMdmParamId(KnownMdmParams.SSKU_WIDTH);
        SskuGoldenParamValue sskuGoldenParamValue3 = generateSskuGoldenParam();
        sskuGoldenParamValue3.setMasterDataSourceType(MasterDataSourceType.AUTO);
        sskuGoldenParamValue3.setMdmParamId(KnownMdmParams.SSKU_HEIGHT);
        SskuGoldenParamValue sskuGoldenParamValue4 = generateSskuGoldenParam();
        sskuGoldenParamValue4.setMasterDataSourceType(MasterDataSourceType.AUTO);
        sskuGoldenParamValue4.setMdmParamId(KnownMdmParams.SSKU_WEIGHT_GROSS);

        var mskuSilverItem = new CommonSsku(shopSkuKey);
        Stream.of(sskuGoldenParamValue1, sskuGoldenParamValue2, sskuGoldenParamValue3, sskuGoldenParamValue4)
            .forEach(mskuSilverItem::addBaseValue);
        List<DimensionsBlock> blocks =
            new ArrayList<>(((Set<DimensionsBlock>) mskuSilverSplitter.splitIntoBlocks(mskuSilverItem)));

        Assertions.assertThat(blocks).hasSize(1);
        Assertions.assertThat(blocks.get(0).getLength()).isCloseTo(sskuGoldenParamValue1.getNumeric().get(),
            Percentage.withPercentage(1));
        Assertions.assertThat(blocks.get(0).getWeightGross()).isCloseTo(sskuGoldenParamValue4.getNumeric().get(),
            Percentage.withPercentage(1));
        Assertions.assertThat(blocks.get(0).getHeight()).isCloseTo(sskuGoldenParamValue3.getNumeric().get(),
            Percentage.withPercentage(1));
        Assertions.assertThat(blocks.get(0).getWidth()).isCloseTo(sskuGoldenParamValue2.getNumeric().get(),
            Percentage.withPercentage(1));
    }

    @Test
    public void testMeasurementExistenceBlockSplitOnTrue() {
        var ts = Instant.now();
        long millis = ts.toEpochMilli();

        var ssku = new CommonSsku(random.nextObject(ShopSkuKey.class)).setBaseValues(List.of(
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.HAS_MEASUREMENT_BEFORE_INHERIT)
                .setXslName(mdmParamCache.get(KnownMdmParams.HAS_MEASUREMENT_BEFORE_INHERIT).getXslName())
                .setBool(true),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_BEFORE_INHERIT)
                .setXslName(mdmParamCache.get(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_BEFORE_INHERIT).getXslName())
                .setNumeric(BigDecimal.valueOf(millis))
        ));

        Set<? extends ItemBlock> blocks = mskuSilverSplitter.splitIntoBlocks(ssku);

        Assertions.assertThat(blocks).hasSize(1);
        ItemBlock block = blocks.iterator().next();

        Assertions.assertThat(block.getBlockType()).isEqualTo(ItemBlock.BlockType.MEASUREMENT_EXISTENCE);
        List<MdmParamValue> values = block.getMdmParamValues();

        Assertions.assertThat(values).hasSize(2);

        MdmParamValue flagValue = values.stream()
            .filter(v -> v.getMdmParamId() == KnownMdmParams.HAS_MEASUREMENT_BEFORE_INHERIT)
            .findAny()
            .orElseThrow();
        MdmParamValue tsValue = values.stream()
            .filter(v -> v.getMdmParamId() == KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_BEFORE_INHERIT)
            .findAny()
            .orElseThrow();
        Assertions.assertThat(flagValue.getBool()).hasValue(true);
        Assertions.assertThat(tsValue.getNumeric()).hasValue(BigDecimal.valueOf(millis));
    }

    @Test
    public void testMeasurementExistenceBlockIgnoredOnFalse() {
        var ts = Instant.now();
        long millis = ts.toEpochMilli();

        var ssku = new CommonSsku(random.nextObject(ShopSkuKey.class)).setBaseValues(List.of(
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.HAS_MEASUREMENT_BEFORE_INHERIT)
                .setXslName(mdmParamCache.get(KnownMdmParams.HAS_MEASUREMENT_BEFORE_INHERIT).getXslName())
                .setBool(false),
            new MdmParamValue()
                .setMdmParamId(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_BEFORE_INHERIT)
                .setXslName(mdmParamCache.get(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_BEFORE_INHERIT).getXslName())
                .setNumeric(BigDecimal.valueOf(millis))
        ));
        Set<? extends ItemBlock> blocks = mskuSilverSplitter.splitIntoBlocks(ssku);
        Assertions.assertThat(blocks).hasSize(0);
    }

    @Test
    public void testMeasurementExistenceBlockIgnoredOnNoData() {
        var ssku = new CommonSsku(random.nextObject(ShopSkuKey.class));
        Set<? extends ItemBlock> blocks = mskuSilverSplitter.splitIntoBlocks(ssku);
        Assertions.assertThat(blocks).hasSize(0);
    }

    private SskuGoldenParamValue generateSskuGoldenParam() {
        return random.nextObject(SskuGoldenParamValue.class);
    }

}
