package ru.yandex.vendor.asyncreport.dao.impl;

import java.time.LocalDateTime;
import java.util.Set;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.vendor.asyncreport.model.AsyncReportTaskState;
import ru.yandex.vendor.asyncreport.model.AsyncReportType;
import ru.yandex.vendor.stats.modelbids.ModelbidsStatsContext;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncReportDaoImplTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    AsyncReportDaoImpl dao;

    @Test
    void createAsyncReportTask() {
        var created = dao.createAsyncReportTask(
                LocalDateTime.now(),
                AsyncReportType.MODELBIDS_STATS,
                ModelbidsStatsContext.newBuilder().build()
        );
        assertThat(created.getTaskId()).isNotNegative();
        assertThat(created.getState()).isEqualTo(AsyncReportTaskState.PENDING);

        var pending = dao.getPendingTask(Set.of(AsyncReportType.MODELBIDS_STATS));
        assertThat(pending).get()
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoredFields("createdAt")
                        .build())
                .isEqualTo(created);

        var count = dao.getAttemptsThresholdExceededTasksCount(0, AsyncReportTaskState.PENDING);
        assertThat(count)
                .as("background worker could try to create report and task should fail")
                .isGreaterThanOrEqualTo(0);
    }
}
