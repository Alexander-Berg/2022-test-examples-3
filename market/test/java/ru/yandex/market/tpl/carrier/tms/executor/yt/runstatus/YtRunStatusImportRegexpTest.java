package ru.yandex.market.tpl.carrier.tms.executor.yt.runstatus;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.tpl.carrier.core.service.yt.runstatus.YtRunStatus;

public class YtRunStatusImportRegexpTest {

    @ParameterizedTest
    @CsvSource({
            "[FTL] 57376,57376",
            "[FTL] 57376/123456,57376",
            "[FTL] 57376    /   123,57376",
    })
    void regexpShouldExtractRunId(String runIdString, long runId) {
        Assertions.assertThat(YtRunStatusImportExecutor.parseRunId(runIdString))
                .isEqualTo(runId);
    }

    @ParameterizedTest
    @CsvSource({
            "[FTL] 57376,",
            "[FTL] 57376/123456,123456",
            "[FTL] 57376    /   123,123",
    })
    void regexpShouldExtractRunItemId(String runIdString, Long runItemId) {
        Optional<Long> runItemIdO = YtRunStatusImportExecutor.parseRunItemId(runIdString);
        if (runItemId == null) {
            Assertions.assertThat(runItemIdO).isEmpty();
        } else {
            Assertions.assertThat(runItemIdO).hasValue(runItemId);
        }

    }

    @ParameterizedTest
    @CsvSource({
            "ПОДТВЕРЖДЕНИЕ ПЕРЕВОЗКИ,CONFIRMED",
            "ПОГРузКА,OUTBOUND"
    })
    void shouldParseRunStatus(String value, YtRunStatus status) {
        Assertions.assertThat(YtRunStatusImportExecutor.parseRunStatus(value))
                .hasValue(status);
    }
}
