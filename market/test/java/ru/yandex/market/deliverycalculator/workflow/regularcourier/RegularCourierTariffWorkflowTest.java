package ru.yandex.market.deliverycalculator.workflow.regularcourier;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.storage.model.FeedSourceType;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.repository.GenerationRepository;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorMetaStorageService;
import ru.yandex.market.deliverycalculator.workflow.FeedSource;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;

import static ru.yandex.market.deliverycalculator.workflow.abstractworkflow.AbstractRegularTariffWorkflow.SHOP_CAMPAIGN_TYPE;

/**
 * Функциональные тесты для программы Своих Тарифов.
 */
class RegularCourierTariffWorkflowTest extends FunctionalTest {

    @Autowired
    private DeliveryCalculatorMetaStorageService metaStorageService;

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    @Qualifier("regularCourierTariffIndexerWorkflow")
    private RegularCourierTariffWorkflow indexerWorkflow;

    @Autowired
    @Qualifier("regularCourierTariffSearchEngineWorkflow")
    private RegularCourierTariffWorkflow searchEngineWorkflow;


    @Test
    @DisplayName("Корректный 'Свой Тариф' с настроенными правилами доставки")
    @DbUnitDataSet(before = "basicTestWithData.before.csv")
    void basicTestWithData() {
        checkTariffProcessing(774, 1, SHOP_CAMPAIGN_TYPE);
    }

    @Test
    @DisplayName("Пустой тариф. Нужен в кэше тарифов для передачи Индексатору use_yml_delivery")
    @DbUnitDataSet(before = "basicTestEmptyTariff.before.csv")
    void basicTestEmptyTariff() {
        // Пустой "Свой Тариф" должен быть загружен в кэш тарифов в не зависимости от что у него нет правил доставки.
        // В отличии от других программ КД, это нужно для передачи в Индексатор параметра use_yml_delivery
        // (через бакет).
        checkTariffProcessing(775, 0, null);
    }

    private void checkTariffProcessing(long shopId, int matrices, String expectedCampaignType) {
        // Варим "свой" тариф
        String bucketsUrl = "bucketsUrl" + shopId;
        PreparedTariff preparedTariff = prepareTariff(shopId, bucketsUrl);

        // Проверяем что параметр "использовать данные из фида" в тарифе совпадает с настройками магазина
        DeliveryCalcProtos.FeedDeliveryOptionsResp deliveryOptionsResp =
                preparedTariff.getFeedDeliveryOptionsResp(1, 1);
        Assertions.assertTrue(deliveryOptionsResp.getUseYmlDelivery());

        // Загружаем информацию о сваренном тарифе
        Generation generation = getLastGeneration();
        searchEngineWorkflow.loadFromGeneration(generation);

        // Проверяем что доступен URL файла с бакетами по сваренному тарифу
        FeedSource feedSource = createFeedSource(shopId);
        List<String> bucketsUrls = searchEngineWorkflow.getBucketsResponseUrls(55335, feedSource, null, 1);
        Assertions.assertIterableEquals(Collections.singleton(bucketsUrl), bucketsUrls);

        // Проверяем наличе тарифа к кэше
        long generationId = generation.getExternalGenerationId();
        TariffInfo tariff = (TariffInfo) searchEngineWorkflow.getTariffCache().getValue(generationId, shopId);
        Assertions.assertNotNull(tariff);
        Assertions.assertEquals(expectedCampaignType, tariff.getCampaignType());
        Assertions.assertEquals(matrices, ArrayUtils.getLength(tariff.getMatrices()));
    }

    private Generation getLastGeneration() {
        PageRequest request = PageRequest.of(0, 100);
        List<Generation> generations = generationRepository.findByIdGreaterThanEqualOrderById(0L, request);
        Assertions.assertEquals(1, generations.size());
        return generations.iterator().next();
    }

    private PreparedTariff prepareTariff(long shopId, String bucketsUrl) {
        List<Long> shopIds = indexerWorkflow.getNotExportedTariffIds();
        Assertions.assertIterableEquals(Collections.singletonList(shopId), shopIds);
        PreparedTariff preparedTariff = indexerWorkflow.prepareTariff(shopId);

        Generation generation = new Generation(1, 1);
        indexerWorkflow.addToGeneration(generation, shopId, preparedTariff, bucketsUrl);
        metaStorageService.addGeneration(generation);

        return preparedTariff;
    }

    private FeedSource createFeedSource(long shopId) {
        FeedSource feedSource = new FeedSource();
        feedSource.setType(FeedSourceType.SHOP);
        feedSource.setShopId(shopId);
        return feedSource;
    }
}
