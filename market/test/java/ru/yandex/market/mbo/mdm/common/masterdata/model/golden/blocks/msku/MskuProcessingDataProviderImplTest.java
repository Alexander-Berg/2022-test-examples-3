package ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MappingBatch;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.GlobalParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.GlobalParamValueService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.WarehouseProjectionCacheImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingDataProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfoRepository;
import ru.yandex.market.mbo.mdm.common.service.MdmParameterValueCachingServiceMock;
import ru.yandex.market.mbo.mdm.common.service.mapping.MdmBestMappingsProvider;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsService;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsServiceImpl;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class MskuProcessingDataProviderImplTest extends MdmBaseDbTestClass {
    private static final long CATEGORY1 = 13567;
    private static final int BLUE_SUPPLIER_ID = 1;
    private static final int WHITE_SUPPLIER_ID = 2;
    private static final long MSKU1 = 123L;
    private static final long MSKU2 = 654L;
    private static final long MSKU3 = 159L;
    private static final ShopSkuKey SHOP_SKU_1 = new ShopSkuKey(BLUE_SUPPLIER_ID, "test");
    private static final ShopSkuKey SHOP_SKU_2 = new ShopSkuKey(BLUE_SUPPLIER_ID, "test2");
    private static final ShopSkuKey SHOP_SKU_3 = new ShopSkuKey(BLUE_SUPPLIER_ID, "test3");
    private static final ShopSkuKey SHOP_SKU_4 = new ShopSkuKey(BLUE_SUPPLIER_ID, "test4");
    private static final ShopSkuKey SHOP_SKU_5 = new ShopSkuKey(BLUE_SUPPLIER_ID, "test5");
    private static final ShopSkuKey SHOP_SKU_6 = new ShopSkuKey(BLUE_SUPPLIER_ID, "test6");
    private static final ShopSkuKey WHITE_SHOP_SKU_1 = new ShopSkuKey(WHITE_SUPPLIER_ID, "test_wt1");
    private static final ShopSkuKey WHITE_SHOP_SKU_2 = new ShopSkuKey(WHITE_SUPPLIER_ID, "test_wt2");
    private static final Instant TS_1 = Instant.ofEpochMilli(1000000);
    private static final Instant TS_2 = Instant.ofEpochMilli(2000000);
    private static final Instant TS_3 = Instant.ofEpochMilli(3000000);

    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private GlobalParamValueService globalParamValueService;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private MdmSupplierRepository supplierRepository;
    @Autowired
    private PriceInfoRepository priceInfoRepository;
    @Autowired
    private CargoTypeRepository cargoTypeRepository;
    @Autowired
    private MdmBestMappingsProvider mdmBestMappingsProvider;

    private MdmParameterValueCachingServiceMock mdmParameterValueCachingServiceMock;
    private MskuProcessingDataProviderImpl assistant;

    @Before
    public void setup() {
        mdmParameterValueCachingServiceMock = new MdmParameterValueCachingServiceMock();
        MdmCategorySettingsService categorySettingsService = new MdmCategorySettingsServiceImpl(
            mdmParameterValueCachingServiceMock,
            cargoTypeRepository,
            categoryParamValueRepository
        );
        assistant = new MskuProcessingDataProviderImpl(
            mskuRepository,
            categoryParamValueRepository,
            categorySettingsService,
            masterDataRepository,
            globalParamValueService,
            goldSskuRepository,
            storageKeyValueService,
            priceInfoRepository,
            Mockito.mock(WarehouseProjectionCacheImpl.class),
            mdmParamCache,
            mdmBestMappingsProvider
        );
        supplierRepository.insertBatch(
            new MdmSupplier().setId(BLUE_SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY),
            new MdmSupplier().setId(WHITE_SUPPLIER_ID).setType(MdmSupplierType.MARKET_SHOP));
        cargoTypeRepository.insertBatch(
            mdmParamCache.get(KnownMdmParams.DANGEROUS_AIR_410).getExternals().getCargoType().orElseThrow(),
            mdmParamCache.get(KnownMdmParams.AGROCHEMICALS_130).getExternals().getCargoType().orElseThrow(),
            mdmParamCache.get(KnownMdmParams.PESTICIDES_140).getExternals().getCargoType().orElseThrow()
        );
    }

    @Test
    public void whenThereAreSeveralMappingsShouldChooseWithLatestAccordingToMappingCacheDaoComparator() {
        MappingCacheDao mapping1 = new MappingCacheDao().setShopSkuKey(SHOP_SKU_1)
            .setCategoryId(1).setMskuId(MSKU1).setUpdateStamp(123L);
        MappingCacheDao mapping2 = new MappingCacheDao().setShopSkuKey(SHOP_SKU_2)
            .setCategoryId(2).setMskuId(MSKU1).setUpdateStamp(321L);
        MappingCacheDao mapping3 = new MappingCacheDao().setShopSkuKey(SHOP_SKU_3)
            .setCategoryId(4).setMskuId(MSKU2).setVersionTimestamp(TS_1);
        MappingCacheDao mapping4 = new MappingCacheDao().setShopSkuKey(SHOP_SKU_4)
            .setCategoryId(5).setMskuId(MSKU2).setVersionTimestamp(TS_2);
        MappingCacheDao mapping5 = new MappingCacheDao().setShopSkuKey(SHOP_SKU_5)
            .setCategoryId(6).setMskuId(MSKU3).setVersionTimestamp(TS_3)
            .setModifiedTimestamp(LocalDateTime.now().minusDays(10));
        MappingCacheDao mapping6 = new MappingCacheDao().setShopSkuKey(SHOP_SKU_6)
            .setCategoryId(7).setMskuId(MSKU3).setUpdateStamp(4373L).setModifiedTimestamp(LocalDateTime.now());

        mappingsCacheRepository.insertOrUpdateAll(List.of(mapping1, mapping2, mapping3,
            mapping4, mapping5, mapping6));
        MappingBatch batch = assistant.loadMskuMappings(List.of(MSKU1, MSKU2, MSKU3));

        Assertions.assertThat(batch.asRawIdMap()).hasSize(3);
        Assertions.assertThat(batch.asRawIdMap()).containsOnlyKeys(MSKU1, MSKU2, MSKU3);
        Assertions.assertThat(batch.getCategoryIds()).containsExactlyInAnyOrder(2L, 5L, 6L);
        Assertions.assertThat(batch.getSskuIds()).containsExactlyInAnyOrder(
            SHOP_SKU_1, SHOP_SKU_2, SHOP_SKU_3, SHOP_SKU_4, SHOP_SKU_5, SHOP_SKU_6);
    }

    @Test
    public void whenThereAreWhiteOrUnknownMappingsShouldUseApprovedForBestSelection() {
        var now = Instant.now();
        var future = now.plusSeconds(100500L);
        var farFuture = future.plusSeconds(100500L);
        MappingCacheDao badApprovedMappingWithBestVersion = new MappingCacheDao()
            .setShopSkuKey(SHOP_SKU_1)
            .setCategoryId(0)
            .setVersionTimestamp(farFuture)
            .setModifiedTimestamp(TimestampUtil.toLocalDateTime(farFuture))
            .setUpdateStamp(farFuture.toEpochMilli())
            .setEoxTimestamp(farFuture)
            .setMbocTimestamp(farFuture)
            .setMskuId(MSKU1);
        MappingCacheDao moderatelyGoodApprovedMapping = new MappingCacheDao()
            .setShopSkuKey(SHOP_SKU_2)
            .setCategoryId(17)
            .setMskuId(MSKU1);
        MappingCacheDao beautifulWhiteMappingWithLargeVersions = new MappingCacheDao()
            .setShopSkuKey(WHITE_SHOP_SKU_1)
            .setCategoryId(18)
            .setMappingKind(MappingCacheDao.MappingKind.SUGGESTED)
            .setVersionTimestamp(future)
            .setModifiedTimestamp(TimestampUtil.toLocalDateTime(future))
            .setUpdateStamp(future.toEpochMilli())
            .setEoxTimestamp(future)
            .setMbocTimestamp(future)
            .setMskuId(MSKU1);

        mappingsCacheRepository.insertOrUpdateAll(List.of(
            badApprovedMappingWithBestVersion, moderatelyGoodApprovedMapping, beautifulWhiteMappingWithLargeVersions
        ));
        MappingBatch batch = assistant.loadMskuMappings(List.of(MSKU1));

        Assertions.assertThat(batch.asRawIdMap()).hasSize(1); // маппинг без категории выбросится,
        Assertions.assertThat(batch.asRawIdMap()).containsOnlyKeys(MSKU1); // а садджест проигнорится, т.к. есть аппрув
        Assertions.assertThat(batch.getCategoryIds()) // категория возьмётся с самого качественного
            .containsExactlyInAnyOrder((long) moderatelyGoodApprovedMapping.getCategoryId());
        // а ключики со всех аппрувов
        Assertions.assertThat(batch.getSskuIds()).containsExactlyInAnyOrder(
            badApprovedMappingWithBestVersion.getShopSkuKey(),
            moderatelyGoodApprovedMapping.getShopSkuKey());
    }

    @Test
    public void whenThereAreWhiteOrUnknownMappingsShouldUseThemIfNoApprovedFound() {
        var now = Instant.now();
        var future = now.plusSeconds(100500L);
        var farFuture = future.plusSeconds(100500L);
        MappingCacheDao goodWhiteMappingWithSmallVersions = new MappingCacheDao()
            .setShopSkuKey(WHITE_SHOP_SKU_1)
            .setCategoryId(17)
            .setMappingKind(MappingCacheDao.MappingKind.SUGGESTED)
            .setVersionTimestamp(future)
            .setModifiedTimestamp(TimestampUtil.toLocalDateTime(future))
            .setUpdateStamp(future.toEpochMilli())
            .setEoxTimestamp(future)
            .setMbocTimestamp(future)
            .setMskuId(MSKU1);
        MappingCacheDao beautifulWhiteMappingWithLargeVersions = new MappingCacheDao()
            .setShopSkuKey(WHITE_SHOP_SKU_2)
            .setCategoryId(18)
            .setMappingKind(MappingCacheDao.MappingKind.SUGGESTED)
            .setVersionTimestamp(farFuture)
            .setModifiedTimestamp(TimestampUtil.toLocalDateTime(farFuture))
            .setUpdateStamp(farFuture.toEpochMilli())
            .setEoxTimestamp(farFuture)
            .setMbocTimestamp(farFuture)
            .setMskuId(MSKU1);

        mappingsCacheRepository.insertOrUpdateAll(List.of(
            goodWhiteMappingWithSmallVersions, beautifulWhiteMappingWithLargeVersions
        ));
        MappingBatch batch = assistant.loadMskuMappings(List.of(MSKU1));

        Assertions.assertThat(batch.asRawIdMap()).hasSize(1);
        Assertions.assertThat(batch.asRawIdMap()).containsOnlyKeys(MSKU1);
        Assertions.assertThat(batch.getCategoryIds())
            .containsExactlyInAnyOrder((long) beautifulWhiteMappingWithLargeVersions.getCategoryId());
        Assertions.assertThat(batch.getSskuIds()).containsExactlyInAnyOrder(
            goodWhiteMappingWithSmallVersions.getShopSkuKey(),
            beautifulWhiteMappingWithLargeVersions.getShopSkuKey()
        );
    }

    @Test
    public void testContextsCreation() {
        ModelKey msku1 = new ModelKey(CATEGORY1, 1);
        // ssku для этого теста не нужны
        MappingBatch mappings = new MappingBatch(Map.of(msku1, List.of()));
        // MDM category values
        List<CategoryParamValue> categoryParamValues = List.of(
            generateMdmCategoryValue(CATEGORY1, "3303", KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX),
            generateMdmCategoryValue(CATEGORY1, 12, KnownMdmParams.MIN_LIMIT_GUARANTEE_PERIOD),
            generateMdmCategoryValue(CATEGORY1, true, KnownMdmParams.DANGEROUS_AIR_410),
            generateMdmCategoryValue(CATEGORY1, false, KnownMdmParams.PESTICIDES_140)
        );
        categoryParamValueRepository.insertBatch(categoryParamValues);
        //Global values
        storageKeyValueService.putValue(MdmProperties.IMEI_MASK, "WOW!");
        // MBO category values
        mdmParameterValueCachingServiceMock.addCategoryParameterValues(CATEGORY1,
            generateMboParameterValue(KnownMdmMboParams.HEAVY_GOOD_OVERRIDE_PARAM_ID, null,
                KnownMdmMboParams.HEAVY_GOOD_OVERRIDE_TO_TRUE_OPTION_ID),
            generateMboParameterValue(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID, false, null),
            generateMboParameterValue(mbo(KnownMdmParams.AGROCHEMICALS_130), true, null),
            generateMboParameterValue(mbo(KnownMdmParams.DANGEROUS_AIR_410), false, null), // игнор., т.к. задано в МДМ
            generateMboParameterValue(mbo(KnownMdmParams.PESTICIDES_140), true, null), // игнор., т.к. задано в МДМ
            generateMboParameterValue(KnownMdmMboParams.GUARANTEE_PERIOD_USE_PARAM_ID, null,
                KnownMdmMboParams.GUARANTEE_PERIOD_REQUIRED_OPTION_ID)
        );

        GlobalParamValue expectedGlobalParamValue = (GlobalParamValue) new GlobalParamValue()
            .setMdmParamId(KnownMdmParams.IMEI_MASK)
            .setXslName("mdm_imei_mask")
            .setString("WOW!")
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        Map<Long, GoldComputationContext> contexts = assistant.generateContexts(mappings, null);

        Assertions.assertThat(contexts).hasSize(1);
        GoldComputationContext context = contexts.get(CATEGORY1);

        Map<Long, MdmParamValue> globalParamValues = context.getGlobalParamValues();
        Assertions.assertThat(globalParamValues).hasSize(1);
        Assertions.assertThat(globalParamValues.get(KnownMdmParams.IMEI_MASK))
            .isEqualTo(expectedGlobalParamValue);

        Map<Long, Boolean> mboCategoryOverrides = context.getMboCategoryOverrides();
        Assertions.assertThat(mboCategoryOverrides).hasSize(1);
        Assertions.assertThat(mboCategoryOverrides.get(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID)).isTrue();

        Map<Long, Boolean> mboCategorySettings = context.getMboCategorySettings();
        Assertions.assertThat(mboCategorySettings).hasSize(3);
        Assertions.assertThat(mboCategorySettings.get(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID)).isFalse();
        Assertions.assertThat(mboCategorySettings.get(KnownMdmMboParams.LIFE_TIME_USE_PARAM_ID)).isFalse(); //by default
        Assertions.assertThat(mboCategorySettings.get(KnownMdmMboParams.GUARANTEE_PERIOD_USE_PARAM_ID)).isTrue();

        Map<Long, CategoryParamValue> mdmCategoryParamValues = context.getCategoryParamValues();
        Assertions.assertThat(mdmCategoryParamValues).isEqualTo(
            categoryParamValues.stream()
                .collect(Collectors.toMap(CategoryParamValue::getMdmParamId, Function.identity()))
        );

        Assertions.assertThat(context.getCategoryCargoTypes()).containsExactlyInAnyOrder(
            mdmParamCache.get(KnownMdmParams.DANGEROUS_AIR_410).getExternals().getCargoType().orElseThrow(),
            mdmParamCache.get(KnownMdmParams.AGROCHEMICALS_130).getExternals().getCargoType().orElseThrow()
        );
    }

    private CategoryParamValue generateMdmCategoryValue(long categoryId, Boolean value, long paramId) {
        MdmParam param = mdmParamCache.get(paramId);
        return (CategoryParamValue) new CategoryParamValue().setCategoryId(categoryId)
            .setBool(value)
            .setMdmParamId(paramId)
            .setXslName(param.getXslName());
    }

    private CategoryParamValue generateMdmCategoryValue(long categoryId, String value, long paramId) {
        MdmParam param = mdmParamCache.get(paramId);
        return (CategoryParamValue) new CategoryParamValue().setCategoryId(categoryId)
            .setString(value)
            .setMdmParamId(paramId)
            .setXslName(param.getXslName());
    }

    private CategoryParamValue generateMdmCategoryValue(long categoryId, int value, long paramId) {
        MdmParam param = mdmParamCache.get(paramId);
        return (CategoryParamValue) new CategoryParamValue().setCategoryId(categoryId)
            .setNumeric(BigDecimal.valueOf(value))
            .setMdmParamId(paramId)
            .setXslName(param.getXslName());
    }

    private MboParameters.ParameterValue generateMboParameterValue(long mboParamId,
                                                                   Boolean boolValue,
                                                                   Integer optionId) {
        var builder = MboParameters.ParameterValue.newBuilder()
            .setParamId(mboParamId);
        if (boolValue != null) {
            builder.setBoolValue(boolValue);
        }
        if (optionId != null) {
            builder.setOptionId(optionId);
        }
        return builder.build();
    }

    private long mbo(long mdmId) {
        return mdmParamCache.get(mdmId).getExternals().getMboParamId();
    }

}
