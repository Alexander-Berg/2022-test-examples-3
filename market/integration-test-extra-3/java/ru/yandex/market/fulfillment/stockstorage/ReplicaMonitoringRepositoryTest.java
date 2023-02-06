package ru.yandex.market.fulfillment.stockstorage;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.GoldenPercentiles;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.MessagesAmountInformation;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.MessagesAttemptNumberInformation;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.StockFreezeReasonTypeAmount;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.StockFreezeSimpleDto;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReasonType;
import ru.yandex.market.fulfillment.stockstorage.repository.replica.ReplicaMonitoringRepository;
import ru.yandex.market.fulfillment.stockstorage.util.Paging;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.ARCHIVE_SKU;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.CHANGED_ANY_TYPE_OF_STOCKS_AMOUNT_EVENT;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.CHANGED_AVAILABILITY_EVENT;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.CHANGED_STOCKS_AMOUNT_EVENT;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.FEED_ID_MAPPING_SYNC;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.FORCE_UNFREEZE_STOCK;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.FULL_SYNC_STOCK;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.KOROBYTE_SYNC;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.LAST_PAGE_FULL_SYNC_STOCK;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.PRIORITY_FULL_SYNC_STOCK;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.PUSH_STOCKS_EVENT;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.SLOW_FULL_SYNC_STOCK;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.SYNC_STOCKS_BARCODES;


public class ReplicaMonitoringRepositoryTest extends AbstractContextualTest {

    @Autowired
    private ReplicaMonitoringRepository replicaMonitoringRepository;

    private LocalDateTime localDateTime = LocalDateTime.parse("2018-11-20T12:32:31.985988");

    @Test
    @DatabaseSetup("classpath:database/states/freeeze_duration_monitoring_state.xml")
    public void findAllActiveFreezeSummaryTest() {
        Paging paging = new Paging(0, 100);
        final List<StockFreezeSimpleDto> allActiveFreezeSummary =
                replicaMonitoringRepository.findAllActiveFreezeSummary(paging);

        assertEquals(3, allActiveFreezeSummary.size());
        Assertions.assertThat(allActiveFreezeSummary).containsExactlyInAnyOrder(
                new StockFreezeSimpleDto(2L, "123", FreezeReasonType.ORDER, localDateTime),
                new StockFreezeSimpleDto(3L, "12345", FreezeReasonType.ORDER, localDateTime),
                new StockFreezeSimpleDto(5L, "123456", FreezeReasonType.ORDER, localDateTime)
        );

    }

    @Test
    @DatabaseSetup("classpath:database/states/freeeze_amount_monitoring_state.xml")
    public void findFreezeAmountGroupedByReasonType() {
        final List<StockFreezeReasonTypeAmount> freezeAmountGroupedByReasonType =
                replicaMonitoringRepository.findFreezeAmountGroupedByReasonType();

        assertEquals(4, freezeAmountGroupedByReasonType.size());
        Assertions.assertThat(freezeAmountGroupedByReasonType).containsExactlyInAnyOrder(
                new StockFreezeReasonTypeAmount(FreezeReasonType.ORDER, 1, 3010),
                new StockFreezeReasonTypeAmount(FreezeReasonType.ORDER, 2, 500000),
                new StockFreezeReasonTypeAmount(FreezeReasonType.OUTBOUND, 1, 40000),
                new StockFreezeReasonTypeAmount(FreezeReasonType.OUTBOUND, 2, 200)
        );

    }

    @Test
    @DatabaseSetup("classpath:database/states/unfreeze_jobs/queued/setup.xml")
    public void countQueued() {
        Map<String, Object> attemptsMap = replicaMonitoringRepository.countQueued();

        softly.assertThat(attemptsMap.get("amountZeroAttempts"))
                .as("Asserting amountZeroAttempts")
                .isEqualTo(2L);
        softly.assertThat(attemptsMap.get("amountFewAttempts"))
                .as("Asserting amountFewAttempts")
                .isEqualTo(3L);
        softly.assertThat(attemptsMap.get("amountManyAttempts"))
                .as("Asserting amountManyAttempts")
                .isEqualTo(1L);
    }

    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/messages_of_different_type.xml")
    public void shouldFindMessageAmountOfDifferentTypesAndEnrichWithAbsentTypes() {
        List<MessagesAmountInformation> actualMessagesAmountFullInformation = replicaMonitoringRepository
                .findMessagesAmountFullInformation();

        softly.assertThat(
                actualMessagesAmountFullInformation.size()
        ).isEqualTo(17);

        softly.assertThat(
                actualMessagesAmountFullInformation.contains(new MessagesAmountInformation(FULL_SYNC_STOCK.name(), 1,
                        2))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation
                        .contains(new MessagesAmountInformation(SLOW_FULL_SYNC_STOCK.name(), 1,
                                1))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation
                        .contains(new MessagesAmountInformation(CHANGED_AVAILABILITY_EVENT.name()))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation.contains(new MessagesAmountInformation(KOROBYTE_SYNC.name()))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation.contains(new MessagesAmountInformation(FEED_ID_MAPPING_SYNC.name()))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation.contains(new MessagesAmountInformation("UNKNOWN", 3, 1))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation.contains(new MessagesAmountInformation(SYNC_STOCKS_BARCODES.name()))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation
                        .contains(new MessagesAmountInformation(CHANGED_STOCKS_AMOUNT_EVENT.name()))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation
                        .contains(new MessagesAmountInformation(CHANGED_ANY_TYPE_OF_STOCKS_AMOUNT_EVENT.name()))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation
                        .contains(new MessagesAmountInformation(PRIORITY_FULL_SYNC_STOCK.name()))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation
                        .contains(new MessagesAmountInformation(LAST_PAGE_FULL_SYNC_STOCK.name()))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation
                        .contains(new MessagesAmountInformation(PUSH_STOCKS_EVENT.name()))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation
                        .contains(new MessagesAmountInformation(FORCE_UNFREEZE_STOCK.name()))
        ).isTrue();

        softly.assertThat(
                actualMessagesAmountFullInformation
                        .contains(new MessagesAmountInformation(ARCHIVE_SKU.name()))
        ).isTrue();
    }

    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/messages_with_attempt_numbers.xml")
    public void shouldFindMessagesAttemptNumberFullInformation() {
        List<MessagesAttemptNumberInformation> actualMessagesAttemptNumberFullInformation =
                replicaMonitoringRepository.findMessagesAttemptNumberFullInformation();


        softly.assertThat(
                actualMessagesAttemptNumberFullInformation.size()
        ).isEqualTo(4);

        softly.assertThat(
                actualMessagesAttemptNumberFullInformation
                    .contains(new MessagesAttemptNumberInformation(FULL_SYNC_STOCK.name(), "Other reason", 1, 1, 0))
        ).isTrue();

        softly.assertThat(
                actualMessagesAttemptNumberFullInformation
                        .contains(new MessagesAttemptNumberInformation("UNKNOWN", "Other reason", 3, 1, 0))
        ).isTrue();
    }

    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/find_min_execute_after.xml")
    public void findMinExecuteAfterForEachQueue() {
        Map<String, LocalDateTime> map =
                replicaMonitoringRepository.findMinExecuteAfterForEachQueue();
        softly.assertThat(LocalDateTime.parse("1980-01-01T00:00")).isEqualTo(map.get(FULL_SYNC_STOCK.name()));
    }

    @Test
    @DatabaseSetup("classpath:database/states/no_crossdock_sku_divergence_state.xml")
    public void shouldNotFindSkuDivergences() {
        LocalDateTime afterCreation = LocalDateTime.of(2018, 4, 18, 10, 24);

        List<String> actualSkuDivergence = replicaMonitoringRepository
                .findAllCrossdockSkuDivergences(172L, Arrays.asList(1L, 2L), afterCreation);

        softly.assertThat(actualSkuDivergence).isEmpty();
    }

    @Test
    @DatabaseSetup("classpath:database/states/crossdock_sku_divergence_state.xml")
    public void shouldFindSkuDivergences() {
        LocalDateTime afterCreation = LocalDateTime.of(2018, 4, 18, 10, 24);

        List<String> actualSkuDivergences = replicaMonitoringRepository
                .findAllCrossdockSkuDivergences(172L, Arrays.asList(1L, 2L), afterCreation);

        softly.assertThat(actualSkuDivergences).containsExactlyInAnyOrder("sku0", "sku1");
    }

    @Test
    @DatabaseSetup("classpath:database/states/crossdock_sku_divergence_state.xml")
    public void shouldNotFindSkuDivergencesDueToCreationTime() {
        LocalDateTime beforeCreation = LocalDateTime.of(2018, 4, 18, 9, 23, 52);

        List<String> actualSkuDivergences = replicaMonitoringRepository
                .findAllCrossdockSkuDivergences(172L, Arrays.asList(1L, 2L), beforeCreation);

        softly.assertThat(actualSkuDivergences).isEmpty();
    }

    @Test
    @DatabaseSetup("classpath:database/states/sku_preset_for_monitoring_last_sync_state.xml")
    public void shouldNotFailedcalculateSkuLastSyncLag() {
        LocalDateTime dateTime = LocalDateTime.of(2018, 4, 18, 9, 23, 52);

        Set<Integer> fulfillmentsIds = Set.of(20, 30);

        Map<Integer, GoldenPercentiles> map = replicaMonitoringRepository.
                calculateSkuLastSyncLag(dateTime, fulfillmentsIds);

        softly.assertThat(map).isNotEmpty();
        softly.assertThat(map.entrySet().size()).isEqualTo(2);
    }
}
