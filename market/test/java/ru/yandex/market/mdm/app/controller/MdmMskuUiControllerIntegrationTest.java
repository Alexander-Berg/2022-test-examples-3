package ru.yandex.market.mdm.app.controller;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.ListUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.mdm.common.infrastructure.FileStatus;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmFileHistoryRepositoryMock;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmFileImportHelperService;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmS3FileServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.MdmUser;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenBlocksPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenSplitterMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MskuSilverSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.ImportResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMboUser;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMboUsersRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmUserRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmUserRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.GlobalParamValueService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.MboMskuUpdateService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.WarehouseProjectionCacheImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.editor.MdmSampleDataService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.MdmCommonMskuMboService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.MdmCommonMskuMboServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuCalculatingProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuCalculatingProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingDataProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingPipeProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingPipeProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuSskuWithPriorityProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuSskuWithPriorityProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.RecomputeMskuGoldServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CommonMskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmLmsCargoTypeCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamExcelAttributes;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuMdmParamExcelExportService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuMdmParamExcelService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfoRepository;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.MdmParameterValueCachingServiceMock;
import ru.yandex.market.mbo.mdm.common.service.mapping.MdmBestMappingsProvider;
import ru.yandex.market.mbo.mdm.common.service.msku.MskuFullCircuitSyncService;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessMskuQueueService;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.mdm.tms.executors.RecomputeMskuGoldExecutor;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.CommonMskuValidator;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepositoryMock;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsServiceImpl;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsService;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.common.utils.SecurityUtil;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

import static org.mockito.Mockito.mock;

/**
 * @author albina-gima
 * @date 10/20/20
 */
public class MdmMskuUiControllerIntegrationTest extends MdmBaseDbTestClass {
    private static final long MBO_USER_ID = 123L;
    private static final long TIMESTAMP = 123L;
    private static final int ISO_CODE_RU = 225;
    private static final long VENDOR_ID = 12345;

    private static final long MSKU_ID_1 = 100L;
    private static final long MSKU_ID_2 = 101L;
    private static final long MSKU_ID_3 = 102L;
    private static final long MSKU_ID_4 = 103L;

    private static final long CATEGORY_ID = 54321;

    private static final int SUPPLIER_1 = 110;
    private static final int BUSINESS = 100;
    private static final String SKU_SUPPLIER_1 = "thing1";

    //<editor-fold desc="MBO PARAMS" defaultstate="collapsed">
    private static final int HEAVY_GOOD_CATEGORY_TRUE_OPTION_ID = 16586307;
    private static final int HEAVY_GOOD_CATEGORY_FALSE_OPTION_ID = 16586312;
    private static final int HEAVY_GOOD_CATEGORY_20_TRUE_OPTION_ID = 17827722;
    private static final int HEAVY_GOOD_CATEGORY_20_FALSE_OPTION_ID = 17827727;
    private static final int PRECIOUS_GOOD_TRUE_OPTION_ID = 16618865;
    private static final int PRECIOUS_GOOD_FALSE_OPTION_ID = 16618870;
    private static final int HOUSEHOLD_CHEMICALS_TRUE_OPTION_ID = 16619156;
    private static final int HOUSEHOLD_CHEMICALS_FALSE_OPTION_ID = 16619161;
    private static final int FOOD_TRUE_OPTION_ID = 16619371;
    private static final int FOOD_FALSE_OPTION_ID = 16619376;
    private static final int INTIMATE_GOOD_TRUE_OPTION_ID = 16669139;
    private static final int INTIMATE_GOOD_FALSE_OPTION_ID = 16669144;
    private static final int SMELLING_GOOD_TRUE_OPTION_ID = 16619181;
    private static final int SMELLING_GOOD_FALSE_OPTION_ID = 16619186;
    private static final int DANGEROUS_GOOD_TRUE_OPTION_ID = 16402544;
    private static final int DANGEROUS_GOOD_FALSE_OPTION_ID = 16402549;
    //</editor-fold>

    //<editor-fold desc="MBO PARAM VALUES" defaultstate="collapsed">
    private static final ModelStorage.ParameterValue HEAVY_GOOD_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, true, HEAVY_GOOD_CATEGORY_TRUE_OPTION_ID, "cargoType300");
    private static final ModelStorage.ParameterValue HEAVY_GOOD_20_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID, true, HEAVY_GOOD_CATEGORY_20_TRUE_OPTION_ID, "cargoType301");
    private static final ModelStorage.ParameterValue MDM_WEIGHT_GROSS_MBO_PARAM_VALUE =
        createNumericParamValue(KnownMdmMboParams.MDM_WEIGHT_GROSS_PARAM_ID, "1.1", "mdm_weight_gross");
    private static final ModelStorage.ParameterValue GUARANTEE_PERIOD_MBO_PARAM_VALUE = createStringParamValue(
        KnownMdmMboParams.WARRANTY_PERIOD_PARAM_ID, "12", "WarrantyPeriod");
    private static final ModelStorage.ParameterValue GUARANTEE_PERIOD_UNIT_MBO_PARAM_VALUE = createEnumParamValue(
        KnownMdmMboParams.GUARANTEE_PERIOD_UNIT_PARAM_ID,
        KnownMdmMboParams.GUARANTEE_PERIOD_OPTION_IDS.get(TimeInUnits.TimeUnit.MONTH), "WarrantyPeriod_Unit");
    private static final ModelStorage.ParameterValue GUARANTEE_PERIOD_COMMENT_MBO_PARAM_VALUE = createStringParamValue(
        KnownMdmMboParams.GUARANTEE_PERIOD_COMMENT_PARAM_ID, "Гарантируем гарантию", "WarrantyPeriod_Comment");
    private static final ModelStorage.ParameterValue HIDE_GUARANTEE_PERIOD_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.HIDE_GUARANTEE_PERIOD_PARAM_ID, true,
        KnownMdmMboParams.HIDE_GUARANTEE_PERIOD_TRUE_OPTION_ID, "hide_warranty_period"
    );
    private static final ModelStorage.ParameterValue VAT_MBO_PARAM_VALUE =
        createNumericEnumParamValue(KnownMdmMboParams.NDS_PARAM_ID, KnownMdmMboParams.VAT_10_OPTION_ID, "NDS");

    private static final ModelStorage.ParameterValue SHELF_LIFE_MBO_PARAM_VALUE =
        createNumericParamValue(KnownMdmMboParams.LIFE_SHELF_PARAM_ID, "2.5", "LifeShelf");
    private static final ModelStorage.ParameterValue SHELF_LIFE_UNIT_MBO_PARAM_VALUE = createEnumParamValue(
        KnownMdmMboParams.SHELF_LIFE_UNIT_PARAM_ID,
        KnownMdmMboParams.SHELF_LIFE_OPTION_IDS.get(TimeInUnits.TimeUnit.YEAR), "ShelfLife_Unit");

    private static final ModelStorage.ParameterValue SHELF_LIFE_COMMENT_MBO_PARAM_VALUE = createStringParamValue(
        KnownMdmMboParams.SHELF_LIFE_COMMENT_PARAM_ID, "Хранить в прохладном месте", "ShelfLife_Comment");
    private static final ModelStorage.ParameterValue HIDE_SHELF_LIFE_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.HIDE_SHELF_LIFE_PARAM_ID, true,
        KnownMdmMboParams.HIDE_SHELF_LIFE_TRUE_OPTION_ID, "hide_shelf_life"
    );
    private static final ModelStorage.ParameterValue SHELF_SERVICE_MBO_PARAM_VALUE = createStringParamValue(
        KnownMdmMboParams.LIFE_TIME_PARAM_ID, "444", "ShelfService");
    private static final ModelStorage.ParameterValue SHELF_SERVICE_UNIT_MBO_PARAM_VALUE = createEnumParamValue(
        KnownMdmMboParams.LIFE_TIME_UNIT_PARAM_ID,
        KnownMdmMboParams.LIFE_TIME_OPTION_IDS.get(TimeInUnits.TimeUnit.DAY), "ShelfService_Unit");
    private static final ModelStorage.ParameterValue SHELF_SERVICE_COMMENT_MBO_PARAM_VALUE = createStringParamValue(
        KnownMdmMboParams.LIFE_TIME_COMMENT_PARAM_ID, "Использовать по назначению", "ShelfService_Comment");
    private static final ModelStorage.ParameterValue HIDE_LIFE_TIME_MBO_PARAMETER_VALUE = createBooleanParamValue(
        KnownMdmMboParams.HIDE_LIFE_TIME_PARAM_ID, true,
        KnownMdmMboParams.HIDE_LIFE_TIME_TRUE_OPTION_ID, "hide_life_time"
    );
    private static final ModelStorage.ParameterValue EXPIR_DATE_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.EXPIR_DATE_PARAM_ID, true, KnownMdmMboParams.EXPIR_DATE_TRUE_OPTION_ID, "expir_date");
    private static final ModelStorage.ParameterValue PRECIOUS_GOOD_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.PRECIOUS_GOOD_PARAM_ID, true, PRECIOUS_GOOD_TRUE_OPTION_ID, "cargoType40");
    private static final ModelStorage.ParameterValue HOUSEHOLD_CHEMICALS_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.HOUSEHOLD_CHEMICALS_PARAM_ID, true, HOUSEHOLD_CHEMICALS_TRUE_OPTION_ID, "cargoType485");
    private static final ModelStorage.ParameterValue FOOD_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.FOOD_PARAM_ID, true, FOOD_TRUE_OPTION_ID, "cargoType750");
    private static final ModelStorage.ParameterValue INTIMATE_GOOD_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.INTIMATE_GOOD_PARAM_ID, true, INTIMATE_GOOD_TRUE_OPTION_ID, "cargoType910");
    private static final ModelStorage.ParameterValue SMELLING_GOOD_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.SMELLING_GOOD_PARAM_ID, true, SMELLING_GOOD_TRUE_OPTION_ID, "cargoType460");
    private static final ModelStorage.ParameterValue DANGEROUS_GOOD_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.DANGEROUS_GOOD_PARAM_ID, true, DANGEROUS_GOOD_TRUE_OPTION_ID, "cargoType400");
    private static final ModelStorage.ParameterValue HONEST_SIGN_OPTIONAL_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.HONEST_SIGN_OPTIONAL_PARAM_ID, true,
        KnownMdmMboParams.HONEST_SIGN_OPTIONAL_TRUE_OPTION_ID, "cargoType990");
    private static final ModelStorage.ParameterValue HONEST_SIGN_DISTINCT_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.HONEST_SIGN_DISTINCT_PARAM_ID, true,
        KnownMdmMboParams.HONEST_SIGN_DISTINCT_TRUE_OPTION_ID, "cargoType985");
    private static final ModelStorage.ParameterValue HONEST_SIGN_REQUIRED_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.HONEST_SIGN_REQUIRED_PARAM_ID, true,
        KnownMdmMboParams.HONEST_SIGN_REQUIRED_TRUE_OPTION_ID, "cargoType980");
    private static final ModelStorage.ParameterValue HONEST_SIGN_ACTIVATION_MBO_PARAM_VALUE = createStringParamValue(
        KnownMdmMboParams.HONEST_SIGN_ACTIVATION_PARAM_ID, "01.10.2020 00:00:00", "HonestSignActivationDate");
    private static final ModelStorage.ParameterValue IMEI_CONTROL_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.IMEI_CONTROL_PARAM_ID, true,
        KnownMdmMboParams.IMEI_CONTROL_TRUE_OPTION_ID, "mdm_imei_control");
    private static final ModelStorage.ParameterValue IMEI_MASK_MBO_PARAM_VALUE = createStringParamValue(
        KnownMdmMboParams.IMEI_MASK_PARAM_ID, "some_imei_mask_from_mbo", "mdm_imei_mask");
    private static final ModelStorage.ParameterValue SERIAL_NUMBER_CONTROL_MBO_PARAM_VALUE = createBooleanParamValue(
        KnownMdmMboParams.SERIAL_NUMBER_CONTROL_PARAM_ID, true,
        KnownMdmMboParams.SERIAL_NUMBER_CONTROL_TRUE_OPTION_ID, "mdm_serial_number_control");
    private static final ModelStorage.ParameterValue SN_MASK_MBO_PARAM_VALUE = createStringParamValue(
        KnownMdmMboParams.SERIAL_NUMBER_MASK_PARAM_ID, "some_sn_mask_from_mbo", "mdm_serial_number_mask");
    //</editor-fold>

    //<editor-fold desc="MODELS" defaultstate="collapsed">
    private static final ModelStorage.Model MODEL1 = ModelStorage.Model.newBuilder()
        .setId(MSKU_ID_1)
        .setGroupModelId(MSKU_ID_1)
        .setCategoryId(CATEGORY_ID)
        .setVendorId(VENDOR_ID)
        .setSourceType("GURU")
        .setCurrentType("GURU")
        .addParameterValues(HEAVY_GOOD_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(HEAVY_GOOD_20_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(MDM_WEIGHT_GROSS_MBO_PARAM_VALUE.toBuilder())
        .build();
    private static final ModelStorage.Model MODEL2 = ModelStorage.Model.newBuilder()
        .setId(MSKU_ID_2)
        .setGroupModelId(MSKU_ID_2)
        .setCategoryId(CATEGORY_ID)
        .setVendorId(VENDOR_ID)
        .setSourceType("GURU")
        .setCurrentType("GURU")
        .addParameterValues(GUARANTEE_PERIOD_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(GUARANTEE_PERIOD_UNIT_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(VAT_MBO_PARAM_VALUE.toBuilder())
        .build();
    private static final ModelStorage.Model MODEL3 = ModelStorage.Model.newBuilder()
        .setId(MSKU_ID_3)
        .setGroupModelId(MSKU_ID_3)
        .setCategoryId(CATEGORY_ID)
        .setVendorId(VENDOR_ID)
        .setSourceType("GURU")
        .setCurrentType("GURU")
        .addParameterValues(SHELF_LIFE_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(SHELF_LIFE_UNIT_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(SHELF_LIFE_COMMENT_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(HIDE_SHELF_LIFE_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(SHELF_SERVICE_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(SHELF_SERVICE_UNIT_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(SHELF_SERVICE_COMMENT_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(HIDE_LIFE_TIME_MBO_PARAMETER_VALUE.toBuilder())
        .addParameterValues(GUARANTEE_PERIOD_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(GUARANTEE_PERIOD_UNIT_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(GUARANTEE_PERIOD_COMMENT_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(HIDE_GUARANTEE_PERIOD_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(EXPIR_DATE_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(HEAVY_GOOD_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(HEAVY_GOOD_20_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(PRECIOUS_GOOD_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(HOUSEHOLD_CHEMICALS_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(FOOD_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(INTIMATE_GOOD_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(SMELLING_GOOD_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(DANGEROUS_GOOD_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(HONEST_SIGN_OPTIONAL_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(HONEST_SIGN_DISTINCT_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(HONEST_SIGN_REQUIRED_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(HONEST_SIGN_ACTIVATION_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(IMEI_CONTROL_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(SERIAL_NUMBER_CONTROL_MBO_PARAM_VALUE.toBuilder())
        .build();
    private static final ModelStorage.Model MODEL4 = ModelStorage.Model.newBuilder()
        .setId(MSKU_ID_4)
        .setGroupModelId(MSKU_ID_4)
        .setCategoryId(CATEGORY_ID)
        .setVendorId(VENDOR_ID)
        .setSourceType("GURU")
        .setCurrentType("GURU")
        .addParameterValues(SHELF_LIFE_MBO_PARAM_VALUE.toBuilder())
        .addParameterValues(SHELF_LIFE_UNIT_MBO_PARAM_VALUE.toBuilder())
        .build();
    //</editor-fold>

    @Autowired
    private MskuMdmParamExcelExportService mskuMdmParamExcelExportService;
    @Autowired
    private MskuMdmParamExcelService mskuMdmParamExcelService;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MdmLmsCargoTypeCache mdmLmsCargoTypeCache;
    @Autowired
    private CargoTypeRepository cargoTypeRepository;
    @Autowired
    private MdmMboUsersRepository mdmMboUsersRepository;
    @Autowired
    private StorageKeyValueService keyValueService;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private GlobalParamValueService globalParamValueService;
    @Autowired
    private MskuToRefreshRepository mskuQueue;
    @Autowired
    private MdmQueuesManager queuesManager;
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    @Autowired
    private MdmParamProvider mdmParamProvider;
    @Autowired
    private CustomsCommCodeRepository codeRepository;
    @Autowired
    private MdmGoodGroupRepository mdmGoodGroupRepository;
    @Autowired
    private CommonMskuValidator commonMskuValidator;
    @Autowired
    private PriceInfoRepository priceInfoRepository;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    private MdmBestMappingsProvider mdmBestMappingsProvider;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private WeightDimensionsValidator weightDimensionsValidator;

    private MboModelsService mboModelsService;
    private RecomputeMskuGoldExecutor executor;
    private MdmUserRepository mdmUserRepository;
    private MskuProcessingDataProviderImpl assistant;
    private MdmMskuUiController controller;

    @Before
    public void before() {
        this.mdmUserRepository = new MdmUserRepositoryMock();
        storageKeyValueService.putValue(MdmProperties.UI_SAVE_AS_ADMIN_ENABLED, true);
        storageKeyValueService.invalidateCache();
        mboModelsService = new MboModelsServiceMock();
        CommonMskuConverter commonMskuConverter = new CommonMskuConverter(mdmParamCache);
        MboMskuUpdateService mboMskuUpdateService = new MboMskuUpdateService(mboModelsService, cargoTypeRepository);
        MdmCommonMskuMboService mdmCommonMskuMboService = new MdmCommonMskuMboServiceImpl(
            mboModelsService, mboMskuUpdateService,
            commonMskuConverter, globalParamValueService, mskuRepository, queuesManager, commonMskuValidator,
            mappingsCacheRepository, mdmUserRepository, storageKeyValueService);
        mskuMdmParamExcelService = new MskuMdmParamExcelService(mdmParamCache, mdmParamProvider,
            mdmCommonMskuMboService, storageKeyValueService);
        var fileImportHelperService = new MdmFileImportHelperService(new MdmS3FileServiceMock(),
            new MdmFileHistoryRepositoryMock());

        MdmSampleDataService mdmSampleDataService = Mockito.mock(MdmSampleDataService.class);

        controller = new MdmMskuUiController(
            new ObjectMapper(),
            mdmParamProvider,
            mdmCommonMskuMboService,
            mskuMdmParamExcelExportService,
            mskuMdmParamExcelService,
            fileImportHelperService,
            mdmMboUsersRepository,
            mdmUserRepository,
            mdmSampleDataService,
            Mockito.mock(MskuFullCircuitSyncService.class));

        var settings = new MdmCategorySettingsServiceImpl(new MdmParameterValueCachingServiceMock(),
            new CargoTypeRepositoryMock(), new CategoryParamValueRepositoryMock());
        MasterDataValidator masterDataValidator = mock(MasterDataValidator.class);
        assistant = new MskuProcessingDataProviderImpl(
            mskuRepository,
            categoryParamValueRepository,
            settings,
            masterDataRepository,
            globalParamValueService,
            goldSskuRepository,
            storageKeyValueService,
            priceInfoRepository,
            Mockito.mock(WarehouseProjectionCacheImpl.class),
            mdmParamCache,
            mdmBestMappingsProvider
        );

        var goldenSplitterMerger = new MskuGoldenSplitterMerger(mdmParamCache);
        var mskuSilverSplitter = new MskuSilverSplitter(mdmParamCache, sskuGoldenParamUtil);
        MskuSilverItemPreProcessor mskuSilverItemPreProcessor = new MskuSilverItemPreProcessor(
            mdmParamCache, mdmLmsCargoTypeCache, featureSwitchingAssistant);
        CustomsCommCodeMarkupService markupService = new CustomsCommCodeMarkupServiceImpl(mdmParamCache, codeRepository,
            new CCCodeValidationService(List.of(), codeRepository), categoryParamValueRepository,
            new TaskQueueRegistratorMock(), mdmGoodGroupRepository, mappingsCacheRepository);
        MskuGoldenBlocksPostProcessor mskuGoldenBlocksPostProcessor =
            new MskuGoldenBlocksPostProcessor(featureSwitchingAssistant, mdmParamCache, markupService, keyValueService);
        WeightDimensionBlockValidationServiceImpl validationService = new WeightDimensionBlockValidationServiceImpl(
            new CachedItemBlockValidationContextProviderImpl(storageKeyValueService),
            weightDimensionsValidator
        );
        MskuGoldenItemService mskuGIS = new MskuGoldenItemService(mskuSilverSplitter,
            goldenSplitterMerger, goldenSplitterMerger, mskuSilverItemPreProcessor, featureSwitchingAssistant,
            mskuGoldenBlocksPostProcessor, validationService, mdmParamCache);

        RecomputeMskuGoldServiceImpl recomputeMskuGoldService = new RecomputeMskuGoldServiceImpl(assistant,
            mskuProcessingPipeProcessor(),
            mskuCalculatingProcessor(mskuGIS, masterDataValidator));

        ProcessMskuQueueService processMskuQueueService = new ProcessMskuQueueService(mskuQueue,
            keyValueService,
            recomputeMskuGoldService);
        executor = new RecomputeMskuGoldExecutor(processMskuQueueService);

        SecurityUtil.authenticate("staffLogin");
        addMboUserToMdmRepo();

        mboModelsService.saveModels(List.of(MODEL1, MODEL2, MODEL3, MODEL4));
        storageKeyValueService.invalidateCache();
    }

    private MskuProcessingPipeProcessor mskuProcessingPipeProcessor() {
        return new MskuProcessingPipeProcessorImpl(queuesManager, mskuSskuWithPriorityProvider());
    }

    private MskuSskuWithPriorityProvider mskuSskuWithPriorityProvider() {
        return new MskuSskuWithPriorityProviderImpl(mdmSskuGroupManager);
    }

    private MskuCalculatingProcessor mskuCalculatingProcessor(MskuGoldenItemService mskuGoldenItemService,
                                                              MasterDataValidator masterDataValidator) {
        return new MskuCalculatingProcessorImpl(mskuRepository, mskuGoldenItemService, masterDataValidator);
    }


    @Test
    public void testUpdateMboParameterValuesWithBoolAndNumericTypes() {
        MskuParamValue heavyGoodMskuParamValue = new MskuParamValue();
        heavyGoodMskuParamValue.setMskuId(MSKU_ID_1)
            .setMdmParamId(KnownMdmParams.HEAVY_GOOD)
            .setBool(false) //в модели true
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        MskuParamValue weightGrossMskuParamValue = new MskuParamValue();
        weightGrossMskuParamValue.setMskuId(MSKU_ID_1)
            .setMdmParamId(KnownMdmParams.WEIGHT_GROSS)
            .setNumeric(BigDecimal.valueOf(1.25d)) //в модели 1.1
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        CommonMsku mskuToUpdate = new CommonMsku(0L, MSKU_ID_1)
            .addParamValue(heavyGoodMskuParamValue)
            .addParamValue(weightGrossMskuParamValue);

        controller.update(mskuToUpdate, null);

        ModelStorage.Model resultModel = mboModelsService.loadRawModels(List.of(MSKU_ID_1)).get(0);
        Map<Long, ModelStorage.ParameterValue> resultParamValues = resultModel.getParameterValuesList().stream()
            .collect(Collectors.toMap(ModelStorage.ParameterValue::getParamId, Function.identity()));

        //проверяем основные поля модели
        Assertions.assertThat(resultModel.getCategoryId()).isEqualTo(CATEGORY_ID);
        Assertions.assertThat(resultModel.getGroupModelId()).isEqualTo(MSKU_ID_1);
        Assertions.assertThat(resultModel.getVendorId()).isEqualTo(VENDOR_ID);
        Assertions.assertThat(resultModel.getSourceType()).isEqualTo("GURU");
        Assertions.assertThat(resultModel.getCurrentType()).isEqualTo("GURU");
        Assertions.assertThat(resultModel.getModifiedUserId()).isEqualTo(MBO_USER_ID);

        //проверяем, что некоторые значения параметров модели остались без изменений
        Assertions.assertThat(resultModel.getParameterValuesList().size()).isEqualTo(3);
        Assertions.assertThat(resultParamValues.keySet()).containsAll(List.of(
            HEAVY_GOOD_MBO_PARAM_VALUE.getParamId(),
            HEAVY_GOOD_20_MBO_PARAM_VALUE.getParamId(),
            MDM_WEIGHT_GROSS_MBO_PARAM_VALUE.getParamId())
        );
        Assertions.assertThat(resultParamValues.get(KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID))
            .isEqualTo(HEAVY_GOOD_20_MBO_PARAM_VALUE);

        //проверяем новое значение mbo-параметра с xsl_name == heavyGood
        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID,
            HEAVY_GOOD_MBO_PARAM_VALUE, false, HEAVY_GOOD_CATEGORY_FALSE_OPTION_ID);

        //проверяем новое значение mbo-параметра с xsl_name == mdm_weight_gross
        assertNumericVariable(resultParamValues, KnownMdmMboParams.MDM_WEIGHT_GROSS_PARAM_ID,
            MDM_WEIGHT_GROSS_MBO_PARAM_VALUE, "1.25");

        //проверяем, что одна из двух моделей в хранилище mbo осталась без изменений
        ModelStorage.Model secondModel = mboModelsService.loadRawModels(List.of(MSKU_ID_2)).get(0);
        Assertions.assertThat(MODEL2).isEqualTo(secondModel);
    }

    @Test
    public void testUpdateMboParameterValuesWithStringEnumAndNumericEnumTypes() {
        MskuParamValue guaranteePeriodMskuParamValue = new MskuParamValue();
        guaranteePeriodMskuParamValue.setMskuId(MSKU_ID_2)
            .setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD)
            .setString("50") //в модели 12
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        MskuParamValue guaranteePeriodUnitMskuParameterValue = new MskuParamValue();
        guaranteePeriodUnitMskuParameterValue.setMskuId(MSKU_ID_2)
            .setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD_UNIT)
            .setOption(new MdmParamOption().setId(5).setRenderedValue("недели")) //в модели указаны Месяцы
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        MskuParamValue vatParamValue = new MskuParamValue();
        vatParamValue.setMskuId(MSKU_ID_2)
            .setMdmParamId(KnownMdmParams.VAT)
            .setOption(new MdmParamOption().setId(3).setRenderedValue("18")) //в модели указан 10
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        CommonMsku mskuToUpdate = new CommonMsku(0L, MSKU_ID_2)
            .addParamValue(guaranteePeriodMskuParamValue)
            .addParamValue(guaranteePeriodUnitMskuParameterValue)
            .addParamValue(vatParamValue);

        controller.update(mskuToUpdate, null);

        ModelStorage.Model resultModel = mboModelsService.loadRawModels(List.of(MSKU_ID_2)).get(0);
        Map<Long, ModelStorage.ParameterValue> resultParamValues = resultModel.getParameterValuesList().stream()
            .collect(Collectors.toMap(ModelStorage.ParameterValue::getParamId, Function.identity()));

        Assertions.assertThat(resultModel.getParameterValuesList().size()).isEqualTo(3);
        Assertions.assertThat(resultParamValues.keySet()).containsAll(List.of(
            GUARANTEE_PERIOD_MBO_PARAM_VALUE.getParamId(),
            GUARANTEE_PERIOD_UNIT_MBO_PARAM_VALUE.getParamId(),
            VAT_MBO_PARAM_VALUE.getParamId())
        );

        //проверяем основные поля модели
        Assertions.assertThat(resultModel.getCategoryId()).isEqualTo(CATEGORY_ID);
        Assertions.assertThat(resultModel.getGroupModelId()).isEqualTo(MSKU_ID_2);
        Assertions.assertThat(resultModel.getVendorId()).isEqualTo(VENDOR_ID);
        Assertions.assertThat(resultModel.getSourceType()).isEqualTo("GURU");
        Assertions.assertThat(resultModel.getCurrentType()).isEqualTo("GURU");
        Assertions.assertThat(resultModel.getModifiedUserId()).isEqualTo(MBO_USER_ID);

        //проверяем новое значение mbo-параметра с xsl_name == WarrantyPeriod
        ModelStorage.ParameterValue updGuaranteePeriod =
            resultParamValues.get(KnownMdmMboParams.WARRANTY_PERIOD_PARAM_ID);
        Assertions.assertThat(updGuaranteePeriod).isEqualToIgnoringGivenFields(GUARANTEE_PERIOD_MBO_PARAM_VALUE,
            "userId_", "strValue_", "valueType_", "valueSource_", "modificationDate_");

        List<ModelStorage.LocalizedString> strValueList = updGuaranteePeriod.getStrValueList();
        Assertions.assertThat(strValueList.get(0).getIsoCode()).isEqualTo("ru");
        Assertions.assertThat(strValueList.get(0).getValue()).isEqualTo("50");

        Assertions.assertThat(updGuaranteePeriod.getValueType()).isEqualTo(MboParameters.ValueType.STRING);
        Assertions.assertThat(updGuaranteePeriod.getValueSource()).isEqualTo(ModelStorage.ModificationSource.MDM);
        Assertions.assertThat(updGuaranteePeriod.getUserId()).isEqualTo(MBO_USER_ID); //был AUTO_USER

        //проверяем новое значение mbo-параметра с xsl_name == WarrantyPeriod_Unit
        assertEnumVariable(resultParamValues, KnownMdmMboParams.GUARANTEE_PERIOD_UNIT_PARAM_ID,
            GUARANTEE_PERIOD_UNIT_MBO_PARAM_VALUE,
            KnownMdmMboParams.GUARANTEE_PERIOD_OPTION_IDS.get(TimeInUnits.TimeUnit.WEEK));

        //проверяем новое значение mbo-параметра с xsl_name == NDS
        assertEnumVariable(resultParamValues, KnownMdmMboParams.NDS_PARAM_ID, VAT_MBO_PARAM_VALUE,
            KnownMdmMboParams.VAT_18_OPTION_ID);

        //проверяем, что одна из двух моделей в хранилище mbo осталась без изменений
        ModelStorage.Model firstModel = mboModelsService.loadRawModels(List.of(MSKU_ID_1)).get(0);
        Assertions.assertThat(MODEL1).isEqualTo(firstModel);
    }

    @Test
    public void whenGivenMdmParamsWithoutMboParamIdShouldNotChangeModel() {
        //не mbo-параметры, не должны изменять модель
        MskuParamValue regNumberMskuParamValue = new MskuParamValue();
        regNumberMskuParamValue.setMskuId(MSKU_ID_1)
            .setMdmParamId(KnownMdmParams.DOCUMENT_REG_NUMBER)
            .setString("reg_number")
            .setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        MskuParamValue supplyScheduleMskuParamValue = new MskuParamValue();
        supplyScheduleMskuParamValue.setMskuId(MSKU_ID_1)
            .setMdmParamId(KnownMdmParams.SUPPLY_SCHEDULE)
            .setOption(new MdmParamOption().setId(1).setRenderedValue("MONDAY"))
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        CommonMsku mskuToUpdate = new CommonMsku(0L, MSKU_ID_1)
            .addParamValue(regNumberMskuParamValue)
            .addParamValue(supplyScheduleMskuParamValue);

        controller.update(mskuToUpdate, null);
        ModelStorage.Model resultModel = mboModelsService.loadRawModels(List.of(MSKU_ID_1)).get(0);
        Assertions.assertThat(resultModel).isEqualTo(MODEL1);
    }

    @Test
    public void whenImportShouldReturnValidationErrorIfColumnContainsValueOfIrrelevantType() {
        ExcelFile.Builder excel = mskuMdmParamExcelService.generateEmptyExcelWithHeaderToUpdateMskus().toBuilder();
        excel.addLine(List.of(
            String.valueOf(MSKU_ID_1), "", "", "", "", "", "", "", "", "", "", "", "", "",
            "irrelevant_value_for_heavy_good", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "2020-10-22T21:51:39.369091Z", "MDM_DEFAULT"));

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(excel.build()));

        ImportResult importResult = controller.importFromExcel(multipartFile, null);

        Assertions.assertThat(importResult.getStatus()).isEqualTo(FileStatus.VALIDATION_ERRORS);
        Assertions.assertThat(importResult.getErrors()).containsExactly("Ключ 100, параметр \"Тяжеловесный и " +
            "крупногабаритный\": значение \"irrelevant_value_for_heavy_good\" должно быть \"да\" или \"нет\"");
    }

    @Test
    public void whenImportShouldReturnInternalErrorIfEmptyFileGiven() {
        ExcelFile.Builder excel = mskuMdmParamExcelService.generateEmptyExcelWithHeaderToUpdateMskus().toBuilder();

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(excel.build()));

        ImportResult importResult = controller.importFromExcel(multipartFile, null);

        Assertions.assertThat(importResult.getStatus()).isEqualTo(FileStatus.INTERNAL_ERROR);
        Assertions.assertThat(importResult.getErrors()).containsExactly("Набор MSKU пуст");
    }

    @Test
    public void whenImportShouldReturnValidationErrorIfHonestSignDateIsInvalid() {
        ExcelFile.Builder excel = mskuMdmParamExcelService.generateEmptyExcelWithHeaderToUpdateMskus().toBuilder();
        excel.addLine(List.of(
            String.valueOf(MSKU_ID_3),
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "invalid_date",
            "", "",
            "2020-10-22T21:51:39.369091Z", "MDM_DEFAULT"));

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(excel.build()));

        ImportResult importResult = controller.importFromExcel(multipartFile, null);

        Assertions.assertThat(importResult.getStatus()).isEqualTo(FileStatus.VALIDATION_ERRORS);
        Assertions.assertThat(importResult.getErrors()).containsExactly("Ключ 102, параметр \"Дата обязательной марк." +
            " Честный ЗНАК\": Параметр \"Дата обязательной марк. Честный ЗНАК\" должен быть задан как дата "
            + MdmParamExcelAttributes.HONEST_SIGN_DATE_PATTERN +
            "\"invalid_date\"");
    }

    @Test
    public void whenImportShouldReturnValidationErrorIfShelfServiceIsInvalid() {
        ExcelFile.Builder excel = mskuMdmParamExcelService.generateEmptyExcelWithHeaderToUpdateMskus().toBuilder();
        excel.addLine(List.of(
            String.valueOf(MSKU_ID_3),
            "", "", "", "", "non_numeric_life_time", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "",
            "2020-10-22T21:51:39.369091Z", "MDM_DEFAULT"));

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(excel.build()));

        ImportResult importResult = controller.importFromExcel(multipartFile, null);

        Assertions.assertThat(importResult.getStatus()).isEqualTo(FileStatus.VALIDATION_ERRORS);
        Assertions.assertThat(importResult.getErrors()).containsExactly("Ключ 102, параметр \"Срок службы\": " +
            "Параметр\"Срок службы\" должен быть задан как число \"non_numeric_life_time\"");
    }

    @Test
    public void whenImportShouldReturnValidationErrorIfWarrantyPeriodIsInvalid() {
        ExcelFile.Builder excel = mskuMdmParamExcelService.generateEmptyExcelWithHeaderToUpdateMskus().toBuilder();
        excel.addLine(List.of(
            String.valueOf(MSKU_ID_3),
            "", "", "", "", "", "", "", "", "non_numeric_guarantee_period", "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "",
            "2020-10-22T21:51:39.369091Z", "MDM_DEFAULT"));

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(excel.build()));

        ImportResult importResult = controller.importFromExcel(multipartFile, null);

        Assertions.assertThat(importResult.getStatus()).isEqualTo(FileStatus.VALIDATION_ERRORS);
        Assertions.assertThat(importResult.getErrors()).containsExactly("Ключ 102, параметр \"Гарантийный срок\": " +
            "Параметр \"Гарантийный срок\" должен быть задан как число \"non_numeric_guarantee_period\"");
    }

    @Test
    public void whenImportShouldReturnValidationErrorIfPreciousGoodIsInvalid() {
        ExcelFile.Builder excel = mskuMdmParamExcelService.generateEmptyExcelWithHeaderToUpdateMskus().toBuilder();
        excel.addLine(List.of(
            String.valueOf(MSKU_ID_3),
            "", "", "", "", "", "", "", "", "1", "не ограничен", "", "", "", "", "", "invalid", "", "", "",
            "", "", "", "", "", "", "", "",
            "2020-10-22T21:51:39.369091Z", "MDM_DEFAULT"));

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(excel.build()));

        ImportResult importResult = controller.importFromExcel(multipartFile, null);

        Assertions.assertThat(importResult.getStatus()).isEqualTo(FileStatus.VALIDATION_ERRORS);
        Assertions.assertThat(importResult.getErrors()).containsExactly(
            "Ключ 102, параметр \"Ценное\": значение \"invalid\" должно быть \"да\" или \"нет\""
        );
    }

    @Test
    public void whenImportShouldReturnOkIfFilledOnlyMskuIdColumn() {
        ExcelFile.Builder excel = mskuMdmParamExcelService.generateEmptyExcelWithHeaderToUpdateMskus().toBuilder();
        // не учитываем 1-ю колонку с mskuId
        List<String> emptyColumns = Collections.nCopies(excel.getHeadersSize() - 1, "");
        excel.addLine(ListUtils.union(List.of(String.valueOf(MSKU_ID_1)), emptyColumns));

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(excel.build()));

        ImportResult importResult = controller.importFromExcel(multipartFile, null);

        Assertions.assertThat(importResult.getStatus()).isEqualTo(FileStatus.OK);
    }

    @Test
    public void whenImportShouldSuccessfullyUpdateAllMboParamValues() {
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setMskuId(MSKU_ID_3)
            .setSupplierId(SUPPLIER_1)
            .setShopSku(SKU_SUPPLIER_1)
            .setCategoryId(Long.valueOf(CATEGORY_ID).intValue())
        );
        mdmSupplierRepository.insertOrUpdateAll(
            List.of(
                new MdmSupplier()
                    .setId(BUSINESS)
                    .setBusinessEnabled(false)
                    .setType(MdmSupplierType.BUSINESS),
                new MdmSupplier()
                    .setId(SUPPLIER_1)
                    .setBusinessEnabled(true)
                    .setBusinessId(BUSINESS)
                    .setType(MdmSupplierType.THIRD_PARTY)));
        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_1, SKU_SUPPLIER_1), true);

        ExcelFile.Builder excel = mskuMdmParamExcelService.generateEmptyExcelWithHeaderToUpdateMskus().toBuilder();
        excel.addLine(List.of(
            String.valueOf(MSKU_ID_3),
            "500", //в модели 2.5
            "дни", //в модели годы
            "Хранить в холодильнике", //в модели Хранить в прохладном месте
            "да",
            "1.5", //в модели 444
            "годы", //в модели дни
            "Используйте с умом", //в модели Использовать по назначению
            "нет",
            "1",   //в модели 12
            "годы", //в модели месяцы
            "Бесконечная гарантия", //Гарантируем гарантию
            "нет",
            "нет", //в MODEL1 true
            "нет", //в MODEL1 true
            "нет", //в MODEL1 true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "01.07.2020 00:00:00", //в модели 1.10
            "нет", //в модели true
            "нет", //в модели true
            "2020-10-22T21:51:39.369091Z", "MDM_DEFAULT"));

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(excel.build()));

        ImportResult importResult = controller.importFromExcel(multipartFile, null);

        Assertions.assertThat(importResult.getStatus()).isEqualTo(FileStatus.OK);

        ModelStorage.Model resultModel = mboModelsService.loadRawModels(List.of(MSKU_ID_3)).get(0);
        Map<Long, ModelStorage.ParameterValue> resultParamValues = resultModel.getParameterValuesList().stream()
            .collect(Collectors.toMap(ModelStorage.ParameterValue::getParamId, Function.identity()));

        //проверяем значения всех параметров модели
        assertNumericVariable(resultParamValues, KnownMdmMboParams.LIFE_SHELF_PARAM_ID,
            SHELF_LIFE_MBO_PARAM_VALUE, "500");

        assertEnumVariable(resultParamValues, KnownMdmMboParams.SHELF_LIFE_UNIT_PARAM_ID,
            SHELF_LIFE_UNIT_MBO_PARAM_VALUE, KnownMdmMboParams.SHELF_LIFE_OPTION_IDS.get(TimeInUnits.TimeUnit.DAY));

        assertStringVariable(resultParamValues, KnownMdmMboParams.SHELF_LIFE_COMMENT_PARAM_ID,
            SHELF_LIFE_COMMENT_MBO_PARAM_VALUE, "Хранить в холодильнике");

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HIDE_SHELF_LIFE_PARAM_ID,
            HIDE_SHELF_LIFE_MBO_PARAM_VALUE, true, KnownMdmMboParams.HIDE_SHELF_LIFE_TRUE_OPTION_ID);

        assertStringVariable(resultParamValues, KnownMdmMboParams.LIFE_TIME_PARAM_ID,
            SHELF_SERVICE_MBO_PARAM_VALUE, "1.5");

        assertEnumVariable(resultParamValues, KnownMdmMboParams.LIFE_TIME_UNIT_PARAM_ID,
            SHELF_SERVICE_UNIT_MBO_PARAM_VALUE, KnownMdmMboParams.LIFE_TIME_OPTION_IDS.get(TimeInUnits.TimeUnit.YEAR));

        assertStringVariable(resultParamValues, KnownMdmMboParams.LIFE_TIME_COMMENT_PARAM_ID,
            SHELF_SERVICE_COMMENT_MBO_PARAM_VALUE, "Используйте с умом");

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HIDE_LIFE_TIME_PARAM_ID,
            HIDE_LIFE_TIME_MBO_PARAMETER_VALUE, false, KnownMdmMboParams.HIDE_LIFE_TIME_FALSE_OPTION_ID);

        assertStringVariable(resultParamValues, KnownMdmMboParams.WARRANTY_PERIOD_PARAM_ID,
            GUARANTEE_PERIOD_MBO_PARAM_VALUE, "1");

        assertEnumVariable(resultParamValues, KnownMdmMboParams.GUARANTEE_PERIOD_UNIT_PARAM_ID,
            GUARANTEE_PERIOD_UNIT_MBO_PARAM_VALUE,
            KnownMdmMboParams.GUARANTEE_PERIOD_OPTION_IDS.get(TimeInUnits.TimeUnit.YEAR));

        assertStringVariable(resultParamValues, KnownMdmMboParams.GUARANTEE_PERIOD_COMMENT_PARAM_ID,
            GUARANTEE_PERIOD_COMMENT_MBO_PARAM_VALUE, "Бесконечная гарантия");

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HIDE_GUARANTEE_PERIOD_PARAM_ID,
            HIDE_GUARANTEE_PERIOD_MBO_PARAM_VALUE, false, KnownMdmMboParams.HIDE_GUARANTEE_PERIOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.EXPIR_DATE_PARAM_ID, EXPIR_DATE_MBO_PARAM_VALUE,
            false, KnownMdmMboParams.EXPIR_DATE_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID,
            HEAVY_GOOD_MBO_PARAM_VALUE, false, HEAVY_GOOD_CATEGORY_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID,
            HEAVY_GOOD_20_MBO_PARAM_VALUE, false, HEAVY_GOOD_CATEGORY_20_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.PRECIOUS_GOOD_PARAM_ID,
            PRECIOUS_GOOD_MBO_PARAM_VALUE, false, PRECIOUS_GOOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HOUSEHOLD_CHEMICALS_PARAM_ID,
            HOUSEHOLD_CHEMICALS_MBO_PARAM_VALUE, false, HOUSEHOLD_CHEMICALS_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.FOOD_PARAM_ID, FOOD_MBO_PARAM_VALUE,
            false, FOOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.INTIMATE_GOOD_PARAM_ID,
            INTIMATE_GOOD_MBO_PARAM_VALUE, false, INTIMATE_GOOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.SMELLING_GOOD_PARAM_ID,
            SMELLING_GOOD_MBO_PARAM_VALUE, false, SMELLING_GOOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.DANGEROUS_GOOD_PARAM_ID,
            DANGEROUS_GOOD_MBO_PARAM_VALUE, false, DANGEROUS_GOOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HONEST_SIGN_OPTIONAL_PARAM_ID,
            HONEST_SIGN_OPTIONAL_MBO_PARAM_VALUE, false, KnownMdmMboParams.HONEST_SIGN_OPTIONAL_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HONEST_SIGN_DISTINCT_PARAM_ID,
            HONEST_SIGN_DISTINCT_MBO_PARAM_VALUE, false, KnownMdmMboParams.HONEST_SIGN_DISTINCT_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HONEST_SIGN_REQUIRED_PARAM_ID,
            HONEST_SIGN_REQUIRED_MBO_PARAM_VALUE, false, KnownMdmMboParams.HONEST_SIGN_REQUIRED_FALSE_OPTION_ID);

        assertStringVariable(resultParamValues, KnownMdmMboParams.HONEST_SIGN_ACTIVATION_PARAM_ID,
            HONEST_SIGN_ACTIVATION_MBO_PARAM_VALUE, "01.07.2020 00:00:00");

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.IMEI_CONTROL_PARAM_ID, IMEI_CONTROL_MBO_PARAM_VALUE,
            false, KnownMdmMboParams.IMEI_CONTROL_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.SERIAL_NUMBER_CONTROL_PARAM_ID,
            SERIAL_NUMBER_CONTROL_MBO_PARAM_VALUE, false, KnownMdmMboParams.SERIAL_NUMBER_CONTROL_FALSE_OPTION_ID);

        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedBatch(1000))
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactly(new ShopSkuKey(BUSINESS, SKU_SUPPLIER_1));
    }

    @Test
    public void whenAllRelatedMdmParamsShouldBeGeneratedAndGoldenItemRecalculatedDuringImport() {
        keyValueService.putValue(MdmProperties.IMEI_MASK, "imei_mask");
        keyValueService.putValue(MdmProperties.SERIAL_NUMBER_MASK, "sn_mask");

        ExcelFile.Builder excel = mskuMdmParamExcelService.generateEmptyExcelWithHeaderToUpdateMskus().toBuilder();
        excel.addLine(List.of(
            String.valueOf(MSKU_ID_4),
            "500", //в модели 2.5
            "дни", //в модели годы
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "нет", //признак контроля imei в модели отсутствует
            "да", //признак контроля sn в модели отсутствует
            "2020-10-22T21:51:39.369091Z", "MDM_DEFAULT"));

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(excel.build()));

        // сгенерируем данные для расчета золотой записи
        ModelKey key = new ModelKey(1L, MSKU_ID_4);

        // 2. Сгенерим SSKU мастер-данные
        ShopSkuKey offer = new ShopSkuKey(12, "123");
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(offer);
        masterDataRepository.insert(masterData);

        // добавим категорийную настройку
        CategoryParamValue categoryValue = new CategoryParamValue().setCategoryId(key.getCategoryId());
        categoryValue.setMdmParamId(KnownMdmParams.IMEI_CONTROL);
        categoryValue.setBool(true); // в импортируемом файле - false
        categoryValue.setXslName("-");
        categoryValue.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);
        categoryParamValueRepository.insert(categoryValue);

        // создадим маппинг: модель + категория + оффер.
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offer).setUpdateStamp(1L));

        // ДО ИМОПРТА ФАЙЛА
        // проверяем, что таблица с msku-параметрами пустая
        Assertions.assertThat(mskuRepository.findAllMskus()).isEmpty();

        // проверяем, что очередь на перевычисление золотой записи пустая
        List<MdmMskuQueueInfo> mskusToRefresh = mskuQueue.findAll();
        Assertions.assertThat(mskusToRefresh.isEmpty()).isEqualTo(true);

        ImportResult importResult = controller.importFromExcel(multipartFile, null);

        // ПОСЛЕ ИМПОРТА ФАЙЛА
        Assertions.assertThat(importResult.getStatus()).isEqualTo(FileStatus.OK);

        ModelStorage.Model resultModel = mboModelsService.loadRawModels(List.of(MSKU_ID_4)).get(0);
        Map<Long, ModelStorage.ParameterValue> resultParamValues = resultModel.getParameterValuesList().stream()
            .collect(Collectors.toMap(ModelStorage.ParameterValue::getParamId, Function.identity()));

        // 4 колонки из excel + 2 сгенерированных параметра маски
        Assertions.assertThat(resultParamValues.size()).isEqualTo(6);

        // проверяем значения всех параметров модели
        assertNumericVariable(resultParamValues, KnownMdmMboParams.LIFE_SHELF_PARAM_ID,
            SHELF_LIFE_MBO_PARAM_VALUE, "500");

        assertEnumVariable(resultParamValues, KnownMdmMboParams.SHELF_LIFE_UNIT_PARAM_ID,
            SHELF_LIFE_UNIT_MBO_PARAM_VALUE, KnownMdmMboParams.SHELF_LIFE_OPTION_IDS.get(TimeInUnits.TimeUnit.DAY));

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.IMEI_CONTROL_PARAM_ID, IMEI_CONTROL_MBO_PARAM_VALUE,
            false, KnownMdmMboParams.IMEI_CONTROL_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.SERIAL_NUMBER_CONTROL_PARAM_ID,
            SERIAL_NUMBER_CONTROL_MBO_PARAM_VALUE,
            true, KnownMdmMboParams.SERIAL_NUMBER_CONTROL_TRUE_OPTION_ID);

        assertStringVariable(resultParamValues, KnownMdmMboParams.IMEI_MASK_PARAM_ID,
            IMEI_MASK_MBO_PARAM_VALUE, keyValueService.getString(MdmProperties.IMEI_MASK, ""));

        assertStringVariable(resultParamValues, KnownMdmMboParams.SERIAL_NUMBER_MASK_PARAM_ID,
            SN_MASK_MBO_PARAM_VALUE, keyValueService.getString(MdmProperties.SERIAL_NUMBER_MASK, ""));

        // проверяем, что msku добавилась в очередь на перевычисление золотой записи
        mskusToRefresh = mskuQueue.findAll();
        Assertions.assertThat(mskusToRefresh.size()).isEqualTo(1);

        // запускаем перерасчет золотой записи
        executor.execute();

        // проверяем, что значения связанных параметров (см. KnownMdmParamRelations) сохранились с макс. приоритетом
        // в хранилище золота msku мдм после перерасчета золотой записи
        Map<Long, CommonMsku> repoMskus = mskuRepository.findAllMskus();
        Assertions.assertThat(repoMskus).hasSize(1);
        CommonMsku msku = repoMskus.get(key.getModelId());
        Assertions.assertThat(msku).isNotNull();

        // 2 SN + 2 IMEI + 2 shelf life + bmdm id + (6 default false HS + mercury)
        Assertions.assertThat(msku.getContainedMdmParamIds()).hasSize(13);
        Assertions.assertThat(msku.getContainedMdmParamIds()).contains(
            KnownMdmParams.IMEI_CONTROL, KnownMdmParams.IMEI_MASK,
            KnownMdmParams.SERIAL_NUMBER_CONTROL, KnownMdmParams.SERIAL_NUMBER_MASK
        );

        Assertions.assertThat(msku.getParamValue(KnownMdmParams.IMEI_CONTROL))
            .map(MdmParamValue::getMasterDataSourceType)
            .contains(MasterDataSourceType.MDM_OPERATOR);
        Assertions.assertThat(msku.getParamValue(KnownMdmParams.IMEI_MASK))
            .map(MdmParamValue::getMasterDataSourceType)
            .contains(MasterDataSourceType.MDM_OPERATOR);
        Assertions.assertThat(msku.getParamValue(KnownMdmParams.SERIAL_NUMBER_CONTROL))
            .map(MdmParamValue::getMasterDataSourceType)
            .contains(MasterDataSourceType.MDM_OPERATOR);
        Assertions.assertThat(msku.getParamValue(KnownMdmParams.SERIAL_NUMBER_MASK))
            .map(MdmParamValue::getMasterDataSourceType)
            .contains(MasterDataSourceType.MDM_OPERATOR);

        Assertions.assertThat(msku.getParamValue(KnownMdmParams.IMEI_CONTROL))
            .flatMap(MdmParamValue::getBool)
            .contains(false);
        Assertions.assertThat(msku.getParamValue(KnownMdmParams.SERIAL_NUMBER_CONTROL))
            .flatMap(MdmParamValue::getBool)
            .contains(true);
    }

    @Test
    public void whenImportAsAdminShouldSuccessfullyUpdateAllMboParamValues() {
        MockMultipartFile multipartFile = generateMultipartFile();

        var sourceTypeId =
            SecurityUtil.getCurrentUserLoginOrDefault(MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID);
        mdmUserRepository.insert(new MdmUser().setLogin(sourceTypeId).setRoles(Set.of("MDM_UI_ADMIN")));
        ImportResult importResult = controller.importFromExcel(multipartFile, MasterDataSourceType.MDM_ADMIN);

        asserCorrectImportMsku3(importResult, MasterDataSourceType.MDM_ADMIN);
    }

    @Test
    public void whenImportAsOeratorShouldSuccessfullyUpdateAllMboParamValues() {
        MockMultipartFile multipartFile = generateMultipartFile();

        var sourceTypeId =
            SecurityUtil.getCurrentUserLoginOrDefault(MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID);
        mdmUserRepository.insert(new MdmUser().setLogin(sourceTypeId).setRoles(Set.of("MDM_UI_OPERATOR")));
        ImportResult importResult = controller.importFromExcel(multipartFile, MasterDataSourceType.MDM_OPERATOR);

        asserCorrectImportMsku3(importResult, MasterDataSourceType.MDM_OPERATOR);
    }

    @Test
    public void whenImportAsAdminShouldReturnAccessError() {
        MockMultipartFile multipartFile = generateMultipartFile();

        var sourceTypeId =
            SecurityUtil.getCurrentUserLoginOrDefault(MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID);
        mdmUserRepository.insert(new MdmUser().setLogin(sourceTypeId).setRoles(Set.of("MDM_UI_OPERATOR")));
        ImportResult importResult = controller.importFromExcel(multipartFile, MasterDataSourceType.MDM_ADMIN);

        Assertions.assertThat(importResult.getStatus()).isEqualTo(FileStatus.VALIDATION_ERRORS);
        Assertions.assertThat(importResult.getErrors()).hasSize(1);
        Assertions.assertThat(importResult.getErrors().get(0))
            .isEqualTo("MskuId: 102, [AccessException: У пользователя недостаточно прав для выполнения операции.]");
    }

    private void addMboUserToMdmRepo() {
        MdmMboUser mboUser = new MdmMboUser()
            .setUid(MBO_USER_ID)
            .setFullName("Test Developer")
            .setStaffLogin("staffLogin")
            .setYandexLogin("yandexLogin");
        mdmMboUsersRepository.insert(mboUser);
    }

    private MockMultipartFile generateMultipartFile() {
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setMskuId(MSKU_ID_3)
            .setSupplierId(SUPPLIER_1)
            .setShopSku(SKU_SUPPLIER_1)
            .setCategoryId(Long.valueOf(CATEGORY_ID).intValue())
        );

        ExcelFile.Builder excel = mskuMdmParamExcelService.generateEmptyExcelWithHeaderToUpdateMskus().toBuilder();
        excel.addLine(List.of(
            String.valueOf(MSKU_ID_3),
            "500", //в модели 2.5
            "дни", //в модели годы
            "Хранить в холодильнике", //в модели Хранить в прохладном месте
            "да",
            "1.5", //в модели 444
            "годы", //в модели дни
            "Используйте с умом", //в модели Использовать по назначению
            "нет",
            "1",   //в модели 12
            "годы", //в модели месяцы
            "Бесконечная гарантия", //Гарантируем гарантию
            "нет",
            "нет", //в MODEL1 true
            "нет", //в MODEL1 true
            "нет", //в MODEL1 true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "нет", //в модели true
            "01.07.2020 00:00:00", //в модели 1.10
            "нет", //в модели true
            "нет", //в модели true
            "2020-10-22T21:51:39.369091Z", "MDM_DEFAULT"));

        return new MockMultipartFile("input-excel", ExcelFileConverter.convertToBytes(excel.build()));
    }

    private void asserCorrectImportMsku3(ImportResult importResult, MasterDataSourceType expected) {
        Assertions.assertThat(importResult.getStatus()).isEqualTo(FileStatus.OK);

        ModelStorage.Model resultModel = mboModelsService.loadRawModels(List.of(MSKU_ID_3)).get(0);
        Map<Long, ModelStorage.ParameterValue> resultParamValues = resultModel.getParameterValuesList().stream()
            .collect(Collectors.toMap(ModelStorage.ParameterValue::getParamId, Function.identity()));

        //проверяем значения всех параметров модели
        assertNumericVariable(resultParamValues, KnownMdmMboParams.LIFE_SHELF_PARAM_ID,
            SHELF_LIFE_MBO_PARAM_VALUE, "500");

        assertEnumVariable(resultParamValues, KnownMdmMboParams.SHELF_LIFE_UNIT_PARAM_ID,
            SHELF_LIFE_UNIT_MBO_PARAM_VALUE, KnownMdmMboParams.SHELF_LIFE_OPTION_IDS.get(TimeInUnits.TimeUnit.DAY));

        assertStringVariable(resultParamValues, KnownMdmMboParams.SHELF_LIFE_COMMENT_PARAM_ID,
            SHELF_LIFE_COMMENT_MBO_PARAM_VALUE, "Хранить в холодильнике");

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HIDE_SHELF_LIFE_PARAM_ID,
            HIDE_SHELF_LIFE_MBO_PARAM_VALUE, true, KnownMdmMboParams.HIDE_SHELF_LIFE_TRUE_OPTION_ID);

        assertStringVariable(resultParamValues, KnownMdmMboParams.LIFE_TIME_PARAM_ID,
            SHELF_SERVICE_MBO_PARAM_VALUE, "1.5");

        assertEnumVariable(resultParamValues, KnownMdmMboParams.LIFE_TIME_UNIT_PARAM_ID,
            SHELF_SERVICE_UNIT_MBO_PARAM_VALUE, KnownMdmMboParams.LIFE_TIME_OPTION_IDS.get(TimeInUnits.TimeUnit.YEAR));

        assertStringVariable(resultParamValues, KnownMdmMboParams.LIFE_TIME_COMMENT_PARAM_ID,
            SHELF_SERVICE_COMMENT_MBO_PARAM_VALUE, "Используйте с умом");

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HIDE_LIFE_TIME_PARAM_ID,
            HIDE_LIFE_TIME_MBO_PARAMETER_VALUE, false, KnownMdmMboParams.HIDE_LIFE_TIME_FALSE_OPTION_ID);

        assertStringVariable(resultParamValues, KnownMdmMboParams.WARRANTY_PERIOD_PARAM_ID,
            GUARANTEE_PERIOD_MBO_PARAM_VALUE, "1");

        assertEnumVariable(resultParamValues, KnownMdmMboParams.GUARANTEE_PERIOD_UNIT_PARAM_ID,
            GUARANTEE_PERIOD_UNIT_MBO_PARAM_VALUE,
            KnownMdmMboParams.GUARANTEE_PERIOD_OPTION_IDS.get(TimeInUnits.TimeUnit.YEAR));

        assertStringVariable(resultParamValues, KnownMdmMboParams.GUARANTEE_PERIOD_COMMENT_PARAM_ID,
            GUARANTEE_PERIOD_COMMENT_MBO_PARAM_VALUE, "Бесконечная гарантия");

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HIDE_GUARANTEE_PERIOD_PARAM_ID,
            HIDE_GUARANTEE_PERIOD_MBO_PARAM_VALUE, false, KnownMdmMboParams.HIDE_GUARANTEE_PERIOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.EXPIR_DATE_PARAM_ID, EXPIR_DATE_MBO_PARAM_VALUE,
            false, KnownMdmMboParams.EXPIR_DATE_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID,
            HEAVY_GOOD_MBO_PARAM_VALUE, false, HEAVY_GOOD_CATEGORY_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID,
            HEAVY_GOOD_20_MBO_PARAM_VALUE, false, HEAVY_GOOD_CATEGORY_20_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.PRECIOUS_GOOD_PARAM_ID,
            PRECIOUS_GOOD_MBO_PARAM_VALUE, false, PRECIOUS_GOOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HOUSEHOLD_CHEMICALS_PARAM_ID,
            HOUSEHOLD_CHEMICALS_MBO_PARAM_VALUE, false, HOUSEHOLD_CHEMICALS_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.FOOD_PARAM_ID, FOOD_MBO_PARAM_VALUE,
            false, FOOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.INTIMATE_GOOD_PARAM_ID,
            INTIMATE_GOOD_MBO_PARAM_VALUE, false, INTIMATE_GOOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.SMELLING_GOOD_PARAM_ID,
            SMELLING_GOOD_MBO_PARAM_VALUE, false, SMELLING_GOOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.DANGEROUS_GOOD_PARAM_ID,
            DANGEROUS_GOOD_MBO_PARAM_VALUE, false, DANGEROUS_GOOD_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HONEST_SIGN_OPTIONAL_PARAM_ID,
            HONEST_SIGN_OPTIONAL_MBO_PARAM_VALUE, false, KnownMdmMboParams.HONEST_SIGN_OPTIONAL_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HONEST_SIGN_DISTINCT_PARAM_ID,
            HONEST_SIGN_DISTINCT_MBO_PARAM_VALUE, false, KnownMdmMboParams.HONEST_SIGN_DISTINCT_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.HONEST_SIGN_REQUIRED_PARAM_ID,
            HONEST_SIGN_REQUIRED_MBO_PARAM_VALUE, false, KnownMdmMboParams.HONEST_SIGN_REQUIRED_FALSE_OPTION_ID);

        assertStringVariable(resultParamValues, KnownMdmMboParams.HONEST_SIGN_ACTIVATION_PARAM_ID,
            HONEST_SIGN_ACTIVATION_MBO_PARAM_VALUE, "01.07.2020 00:00:00");

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.IMEI_CONTROL_PARAM_ID, IMEI_CONTROL_MBO_PARAM_VALUE,
            false, KnownMdmMboParams.IMEI_CONTROL_FALSE_OPTION_ID);

        assertBooleanVariable(resultParamValues, KnownMdmMboParams.SERIAL_NUMBER_CONTROL_PARAM_ID,
            SERIAL_NUMBER_CONTROL_MBO_PARAM_VALUE, false, KnownMdmMboParams.SERIAL_NUMBER_CONTROL_FALSE_OPTION_ID);


        CommonMsku mdmMsku = mskuRepository.findMsku(MSKU_ID_3).orElseThrow();
        Assertions.assertThat(mdmMsku).isNotNull();
        Assertions.assertThat(mdmMsku.getParamValues().values())
            .filteredOn(pv -> pv.getMdmParamId() != KnownMdmParams.BMDM_ID)
            .allMatch(pv -> pv.getMasterDataSourceType() == expected);
    }


    private static ModelStorage.LocalizedString toLocalizedString(String val) {
        return ModelStorage.LocalizedString.newBuilder()
            .setValue(val)
            .setIsoCode(Language.forId(ISO_CODE_RU).getIsoCode())
            .build();
    }

    private static void assertEnumVariable(Map<Long, ModelStorage.ParameterValue> resultParamValues,
                                           long mboParamId,
                                           ModelStorage.ParameterValue mboParamValue,
                                           long expectedValue) {
        ModelStorage.ParameterValue updShelfLifeUnit = resultParamValues.get(mboParamId);
        Assertions.assertThat(updShelfLifeUnit).isEqualToIgnoringGivenFields(mboParamValue,
            "userId_", "optionId_", "valueSource_", "modificationDate_");
        Assertions.assertThat(updShelfLifeUnit.getOptionId()).isEqualTo(Math.toIntExact(expectedValue));
        Assertions.assertThat(updShelfLifeUnit.getValueSource()).isEqualTo(ModelStorage.ModificationSource.MDM);
        Assertions.assertThat(updShelfLifeUnit.getUserId()).isEqualTo(MBO_USER_ID);
    }

    private static void assertBooleanVariable(Map<Long, ModelStorage.ParameterValue> resultParamValues,
                                              long mboParamId,
                                              ModelStorage.ParameterValue mboParamValue,
                                              boolean expectedValue, int expectedOptionId) {
        ModelStorage.ParameterValue updMboParameterValue = resultParamValues.get(mboParamId);
        Assertions.assertThat(updMboParameterValue).isEqualToIgnoringGivenFields(mboParamValue,
            "userId_", "boolValue_", "optionId_", "valueSource_", "modificationDate_");
        Assertions.assertThat(updMboParameterValue.getBoolValue()).isEqualTo(expectedValue);
        Assertions.assertThat(updMboParameterValue.getOptionId()).isEqualTo(expectedOptionId);
        Assertions.assertThat(updMboParameterValue.getValueSource()).isEqualTo(ModelStorage.ModificationSource.MDM);
        Assertions.assertThat(updMboParameterValue.getUserId()).isEqualTo(MBO_USER_ID);
    }

    private static void assertStringVariable(Map<Long, ModelStorage.ParameterValue> resultParamValues,
                                             long mboParamId,
                                             ModelStorage.ParameterValue mboParamValue,
                                             String expectedValue) {
        ModelStorage.ParameterValue updMboParameterValue = resultParamValues.get(mboParamId);
        Assertions.assertThat(updMboParameterValue).isEqualToIgnoringGivenFields(mboParamValue,
            "userId_", "strValue_", "valueType_", "valueSource_", "modificationDate_");
        List<ModelStorage.LocalizedString> honestSignStrValueList = updMboParameterValue.getStrValueList();
        Assertions.assertThat(honestSignStrValueList.get(0).getIsoCode()).isEqualTo("ru");
        Assertions.assertThat(honestSignStrValueList.get(0).getValue()).isEqualTo(expectedValue);
        Assertions.assertThat(updMboParameterValue.getValueType()).isEqualTo(MboParameters.ValueType.STRING);
        Assertions.assertThat(updMboParameterValue.getValueSource()).isEqualTo(ModelStorage.ModificationSource.MDM);
        Assertions.assertThat(updMboParameterValue.getUserId()).isEqualTo(MBO_USER_ID);
    }

    private static void assertNumericVariable(Map<Long, ModelStorage.ParameterValue> resultParamValues,
                                              long mboParamId,
                                              ModelStorage.ParameterValue mboParamValue,
                                              String expectedValue) {
        ModelStorage.ParameterValue updShelfLife = resultParamValues.get(mboParamId);
        Assertions.assertThat(updShelfLife).isEqualToIgnoringGivenFields(mboParamValue,
            "userId_", "numericValue_", "valueType_", "valueSource_", "modificationDate_");
        String numericValue = updShelfLife.getNumericValue();
        Assertions.assertThat(numericValue).isEqualTo(expectedValue);
        Assertions.assertThat(updShelfLife.getValueType()).isEqualTo(MboParameters.ValueType.NUMERIC);
        Assertions.assertThat(updShelfLife.getValueSource()).isEqualTo(ModelStorage.ModificationSource.MDM);
        Assertions.assertThat(updShelfLife.getUserId()).isEqualTo(MBO_USER_ID);
    }

    private static ModelStorage.ParameterValue createBooleanParamValue(long paramId, boolean value, int optionId,
                                                                       String xslName) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(paramId)
            .setTypeId(ModelStorage.ParameterValueType.BOOLEAN_VALUE)
            .setBoolValue(value)
            .setOptionId(optionId)
            .setXslName(xslName)
            .setValueSource(ModelStorage.ModificationSource.TOOL)
            .setUserId(KnownMdmMboParams.AUTO_USER)
            .setModificationDate(TIMESTAMP)
            .setValueType(MboParameters.ValueType.BOOLEAN)
            .build();
    }

    private static ModelStorage.ParameterValue createStringParamValue(long paramId, String value, String xslName) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(paramId)
            .setTypeId(ModelStorage.ParameterValueType.STRING_VALUE)
            .addStrValue(toLocalizedString(value))
            .setXslName(xslName)
            .setValueSource(ModelStorage.ModificationSource.MDM)
            .setUserId(KnownMdmMboParams.AUTO_USER)
            .setModificationDate(TIMESTAMP)
            .setValueType(MboParameters.ValueType.STRING)
            .build();
    }

    private static ModelStorage.ParameterValue createEnumParamValue(long paramId, long optionId, String xslName) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(paramId)
            .setTypeId(ModelStorage.ParameterValueType.ENUM_VALUE)
            .setOptionId(Math.toIntExact(optionId))
            .setXslName(xslName)
            .setValueSource(ModelStorage.ModificationSource.MDM)
            .setUserId(KnownMdmMboParams.AUTO_USER)
            .setModificationDate(TIMESTAMP)
            .setValueType(MboParameters.ValueType.ENUM)
            .build();
    }

    private static ModelStorage.ParameterValue createNumericParamValue(long paramId, String value, String xslName) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(paramId)
            .setTypeId(ModelStorage.ParameterValueType.NUMERIC_VALUE)
            .setNumericValue(value)
            .setXslName(xslName)
            .setValueSource(ModelStorage.ModificationSource.MDM)
            .setUserId(KnownMdmMboParams.AUTO_USER)
            .setModificationDate(TIMESTAMP)
            .setValueType(MboParameters.ValueType.NUMERIC)
            .build();
    }

    private static ModelStorage.ParameterValue createNumericEnumParamValue(long paramId, int optionId, String xslName) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(paramId)
            .setTypeId(ModelStorage.ParameterValueType.NUMERIC_ENUM_VALUE)
            .setOptionId(optionId)
            .setXslName(xslName)
            .setValueSource(ModelStorage.ModificationSource.MDM)
            .setUserId(KnownMdmMboParams.AUTO_USER)
            .setModificationDate(TIMESTAMP)
            .setValueType(MboParameters.ValueType.NUMERIC_ENUM)
            .build();
    }

}
