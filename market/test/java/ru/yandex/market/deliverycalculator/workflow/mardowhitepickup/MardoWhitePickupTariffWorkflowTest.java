package ru.yandex.market.deliverycalculator.workflow.mardowhitepickup;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.model.FeedSourceType;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.BaseGeneration;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.repository.GenerationRepository;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorMetaStorageService;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.deliverycalculator.workflow.FeedSource;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;

class MardoWhitePickupTariffWorkflowTest extends FunctionalTest {
    private static final int SHOP_ID = 774;
    private static final long TARIFF_ID_AUTO_CALC = 1036;
    private static final long TARIFF_ID_NOT_AUTO_CALC = 1037;
    private static final int FEED_ID = 55335;
    @Autowired
    private DeliveryCalculatorMetaStorageService metaStorageService;

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    private TariffInfoProvider tariffInfoProvider;

    @Autowired
    @Qualifier("mardoWhitePickupIndexerWorkflow")
    private MardoWhitePickupTariffWorkflow indexerWorkflow;

    @Autowired
    @Qualifier("mardoWhitePickupSearchEngineWorkflow")
    private MardoWhitePickupTariffWorkflow searchEngineWorkflow;

    @BeforeEach
    void init() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, url -> url, getClass());
    }

    @Test
    @DbUnitDataSet(before = "basicTest.before.csv")
    @DisplayName("Тест проверяет, что тарифы варятся, добавляются в поколение, авторасчет самовывоза указывается правильно, есть бакеты")
    void basicTest() {
        Collection<? extends Long> tariffIds = indexerWorkflow.getNotExportedTariffIds();
        Assertions.assertIterableEquals(ImmutableList.of(TARIFF_ID_AUTO_CALC, TARIFF_ID_NOT_AUTO_CALC), tariffIds);

        FeedSource feedSource = createFeedSource(SHOP_ID);

        final List<String> bucketUrls = prepareGenerationAndReturnBucketUrls(
                TARIFF_ID_AUTO_CALC, TARIFF_ID_NOT_AUTO_CALC, feedSource, 1, true);
        assertBuckets(feedSource, bucketUrls);

        //пробуем добавить еще одно поколение с этими же тарифами
        final List<String> bucketUrls2 = prepareGenerationAndReturnBucketUrls(
                TARIFF_ID_AUTO_CALC, TARIFF_ID_NOT_AUTO_CALC, feedSource, 2, true);
        assertBuckets(feedSource, bucketUrls2);

        //проверяем, что у обоих поколений не поменялись бакеты
        Assertions.assertEquals(bucketUrls.get(0), bucketUrls2.get(1));
    }

    @Nonnull
    private List<String> prepareGenerationAndReturnBucketUrls(long tariffIdAutoCalc,
                                                              long tariffIdNotAutoCalc,
                                                              FeedSource feedSource,
                                                              int generationId,
                                                              boolean isAutoCalc) {
        Generation generation = new Generation(generationId, generationId);

        WhitePickupMetaTariff metaTariffAC = getWhitePickupMetaTariff(generation, tariffIdAutoCalc);
        WhitePickupMetaTariff metaTariffNotAC = tariffIdNotAutoCalc > 0 ?
                getWhitePickupMetaTariff(generation, tariffIdNotAutoCalc) : null;

        metaStorageService.addGeneration(generation);

        List<Generation> generations = generationRepository.findByIdGreaterThanEqualOrderById(0L,
                PageRequest.of(0, 100));
        generations.forEach(searchEngineWorkflow::loadFromGeneration);

        return generations.stream().flatMap(g -> g.getMardoWhitePickupGenerations().stream())
                .map(BaseGeneration::getBucketsUrl).collect(Collectors.toList());
    }

    private void assertBuckets(FeedSource feedSource, List<String> bucketUrls) {
        List<String> bucketsUrls = searchEngineWorkflow.getBucketsResponseUrls(FEED_ID, feedSource, null, 1);
        Assertions.assertTrue(bucketsUrls.isEmpty()); //в старых ручках бакетов не должно остаться
        //проверим, что бакеты белого самовывоза всё же сварились
        Assertions.assertTrue(bucketUrls.size() > 0 && bucketUrls.stream().allMatch(Objects::nonNull));
    }

    private WhitePickupMetaTariff getWhitePickupMetaTariff(Generation generation, long tariffId) {
        WhitePickupPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(tariffId);
        preparedTariff.getFeedDeliveryOptionsResp(1, 1);
        indexerWorkflow.addToGeneration(generation, tariffId, preparedTariff, "bucketsUrl");
        return preparedTariff.getMetaTariff();
    }

    @Nonnull
    private FeedSource createFeedSource(long shopId) {
        FeedSource feedSource = new FeedSource();

        feedSource.setType(FeedSourceType.SHOP);
        feedSource.setShopId(shopId);
        feedSource.setRegionId(213);

        return feedSource;
    }
}
