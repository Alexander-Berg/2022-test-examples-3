package ru.yandex.market.logistics.lom.jobs.executor;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.mockito.Mockito.mock;

@DatabaseSetup("/controller/order/search/orders.xml")
@DatabaseSetup(
    value = "/jobs/executor/logStatistics/log_statistics.xml",
    type = DatabaseOperation.INSERT
)
@DatabaseSetup(
    value = "/jobs/executor/checkOrderConfirmation/check_order_confirmation.xml",
    type = DatabaseOperation.INSERT
)
@DisplayName("Тесты логирования статистики")
@ParametersAreNonnullByDefault
class LogStatisticsExecutorTest extends AbstractContextualTest {

    @Autowired
    private LogStatisticsExecutor logStatisticsExecutor;

    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @Test
    @DisplayName("Проверка записи статистики в лог")
    void logStatistics() {
        logStatisticsExecutor.doJob(jobContext);

        String log = backLogCaptor.getResults().toString();
        assertShipmentApplicationByStatus(log);
        assertRegistryCountByStatus(log);
    }

    private void assertShipmentApplicationByStatus(String log) {
        softly.assertThat(log)
            .contains("level=INFO\t" +
                "format=plain\t" +
                "code=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "payload=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,label\t" +
                "extra_values=1,REGISTRY_SENT\n")
            .contains("level=INFO\t" +
                "format=plain\t" +
                "code=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "payload=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,label\t" +
                "extra_values=2,CANCELLED\n")
            .contains("level=INFO\t" +
                "format=plain\t" +
                "code=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "payload=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,label\t" +
                "extra_values=1,CREATED\n")
            .contains("level=INFO\t" +
                "format=plain\t" +
                "code=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "payload=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,label\t" +
                "extra_values=1,DELIVERY_SERVICE_PROCESSING\n")
            .contains("level=INFO\t" +
                "format=plain\t" +
                "code=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "payload=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,label\t" +
                "extra_values=2,ERROR\n")
            .contains("level=INFO\t" +
                "format=plain\t" +
                "code=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "payload=COUNT_SHIPMENT_APPLICATION_BY_STATUS\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,label\t" +
                "extra_values=1,NEW\n");
    }

    private void assertRegistryCountByStatus(String log) {
        softly.assertThat(log)
            .contains("level=INFO\t" +
                "format=plain\t" +
                "code=REGISTRY_COUNT_BY_STATUS\t" +
                "payload=REGISTRY_COUNT_BY_STATUS\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,label\t" +
                "extra_values=5,CREATED\n")
            .contains("level=INFO\t" +
                "format=plain\t" +
                "code=REGISTRY_COUNT_BY_STATUS\t" +
                "payload=REGISTRY_COUNT_BY_STATUS\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,label\t" +
                "extra_values=1,ERROR\n")
            .contains("level=INFO\t" +
                "format=plain\t" +
                "code=REGISTRY_COUNT_BY_STATUS\t" +
                "payload=REGISTRY_COUNT_BY_STATUS\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,label\t" +
                "extra_values=2,PROCESSING\n");
    }
}
