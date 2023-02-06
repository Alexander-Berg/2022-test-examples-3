package ru.yandex.market.mdm.app.proto;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.utils.ValidationContextHelperService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.BusinessLockStatusRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MasterDataLogIdService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.services.StorageKeyValueCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.StorageKeyValueCachingServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.ExistingCCCodeCacheMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.VghValidationRequirementsProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.CustomsCommodityCodeBlockValidator;
import ru.yandex.market.mbo.mdm.common.service.MdmBusinessMigrationMonitoringServiceMock;
import ru.yandex.market.mbo.mdm.common.service.MdmSolomonPushService;
import ru.yandex.market.mbo.mdm.common.service.SskuValidationService;
import ru.yandex.market.mbo.mdm.common.service.SskuValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.service.monitoring.MdmBusinessMigrationMonitoringService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.services.document.DocumentService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mdm.http.MasterDataProto;
import ru.yandex.market.mdm.http.MdmCommon;

/**
 * @author dmserebr
 * @date 31/08/2020
 */
public class MasterDataServiceImplValidateTest extends MdmBaseDbTestClass {
    private static final int SUPPLIER_ID = 1234;
    private static final String SHOP_SKU = "00065.shopSku";

    private static final ShopSkuKey SHOP_SKU_KEY = new ShopSkuKey(SUPPLIER_ID, SHOP_SKU);

    private static final int CATEGORY_ID = 12;
    private static final int MSKU_ID = 123456;

    private static final int BUSINESS_ID = 999;
    private static final int SERVICE_ID = 9990;
    private static final int FP_BUSINESS_ID = BeruIdMock.DEFAULT_PROD_BIZ_ID;
    private static final int FP_SERVICE_ID = BeruIdMock.DEFAULT_PROD_FP_ID;

    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private MasterDataLogIdService masterDataLogIdService;
    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private VerdictCalculationHelper verdictCalculationHelper;
    @Autowired
    private MasterDataValidationService masterDataValidationService;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private ServiceSskuConverter serviceSskuConverter;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private BusinessLockStatusRepository businessLockStatusRepository;
    @Autowired
    private VghValidationRequirementsProvider vghValidationRequirementsProvider;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private MdmSupplierCachingService supplierCachingService;
    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private ExistingCCCodeCacheMock existingCCCodeCacheMock;
    @Autowired
    private DocumentService documentService;

    private MboMappingsService mboMappingsService;
    private StorageKeyValueServiceMock storageKeyValueService;
    private StorageKeyValueCachingService storageKeyValueCachingService;

    private MasterDataServiceImpl masterDataService;

    @Before
    public void before() {
        storageKeyValueService = new StorageKeyValueServiceMock();
        storageKeyValueCachingService = new StorageKeyValueCachingServiceMock(storageKeyValueService);
        mboMappingsService = Mockito.mock(MboMappingsService.class);

        Mockito.when(mboMappingsService.searchLiteApprovedMappingsByKeys(Mockito.any()))
            .thenReturn(MboMappings.SearchLiteMappingsResponse.newBuilder()
                .addMapping(MbocCommon.MappingInfoLite.newBuilder()
                    .setSupplierId(SUPPLIER_ID).setShopSku(SHOP_SKU).setCategoryId(CATEGORY_ID).setModelId(MSKU_ID))
                .build());
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setSupplierId(SUPPLIER_ID)
            .setShopSku(SHOP_SKU)
            .setCategoryId(CATEGORY_ID)
            .setMskuId(MSKU_ID)
        );
        mdmSupplierRepository.insert(new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(SUPPLIER_ID));
        SupplierConverterService supplierConverterService = new SupplierConverterServiceMock();
        ValidationContextHelperService validationContextHelperService = new ValidationContextHelperService(
            mboMappingsService,
            new CategoryParamValueRepositoryMock()
        );

        SskuValidationService sskuValidationService = new SskuValidationServiceImpl(
            masterDataValidationService,
            validationContextHelperService,
                mappingsCacheRepository,
            vghValidationRequirementsProvider,
            mdmSskuGroupManager,
            documentService);

        ReferenceItemRepositoryMock referenceItemRepository = new ReferenceItemRepositoryMock();
        MdmBusinessMigrationMonitoringService businessMigrationMonitoringService =
            new MdmBusinessMigrationMonitoringServiceMock();
        masterDataService = new MasterDataServiceImpl(
            masterDataRepository,
            masterDataLogIdService,
            mboMappingsService,
            new MdmLogbrokerServiceMock(),
            fromIrisItemRepository,
            supplierConverterService,
            storageKeyValueCachingService,
            Mockito.mock(MasterDataBusinessMergeService.class),
            mdmSskuGroupManager,
            serviceSskuConverter,
            referenceItemRepository,
            Mockito.mock(MdmSolomonPushService.class),
            sskuValidationService,
            mdmSupplierRepository,
            businessLockStatusRepository,
            businessMigrationMonitoringService,
            goldSskuRepository,
            new BeruIdMock(),
            silverSskuRepository
        );
    }

    @Test
    public void testSaveAllowedForValidationOnly() {
        prepareSuppliers(false, false);
        var masterData = MdmCommon.SskuMasterData.newBuilder()
            .setSupplierId(SERVICE_ID)
            .setShopSku(SHOP_SKU)
            .addManufacturerCountries("Китай");
        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(masterData)
            .build();
        var response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        for (MasterDataProto.OperationInfo result : response.getResultsList()) {
            Assertions.assertThat(result.getStatus()).isEqualTo(MasterDataProto.OperationStatus.INTERNAL_ERROR);
            Assertions.assertThat(result.getErrors(0).getMessage())
                .containsIgnoringCase(MasterDataServiceImpl.VALIDATE_ERROR_MESSAGE);
        }
    }

    @Test
    public void testSaveByDisabledServiceAllowed() {
        prepareSuppliers(false, false);
        var response = masterDataService.saveSskuMasterData(
            MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
                .setValidateOnly(true)
                .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                    .setSupplierId(SERVICE_ID)
                    .setShopSku(SHOP_SKU)
                    .addManufacturerCountries("Китай"))
                .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                    .setSupplierId(FP_SERVICE_ID)
                    .setShopSku(SHOP_SKU)
                    .addManufacturerCountries("Россия"))
                .build()
        );

        Assertions.assertThat(response.getResultsList()).hasSize(2);
        for (MasterDataProto.OperationInfo result : response.getResultsList()) {
            Assertions.assertThat(result.getStatus()).isEqualTo(MasterDataProto.OperationStatus.OK);
        }
    }

    @Test
    public void testSaveByEkatServiceForbiddenWithFlag() {
        prepareSuppliers(true, true);
        storageKeyValueCachingService.putValue(MdmProperties.FORBID_STAGE_3_SAVE_IN_MDM_API, true);

        // validation without save
        var response = masterDataService.saveSskuMasterData(MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SERVICE_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("Китай"))
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(FP_SERVICE_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("Россия"))
            .setValidateOnly(true)
            .build());

        Assertions.assertThat(response.getResultsList()).hasSize(2);
        for (MasterDataProto.OperationInfo result : response.getResultsList()) {
            Assertions.assertThat(result.getStatus()).isEqualTo(MasterDataProto.OperationStatus.OK);
        }
    }

    @Test
    public void testSaveByEkatServiceAllowedByDefault() {
        prepareSuppliers(true, true);
        // validation without save
        var response = masterDataService.saveSskuMasterData(MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SERVICE_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("Китай"))
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(FP_SERVICE_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("Россия"))
            .setValidateOnly(true)
            .build());

        Assertions.assertThat(response.getResultsList()).hasSize(2);
        for (MasterDataProto.OperationInfo result : response.getResultsList()) {
            Assertions.assertThat(result.getStatus()).isEqualTo(MasterDataProto.OperationStatus.OK);
        }
    }

    @Test
    public void testAnything3pBusinessForbidden() {
        prepareSuppliers(false, true); // екатность сервисов тут по факту не важна

        // validation without save
        var response = masterDataService.saveSskuMasterData(MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(BUSINESS_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("Китай"))
            .setValidateOnly(true)
            .build());

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        for (MasterDataProto.OperationInfo result : response.getResultsList()) {
            Assertions.assertThat(result.getStatus()).isEqualTo(MasterDataProto.OperationStatus.INTERNAL_ERROR);
            Assertions.assertThat(result.getErrors(0).getMessage())
                .containsIgnoringCase("business keys are not allowed");
        }
    }

    @Test
    public void testSaveBy1pBusinessDisabledForbiddenValidationAllowed() {
        prepareSuppliers(true, false);

        // validation without save
        var response = masterDataService.saveSskuMasterData(MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(FP_BUSINESS_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("Китай"))
            .setValidateOnly(true)
            .build());

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        for (MasterDataProto.OperationInfo result : response.getResultsList()) {
            Assertions.assertThat(result.getStatus()).isEqualTo(MasterDataProto.OperationStatus.OK);
        }
    }

    @Test
    public void testSaveSskuMasterDataWithoutShippingUnit() {
        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("China")
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus()).isEqualTo(MasterDataProto.OperationStatus.OK);
    }

    @Test
    public void testSaveInvalidSskuMasterDataWithoutShippingUnit() {
        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("qwjeqkwehkjqhasd")
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus())
            .isEqualTo(MasterDataProto.OperationStatus.VALIDATION_ERROR);
    }

    @Test
    public void testSaveSskuMasterDataWithValidShippingUnit() {
        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("China")
                .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                    .setBoxLengthUm(50000).setBoxWidthUm(50000).setBoxHeightUm(200000).setWeightGrossMg(1000000))
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus()).isEqualTo(MasterDataProto.OperationStatus.OK);
    }

    @Test
    public void testSaveInvalidSskuMasterDataWithValidShippingUnit() {
        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("Chqkweqlkina")
                .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                    .setBoxLengthUm(50000).setBoxWidthUm(50000).setBoxHeightUm(200000).setWeightGrossMg(1000000))
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus())
            .isEqualTo(MasterDataProto.OperationStatus.VALIDATION_ERROR);
    }

    @Test
    public void testSaveSskuMasterDataWithValidShippingUnitIfAlreadyExistsAndTheSame() {
        FromIrisItemWrapper beforeItem = new FromIrisItemWrapper(SHOP_SKU_KEY);
        beforeItem.setReferenceItem(ItemWrapperTestUtil.createItem(SHOP_SKU_KEY,
            MdmIrisPayload.MasterDataSource.SUPPLIER, String.valueOf(SUPPLIER_ID),
            ItemWrapperTestUtil.generateShippingUnit(5.0, 7.0, 10.0, 1.0, null, null)));
        beforeItem.setProcessed(true);
        fromIrisItemRepository.insert(beforeItem);

        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("China")
                .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                    .setBoxLengthUm(50000).setBoxWidthUm(70000).setBoxHeightUm(100000).setWeightGrossMg(1000000))
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus()).isEqualTo(MasterDataProto.OperationStatus.OK);
    }

    @Test
    public void testSaveSskuMasterDataWithValidShippingUnitIfAlreadyExistsAndDifferent() {
        FromIrisItemWrapper beforeItem = new FromIrisItemWrapper(SHOP_SKU_KEY);
        beforeItem.setReferenceItem(ItemWrapperTestUtil.createItem(SHOP_SKU_KEY,
            MdmIrisPayload.MasterDataSource.SUPPLIER, String.valueOf(SUPPLIER_ID),
            ItemWrapperTestUtil.generateShippingUnit(5.0, 7.0, 10.0, 1.0, null, null)));
        beforeItem.setProcessed(true);
        fromIrisItemRepository.insert(beforeItem);

        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("China")
                .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                    .setBoxLengthUm(50000).setBoxWidthUm(70000).setBoxHeightUm(100000).setWeightGrossMg(2000000))
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus()).isEqualTo(MasterDataProto.OperationStatus.OK);
    }

    @Test
    public void testSaveSskuMasterDataWithInvalidShippingUnit() {
        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("China")
                .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                    .setBoxLengthUm(50000).setBoxWidthUm(70000).setBoxHeightUm(100000).setWeightGrossMg(10))
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus())
            .isEqualTo(MasterDataProto.OperationStatus.VALIDATION_ERROR);
    }

    @Test
    public void testSaveSskuMasterDataWithInvalidShippingUnitWhenValidShippingUnitExists() {
        FromIrisItemWrapper beforeItem = new FromIrisItemWrapper(SHOP_SKU_KEY);
        beforeItem.setReferenceItem(ItemWrapperTestUtil.createItem(SHOP_SKU_KEY,
            MdmIrisPayload.MasterDataSource.SUPPLIER, String.valueOf(SUPPLIER_ID),
            ItemWrapperTestUtil.generateShippingUnit(5.0, 7.0, 10.0, 1.0, null, null)));
        beforeItem.setProcessed(true);
        fromIrisItemRepository.insert(beforeItem);

        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("China")
                .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                    .setBoxLengthUm(50000).setBoxWidthUm(70000).setBoxHeightUm(100000).setWeightGrossMg(10))
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus())
            .isEqualTo(MasterDataProto.OperationStatus.VALIDATION_ERROR);
    }

    @Test
    public void testSaveSskuMasterDataWithValidShippingUnitWhenInvalidShippingUnitExists() {
        FromIrisItemWrapper beforeItem = new FromIrisItemWrapper(SHOP_SKU_KEY);
        beforeItem.setReferenceItem(ItemWrapperTestUtil.createItem(SHOP_SKU_KEY,
            MdmIrisPayload.MasterDataSource.SUPPLIER, MasterDataSourceType.INVALID_SUPPLIER_ITEM_SOURCE_ID,
            ItemWrapperTestUtil.generateShippingUnit(5.0, 7.0, 10.0, 0.00001, null, null)));
        beforeItem.setProcessed(true);
        beforeItem.setValidationErrorCount(1);
        fromIrisItemRepository.insert(beforeItem);

        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("China")
                .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                    .setBoxLengthUm(50000).setBoxWidthUm(70000).setBoxHeightUm(100000).setWeightGrossMg(1000000))
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus()).isEqualTo(MasterDataProto.OperationStatus.OK);
    }

    @Test
    public void testSaveSskuMasterDataWithInvalidShippingUnitWhenAnotherInvalidShippingUnitExists() {
        FromIrisItemWrapper beforeItem = new FromIrisItemWrapper(SHOP_SKU_KEY);
        beforeItem.setReferenceItem(ItemWrapperTestUtil.createItem(SHOP_SKU_KEY,
            MdmIrisPayload.MasterDataSource.SUPPLIER, MasterDataSourceType.INVALID_SUPPLIER_ITEM_SOURCE_ID,
            ItemWrapperTestUtil.generateShippingUnit(5.0, 7.0, 10.0, 0.00001, null, null)));
        beforeItem.setProcessed(true);
        beforeItem.setValidationErrorCount(1);
        fromIrisItemRepository.insert(beforeItem);

        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("China")
                .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                    .setBoxLengthUm(50000).setBoxWidthUm(70000).setBoxHeightUm(100000).setWeightGrossMg(20))
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus())
            .isEqualTo(MasterDataProto.OperationStatus.VALIDATION_ERROR);
    }

    @Test
    public void testIfValidateOnlyStillSaveInvalidShippingUnit() {
        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("China")
                .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                    .setBoxLengthUm(50000).setBoxWidthUm(50000).setBoxHeightUm(200000).setWeightGrossMg(10))
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus())
            .isEqualTo(MasterDataProto.OperationStatus.VALIDATION_ERROR);
    }

    @Test
    public void testIfValidateOnlyDoNotSaveValidShippingUnit() {
        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(MdmCommon.SskuMasterData.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .addManufacturerCountries("China")
                .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                    .setBoxLengthUm(50000).setBoxWidthUm(50000).setBoxHeightUm(200000).setWeightGrossMg(1000000))
                .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus()).isEqualTo(MasterDataProto.OperationStatus.OK);

        List<MasterData> masterData = masterDataRepository.findByIds(List.of(SHOP_SKU_KEY));
        Assertions.assertThat(masterData).isEmpty();

        List<FromIrisItemWrapper> actualItems = fromIrisItemRepository.findByShopSkuKeysWithSource(
            List.of(SHOP_SKU_KEY), MdmIrisPayload.MasterDataSource.SUPPLIER, false, true);
        Assertions.assertThat(actualItems).isEmpty();
    }

    @Test
    public void testCustomsCommodityCode1pValidation() {
        var unknownCCCodeErrorCode = MbocErrors.get().mdUnknownCustomsCommodityCode("", "").getErrorCode();
        existingCCCodeCacheMock.deleteAll();
        skv.putValue(
            MdmProperties.CCC_VALIDATION_MODE,
            CustomsCommodityCodeBlockValidator.ValidationMode.GENERATE_ERROR
        );

        String code = "1604320010";
        var request = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(
                MdmCommon.SskuMasterData.newBuilder()
                    .setSupplierId(FP_SERVICE_ID)
                    .setShopSku(SHOP_SKU)
                    .addManufacturerCountries("China")
                    .setCustomsCommodityCode(code)
                    .build())
            .setValidateOnly(true)
            .build();

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus())
            .isEqualTo(MasterDataProto.OperationStatus.VALIDATION_ERROR);
        Assertions.assertThat(response.getResults(0).getErrors(0).getErrorCode())
            .isEqualTo(unknownCCCodeErrorCode);

        existingCCCodeCacheMock.add(code);
        response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(response.getResultsList()).hasSize(1);
        Assertions.assertThat(response.getResults(0).getStatus()).isEqualTo(MasterDataProto.OperationStatus.OK);
    }

    private void prepareSuppliers(boolean enable1p, boolean enable3p) {
        MdmSupplier business = new MdmSupplier().setId(BUSINESS_ID)
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier service = new MdmSupplier().setId(SERVICE_ID)
            .setBusinessEnabled(enable3p)
            .setBusinessId(business.getId())
            .setType(MdmSupplierType.THIRD_PARTY);
        MdmSupplier business1P = new MdmSupplier().setId(FP_BUSINESS_ID)
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier service1P = new MdmSupplier().setId(FP_SERVICE_ID)
            .setBusinessEnabled(enable1p)
            .setBusinessId(business1P.getId())
            .setType(MdmSupplierType.FIRST_PARTY);
        mdmSupplierRepository.insertBatch(business, service, business1P, service1P);
        supplierCachingService.refresh();

        if (enable1p) {
            sskuExistenceRepository.markExistence(new ShopSkuKey(service1P.getId(), SHOP_SKU), true);
        }
        if (enable3p) {
            sskuExistenceRepository.markExistence(new ShopSkuKey(service.getId(), SHOP_SKU), true);
        }
    }

}
