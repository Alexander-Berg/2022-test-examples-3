package ru.yandex.market.mbo.mdm.tms.executors;

import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenBlocksPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenSplitterMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MskuSilverSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToMboQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.GlobalParamValueService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.WarehouseProjectionCacheImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuCalculatingProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuCalculatingProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingDataProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingPipeProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingPipeProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuSskuWithPriorityProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuSskuWithPriorityProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.RecomputeMskuGoldServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmLmsCargoTypeCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuParamEnrichService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfoRepository;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.MdmParameterValueCachingServiceMock;
import ru.yandex.market.mbo.mdm.common.service.mapping.MdmBestMappingsProvider;
import ru.yandex.market.mbo.mdm.common.service.queue.MskuToMboQueueService;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessMskuQueueService;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmDbWithCleaningTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.CargoType;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.MskuSyncResult;
import ru.yandex.market.mboc.common.masterdata.model.cccode.Cis;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;
import ru.yandex.market.mboc.common.masterdata.model.cccode.MdmParamMarkupState;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.MskuSyncResultRepository;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsService;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsServiceImpl;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsServiceMock;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComputeCargotypesAndSendToMboTest extends MdmDbWithCleaningTestClass {
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private StorageKeyValueService keyValueService;
    @Autowired
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    @Autowired
    private MskuGoldenSplitterMerger mskuGoldenSplitterMerger;
    @Autowired
    private MskuSilverItemPreProcessor mskuSilverItemPreProcessor;
    @Autowired
    private CustomsCommCodeRepository customsCommCodeRepository;
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private MappingsCacheRepository mappingsCache;
    @Autowired
    private MdmGoodGroupRepository mdmGoodGroupRepository;
    @Autowired
    private CargoTypeRepository cargoTypeRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private GlobalParamValueService globalParamValueService;
    @Autowired
    private PriceInfoRepository priceInfoRepository;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private MskuToRefreshRepository mskuToRefreshQueue;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MskuParamEnrichService mskuParamEnrichService;
    @Autowired
    private MskuToMboQueueRepository mskuToMboQueue;
    @Autowired
    private MskuSyncResultRepository mskuSyncResultRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MdmLmsCargoTypeCache mdmLmsCargoTypeCache;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    private MdmBestMappingsProvider mdmBestMappingsProvider;
    @Autowired
    protected WeightDimensionsValidator weightDimensionsValidator;

    private RecomputeMskuGoldExecutor recomputeMskuGoldExecutor;
    private UploadMskuMasterDataToMskuExecutor sendToMboExecutor;
    private MboModelsServiceMock mboModelsService;


    private static MasterDataValidator alwaysOkMasterDataValidator() {
        MasterDataValidator masterDataValidator = mock(MasterDataValidator.class);
        when(masterDataValidator.validateMasterData(any(MasterData.class))).thenReturn(List.of());
        return masterDataValidator;
    }

    @Before
    public void setUp() {
        MasterDataValidator masterDataValidator = alwaysOkMasterDataValidator();
        MskuSilverSplitter mskuSilverSplitter = new MskuSilverSplitter(mdmParamCache, sskuGoldenParamUtil);
        CustomsCommCodeMarkupService markupService = new CustomsCommCodeMarkupServiceImpl(
            mdmParamCache,
            customsCommCodeRepository,
            new CCCodeValidationService(List.of(), customsCommCodeRepository), categoryParamValueRepository,
            new TaskQueueRegistratorMock(),
            mdmGoodGroupRepository,
            mappingsCache
        );
        MskuGoldenBlocksPostProcessor mskuGoldenBlocksPostProcessor =
            new MskuGoldenBlocksPostProcessor(featureSwitchingAssistant, mdmParamCache, markupService, keyValueService);
        WeightDimensionBlockValidationServiceImpl validationService = new WeightDimensionBlockValidationServiceImpl(
            new CachedItemBlockValidationContextProviderImpl(keyValueService),
            weightDimensionsValidator
        );
        MskuGoldenItemService mskuGIS = new MskuGoldenItemService(
            mskuSilverSplitter,
            mskuGoldenSplitterMerger,
            mskuGoldenSplitterMerger,
            mskuSilverItemPreProcessor,
            featureSwitchingAssistant,
            mskuGoldenBlocksPostProcessor,
            validationService,
            mdmParamCache
        );
        MdmCategorySettingsService categorySettingsService = new MdmCategorySettingsServiceImpl(
            new MdmParameterValueCachingServiceMock(),
            cargoTypeRepository,
            categoryParamValueRepository
        );
        MskuProcessingDataProviderImpl assistant = new MskuProcessingDataProviderImpl(
            mskuRepository,
            categoryParamValueRepository,
            categorySettingsService,
            masterDataRepository,
            globalParamValueService,
            goldSskuRepository,
            keyValueService,
            priceInfoRepository,
            Mockito.mock(WarehouseProjectionCacheImpl.class),
            mdmParamCache,
            mdmBestMappingsProvider
        );

        RecomputeMskuGoldServiceImpl recomputeMskuGoldService = new RecomputeMskuGoldServiceImpl(
            assistant,
            mskuProcessingPipeProcessor(),
            mskuCalculatingProcessor(mskuGIS, masterDataValidator));

        ProcessMskuQueueService processMskuQueueService = new ProcessMskuQueueService(mskuToRefreshQueue,
            new StorageKeyValueServiceMock(),
            recomputeMskuGoldService);
        recomputeMskuGoldExecutor = new RecomputeMskuGoldExecutor(processMskuQueueService);

        mboModelsService = new MboModelsServiceMock();
        MskuToMboQueueService mskuToMboQueueService = new MskuToMboQueueService(
            mboModelsService,
            keyValueService,
            mskuParamEnrichService,
            mskuToMboQueue,
            mskuRepository,
            mskuSyncResultRepository,
            transactionTemplate
        );
        sendToMboExecutor = new UploadMskuMasterDataToMskuExecutor(mskuToMboQueueService);
    }

    private MskuProcessingPipeProcessor mskuProcessingPipeProcessor() {
        return new MskuProcessingPipeProcessorImpl(mdmQueuesManager, mskuSskuWithPriorityProvider());
    }

    private MskuSskuWithPriorityProvider mskuSskuWithPriorityProvider() {
        return new MskuSskuWithPriorityProviderImpl(mdmSskuGroupManager);
    }

    private MskuCalculatingProcessor mskuCalculatingProcessor(MskuGoldenItemService mskuGoldenItemService,
                                                              MasterDataValidator masterDataValidator) {
        return new MskuCalculatingProcessorImpl(mskuRepository, mskuGoldenItemService, masterDataValidator);
    }


    @Test
    public void whenSimpleCargotypeCategoryValueChangedFromTrueToFalseShouldChangeMboModelValueFromTrueToFalse() {
        // Наш оффер
        mdmSupplierRepository.insertOrUpdate(
            new MdmSupplier()
                .setId(1945)
                .setName("USA")
                .setType(MdmSupplierType.THIRD_PARTY)
        );
        ShopSkuKey offer = new ShopSkuKey(1945, "Uranium-235");
        long mskuId = 789L;

        // Категория, на которой проставлен карготип бытовая химия
        long categoryId = 123L;
        categoryParamValueRepository.insertOrUpdate(
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(categoryId)
                .setMdmParamId(KnownMdmParams.HOUSEHOLD_CHEMICALS)
                .setBool(true)
        );

        // В МБО пустая модель
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(mskuId)
            .setCategoryId(categoryId)
            .setDeleted(false)
            .setPublished(true)
            .setCurrentType(ModelStorage.ModelType.SKU.name())
            .build();
        mboModelsService.saveModels(List.of(model));

        // Загрузим маппинг
        updateMapping(offer, mskuId, categoryId);

        // Закинем в очередь msku_to_refresh и запустим пересчет
        mskuToRefreshQueue.enqueue(mskuId, MdmEnqueueReason.DEVELOPER_TOOL);
        commitAndRecomputeMsku();

        // Проверим, что карготип простваился в МДМ
        Assertions.assertThat(mskuRepository.findMsku(mskuId))
            .flatMap(commonMsku -> commonMsku.getParamValue(KnownMdmParams.HOUSEHOLD_CHEMICALS))
            .flatMap(MdmParamValue::getBool)
            .contains(true);

        // Проверим, что msku попала в очередь на отправку в mbo
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(1);
        Assertions.assertThat(mskuToMboQueue.getUnprocessedBatch(1))
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactly(mskuId);

        // Запустим отправку в МБО
        commitAndSendToMbo();
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(0);

        // Проверим, что модель обновилась. Карготип стал true.
        Assertions.assertThat(mskuSyncResultRepository.findById(mskuId).getStatus())
            .isEqualTo(MskuSyncResult.MskuSyncStatus.OK);
        Boolean cargoTypeValueInMbo = loadModelFromStorage(mskuId).getParameterValuesList().stream()
            .filter(parameterValue -> parameterValue.getParamId() == KnownMdmMboParams.HOUSEHOLD_CHEMICALS_PARAM_ID)
            .findAny()
            .map(ModelStorage.ParameterValue::getBoolValue)
            .orElse(null);
        Assertions.assertThat(cargoTypeValueInMbo).isTrue();

        // Категорийная настройка сменилась на false
        categoryParamValueRepository.insertOrUpdate(
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(categoryId)
                .setMdmParamId(KnownMdmParams.HOUSEHOLD_CHEMICALS)
                .setBool(false)
        );

        // Закинем в очередь msku_to_refresh и запустим пересчет
        mskuToRefreshQueue.enqueue(mskuId, MdmEnqueueReason.DEVELOPER_TOOL);
        commitAndRecomputeMsku();

        // Проверим, что карготип простваился в МДМ
        Assertions.assertThat(mskuRepository.findMsku(mskuId))
            .flatMap(commonMsku -> commonMsku.getParamValue(KnownMdmParams.HOUSEHOLD_CHEMICALS))
            .flatMap(MdmParamValue::getBool)
            .contains(false);

        // Проверим, что msku попала в очередь на отправку в mbo
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(1);
        Assertions.assertThat(mskuToMboQueue.getUnprocessedBatch(1))
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactly(mskuId);

        // Запустим отправку в МБО
        commitAndSendToMbo();
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(0);

        // Проверим, что модель обновилась. Карготип стал false.
        Assertions.assertThat(mskuSyncResultRepository.findById(mskuId).getStatus())
            .isEqualTo(MskuSyncResult.MskuSyncStatus.OK);
        Boolean updatedCargoTypeValueInMbo = loadModelFromStorage(mskuId).getParameterValuesList().stream()
            .filter(parameterValue -> parameterValue.getParamId() == KnownMdmMboParams.HOUSEHOLD_CHEMICALS_PARAM_ID)
            .findAny()
            .map(ModelStorage.ParameterValue::getBoolValue)
            .orElse(null);
        Assertions.assertThat(updatedCargoTypeValueInMbo).isFalse();
    }

    @Test
    public void whenSimpleCargotypeCategoryValueFalseAndNoMskuDataInMdmShouldChangeMboValueFromTrueToFalse() {
        // Наш оффер
        mdmSupplierRepository.insertOrUpdate(
            new MdmSupplier()
                .setId(1949)
                .setName("USSR")
                .setType(MdmSupplierType.THIRD_PARTY)
        );
        ShopSkuKey offer = new ShopSkuKey(1949, "Изделие-501");
        long mskuId = 789L;

        // Категория, на которой карготип бытовая химия false
        long categoryId = 123L;
        categoryParamValueRepository.insertOrUpdate(
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(categoryId)
                .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
                .setString("9007")
        );
        categoryParamValueRepository.insertOrUpdate(
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(categoryId)
                .setMdmParamId(KnownMdmParams.HOUSEHOLD_CHEMICALS)
                .setBool(false)
        );

        var cargoType = new CargoType();
        cargoType.setMboParameterId(mdmParamCache.get(KnownMdmParams.HOUSEHOLD_CHEMICALS).getExternals()
            .getMboParamId());
        cargoType.setId(1);
        cargoType.setDescription("cargoType503");
        cargoType.setMboBoolFalseOptionId(1L);
        cargoType.setMboBoolFalseOptionId(2L);
        cargoTypeRepository.insertOrUpdate(cargoType);
        mdmLmsCargoTypeCache.refresh();

        // Но до этого, в МБО уже было проставлено true
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(mskuId)
            .setCategoryId(categoryId)
            .setDeleted(false)
            .setPublished(true)
            .setCurrentType(ModelStorage.ModelType.SKU.name())
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.HOUSEHOLD_CHEMICALS_PARAM_ID)
                .setXslName(mdmParamCache.get(KnownMdmParams.HOUSEHOLD_CHEMICALS).getExternals().getMboParamXslName())
                .setBoolValue(true)
                .setValueSource(ModelStorage.ModificationSource.AUTO)
                .build())
            .build();
        mboModelsService.saveModels(List.of(model));

        // Загрузим маппинг
        updateMapping(offer, mskuId, categoryId);

        // Закинем в очередь msku_to_refresh и запустим пересчет
        mskuToRefreshQueue.enqueue(mskuId, MdmEnqueueReason.DEVELOPER_TOOL);
        commitAndRecomputeMsku();

        // Проверим карготип простваился в МДМ
        Assertions.assertThat(mskuRepository.findMsku(mskuId))
            .flatMap(commonMsku -> commonMsku.getParamValue(KnownMdmParams.HOUSEHOLD_CHEMICALS))
            .flatMap(MdmParamValue::getBool)
            .contains(false);

        // Проверим, что msku попала в очередь на отправку в mbo
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(1);
        Assertions.assertThat(mskuToMboQueue.getUnprocessedBatch(1))
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactly(mskuId);

        // Запустим отправку в МБО
        commitAndSendToMbo();
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(0);

        // Проверим, что модель обновилась. Карготип стал false.
        Assertions.assertThat(mskuSyncResultRepository.findById(mskuId).getStatus())
            .isEqualTo(MskuSyncResult.MskuSyncStatus.OK);
        Boolean cargoTypeValueInMbo = loadModelFromStorage(mskuId).getParameterValuesList().stream()
            .filter(parameterValue -> parameterValue.getParamId() == KnownMdmMboParams.HOUSEHOLD_CHEMICALS_PARAM_ID)
            .findAny()
            .map(ModelStorage.ParameterValue::getBoolValue)
            .orElse(null);
        Assertions.assertThat(cargoTypeValueInMbo).isFalse();
    }

    /**
     * Когда на категории префикс ТН ВЭД сменился на тот, у которого нет настроек ЧЗ, ЧЗ на модели в МБО должен сняться.
     */
    @Test
    public void whenCategoryValueChangedFromHSTrueToHSNothingShouldChangeMboModelValueFromTrueToFalse() {
        // Наш оффер
        mdmSupplierRepository.insertOrUpdate(
            new MdmSupplier()
                .setId(1960)
                .setName("France")
                .setType(MdmSupplierType.THIRD_PARTY)
        );
        ShopSkuKey offer = new ShopSkuKey(1960, "Gerboise bleue");
        long mskuId = 789L;
        long categoryId = 123L;

        // Загрузим маппинг
        updateMapping(offer, mskuId, categoryId);

        // Проставим на категорию префикс ТН ВЭД с обязательным ЧЗ
        customsCommCodeRepository.insert(
            new CustomsCommCode()
                .setCode("9007")
                .setHonestSign(
                    new MdmParamMarkupState()
                        .setCis(Cis.REQUIRED)
                        .setMarkupActivationTs(LocalDateTime.of(2011, 12, 29, 12, 45))
                )
        );
        categoryParamValueRepository.insertOrUpdate(
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(categoryId)
                .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
                .setString("9007")
        );

        // В МБО пустая модель
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(mskuId)
            .setCategoryId(categoryId)
            .setDeleted(false)
            .setPublished(true)
            .setCurrentType(ModelStorage.ModelType.SKU.name())
            .build();
        mboModelsService.saveModels(List.of(model));

        // Закинем в очередь msku_to_refresh и запустим пересчет
        mskuToRefreshQueue.enqueue(mskuId, MdmEnqueueReason.DEVELOPER_TOOL);
        commitAndRecomputeMsku();

        // Проверим, что карготип простваился в МДМ
        Assertions.assertThat(mskuRepository.findMsku(mskuId))
            .flatMap(commonMsku -> commonMsku.getParamValue(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED))
            .flatMap(MdmParamValue::getBool)
            .contains(true);

        // Проверим, что msku попала в очередь на отправку в mbo
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(1);
        Assertions.assertThat(mskuToMboQueue.getUnprocessedBatch(1))
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactly(mskuId);

        // Запустим отправку в МБО
        commitAndSendToMbo();
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(0);

        // Проверим, что модель обновилась. Карготип стал true.
        Assertions.assertThat(mskuSyncResultRepository.findById(mskuId).getStatus())
            .isEqualTo(MskuSyncResult.MskuSyncStatus.OK);
        Boolean cargoTypeValueInMbo = loadModelFromStorage(mskuId).getParameterValuesList().stream()
            .filter(parameterValue -> parameterValue.getParamId() == KnownMdmMboParams.HONEST_SIGN_REQUIRED_PARAM_ID)
            .findAny()
            .map(ModelStorage.ParameterValue::getBoolValue)
            .orElse(null);
        Assertions.assertThat(cargoTypeValueInMbo).isTrue();

        // На категории сменился префикс ТН ВЭД, для которого нет настроек ЧЗ
        categoryParamValueRepository.insertOrUpdate(
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(categoryId)
                .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
                .setString("9008")
        );

        // Закинем в очередь msku_to_refresh и запустим пересчет
        mskuToRefreshQueue.enqueue(mskuId, MdmEnqueueReason.DEVELOPER_TOOL);
        commitAndRecomputeMsku();

        // Проверим карготип в МДМ
        Assertions.assertThat(mskuRepository.findMsku(mskuId))
            .flatMap(commonMsku -> commonMsku.getParamValue(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED))
            .flatMap(MdmParamValue::getBool)
            .contains(false);

        // Проверим, что msku попала в очередь на отправку в mbo
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(1);
        Assertions.assertThat(mskuToMboQueue.getUnprocessedBatch(1))
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactly(mskuId);

        // Запустим отправку в МБО
        commitAndSendToMbo();
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(0);

        // Проверим, что модель обновилась. Карготип стал false.
        Assertions.assertThat(mskuSyncResultRepository.findById(mskuId).getStatus())
            .isEqualTo(MskuSyncResult.MskuSyncStatus.OK);
        Boolean updatedCargoTypeValueInMbo = loadModelFromStorage(mskuId).getParameterValuesList().stream()
            .filter(parameterValue -> parameterValue.getParamId() == KnownMdmMboParams.HONEST_SIGN_REQUIRED_PARAM_ID)
            .findAny()
            .map(ModelStorage.ParameterValue::getBoolValue)
            .orElse(null);
        Assertions.assertThat(updatedCargoTypeValueInMbo).isFalse();
    }

    /**
     * Когда на категории префикс ТН ВЭД пропал, ЧЗ должен сняться.
     */
    @Test
    public void whenCategoryValueChangedFromHSTrueToNothingShouldChangeMboModelValueFromTrueToFalse() {
        // Наш оффер
        mdmSupplierRepository.insertOrUpdate(
            new MdmSupplier()
                .setId(1960)
                .setName("France")
                .setType(MdmSupplierType.THIRD_PARTY)
        );
        ShopSkuKey offer = new ShopSkuKey(1960, "Gerboise bleue");
        long mskuId = 789L;
        long categoryId = 123L;

        // Загрузим маппинг
        updateMapping(offer, mskuId, categoryId);

        // Проставим на категорию префикс ТН ВЭД с обязательным ЧЗ
        customsCommCodeRepository.insert(
            new CustomsCommCode()
                .setCode("9007")
                .setHonestSign(
                    new MdmParamMarkupState()
                        .setCis(Cis.REQUIRED)
                        .setMarkupActivationTs(LocalDateTime.of(2011, 12, 29, 12, 45))
                )
        );
        categoryParamValueRepository.insertOrUpdate(
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(categoryId)
                .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
                .setString("9007")
        );

        // В МБО пустая модель
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(mskuId)
            .setCategoryId(categoryId)
            .setDeleted(false)
            .setPublished(true)
            .setCurrentType(ModelStorage.ModelType.SKU.name())
            .build();
        mboModelsService.saveModels(List.of(model));

        // Закинем в очередь msku_to_refresh и запустим пересчет
        mskuToRefreshQueue.enqueue(mskuId, MdmEnqueueReason.DEVELOPER_TOOL);
        commitAndRecomputeMsku();

        // Проверим, что карготип простваился в МДМ
        Assertions.assertThat(mskuRepository.findMsku(mskuId))
            .flatMap(commonMsku -> commonMsku.getParamValue(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED))
            .flatMap(MdmParamValue::getBool)
            .contains(true);

        // Проверим, что msku попала в очередь на отправку в mbo
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(1);
        Assertions.assertThat(mskuToMboQueue.getUnprocessedBatch(1))
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactly(mskuId);

        // Запустим отправку в МБО
        commitAndSendToMbo();
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(0);

        // Проверим, что модель обновилась. Карготип стал true.
        Assertions.assertThat(mskuSyncResultRepository.findById(mskuId).getStatus())
            .isEqualTo(MskuSyncResult.MskuSyncStatus.OK);
        Boolean cargoTypeValueInMbo = loadModelFromStorage(mskuId).getParameterValuesList().stream()
            .filter(parameterValue -> parameterValue.getParamId() == KnownMdmMboParams.HONEST_SIGN_REQUIRED_PARAM_ID)
            .findAny()
            .map(ModelStorage.ParameterValue::getBoolValue)
            .orElse(null);
        Assertions.assertThat(cargoTypeValueInMbo).isTrue();

        // Msku переехала в категорию, где нет префикса ТН ВЭД
        long categoryWithoutCCCPrefix = 999L;
        updateMapping(offer, mskuId, categoryWithoutCCCPrefix);

        // Закинем в очередь msku_to_refresh и запустим пересчет
        mskuToRefreshQueue.enqueue(mskuId, MdmEnqueueReason.DEVELOPER_TOOL);
        commitAndRecomputeMsku();

        // Проверим карготип в МДМ
        Assertions.assertThat(mskuRepository.findMsku(mskuId))
            .flatMap(commonMsku -> commonMsku.getParamValue(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED))
            .flatMap(MdmParamValue::getBool)
            .contains(false);

        // Проверим, что msku попала в очередь на отправку в mbo
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(1);
        Assertions.assertThat(mskuToMboQueue.getUnprocessedBatch(1))
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactly(mskuId);

        // Запустим отправку в МБО
        commitAndSendToMbo();
        Assertions.assertThat(mskuToMboQueue.getUnprocessedItemsCount()).isEqualTo(0);

        // Проверим, что модель обновилась. Карготип стал false.
        Assertions.assertThat(mskuSyncResultRepository.findById(mskuId).getStatus())
            .isEqualTo(MskuSyncResult.MskuSyncStatus.OK);
        Boolean updatedCargoTypeValueInMbo = loadModelFromStorage(mskuId).getParameterValuesList().stream()
            .filter(parameterValue -> parameterValue.getParamId() == KnownMdmMboParams.HONEST_SIGN_REQUIRED_PARAM_ID)
            .findAny()
            .map(ModelStorage.ParameterValue::getBoolValue)
            .orElse(null);
        Assertions.assertThat(updatedCargoTypeValueInMbo).isFalse();
    }

    private void updateMapping(ShopSkuKey offer, long mskuId, long categoryId) {
        MappingCacheDao mapping = new MappingCacheDao()
            .setShopSkuKey(offer)
            .setMskuId(mskuId)
            .setCategoryId(Math.toIntExact(categoryId));
        mappingsCache.insertOrUpdateAll(List.of(mapping));
    }

    private void commitAndSendToMbo() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        sendToMboExecutor.execute();
    }

    private void commitAndRecomputeMsku() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        recomputeMskuGoldExecutor.execute();
    }

    private ModelStorage.Model loadModelFromStorage(long mskuId) {
        List<ModelStorage.Model> models = mboModelsService.loadRawModels(List.of(mskuId));
        if (models.size() > 1) {
            throw new IllegalStateException("Only single model should be returned");
        }
        return models.stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Model storage does not return test model"));
    }
}
