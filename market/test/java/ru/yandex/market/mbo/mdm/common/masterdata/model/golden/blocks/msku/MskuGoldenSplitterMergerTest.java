package ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmModificationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class MskuGoldenSplitterMergerTest extends MdmBaseDbTestClass {
    private static final long SEED = 4381L;
    private EnhancedRandom random;
    private MskuGoldenSplitterMerger splitterMerger;
    private MdmModificationInfo specificModificationInfo;

    @Autowired
    private MdmParamCache mdmParamCache;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(SEED);
        splitterMerger = new MskuGoldenSplitterMerger(mdmParamCache);
        specificModificationInfo = random.nextObject(MdmModificationInfo.class)
            .setMasterDataSourceId("")
            .setMasterDataSourceType(MasterDataSourceType.MDM_DEFAULT);
    }

    @Test
    public void whenNoParamsThenNoBlocks() {
        CommonMsku gold = new CommonMsku(new ModelKey(0L, 0L), List.of());
        Set<? extends ItemBlock> blocks = splitterMerger.splitIntoBlocks(gold);
        assertThat(blocks).isEmpty();

        CommonMsku newGold = splitterMerger.joinFromBlocks(new ModelKey(0L, 0L), blocks);
        assertThat(newGold.getValues()).isEmpty();
        assertThat(newGold).isEqualTo(gold);
    }

    @Test
    public void whenParamsExistShouldSplitJoinToSameGold() {
        CommonMsku gold = new CommonMsku(new ModelKey(0L, 0L), List.of(
            nextNonNegativeNumericValue(KnownMdmParams.SHELF_LIFE),
            nextUnitValue(KnownMdmParams.SHELF_LIFE_UNIT),
            nextStringValue(KnownMdmParams.SHELF_LIFE_COMMENT),

            nextNonNegativeStrNumericValue(KnownMdmParams.LIFE_TIME),
            nextUnitValue(KnownMdmParams.LIFE_TIME_UNIT),
            nextStringValue(KnownMdmParams.LIFE_TIME_COMMENT),

            nextNonNegativeStrNumericValue(KnownMdmParams.GUARANTEE_PERIOD),
            nextUnitValue(KnownMdmParams.GUARANTEE_PERIOD_UNIT),
            nextStringValue(KnownMdmParams.GUARANTEE_PERIOD_COMMENT),

            nextVatValue(KnownMdmParams.VAT),
            nextStringValue(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID),
            nextBoolValue(KnownMdmParams.EXPIR_DATE),
            nextBoolValue(KnownMdmParams.HIDE_SHELF_LIFE),
            nextBoolValue(KnownMdmParams.HIDE_LIFE_TIME),
            nextBoolValue(KnownMdmParams.HIDE_GUARANTEE_PERIOD),
            nextNonNegativeNumericValue(KnownMdmParams.LENGTH),
            nextNonNegativeNumericValue(KnownMdmParams.WIDTH),
            nextNonNegativeNumericValue(KnownMdmParams.HEIGHT),
            nextNonNegativeNumericValue(KnownMdmParams.WEIGHT_GROSS),

            (MskuParamValue) nextBoolValue(KnownMdmParams.HAS_MEASUREMENT_BEFORE_INHERIT).setBool(true),
            nextNonNegativeNumericValue(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_BEFORE_INHERIT)
        ));

        Set<? extends ItemBlock> blocks = splitterMerger.splitIntoBlocks(gold);
        // value+unit+comment - один блок, length + width + height + weight_gross - один блок
        assertThat(blocks).hasSize(11);

        CommonMsku newGold = splitterMerger.joinFromBlocks(new ModelKey(0L, 0L), blocks);
        assertThat(newGold).isEqualTo(gold);
    }

    @Test
    public void shouldSetModificationInfoForSelfLifeLifeTimeGuaranteePeriod() {
        MdmModificationInfo expectedModificationInfo = new MdmModificationInfo()
            .setUpdatedTs(Instant.now())
            .setSourceUpdatedTs(Instant.now())
            .setUpdatedByUid(13)
            .setUpdatedByLogin("operator1")
            .setMasterDataSourceId("1849")
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        List<MskuParamValue> expectedParams = List.of(
            nextNonNegativeNumericValue(KnownMdmParams.SHELF_LIFE),
            nextUnitValue(KnownMdmParams.SHELF_LIFE_UNIT),
            nextStringValue(KnownMdmParams.SHELF_LIFE_COMMENT),

            nextNonNegativeStrNumericValue(KnownMdmParams.LIFE_TIME),
            nextUnitValue(KnownMdmParams.LIFE_TIME_UNIT),
            nextStringValue(KnownMdmParams.LIFE_TIME_COMMENT),

            nextNonNegativeStrNumericValue(KnownMdmParams.GUARANTEE_PERIOD),
            nextUnitValue(KnownMdmParams.GUARANTEE_PERIOD_UNIT),
            nextStringValue(KnownMdmParams.GUARANTEE_PERIOD_COMMENT)
        );

        for(MskuParamValue param: expectedParams) {
            param.setModificationInfo(expectedModificationInfo);
        }

        CommonMsku gold = new CommonMsku(new ModelKey(0L, 0L), expectedParams);
        Set<? extends ItemBlock> blocks = splitterMerger.splitIntoBlocks(gold);
        assertThat(blocks).hasSize(3);

        CommonMsku newGold = splitterMerger.joinFromBlocks(new ModelKey(0L, 0L), blocks);
        assertThat(newGold).isEqualTo(gold);

        for(MskuParamValue param: newGold.getParamValues().values()) {
            MdmModificationInfo actualModificationInfo = param.getModificationInfo();
            assertThat(actualModificationInfo).isEqualTo(expectedModificationInfo);
        }
    }

    private MskuParamValue nextValue(long mdmParamId) {
        MskuParamValue value = random.nextObject(MskuParamValue.class);
        value.setMskuId(0L);
        value.setMdmParamId(mdmParamId);
        value.setXslName(mdmParamCache.get(mdmParamId).getXslName());
        value.setProcessed(false);
        // Обрежем мультизначения
        value.setOption(value.getOption().get());
        value.setString(value.getString().get());
        value.setNumeric(BigDecimal.valueOf(value.getNumeric().get().multiply(new BigDecimal("10")).intValue()));
        value.setBool(value.getBool().get());

        value.setModificationInfo(specificModificationInfo);
        return value;
    }

    private MskuParamValue nextUnitValue(long mdmParamId) {
        MskuParamValue value = nextValue(mdmParamId);
        value.setOption(new MdmParamOption().setId(1 + random.nextInt(5)));
        value.setStrings(List.of());
        value.setNumerics(List.of());
        value.setBools(List.of());
        return value;
    }

    private MskuParamValue nextVatValue(long mdmParamId) {
        MskuParamValue value = nextValue(mdmParamId);
        value.setOption(new MdmParamOption().setId(1 + random.nextInt(3)));
        value.setStrings(List.of());
        value.setNumerics(List.of());
        value.setBools(List.of());
        return value;
    }

    private MskuParamValue nextNumericValue(long mdmParamId) {
        MskuParamValue value = nextValue(mdmParamId);
        value.setBools(List.of());
        value.setStrings(List.of());
        value.setOptions(List.of());
        return value;
    }

    private MskuParamValue nextStringValue(long mdmParamId) {
        MskuParamValue value = nextValue(mdmParamId);
        value.setBools(List.of());
        value.setOptions(List.of());
        value.setNumerics(List.of());
        return value;
    }

    private MskuParamValue nextStrNumericValue(long mdmParamId) {
        MskuParamValue value = nextValue(mdmParamId);
        value.setBools(List.of());
        value.setOptions(List.of());
        value.setNumerics(List.of());
        value.setString(String.valueOf(random.nextInt()));
        return value;
    }

    private MskuParamValue nextBoolValue(long mdmParamId) {
        MskuParamValue value = nextValue(mdmParamId);
        value.setOptions(List.of());
        value.setNumerics(List.of());
        value.setStrings(List.of());
        return value;
    }

    private MskuParamValue nextNonNegativeNumericValue(long mdmParamId) {
        MskuParamValue value = nextNumericValue(mdmParamId);
        value.getNumeric()
            .map(BigDecimal::abs)
            .ifPresent(value::setNumeric);
        return value;
    }

    private MskuParamValue nextNonNegativeStrNumericValue(long mdmParamId) {
        MskuParamValue value = nextStrNumericValue(mdmParamId);
        value.getString()
            .map(Integer::parseInt)
            .map(Math::abs)
            .map(String::valueOf)
            .ifPresent(value::setString);
        return value;
    }
}
