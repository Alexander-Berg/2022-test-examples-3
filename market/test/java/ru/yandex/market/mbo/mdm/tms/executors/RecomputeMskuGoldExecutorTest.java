package ru.yandex.market.mbo.mdm.tms.executors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.mutable.MutableInt;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmModificationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueuePriorities;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.services.WarehouseProjectionCacheImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuCalculatingProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuCalculatingProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingDataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingDataProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingPipeProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingPipeProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuSskuWithPriorityProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuSskuWithPriorityProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.RecomputeMskuGoldServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessMskuQueueService;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.BatchProcessingProperties.BatchProcessingPropertiesBuilder.constantBatchProperties;

@SuppressWarnings("checkstyle:MagicNumber")
public class RecomputeMskuGoldExecutorTest extends RecomputeMskuGoldExecutorBaseTest {


    @Test
    public void whenEmptyQueueThenNothingHappens() {
        Preconditions.checkArgument(mskuQueue.findAll().isEmpty());

        executor.execute();
        assertThat(mskuRepository.findAllMskus()).isEmpty();
    }

    @Test
    public void whenEmptyQueueThenExistingGoldUnaffected() {
        Preconditions.checkArgument(mskuQueue.findAll().isEmpty());
        var randomMsku = new CommonMsku(
            1L,
            List.of(
                randomMskuParamValue(1L, KnownMdmParams.PRICE),
                randomMskuParamValue(1L, KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX),
                randomMskuParamValue(1L, KnownMdmParams.HOUSEHOLD_CHEMICALS)
            )
        );
        mskuRepository.insertOrUpdateMsku(randomMsku);
        ShopSkuKey offer = new ShopSkuKey(12, "moo");
        MasterData masterData = TestDataUtils.generateMasterData(offer, random);
        masterDataRepository.insert(masterData);
        mappingsCacheRepository.insert(
            new MappingCacheDao().setMskuId(1L).setCategoryId(9041).setShopSkuKey(offer).setUpdateStamp(1L));
        // добавим информацию о поставщике
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(offer.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));

        executor.execute();
        assertThat(mskuRepository.findAllMskus().values())
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .containsOnly(randomMsku);
    }

    @Test
    public void whenMskuEnqueuedShouldComputeGold() {
        // Сперва придётся проделать много подготовительной работы.
        ModelKey key = new ModelKey(1L, 100L);

        // 1. Положим модельку в очередь.
        mskuQueue.enqueue(key.getModelId(), MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR);

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

        var paramValues = generateCisCargoTypes(key.getModelId());
        paramValues.addAll(List.of(expectedCustomsCommCode, expectedCodePrefix, heavyGoodMskuValue));
        var expectedMsku = new CommonMsku(key, paramValues);

        // Запускаем
        executor.execute();
        assertThat(mskuQueue.getUnprocessedBatch(1)).isEmpty();
        assertThat(mskuRepository.findAllMskus().values())
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .containsOnly(expectedMsku);

        List<MdmMskuQueueInfo> allIMskuToMboInfos = mskuToMboQueue.findAll();
        assertThat(allIMskuToMboInfos).hasSize(1);
        assertThat(allIMskuToMboInfos.stream())
            .filteredOn(info -> !info.isProcessed())
            .map(MdmQueueInfoBase::getEntityKey)
            .allMatch(id -> id == key.getModelId());
    }

    @Test
    public void whenMskuProcessedSeveralTimesShouldPopulateSskuQueueInAppropriateManner() {
        // Проверим весьма специфичный кейс, когда одна и та же модель может свариться несколько раз. Здесь нам важно
        // убедиться, что она не будет ломать SSKU-очередь дублями SSKU-ключиков.
        ModelKey key1 = new ModelKey(1L, 100L);
        ModelKey key2 = new ModelKey(2L, 100L);

        // 1. Положим модельку в очередь.
        mskuQueue.enqueue(key1.getModelId(), MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR);

        // 3. Сгенерим SSKU мастер-данные. Пусть на уровне SSKU будет задан ТН ВЭД и применимость срока годности.
        // В данном тесте совершенно не принципиально, какие именно там значения, главное чтобы что-то сварилось.
        ShopSkuKey offer1 = new ShopSkuKey(12, "moo");
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(offer1);
        masterData.setCustomsCommodityCode("50600980031");
        masterData.setShelfLifeRequired(true);
        masterDataRepository.insert(masterData);

        ShopSkuKey offer2 = new ShopSkuKey(13, "moo");
        MasterData masterData2 = new MasterData();
        masterData2.setShopSkuKey(offer2);
        masterData2.setCustomsCommodityCode("50600980031");
        masterData2.setShelfLifeRequired(false);
        masterDataRepository.insert(masterData2);

        // 4. Создадим маппинг: модель + категория + оффер. Здесь важно то, что одна и та же модель мапится дважды
        // с разной категорией. В идеальном мире такого быть не должно, но на практике встречается.
        mappingsCacheRepository.insert(
            new MappingCacheDao().setModelKey(key1).setShopSkuKey(offer1).setUpdateStamp(1L));
        mappingsCacheRepository.insert(
            new MappingCacheDao().setModelKey(key2).setShopSkuKey(offer2).setUpdateStamp(2L));
        // добавим информацию о поставщике
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(offer1.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(offer2.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));

        // Запускаем
        executor.execute();

        // Проверяем, что а) SSKU очередь не сломалась на вставку
        sskuQueue.enqueueAll(List.of(offer1, offer2), MdmEnqueueReason.DEFAULT);

        // и б) очередь не сломалась на чтение.
        MutableInt batchCounter = new MutableInt(0);
        List<ShopSkuKey> enqueuedSskus = new ArrayList<>();
        sskuQueue.processUniqueEntitiesInBatches(
            constantBatchProperties(1).build(),
            infos -> {
                List<ShopSkuKey> keys = infos.stream().map(MdmQueueInfoBase::getEntityKey).collect(Collectors.toList());
                batchCounter.increment();
                enqueuedSskus.addAll(keys);
                return true;
            });
        assertThat(batchCounter.intValue()).isEqualTo(2);
        assertThat(enqueuedSskus).containsExactlyInAnyOrder(offer1, offer2);

        List<MdmMskuQueueInfo> allIMskuToMboInfos = mskuToMboQueue.findAll();
        assertThat(allIMskuToMboInfos).hasSize(1);
        assertThat(allIMskuToMboInfos.stream())
            .filteredOn(info -> !info.isProcessed())
            .map(MdmQueueInfoBase::getEntityKey)
            .allMatch(id -> id == key2.getModelId());
    }

    @Test
    public void whenExceptionIsThrownThenOldParamsAreKeptIntact() {
        // В ходе перевычислений мы делаем потенциально опасную процедуру - сперва удаляем все существующие МСКУ
        // параметры для конкретной модели. Нужно убедиться, что в случае рандомного падения посередине процесса мы
        // всегда откатываемся на прежнее состояние и не теряем информацию.
        MasterDataValidator masterDataValidator = mock(MasterDataValidator.class);
        when(masterDataValidator.validateMasterData(any(MasterData.class))).thenReturn(List.of());
        MasterDataRepository masterDataRepository = Mockito.mock(MasterDataRepository.class);
        when(masterDataRepository.findByShopSkuKeysWithoutNulls(anyCollection(), anyBoolean()))
            .thenThrow(RuntimeException.class);

        MskuProcessingDataProvider assistant = new MskuProcessingDataProviderImpl(
            mskuRepository,
            categoryParamValueRepository,
            null,
            masterDataRepository,
            globalParamValueService,
            goldSskuRepository,
            storageKeyValueService,
            priceInfoRepository,
            Mockito.mock(WarehouseProjectionCacheImpl.class),
            mdmParamCache,
            mdmBestMappingsProvider
        );

        RecomputeMskuGoldServiceImpl recomputeMskuGoldService = new RecomputeMskuGoldServiceImpl(
            assistant,
            mskuProcessingPipeProcessor(),
            mskuCalculatingProcessor(null, masterDataValidator));

        ProcessMskuQueueService processMskuQueueService = new ProcessMskuQueueService(mskuQueue,
            storageKeyValueService,
            recomputeMskuGoldService);

        RecomputeMskuGoldExecutor executor = new RecomputeMskuGoldExecutor(processMskuQueueService);
        ModelKey key = new ModelKey(1L, 100L);

        // 1. Положим модельку в очередь.
        mskuQueue.enqueue(key.getModelId(), MdmEnqueueReason.CHANGED_MAPPING_EOX);

        // 3. Создадим маппинг: модель + категория + оффер.
        ShopSkuKey offer = new ShopSkuKey(12, "moo");
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offer).setUpdateStamp(1L));
        // добавим информацию о поставщике
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(offer.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));

        // 4. Создадим несколько уже существующих золотых параметров. Пусть это будет ghtabrc
        // ТН ВЭД и применимость сроков годности.
        MskuParamValue expirDateMskuValue = new MskuParamValue().setMskuId(key.getModelId());
        expirDateMskuValue.setBool(false);
        expirDateMskuValue.setXslName(mdmParamCache.get(KnownMdmParams.EXPIR_DATE).getXslName());
        expirDateMskuValue.setMdmParamId(KnownMdmParams.EXPIR_DATE);
        expirDateMskuValue.setUpdatedTs(TimestampUtil.toInstant(DateTimeUtils.dateTimeNow().minusDays(1)));
        MskuParamValue vatMskuValue = new MskuParamValue().setMskuId(key.getModelId());
        vatMskuValue.setString("111");
        vatMskuValue.setXslName(mdmParamCache.get(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX).getXslName());
        vatMskuValue.setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX);
        var existingMsku = new CommonMsku(key, List.of(expirDateMskuValue, vatMskuValue));
        mskuRepository.insertOrUpdateMsku(existingMsku);

        // Запускаем
        try {
            executor.execute();
            fail("Should not reach this point.");
        } catch (RuntimeException any) {
            // Из-за фейкового падения мы не помечаем триггер обработанным.
            assertThat(mskuQueue.getUnprocessedBatch(1).get(0).getEntityKey()).isEqualTo(key.getModelId());

            // Эти параметры должны были удалиться, но так как транзакция откатилась после падения,
            // параметры обязаны вернуться к жизни и остаться без изменений.
            assertThat(mskuRepository.findAllMskus().values())
                .map(TestBmdmUtils::removeBmdmIdAndVersion)
                .containsOnly(existingMsku);
        }
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
    public void testUsingOfSskuGoldenParamsForMskuCalculation() {
        ModelKey key1 = new ModelKey(1L, 100L);

        SskuGoldenParamValue paramValue1 = generateSskuGoldenParam();
        paramValue1.setMdmParamId(KnownMdmParams.SSKU_LENGTH);
        paramValue1.setNumeric(new BigDecimal("10"));

        SskuGoldenParamValue paramValue2 = generateSskuGoldenParam();
        paramValue2.setShopSkuKey(paramValue1.getShopSkuKey());
        paramValue2.setMdmParamId(KnownMdmParams.SSKU_WIDTH);
        paramValue2.setNumeric(new BigDecimal("20"));

        SskuGoldenParamValue paramValue3 = generateSskuGoldenParam();
        paramValue3.setShopSkuKey(paramValue1.getShopSkuKey());
        paramValue3.setMdmParamId(KnownMdmParams.SSKU_HEIGHT);
        paramValue3.setNumeric(new BigDecimal("10"));

        SskuGoldenParamValue paramValue4 = generateSskuGoldenParam();
        paramValue4.setShopSkuKey(paramValue1.getShopSkuKey());
        paramValue4.setMdmParamId(KnownMdmParams.SSKU_WEIGHT_GROSS);
        paramValue4.setNumeric(new BigDecimal("4"));

        goldSskuRepository.insertOrUpdateSsku(
            new CommonSsku(paramValue1.getShopSkuKey())
                .addBaseValue(paramValue1)
                .addBaseValue(paramValue2)
                .addBaseValue(paramValue3)
                .addBaseValue(paramValue4)
        );

        mappingsCacheRepository.insert(
            new MappingCacheDao().setModelKey(key1).setShopSkuKey(paramValue1.getShopSkuKey()).setUpdateStamp(1L));
        // добавим информацию о поставщике
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(paramValue1.getShopSkuKey().getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));

        mskuQueue.enqueue(key1.getModelId(), MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR);

        storageKeyValueService.putValue(MdmProperties.USE_OWN_SSKU_WD_FOR_MSKU_GOLD_GLOBALLY, true);

        executor.execute();

        Map<Long, CommonMsku> resultingMskus = mskuRepository.findAllMskus();
        Assertions.assertThat(resultingMskus).hasSize(1);

        CommonMsku resultingMsku = resultingMskus.get(key1.getModelId());
        Assertions.assertThat(resultingMsku).isNotNull();

        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .hasValueSatisfying(value -> Assertions.assertThat(value).isEqualByComparingTo(new BigDecimal("10")));
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .hasValueSatisfying(value -> Assertions.assertThat(value).isEqualByComparingTo(new BigDecimal("20")));
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .hasValueSatisfying(value -> Assertions.assertThat(value).isEqualByComparingTo(new BigDecimal("10")));
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .hasValueSatisfying(value -> Assertions.assertThat(value).isEqualByComparingTo(new BigDecimal("4")));
    }

    @Test
    public void testNonEnqueueSskuMode() {
        storageKeyValueService.putValue(MdmProperties.PREVENT_SSKUS_ENQUEUE_BY_DEVELOPER_TOOL_ENQUEUED_MSKUS, true);
        ModelKey key1 = new ModelKey(1L, 100L);
        ModelKey key2 = new ModelKey(2L, 200L);
        mskuQueue.enqueue(key1.getModelId(), MdmEnqueueReason.DEVELOPER_TOOL);
        mskuQueue.enqueue(key2.getModelId(), MdmEnqueueReason.DEVELOPER_TOOL);
        mskuQueue.enqueue(key2.getModelId(), MdmEnqueueReason.CHANGED_MAPPING_EOX);
        ShopSkuKey ssku1 = new ShopSkuKey(1, "100");
        ShopSkuKey ssku2 = new ShopSkuKey(2, "200");
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key1).setShopSkuKey(ssku1).setUpdateStamp(1L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key2).setShopSkuKey(ssku2).setUpdateStamp(1L));
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(ssku1.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(ssku2.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData1 = new MasterData()
            .setShopSkuKey(ssku1)
            .setShelfLife(TimeInUnits.UNLIMITED);
        MasterData masterData2 = new MasterData()
            .setShopSkuKey(ssku2)
            .setShelfLife(TimeInUnits.UNLIMITED);
        masterDataRepository.insertBatch(masterData1, masterData2);
        executor.execute();

        Optional<CommonMsku> msku1 = mskuRepository.findMsku(key1.getModelId());
        Assertions.assertThat(msku1)
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .map(CommonMsku::getValues)
            // 2 shelf life + 6 default HS + Mercury false
            .hasValueSatisfying(paramValues -> Assertions.assertThat(paramValues).hasSize(8));
        Assertions.assertThat(msku1)
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.SHELF_LIFE))
            .flatMap(MdmParamValue::getNumeric)
            .hasValue(BigDecimal.ONE);
        Assertions.assertThat(msku1)
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.SHELF_LIFE_UNIT))
            .flatMap(MdmParamValue::getOption)
            .map(MdmParamOption::getId)
            .hasValue(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.UNLIMITED));

        Optional<CommonMsku> msku2 = mskuRepository.findMsku(key2.getModelId());
        Assertions.assertThat(msku1)
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .map(CommonMsku::getValues)
            // 2 shelf life + 6 default false cis cargotypes
            .hasValueSatisfying(paramValues -> Assertions.assertThat(paramValues).hasSize(8));
        Assertions.assertThat(msku2)
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.SHELF_LIFE))
            .flatMap(MdmParamValue::getNumeric)
            .hasValue(BigDecimal.ONE);
        Assertions.assertThat(msku2)
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.SHELF_LIFE_UNIT))
            .flatMap(MdmParamValue::getOption)
            .map(MdmParamOption::getId)
            .hasValue(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.UNLIMITED));

        List<ShopSkuKey> enqueuedSskus = sskuQueue.findAll().stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .collect(Collectors.toList());
        //ssku2 попадет в пересчет, так как key2 был положен не только тулой, но и другим источником (сменой мапингов)
        Assertions.assertThat(enqueuedSskus).containsOnly(ssku2);
    }

    @Test
    public void testMskuProcessingEnqueuesSskusWithCorrectPriorities() {
        storageKeyValueService.putValue(MdmProperties.USE_PRIORITIES_IN_MSKU_TO_REFRESH_QUEUE, true);

        ModelKey key1 = new ModelKey(1L, 100L);
        ModelKey key2 = new ModelKey(2L, 200L);
        // Задаем приоритеты
        mskuQueue.enqueue(key1.getModelId(), MdmEnqueueReason.DEVELOPER_TOOL,
            MdmQueuePriorities.DATACAMP_OFFERS_PRIORITY);
        mskuQueue.enqueue(key2.getModelId(), MdmEnqueueReason.DEVELOPER_TOOL, MdmQueuePriorities.NORMAL_PRIORITY);

        ShopSkuKey ssku1 = new ShopSkuKey(1, "100");
        ShopSkuKey ssku2 = new ShopSkuKey(2, "200");
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key1).setShopSkuKey(ssku1).setUpdateStamp(1L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key2).setShopSkuKey(ssku2).setUpdateStamp(1L));
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(ssku1.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(ssku2.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));
        MasterData masterData1 = new MasterData()
            .setShopSkuKey(ssku1)
            .setShelfLife(TimeInUnits.UNLIMITED);
        MasterData masterData2 = new MasterData()
            .setShopSkuKey(ssku2)
            .setShelfLife(TimeInUnits.UNLIMITED);
        masterDataRepository.insertBatch(masterData1, masterData2);

        executor.execute();

        Map<Integer, List<SskuToRefreshInfo>> enqueuedSskusByPriority = sskuQueue.findAll().stream()
            .collect(Collectors.groupingBy(MdmQueueInfoBase::getPriority));

        Assertions.assertThat(enqueuedSskusByPriority.get(MdmQueuePriorities.DATACAMP_OFFERS_PRIORITY).size())
            .isEqualTo(1);
        Assertions.assertThat(enqueuedSskusByPriority.get(MdmQueuePriorities.DATACAMP_OFFERS_PRIORITY).get(0)
            .getEntityKey()).isEqualTo(ssku1);

        Assertions.assertThat(enqueuedSskusByPriority.get(MdmQueuePriorities.NORMAL_PRIORITY).size()).isEqualTo(1);
        Assertions.assertThat(enqueuedSskusByPriority.get(MdmQueuePriorities.NORMAL_PRIORITY).get(0).getEntityKey())
            .isEqualTo(ssku2);
    }

    @Test
    public void whenEnqueueSskusOnlyBusinessKeysShouldBeEnqueued() {
        storageKeyValueService.putValue(MdmProperties.USE_PRIORITIES_IN_MSKU_TO_REFRESH_QUEUE, true);

        ModelKey key = new ModelKey(1L, 100L);
        // Задаем приоритеты
        mskuQueue.enqueue(key.getModelId(), MdmEnqueueReason.DEVELOPER_TOOL, MdmQueuePriorities.NORMAL_PRIORITY);

        ShopSkuKey business = new ShopSkuKey(213, "200");
        ShopSkuKey service = new ShopSkuKey(3213, "200");
        mappingsCacheRepository.insert(
            new MappingCacheDao().setModelKey(key).setShopSkuKey(business).setUpdateStamp(1L)
        );
        mappingsCacheRepository.insert(
            new MappingCacheDao().setModelKey(key).setShopSkuKey(service).setUpdateStamp(1L)
        );
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(business.getSupplierId())
            .setBusinessEnabled(true)
            .setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(service.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessEnabled(true)
            .setBusinessId(business.getSupplierId()));
        sskuExistenceRepository.markExistence(service, true);
        MasterData masterData = new MasterData()
            .setShopSkuKey(service)
            .setShelfLife(TimeInUnits.UNLIMITED);
        masterDataRepository.insertBatch(masterData);
        mdmSupplierCachingService.refresh();

        executor.execute();

        List<ShopSkuKey> enqueuedSskus = sskuQueue.findAll().stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .collect(Collectors.toList());
        Assertions.assertThat(enqueuedSskus).containsOnly(business);
    }

    @Test
    public void testUsingOfSskuGoldenParamsForMskuShelfLifeGoldCalculation() {
        ModelKey key1 = new ModelKey(1L, 100L);

        SskuGoldenParamValue paramValue1 = new SskuGoldenParamValue();
        paramValue1.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE);
        paramValue1.setNumeric(new BigDecimal("10"));
        paramValue1.setShopSkuKey(new ShopSkuKey(123, "ssku"));
        paramValue1.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        var unitOption =
            new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR));
        SskuGoldenParamValue paramValue2 = new SskuGoldenParamValue();
        paramValue2.setShopSkuKey(paramValue1.getShopSkuKey());
        paramValue2.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE_UNIT);
        paramValue2.setOption(
            new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR)));
        paramValue2.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        SskuGoldenParamValue paramValue3 = new SskuGoldenParamValue();
        paramValue3.setShopSkuKey(paramValue1.getShopSkuKey());
        paramValue3.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE_COMMENT);
        paramValue3.setString("comment");
        paramValue3.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        goldSskuRepository.insertOrUpdateSsku(
            new CommonSsku(paramValue1.getShopSkuKey())
                .addBaseValue(paramValue1)
                .addBaseValue(paramValue2)
                .addBaseValue(paramValue3)
        );

        mappingsCacheRepository.insert(
            new MappingCacheDao().setModelKey(key1).setShopSkuKey(paramValue1.getShopSkuKey()).setUpdateStamp(1L));
        // добавим информацию о поставщике
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(paramValue1.getShopSkuKey().getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));

        mskuQueue.enqueue(key1.getModelId(), MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR);

        executor.execute();

        Map<Long, CommonMsku> resultingMskus = mskuRepository.findAllMskus();
        Assertions.assertThat(resultingMskus).hasSize(1);

        CommonMsku resultingMsku = resultingMskus.get(key1.getModelId());
        Assertions.assertThat(resultingMsku).isNotNull();

        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.SHELF_LIFE)
                .flatMap(MdmParamValue::getNumeric))
            .hasValue(new BigDecimal("10"));
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.SHELF_LIFE_UNIT)
                .flatMap(MdmParamValue::getOption))
            .hasValue(unitOption);
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.SHELF_LIFE_UNIT)
                .flatMap(MdmParamValue::getOption)
                .flatMap(option -> Optional.of(option.getRenderedValue())))
            .hasValue("годы");
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.SHELF_LIFE_COMMENT)
                .flatMap(MdmParamValue::getString))
            .hasValue("comment");
    }

    @Test
    public void zeroMskuShouldProcessSuccessfullyWithoutComputing() {
        // given
        mskuQueue.enqueue(0L, MdmEnqueueReason.CHANGED_BY_MDM_ADMIN, Integer.MAX_VALUE);

        // when
        executor.execute();

        // then
        Assertions.assertThat(mskuQueue.getUnprocessedItemsCount()).isZero();
        Assertions.assertThat(mskuRepository.findAllMskus()).isEmpty();
    }

    private SskuGoldenParamValue generateSskuGoldenParam() {
        return random.nextObject(SskuGoldenParamValue.class);
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

        return new ArrayList<>(List.of(expectedMercuryCisOptional, expectedMercuryCisDistinct,
            expectedMercuryCisRequired,
            expectedHsCisOptional, expectedHsCisDistinct, expectedHsCisRequired));
    }

    private MskuParamValue randomMskuParamValue(long mskuId, long mdmParamId) {
        MskuParamValue result = new MskuParamValue().setMskuId(mskuId);
        TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(mdmParamId)).copyTo(result);
        return result;
    }
}
