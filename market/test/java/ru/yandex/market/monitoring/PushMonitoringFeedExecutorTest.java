package ru.yandex.market.monitoring;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.upload.FileUploadService;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.event.BaseLogbrokerEvent;
import ru.yandex.market.mbi.core.IndexerApiClient;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link PushMonitoringFeedExecutor}.
 */
@DbUnitDataSet(before = "pushMonitoringExecutorTest.before.csv")
class PushMonitoringFeedExecutorTest extends FunctionalTest {

    @Autowired
    private FileUploadService uploadService;

    @Autowired
    private IndexerApiClient indexerApiClient;

    @Autowired
    private PushMonitoringFeedExecutor pushMonitoringFeedExecutor;

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @DisplayName("Проверяем, что если в енв пропертях будут неизвестные фид или ид магаза, то проверка упадет")
    @Test
    void doJobEmpty() {

        assertThatThrownBy(() -> pushMonitoringFeedExecutor.doJob(null))
                .isInstanceOfSatisfying(RuntimeException.class,
                        exception -> {
                            assertThat(exception.getMessage())
                                    .contains("Price feed not found for supplier 666. Check environment properties");
                            assertThat(exception.getSuppressed()[0].getMessage())
                                    .matches("Feed with id \\d+ not found. Check environment properties");
                        });
    }

    @Test
    @DbUnitDataSet(before = "pushMonitoringExecutorTestDoJob.before.csv",
            after = "pushMonitoringFeedExecutorTest.after.csv")
    void doJob() {
        var captor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        when(uploadService.getStandardLocationUrl(any())).thenReturn("tada");
        doNothing().when(feedProcessorUpdateLogbrokerEventPublisher).publishEvent(captor.capture());

        pushMonitoringFeedExecutor.doJob(null);

        verifyZeroInteractions(indexerApiClient);

        List<Long> feedIds = captor.getAllValues().stream()
                .map(BaseLogbrokerEvent::getPayload)
                .map(FeedUpdateTaskOuterClass.FeedUpdateTask::getFeed)
                .map(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo::getFeedId)
                .collect(Collectors.toList());
        List<Long> shopIds = captor.getAllValues().stream()
                .map(BaseLogbrokerEvent::getPayload)
                .map(FeedUpdateTaskOuterClass.FeedUpdateTask::getFeed)
                .map(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo::getShopId)
                .collect(Collectors.toList());

        assertThat(feedIds).containsExactlyInAnyOrder(10669L, 10667L, 30666L);
        assertThat(shopIds).containsExactlyInAnyOrder(669L, 669L, 666L);
    }
}
