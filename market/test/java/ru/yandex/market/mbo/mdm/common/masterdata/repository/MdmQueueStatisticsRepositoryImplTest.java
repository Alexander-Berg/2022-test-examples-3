package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.metrics.models.MdmQueueStatistics;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToDatacampQRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

/**
 * @author albina-gima
 * @date 11/16/21
 */
public class MdmQueueStatisticsRepositoryImplTest extends MdmBaseDbTestClass {
    private static final String QUEUE_1 = SendToDatacampQRepositoryImpl.TABLE;
    private static final String QUEUE_2 = SskuToRefreshRepositoryImpl.TABLE;

    private static final Instant OLD_TS = Instant.now().minus(4, ChronoUnit.MINUTES);
    private static final Instant FRESH_TS = Instant.now().minus(3, ChronoUnit.MINUTES);

    @Autowired
    private MdmQueueStatisticsRepository repository;

    @Test
    public void testInsertIfNotPresent() {
        // given
        var statExpected1 = new MdmQueueStatistics(QUEUE_1, OLD_TS, 150L, 100L);
        var statExpected2 = new MdmQueueStatistics(QUEUE_1, FRESH_TS, 150L, 100L);

        // when
        repository.insertIfNotPresent(List.of(statExpected1, statExpected2));

        // then
        List<MdmQueueStatistics> resultQueueStat = repository.getStatisticsForQueue(QUEUE_1, OLD_TS, Instant.now());
        Assertions.assertThat(resultQueueStat).isNotEmpty();
        Assertions.assertThat(resultQueueStat.size()).isEqualTo(2);

        // check that timestamps were truncated to minutes
        statExpected1.setTs(OLD_TS.truncatedTo(ChronoUnit.MINUTES));
        statExpected2.setTs(FRESH_TS.truncatedTo(ChronoUnit.MINUTES));
        Assertions.assertThat(resultQueueStat).containsExactlyInAnyOrder(statExpected1, statExpected2);
    }

    @Test
    public void testGetStatisticsForTodayForAllQueues() {
        // given
        // ожидаемая статистика для каждой очереди: добавлено 350 записей, обработано - 200
        var statExpected1 = new MdmQueueStatistics(QUEUE_1, OLD_TS, 200L, 100L);
        var statExpected2 = new MdmQueueStatistics(QUEUE_1, FRESH_TS, 150L, 100L);
        var statExpected3 = new MdmQueueStatistics(QUEUE_2, OLD_TS, 200L, 100L);
        var statExpected4 = new MdmQueueStatistics(QUEUE_2, FRESH_TS, 150L, 100L);
        repository.insertIfNotPresent(List.of(statExpected1, statExpected2, statExpected3, statExpected4));

        // when
        Map<String, List<MdmQueueStatistics>> todayStatForAllQueues = repository.getStatisticsForTodayForAllQueues();

        // then
        Assertions.assertThat(todayStatForAllQueues).isNotEmpty();
        Assertions.assertThat(todayStatForAllQueues.keySet()).containsExactlyInAnyOrder(QUEUE_1, QUEUE_2);

        Assertions.assertThat(todayStatForAllQueues.get(QUEUE_1).size()).isEqualTo(1);
        Assertions.assertThat(todayStatForAllQueues.get(QUEUE_2).size()).isEqualTo(1);

        Assertions.assertThat(todayStatForAllQueues.get(QUEUE_1).get(0).getAddedCount()).isEqualTo(350L);
        Assertions.assertThat(todayStatForAllQueues.get(QUEUE_1).get(0).getProcessedCount()).isEqualTo(200L);

        Assertions.assertThat(todayStatForAllQueues.get(QUEUE_2).get(0).getAddedCount()).isEqualTo(350L);
        Assertions.assertThat(todayStatForAllQueues.get(QUEUE_2).get(0).getProcessedCount()).isEqualTo(200L);
    }
}
