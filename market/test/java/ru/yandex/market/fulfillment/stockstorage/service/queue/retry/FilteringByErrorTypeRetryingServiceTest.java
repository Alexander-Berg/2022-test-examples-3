package ru.yandex.market.fulfillment.stockstorage.service.queue.retry;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueItem;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.UnitIdExecutionQueuePayload;

import static ru.yandex.market.fulfillment.stockstorage.service.execution.ExecutionQueueType.CHANGED_AVAILABILITY_EVENT;

public class FilteringByErrorTypeRetryingServiceTest {

    private final SoftAssertions softly = new SoftAssertions();

    @AfterEach
    public final void triggerSoftAssertions() {
        softly.assertAll();
    }

    @Test
    public void filterTest() {

        FilteringByErrorTypeRetryingService retryingService =
                new FilteringByErrorTypeRetryingService(getClock(), Duration.ZERO, Set.of(ErrorType.FEED_ID_NOT_FOUND));

        List<FailedExecutionQueueItem<UnitIdExecutionQueuePayload>> failedItems = createItemsToRetry();
        List<ExecutionQueueItem<?>> itemsToRetry = retryingService.createItemsToRetry(failedItems);
        Set<String> errors = itemsToRetry.stream()
                .map(ExecutionQueueItem::getFailReason)
                .collect(Collectors.toSet());

        softly.assertThat(itemsToRetry).hasSize(ErrorType.values().length - 1);
        softly.assertThat(errors).doesNotContain(ErrorType.FEED_ID_NOT_FOUND.name());
    }

    @Test
    public void filterAllTest() {

        FilteringByErrorTypeRetryingService retryingService =
                new FilteringByErrorTypeRetryingService(getClock(), Duration.ZERO, EnumSet.allOf(ErrorType.class));

        List<FailedExecutionQueueItem<UnitIdExecutionQueuePayload>> failedItems = createItemsToRetry();
        List<ExecutionQueueItem<?>> itemsToRetry = retryingService.createItemsToRetry(failedItems);

        softly.assertThat(itemsToRetry).hasSize(0);
    }

    private List<FailedExecutionQueueItem<UnitIdExecutionQueuePayload>> createItemsToRetry() {
        return Arrays.stream(ErrorType.values())
                .map(this::createItemToRetry)
                .collect(Collectors.toList());
    }

    private FailedExecutionQueueItem<UnitIdExecutionQueuePayload> createItemToRetry(ErrorType errorType) {
        LocalDateTime now = LocalDateTime.now();
        return new FailedExecutionQueueItem<>(
                getQueueError(errorType),
                ExecutionQueueItem.of(now, now.plusSeconds(1),
                        CHANGED_AVAILABILITY_EVENT,
                        new UnitIdExecutionQueuePayload())
        );
    }

    private QueueError getQueueError(ErrorType errorType) {
        return QueueError.builder()
                .errorType(errorType)
                .errorMessage(errorType.name())
                .build();
    }

    private Clock getClock() {
        ZoneId zoneId = ZoneId.of("Europe/Moscow");
        return Clock.fixed(LocalDateTime.of(2018, 1, 1, 0, 0)
                .atZone(zoneId).toInstant(), zoneId);
    }
}
