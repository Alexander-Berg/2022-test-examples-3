    package ru.yandex.market.ff.health.solomon.jobs;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.health.solomon.SolomonPushClient;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.DbQueueService;
import ru.yandex.market.ff.util.FileContentUtils;

public class SolomonMonitoringDbQueueJobSchedulerTest extends IntegrationTest {

    private SolomonMonitoringDbQueueJobScheduler executor;

    @Autowired
    private SolomonPushClient solomonPushClient;

    @Autowired
    private DbQueueService dbQueueService;

    @Autowired
    private DateTimeService dateTimeService;

    @BeforeEach
    public void init() {
        executor = new SolomonMonitoringDbQueueJobScheduler(solomonPushClient, dbQueueService, dateTimeService);
    }

    @Test
    @DatabaseSetup("classpath:scheduler/db-queue-solomon-monitoring/before.xml")
    public void dbQueueSolomonMonitoringCreatedCorrect() throws IOException, JSONException {
        String expectedMonitoringToSolomon = FileContentUtils.getFileContent(
            "scheduler/db-queue-solomon-monitoring/monitoring-to-solomon.json");
        executor.calculateAndSend();
        ArgumentCaptor<String> actualMonitoringToSolomonCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(solomonPushClient).push(actualMonitoringToSolomonCaptor.capture());
        String actualMonitoringToSolomon = actualMonitoringToSolomonCaptor.getValue();
        JSONAssert.assertEquals(expectedMonitoringToSolomon, actualMonitoringToSolomon, JSONCompareMode.NON_EXTENSIBLE);
    }
}
