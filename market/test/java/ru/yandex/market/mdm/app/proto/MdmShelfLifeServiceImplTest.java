package ru.yandex.market.mdm.app.proto;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.MdmShelfLife;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.MboMskuUpdateService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.MdmShelfLifeServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsServiceMock;
import ru.yandex.market.mdm.http.MdmCommon;

/**
 * @author dmserebr
 * @date 29/12/2020
 */
public class MdmShelfLifeServiceImplTest extends MdmBaseDbTestClass {
    private static final long MSKU_1 = 1234;
    private static final long MSKU_2 = 12345;
    private static final long MSKU_3 = 56789;
    private static final int CATEGORY_ID_1 = 455;
    private static final int CATEGORY_ID_2 = 566;
    private static final int BUSINESS = 22;
    private static final int SUPPLIER_1 = 23;
    private static final int SUPPLIER_2 = 35;
    private static final String SHOP_SKU_1 = "tqjwke";
    private static final String SHOP_SKU_2 = "tqjwke1";
    private static final String SHOP_SKU_3 = "tqjwke2";
    private static final String MISSING_SHOP_SKU = "missing";
    private static final ShopSkuKey BUSINESS_KEY_1 = new ShopSkuKey(BUSINESS, SHOP_SKU_1);
    private static final ShopSkuKey BUSINESS_KEY_2 = new ShopSkuKey(BUSINESS, SHOP_SKU_2);
    private static final ShopSkuKey BUSINESS_KEY_3 = new ShopSkuKey(BUSINESS, SHOP_SKU_3);
    private static final ShopSkuKey SHOP_SKU_KEY_1 = new ShopSkuKey(SUPPLIER_1, SHOP_SKU_1);
    private static final ShopSkuKey SHOP_SKU_KEY_2 = new ShopSkuKey(SUPPLIER_1, SHOP_SKU_2);
    private static final ShopSkuKey SHOP_SKU_KEY_3 = new ShopSkuKey(SUPPLIER_1, SHOP_SKU_3);
    private static final long TIMESTAMP = 123L;

    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    private MskuToRefreshRepository mskuToRefreshRepository;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private CargoTypeRepository cargoTypeRepository;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;

    private MdmShelfLifeServiceImpl mdmShelfLifeService;
    private MboModelsServiceMock mboModelsService;

    @Before
    public void before() {
        mboModelsService = new MboModelsServiceMock();
        mboModelsService.saveModels(List.of(buildModel(MSKU_1).build()));
        mboModelsService.saveModels(List.of(buildModel(MSKU_3).build()));
        mdmShelfLifeService = new MdmShelfLifeServiceImpl(
            mappingsCacheRepository,
            mskuRepository,
            silverSskuRepository,
            categoryParamValueRepository,
            storageKeyValueService,
            mdmParamCache,
            mdmQueuesManager,
            new MboMskuUpdateService(mboModelsService, cargoTypeRepository), mdmSskuGroupManager);

        mappingsCacheRepository.insert(new MappingCacheDao()
            .setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_1).setMskuId(MSKU_1).setCategoryId(CATEGORY_ID_1));
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_2).setMskuId(MSKU_1).setCategoryId(CATEGORY_ID_1));
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_3).setMskuId(MSKU_2).setCategoryId(CATEGORY_ID_1));
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setSupplierId(SUPPLIER_2).setShopSku(SHOP_SKU_1).setMskuId(MSKU_3).setCategoryId(CATEGORY_ID_2));

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
        sskuExistenceRepository.markExistence(List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3), true);
    }

    @Test
    public void testEmptyRequest() {
        var request = MdmShelfLife.UpdateShelfLifeInfoRequest.newBuilder().build();

        var response = mdmShelfLifeService.updateShelfLifeInfo(request);

        Assertions.assertThat(response.getResultCount()).isEqualTo(1);
        Assertions.assertThat(response.getResult(0).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.VALIDATION_ERROR);
        Assertions.assertThat(response.getResult(0).getMessage())
            .isEqualTo("Shop sku key list is empty");
    }

    @Test
    public void testCannotUpdateShelfLifeOnly() {
        var request = MdmShelfLife.UpdateShelfLifeInfoRequest.newBuilder().addUpdateInfo(
            MdmShelfLife.ShelfLifeInfo.newBuilder()
                .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_1))
                .setShelfLife(MdmCommon.TimeInUnits.newBuilder().setValue(10).setUnit(MdmCommon.TimeUnit.MONTH))
        ).addUpdateInfo(
            MdmShelfLife.ShelfLifeInfo.newBuilder()
                .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_2))
                .setShelfLifeRequired(true)
        ).build();

        var response = mdmShelfLifeService.updateShelfLifeInfo(request);

        Assertions.assertThat(response.getResultCount()).isEqualTo(1);
        Assertions.assertThat(response.getResult(0).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.VALIDATION_ERROR);
        Assertions.assertThat(response.getResult(0).getMessage())
            .isEqualTo("Some of the items do not contain shelfLifeRequired flag");

        Assertions.assertThat(mskuRepository.findAllMskus()).isEmpty();

        List<SskuSilverParamValue> sskuParamValues = silverSskuRepository.findAll();
        Assertions.assertThat(sskuParamValues).isEmpty();
    }

    @Test
    public void testCanUpdateShelfLifeRequiredOnly() {
        var request = MdmShelfLife.UpdateShelfLifeInfoRequest.newBuilder().addUpdateInfo(
            MdmShelfLife.ShelfLifeInfo.newBuilder()
                .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_1))
                .setShelfLifeRequired(true)
        ).build();

        var response = mdmShelfLifeService.updateShelfLifeInfo(request);

        Assertions.assertThat(response.getResultCount()).isEqualTo(1);
        Assertions.assertThat(response.getResult(0).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.OK);
        Assertions.assertThat(response.getResult(0).getMessage())
            .isEqualTo("ShelfLifeRequired updated successfully to true");

        Optional<CommonMsku> msku = mskuRepository.findMsku(MSKU_1);

        assertExpirDateParamValue(msku.orElse(null), MSKU_1, true);

        assertEnqueuedEntities(mskuToRefreshRepository.findAll(), Set.of(MSKU_1));
        assertEnqueuedEntities(sskuToRefreshRepository.findAll(), Set.of(BUSINESS_KEY_1));
        Assertions.assertThat(
            getModelExpirDateParameterValue(MSKU_1)
                .stream()
                .map(ModelStorage.ParameterValue::getBoolValue)
                .findFirst().get()
        ).isTrue();
    }

    @Test
    public void testCannotSetShelfLifeRequiredFlagToTrueWhenCategoryIsFrozenForSuchChanges() {
        // given
        // 1. Задаем категорийную настройку
        CategoryParamValue cpv = new CategoryParamValue();
        cpv.setCategoryId(CATEGORY_ID_1)
            .setMdmParamId(KnownMdmParams.BAN_TO_ADD_EXPIR_DATE_FROM_WMS)
            .setBool(true);
        categoryParamValueRepository.insert(cpv);

        // 2. Формируем запрос, чтобы выставить флажок применимости в true,
        // но такое изменение под запретом из-за категорийной настройки.
        var request = MdmShelfLife.UpdateShelfLifeInfoRequest.newBuilder().addUpdateInfo(
            MdmShelfLife.ShelfLifeInfo.newBuilder()
                .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_1))
                .setShelfLifeRequired(true)
        ).build();

        // when
        var response = mdmShelfLifeService.updateShelfLifeInfo(request);

        // then
        Assertions.assertThat(response.getResultCount()).isEqualTo(1);
        Assertions.assertThat(response.getResult(0).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.VALIDATION_ERROR);
        Assertions.assertThat(response.getResult(0).getMessage())
            .contains("was not changed as shop sku belongs to category where such change is forbidden");

        Optional<CommonMsku> msku = mskuRepository.findMsku(MSKU_1);
        Assertions.assertThat(msku).isEmpty();

        Assertions.assertThat(getModelExpirDateParameterValue(MSKU_1)).isEmpty();
    }

    @Test
    public void testCannotSetShelfLifeRequiredFlagToFalseWhenCategoryIsFrozenForSuchChanges() {
        // given
        // 1. Создаем модель в mbo с флажком применимости СГ == true
        boolean expirDateRequiredFlag = true;
        createModelWithExpirDateParamAndSaveToMbo(MSKU_1, expirDateRequiredFlag);

        // 2. Задаем категорийную настройку
        CategoryParamValue cpv = new CategoryParamValue();
        cpv.setCategoryId(CATEGORY_ID_1)
            .setMdmParamId(KnownMdmParams.BAN_TO_REMOVE_EXPIR_DATE_FROM_WMS)
            .setBool(true);
        categoryParamValueRepository.insert(cpv);

        // 3. Формируем запрос, чтобы выставить флажок применимости в false,
        // но такое изменение под запретом из-за категорийной настройки.
        var request = MdmShelfLife.UpdateShelfLifeInfoRequest.newBuilder().addUpdateInfo(
            MdmShelfLife.ShelfLifeInfo.newBuilder()
                .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_1))
                .setShelfLifeRequired(false)
        ).build();

        // when
        var response = mdmShelfLifeService.updateShelfLifeInfo(request);

        // then
        Assertions.assertThat(response.getResultCount()).isEqualTo(1);
        Assertions.assertThat(response.getResult(0).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.VALIDATION_ERROR);
        Assertions.assertThat(response.getResult(0).getMessage())
            .contains("was not changed as shop sku belongs to category where such change is forbidden");

        Optional<CommonMsku> msku = mskuRepository.findMsku(MSKU_2);
        Assertions.assertThat(msku).isEmpty();

        Map<String, List<ModelStorage.ParameterValue>> modelParamValuesMap =
            getModelExpirDateParameterValue(MSKU_1).stream()
                .collect(Collectors.groupingBy(ModelStorage.ParameterValue::getXslName));

        Assertions.assertThat(modelParamValuesMap.size()).isEqualTo(1);
        Assertions.assertThat(modelParamValuesMap.get("expir_date").get(0).getBoolValue())
            .isEqualTo(expirDateRequiredFlag);
    }

    @Test
    public void testCannotSetShelfLifeRequiredFlagToTrueOnSomeModels() {
        // given
        // 1. Задаем категорийную настройку
        CategoryParamValue cpv = new CategoryParamValue();
        cpv.setCategoryId(CATEGORY_ID_2)
            .setMdmParamId(KnownMdmParams.BAN_TO_ADD_EXPIR_DATE_FROM_WMS)
            .setBool(true);
        categoryParamValueRepository.insert(cpv);

        // 2. Формируем запрос
        var request = MdmShelfLife.UpdateShelfLifeInfoRequest.newBuilder()
            .addUpdateInfo(
                MdmShelfLife.ShelfLifeInfo.newBuilder()
                    .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_1))
                    .setShelfLifeRequired(true)
            ).addUpdateInfo(
                MdmShelfLife.ShelfLifeInfo.newBuilder()
                    .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_2).setShopSku(SHOP_SKU_1))
                    .setShelfLifeRequired(true) // запрет на проставление true из-за кат. настроек
            ).build();

        // when
        var response = mdmShelfLifeService.updateShelfLifeInfo(request);

        // then
        Assertions.assertThat(response.getResultCount()).isEqualTo(2);
        Assertions.assertThat(response.getResult(0).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.OK);
        Assertions.assertThat(response.getResult(0).getMessage())
            .isEqualTo("ShelfLifeRequired updated successfully to true");

        Assertions.assertThat(response.getResult(1).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.VALIDATION_ERROR);
        Assertions.assertThat(response.getResult(1).getMessage())
            .contains("was not changed as shop sku belongs to category where such change is forbidden");

        // 1-я msku должна успешно обработаться и попасть в очередь
        Optional<CommonMsku> msku = mskuRepository.findMsku(MSKU_1);
        assertExpirDateParamValue(msku.orElse(null), MSKU_1, true);

        assertEnqueuedEntities(mskuToRefreshRepository.findAll(), Set.of(MSKU_1));
        assertEnqueuedEntities(sskuToRefreshRepository.findAll(), Set.of(BUSINESS_KEY_1));

        Assertions.assertThat(
            getModelExpirDateParameterValue(MSKU_1)
                .stream()
                .map(ModelStorage.ParameterValue::getBoolValue)
                .findFirst().get()
        ).isTrue();

        // другая msku не должна измениться
        msku = mskuRepository.findMsku(MSKU_3);
        Assertions.assertThat(msku).isEmpty();

        Assertions.assertThat(getModelExpirDateParameterValue(MSKU_3)).isEmpty();
    }

    @Test
    public void testCanUpdateShelfLifeRequiredAndShelfLifeOnMultipleItems() {
        var request = MdmShelfLife.UpdateShelfLifeInfoRequest.newBuilder().addUpdateInfo(
            MdmShelfLife.ShelfLifeInfo.newBuilder()
                .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_1))
                .setShelfLifeRequired(true)
        ).addUpdateInfo(
            MdmShelfLife.ShelfLifeInfo.newBuilder()
                .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_2))
                .setShelfLifeRequired(true)
                .setShelfLife(MdmCommon.TimeInUnits.newBuilder().setValue(10).setUnit(MdmCommon.TimeUnit.MONTH))
        ).addUpdateInfo(
            MdmShelfLife.ShelfLifeInfo.newBuilder()
                .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_3))
                .setShelfLifeRequired(false)
        ).addUpdateInfo(
            MdmShelfLife.ShelfLifeInfo.newBuilder()
                .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(MISSING_SHOP_SKU))
                .setShelfLifeRequired(true)
                .setShelfLife(MdmCommon.TimeInUnits.newBuilder().setValue(1).setUnit(MdmCommon.TimeUnit.MONTH))
        ).build();

        var response = mdmShelfLifeService.updateShelfLifeInfo(request);

        Assertions.assertThat(response.getResultCount()).isEqualTo(4);
        Assertions.assertThat(response.getResult(0).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.OK);
        Assertions.assertThat(response.getResult(0).getMessage())
            .isEqualTo("ShelfLifeRequired updated successfully to true");

        Assertions.assertThat(response.getResult(1).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.OK);
        Assertions.assertThat(response.getResult(1).getMessage())
            .isEqualTo("ShelfLifeRequired updated successfully to true, shelf life is saved as 10 MONTH");

        Assertions.assertThat(response.getResult(2).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.OK);
        Assertions.assertThat(response.getResult(2).getMessage())
            .isEqualTo("ShelfLifeRequired updated successfully to false");

        Assertions.assertThat(response.getResult(3).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.VALIDATION_ERROR);
        Assertions.assertThat(response.getResult(3).getMessage())
            .isEqualTo("Mapping not found for shop sku key [shop_id: 23; shop_sku: missing]");

        Map<Long, CommonMsku> mskus = mskuRepository.findMskus(List.of(MSKU_1, MSKU_2));

        Assertions.assertThat(mskus).hasSize(2);
        assertExpirDateParamValue(mskus.get(MSKU_1), MSKU_1, true);
        assertExpirDateParamValue(mskus.get(MSKU_2), MSKU_2, false);

        Map<ShopSkuKey, List<SskuSilverParamValue>> sskuParamValues = silverSskuRepository.findParametrizedSskus(
            List.of(BUSINESS_KEY_1, BUSINESS_KEY_2, BUSINESS_KEY_3));
        Assertions.assertThat(sskuParamValues).hasSize(1); // only values for BUSINESS_KEY_2
        assertShelfLifeAndUnitParamValues(sskuParamValues.get(BUSINESS_KEY_2), BUSINESS_KEY_2,
            10, TimeInUnits.TimeUnit.MONTH);

        assertEnqueuedEntities(mskuToRefreshRepository.findAll(), Set.of(MSKU_1, MSKU_2));
        assertEnqueuedEntities(sskuToRefreshRepository.findAll(), Set.of(BUSINESS_KEY_1, BUSINESS_KEY_2,
            BUSINESS_KEY_3));

        assertEnqueuedRlsRecalculation(List.of(BUSINESS_KEY_1, BUSINESS_KEY_2, BUSINESS_KEY_3));
    }

    @Test
    public void testContradictoryValuesOnDifferentSskusOfTheSameMsku() {
        var request = MdmShelfLife.UpdateShelfLifeInfoRequest.newBuilder().addUpdateInfo(
            MdmShelfLife.ShelfLifeInfo.newBuilder()
                .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_1))
                .setShelfLifeRequired(true)
        ).addUpdateInfo(
            MdmShelfLife.ShelfLifeInfo.newBuilder()
                .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_2))
                .setShelfLifeRequired(false)
                .setShelfLife(MdmCommon.TimeInUnits.newBuilder().setValue(10).setUnit(MdmCommon.TimeUnit.MONTH))
        ).build();

        var response = mdmShelfLifeService.updateShelfLifeInfo(request);

        Assertions.assertThat(response.getResultCount()).isEqualTo(2);
        Assertions.assertThat(response.getResult(0).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.VALIDATION_ERROR);
        Assertions.assertThat(response.getResult(0).getMessage())
            .isEqualTo("Value of shelfLifeRequired = true for shop sku key [shop_id: 23; shop_sku: tqjwke] " +
                "contradicts with other values of the same msku 1234");

        Assertions.assertThat(response.getResult(1).getStatus())
            .isEqualTo(MdmShelfLife.ShelfLifeUpdateResult.Status.VALIDATION_ERROR);
        Assertions.assertThat(response.getResult(1).getMessage())
            .isEqualTo("Value of shelfLifeRequired = false for shop sku key [shop_id: 23; shop_sku: tqjwke1] " +
                "contradicts with other values of the same msku 1234");

        Assertions.assertThat(mskuRepository.findAllMskus()).isEmpty();
    }

    @Test
    public void allowToUpdateShelfLifeRequiredFlagShouldReturnTruesIfNoCategoryRestrictionsExistForGivenShopSkuKey() {
        // given
        var request = MdmShelfLife.AllowToUpdateShelfLifeRequiredRequest.newBuilder()
            .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_1).setShopSku(SHOP_SKU_1))
            .build();

        // when
        var response = mdmShelfLifeService.allowToUpdateShelfLifeRequiredFlag(request);

        // then
        Assertions.assertThat(response.getAllowToSetTrue()).isTrue();
        Assertions.assertThat(response.getAllowToSetFalse()).isTrue();
    }

    @Test
    public void allowToUpdateShelfLifeRequiredFlagShouldReturnFalseWhenBanToAddExpirDateExists() {
        // given
        // 1. Задаем категорийную настройку
        CategoryParamValue cpv = new CategoryParamValue();
        cpv.setCategoryId(CATEGORY_ID_2)
            .setMdmParamId(KnownMdmParams.BAN_TO_ADD_EXPIR_DATE_FROM_WMS) // запрет на выставление флажка в 'true'
            .setBool(true);
        categoryParamValueRepository.insert(cpv);

        // 2. Формируем запрос
        var request = MdmShelfLife.AllowToUpdateShelfLifeRequiredRequest.newBuilder()
            .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_2).setShopSku(SHOP_SKU_1))
            .build();

        // when
        var response = mdmShelfLifeService.allowToUpdateShelfLifeRequiredFlag(request);

        // then
        Assertions.assertThat(response.getAllowToSetTrue()).isFalse();
        Assertions.assertThat(response.getAllowToSetFalse()).isTrue();
    }

    @Test
    public void allowToUpdateShelfLifeRequiredFlagShouldReturnTrueWhenBanToRemoveExpirDateExists() {
        // given
        // 1. Задаем категорийную настройку
        CategoryParamValue cpv = new CategoryParamValue();
        cpv.setCategoryId(CATEGORY_ID_2)
            .setMdmParamId(KnownMdmParams.BAN_TO_REMOVE_EXPIR_DATE_FROM_WMS) // запрет на выставление флажка в 'false'
            .setBool(true);
        categoryParamValueRepository.insert(cpv);

        // 2. Формируем запрос
        var request = MdmShelfLife.AllowToUpdateShelfLifeRequiredRequest.newBuilder()
            .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(SUPPLIER_2).setShopSku(SHOP_SKU_1))
            .build();

        // when
        var response = mdmShelfLifeService.allowToUpdateShelfLifeRequiredFlag(request);

        // then
        Assertions.assertThat(response.getAllowToSetTrue()).isTrue();
        Assertions.assertThat(response.getAllowToSetFalse()).isFalse();
    }

    @Test
    public void allowToUpdateShelfLifeRequiredFlagWhenNoMappingInMdm() {
        // given
        int nonExistentSupplier = 100500;
        String nonExistentShopSku = "ghost";
        var request = MdmShelfLife.AllowToUpdateShelfLifeRequiredRequest.newBuilder()
            .setShopSkuKey(MdmCommon.ShopSkuKey.newBuilder().setSupplierId(nonExistentSupplier).setShopSku(nonExistentShopSku))
            .build();

        // when
        var response = mdmShelfLifeService.allowToUpdateShelfLifeRequiredFlag(request);

        // then
        Assertions.assertThat(response.getAllowToSetTrue()).isTrue();
        Assertions.assertThat(response.getAllowToSetFalse()).isTrue();
    }

    private static void assertExpirDateParamValue(CommonMsku commonMsku, long mskuId, boolean expirDateValue) {
        Assertions.assertThat(commonMsku).isNotNull();
        Assertions.assertThat(commonMsku.getMskuId()).isEqualTo(mskuId);
        MskuParamValue paramValue = commonMsku.getParamValue(KnownMdmParams.EXPIR_DATE).orElse(null);
        Assertions.assertThat(paramValue).isNotNull();
        Assertions.assertThat(paramValue.getMskuId()).isEqualTo(mskuId);
        Assertions.assertThat(paramValue.getMdmParamId()).isEqualTo(KnownMdmParams.EXPIR_DATE);
        Assertions.assertThat(paramValue.getBool()).isEqualTo(Optional.of(expirDateValue));
        Assertions.assertThat(paramValue.getMasterDataSourceType()).isEqualTo(MasterDataSourceType.MEASUREMENT);
        Assertions.assertThat(paramValue.getMasterDataSourceId()).isEqualTo(MasterDataSourceType.WMS_DIRECT_SOURCE_ID);
    }

    private void assertShelfLifeAndUnitParamValues(Collection<SskuSilverParamValue> paramValues,
                                                   ShopSkuKey shopSkuKey,
                                                   long value,
                                                   TimeInUnits.TimeUnit unit) {
        Optional<SskuSilverParamValue> shelfLifeParamValue = paramValues.stream()
            .filter(pv -> pv.getMdmParamId() == KnownMdmParams.SHELF_LIFE)
            .findAny();

        Assertions.assertThat(shelfLifeParamValue).isPresent();
        Assertions.assertThat(shelfLifeParamValue.get().getShopSkuKey()).isEqualTo(shopSkuKey);
        Assertions.assertThat(shelfLifeParamValue.get().getNumeric()).isEqualTo(Optional.of(new BigDecimal(value)));
        Assertions.assertThat(shelfLifeParamValue.get().getMasterDataSourceType())
            .isEqualTo(MasterDataSourceType.MDM_DEFAULT);
        Assertions.assertThat(shelfLifeParamValue.get().getMasterDataSourceId())
            .isEqualTo(MasterDataSourceType.WMS_DIRECT_SOURCE_ID);

        Optional<SskuSilverParamValue> shelfLifeUnitParamValue = paramValues.stream()
            .filter(pv -> pv.getMdmParamId() == KnownMdmParams.SHELF_LIFE_UNIT)
            .findAny();

        Assertions.assertThat(shelfLifeUnitParamValue).isPresent();

        MdmParam mdmParam = mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT);
        long optionId = KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(unit);

        Assertions.assertThat(shelfLifeUnitParamValue.get().getShopSkuKey()).isEqualTo(shopSkuKey);
        Assertions.assertThat(shelfLifeUnitParamValue.get().getOption())
            .isEqualTo(Optional.of(mdmParam.getExternals().getOption(optionId)));
        Assertions.assertThat(shelfLifeUnitParamValue.get().getMasterDataSourceType())
            .isEqualTo(MasterDataSourceType.MDM_DEFAULT);
        Assertions.assertThat(shelfLifeUnitParamValue.get().getMasterDataSourceId())
            .isEqualTo(MasterDataSourceType.WMS_DIRECT_SOURCE_ID);
    }

    private static <T> void assertEnqueuedEntities(List<? extends MdmQueueInfoBase<T>> mskuToRefreshInfos,
                                                   Set<T> mskuIds) {
        Assertions.assertThat(mskuToRefreshInfos).hasSize(mskuIds.size());
        Assertions.assertThat(mskuToRefreshInfos.stream().map(MdmQueueInfoBase::getEntityKey))
            .containsExactlyInAnyOrderElementsOf(mskuIds);
        Assertions.assertThat(mskuToRefreshInfos.stream()
                .flatMap(info -> info.getOnlyReasons().stream()).collect(Collectors.toList()))
            .containsOnly(MdmEnqueueReason.WMS_DIRECT_UPDATE);
    }

    private void assertEnqueuedRlsRecalculation(Collection<ShopSkuKey> shopSkuKeys) {
        List<SskuToRefreshInfo> allSskuToRefreshInfo = sskuToRefreshRepository.findAll();
        Assertions.assertThat(allSskuToRefreshInfo)
            .isNotEmpty()
            .flatMap(SskuToRefreshInfo::getRefreshReasons)
            .map(MdmQueueInfoBase.TimeAndReason::getReason)
            .containsOnly(MdmEnqueueReason.WMS_DIRECT_UPDATE);
        Assertions.assertThat(allSskuToRefreshInfo)
            .map(SskuToRefreshInfo::getEntityKey)
            .containsExactlyInAnyOrderElementsOf(shopSkuKeys);
    }

    private ModelStorage.Model.Builder buildModel(long mskuId) {
        return ModelStorage.Model.newBuilder().setId(mskuId)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .setId(23124324L)
                .build());
    }

    private ModelStorage.Model createModelWithExpirDateParamAndSaveToMbo(long mskuId, boolean expirDateRequiredFlag) {
        List<ModelStorage.Model> model = List.of(buildModel(mskuId)
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                .setXslName("expir_date")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(expirDateRequiredFlag)
                .setModificationDate(TIMESTAMP))
            .build());
        mboModelsService.saveModels(model);
        return model.get(0);
    }

    private List<ModelStorage.ParameterValue> getModelExpirDateParameterValue(long mskuId) {
        return mboModelsService.loadModels(List.of(mskuId),
                Set.of("expir_date"),
                Set.of(KnownMdmMboParams.EXPIR_DATE_PARAM_ID),
                false)
            .get(mskuId)
            .getParameterValues();
    }
}
