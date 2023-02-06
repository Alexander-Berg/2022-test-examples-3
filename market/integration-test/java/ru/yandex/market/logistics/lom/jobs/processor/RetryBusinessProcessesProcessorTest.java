package ru.yandex.market.logistics.lom.jobs.processor;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.RetryBusinessProcessesPayload;
import ru.yandex.market.logistics.lom.service.businessProcess.AbstractBusinessProcessStateYdbServiceTest;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

@ParametersAreNonnullByDefault
@DisplayName("Перевыставление бизнес-процессов")
@DatabaseSetup("/jobs/processor/retry_business_processes/before/setup.xml")
class RetryBusinessProcessesProcessorTest extends AbstractBusinessProcessStateYdbServiceTest {

    @Autowired
    private RetryBusinessProcessesProcessor retryBusinessProcessesProcessor;

    @Test
    @JpaQueriesCount(0)
    @DisplayName("Пустой пейлоад с идентификаторами")
    void emptyProcessesIds() {
        softly.assertThatCode(
                () -> processPayload(List.of())
            )
            .doesNotThrowAnyException();
    }

    @Test
    @JpaQueriesCount(13)
    @DisplayName("Процессы успешно перевыставлены")
    @ExpectedDatabase(
        value = "/jobs/processor/retry_business_processes/after/retried.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sucessRetryProcesses() {
        insertProcessToYdb(2L, BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_FAILED);
        clock.setFixed(Instant.parse("2021-08-30T17:00:00.00Z"), ZoneOffset.UTC);

        processPayload(List.of(1L, 2L));
    }

    @Test
    @JpaQueriesCount(1)
    @DisplayName("Перевыставление несуществующих процессов")
    @ExpectedDatabase(
        value = "/jobs/processor/retry_business_processes/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void notExistingProcesses() {
        softly.assertThatCode(
                () -> processPayload(List.of(123456789L, 987654321L))
            )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [BUSINESS_PROCESS] with id [123456789]");
    }

    @Test
    @JpaQueriesCount(3)
    @DisplayName("Процессы не удовлетворяют условиям перевыставления")
    @ExpectedDatabase(
        value = "/jobs/processor/retry_business_processes/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void incorrectForRetryProcesses() {
        insertProcessToYdb(2L, BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_FAILED);

        processPayload(List.of(2L));
    }

    private void processPayload(List<Long> processIds) {
        retryBusinessProcessesProcessor.processPayload(
            new RetryBusinessProcessesPayload("abc", processIds)
        );
    }
}
