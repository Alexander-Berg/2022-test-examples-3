package ru.yandex.market.mboc.tms.executors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.AbstractRandomizer;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.export.MboParameters.ValueType;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.ModificationSource;
import ru.yandex.market.mbo.http.ModelStorage.OperationStatusType;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToMboQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuParamEnrichServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.queue.MskuToMboQueueService;
import ru.yandex.market.mbo.mdm.common.utils.MdmDbWithCleaningTestClass;
import ru.yandex.market.mbo.mdm.tms.executors.UploadMskuMasterDataToMskuExecutor;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MskuSyncResult;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.repository.MskuSyncResultRepository;
import ru.yandex.market.mboc.common.randomizers.LocalizedStringRandomizer;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelConverter;
import ru.yandex.market.mboc.common.services.modelstorage.models.LocalizedString;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;

@SuppressWarnings("checkstyle:MagicNumber")
public class UploadMskuMasterDataToMskuExecutorTest extends MdmDbWithCleaningTestClass {
    public static final int SEED = 42;
    private static final int TEST_DATA_COUNT = 50;
    private static final int NON_MDM_PARAMETER = 1234567;
    private static final String TEST_VALUE = "123456";
    private static final String OTHER_TEST_VALUE = "654321";
    private static final Long MBO_USER_ID = 123L;
    private static final List<Long> TEST_MSKU_PARAMS = List.of(
        KnownMdmParams.SHELF_LIFE,
        KnownMdmParams.SHELF_LIFE_UNIT,
        KnownMdmParams.SHELF_LIFE_COMMENT,
        KnownMdmParams.LIFE_TIME,
        KnownMdmParams.LIFE_TIME_UNIT,
        KnownMdmParams.LIFE_TIME_COMMENT,
        KnownMdmParams.GUARANTEE_PERIOD,
        KnownMdmParams.GUARANTEE_PERIOD_UNIT,
        KnownMdmParams.GUARANTEE_PERIOD_COMMENT,
        KnownMdmParams.EXPIR_DATE,
        KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID
    );
    private static final Set<Long> NUMERIC_STRING_PARAMS = Set.of(
        KnownMdmParams.LIFE_TIME,
        KnownMdmParams.GUARANTEE_PERIOD
    );

    @Autowired
    private MskuSyncResultRepository mskuSyncResultRepository;

    @Autowired
    private StorageKeyValueService keyValueService;

    @Autowired
    private MdmParamCache mdmParamCache;

    @Autowired
    private MskuRepository mskuRepository;

    @Autowired
    private MskuToMboQueueRepository mskuToMboQueueRepository;

    private MboModelsServiceMock mboModelsServiceMock;

    private UploadMskuMasterDataToMskuExecutor executor;

    private EnhancedRandom defaultRandom;

    private Map<Long, MdmParam> allCargoTypes;

    private static ModelStorage.ParameterValue maybeMarkAsOperatorFilled(ModelStorage.ParameterValue parameterValue) {
        Random random = new Random(SEED);
        for (int i = 0; i < MskuParamEnrichServiceImpl.CONNECTED_PARAMETERS_LIST.size(); i++) {
            List<Long> list = List.copyOf(MskuParamEnrichServiceImpl.CONNECTED_PARAMETERS_LIST.get(i));
            int j = random.nextInt(list.size());
            if (list.get(j) == parameterValue.getParamId()) {
                return parameterValue.toBuilder()
                    .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                    .build();
            }
        }
        return parameterValue;
    }

    @SuppressWarnings("ConstantConditions")
    private static ModelStorage.ParameterValue updateParameterFromMdmCommonMsku(ModelStorage.ParameterValue pv,
                                                                                CommonMsku commonMsku) {
        ModelStorage.ParameterValue.Builder builder = pv.toBuilder();

        if (pv.getParamId() == KnownMdmMboParams.SHELF_LIFE_UNIT_PARAM_ID) {
            commonMsku.getParamValue(KnownMdmParams.SHELF_LIFE_UNIT)
                .flatMap(MdmParamValue::getOption)
                .map(MdmParamOption::getId)
                .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                .map(KnownMdmMboParams.SHELF_LIFE_OPTION_IDS::get)
                .map(Math::toIntExact)
                .ifPresent(builder::setOptionId);
        } else if (pv.getParamId() == KnownMdmMboParams.LIFE_SHELF_PARAM_ID) {
            String mdmValue = commonMsku.getParamValue(KnownMdmParams.SHELF_LIFE)
                .flatMap(MdmParamValue::getNumeric)
                .map(BigDecimal::toPlainString)
                .orElse(null);
            builder.setNumericValue(mdmValue);
        } else if (pv.getParamId() == KnownMdmMboParams.LIFE_TIME_UNIT_PARAM_ID) {
            commonMsku.getParamValue(KnownMdmParams.LIFE_TIME_UNIT)
                .flatMap(MdmParamValue::getOption)
                .map(MdmParamOption::getId)
                .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                .map(KnownMdmMboParams.LIFE_TIME_OPTION_IDS::get)
                .map(Math::toIntExact)
                .ifPresent(builder::setOptionId);
        } else if (pv.getParamId() == KnownMdmMboParams.LIFE_TIME_PARAM_ID) {
            String mdmValue = commonMsku.getParamValue(KnownMdmParams.LIFE_TIME)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            builder.clearStrValue().addStrValue(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode(Language.RUSSIAN.getIsoCode())
                .setValue(mdmValue)
                .build());
        } else if (pv.getParamId() == KnownMdmMboParams.GUARANTEE_PERIOD_UNIT_PARAM_ID) {
            commonMsku.getParamValue(KnownMdmParams.GUARANTEE_PERIOD_UNIT)
                .flatMap(MdmParamValue::getOption)
                .map(MdmParamOption::getId)
                .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                .map(KnownMdmMboParams.GUARANTEE_PERIOD_OPTION_IDS::get)
                .map(Math::toIntExact)
                .ifPresent(builder::setOptionId);
        } else if (pv.getParamId() == KnownMdmMboParams.WARRANTY_PERIOD_PARAM_ID) {
            String mdmValue = commonMsku.getParamValue(KnownMdmParams.GUARANTEE_PERIOD)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            builder.clearStrValue().addStrValue(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode(Language.RUSSIAN.getIsoCode())
                .setValue(mdmValue)
                .build());
        } else {
            throw new IllegalStateException();
        }

        return builder.build();
    }

    private static Map<Long, ModelStorage.ParameterValue> getParamValues(ModelStorage.Model model) {
        return model.getParameterValuesList().stream()
            .collect(
                Collectors.toMap(ModelStorage.ParameterValue::getParamId, value -> value)
            );
    }

    private static ModelStorage.Model withParameterValue(ModelStorage.Model model,
                                                         ModelStorage.ParameterValue... parameterValuesToUpdate) {
        Set<Long> parametersToUpdate = Stream.of(parameterValuesToUpdate)
            .map(ModelStorage.ParameterValue::getParamId)
            .collect(Collectors.toSet());

        List<ModelStorage.ParameterValue> values = model.getParameterValuesList().stream()
            .filter(pv -> !(parametersToUpdate.contains(pv.getParamId())))
            .collect(Collectors.toList());

        values.addAll(Arrays.asList(parameterValuesToUpdate));

        return model.toBuilder().clearParameterValues().addAllParameterValues(values).build();
    }

    private static boolean equalsIgnoringModificationSource(ModelStorage.ParameterValue p1,
                                                            ModelStorage.ParameterValue p2) {
        ModelStorage.ParameterValue b1 = p1.toBuilder()
            .clearModificationDate()
            .clearValueSource()
            .clearUserId()
            .build();

        ModelStorage.ParameterValue b2 = p2.toBuilder()
            .clearModificationDate()
            .clearValueSource()
            .clearUserId()
            .build();

        return Objects.equals(b1, b2);
    }

    private void incTimeAndRandUnit(CommonMsku commonMsku, long valueParamId, long unitParamId) {
        MskuParamValue timeValuePV = (MskuParamValue) new MskuParamValue()
            .setMskuId(commonMsku.getMskuId())
            .setXslName(mdmParamCache.get(valueParamId).getXslName())
            .setMdmParamId(valueParamId);
        commonMsku.getParamValue(valueParamId).ifPresent(oldParamValue -> oldParamValue.copyTo(timeValuePV));
        if (NUMERIC_STRING_PARAMS.contains(valueParamId)) {
            String newTimeValue = timeValuePV.getString()
                .map(BigDecimal::new)
                .map(value -> value.add(BigDecimal.ONE))
                .map(BigDecimal::toPlainString)
                .orElse("1");
            timeValuePV.setString(newTimeValue);
        } else {
            BigDecimal newTimeValue = timeValuePV.getNumeric().orElse(BigDecimal.ZERO).add(BigDecimal.ONE);
            timeValuePV.setNumeric(newTimeValue);
        }
        commonMsku.addParamValue(timeValuePV);

        MskuParamValue unitParamValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(commonMsku.getMskuId())
            .setXslName(mdmParamCache.get(unitParamId).getXslName())
            .setMdmParamId(unitParamId);
        commonMsku.getParamValue(unitParamId).ifPresent(oldParamValue -> oldParamValue.copyTo(unitParamValue));
        TimeInUnits.TimeUnit oldUnit = unitParamValue.getOption()
            .map(MdmParamOption::getId)
            .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
            .orElse(TimeInUnits.TimeUnit.DAY);
        Arrays.stream(TimeInUnits.TimeUnit.values())
            .filter(unit -> unit != oldUnit && unit != TimeInUnits.TimeUnit.UNLIMITED)
            .sorted(Comparator.naturalOrder())
            .reduce((a, b) -> defaultRandom.nextBoolean() ? a : b)
            .map(KnownMdmParams.TIME_UNITS_OPTIONS.inverse()::get)
            .flatMap(optionId -> mdmParamCache.get(unitParamId).getOptions().stream()
                .filter(option -> option.getId() == optionId)
                .findAny())
            .ifPresent(unitParamValue::setOption);
        commonMsku.addParamValue(unitParamValue);
    }

    @Before
    public void setup() {
        mboModelsServiceMock = Mockito.spy(new MboModelsServiceMock());

        MskuToMboQueueService mskuToMboQueueService = new MskuToMboQueueService(
            mboModelsServiceMock,
            keyValueService,
            new MskuParamEnrichServiceImpl(mdmParamCache),
            mskuToMboQueueRepository,
            mskuRepository,
            mskuSyncResultRepository,
            transactionTemplate
        );

        executor = Mockito.spy(new UploadMskuMasterDataToMskuExecutor(mskuToMboQueueService));

        defaultRandom = TestDataUtils.defaultRandom(SEED);

        allCargoTypes = mdmParamCache.getAll().stream()
            .filter(mdmParam -> mdmParam.getExternals().getMboParamId() > 0)
            .filter(mdmParam -> mdmParam.getExternals().getCargotypeId() > 0)
            .collect(Collectors.toMap(MdmParam::getId, Function.identity()));
    }

    @Test
    public void whenMskuDeletedShouldNotUpdate() {
        TestData testData = generateTestData(1).get(0);
        testData.setModel(testData.getModel().toBuilder().setDeleted(true).build());

        insertTestData(testData);

        processMskuToMboQueueAfterCommit();

        ModelStorage.Model updatedModel = loadModelFromStorage(testData.getModel());
        Assertions.assertThat(updatedModel).isEqualTo(testData.getModel());

        Assertions.assertThat(mskuSyncResultRepository.findAll()).containsExactly(
            new MskuSyncResult(testData.getModel().getId(), MskuSyncResult.MskuSyncStatus.UP_TO_DATE, null,
                "Internally detected up to date")
        );
    }

    @Test
    public void whenNotPublishedOnBlueShouldNotFailOnValidationError() {
        List<TestData> testDatas = generateTestData(2);

        TestData testDataGuru = testDatas.get(0);
        testDataGuru.setModel(testDataGuru.getModel().toBuilder()
            .setCurrentType(ModelStorage.ModelType.GURU.name())
            .setBluePublished(false).build());

        TestData testDataSku = testDatas.get(1);
        testDataSku.setModel(testDataSku.getModel().toBuilder()
            .setCurrentType(ModelStorage.ModelType.SKU.name())
            .setPublished(false).build());

        insertTestData(testDatas);

        mboModelsServiceMock.setSpecialResultsForModel(
            testDataGuru.getModel().getId(),
            operationStatus(testDataGuru.getModel(), ModelStorage.OperationStatusType.VALIDATION_ERROR)
        );
        mboModelsServiceMock.setSpecialResultsForModel(
            testDataSku.getModel().getId(),
            operationStatus(testDataSku.getModel(), ModelStorage.OperationStatusType.VALIDATION_ERROR)
        );
        Assertions.assertThatCode(this::processMskuToMboQueueAfterCommit).doesNotThrowAnyException();

        ModelStorage.Model guruModelAfter = loadModelFromStorage(testDataGuru.getModel());
        ModelStorage.Model skuModelAfter = loadModelFromStorage(testDataSku.getModel());

        Assertions.assertThat(guruModelAfter).isEqualTo(testDataGuru.getModel());
        Assertions.assertThat(skuModelAfter).isEqualTo(testDataSku.getModel());

        Assertions.assertThat(mskuSyncResultRepository.findAll()).containsExactlyInAnyOrder(
            new MskuSyncResult(testDataGuru.getModel().getId(), MskuSyncResult.MskuSyncStatus.VALIDATION_ERROR,
                "VALIDATION_ERROR", "Validation errors: "),
            new MskuSyncResult(testDataSku.getModel().getId(), MskuSyncResult.MskuSyncStatus.VALIDATION_ERROR,
                "VALIDATION_ERROR", "Validation errors: ")
        );
    }

    @Test
    public void whenModelParallelModifiedShouldRetry() {
        List<TestData> testDatas = generateTestData(TEST_DATA_COUNT);
        Set<Long> mskuIds = testDatas.stream()
            .map(TestData::getModel)
            .map(ModelStorage.Model::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        insertTestData(testDatas);

        ModelStorage.Model exampleModel1 = testDatas.stream()
            .skip(5)
            .findAny()
            .map(TestData::getModel)
            .orElseThrow();
        ModelStorage.Model exampleModel2 = testDatas.stream()
            .skip(10)
            .findAny()
            .map(TestData::getModel)
            .orElseThrow();

        mboModelsServiceMock.setSpecialResultCalledOnceForModel(
            exampleModel1.getId(), operationStatus(exampleModel1, OperationStatusType.MODEL_MODIFIED));
        mboModelsServiceMock.setSpecialResultCalledOnceForModel(
            exampleModel2.getId(), operationStatus(exampleModel2, OperationStatusType.MODEL_MODIFIED));
        Assertions.assertThatCode(this::processMskuToMboQueueAfterCommit).doesNotThrowAnyException();

        List<MskuSyncResult> results = mskuSyncResultRepository.findAll();
        Assertions.assertThat(results.stream())
            .map(MskuSyncResult::getModelId)
            .containsExactlyInAnyOrderElementsOf(mskuIds);
        Assertions.assertThat(results.stream())
            .map(MskuSyncResult::getStatus)
            .allMatch(MskuSyncResult.MskuSyncStatus.OK::equals);
        Mockito.verify(mboModelsServiceMock, Mockito.times(2)).loadRawModels(Mockito.anyCollection());
    }

    @Test
    public void whenMskuUpdateShouldNotUpdateUnrelatedModelAttributes() {
        List<TestData> testDatas = generateTestData(TEST_DATA_COUNT);

        ModelStorage.ParameterValue nonMdmParameterValue = generateNonMdmParameterValue(TEST_VALUE);
        testDatas.forEach(
            t -> t.setModel(withParameterValue(t.getModel(), nonMdmParameterValue))
        );

        insertTestData(testDatas);

        processMskuToMboQueueAfterCommit();

        for (TestData testData : testDatas) {
            ModelStorage.Model modelBefore = testData.getModel();
            ModelStorage.Model updatedModel = loadModelFromStorage(modelBefore);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(updatedModel).isNotEqualTo(modelBefore);

                softly.assertThat(updatedModel)
                    .isEqualToIgnoringGivenFields(modelBefore, "parameterValues_");

                softly.assertThat(modelBefore.getParameterValuesList())
                    .isSubsetOf(updatedModel.getParameterValuesList());

                softly.assertThat(updatedModel.getParameterValuesList()).contains(nonMdmParameterValue);
            });
        }
    }

    @Test
    public void whenMskuContainsParameterMultiValuesShouldPreserveAllValues() {
        TestData testData = generateTestData(1).get(0);

        ModelStorage.ParameterValue nonMdmParameterValue = generateNonMdmParameterValue(TEST_VALUE);
        ModelStorage.ParameterValue otherNonMdmParameterValue = generateNonMdmParameterValue(OTHER_TEST_VALUE);

        testData.setModel(withParameterValue(testData.getModel(), nonMdmParameterValue, otherNonMdmParameterValue));
        insertTestData(testData);

        ModelStorage.Model modelBefore = testData.getModel();
        processMskuToMboQueueAfterCommit();

        ModelStorage.Model updatedModel = loadModelFromStorage(modelBefore);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(modelBefore.getParameterValuesList())
                .isSubsetOf(updatedModel.getParameterValuesList());

            softly.assertThat(updatedModel.getParameterValuesList())
                .contains(nonMdmParameterValue, otherNonMdmParameterValue);
        });
    }

    @Test
    public void whenMskuUpdateShouldSetExpectedParameterValues() {
        List<TestData> testDatas = generateTestData(TEST_DATA_COUNT);
        insertTestData(testDatas);

        processMskuToMboQueueAfterCommit();
        for (TestData testData : testDatas) {
            ModelStorage.Model modelBefore = testData.getModel();
            ModelStorage.Model updatedModel = loadModelFromStorage(modelBefore);

            Map<Long, ModelStorage.ParameterValue> parameterValueMap = getParamValues(updatedModel);

            String customsCommodityCodeFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.CUSTOMS_COMMODITY_CODE_PARAM_ID)
                .getStrValue(0)
                .getValue();

            Boolean expirDateBooleanFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                .getBoolValue();

            int expirDateOptionIdFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                .getOptionId();

            String shelfLifeFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.LIFE_SHELF_PARAM_ID)
                .getNumericValue();
            int shelfLifeUnitFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.SHELF_LIFE_UNIT_PARAM_ID)
                .getOptionId();
            String shelfLifeCommentFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.SHELF_LIFE_COMMENT_PARAM_ID)
                .getStrValue(0).getValue();

            int lifeTimeUnitFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.LIFE_TIME_UNIT_PARAM_ID)
                .getOptionId();
            String lifeTimeFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.LIFE_TIME_PARAM_ID)
                .getStrValue(0)
                .getValue();
            String lifeTimeCommentFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.LIFE_TIME_COMMENT_PARAM_ID)
                .getStrValue(0).getValue();

            String guaranteePeriodFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.WARRANTY_PERIOD_PARAM_ID)
                .getStrValue(0)
                .getValue();
            int guaranteePeriodUnitFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.GUARANTEE_PERIOD_UNIT_PARAM_ID)
                .getOptionId();
            String guaranteePeriodCommentFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.GUARANTEE_PERIOD_COMMENT_PARAM_ID)
                .getStrValue(0).getValue();

            CommonMsku mdmMsku = testData.getMdmCommonMsku();
            String customsCommodityCodeFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            String shelfLifeFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.SHELF_LIFE)
                .flatMap(MdmParamValue::getNumeric)
                .map(BigDecimal::toPlainString)
                .orElse(null);
            int shelfLifeUintFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.SHELF_LIFE_UNIT)
                .flatMap(MdmParamValue::getOption)
                .map(MdmParamOption::getId)
                .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                .map(KnownMdmMboParams.SHELF_LIFE_OPTION_IDS::get)
                .map(Math::toIntExact)
                .orElse(0);
            String shelfLifeCommentFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.SHELF_LIFE_COMMENT)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            String lifeTimeFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.LIFE_TIME)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            int lifeTimeUintFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.LIFE_TIME_UNIT)
                .flatMap(MdmParamValue::getOption)
                .map(MdmParamOption::getId)
                .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                .map(KnownMdmMboParams.LIFE_TIME_OPTION_IDS::get)
                .map(Math::toIntExact)
                .orElse(0);
            String lifeTimeCommentFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.LIFE_TIME_COMMENT)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            String guaranteePeriodFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.GUARANTEE_PERIOD)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            int guaranteePeriodUintFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.GUARANTEE_PERIOD_UNIT)
                .flatMap(MdmParamValue::getOption)
                .map(MdmParamOption::getId)
                .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                .map(KnownMdmMboParams.GUARANTEE_PERIOD_OPTION_IDS::get)
                .map(Math::toIntExact)
                .orElse(0);
            String guaranteePeriodCommentFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.GUARANTEE_PERIOD_COMMENT)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            boolean expirDateFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.EXPIR_DATE)
                .flatMap(MdmParamValue::getBool)
                .orElse(false);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(shelfLifeFromUpdatedModel).isEqualTo(shelfLifeFromMdmMsku);
                softly.assertThat(lifeTimeFromUpdatedModel).isEqualTo(lifeTimeFromMdmMsku);
                softly.assertThat(guaranteePeriodFromUpdatedModel).isEqualTo(guaranteePeriodFromMdmMsku);

                softly.assertThat(shelfLifeUnitFromUpdatedModel).isEqualTo(shelfLifeUintFromMdmMsku);
                softly.assertThat(lifeTimeUnitFromUpdatedModel).isEqualTo(lifeTimeUintFromMdmMsku);
                softly.assertThat(guaranteePeriodUnitFromUpdatedModel).isEqualTo(guaranteePeriodUintFromMdmMsku);

                softly.assertThat(shelfLifeCommentFromUpdatedModel).isEqualTo(shelfLifeCommentFromMdmMsku);
                softly.assertThat(lifeTimeCommentFromUpdatedModel).isEqualTo(lifeTimeCommentFromMdmMsku);
                softly.assertThat(guaranteePeriodCommentFromUpdatedModel).isEqualTo(guaranteePeriodCommentFromMdmMsku);

                softly.assertThat(customsCommodityCodeFromUpdatedModel).isEqualTo(customsCommodityCodeFromMdmMsku);

                softly.assertThat(expirDateBooleanFromUpdatedModel).isEqualTo(expirDateFromMdmMsku);
                softly.assertThat(expirDateOptionIdFromUpdatedModel).isEqualTo(expirDateBooleanFromUpdatedModel ?
                    KnownMdmMboParams.EXPIR_DATE_TRUE_OPTION_ID : KnownMdmMboParams.EXPIR_DATE_FALSE_OPTION_ID);

                List<ModelStorage.ParameterValue> expirDateValues = updatedModel.getParameterValuesList().stream()
                    .filter(pv -> pv.getParamId() == KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                    .collect(Collectors.toList());

                softly.assertThat(expirDateValues).hasSize(1);

                for (MdmParam cargoTypeParam : allCargoTypes.values()) {
                    long mboId = cargoTypeParam.getExternals().getMboParamId();
                    long mdmId = cargoTypeParam.getId();
                    boolean mdmValue = mdmMsku.getParamValue(mdmId)
                        .flatMap(MdmParamValue::getBool)
                        .orElse(false);
                    boolean mboValue = Optional.ofNullable(parameterValueMap.get(mboId))
                        .map(ParameterValue::getBoolValue)
                        .orElse(false);
                    softly.assertThat(mdmValue).isEqualTo(mboValue);
                }
            });
        }
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @Test
    public void whenUploadingModelsBatchShouldNotMixParameters() {
        List<TestData> testDataBatch = generateTestData(TEST_DATA_COUNT);

        EnhancedRandom random = TestDataUtils.defaultRandom(SEED);
        testDataBatch.forEach(testData -> {
            ModelStorage.ParameterValue randomNonMdmParameter = ModelStorage.ParameterValue.newBuilder()
                .setParamId(random.nextInt())
                .setValueSource(ModelStorage.ModificationSource.AUTO)
                .setNumericValue(random.nextInt() + "")
                .build();
            testData.setModel(withParameterValue(testData.getModel(), randomNonMdmParameter));
        });

        insertTestData(testDataBatch);

        processMskuToMboQueueAfterCommit();

        for (TestData testData : testDataBatch) {
            ModelStorage.Model modelBefore = testData.getModel();
            ModelStorage.Model updatedModel = loadModelFromStorage(modelBefore);

            Map<Long, ModelStorage.ParameterValue> parameterValueMap = getParamValues(updatedModel);

            String customsCommodityCodeFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.CUSTOMS_COMMODITY_CODE_PARAM_ID)
                .getStrValue(0)
                .getValue();

            Boolean expirDateBooleanFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                .getBoolValue();

            int expirDateOptionIdFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                .getOptionId();

            String shelfLifeFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.LIFE_SHELF_PARAM_ID)
                .getNumericValue();
            int shelfLifeUnitFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.SHELF_LIFE_UNIT_PARAM_ID)
                .getOptionId();
            String shelfLifeCommentFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.SHELF_LIFE_COMMENT_PARAM_ID)
                .getStrValue(0).getValue();

            int lifeTimeUnitFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.LIFE_TIME_UNIT_PARAM_ID)
                .getOptionId();
            String lifeTimeFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.LIFE_TIME_PARAM_ID)
                .getStrValue(0)
                .getValue();
            String lifeTimeCommentFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.LIFE_TIME_COMMENT_PARAM_ID)
                .getStrValue(0).getValue();

            String guaranteePeriodFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.WARRANTY_PERIOD_PARAM_ID)
                .getStrValue(0)
                .getValue();
            int guaranteePeriodUnitFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.GUARANTEE_PERIOD_UNIT_PARAM_ID)
                .getOptionId();
            String guaranteePeriodCommentFromUpdatedModel = parameterValueMap
                .get(KnownMdmMboParams.GUARANTEE_PERIOD_COMMENT_PARAM_ID)
                .getStrValue(0).getValue();

            CommonMsku mdmMsku = testData.getMdmCommonMsku();
            String customsCommodityCodeFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            String shelfLifeFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.SHELF_LIFE)
                .flatMap(MdmParamValue::getNumeric)
                .map(BigDecimal::toPlainString)
                .orElse(null);
            int shelfLifeUintFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.SHELF_LIFE_UNIT)
                .flatMap(MdmParamValue::getOption)
                .map(MdmParamOption::getId)
                .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                .map(KnownMdmMboParams.SHELF_LIFE_OPTION_IDS::get)
                .map(Math::toIntExact)
                .orElse(0);
            String shelfLifeCommentFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.SHELF_LIFE_COMMENT)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            String lifeTimeFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.LIFE_TIME)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            int lifeTimeUintFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.LIFE_TIME_UNIT)
                .flatMap(MdmParamValue::getOption)
                .map(MdmParamOption::getId)
                .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                .map(KnownMdmMboParams.LIFE_TIME_OPTION_IDS::get)
                .map(Math::toIntExact)
                .orElse(0);
            String lifeTimeCommentFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.LIFE_TIME_COMMENT)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            String guaranteePeriodFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.GUARANTEE_PERIOD)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            int guaranteePeriodUintFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.GUARANTEE_PERIOD_UNIT)
                .flatMap(MdmParamValue::getOption)
                .map(MdmParamOption::getId)
                .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                .map(KnownMdmMboParams.GUARANTEE_PERIOD_OPTION_IDS::get)
                .map(Math::toIntExact)
                .orElse(0);
            String guaranteePeriodCommentFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.GUARANTEE_PERIOD_COMMENT)
                .flatMap(MdmParamValue::getString)
                .orElse(null);
            boolean expirDateFromMdmMsku = mdmMsku.getParamValue(KnownMdmParams.EXPIR_DATE)
                .flatMap(MdmParamValue::getBool)
                .orElse(false);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(shelfLifeFromUpdatedModel)
                    .withFailMessage("Shelf life value. Updated model: %s. Mdm msku: %s.",
                        shelfLifeFromUpdatedModel, shelfLifeFromMdmMsku)
                    .isEqualTo(shelfLifeFromMdmMsku);
                softly.assertThat(lifeTimeFromUpdatedModel).isEqualTo(lifeTimeFromMdmMsku);
                softly.assertThat(guaranteePeriodFromUpdatedModel).isEqualTo(guaranteePeriodFromMdmMsku);

                softly.assertThat(shelfLifeUnitFromUpdatedModel).isEqualTo(shelfLifeUintFromMdmMsku);
                softly.assertThat(lifeTimeUnitFromUpdatedModel).isEqualTo(lifeTimeUintFromMdmMsku);
                softly.assertThat(guaranteePeriodUnitFromUpdatedModel).isEqualTo(guaranteePeriodUintFromMdmMsku);

                softly.assertThat(shelfLifeCommentFromUpdatedModel).isEqualTo(shelfLifeCommentFromMdmMsku);
                softly.assertThat(lifeTimeCommentFromUpdatedModel).isEqualTo(lifeTimeCommentFromMdmMsku);
                softly.assertThat(guaranteePeriodCommentFromUpdatedModel).isEqualTo(guaranteePeriodCommentFromMdmMsku);

                softly.assertThat(customsCommodityCodeFromUpdatedModel).isEqualTo(customsCommodityCodeFromMdmMsku);

                softly.assertThat(expirDateBooleanFromUpdatedModel).isEqualTo(expirDateFromMdmMsku);
                softly.assertThat(expirDateOptionIdFromUpdatedModel).isEqualTo(expirDateBooleanFromUpdatedModel ?
                    KnownMdmMboParams.EXPIR_DATE_TRUE_OPTION_ID : KnownMdmMboParams.EXPIR_DATE_FALSE_OPTION_ID);

                List<ModelStorage.ParameterValue> expirDateValues = updatedModel.getParameterValuesList().stream()
                    .filter(pv -> pv.getParamId() == KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                    .collect(Collectors.toList());

                softly.assertThat(expirDateValues).hasSize(1);

                for (MdmParam cargoTypeParam : allCargoTypes.values()) {
                    long mboId = cargoTypeParam.getExternals().getMboParamId();
                    long mdmId = cargoTypeParam.getId();
                    boolean mdmValue = mdmMsku.getParamValue(mdmId)
                        .flatMap(MdmParamValue::getBool)
                        .orElse(false);
                    boolean mboValue = Optional.ofNullable(parameterValueMap.get(mboId))
                        .map(ParameterValue::getBoolValue)
                        .orElse(false);
                    softly.assertThat(mdmValue).isEqualTo(mboValue);
                }
            });
        }
    }

    @Test
    public void whenMskuUpdateShouldNotUpdateManuallySetMskuParameterValues() {
        TestData testData = generateTestData(1).get(0);
        ModelStorage.Model model = testData.getModel();
        CommonMsku mdmCommonMsku = testData.getMdmCommonMsku();

        ModelStorage.ParameterValue expirDate = KnownMdmMboParams.getShelfLifeRequiredParameterValue(
            !mdmCommonMsku.getParamValue(KnownMdmParams.EXPIR_DATE).flatMap(MdmParamValue::getBool).orElse(false),
            ModificationSource.OPERATOR_FILLED,
            MBO_USER_ID
        );
        testData.setModel(withParameterValue(model, expirDate));

        insertTestData(testData);

        processMskuToMboQueueAfterCommit();

        ModelStorage.Model updatedModel = loadModelFromStorage(model);

        SoftAssertions.assertSoftly(softly -> {
            ModelStorage.ParameterValue updatedExpirDate = getParamValues(updatedModel)
                .get(KnownMdmMboParams.EXPIR_DATE_PARAM_ID);

            softly.assertThat(updatedExpirDate.getValueSource())
                .isEqualTo(ModelStorage.ModificationSource.OPERATOR_FILLED);
            softly.assertThat(updatedExpirDate.getBoolValue())
                .isEqualTo(expirDate.getBoolValue());

            List<ModelStorage.ParameterValue> expirDateValues = updatedModel.getParameterValuesList().stream()
                .filter(pv -> pv.getParamId() == KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                .collect(Collectors.toList());

            softly.assertThat(expirDateValues).hasSize(1);
        });
    }

    @Test
    public void whenMboMskuAlreadyHaveCorrectValueShouldNotUpdate() {
        TestData testData = generateTestData(1).get(0);
        ModelStorage.Model model = testData.getModel();
        CommonMsku mdmCommonMsku = testData.getMdmCommonMsku();

        boolean mdmExpirDate = mdmCommonMsku.getParamValue(KnownMdmParams.EXPIR_DATE)
            .flatMap(MdmParamValue::getBool)
            .orElse(false);

        // Set same parameter value with TOOL modification source, which allowed to rewrite
        ModelStorage.ParameterValue expirDate = KnownMdmMboParams
            .getShelfLifeRequiredParameterValue(mdmExpirDate, ModificationSource.MDM, MBO_USER_ID)
            .toBuilder()
            .setValueSource(ModelStorage.ModificationSource.TOOL)
            .build();
        testData.setModel(withParameterValue(model, expirDate));

        insertTestData(testData);

        processMskuToMboQueueAfterCommit();

        ModelStorage.Model updatedModel = loadModelFromStorage(model);

        SoftAssertions.assertSoftly(softly -> {
            ModelStorage.ParameterValue updatedExpirDate = getParamValues(updatedModel)
                .get(KnownMdmMboParams.EXPIR_DATE_PARAM_ID);

            // ModificationSource must not change
            softly.assertThat(updatedExpirDate.getValueSource())
                .isEqualTo(ModelStorage.ModificationSource.TOOL);

            softly.assertThat(updatedExpirDate.getBoolValue())
                .isEqualTo(expirDate.getBoolValue());

            List<ModelStorage.ParameterValue> expirDateValues = updatedModel.getParameterValuesList().stream()
                .filter(pv -> pv.getParamId() == KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                .collect(Collectors.toList());

            softly.assertThat(expirDateValues).hasSize(1);
        });
    }

    @Test
    public void whenMboMskuNotHaveCargoTypeParamShouldNotCreateNewFalse() {
        long heavyGoodMboParamId = allCargoTypes.get(KnownMdmParams.HEAVY_GOOD).getExternals().getMboParamId();
        TestData testData = generateTestData(1).get(0);
        ModelStorage.Model model = testData.getModel();
        CommonMsku commonMsku = testData.getMdmCommonMsku();
        commonMsku.addParamValue((MskuParamValue) new MskuParamValue()
            .setMskuId(commonMsku.getMskuId())
            .setMdmParamId(KnownMdmParams.HEAVY_GOOD)
            .setBool(false)
        );

        insertTestData(testData);

        processMskuToMboQueueAfterCommit();

        ModelStorage.Model updatedModel = loadModelFromStorage(model);

        SoftAssertions.assertSoftly(softly -> {
            List<ModelStorage.ParameterValue> heavyGoods = updatedModel.getParameterValuesList().stream()
                .filter(pv -> pv.getParamId() == heavyGoodMboParamId)
                .collect(Collectors.toList());

            softly.assertThat(heavyGoods).isEmpty();
        });
    }

    @Test
    public void whenMboMskuHaveCargoTypeParamWithEmptyValueShouldUpdateToFalse() {
        long heavyGoodMboParamId = allCargoTypes.get(KnownMdmParams.HEAVY_GOOD).getExternals().getMboParamId();
        // Same as previous test, but assuming model has parameter with empty value.
        TestData testData = generateTestData(1).get(0);
        ModelStorage.Model model = testData.getModel();
        CommonMsku commonMsku = testData.getMdmCommonMsku();
        commonMsku.addParamValue((MskuParamValue) new MskuParamValue()
            .setMskuId(commonMsku.getMskuId())
            .setMdmParamId(KnownMdmParams.HEAVY_GOOD)
            .setBool(false)
        );

        ModelStorage.ParameterValue isHeavyGood = ParameterValue.newBuilder()
            .setParamId(heavyGoodMboParamId)
            .setTypeId(ValueType.BOOLEAN_VALUE)
            .setValueType(ValueType.BOOLEAN)
            .setXslName("cargoType300")
            .setValueSource(ModificationSource.AUTO)
            .build();
        testData.setModel(withParameterValue(model, isHeavyGood));
        insertTestData(testData);

        processMskuToMboQueueAfterCommit();

        ModelStorage.Model updatedModel = loadModelFromStorage(model);

        SoftAssertions.assertSoftly(softly -> {
            ModelStorage.ParameterValue updatedIsHeavy = getParamValues(updatedModel)
                .get(heavyGoodMboParamId);

            // ModificationSource will change to MDM
            softly.assertThat(updatedIsHeavy.getValueSource())
                .isEqualTo(ModelStorage.ModificationSource.MDM);

            softly.assertThat(updatedIsHeavy.hasBoolValue())
                .isEqualTo(true);
            softly.assertThat(updatedIsHeavy.getBoolValue())
                .isEqualTo(false);

            List<ModelStorage.ParameterValue> isHeavyAllValues = updatedModel.getParameterValuesList().stream()
                .filter(pv -> pv.getParamId() == heavyGoodMboParamId)
                .collect(Collectors.toList());

            softly.assertThat(isHeavyAllValues).hasSize(1);
        });
    }

    @Test
    public void whenMskuUpdateShouldUpdateCargoTypeFromNullToTrue() {
        long heavyGoodMboParamId = allCargoTypes.get(KnownMdmParams.HEAVY_GOOD).getExternals().getMboParamId();
        TestData testData = generateTestData(1).get(0);
        ModelStorage.Model model = testData.getModel();
        CommonMsku commonMsku = testData.getMdmCommonMsku();
        commonMsku.addParamValue((MskuParamValue) new MskuParamValue()
            .setMskuId(commonMsku.getMskuId())
            .setMdmParamId(KnownMdmParams.HEAVY_GOOD)
            .setBool(true)
        );

        insertTestData(testData);

        processMskuToMboQueueAfterCommit();

        ModelStorage.Model updatedModel = loadModelFromStorage(model);

        SoftAssertions.assertSoftly(softly -> {
            ModelStorage.ParameterValue updatedIsHeavy = getParamValues(updatedModel)
                .get(heavyGoodMboParamId);

            softly.assertThat(updatedIsHeavy.getValueSource())
                .isEqualTo(ModelStorage.ModificationSource.MDM);

            softly.assertThat(updatedIsHeavy.getBoolValue())
                .isEqualTo(true);

            List<ModelStorage.ParameterValue> isHeavyAllValues = updatedModel.getParameterValuesList().stream()
                .filter(pv -> pv.getParamId() == heavyGoodMboParamId)
                .collect(Collectors.toList());

            softly.assertThat(isHeavyAllValues).hasSize(1);
        });
    }

    @Test
    public void whenMskuUpdateShouldUpdateCargoTypeFromNullToTrue2() {
        // Same as before but model has empty param now.
        long heavyGoodMboParamId = allCargoTypes.get(KnownMdmParams.HEAVY_GOOD).getExternals().getMboParamId();
        TestData testData = generateTestData(1).get(0);
        ModelStorage.Model model = testData.getModel();
        CommonMsku commonMsku = testData.getMdmCommonMsku();
        commonMsku.addParamValue((MskuParamValue) new MskuParamValue()
            .setMskuId(commonMsku.getMskuId())
            .setMdmParamId(KnownMdmParams.HEAVY_GOOD)
            .setBool(true)
        );

        ModelStorage.ParameterValue isHeavyGood = ParameterValue.newBuilder()
            .setParamId(heavyGoodMboParamId)
            .setTypeId(ValueType.BOOLEAN_VALUE)
            .setValueType(ValueType.BOOLEAN)
            .setXslName("cargoType300")
            .setValueSource(ModificationSource.AUTO)
            .build();
        testData.setModel(withParameterValue(model, isHeavyGood));
        insertTestData(testData);

        processMskuToMboQueueAfterCommit();

        ModelStorage.Model updatedModel = loadModelFromStorage(model);

        SoftAssertions.assertSoftly(softly -> {
            ModelStorage.ParameterValue updatedIsHeavy = getParamValues(updatedModel)
                .get(heavyGoodMboParamId);

            softly.assertThat(updatedIsHeavy.getValueSource())
                .isEqualTo(ModelStorage.ModificationSource.MDM);

            softly.assertThat(updatedIsHeavy.getBoolValue())
                .isEqualTo(true);

            List<ModelStorage.ParameterValue> isHeavyAllValues = updatedModel.getParameterValuesList().stream()
                .filter(pv -> pv.getParamId() == heavyGoodMboParamId)
                .collect(Collectors.toList());

            softly.assertThat(isHeavyAllValues).hasSize(1);
        });
    }

    @Test
    public void whenMskuUpdateShouldUpdateCargoTypeFromFalseToTrue() {
        TestData testData = generateTestData(1).get(0);
        ModelStorage.Model model = testData.getModel();
        CommonMsku commonMsku = testData.getMdmCommonMsku();
        commonMsku.addParamValue((MskuParamValue) new MskuParamValue()
            .setMskuId(commonMsku.getMskuId())
            .setMdmParamId(KnownMdmParams.HEAVY_GOOD)
            .setBool(true)
        );
        long heavyGoodMboParamId = allCargoTypes.get(KnownMdmParams.HEAVY_GOOD).getExternals().getMboParamId();

        ModelStorage.ParameterValue isHeavyGood = ParameterValue.newBuilder()
            .setParamId(heavyGoodMboParamId)
            .setTypeId(ValueType.BOOLEAN_VALUE)
            .setValueType(ValueType.BOOLEAN)
            .setBoolValue(false)
            .setXslName("cargoType300")
            .setValueSource(ModificationSource.AUTO)
            .build();
        testData.setModel(withParameterValue(model, isHeavyGood));
        insertTestData(testData);

        processMskuToMboQueueAfterCommit();

        ModelStorage.Model updatedModel = loadModelFromStorage(model);

        SoftAssertions.assertSoftly(softly -> {
            ModelStorage.ParameterValue updatedIsHeavy = getParamValues(updatedModel)
                .get(heavyGoodMboParamId);

            softly.assertThat(updatedIsHeavy.getValueSource())
                .isEqualTo(ModelStorage.ModificationSource.MDM);

            softly.assertThat(updatedIsHeavy.getBoolValue())
                .isEqualTo(true);

            List<ModelStorage.ParameterValue> isHeavyAllValues = updatedModel.getParameterValuesList().stream()
                .filter(pv -> pv.getParamId() == heavyGoodMboParamId)
                .collect(Collectors.toList());

            softly.assertThat(isHeavyAllValues).hasSize(1);
        });
    }

    @Test
    public void whenUpdatingMskuShouldNotCreateDuplicatedParameterValues() {
        TestData testData = generateTestData(1).get(0);
        ModelStorage.Model model = testData.getModel();
        CommonMsku mdmCommonMsku = testData.getMdmCommonMsku();
        boolean mdmExpirDate = mdmCommonMsku.getParamValue(KnownMdmParams.EXPIR_DATE)
            .flatMap(MdmParamValue::getBool)
            .orElse(false);

        ModelStorage.ParameterValue expirDate = KnownMdmMboParams.getShelfLifeRequiredParameterValue(
            !mdmExpirDate,
            ModificationSource.MDM,
            MBO_USER_ID
        );

        testData.setModel(withParameterValue(model, expirDate));

        insertTestData(testData);

        processMskuToMboQueueAfterCommit();

        ModelStorage.Model updatedModel = loadModelFromStorage(model);

        SoftAssertions.assertSoftly(softly -> {
            ModelStorage.ParameterValue updatedExpirDate = getParamValues(updatedModel)
                .get(KnownMdmMboParams.EXPIR_DATE_PARAM_ID);

            softly.assertThat(updatedExpirDate.getValueSource()).isEqualTo(ModelStorage.ModificationSource.MDM);
            softly.assertThat(updatedExpirDate.getBoolValue()).isEqualTo(mdmExpirDate);

            List<ModelStorage.ParameterValue> expirDateValues = updatedModel.getParameterValuesList().stream()
                .filter(pv -> pv.getParamId() == KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                .collect(Collectors.toList());

            softly.assertThat(expirDateValues).hasSize(1);
        });
    }

    @Test
    public void whenUpdatesMskuShouldNotRewriteOperatorFilled() {
        TestData testData = generateTestData(1).get(0);
        ModelStorage.Model model = testData.getModel();

        Set<Long> connectedIds = MskuParamEnrichServiceImpl.CONNECTED_PARAMETERS_LIST.stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        insertTestData(testData);

        processMskuToMboQueueAfterCommit();
        ModelStorage.Model updated1Model = loadModelFromStorage(model);

        List<ModelStorage.ParameterValue> updated1Params = updated1Model.getParameterValuesList().stream()
            .filter(x -> connectedIds.contains(x.getParamId()))
            .collect(Collectors.toList());

        CommonMsku mdmCommonMsku = testData.getMdmCommonMsku();
        incTimeAndRandUnit(mdmCommonMsku, KnownMdmParams.SHELF_LIFE, KnownMdmParams.SHELF_LIFE_UNIT);
        incTimeAndRandUnit(mdmCommonMsku, KnownMdmParams.LIFE_TIME, KnownMdmParams.LIFE_TIME_UNIT);
        incTimeAndRandUnit(mdmCommonMsku, KnownMdmParams.GUARANTEE_PERIOD, KnownMdmParams.GUARANTEE_PERIOD_UNIT);
        insertTestData(testData);

        mboModelsServiceMock.saveModels(Collections.singleton(
            withParameterValue(updated1Model, updated1Params.stream()
                .map(parameterValue -> parameterValue.toBuilder()
                    .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                    .build())
                .toArray(ModelStorage.ParameterValue[]::new))));

        processMskuToMboQueueAfterCommit();
        ModelStorage.Model updated2Model = loadModelFromStorage(model);
        List<ModelStorage.ParameterValue> updated2Params = updated2Model.getParameterValuesList().stream()
            .filter(x -> connectedIds.contains(x.getParamId()))
            .collect(Collectors.toList());

        updated1Params = updated1Params.stream()
            .map(x -> x.toBuilder().clearModificationDate().clearValueSource().build())
            .sorted(Comparator.comparing(ModelStorage.ParameterValue::getXslName))
            .collect(Collectors.toList());
        updated2Params = updated2Params.stream()
            .map(x -> x.toBuilder().clearModificationDate().clearValueSource().build())
            .sorted(Comparator.comparing(ModelStorage.ParameterValue::getXslName))
            .collect(Collectors.toList());

        List<ModelStorage.ParameterValue> finalUpdated1Params = updated1Params;
        List<ModelStorage.ParameterValue> finalUpdated2Params = updated2Params;

        SoftAssertions.assertSoftly(softly -> {
            for (int i = 0; i < Math.max(finalUpdated1Params.size(), finalUpdated2Params.size()); i++) {
                ModelStorage.ParameterValue pv1 = finalUpdated1Params.get(i);
                ModelStorage.ParameterValue pv2 = finalUpdated2Params.get(i);
                softly.assertThat(pv1)
                    .isEqualTo(pv2);
            }
        });
    }

    @Test
    public void whenUpdatesMskuShouldRewriteNonOperatorFilled() {
        TestData testData = generateTestData(1).get(0);
        ModelStorage.Model model = testData.getModel();

        Set<Long> connectedIds = MskuParamEnrichServiceImpl.CONNECTED_PARAMETERS_LIST.stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        insertTestData(testData);

        processMskuToMboQueueAfterCommit();
        ModelStorage.Model updated1Model = loadModelFromStorage(model);

        List<ModelStorage.ParameterValue> updated1Params = updated1Model.getParameterValuesList().stream()
            .filter(x -> connectedIds.contains(x.getParamId()))
            .collect(Collectors.toList());

        CommonMsku mdmCommonMsku = testData.getMdmCommonMsku();
        incTimeAndRandUnit(mdmCommonMsku, KnownMdmParams.SHELF_LIFE, KnownMdmParams.SHELF_LIFE_UNIT);
        incTimeAndRandUnit(mdmCommonMsku, KnownMdmParams.LIFE_TIME, KnownMdmParams.LIFE_TIME_UNIT);
        incTimeAndRandUnit(mdmCommonMsku, KnownMdmParams.GUARANTEE_PERIOD, KnownMdmParams.GUARANTEE_PERIOD_UNIT);
        insertTestData(testData);

        processMskuToMboQueueAfterCommit();
        ModelStorage.Model updated2Model = loadModelFromStorage(model);
        List<ModelStorage.ParameterValue> updated2Params = updated2Model.getParameterValuesList().stream()
            .filter(x -> connectedIds.contains(x.getParamId()))
            .collect(Collectors.toList());

        updated1Params = updated1Params.stream()
            .map(x -> x.toBuilder().clearModificationDate().clearValueSource().build())
            .sorted(Comparator.comparing(ModelStorage.ParameterValue::getXslName))
            .collect(Collectors.toList());
        updated2Params = updated2Params.stream()
            .map(x -> x.toBuilder().clearModificationDate().clearValueSource().build())
            .sorted(Comparator.comparing(ModelStorage.ParameterValue::getXslName))
            .collect(Collectors.toList());

        List<ModelStorage.ParameterValue> finalUpdated1Params = updated1Params;
        List<ModelStorage.ParameterValue> finalUpdated2Params = updated2Params;

        SoftAssertions.assertSoftly(softly -> {
            for (int i = 0; i < Math.max(finalUpdated1Params.size(), finalUpdated2Params.size()); i++) {
                ModelStorage.ParameterValue pv1 =
                    updateParameterFromMdmCommonMsku(finalUpdated1Params.get(i), mdmCommonMsku);
                ModelStorage.ParameterValue pv2 = finalUpdated2Params.get(i);
                softly.assertThat(equalsIgnoringModificationSource(pv1, pv2))
                    .withFailMessage("PV1 %s, PV2 %s", pv1, pv2)
                    .isTrue();
            }
        });
    }

    @Test
    public void whenUpdatesMskuShouldNotRewriteIfAtLeastOneOfConnectedParamsIsOperatorFilled() {
        TestData testData = generateTestData(1).get(0);
        ModelStorage.Model model = testData.getModel();

        Set<Long> connectedIds = MskuParamEnrichServiceImpl.CONNECTED_PARAMETERS_LIST.stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        insertTestData(testData);

        processMskuToMboQueueAfterCommit();
        ModelStorage.Model updated1Model = loadModelFromStorage(model);

        List<ModelStorage.ParameterValue> updated1Params = updated1Model.getParameterValuesList().stream()
            .filter(x -> connectedIds.contains(x.getParamId()))
            .collect(Collectors.toList());

        CommonMsku mdmCommonMsku = testData.getMdmCommonMsku();
        incTimeAndRandUnit(mdmCommonMsku, KnownMdmParams.SHELF_LIFE, KnownMdmParams.SHELF_LIFE_UNIT);
        incTimeAndRandUnit(mdmCommonMsku, KnownMdmParams.LIFE_TIME, KnownMdmParams.LIFE_TIME_UNIT);
        incTimeAndRandUnit(mdmCommonMsku, KnownMdmParams.GUARANTEE_PERIOD, KnownMdmParams.GUARANTEE_PERIOD_UNIT);
        insertTestData(testData);

        mboModelsServiceMock.saveModels(Collections.singleton(
            withParameterValue(updated1Model, updated1Params.stream()
                .map(UploadMskuMasterDataToMskuExecutorTest::maybeMarkAsOperatorFilled)
                .toArray(ModelStorage.ParameterValue[]::new))));

        processMskuToMboQueueAfterCommit();
        ModelStorage.Model updated2Model = loadModelFromStorage(model);
        List<ModelStorage.ParameterValue> updated2Params = updated2Model.getParameterValuesList().stream()
            .filter(x -> connectedIds.contains(x.getParamId()))
            .collect(Collectors.toList());

        Assertions.assertThat(updated1Model.getParameterValuesList())
            .hasSameSizeAs(updated2Model.getParameterValuesList());

        updated1Params = updated1Params.stream()
            .map(x -> x.toBuilder().clearModificationDate().clearValueSource().build())
            .sorted(Comparator.comparing(ModelStorage.ParameterValue::getXslName))
            .collect(Collectors.toList());
        updated2Params = updated2Params.stream()
            .map(x -> x.toBuilder().clearModificationDate().clearValueSource().build())
            .sorted(Comparator.comparing(ModelStorage.ParameterValue::getXslName))
            .collect(Collectors.toList());

        List<ModelStorage.ParameterValue> finalUpdated1Params = updated1Params;
        List<ModelStorage.ParameterValue> finalUpdated2Params = updated2Params;

        SoftAssertions.assertSoftly(softly -> {
            for (int i = 0; i < Math.max(finalUpdated1Params.size(), finalUpdated2Params.size()); i++) {
                ModelStorage.ParameterValue pv1 = finalUpdated1Params.get(i);
                ModelStorage.ParameterValue pv2 = finalUpdated2Params.get(i);

                softly.assertThat(pv1)
                    .isEqualTo(pv2);
            }
        });
    }

    private ModelStorage.OperationStatus operationStatus(ModelStorage.Model model,
                                                         ModelStorage.OperationStatusType status) {
        return ModelStorage.OperationStatus.newBuilder()
            .setType(ModelStorage.OperationType.CHANGE)
            .setStatus(status)
            .setModel(model)
            .setModelId(model.getId())
            .build();
    }

    private ModelStorage.Model loadModelFromStorage(ModelStorage.Model model) {
        List<ModelStorage.Model> models = mboModelsServiceMock.loadRawModels(
            model.getCategoryId(),
            Collections.singleton(model.getId())
        );

        if (models.size() > 1) {
            throw new IllegalStateException("Only single model should be returned");
        }

        return models.stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Model storage does not return test model"));
    }

    private ModelStorage.ParameterValue generateNonMdmParameterValue(String numericValue) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(NON_MDM_PARAMETER)
            .setValueSource(ModelStorage.ModificationSource.AUTO)
            .setNumericValue(numericValue)
            .build();
    }

    private List<TestData> generateTestData(int count) {
        EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(defaultRandom.nextLong())
            .randomize(TestData.class, new TestDataRandomizer(defaultRandom))
            .build();

        return enhancedRandom.objects(TestData.class, count).collect(Collectors.toList());
    }

    private void insertTestData(TestData testData) {
        insertTestData(Collections.singletonList(testData));
    }

    private void insertTestData(List<TestData> data) {
        mskuRepository.insertOrUpdateMskus(
            data.stream().map(TestData::getMdmCommonMsku).collect(Collectors.toList())
        );
        mboModelsServiceMock.saveModels(
            data.stream().map(TestData::getModel).collect(Collectors.toList())
        );
        mskuToMboQueueRepository.enqueueAll(
            data.stream()
                .map(TestData::getModel)
                .map(ModelStorage.Model::getId)
                .collect(Collectors.toList()),
            MdmEnqueueReason.DEFAULT
        );
    }

    private static class TestData {
        private final CommonMsku mdmCommonMsku;
        private ModelStorage.Model model;

        TestData(CommonMsku mdmCommonMsku, ModelStorage.Model model) {
            this.mdmCommonMsku = mdmCommonMsku;
            this.model = model;
        }

        public CommonMsku getMdmCommonMsku() {
            return mdmCommonMsku;
        }

        public ModelStorage.Model getModel() {
            return model;
        }

        public void setModel(ModelStorage.Model model) {
            this.model = model;
        }
    }

    private class TestDataRandomizer extends AbstractRandomizer<TestData> {

        private final EnhancedRandom enhancedRandom;

        TestDataRandomizer(EnhancedRandom defaultRandom) {
            super(defaultRandom.nextLong());
            this.enhancedRandom = TestDataUtils.defaultRandomBuilder(defaultRandom.nextLong())
                .randomize(LocalizedString.class, new LocalizedStringRandomizer(defaultRandom.nextLong()))
                .build();
        }

        @Override
        public TestData getRandomValue() {
            Model model = TestDataUtils.generateValidModel(enhancedRandom)
                .setModelType(Model.ModelType.SKU);

            ModelStorage.Model protoModel = ModelConverter.reverseConvert(model).toBuilder()
                .setDeleted(false)
                .setPublished(true)
                .build();

            List<Long> paramIds = new ArrayList<>(TEST_MSKU_PARAMS);
            paramIds.addAll(allCargoTypes.keySet());
            List<MskuParamValue> mskuParamValues =
                TestMdmParamUtils.createRandomMdmParamValues(enhancedRandom, mdmParamCache.find(paramIds))
                    .values().stream()
                    .map(mdmParamValue -> {
                        MskuParamValue mskuParamValue = new MskuParamValue().setMskuId(model.getId());
                        mdmParamValue.copyTo(mskuParamValue);
                        return mskuParamValue;
                    })
                    .collect(Collectors.toList());
            CommonMsku commonMsku = new CommonMsku(model.getId(), mskuParamValues);
            for (long numericStringParam : NUMERIC_STRING_PARAMS) {
                commonMsku.getParamValue(numericStringParam)
                    .ifPresent(param -> {
                        param.setNumerics(List.of());
                        param.setString(String.valueOf(enhancedRandom.nextInt(0x10000000)));
                    });
            }
            KnownMdmParams.TIME_UNIT_BY_VALUE.forEach((valueParamId, unitParamId) -> {
                boolean isUnlimited = commonMsku.getParamValue(unitParamId)
                    .flatMap(MdmParamValue::getOption)
                    .map(MdmParamOption::getId)
                    .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                    .filter(timeUnit -> timeUnit == TimeInUnits.TimeUnit.UNLIMITED)
                    .isPresent();
                if (isUnlimited) {
                    commonMsku.getParamValue(valueParamId).ifPresent(v -> {
                        if (NUMERIC_STRING_PARAMS.contains(valueParamId)) {
                            v.setString("1");
                        }
                            v.setNumeric(BigDecimal.ONE);
                    });
                }
            });
            return new TestData(commonMsku, protoModel);
        }
    }

    private void processMskuToMboQueueAfterCommit() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        executor.execute();
    }
}
