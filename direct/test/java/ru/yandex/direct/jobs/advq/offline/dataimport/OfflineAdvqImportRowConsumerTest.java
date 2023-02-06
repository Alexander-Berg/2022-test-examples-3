package ru.yandex.direct.jobs.advq.offline.dataimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupShowsForecast;
import ru.yandex.direct.core.entity.keyword.model.KeywordForecast;
import ru.yandex.direct.jobs.advq.offline.processing.OfflineAdvqProcessingBaseTableRow;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

class OfflineAdvqImportRowConsumerTest {
    private static final int TEST_CHUNK_SIZE = 20;

    private OfflineAdvqImportRowConsumer consumer;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);

        consumer = new OfflineAdvqImportRowConsumer();
    }

    @Test
    void testFinalizeEmpty() {
        List<AdGroupShowsForecast> data = consumer.getDataAndCleanup();
        assertThat("Пустой результат", data.size(), equalTo(0));
    }

    @Test
    void testFinalize() {
        List<AdGroupShowsForecast> groupForecasts = consumeAdGroupShowsForecasts(TEST_CHUNK_SIZE);
        List<AdGroupShowsForecast> data = consumer.getDataAndCleanup();

        assertThat("Были сохранены все данные", data.size(), equalTo(TEST_CHUNK_SIZE + 1));
        assertThat("Данные были сохранены", data,
                containsInAnyOrder(mapList(groupForecasts, BeanDifferMatcher::beanDiffer)));
    }

    @Test
    void testFinalizeCleansConsumer() {
        consumeAdGroupShowsForecasts(TEST_CHUNK_SIZE);
        consumer.getDataAndCleanup();

        assertThat("Был сохранен чанк ожидаемого размера", consumer.getGroupIdToGeo().size(), equalTo(0));
        assertThat("Данные были сохранены", consumer.getGroupIdToData().size(), equalTo(0));
    }

    private List<AdGroupShowsForecast> consumeAdGroupShowsForecasts(int chunkSize) {
        Random random = new Random();
        List<AdGroupShowsForecast> groupForecasts = new ArrayList<>();
        for (int groupNum = 0; groupNum < chunkSize; groupNum++) {
            long adGroupId = groupNum + 1;
            List<KeywordForecast> forecasts = new ArrayList<>();

            int keywordsAmount = random.nextInt(50) + 1;
            for (int keywordNum = 1; keywordNum <= keywordsAmount; keywordNum++) {
                long keywordId = groupNum * 100 + keywordNum;
                long forecast = random.nextInt(1024000);
                String keyword = "keyword" + adGroupId + keywordId;
                forecasts.add(
                        new KeywordForecast()
                                .withId(keywordId)
                                .withKeyword(keyword)
                                .withShowsForecast(forecast));

                consumer.accept(getInputRow(keywordId, adGroupId, keyword, "1,2", forecast));
            }

            groupForecasts.add(
                    new AdGroupShowsForecast()
                            .withId(adGroupId)
                            .withGeo("1,2")
                            .withKeywordForecasts(forecasts));
        }

        consumer.accept(getInputRow(0, 1234567L, "", "1,2,3", 0));
        groupForecasts.add(
                new AdGroupShowsForecast()
                        .withId(1234567L)
                        .withGeo("1,2,3")
                        .withKeywordForecasts(Collections.emptyList()));
        return groupForecasts;
    }

    private OfflineAdvqProcessingBaseTableRow getInputRow(long id, long groupId, String keyword, String geo,
                                                          long forecast) {
        OfflineAdvqProcessingBaseTableRow row = new OfflineAdvqProcessingBaseTableRow();
        row.setValue(OfflineAdvqProcessingBaseTableRow.ID, id);
        row.setValue(OfflineAdvqProcessingBaseTableRow.GROUP_ID, groupId);
        row.setValue(OfflineAdvqProcessingBaseTableRow.ORIGINAL_KEYWORD, keyword);
        row.setValue(OfflineAdvqProcessingBaseTableRow.GEO, geo);
        row.setValue(OfflineAdvqProcessingBaseTableRow.FORECAST, forecast);
        return row;
    }
}
