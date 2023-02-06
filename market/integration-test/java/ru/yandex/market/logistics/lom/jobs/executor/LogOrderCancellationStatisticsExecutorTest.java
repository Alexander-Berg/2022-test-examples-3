package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;
import java.time.ZoneId;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.mockito.Mockito.mock;

@DatabaseSetup("/jobs/executor/logOrderCancellationStatisticsExecutor/log_order_cancellation_statistics.xml")
@DisplayName("Тесты логирования статистики заявок на отмену заказа")
@ParametersAreNonnullByDefault
class LogOrderCancellationStatisticsExecutorTest extends AbstractContextualTest {

    @Autowired
    private LogOrderCancellationStatisticsExecutor logOrderCancellationStatisticsExecutor;

    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2020-11-02T14:00:00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Проверка записи статистики в лог")
    void logStatistics() {
        logOrderCancellationStatisticsExecutor.doJob(jobContext);

        softly.assertThat(backLogCaptor.getResults().size()).isEqualTo(4);
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=REQUEST_METRICS\t" +
                "payload=CancellationOrderRequest/1/REQUEST_METRICS/PROCESSING\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=ORDER_CANCELLATION_STATS\t" +
                "extra_keys=updatedTime,requestType,requestId,createdTime,status\t" +
                "extra_values=1604203200,CancellationOrderRequest,1,1604203200,PROCESSING\n",
            "level=INFO\t" +
                "format=plain\t" +
                "code=REQUEST_METRICS\t" +
                "payload=CancellationSegmentRequest/1/REQUEST_METRICS/PROCESSING\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=ORDER_CANCELLATION_STATS\t" +
                "extra_keys=updatedTime," +
                "requestType,requestId,createdTime,partnerId,status\t" +
                "extra_values=1604208600,CancellationSegmentRequest,1,1604206800,11,PROCESSING\n",
            "level=INFO\t" +
                "format=plain\tcode=REQUEST_METRICS\t" +
                "payload=CancellationSegmentRequest/2/REQUEST_METRICS/PROCESSING\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=ORDER_CANCELLATION_STATS\t" +
                "extra_keys=updatedTime,requestType,requestId,createdTime,partnerId,status\t" +
                "extra_values=1604212200,CancellationSegmentRequest,2,1604210400,12,PROCESSING\n"
        );
    }
}
