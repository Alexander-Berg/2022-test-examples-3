package ru.yandex.market.deliverycalculator.workflow.mardowhitecourier;

import java.util.Collection;
import java.util.List;

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
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.repository.GenerationRepository;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorMetaStorageService;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.deliverycalculator.workflow.FeedSource;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;

class MardoWhiteCourierTariffWorkflowTest extends FunctionalTest {

    @Autowired
    private DeliveryCalculatorMetaStorageService metaStorageService;

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    private TariffInfoProvider tariffInfoProvider;

    @Autowired
    @Qualifier("mardoWhiteCourierIndexerWorkflow")
    private MardoWhiteCourierTariffWorkflow indexerWorkflow;

    @Autowired
    @Qualifier("mardoWhiteCourierSearchEngineWorkflow")
    private MardoWhiteCourierTariffWorkflow searchEngineWorkflow;

    @BeforeEach
    void init() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, url -> url, getClass());
    }

    @Test
    @DbUnitDataSet(before = "basicTest.before.csv")
    @DisplayName("Тест проверяет, что тарифы варятся, добавляются в поколение, авторасчет указывается правильно, есть бакеты")
    void basicTest() {
        long shopId = 774;
        long tariffIdAutoCalc = 1036;
        long tariffIdNotAutoCalc = 1037;
        int feedId = 55335;

        Collection<? extends Long> tariffIds = indexerWorkflow.getNotExportedTariffIds();
        Assertions.assertIterableEquals(ImmutableList.of(tariffIdAutoCalc, tariffIdNotAutoCalc), tariffIds);

        FeedSource feedSource = createFeedSource(shopId);

        Generation generation = new Generation(1, 1);

        metaStorageService.addGeneration(generation);

        List<Generation> generations = generationRepository.findByIdGreaterThanEqualOrderById(0L,
                PageRequest.of(0, 100));
        generations.forEach(searchEngineWorkflow::loadFromGeneration);

        List<String> bucketsUrls = searchEngineWorkflow.getBucketsResponseUrls(feedId, feedSource, null, 1);
        Assertions.assertTrue(bucketsUrls.isEmpty()); //в старых ручках курьерских бакетов не должно остаться
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
