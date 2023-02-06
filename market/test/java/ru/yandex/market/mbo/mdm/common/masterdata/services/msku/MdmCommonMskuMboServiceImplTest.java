package ru.yandex.market.mbo.mdm.common.masterdata.services.msku;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MboMskuUpdateResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.MdmMskuSearchFilter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmUserRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmUserRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToMboQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.GlobalParamValueService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.MboMskuUpdateService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CommonMskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.CommonMskuValidator;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsService;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author albina-gima
 * @date 10/1/20
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MdmCommonMskuMboServiceImplTest extends MdmBaseDbTestClass {
    private static final long TIMESTAMP = 123L;

    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private GlobalParamValueService globalParamValueService;
    @Autowired
    private MdmQueuesManager queuesManager;
    @Autowired
    private CommonMskuValidator commonMskuValidator;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private MskuToMboQueueRepository mskuToMboQueue;

    private MboMskuUpdateService mboMskuUpdateService;
    private MboModelsService mboModelsServiceMock;
    private MdmCommonMskuMboService service;
    private CargoTypeRepository cargoTypeRepository;
    private CommonMskuConverter commonMskuConverter;
    private MdmUserRepository mdmUserRepository;

    @Before
    public void setup() {
        this.mdmUserRepository = new MdmUserRepositoryMock();
        mboModelsServiceMock = Mockito.spy(new MboModelsServiceMock());
        cargoTypeRepository = new CargoTypeRepositoryMock();
        commonMskuConverter = new CommonMskuConverter(mdmParamCache);
        mboMskuUpdateService = new MboMskuUpdateService(mboModelsServiceMock, cargoTypeRepository);
        service = new MdmCommonMskuMboServiceImpl(mboModelsServiceMock, mboMskuUpdateService, commonMskuConverter,
            globalParamValueService, mskuRepository, queuesManager, commonMskuValidator,
                mappingsCacheRepository, mdmUserRepository, skv);
    }

    @Test
    public void testFindShouldReturnCommonMskusByFilter() {
        long mskuId = 21L;

        ModelStorage.Model modelForMbo = ModelStorage.Model.newBuilder()
            .setId(mskuId).setCurrentType("GURU")
            .addParameterValues(getModelParamVal(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID,
                "cargoType300", MboParameters.ValueType.BOOLEAN, true))
            .addParameterValues(getModelParamVal(KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID,
                "cargoType301", MboParameters.ValueType.BOOLEAN, true))
            .build();

        //expected value
        var heavyGoodMskuParamValue = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.HEAVY_GOOD,
            mskuId,
            true,
            null,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        heavyGoodMskuParamValue.setXslName("cargoType300");
        var heavyGood20MskuParamValue = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.HEAVY_GOOD_20,
            mskuId,
            true,
            null,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        heavyGood20MskuParamValue.setXslName("cargoType301");

        CommonMsku mskuExpected = new CommonMsku(0L, mskuId)
            .setParamValues(Stream.of(heavyGoodMskuParamValue, heavyGood20MskuParamValue)
                .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity())));

        mboModelsServiceMock.saveModels(List.of(modelForMbo));
        List<CommonMsku> mskuResult = service.find(new MdmMskuSearchFilter().setMarketSkuIds(List.of(mskuId)));

        Assertions.assertThat(mskuResult).hasSize(1);

        List<MskuParamValue> expectedParams = new ArrayList<>(mskuExpected.getValues());
        ArrayList<MskuParamValue> resultParams = new ArrayList<>(mskuResult.get(0).getValues());
        Assertions.assertThat(expectedParams)
            .usingElementComparatorIgnoringFields("modificationInfo").containsAll(resultParams);
    }

    @Test
    public void testFindShouldReturnEmptyMskuListIfMskuIdNotExistInMbo() {
        long mskuId = 21L;
        long notExistingMskuIdInMbo = 20L;

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(mskuId).setCurrentType("GURU")
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID)
                .setXslName("cargoType300")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(true)
                .setModificationDate(TIMESTAMP))
            .build();

        mboModelsServiceMock.saveModels(List.of(model));
        List<CommonMsku> resultMskus =
            service.find(new MdmMskuSearchFilter().setMarketSkuIds(List.of(notExistingMskuIdInMbo)));

        Assertions.assertThat(resultMskus).hasSize(0);
    }

    @Test
    public void testUpdateShouldChangeExistingMsku() {
        long mskuId = 21L;
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(mskuId).setCurrentType("GURU")
            .addParameterValues(getModelParamVal(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID,
                "cargoType300", MboParameters.ValueType.BOOLEAN, true))
            .addParameterValues(getModelParamVal(KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID,
                "cargoType301", MboParameters.ValueType.BOOLEAN, true))
            .build();

        var heavyGoodMskuParamValue = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.HEAVY_GOOD,
            mskuId,
            false, //поменялось с true на false
            null,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        heavyGoodMskuParamValue.setXslName("cargoType300");
        var heavyGood20MskuParamValue = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.HEAVY_GOOD_20,
            mskuId,
            false, //поменялось с true на false
            null,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        heavyGood20MskuParamValue.setXslName("cargoType301");

        CommonMsku changedMsku = new CommonMsku(0L, mskuId)
            .setParamValues(Stream.of(heavyGoodMskuParamValue, heavyGood20MskuParamValue)
                .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity())));

        mboModelsServiceMock.saveModels(List.of(model));
        List<MboMskuUpdateResult> updateResult = service.update(List.of(changedMsku), 12345L);

        Assertions.assertThat(updateResult).hasSize(1);
        Assertions.assertThat(updateResult.get(0)).isEqualTo(
            new MboMskuUpdateResult(model.getId(), MboMskuUpdateResult.Status.OK));

        //check that existing msku was updated
        List<CommonMsku> updatedMsku = service.find(new MdmMskuSearchFilter().setMarketSkuIds(List.of(mskuId)));

        List<MskuParamValue> expectedParams = List.of(heavyGoodMskuParamValue, heavyGood20MskuParamValue);
        ArrayList<MskuParamValue> updatedParams = new ArrayList<>(updatedMsku.get(0).getValues());
        Assertions.assertThat(updatedParams)
            .usingElementComparatorIgnoringFields("modificationInfo").containsAll(expectedParams);

        // check param values inside MDM updated as well
        Optional<CommonMsku> fetchedMsku = mskuRepository.findMsku(changedMsku.getMskuId());
        Assertions.assertThat(fetchedMsku).isPresent();
        Assertions.assertThat(fetchedMsku.get().getValues())
            .filteredOn(pv -> pv.getMdmParamId() != KnownMdmParams.BMDM_ID)
            .containsExactlyInAnyOrder(heavyGoodMskuParamValue, heavyGood20MskuParamValue);
    }

    @Test
    public void testUpdateShouldIgnoreEmptyParams() {
        long mskuId = 21L;
        int optionIdDays = Math.toIntExact(KnownMdmMboParams.SHELF_LIFE_OPTION_IDS.get(TimeInUnits.TimeUnit.DAY));
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(mskuId).setCurrentType("GURU")
            .addParameterValues(getModelParamVal(KnownMdmMboParams.LIFE_SHELF_PARAM_ID,
                "lifeShelf", MboParameters.ValueType.NUMERIC, "12.0"))
            .addParameterValues(getModelParamVal(KnownMdmMboParams.SHELF_LIFE_UNIT_PARAM_ID,
                "lifeShelf_unit", MboParameters.ValueType.ENUM, optionIdDays))
            .build();

        var shelfLife = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.SHELF_LIFE,
            mskuId,
            null,
            null,
            "",
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        shelfLife.setXslName("lifeShelf");
        var shelfLifeUnit = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.SHELF_LIFE_UNIT,
            mskuId,
            null,
            null,
            "",
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        shelfLifeUnit.setXslName("lifeShelf_unit");

        CommonMsku changedMsku = new CommonMsku(0L, mskuId)
            .setParamValues(Stream.of(shelfLife, shelfLifeUnit)
                .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity())));

        mboModelsServiceMock.saveModels(List.of(model));
        List<MboMskuUpdateResult> updateResult = service.update(List.of(changedMsku), 12345L);

        Assertions.assertThat(updateResult).hasSize(1);
        Assertions.assertThat(updateResult.get(0)).isEqualTo(
            new MboMskuUpdateResult(model.getId(), MboMskuUpdateResult.Status.NO_OP));

        //check that existing msku was not updated
        List<CommonMsku> updatedMsku = service.find(new MdmMskuSearchFilter().setMarketSkuIds(List.of(mskuId)));
        Assertions.assertThat(updatedMsku).hasSize(1);

        Assertions.assertThat(updatedMsku.iterator().next()).isEqualTo(commonMskuConverter.toCommonMsku(model));

        var expectedShelfLife = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.SHELF_LIFE,
            mskuId,
            null,
            12.0,
            null,
            null,
            MasterDataSourceType.MDM_DEFAULT,
            Instant.now()
        );
        expectedShelfLife.setXslName("lifeShelf");
        var expectedShelfLifeUnit = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.SHELF_LIFE_UNIT,
            mskuId,
            null,
            null,
            null,
            new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.DAY)),
            MasterDataSourceType.MDM_DEFAULT,
            Instant.now()
        );
        expectedShelfLifeUnit.setXslName("lifeShelf_unit");

        List<MskuParamValue> expectedParams = List.of(expectedShelfLife, expectedShelfLifeUnit);
        ArrayList<MskuParamValue> updatedParams = new ArrayList<>(updatedMsku.get(0).getValues());
        Assertions.assertThat(updatedParams)
            // default field-by-field cmp fails on decimals, e.g. 12.0 != 12
            .usingElementComparator((a, b) -> a.valueAndSourceEquals(b) ? 0 : -1)
            .containsAll(expectedParams);
    }

    @Test
    public void testUpdateShouldFailOnInvalidParams() {
        long mskuId = 21L;
        int optionIdDays = Math.toIntExact(KnownMdmMboParams.LIFE_TIME_OPTION_IDS.get(TimeInUnits.TimeUnit.DAY));
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(mskuId).setCurrentType("GURU")
            .addParameterValues(getModelParamVal(KnownMdmMboParams.LIFE_TIME_PARAM_ID,
                "lifeTime", MboParameters.ValueType.NUMERIC, "12.0"))
            .addParameterValues(getModelParamVal(KnownMdmMboParams.LIFE_TIME_UNIT_PARAM_ID,
                "lifeTime_unit", MboParameters.ValueType.ENUM, optionIdDays))
            .build();

        var lifeTime = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.LIFE_TIME,
            mskuId,
            null,
            null,
            "1000000",
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        lifeTime.setXslName("lifeTime");
        var lifeTimeUnit = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.LIFE_TIME_UNIT,
            mskuId,
            null,
            null,
            null,
            new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR)),
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        lifeTimeUnit.setXslName("lifeTime_unit");

        CommonMsku changedMsku = new CommonMsku(0L, mskuId)
            .setParamValues(Stream.of(lifeTime, lifeTimeUnit)
                .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity())));

        mboModelsServiceMock.saveModels(List.of(model));
        List<MboMskuUpdateResult> updateResult = service.update(List.of(changedMsku), 12345L);

        Assertions.assertThat(updateResult).hasSize(1);
        Assertions.assertThat(updateResult.get(0).getStatus()).isEqualTo(MboMskuUpdateResult.Status.NOT_OK);
        Assertions.assertThat(updateResult.get(0).stringifyErrors())
            .contains("[Значение '1000000 лет' для колонки 'Срок службы' должно быть в диапазоне 1 день - 50 лет]");

        //check that existing msku was not updated
        List<CommonMsku> updatedMsku = service.find(new MdmMskuSearchFilter().setMarketSkuIds(List.of(mskuId)));
        Assertions.assertThat(updatedMsku).hasSize(1);

        Assertions.assertThat(updatedMsku.iterator().next()).isEqualTo(commonMskuConverter.toCommonMsku(model));

        var expectedShelfLife = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.LIFE_TIME,
            mskuId,
            null,
            12.0,
            null,
            null,
            MasterDataSourceType.MDM_DEFAULT,
            Instant.now()
        );
        expectedShelfLife.setXslName("lifeTime");
        var expectedShelfLifeUnit = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.LIFE_TIME_UNIT,
            mskuId,
            null,
            null,
            null,
            new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.DAY)),
            MasterDataSourceType.MDM_DEFAULT,
            Instant.now()
        );
        expectedShelfLifeUnit.setXslName("lifeTime_unit");

        List<MskuParamValue> expectedParams = List.of(expectedShelfLife, expectedShelfLifeUnit);
        ArrayList<MskuParamValue> updatedParams = new ArrayList<>(updatedMsku.get(0).getValues());
        Assertions.assertThat(updatedParams)
            // default field-by-field cmp fails on decimals, e.g. 12.0 != 12
            .usingElementComparator((a, b) -> a.valueAndSourceEquals(b) ? 0 : -1)
            .containsAll(expectedParams);
    }

    @Test
    public void whenMboBatchSizeLimitExceededEnqueueInsteadOfSyncSaving() {
        // given
        skv.putValue(MdmProperties.MBO_MSKU_UPDATE_BATCH_SIZE_LIMIT, 9);
        skv.invalidateCache();

        Random random = new Random("MARKETMDM-809: АВТОРАЗМЕТКА 2.0".hashCode());

        List<CommonMsku> mskus = new ArrayList<>();
        List<Long> mskuIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final long mskuId = i + 100500;
            CommonMsku msku = new CommonMsku(mskuId, List.of());
            Stream.of(KnownMdmParams.IS_TRACEABLE, KnownMdmParams.WEIGHT_NET)
                .map(mdmParamCache::get)
                .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
                .map(pv -> {
                    MskuParamValue mskuPV = new MskuParamValue().setMskuId(mskuId);
                    pv.copyTo(mskuPV);
                    return mskuPV;
                })
                .forEach(msku::addParamValue);
            mskus.add(msku);
            mskuIds.add(mskuId);
        }

        // when
        List<MboMskuUpdateResult> updateResult = service.update(mskus, 0L);

        // then
        Assertions.assertThat(updateResult).hasSize(10);
        Assertions.assertThat(updateResult)
            .map(MboMskuUpdateResult::getMskuId)
            .containsExactlyInAnyOrderElementsOf(mskuIds);
        Assertions.assertThat(updateResult)
            .map(MboMskuUpdateResult::getStatus)
            .containsOnly(MboMskuUpdateResult.Status.OK);

        Assertions.assertThat(mskuRepository.findMskus(mskuIds).values())
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .containsExactlyInAnyOrderElementsOf(mskus);

        Mockito.verifyZeroInteractions(mboModelsServiceMock);

        Assertions.assertThat(mskuToMboQueue.getUnprocessedBatch(1024))
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactlyInAnyOrderElementsOf(mskuIds);
    }

    private static ModelStorage.ParameterValue.Builder getModelParamVal(long paramId,
                                                                        String xslName,
                                                                        MboParameters.ValueType type,
                                                                        Object typeValue) {
        ModelStorage.ParameterValue.Builder builder = ModelStorage.ParameterValue.newBuilder();
        builder.setParamId(paramId)
            .setXslName(xslName)
            .setTypeId(type.getNumber())
            .setValueType(type)
            .setModificationDate(TIMESTAMP);

        if (MboParameters.ValueType.BOOLEAN.equals(type) && typeValue instanceof Boolean) {
            builder.setBoolValue((boolean) typeValue);
        } else if (MboParameters.ValueType.NUMERIC.equals(type) && typeValue instanceof String) {
            builder.setNumericValue((String) typeValue);
        } else if (MboParameters.ValueType.ENUM.equals(type) && typeValue instanceof Integer) {
            builder.setOptionId((Integer) typeValue);
        }

        return builder;
    }
}
