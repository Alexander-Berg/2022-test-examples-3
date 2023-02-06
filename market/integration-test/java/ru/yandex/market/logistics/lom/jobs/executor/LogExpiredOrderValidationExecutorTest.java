package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;
import java.time.ZoneId;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

@DatabaseSetup("/jobs/executor/logOrderExpiredValidationStatisticsExecutor/log_order_expired_validation_statistics.xml")
@DisplayName("Тесты логирования статистики количества заказов с истекшим сроком валидации")
@ParametersAreNonnullByDefault
public class LogExpiredOrderValidationExecutorTest extends AbstractContextualTest {

    @Autowired
    private LogExpiredOrderValidationExecutor logExpiredOrderValidationExecutor;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-06-04T12:00:00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Проверка записи статистики в лог")
    void logExpiredValidationOrders() {
        logExpiredOrderValidationExecutor.doJob(null);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "payload=2 orders with expired validation (last update more than 30 minutes ago)\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=ORDER_EXPIRED_VALIDATION_STATS\t" +
                    "extra_keys=amount\t" +
                    "extra_values=2\n"
            );
    }

}
