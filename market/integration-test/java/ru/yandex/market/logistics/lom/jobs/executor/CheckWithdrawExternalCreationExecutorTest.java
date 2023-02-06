package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DatabaseSetup("/jobs/executor/checkWithdrawExternalCreation/check_withdraw_external_creation.xml")
class CheckWithdrawExternalCreationExecutorTest extends AbstractContextualTest {

    @Autowired
    private CheckWithdrawExternalCreationExecutor checkWithdrawExternalCreationExecutor;

    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @Test
    @DisplayName("Найдена заявка на забор, которая была отправлена в службу, но не получила ID")
    void foundWithdrawInProcessingStatus() {
        clock.setFixed(
            LocalDateTime.parse("2019-10-25T21:15").toInstant(ZoneOffset.of("+03:00")),
            DateTimeUtils.MOSCOW_ZONE
        );

        checkWithdrawExternalCreationExecutor.doJob(jobContext);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("level=ERROR\t" +
                "format=plain\t" +
                "code=WITHDRAW_EXTERNAL_CREATION_TIMEOUT_EXPIRED\t" +
                "payload=Withdraw without external ID\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=BUSINESS_SHIPMENT_EVENT\t" +
                "entity_types=shipmentApplication,shipment,partner,platform\t" +
                "entity_values=shipmentApplication:2,shipment:2,partner:48,platform:YANDEX_DELIVERY\n");
    }

    @Test
    @DisplayName("Заявки на забор без ID службы не найдены")
    void notFoundWithdrawInProcessingStatus() {
        clock.setFixed(
            LocalDateTime.parse("2019-10-26T21:15").toInstant(ZoneOffset.of("+03:00")),
            DateTimeUtils.MOSCOW_ZONE
        );

        checkWithdrawExternalCreationExecutor.doJob(jobContext);

        assertThat(backLogCaptor.getResults().toString())
            .doesNotContain("WITHDRAW_EXTERNAL_CREATION_TIMEOUT_EXPIRED");
    }
}
