package ru.yandex.market.mboc.common.masterdata.services.united;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Assertions.assertThat;

public class MskuUnitedProcessingExpirationDatesApplyTest extends UnitedProcessingServiceSetupBaseTest {

    @Test
    public void whenExpirationDatesApplyShouldPropagateToMskuLevel() {
        proceedGoldRecalculation(
            // category
            KnownMdmParams.EXPIRATION_DATES_REQUIRED_OPTION,
            MasterDataSourceType.MDM_ADMIN,
            // existing gold
            false,
            MasterDataSourceType.AUTO,
            "other_auto",
            // expected after recalculation
            true,
            MasterDataSourceType.AUTO,
            MasterDataSourceType.EXPIR_DATE_FROM_CATEGORY_SETTINGS_SOURCE_ID
        );
    }

    @Test
    public void whenMskuLevelWithManualChangesShouldPrioritizeCategoryLevel() {
        proceedGoldRecalculation(
            // category
            KnownMdmParams.EXPIRATION_DATES_MAY_USE_OPTION,
            MasterDataSourceType.MDM_ADMIN,
            // existing gold
            true,
            MasterDataSourceType.MDM_OPERATOR,
            "manual",
            // expected after recalculation
            true,
            MasterDataSourceType.MDM_OPERATOR,
            "manual"
        );
    }

    @Test
    public void whenMskuLevelWithMeasurementChangesShouldPrioritizeMskuLevel() {
        proceedGoldRecalculation(
            // category
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            MasterDataSourceType.AUTO,
            // existing gold
            true,
            MasterDataSourceType.MEASUREMENT,
            MasterDataSourceType.WMS_DIRECT_SOURCE_ID,
            // expected after recalculation
            true,
            MasterDataSourceType.MEASUREMENT,
            MasterDataSourceType.WMS_DIRECT_SOURCE_ID
        );
    }

    @Test
    public void whenMskuLevelWithAutoChangesShouldReplacedByCategoryLevel() {
        proceedGoldRecalculation(
            // category
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            MasterDataSourceType.AUTO,
            // existing gold
            true,
            MasterDataSourceType.AUTO,
            "other_auto_changes",
            // expected after recalculation
            false,
            MasterDataSourceType.AUTO,
            MasterDataSourceType.EXPIR_DATE_FROM_CATEGORY_SETTINGS_SOURCE_ID
        );
    }

    @Test
    public void whenMskuLevelShouldStayTheSameForMayUseCategoryOption() {
        proceedGoldRecalculation(
            // category
            KnownMdmParams.EXPIRATION_DATES_MAY_USE_OPTION,
            MasterDataSourceType.AUTO,
            // existing gold
            true,
            MasterDataSourceType.MDM_OPERATOR,
            "manual",
            // expected after recalculation
            true,
            MasterDataSourceType.MDM_OPERATOR,
            "manual"
        );
    }

    @Test
    public void whenHaveEnoughShopSskusWithSameShelfLifeShouldForceTrue() {
        ModelKey key = new ModelKey(1L, 100L);
        List<MasterData> shopSkus = new ArrayList<>();
        for (int i = 0; i < MskuSilverItemPreProcessor.SUPPLIERS_FOR_EXPIR_DATE_BLOCK_CREATION; i++) {
            shopSkus.add(
                new MasterData()
                    .setShopSkuKey(new ShopSkuKey(10 + i, "123"))
                    .setShelfLife(120, TimeInUnits.TimeUnit.DAY)
            );
        }
        proceedGoldRecalculation(
            key,
            shopSkus,
            // category
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            MasterDataSourceType.AUTO,
            // existing gold
            false,
            MasterDataSourceType.AUTO,
            MasterDataSourceType.EXPIR_DATE_FROM_CATEGORY_SETTINGS_SOURCE_ID,
            // expected after recalculation
            true,
            MasterDataSourceType.AUTO,
            MasterDataSourceType.EXPIR_DATE_FROM_SHELF_LIVES_SOURCE_ID
        );
    }

    @Test
    public void whenHaveEnoughShopSskusWithSameShelfLifeShouldNotChangeManualParam() {
        ModelKey key = new ModelKey(1L, 100L);
        List<MasterData> shopSkus = new ArrayList<>();
        for (int i = 0; i < MskuSilverItemPreProcessor.SUPPLIERS_FOR_EXPIR_DATE_BLOCK_CREATION; i++) {
            shopSkus.add(
                new MasterData()
                    .setShopSkuKey(new ShopSkuKey(10 + i, "123"))
                    .setShelfLife(120, TimeInUnits.TimeUnit.DAY)
            );
        }
        proceedGoldRecalculation(
            key,
            shopSkus,
            // category
            KnownMdmParams.EXPIRATION_DATES_REQUIRED_OPTION,
            MasterDataSourceType.AUTO,
            // existing gold
            false,
            MasterDataSourceType.MDM_OPERATOR,
            "manual_change",
            // expected after recalculation
            false,
            MasterDataSourceType.MDM_OPERATOR,
            "manual_change"
        );
    }

    @Test
    public void whenHaveMboOperatorChangesNotRewriteItWithAutoCalculations() {
        proceedGoldRecalculation(
            // category
            KnownMdmParams.EXPIRATION_DATES_NOT_ALLOWED_OPTION,
            MasterDataSourceType.MDM_OPERATOR,
            // existing gold
            true,
            MasterDataSourceType.MBO_OPERATOR,
            "krotkov",
            // expected after recalculation
            true,
            MasterDataSourceType.MBO_OPERATOR,
            "krotkov"
        );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void proceedGoldRecalculation(
        MdmParamOption categoryOption,
        MasterDataSourceType categorySourceType,
        boolean existingGoldValue,
        MasterDataSourceType existingGoldSourceType,
        String existingGoldSourceId,
        boolean expectedGoldValue,
        MasterDataSourceType expectedGoldSourceType,
        String expectedGoldSourceId
    ) {
        ModelKey key = new ModelKey(1L, 100L);
        ShopSkuKey offer = new ShopSkuKey(12, "123");
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(offer);
        proceedGoldRecalculation(
            key,
            List.of(masterData),
            categoryOption,
            categorySourceType,
            existingGoldValue,
            existingGoldSourceType,
            existingGoldSourceId,
            expectedGoldValue,
            expectedGoldSourceType,
            expectedGoldSourceId
        );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void proceedGoldRecalculation(
        ModelKey modelKey,
        List<MasterData> shopSkusMD,
        MdmParamOption categoryOption,
        MasterDataSourceType categorySourceType,
        boolean existingGoldValue,
        MasterDataSourceType existingGoldSourceType,
        String existingGoldSourceId,
        boolean expectedGoldValue,
        MasterDataSourceType expectedGoldSourceType,
        String expectedGoldSourceId
    ) {
        // Сперва придётся проделать много подготовительной работы.
        // 1. Положим модельку в очередь.
        enqueueMskusWithReason(List.of(modelKey.getModelId()), MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR);

        // 2. Загрузим SSKU мастер-данные
        for (MasterData masterData : shopSkusMD) {
            masterDataRepository.insert(masterData);
        }

        // 3. Сохраним настройку уровня категории
        CategoryParamValue categoryValue = new CategoryParamValue().setCategoryId(modelKey.getCategoryId());
        categoryValue.setMdmParamId(KnownMdmParams.EXPIRATION_DATES_APPLY);
        categoryValue.setOption(categoryOption);
        categoryValue.setXslName("ExpirationDatesApply");
        categoryValue.setMasterDataSourceType(categorySourceType);
        categoryParamValueRepository.insert(categoryValue);

        // 4. Создадим маппинги: модель + категория -> офферы.
        shopSkusMD.stream().map(MasterData::getShopSkuKey).forEach(
            offer -> mappingsCacheRepository
                .insert(new MappingCacheDao().setModelKey(modelKey).setShopSkuKey(offer).setUpdateStamp(1L))
        );
        // добавим информацию о поставщике
        mdmSupplierRepository.insertBatch(shopSkusMD.stream()
            .map(MasterData::getSupplierId)
            .distinct()
            .map(id -> new MdmSupplier()
                .setId(id)
                .setType(MdmSupplierType.THIRD_PARTY))
            .collect(Collectors.toList()));

        // 5. Создадим существующую золотую запись, которую нужно будет обновить
        MskuParamValue expirDateMskuValue = new MskuParamValue().setMskuId(modelKey.getModelId());
        expirDateMskuValue.setBool(existingGoldValue);
        expirDateMskuValue.setXslName("expir_date");
        expirDateMskuValue.setMdmParamId(KnownMdmParams.EXPIR_DATE);
        expirDateMskuValue.setMasterDataSourceType(existingGoldSourceType);
        expirDateMskuValue.setMasterDataSourceId(existingGoldSourceId);
        mskuRepository.insertOrUpdateMsku(new CommonMsku(modelKey, List.of(expirDateMskuValue)));

        // 6. Наконец, создадим параметры, которые мы ожидаем увидеть на уровне МСКУ после перерасчёта.
        MskuParamValue expectedExpirDate = new MskuParamValue().setMskuId(modelKey.getModelId());
        expectedExpirDate.setBool(expectedGoldValue);
        expectedExpirDate.setXslName("expir_date");
        expectedExpirDate.setMdmParamId(KnownMdmParams.EXPIR_DATE);
        expectedExpirDate.setMasterDataSourceType(expectedGoldSourceType);
        expectedExpirDate.setMasterDataSourceId(expectedGoldSourceId);

        // Запускаем
        execute();

        // Проверяем
//        assertThat(mskuAndSskuQueue.getUnprocessedBatch(1)).isEmpty();
        List<MskuParamValue> expirDates = mskuRepository.findAllMskus().values().stream()
            .map(msku -> msku.getParamValue(KnownMdmParams.EXPIR_DATE))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
        assertThat(expirDates).containsExactly(expectedExpirDate);
    }
}

