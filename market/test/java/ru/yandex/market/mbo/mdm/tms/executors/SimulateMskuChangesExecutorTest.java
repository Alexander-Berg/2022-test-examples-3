package ru.yandex.market.mbo.mdm.tms.executors;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToMboQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.editor.MdmSampleDataService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

@SuppressWarnings("checkstyle:MagicNumber")
public class SimulateMskuChangesExecutorTest extends MdmBaseDbTestClass {
    private SimulateMskuChangesExecutor executor;

    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private MskuToMboQueueRepository mskuToMboQueueRepository;
    @Autowired
    private MdmParamCache mdmParamCache;
    private StorageKeyValueServiceMock skv;

    @Before
    public void setup() {
        MdmSampleDataService mdmSampleDataService = new MdmSampleDataService() {
            @Override
            public List<ShopSkuKey> getSampleSskuKeys(@Nullable Integer limit, @Nullable Long seed) {
                return null;
            }

            @Override
            public List<Long> getSampleMskuIds(@Nullable Integer limit, @Nullable Long seed) {
                return mskuRepository.findAllMskus().values().stream()
                    .map(CommonMsku::getMskuId)
                    .collect(Collectors.toList());
            }
        };

        skv = new StorageKeyValueServiceMock();
        executor = new SimulateMskuChangesExecutor(
            mdmSampleDataService,
            mskuRepository,
            mskuToMboQueueRepository,
            skv,
            mdmParamCache
        );
    }

    @Test
    public void testNotRunIfDisabled() {
        var msku1 = msku(
            1,
            value(1, KnownMdmParams.EXPIR_DATE, false),
            value(1, KnownMdmParams.WIDTH, 10),
            value(1, KnownMdmParams.LENGTH, 20),
            value(1, KnownMdmParams.HEIGHT, 30),
            value(1, KnownMdmParams.WEIGHT_GROSS, 40),
            value(1, KnownMdmParams.SERIAL_NUMBER_CONTROL, false),
            value(1, KnownMdmParams.IMEI_CONTROL, true),
            value(1, KnownMdmParams.IMEI_MASK, "maaaask"),
            value(1, KnownMdmParams.SERIAL_NUMBER_MASK, "aaaaaaaaa")
        );
        var msku2 = msku(
            2,
            value(2, KnownMdmParams.EXPIR_DATE, true),
            value(2, KnownMdmParams.WIDTH, 4),
            value(2, KnownMdmParams.LENGTH, 3),
            value(2, KnownMdmParams.HEIGHT, 2),
            value(2, KnownMdmParams.WEIGHT_GROSS, 9),
            value(2, KnownMdmParams.SERIAL_NUMBER_CONTROL, true),
            value(2, KnownMdmParams.IMEI_CONTROL, true),
            value(2, KnownMdmParams.IMEI_MASK, "rrrrrrr"),
            value(2, KnownMdmParams.SERIAL_NUMBER_MASK, "ttttttt")
        );
        mskuRepository.insertOrUpdateMsku(msku1);
        mskuRepository.insertOrUpdateMsku(msku2);

        Assertions.assertThat(selectMsku(1)).isEqualTo(msku1);
        Assertions.assertThat(selectMsku(2)).isEqualTo(msku2);

        executor.execute();
        Assertions.assertThat(selectMsku(1)).isEqualTo(msku1);
        Assertions.assertThat(selectMsku(2)).isEqualTo(msku2);
    }

    @Test
    public void testMskuChanges() {
        skv.putValue(MdmProperties.MSKU_ACTIVITY_SIMULATION_ENABLED, true);
        var msku1 = msku(
            1,
            value(1, KnownMdmParams.EXPIR_DATE, false),
            value(1, KnownMdmParams.WIDTH, 10),
            value(1, KnownMdmParams.LENGTH, 20),
            value(1, KnownMdmParams.HEIGHT, 30),
            value(1, KnownMdmParams.WEIGHT_GROSS, 40),
            value(1, KnownMdmParams.SERIAL_NUMBER_CONTROL, false),
            value(1, KnownMdmParams.IMEI_CONTROL, true),
            value(1, KnownMdmParams.IMEI_MASK, "maaaask"),
            value(1, KnownMdmParams.SERIAL_NUMBER_MASK, "aaaaaaaaa")
        );
        var msku2 = msku(
            2,
            value(2, KnownMdmParams.EXPIR_DATE, true),
            value(2, KnownMdmParams.WIDTH, 4),
            value(2, KnownMdmParams.LENGTH, 3),
            value(2, KnownMdmParams.HEIGHT, 2),
            value(2, KnownMdmParams.WEIGHT_GROSS, 9),
            value(2, KnownMdmParams.SERIAL_NUMBER_CONTROL, true),
            value(2, KnownMdmParams.IMEI_CONTROL, true),
            value(2, KnownMdmParams.IMEI_MASK, "rrrrrrr"),
            value(2, KnownMdmParams.SERIAL_NUMBER_MASK, "ttttttt")
        );
        mskuRepository.insertOrUpdateMsku(msku1);
        mskuRepository.insertOrUpdateMsku(msku2);

        Assertions.assertThat(selectMsku(1)).isEqualTo(msku1);
        Assertions.assertThat(selectMsku(2)).isEqualTo(msku2);

        executor.execute();
        Assertions.assertThat(selectMsku(1)).isNotEqualTo(msku1);
        Assertions.assertThat(selectMsku(2)).isNotEqualTo(msku2);
        assertParameterValidity(selectMsku(1));
        assertParameterValidity(selectMsku(2));
    }

    @Test
    public void testParamSeedAutoUpdated() {
        skv.putValue(MdmProperties.MSKU_ACTIVITY_SIMULATION_ENABLED, true);
        Assertions.assertThat(skv.getLong(MdmProperties.MSKU_ACTIVITY_SIMULATION_PARAMS_SEED, null)).isNull();

        executor.execute();
        Long newSeed = skv.getLong(MdmProperties.MSKU_ACTIVITY_SIMULATION_PARAMS_SEED, null);
        Assertions.assertThat(newSeed).isNotNull();

        executor.execute();
        Long newestSeed = skv.getLong(MdmProperties.MSKU_ACTIVITY_SIMULATION_PARAMS_SEED, null);
        Assertions.assertThat(newestSeed).isNotNull();
        Assertions.assertThat(newestSeed).isNotEqualTo(newSeed);
    }

    @Test
    public void testMboQueueFilled() {
        skv.putValue(MdmProperties.MSKU_ACTIVITY_SIMULATION_ENABLED, true);
        var msku1 = msku(
            1,
            value(1, KnownMdmParams.EXPIR_DATE, false),
            value(1, KnownMdmParams.WIDTH, 10),
            value(1, KnownMdmParams.LENGTH, 20),
            value(1, KnownMdmParams.HEIGHT, 30),
            value(1, KnownMdmParams.WEIGHT_GROSS, 40),
            value(1, KnownMdmParams.SERIAL_NUMBER_CONTROL, false),
            value(1, KnownMdmParams.IMEI_CONTROL, true),
            value(1, KnownMdmParams.IMEI_MASK, "maaaask"),
            value(1, KnownMdmParams.SERIAL_NUMBER_MASK, "aaaaaaaaa")
        );
        var msku2 = msku(
            2,
            value(2, KnownMdmParams.EXPIR_DATE, true),
            value(2, KnownMdmParams.WIDTH, 4),
            value(2, KnownMdmParams.LENGTH, 3),
            value(2, KnownMdmParams.HEIGHT, 2),
            value(2, KnownMdmParams.WEIGHT_GROSS, 9),
            value(2, KnownMdmParams.SERIAL_NUMBER_CONTROL, true),
            value(2, KnownMdmParams.IMEI_CONTROL, true),
            value(2, KnownMdmParams.IMEI_MASK, "rrrrrrr"),
            value(2, KnownMdmParams.SERIAL_NUMBER_MASK, "ttttttt")
        );
        mskuRepository.insertOrUpdateMsku(msku1);
        mskuRepository.insertOrUpdateMsku(msku2);

        executor.execute();
        Assertions.assertThat(mskuToMboQueueRepository.findAll().stream())
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactlyInAnyOrder(1L, 2L);
    }

    private void assertParameterValidity(CommonMsku msku) {
        Set<MskuParamValue> values = TestBmdmUtils.removeBmdmIdAndVersion(msku).getValues();
        Assertions.assertThat(values).hasSize(9); // Число участвующих в тесте парамов. Оно не должно было измениться.
        for (MskuParamValue value : values) {
            Assertions.assertThat(value.getMskuId()).isGreaterThan(0L);
            Assertions.assertThat(value.getMdmParamId()).isGreaterThan(0L);
            Assertions.assertThat(value.getXslName()).isNotBlank();
            MdmParam param = mdmParamCache.get(value.getMdmParamId());
            switch (param.getValueType()) {
                case STRING:
                    Assertions.assertThat(value.getString().orElseThrow()).isNotBlank();
                    Assertions.assertThat(value.getNumeric()).isEmpty();
                    Assertions.assertThat(value.getOption()).isEmpty();
                    Assertions.assertThat(value.getBool()).isEmpty();
                    break;
                case MBO_NUMERIC_ENUM:
                case ENUM:
                case MBO_ENUM:
                    Assertions.assertThat(value.getOption().map(MdmParamOption::getId).orElseThrow())
                        .isIn(param.getOptions().stream().map(MdmParamOption::getId).collect(Collectors.toSet()));
                    Assertions.assertThat(value.getNumeric()).isEmpty();
                    Assertions.assertThat(value.getString()).isEmpty();
                    Assertions.assertThat(value.getBool()).isEmpty();
                    break;
                case MBO_BOOL:
                case BOOL:
                    Assertions.assertThat(value.getBool()).isPresent();
                    Assertions.assertThat(value.getNumeric()).isEmpty();
                    Assertions.assertThat(value.getOption()).isEmpty();
                    Assertions.assertThat(value.getString()).isEmpty();
                    break;
                case NUMERIC:
                    Assertions.assertThat(value.getNumeric()).isPresent();
                    Assertions.assertThat(value.getBool()).isEmpty();
                    Assertions.assertThat(value.getOption()).isEmpty();
                    Assertions.assertThat(value.getString()).isEmpty();
                    break;
                default:
                    break;
            }
            Assertions.assertThat(value.getMasterDataSourceType()).isEqualTo(MasterDataSourceType.MDM_DEFAULT);
            Assertions.assertThat(value.getMasterDataSourceId()).isEqualTo("tamper-tool");
        }
    }

    private CommonMsku selectMsku(long mskuId) {
        return mskuRepository.findMsku(mskuId)
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .orElse(null);
    }

    private CommonMsku msku(long mskuId, MskuParamValue... values) {
        return new CommonMsku(new ModelKey(0L, mskuId), new HashSet<>(Arrays.asList(values)));
    }

    private MskuParamValue value(long mskuId, long mdmParamId, Object value) {
        MskuParamValue result = new MskuParamValue().setMskuId(mskuId);
        result.setMdmParamId(mdmParamId);
        result.setXslName(mdmParamCache.get(mdmParamId).getXslName());
        switch (mdmParamCache.get(mdmParamId).getValueType()) {
            case STRING:
                result.setString((String) value);
                break;
            case MBO_NUMERIC_ENUM:
            case ENUM:
            case MBO_ENUM:
                result.setOption(new MdmParamOption((Long) value));
                break;
            case MBO_BOOL:
            case BOOL:
                result.setBool((Boolean) value);
                break;
            case NUMERIC:
                result.setNumeric(BigDecimal.valueOf((int) value));
                break;
            default:
                break;
        }
        return result;
    }
}
