package ru.yandex.market.logistics.logistics4shops.jobs;

import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.logging.code.LogisticEventCode;
import ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

@DisplayName("Тест подсчета неэкспортированных событий")
class PublishLogbrokerLogisticEventsStatisticsJobTest extends AbstractIntegrationTest {

    @Autowired
    private PublishLogbrokerLogisticEventsStatisticsJob job;

    @Test
    @DisplayName("Проверка записи статистики в лог")
    @DatabaseSetup("/jobs/publish_logbroker_logistic_events_statistics/before/multiple_events.xml")
    @SneakyThrows
    void logStatistics() {
        job.execute(null);

        assertLogs().anyMatch(BackLogAssertions.logEqualsTo(
            TskvLogRecord.info("2 logistic events pending for export to logbroker")
                .setLoggingCode(LogisticEventCode.COUNT_NOT_EXPORTED_LOGISTIC_EVENTS)
                .setExtra(Map.of("amount", "2"))
        ));
    }
}
