package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MeasurementExistenceBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ShelfLifeBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;

public class MskuGoldenItemServiceHandleBlocksTest extends MdmBaseDbTestClass {

    @Autowired
    private MdmParamCache mdmParamCache;

    private MskuGoldenItemService service;

    @Before
    public void setup() {
        service = new MskuGoldenItemService(null, null, null, null, null, null, null, mdmParamCache);
    }

    @Test
    public void testMeasurementExistenceOnTrueBlocksPicksLatest() {
        List<ItemBlock> blocks = List.of(
            meBlock(true, 100500), // <----
            meBlock(false, 200600),
            meBlock(true, 1999),
            meBlock(false, 300700)
        );
        var maybeResult = service.handleBlocks(
            ItemBlock.BlockType.MEASUREMENT_EXISTENCE,
            0L,
            blocks,
            null,
            GoldComputationContext.EMPTY_CONTEXT
        );

        Assertions.assertThat(maybeResult).hasValue(meBlock(true, 100500));
    }

    @Test
    public void testExistingMeasurementExistenceBlockIsIgnored() {
        List<ItemBlock> blocks = List.of(
            meBlock(true, 100500), // <----
            meBlock(false, 200600),
            meBlock(true, 1999),
            meBlock(false, 300700)
        );
        var maybeResult = service.handleBlocks(
            ItemBlock.BlockType.MEASUREMENT_EXISTENCE,
            0L,
            blocks,
            meBlock(true, 90000000), // ignored
            GoldComputationContext.EMPTY_CONTEXT
        );

        Assertions.assertThat(maybeResult).hasValue(meBlock(true, 100500));
    }

    @Test
    public void testScrewedUpMetaTsDoesNotAffectRealParamedTsInExistenceBlock() {
        // Может так случиться, что из-за перекладываний парамов где-то профакапится UpdatedTs в метаинфе о параме.
        // Тем не менее, это не должно будет повлиять на алгоритм, т.к. работа должна вестись по реальному отдельно
        // хранимому таймштампу в виде отдельного параметра.
        List<ItemBlock> blocks = List.of(
            meBlock(true, 100500), // <----
            meBlock(false, 200600),
            meBlock(true, 1999, Instant.ofEpochMilli(1000000000)), // он не победит
            meBlock(false, 300700)
        );

        var maybeResult = service.handleBlocks(
            ItemBlock.BlockType.MEASUREMENT_EXISTENCE,
            0L,
            blocks,
            null,
            GoldComputationContext.EMPTY_CONTEXT
        );

        Assertions.assertThat(maybeResult).hasValue(meBlock(true, 100500));
    }

    /**
     * Данный кейс носит дискуссионный характер, возможно, мы не хотим такое поведение.
     */
    @Test
    public void testShelfLifePicksIncompleteBlockByTs() {
        List<ItemBlock> blocks = List.of(
            slBlock(null, "Самый поздний", 100500L),
            slBlock(12, "хранить как угодно", 600L),
            slBlock(14, "", 700L)
        );
        var maybeResult = service.handleBlocks(
            ItemBlock.BlockType.SHELF_LIFE,
            0L,
            blocks,
            null,
            GoldComputationContext.EMPTY_CONTEXT
        );

        Assertions.assertThat(maybeResult).hasValue(slBlock(null, "Самый поздний", 100500L));
    }

    private ItemBlock slBlock(@Nullable Integer days, @Nullable String comment, long millis) {
        ShelfLifeBlock block = new ShelfLifeBlock(
            mdmParamCache.get(KnownMdmParams.SHELF_LIFE),
            mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT),
            mdmParamCache.get(KnownMdmParams.SHELF_LIFE_COMMENT)
        );
        return block.fromSskuMasterData(
            days == null ? null : new TimeInUnits(days, TimeInUnits.TimeUnit.DAY),
            comment,
            Instant.ofEpochMilli(millis)
        );
    }

    private ItemBlock meBlock(boolean flag, long millis) {
        Instant ts = Instant.ofEpochMilli(millis);
        var flagParam = mdmParamCache.get(KnownMdmParams.HAS_MEASUREMENT_BEFORE_INHERIT);
        var tsParam = mdmParamCache.get(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_BEFORE_INHERIT);

        MdmParamValue flagValue = new MdmParamValue();
        flagValue.setMasterDataSource(MasterDataSource.DEFAULT_AUTO_SOURCE);
        flagValue.setBool(flag);
        flagValue.setMdmParamId(flagParam.getId());
        flagValue.setXslName(flagParam.getXslName());
        flagValue.setUpdatedTs(ts);
        flagValue.setSourceUpdatedTs(ts);

        MdmParamValue tsValue = new MdmParamValue();
        tsValue.setMasterDataSource(MasterDataSource.DEFAULT_AUTO_SOURCE);
        tsValue.setNumeric(BigDecimal.valueOf(millis));
        tsValue.setMdmParamId(tsParam.getId());
        tsValue.setXslName(tsParam.getXslName());
        tsValue.setUpdatedTs(ts);
        tsValue.setSourceUpdatedTs(ts);
        return new MeasurementExistenceBlock(flagValue, tsValue);
    }

    private ItemBlock meBlock(boolean flag, long millis, Instant fakeTs) {
        var flagParam = mdmParamCache.get(KnownMdmParams.HAS_MEASUREMENT_BEFORE_INHERIT);
        var tsParam = mdmParamCache.get(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_BEFORE_INHERIT);

        MdmParamValue flagValue = new MdmParamValue();
        flagValue.setMasterDataSource(MasterDataSource.DEFAULT_AUTO_SOURCE);
        flagValue.setBool(flag);
        flagValue.setMdmParamId(flagParam.getId());
        flagValue.setXslName(flagParam.getXslName());
        flagValue.setUpdatedTs(fakeTs);
        flagValue.setSourceUpdatedTs(fakeTs);

        MdmParamValue tsValue = new MdmParamValue();
        tsValue.setMasterDataSource(MasterDataSource.DEFAULT_AUTO_SOURCE);
        tsValue.setNumeric(BigDecimal.valueOf(millis));
        tsValue.setMdmParamId(tsParam.getId());
        tsValue.setXslName(tsParam.getXslName());
        tsValue.setUpdatedTs(fakeTs);
        tsValue.setSourceUpdatedTs(fakeTs);
        return new MeasurementExistenceBlock(flagValue, tsValue);
    }
}
