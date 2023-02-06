package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuCalculatingProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuCalculatingProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingData;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.utils.Multiwatch;

/**
 * @author dmserebr
 * @date 02/04/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class RecomputeMskuGoldServiceSaveParamsTest {
    private static final long MSKU_ID = 1L;
    private static final ModelKey MODEL_KEY = new ModelKey(1L, MSKU_ID);
    private static final Instant TS_1 = TimestampUtil.toInstant(
        LocalDateTime.of(2020, 1, 1, 0, 0, 0));
    private static final Instant TS_2 = TimestampUtil.toInstant(
        LocalDateTime.of(2020, 1, 2, 0, 0, 0));

    private MskuCalculatingProcessor mskuCalculatingProcessor;
    private MskuRepositoryMock mskuRepository;

    @Before
    public void setup() {

        mskuRepository = new MskuRepositoryMock();
        mskuCalculatingProcessor = new MskuCalculatingProcessorImpl(
            mskuRepository,
            null,
            null);
    }


    @Test
    public void testAddValuesIfNotExist() {
        var pv1 = createNumericParamValue(1L, 10.0, TS_1);
        var pv2 = createBoolParamValue(2L, true, TS_1);
        var pv3 = createStringParamValue(3L, "test", TS_1);
        var pv4 = createEnumParamValue(4L, new MdmParamOption().setId(10), TS_1);


        Map<ModelKey, CommonMsku> newGold =
            Map.of(MODEL_KEY, new CommonMsku(MODEL_KEY, List.of(pv1, pv2, pv3, pv4)));

        mskuCalculatingProcessor.saveGoldIfPossible(newGold, MskuProcessingData.EMPTY_DATA,
            new Multiwatch("kek"));

        Assertions.assertThat(mskuRepository.findAll()).containsExactlyInAnyOrder(pv1, pv2, pv3, pv4);
    }

    @Test
    public void testUpdateValuesIfExistAndDifferent() {
        var pv1 = createNumericParamValue(1L, 10.0, TS_1);
        var pv2 = createBoolParamValue(2L, true, TS_1);
        var pv3 = createStringParamValue(3L, "test", TS_1);
        var pv4 = createEnumParamValue(4L, new MdmParamOption().setId(10), TS_1);
        List<MskuParamValue> existingParamValues = List.of(pv1, pv2, pv3, pv4);
        mskuRepository.insertBatch(existingParamValues);

        var pv1new = createNumericParamValue(1L, 15.0, TS_2);
        var pv2new = createBoolParamValue(2L, false, TS_2);
        var pv3new = createStringParamValue(3L, "test2", TS_2);
        var pv4new = createEnumParamValue(4L, new MdmParamOption().setId(20), TS_2);

        Map<ModelKey, CommonMsku> newGold =
            Map.of(MODEL_KEY, new CommonMsku(MODEL_KEY, List.of(pv1new, pv2new, pv3new, pv4new)));

        mskuCalculatingProcessor.saveGoldIfPossible(newGold, MskuProcessingData.EMPTY_DATA,
            new Multiwatch("kek"));

        Assertions.assertThat(mskuRepository.findAll())
            .containsExactlyInAnyOrder(pv1new, pv2new, pv3new, pv4new);
    }

    @Test
    public void testUpdateSameValuesOnlyIfDifferentModificationSoruce() {
        var pv1 = createNumericParamValue(1L, 10.0, TS_1);
        var pv2 = createNumericParamValue(2L, 20.0, TS_1);
        List<MskuParamValue> existingParamValues = List.of(pv1, pv2);
        mskuRepository.insertBatch(existingParamValues);

        var pv1new = createNumericParamValue(1L, 10.0, TS_2);
        var pv2new = createNumericParamValue(2L, 20.0, TS_2);
        pv2new.setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR);
        Map<ModelKey, CommonMsku> newGold = Map.of(MODEL_KEY, new CommonMsku(MODEL_KEY, List.of(pv1new, pv2new)));
        mskuCalculatingProcessor.saveGoldIfPossible(newGold,
            MskuProcessingData.EMPTY_DATA, new Multiwatch("kek"));

        Assertions.assertThat(mskuRepository.findAll()).containsExactlyInAnyOrder(pv1, pv2new);
    }

    @Test
    public void testDeleteInactualValues() {
        var pv1 = createNumericParamValue(1L, 10.0, TS_1);
        var pv2 = createNumericParamValue(2L, 20.0, TS_1);
        List<MskuParamValue> existingParamValues = List.of(pv1, pv2);
        mskuRepository.insertBatch(existingParamValues);

        var pv1new = createNumericParamValue(1L, 11.0, TS_2);

        Map<ModelKey, CommonMsku> newGold = Map.of(MODEL_KEY, new CommonMsku(MODEL_KEY, List.of(pv1new, pv1new)));
        mskuCalculatingProcessor.saveGoldIfPossible(newGold, MskuProcessingData.EMPTY_DATA,
            new Multiwatch("kek"));

        Assertions.assertThat(mskuRepository.findAll()).containsExactlyInAnyOrder(pv1new);
    }

    private MskuParamValue createNumericParamValue(long mdmParamId, double number, Instant ts) {
        return TestMdmParamUtils.createMskuParamValue(
            mdmParamId, MSKU_ID, null, number, null, null, MasterDataSourceType.AUTO, ts);
    }

    private MskuParamValue createBoolParamValue(long mdmParamId, boolean boolValue, Instant ts) {
        return TestMdmParamUtils.createMskuParamValue(
            mdmParamId, MSKU_ID, boolValue, null, null, null, MasterDataSourceType.AUTO, ts);
    }

    private MskuParamValue createStringParamValue(long mdmParamId, String str, Instant ts) {
        return TestMdmParamUtils.createMskuParamValue(
            mdmParamId, MSKU_ID, null, null, str, null, MasterDataSourceType.AUTO, ts);
    }

    private MskuParamValue createEnumParamValue(long mdmParamId, MdmParamOption option, Instant ts) {
        return TestMdmParamUtils.createMskuParamValue(
            mdmParamId, MSKU_ID, null, null, null, option, MasterDataSourceType.AUTO, ts);
    }
}
