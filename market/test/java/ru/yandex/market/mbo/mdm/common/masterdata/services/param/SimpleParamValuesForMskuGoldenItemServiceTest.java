package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenBlocksPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenSplitterMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MskuSilverSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.GlobalParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmModificationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationService;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.model.cccode.Cis;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;
import ru.yandex.market.mboc.common.masterdata.model.cccode.MdmParamMarkupState;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class SimpleParamValuesForMskuGoldenItemServiceTest extends MdmBaseDbTestClass {
    private static final long MODEL_ID = 10L;
    private static final long CATEGORY_ID = 10L;
    private static final ModelKey MODEL_KEY = new ModelKey(CATEGORY_ID, MODEL_ID);

    @Autowired
    private MdmGoodGroupRepository mdmGoodGroupRepository;
    @Autowired
    private CustomsCommCodeRepository codeRepository;
    @Autowired
    private MdmParamCache cache;
    @Autowired
    private MdmLmsCargoTypeCache mdmLmsCargoTypeCache;
    @Autowired
    private WeightDimensionBlockValidationService weightDimensionBlockValidationService;
    @Autowired
    private StorageKeyValueService keyValueService;
    @Autowired
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;

    private MskuGoldenItemService service;

    @Before
    public void setup() {
        codeRepository = new CustomsCommCodeRepositoryMock();
        CustomsCommCodeMarkupService codeMarkupService = new CustomsCommCodeMarkupServiceImpl(cache,
            codeRepository, new CCCodeValidationService(List.of(), codeRepository),
            new CategoryParamValueRepositoryMock(), new TaskQueueRegistratorMock(), mdmGoodGroupRepository,
            new MappingsCacheRepositoryMock());
        service = new MskuGoldenItemService(
            new MskuSilverSplitter(cache, sskuGoldenParamUtil),
            new MskuGoldenSplitterMerger(cache),
            new MskuGoldenSplitterMerger(cache),
            new MskuSilverItemPreProcessor(cache, mdmLmsCargoTypeCache, featureSwitchingAssistant),
            featureSwitchingAssistant,
            new MskuGoldenBlocksPostProcessor(featureSwitchingAssistant, cache, codeMarkupService, keyValueService),
            weightDimensionBlockValidationService,
            cache
        );

        cache.refresh();
    }

    @Test
    public void whenNoDataCreateOnlyDefaultHSAndMercuryParams() {
        Optional<CommonMsku> result =
            service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT, List.of(), null);
        assertThat(result.stream())
            .flatMap(CommonMsku::getValues)
            .hasSize(KnownMdmParams.MERCURY_CARGOTYPES.size() + KnownMdmParams.HONEST_SIGN_CARGOTYPES.size())
            .allMatch(pv -> !pv.getBool().orElseThrow()) // все false
            .map(MdmParamValue::getMdmParamId)
            .containsAll(KnownMdmParams.MERCURY_CARGOTYPES)
            .containsAll(KnownMdmParams.HONEST_SIGN_CARGOTYPES);
    }

    @Test
    public void whenCategoryParamPassedShouldProxyItToMsku() {
        CategoryParamValue categoryValue = (CategoryParamValue) new CategoryParamValue()
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString("00645001")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        GoldComputationContext context = createContext(List.of(categoryValue));

        Optional<CommonMsku> gold =
            service.calculateGoldenItem(MODEL_KEY, context, List.of(context), null);

        MskuParamValue expected = cloneParamValueWithNewModificationInfo(categoryValue,
            MasterDataSourceType.AUTO, MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_CATEGORY_SETTINGS);

        assertThat(gold).isPresent();
        assertThat(TestMdmParamUtils.filterCisCargoTypes(gold.get().getValues())).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void whenHaveOperatorValueKeepIt() {
        MskuParamValue mskuValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString("00645001")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        var mskuParams = generateCisCargoTypes(MODEL_ID);
        CommonMsku existingGoldenItem = new CommonMsku(MODEL_KEY, mskuParams);
        existingGoldenItem.addParamValue(mskuValue);

        Optional<CommonMsku> gold =
            service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT, List.of(), existingGoldenItem);

        assertThat(gold).isEmpty(); // No changes were made - keeping old gold.
    }

    @Test
    public void whenBothOperatorAndCategoryParamsPresentShouldPrioritizeOperatorValue() {
        CategoryParamValue categoryValue = (CategoryParamValue) new CategoryParamValue()
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString("category_value")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        GoldComputationContext context = createContext(List.of(categoryValue));

        MskuParamValue mskuValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString("msku_value")
            .setUpdatedByLogin("petya")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        var mskuParams = generateCisCargoTypes(MODEL_ID);
        CommonMsku existingGoldenItem = new CommonMsku(MODEL_KEY, mskuParams);
        existingGoldenItem.addParamValue(mskuValue);

        Optional<CommonMsku> gold =
            service.calculateGoldenItem(MODEL_KEY, context, List.of(context), existingGoldenItem);

        assertThat(gold).isEmpty(); // No changes were made - keeping old gold.
    }

    @Test
    public void whenCategoryAndGlobalParamsPresentShouldPrioritizeCategoryLevel() {
        CategoryParamValue categoryImeiMaskValue = (CategoryParamValue) new CategoryParamValue()
            .setMdmParamId(KnownMdmParams.IMEI_MASK)
            .setString("category_imei_mask_value")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        CategoryParamValue categoryImeiRequiredValue = (CategoryParamValue) new CategoryParamValue()
            .setMdmParamId(KnownMdmParams.IMEI_CONTROL)
            .setString("category_imei_required_value")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        GlobalParamValue globalImeiMaskValue = (GlobalParamValue) new GlobalParamValue()
            .setMdmParamId(KnownMdmParams.IMEI_MASK)
            .setString("global_imei_mask_value")
            .setUpdatedByLogin("petya")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.TOOL);
        GlobalParamValue globalImeiRequiredValue = (GlobalParamValue) new GlobalParamValue()
            .setMdmParamId(KnownMdmParams.IMEI_CONTROL)
            .setString("global_imei_required_value")
            .setUpdatedByLogin("petya")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.TOOL);

        GoldComputationContext context = createContext(
            List.of(categoryImeiMaskValue, categoryImeiRequiredValue),
            List.of(globalImeiMaskValue, globalImeiRequiredValue)
        );
        Optional<CommonMsku> gold = service.calculateGoldenItem(MODEL_KEY, context, List.of(context), null);

        MskuParamValue imeiRequiredExpected = cloneParamValueWithNewModificationInfo(categoryImeiRequiredValue,
            MasterDataSourceType.AUTO, MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_CATEGORY_SETTINGS);
        MskuParamValue imeiMaskExpected = cloneParamValueWithNewModificationInfo(categoryImeiMaskValue,
            MasterDataSourceType.AUTO, MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_CATEGORY_SETTINGS);

        assertThat(gold).isPresent();
        assertThat(gold.get().getValues())
            // отфильтруем дефолтные false для HS и Mercury
            .filteredOn(pv -> !KnownMdmParams.HONEST_SIGN_CARGOTYPES.contains(pv.getMdmParamId()))
            .filteredOn(pv -> !KnownMdmParams.MERCURY_CARGOTYPES.contains(pv.getMdmParamId()))
            .containsExactlyInAnyOrder(imeiRequiredExpected, imeiMaskExpected);
    }

    @Test
    public void whenOnlyGlobalParamsPresentTakeThem() {
        GlobalParamValue globalImeiMaskValue = (GlobalParamValue) new GlobalParamValue()
            .setMdmParamId(KnownMdmParams.IMEI_MASK)
            .setString("global_imei_mask_value")
            .setUpdatedByLogin("petya")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.TOOL);
        GlobalParamValue globalImeiRequiredValue = (GlobalParamValue) new GlobalParamValue()
            .setMdmParamId(KnownMdmParams.IMEI_CONTROL)
            .setString("global_imei_required_value")
            .setUpdatedByLogin("petya")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.TOOL);

        GoldComputationContext context = createContext(
            List.of(),
            List.of(globalImeiMaskValue, globalImeiRequiredValue)
        );
        Optional<CommonMsku> gold = service.calculateGoldenItem(MODEL_KEY, context, List.of(context), null);

        MskuParamValue imeiRequiredExpected = cloneParamValueWithNewModificationInfo(globalImeiRequiredValue,
            MasterDataSourceType.MDM_GLOBAL, MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_GLOBAL_SETTINGS);
        MskuParamValue imeiMaskExpected = cloneParamValueWithNewModificationInfo(globalImeiMaskValue,
            MasterDataSourceType.MDM_GLOBAL, MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_GLOBAL_SETTINGS);

        assertThat(gold).isPresent();
        assertThat(gold.get().getValues())
            // отфильтруем дефолтные false для HS и Mercury
            .filteredOn(pv -> !KnownMdmParams.HONEST_SIGN_CARGOTYPES.contains(pv.getMdmParamId()))
            .filteredOn(pv -> !KnownMdmParams.MERCURY_CARGOTYPES.contains(pv.getMdmParamId()))
            .containsExactlyInAnyOrder(imeiRequiredExpected, imeiMaskExpected);
    }

    @Test
    public void whenOldMskuValueIsAutoShouldTakeCategoryValue() {
        CategoryParamValue categoryValue = (CategoryParamValue) new CategoryParamValue()
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString("category_value")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        GoldComputationContext context = createContext(List.of(categoryValue));

        MskuParamValue mskuValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString("msku_value")
            .setUpdatedByLogin("petya")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        CommonMsku existingGoldenItem = new CommonMsku(MODEL_KEY, List.of(mskuValue));

        Optional<CommonMsku> gold =
            service.calculateGoldenItem(MODEL_KEY, context, List.of(context), existingGoldenItem);

        MskuParamValue expected = cloneParamValueWithNewModificationInfo(categoryValue, MasterDataSourceType.AUTO,
            MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_CATEGORY_SETTINGS);

        assertThat(gold).isPresent();
        assertThat(TestMdmParamUtils.filterCisCargoTypes(gold.get().getValues())).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testParamsFromGoldNotPresentOnSilverShouldDisappear() {
        MskuParamValue otherValue1 = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.GTIN)
            .setXslName("-")
            .setString("some_value1")
            .setUpdatedByLogin("petya")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue otherValue2 = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.EXPIR_DATE)
            .setXslName("-")
            .setBool(true)
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        CommonMsku existingGold = new CommonMsku(MODEL_KEY, List.of(otherValue1, otherValue2));

        CategoryParamValue categoryValue = (CategoryParamValue) new CategoryParamValue()
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString("00645001")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        GoldComputationContext context = createContext(List.of(categoryValue));

        Optional<CommonMsku> gold =
            service.calculateGoldenItem(MODEL_KEY, context, List.of(context), existingGold);

        MskuParamValue expected = cloneParamValueWithNewModificationInfo(categoryValue, MasterDataSourceType.AUTO,
            MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_CATEGORY_SETTINGS);

        assertThat(gold).isPresent();
        assertThat(TestMdmParamUtils.filterCisCargoTypes(gold.get().getValues()))
            .containsExactlyInAnyOrder(expected, otherValue2);
    }

    @Test
    public void testAdditionalCargotypesGeneratedDuringPostProcess() {
        CategoryParamValue categoryValue = (CategoryParamValue) new CategoryParamValue()
            .setCategoryId(CATEGORY_ID)
            .setXslName("-")
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString("00645")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        GoldComputationContext context = createContext(List.of(categoryValue));

        LocalDateTime activationTs = DateTimeUtils.dateTimeNow();

        CustomsCommCode codeMarkup = new CustomsCommCode()
            .setId(1)
            .setCode("00645")
            .setHonestSign(new MdmParamMarkupState().setCis(Cis.DISTINCT))
            .setTraceable(true)
            .setMercury(new MdmParamMarkupState().setCis(Cis.REQUIRED).setMarkupActivationTs(activationTs));
        codeRepository.insert(codeMarkup);

        Optional<CommonMsku> gold =
            service.calculateGoldenItem(MODEL_KEY, context, List.of(context), null);

        MskuParamValue expectedCode = cloneParamValueWithNewModificationInfo(categoryValue, MasterDataSourceType.AUTO,
            MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_CATEGORY_SETTINGS);
        MskuParamValue exptectedTraceableTrue = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.IS_TRACEABLE)
            .setBool(true)
            .setXslName(xslOf(KnownMdmParams.IS_TRACEABLE))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        MskuParamValue expectedHonestSignOptional = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_OPTIONAL)
            .setBool(false)
            .setXslName(xslOf(KnownMdmParams.HONEST_SIGN_CIS_OPTIONAL))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedHonestSignRequired = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED)
            .setBool(false)
            .setXslName(xslOf(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedHonestSignDistinct = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT)
            .setBool(true)
            .setXslName(xslOf(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryOptional = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_OPTIONAL)
            .setBool(false)
            .setXslName(xslOf(KnownMdmParams.MERCURY_CIS_OPTIONAL))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryRequired = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_REQUIRED)
            .setBool(true)
            .setXslName(xslOf(KnownMdmParams.MERCURY_CIS_REQUIRED))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryDistinct = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_DISTINCT)
            .setBool(false)
            .setXslName(xslOf(KnownMdmParams.MERCURY_CIS_DISTINCT))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryDate = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.MERCURY_ACTIVATION_TS)
            .setString(activationTs.format(TimestampUtil.MBO_DT_FORMATTER))
            .setXslName(xslOf(KnownMdmParams.MERCURY_ACTIVATION_TS))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        assertThat(gold).isPresent();
        assertThat(gold.get().getValues()).containsExactlyInAnyOrder(expectedCode, exptectedTraceableTrue,
            expectedHonestSignOptional, expectedHonestSignRequired, expectedHonestSignDistinct,
            expectedMercuryOptional, expectedMercuryRequired, expectedMercuryDistinct, expectedMercuryDate);
    }

    @Test
    public void testExistingCisValuesMergedByPrioritiesWithNewOnes() {
        CategoryParamValue categoryValue = (CategoryParamValue) new CategoryParamValue()
            .setCategoryId(CATEGORY_ID)
            .setXslName("-")
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString("00645")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        GoldComputationContext context = createContext(List.of(categoryValue));

        LocalDateTime activationTs = DateTimeUtils.dateTimeNow();

        MskuParamValue goldenHonestSignOptional = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_OPTIONAL)
            .setBool(true)
            .setXslName(xslOf(KnownMdmParams.HONEST_SIGN_CIS_OPTIONAL))
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        MskuParamValue goldenHonestSignRequired = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED)
            .setBool(true)
            .setXslName(xslOf(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED))
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);
        MskuParamValue goldenHonestSignDistinct = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT)
            .setBool(false)
            .setXslName(xslOf(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT))
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        CommonMsku existingGold = new CommonMsku(MODEL_KEY, List.of(
            goldenHonestSignOptional, goldenHonestSignRequired, goldenHonestSignDistinct
        ));

        CustomsCommCode codeMarkup = new CustomsCommCode()
            .setId(1)
            .setCode("00645")
            .setHonestSign(new MdmParamMarkupState().setCis(Cis.DISTINCT))
            .setMercury(new MdmParamMarkupState().setCis(Cis.REQUIRED).setMarkupActivationTs(activationTs));
        codeRepository.insert(codeMarkup);

        Optional<CommonMsku> gold =
            service.calculateGoldenItem(MODEL_KEY, context, List.of(context), existingGold);

        MskuParamValue expectedCode = cloneParamValueWithNewModificationInfo(categoryValue, MasterDataSourceType.AUTO,
            MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_CATEGORY_SETTINGS);
        MskuParamValue expectedHonestSignOptional = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_OPTIONAL)
            .setBool(true) // победило операторское
            .setXslName(xslOf(KnownMdmParams.HONEST_SIGN_CIS_OPTIONAL))
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        MskuParamValue expectedHonestSignRequired = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED)
            .setBool(true) // победило операторское
            .setXslName(xslOf(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED))
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);
        MskuParamValue expectedHonestSignDistinct = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT)
            .setBool(false) // победило операторское
            .setXslName(xslOf(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT))
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        MskuParamValue expectedMercuryOptional = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_OPTIONAL)
            .setBool(false)
            .setXslName(xslOf(KnownMdmParams.MERCURY_CIS_OPTIONAL))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryRequired = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_REQUIRED)
            .setBool(true)
            .setXslName(xslOf(KnownMdmParams.MERCURY_CIS_REQUIRED))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryDistinct = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_DISTINCT)
            .setBool(false)
            .setXslName(xslOf(KnownMdmParams.MERCURY_CIS_DISTINCT))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryDate = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.MERCURY_ACTIVATION_TS)
            .setString(activationTs.format(TimestampUtil.MBO_DT_FORMATTER))
            .setXslName(xslOf(KnownMdmParams.MERCURY_ACTIVATION_TS))
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        assertThat(gold).isPresent();
        assertThat(gold.get().getValues()).containsExactlyInAnyOrder(expectedCode,
            expectedHonestSignOptional, expectedHonestSignRequired, expectedHonestSignDistinct,
            expectedMercuryOptional, expectedMercuryRequired, expectedMercuryDistinct, expectedMercuryDate);
    }

    @Test
    public void testAllMskuParamValuesPresentInGoldAfterPostProcess() {
        MskuParamValue otherValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.EXPIRATION_DATES_APPLY)
            .setXslName("-")
            .setBool(true)
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        // маски sn/imei должны быть включены в golden item,
        // т.к. необходимые связанные параметры (признаки контроля sn/imei) присутствуют
        MskuParamValue imeiControl = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.IMEI_CONTROL)
            .setXslName("-")
            .setBool(false)
            .setUpdatedByLogin("petya")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        MskuParamValue imeiMask = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.IMEI_MASK)
            .setXslName("-")
            .setString("some_imei_mask")
            .setUpdatedByLogin("petya")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        MskuParamValue snControl = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.SERIAL_NUMBER_CONTROL)
            .setXslName("-")
            .setBool(true)
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        MskuParamValue snMask = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.SERIAL_NUMBER_MASK)
            .setXslName("-")
            .setString("some_sn_mask")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        CommonMsku existingItem = new CommonMsku(MODEL_KEY,
            List.of(otherValue, imeiControl, imeiMask, snControl, snMask));

        CategoryParamValue categoryValue = (CategoryParamValue) new CategoryParamValue()
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString("00645001")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        GoldComputationContext context = createContext(List.of(categoryValue));

        Optional<CommonMsku> gold =
            service.calculateGoldenItem(MODEL_KEY, context, List.of(context), existingItem);

        MskuParamValue categoryValueExpected = cloneParamValueWithNewModificationInfo(categoryValue,
            MasterDataSourceType.AUTO, MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_CATEGORY_SETTINGS);

        assertThat(gold).isPresent();
        assertThat(TestMdmParamUtils.filterCisCargoTypes(gold.get().getValues()))
            .containsExactlyInAnyOrder(categoryValueExpected, otherValue, imeiControl, imeiMask, snControl, snMask);
    }

    @Test
    public void testMskuParamValuesFilteredDuringPostProcess() {
        MskuParamValue otherValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.EXPIRATION_DATES_APPLY)
            .setXslName("-")
            .setBool(true)
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        // не должен быть включен в golden item, т.к. отсутствует признак "контроль imei"
        MskuParamValue imeiMask = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.IMEI_MASK)
            .setXslName("-")
            .setString("some_imei_mask")
            .setUpdatedByLogin("petya")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        // не должен быть включен в golden item, т.к. отсутствует признак "контроль sn"
        MskuParamValue snMask = (MskuParamValue) new MskuParamValue()
            .setMskuId(MODEL_ID)
            .setMdmParamId(KnownMdmParams.SERIAL_NUMBER_MASK)
            .setXslName("-")
            .setString("some_sn_mask")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        CommonMsku existingGold = new CommonMsku(MODEL_KEY, List.of(otherValue, imeiMask, snMask));

        CategoryParamValue categoryValue = (CategoryParamValue) new CategoryParamValue()
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString("00645001")
            .setUpdatedByLogin("vasia")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        GoldComputationContext context = createContext(List.of(categoryValue));

        Optional<CommonMsku> gold =
            service.calculateGoldenItem(MODEL_KEY, context, List.of(context), existingGold);

        MskuParamValue categoryValueExpected = cloneParamValueWithNewModificationInfo(categoryValue,
            MasterDataSourceType.AUTO, MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_CATEGORY_SETTINGS);

        assertThat(gold).isPresent();
        assertThat(TestMdmParamUtils.filterCisCargoTypes(gold.get().getValues()))
            .containsExactlyInAnyOrder(categoryValueExpected);
    }

    @Test
    public void testMboOperatorBeatsCategoryAndGlobal() {
        // given
        CustomsCommCode codeMarkup00645 = new CustomsCommCode()
            .setId(1)
            .setCode("00645")
            .setHonestSign(new MdmParamMarkupState().setCis(Cis.DISTINCT))
            .setTraceable(true);
        CustomsCommCode codeMarkup00945 = new CustomsCommCode()
            .setId(1)
            .setCode("00945")
            .setHonestSign(new MdmParamMarkupState().setCis(Cis.REQUIRED))
            .setTraceable(false);
        codeRepository.insertBatch(codeMarkup00645, codeMarkup00945);

        List<CategoryParamValue> categoryParamValues = List.of(
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(CATEGORY_ID)
                .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
                .setXslName(xslOf(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX))
                .setString("00645")
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR),
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(CATEGORY_ID)
                .setMdmParamId(KnownMdmParams.SERIAL_NUMBER_MASK)
                .setXslName(xslOf(KnownMdmParams.SERIAL_NUMBER_MASK))
                .setString("category_sn_mask_value")
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR),
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(CATEGORY_ID)
                .setMdmParamId(KnownMdmParams.SERIAL_NUMBER_CONTROL)
                .setXslName(xslOf(KnownMdmParams.SERIAL_NUMBER_CONTROL))
                .setBool(true)
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
        );

        List<GlobalParamValue> globalParamValues = List.of(
            (GlobalParamValue) new GlobalParamValue()
                .setMdmParamId(KnownMdmParams.IMEI_MASK)
                .setXslName(xslOf(KnownMdmParams.IMEI_MASK))
                .setString("global_imei_mask_value")
                .setMasterDataSourceType(MasterDataSourceType.TOOL),
            (GlobalParamValue) new GlobalParamValue()
                .setMdmParamId(KnownMdmParams.IMEI_CONTROL)
                .setXslName(xslOf(KnownMdmParams.IMEI_CONTROL))
                .setBool(true)
                .setMasterDataSourceType(MasterDataSourceType.TOOL)
        );

        CommonMsku existingMsku = new CommonMsku(
            MODEL_KEY,
            List.of(
                (MskuParamValue) new MskuParamValue()
                    .setMskuId(MODEL_ID)
                    .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
                    .setXslName(xslOf(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX))
                    .setString("00945")
                    .setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR),
                (MskuParamValue) new MskuParamValue()
                    .setMskuId(MODEL_ID)
                    .setMdmParamId(KnownMdmParams.SERIAL_NUMBER_MASK)
                    .setXslName(xslOf(KnownMdmParams.SERIAL_NUMBER_MASK))
                    .setString("mbo_operator_sn_mask_value")
                    .setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR),
                (MskuParamValue) new MskuParamValue()
                    .setMskuId(MODEL_ID)
                    .setMdmParamId(KnownMdmParams.SERIAL_NUMBER_CONTROL)
                    .setXslName(xslOf(KnownMdmParams.SERIAL_NUMBER_CONTROL))
                    .setBool(true)
                    .setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR),
                (MskuParamValue) new MskuParamValue()
                    .setMskuId(MODEL_ID)
                    .setMdmParamId(KnownMdmParams.IMEI_MASK)
                    .setXslName(xslOf(KnownMdmParams.IMEI_MASK))
                    .setString("mbo_operator_imei_mask_value")
                    .setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR),
                (MskuParamValue) new MskuParamValue()
                    .setMskuId(MODEL_ID)
                    .setMdmParamId(KnownMdmParams.IMEI_CONTROL)
                    .setXslName(xslOf(KnownMdmParams.IMEI_CONTROL))
                    .setBool(true)
                    .setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR)
            )
        );

        GoldComputationContext context = createContext(categoryParamValues, globalParamValues);

        // when
        CommonMsku calculationResult =
            service.calculateGoldenItem(MODEL_KEY, context, List.of(), existingMsku).orElseThrow();

        // then
        Assertions.assertThat(removeTraceable(TestMdmParamUtils.filterCisCargoTypes(calculationResult)))
            .isEqualTo(existingMsku);
        // ЧЗ разметка сгенерировалась по префиксу от mbo оператора:
        Assertions.assertThat(calculationResult.getParamValue(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED))
            .flatMap(MdmParamValue::getBool)
            .contains(true);
        Assertions.assertThat(calculationResult.getParamValue(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT))
            .flatMap(MdmParamValue::getBool)
            .contains(false);
        Assertions.assertThat(calculationResult.getParamValue(KnownMdmParams.IS_TRACEABLE))
            .flatMap(MdmParamValue::getBool)
            .contains(false);
    }

    @Test
    public void testMboOperatorBeatsAutogeneratedHS() {
        // given
        CustomsCommCode codeMarkup00645 = new CustomsCommCode()
            .setId(1)
            .setCode("00645")
            .setHonestSign(new MdmParamMarkupState().setCis(Cis.REQUIRED))
            .setTraceable(true);
        codeRepository.insert(codeMarkup00645);

        List<CategoryParamValue> categoryParamValues = List.of(
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(CATEGORY_ID)
                .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
                .setXslName(xslOf(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX))
                .setString("00645")
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)

        );

        CommonMsku existingMsku = new CommonMsku(
            MODEL_KEY,
            List.of(
                (MskuParamValue) new MskuParamValue()
                    .setMskuId(MODEL_ID)
                    .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED)
                    .setXslName(xslOf(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED))
                    .setBool(false)
                    .setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR)
            )
        );

        GoldComputationContext context = createContext(categoryParamValues);

        // when
        CommonMsku calculationResult =
            service.calculateGoldenItem(MODEL_KEY, context, List.of(), existingMsku).orElseThrow();

        // then
        Assertions.assertThat(calculationResult.getParamValue(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED))
            .flatMap(MdmParamValue::getBool)
            .contains(false);
    }

    private GoldComputationContext createContext(List<CategoryParamValue> categoryParamValues) {
        return createContext(categoryParamValues, List.of());
    }

    private GoldComputationContext createContext(List<CategoryParamValue> categoryParamValues,
                                                 List<GlobalParamValue> globalParamValues) {

        return new GoldComputationContext(
            CATEGORY_ID,
            List.of(),
            Map.of(),
            Map.of(),
            categoryParamValues.stream()
                .collect(Collectors.toMap(CategoryParamValue::getMdmParamId, Function.identity())),
            globalParamValues.stream()
                .collect(Collectors.toMap(GlobalParamValue::getMdmParamId, Function.identity())),
            Map.of(), Set.of()
        );
    }

    private List<MskuParamValue> generateCisCargoTypes(Long mskuId) {
        MskuParamValue expectedMercuryCisOptional = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_OPTIONAL)
            .setXslName("mercuryOptionalStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryCisDistinct = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_DISTINCT)
            .setXslName("mercuryDistinctStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryCisRequired = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_REQUIRED)
            .setXslName("mercuryRequiredStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        MskuParamValue expectedHsCisOptional = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_OPTIONAL)
            .setXslName("cargoType990")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedHsCisDistinct = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT)
            .setXslName("cargoType985")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedHsCisRequired = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED)
            .setXslName("cargoType980")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        return List.of(expectedMercuryCisOptional, expectedMercuryCisDistinct, expectedMercuryCisRequired,
            expectedHsCisOptional, expectedHsCisDistinct, expectedHsCisRequired);
    }

    private MskuParamValue cloneParamValueWithNewModificationInfo(MdmParamValue mdmParamValue,
                                                                  MasterDataSourceType sourceType,
                                                                  String sourceId) {
        MskuParamValue result = new MskuParamValue();
        mdmParamValue.copyTo(result);

        MdmModificationInfo modificationInfo = new MdmModificationInfo()
            .setMasterDataSourceId(sourceId)
            .setMasterDataSourceType(sourceType);
        result.setMskuId(MODEL_ID)
            .setModificationInfo(modificationInfo);
        return result;
    }

    private String xslOf(long id) {
        return cache.get(id).getXslName();
    }

    private static CommonMsku removeTraceable(CommonMsku msku) {
        return new CommonMsku(
            msku.getKey(),
            msku.getValues().stream()
                .filter(pv -> pv.getMdmParamId() != KnownMdmParams.IS_TRACEABLE)
                .collect(Collectors.toList())
        );
    }
}
