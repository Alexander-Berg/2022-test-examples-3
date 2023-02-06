package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DatabaseSetup("/jobs/executor/checkImportExternalCreation/check_import_external_creation.xml")
class CheckImportExternalCreationExecutorTest extends AbstractContextualTest {

    @Autowired
    private CheckImportExternalCreationExecutor checkImportExternalCreationExecutor;

    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @Test
    @DisplayName("Найдена заявка на самопривоз, которая была отправлена в службу, но не получила ID")
    void foundWithdrawInProcessingStatus() {
        clock.setFixed(Instant.parse("2019-10-25T12:16:00.00Z"), ZoneId.systemDefault());
        checkImportExternalCreationExecutor.doJob(jobContext);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("level=ERROR\t" +
                "format=plain\t" +
                "code=IMPORT_EXTERNAL_CREATION_TIMEOUT_EXPIRED\t" +
                "payload=Withdraw without external ID\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=BUSINESS_SHIPMENT_EVENT\t" +
                "entity_types=shipmentApplication,shipment,partner,platform\t" +
                "entity_values=shipmentApplication:2,shipment:2,partner:48,platform:YANDEX_DELIVERY\n");
    }

    @Test
    @DisplayName("Заявки на самопривоз без ID службы не найдены")
    void notFoundWithdrawInProcessingStatus() {
        clock.setFixed(Instant.parse("2019-10-25T12:14:00.00Z"), ZoneId.systemDefault());

        checkImportExternalCreationExecutor.doJob(jobContext);

        assertThat(backLogCaptor.getResults().toString())
            .doesNotContain("IMPORT_EXTERNAL_CREATION_TIMEOUT_EXPIRED");
    }
}
