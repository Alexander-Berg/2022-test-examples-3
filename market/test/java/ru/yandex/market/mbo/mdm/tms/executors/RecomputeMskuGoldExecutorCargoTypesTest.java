package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.CargoType;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Assertions.assertThat;

public class RecomputeMskuGoldExecutorCargoTypesTest extends RecomputeMskuGoldExecutorBaseTest {
    private static final long SEED = 9284643L;
    private static final long CATEGORY_ID = 1L;
    private static final long MSKU_ID = 100L;
    private static final ModelKey MODEL_KEY = new ModelKey(CATEGORY_ID, MSKU_ID);
    private static final ShopSkuKey SHOP_SKU_KEY = new ShopSkuKey(123, "213");
    private static final long CLOTHES_CARGOTYPE_ID = 507L;
    private static final long FRESH_CARGOTYPE_ID = 252L;
    private static final long EXTRA_FRESH_CARGOTYPE_ID = 251L;
    private EnhancedRandom random = TestDataUtils.defaultRandom(SEED);

    private MdmParam expirDateParam;
    private MdmParam clothesCargoTypeParam;
    private MdmParam freshCargoTypeParam;
    private MdmParam extraFreshCargoTypeParam;

    @Before
    public void setUp() throws Exception {
        expirDateParam = mdmParamCache.get(KnownMdmParams.EXPIR_DATE);
        clothesCargoTypeParam = mdmParamCache.get(CLOTHES_CARGOTYPE_ID);
        freshCargoTypeParam = mdmParamCache.get(FRESH_CARGOTYPE_ID);
        extraFreshCargoTypeParam = mdmParamCache.get(EXTRA_FRESH_CARGOTYPE_ID);
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setModelKey(MODEL_KEY)
            .setShopSkuKey(SHOP_SKU_KEY)
            .setMappingKind(MappingCacheDao.MappingKind.APPROVED)
        );
    }

    @Test
    public void whenCargotypeOnMboCategoryExistsItShouldBeAddedOnMsku() {
        processCalculation(
            List.of(clothesCargoTypeParam, freshCargoTypeParam, extraFreshCargoTypeParam),
            List.of(
                generateMboCategoryCargoType(clothesCargoTypeParam, true),
                generateMboCategoryCargoType(freshCargoTypeParam, true)
            ),
            List.of(),
            new CommonMsku(MODEL_KEY, List.of(
                generateMdmMskuBoolParamValue(expirDateParam, true, MasterDataSourceType.AUTO),
                // этот карготип должен будет заfalsеиться, потому что мы его не добавили в категорийные параметры МБО.
                generateMdmMskuBoolParamValue(extraFreshCargoTypeParam, true, MasterDataSourceType.AUTO)
            )),
            new CommonMsku(MODEL_KEY, List.of(
                generateMdmMskuBoolParamValue(expirDateParam, true, MasterDataSourceType.AUTO),
                generateMdmMskuBoolParamValue(freshCargoTypeParam, true, MasterDataSourceType.AUTO),
                generateMdmMskuBoolParamValue(clothesCargoTypeParam, true, MasterDataSourceType.AUTO),
                generateMdmMskuBoolParamValue(extraFreshCargoTypeParam, false, MasterDataSourceType.AUTO)
            ))
        );
    }

    @Test
    public void whenExistMskuMasterDataWithFilledCargoTypesTheyShouldBeReplaced() {
        processCalculation(
            List.of(clothesCargoTypeParam),
            List.of(generateMboCategoryCargoType(clothesCargoTypeParam, true)),
            List.of(),
            null,
            new CommonMsku(MODEL_KEY, List.of(
                generateMdmMskuBoolParamValue(clothesCargoTypeParam, true, MasterDataSourceType.AUTO)))
        );
    }

    @Test
    public void whenExistMskuMasterDataWithFilledCargoTypesMboCargoTypesShouldBeReplacedByMdm() {
        processCalculation(
            List.of(clothesCargoTypeParam),
            List.of(generateMboCategoryCargoType(clothesCargoTypeParam, true)),
            List.of(generateMdmCategoryCargoType(clothesCargoTypeParam, false)),
            null,
            new CommonMsku(MODEL_KEY, List.of(
                generateMdmMskuBoolParamValue(clothesCargoTypeParam, false, MasterDataSourceType.AUTO)))
        );
    }

    @Test
    public void whenHaveCargotypeByMboOperatorNotRewriteItWithComputed() {
        processCalculation(
            List.of(clothesCargoTypeParam),
            List.of(),
            List.of(generateMdmCategoryCargoType(clothesCargoTypeParam, false)),
            new CommonMsku(MODEL_KEY, List.of(
                generateMdmMskuBoolParamValue(clothesCargoTypeParam, true, MasterDataSourceType.MBO_OPERATOR))),
            new CommonMsku(MODEL_KEY, List.of(
                generateMdmMskuBoolParamValue(clothesCargoTypeParam, true, MasterDataSourceType.MBO_OPERATOR)))
        );
    }

    private void processCalculation(
        List<MdmParam> cargoTypesTable,
        List<MboParameters.ParameterValue> mboCategorySettings,
        List<CategoryParamValue> mdmCategorySettings,
        CommonMsku existing,
        CommonMsku expected
    ) {
        // Сперва придётся проделать много подготовительной работы.
        // 1. Добавим карготипы в табличку карготипов
        cargoTypesTable.forEach(this::insertCargoTypeFor);

        // 2. Добавим категорийные настройки МБО
        parameterValueCachingServiceMock.addCategoryParameterValues(CATEGORY_ID, mboCategorySettings);

        // 3. Добавим категорийные настройки МДМ
        categoryParamValueRepository.insertOrUpdateAll(mdmCategorySettings);

        // 4. Добавим существующую msku
        if (existing != null) {
            mskuRepository.insertOrUpdateMsku(existing);
        }

        // 5. Положим MSKU в очередь
        mskuQueue.enqueue(MSKU_ID, MdmEnqueueReason.DEFAULT);

        // 6. Запускаем
        executor.execute();

        // 7. Проверяем msku параметры
        assertThat(mskuQueue.getUnprocessedBatch(1)).isEmpty();
        assertThat(mskuRepository.findMsku(MSKU_ID))
            .map(TestBmdmUtils::removeBmdmIdAndVersion) // удалим служебные storage-api параметры
            .map(TestMdmParamUtils::filterCisCargoTypes) // удалим дефолтные false ЧЗ и Mercury
            .hasValueSatisfying(msku -> Assertions.assertThat(msku.valueAndSourceEquals(expected))
                .withFailMessage("Expected %s, but actual %s", expected, msku)
                .isTrue());

        // 8. Проверяем очередь отправки в мбо
        List<MdmMskuQueueInfo> allIMskuToMboInfos = mskuToMboQueue.findAll();
        assertThat(allIMskuToMboInfos).hasSize(1);
        assertThat(allIMskuToMboInfos)
            .filteredOn(info -> !info.isProcessed())
            .map(MdmQueueInfoBase::getEntityKey)
            .allMatch(id -> id == MSKU_ID);
    }

    private void insertCargoTypeFor(MdmParam param) {
        int cargoTypeId = param.getExternals().getCargotypeId();
        CargoType cargoType = new CargoType(cargoTypeId,
            "cargoType" + cargoTypeId,
            param.getExternals().getMboParamId(),
            random.nextLong(),
            random.nextLong());
        cargoTypeRepository.insert(cargoType);
        mdmLmsCargoTypeCache.refresh();
    }

    private static MboParameters.ParameterValue generateMboCategoryCargoType(MdmParam param, boolean value) {
        return MboParameters.ParameterValue.newBuilder()
            .setParamId(param.getExternals().getMboParamId())
            .setXslName(param.getXslName())
            .setBoolValue(value)
            .setTypeId(ModelStorage.ParameterValueType.BOOLEAN_VALUE)
            .setValueType(MboParameters.ValueType.BOOLEAN)
            .setOptionId(param.getExternals().getBoolBindings().get(value).intValue())
            .build();
    }

    private static CategoryParamValue generateMdmCategoryCargoType(MdmParam mdmParam, boolean value) {
        return (CategoryParamValue) new CategoryParamValue()
            .setCategoryId(CATEGORY_ID)
            .setMdmParamId(mdmParam.getId())
            .setBool(value)
            .setXslName(mdmParam.getXslName());
    }

    private static MskuParamValue generateMdmMskuBoolParamValue(MdmParam mdmParam,
                                                                boolean value,
                                                                MasterDataSourceType sourceType) {
        return (MskuParamValue) new MskuParamValue()
            .setMskuId(MSKU_ID)
            .setMdmParamId(mdmParam.getId())
            .setBool(value)
            .setXslName(mdmParam.getXslName())
            .setMasterDataSourceType(sourceType);
    }
}
