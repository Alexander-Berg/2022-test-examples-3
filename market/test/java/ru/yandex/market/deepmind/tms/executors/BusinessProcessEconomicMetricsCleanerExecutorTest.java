package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.BusinessProcessEconomicMetrics;
import ru.yandex.market.deepmind.common.repository.BusinessProcessEconomicMetricsRepository;

public class BusinessProcessEconomicMetricsCleanerExecutorTest extends DeepmindBaseDbTestClass {
    @Autowired
    private BusinessProcessEconomicMetricsRepository economicMetricsRepository;
    private BusinessProcessEconomicMetricsCleanerExecutor executor;

    @Before
    public void setUp() {
        executor = new BusinessProcessEconomicMetricsCleanerExecutor(economicMetricsRepository);
        economicMetricsRepository.save(
            businessProcessMetric("TEST-1", "to_pending", Instant.now()),
            businessProcessMetric("TEST-2", "to_inactive", Instant.now().minus(125, ChronoUnit.DAYS)),
            businessProcessMetric("TEST-3", "to_pending", Instant.now().minus(121, ChronoUnit.DAYS))
        );
    }

    @Test
    public void dropOldRowsByCreatedAtTest() {
        int countBefore = economicMetricsRepository.findAll().size();
        Assertions.assertThat(countBefore).isEqualTo(3);
        executor.execute();
        int countAfter = economicMetricsRepository.findAll().size();
        Assertions.assertThat(countAfter).isEqualTo(1);
    }

    private BusinessProcessEconomicMetrics businessProcessMetric(String ticket, String bp, Instant createdAt) {
        return new BusinessProcessEconomicMetrics()
            .setTicket(ticket)
            .setBusinessProcess(bp)
            .setCreatedAt(createdAt)
            .setRowNum(0)
            .setData(Map.of());
    }
}
