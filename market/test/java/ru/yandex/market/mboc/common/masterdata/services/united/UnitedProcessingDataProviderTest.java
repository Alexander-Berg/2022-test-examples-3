package ru.yandex.market.mboc.common.masterdata.services.united;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmModificationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuIdOrOrphanSskuKeyInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuIdOrShopSkuKeyContainer;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.SskuGoldenItemsUpdateTask;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingData;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.united.UnitedProcessingData;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MicrometerWatch;

import static org.assertj.core.api.Assertions.assertThat;

public class UnitedProcessingDataProviderTest extends UnitedProcessingServiceSetupBaseTest {
    private static final long MSKU_ID = 1000L;
    private static final ModelKey MODEL_KEY = new ModelKey(1000L, MSKU_ID);

    private final ThreadLocal<MicrometerWatch> watch = new ThreadLocal<>();

    @Test
    public void updatedProcessingDataNotChangedIfNothingChanged() {
        long mskuId = 100L;
        ModelKey key = new ModelKey(1L, mskuId);
        prepareMskuProcessingData(key);

        UnitedProcessingData unitedProcessingData = unitedProcessingDataProvider.loadProcessingData(
            List.of(new MdmMskuIdOrOrphanSskuKeyInfo().setEntityKey(MdmMskuIdOrShopSkuKeyContainer.ofMsku(mskuId))),
            initWatch(1),
            true
        );
        MskuProcessingData oldMskuProcessingData =
            mskuProcessingDataProvider.loadProcessingData(List.of(mskuId), initWatch(1));
        MskuProcessingData newMskuProcessingData = unitedProcessingDataProvider.updateProcessingDataByNewSsku(
            unitedProcessingData.getUnitedProcessingRawData(),
            SskuGoldenItemsUpdateTask.EMPTY
        ).getMskuProcessingData();

        assertThat(newMskuProcessingData.getContexts().get(key.getCategoryId()).toString())
            .isEqualTo(oldMskuProcessingData.getContexts().get(key.getCategoryId()).toString());
        assertThat(newMskuProcessingData.getMappings()).isEqualTo(oldMskuProcessingData.getMappings());
        assertThat(newMskuProcessingData.getWorkingMskuIds().toArray())
            .isEqualTo(oldMskuProcessingData.getWorkingMskuIds().toArray());
        assertThat(newMskuProcessingData.getOldGold().get(key).getValues())
            .usingElementComparatorIgnoringFields("modificationInfo")
            .isEqualTo(oldMskuProcessingData.getOldGold().get(key).getValues());
        assertThat(newMskuProcessingData.getMasterData(key))
            .containsExactlyInAnyOrderElementsOf(oldMskuProcessingData.getMasterData(key));
        assertThat(newMskuProcessingData.getSskuGoldenParamValues(key))
            .containsExactlyInAnyOrderElementsOf(oldMskuProcessingData.getSskuGoldenParamValues(key));
        assertThat(newMskuProcessingData.getPriceInfos(key))
            .containsExactlyInAnyOrderElementsOf(oldMskuProcessingData.getPriceInfos(key));
    }


    @Test
    public void producedMskuProcessingDataIsValid() {
        prepareMskuProcessingData(MODEL_KEY);

        MskuProcessingData oldMskuProcessingData =
            mskuProcessingDataProvider.loadProcessingData(List.of(MSKU_ID), initWatch(1));
        MdmMskuIdOrOrphanSskuKeyInfo info =
            new MdmMskuIdOrOrphanSskuKeyInfo().setEntityKey(MdmMskuIdOrShopSkuKeyContainer.ofMsku(MSKU_ID));
        MskuProcessingData newMskuProcessingData = unitedProcessingDataProvider.loadProcessingData(
                List.of(info),
                initWatch(1),
                true)
            .getMskuProcessingData();

        assertThat(newMskuProcessingData.getContexts().get(MODEL_KEY.getCategoryId()).toString()).
            isEqualTo(oldMskuProcessingData.getContexts().get(MODEL_KEY.getCategoryId()).toString());
        assertThat(newMskuProcessingData.getMappings()).isEqualTo(oldMskuProcessingData.getMappings());
        assertThat(newMskuProcessingData.getWorkingMskuIds().toArray())
            .isEqualTo(oldMskuProcessingData.getWorkingMskuIds().toArray());
        assertThat(newMskuProcessingData.getOldGold().get(MODEL_KEY).getValues())
            .usingElementComparatorIgnoringFields("modificationInfo")
            .isEqualTo(oldMskuProcessingData.getOldGold().get(MODEL_KEY).getValues());
        assertThat(newMskuProcessingData.getMasterData(MODEL_KEY))
            .containsExactlyInAnyOrderElementsOf(oldMskuProcessingData.getMasterData(MODEL_KEY));
        assertThat(newMskuProcessingData.getSskuGoldenParamValues(MODEL_KEY))
            .containsExactlyInAnyOrderElementsOf(oldMskuProcessingData.getSskuGoldenParamValues(MODEL_KEY));
        assertThat(newMskuProcessingData.getPriceInfos(MODEL_KEY))
            .containsExactlyInAnyOrderElementsOf(oldMskuProcessingData.getPriceInfos(MODEL_KEY));
    }

    private void prepareMskuProcessingData(ModelKey key) {
        // 2. Сгенерим SSKU мастер-данные. Пусть на уровне SSKU будет задан ТН ВЭД и применимость срока годности.
        ShopSkuKey offer = new ShopSkuKey(12, "moo");
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(offer);
        masterData.setCustomsCommodityCode("50600980031");
        masterDataRepository.insert(masterData);

        //сгенерим белые данные и данные неизвестного поставщика - они не должны повлиять на расчет
        ShopSkuKey offerWhite = new ShopSkuKey(22, "mau");
        MasterData masterDataWhite = new MasterData().setShopSkuKey(offerWhite).setCustomsCommodityCode("700");
        masterDataWhite.setVat(VatRate.VAT_10);
        masterDataRepository.insert(masterDataWhite);
        ShopSkuKey offerUnknown = new ShopSkuKey(32, "gav");
        masterDataRepository.insert(new MasterData().setShopSkuKey(offerUnknown).setCustomsCommodityCode("800"));

        // 3. Сгенерим настройку уровня категории. Пусть это будет префикс ТН ВЭДа (не путать с цельным ТН ВЭДом выше).
        CategoryParamValue categoryValue = new CategoryParamValue().setCategoryId(key.getCategoryId());
        categoryValue.setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX);
        categoryValue.setString("50600");
        categoryValue.setXslName(mdmParamCache.get(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX).getXslName());
        categoryValue.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);
        categoryParamValueRepository.insert(categoryValue);

        // 4. Создадим маппинг: модель + категория + оффер.
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offer).setUpdateStamp(1L));
        //мапинги для данных белонеизвестных поставщиков
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key)
            .setMappingKind(MappingCacheDao.MappingKind.SUGGESTED)
            .setShopSkuKey(offerWhite).setUpdateStamp(2L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key)
            .setMappingKind(MappingCacheDao.MappingKind.SUGGESTED)
            .setShopSkuKey(offerUnknown).setUpdateStamp(3L));
        // добавим информацию о поставщике
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(offer.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(offerWhite.getSupplierId())
            .setType(MdmSupplierType.MARKET_SHOP));

        // 5. Для чистоты эксперимента создадим уже существующий золотой параметр. Пусть это будет КГТ.
        MskuParamValue heavyGoodMskuValue = new MskuParamValue().setMskuId(key.getModelId());
        heavyGoodMskuValue.setBool(true);
        heavyGoodMskuValue.setXslName(mdmParamCache.get(KnownMdmParams.HEAVY_GOOD).getXslName());
        heavyGoodMskuValue.setMdmParamId(KnownMdmParams.HEAVY_GOOD);
        heavyGoodMskuValue.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        mskuRepository.insertOrUpdateMsku(new CommonMsku(key, List.of(heavyGoodMskuValue)));

        // 6. Наконец, создадим параметры, которые мы ожидаем увидеть на уровне МСКУ после перерасчёта.
        MskuParamValue expectedCustomsCommCode = new MskuParamValue().setMskuId(key.getModelId());
        expectedCustomsCommCode.setString(masterData.getCustomsCommodityCode());
        expectedCustomsCommCode.setXslName(mdmParamCache.get(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID).getXslName());
        expectedCustomsCommCode.setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID);
        expectedCustomsCommCode.setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedCodePrefix = new MskuParamValue().setMskuId(key.getModelId());
        categoryValue.copyTo(expectedCodePrefix);
        expectedCodePrefix.setModificationInfo(
            new MdmModificationInfo()
                .setMasterDataSourceType(MasterDataSourceType.AUTO)
                .setMasterDataSourceId(MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_CATEGORY_SETTINGS)
        );
    }

    private MicrometerWatch initWatch(int batchSize) {
        if (watch.get() != null) {
            return watch.get();
        }

        MicrometerWatch multiwatch = new MicrometerWatch("mw_msku_ssku_gold_steps", List.of("kek"), batchSize);
        multiwatch.startGlobal();
        watch.set(multiwatch);
        return multiwatch;
    }
}
