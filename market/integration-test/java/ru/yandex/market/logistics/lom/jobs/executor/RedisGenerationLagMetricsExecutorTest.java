package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.service.redis.AbstractRedisTest;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;

import static org.mockito.Mockito.verify;

@DisplayName("Тест логирования времени отставания поколений Redis от данных LMS")
class RedisGenerationLagMetricsExecutorTest extends AbstractRedisTest {

    private static final String LOG_MESSAGE_TEMPLATE =
        "level=INFO\t" +
            "format=plain\t" +
            "code=%s\t" +
            "payload=Redis lag from LMS data: %d\t" +
            "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
            "tags=REDIS_DATA_STATS\t" +
            "extra_keys=lag\t" +
            "extra_values=%d";

    private static final String ERROR_MESSAGE_PREFIX_TEMPLATE =
        "level=ERROR\tformat=json-exception\tcode=%s\tpayload={%s";

    private static final String READABLE_VERSION = "2021-11-15T11:15:00Z";
    private static final String UNPARSABLE_VERSION = "unparsable";

    private static final String YT_STATISTICS_CODE = "REDIS_YT_GENERATION_LAG";

    @Autowired
    private RedisGenerationLagMetricsExecutor executor;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
        clock.setFixed(Instant.parse("2021-11-15T11:16:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Логирование отставаний поколений")
    void logRedisLag(String ytVersion) {
        Mockito.doReturn(ytVersion).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        executor.doJob(null);

        String backLog = backLogCaptor.getResults().toString();

        if (READABLE_VERSION.equals(ytVersion)) {
            verifyLoggedLag(backLog, YT_STATISTICS_CODE, 60000);
        } else {
            verifyLoggedLag(backLog, YT_STATISTICS_CODE, Long.MAX_VALUE);
        }

        if (ytVersion == null) {
            verifyLogNotFoundError(backLog, YT_STATISTICS_CODE);
        }

        if (UNPARSABLE_VERSION.equals(ytVersion)) {
            verifyLogParseError(backLog, YT_STATISTICS_CODE);
        }

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
    }

    @Nonnull
    private static Stream<Arguments> logRedisLag() {
        return Stream.of(
            READABLE_VERSION,
            UNPARSABLE_VERSION,
            null
        ).map(Arguments::of);
    }

    private void verifyLogNotFoundError(String backLog, String statisticsCode) {
        verifyLoggedError(
            backLog,
            statisticsCode,
            "\\\"eventMessage\\\":\\\"Exception during parsing of version 0\\\""
        );
    }

    private void verifyLogParseError(String backLog, String statisticsCode) {
        verifyLoggedError(
            backLog,
            statisticsCode,
            String.format("\\\"eventMessage\\\":\\\"Exception during parsing of version %s\\\"", UNPARSABLE_VERSION)
        );
    }

    private void verifyLoggedError(String backLog, String statisticsCode, String exceptionMessage) {
        softly.assertThat(backLog).contains(String.format(
            ERROR_MESSAGE_PREFIX_TEMPLATE,
            statisticsCode,
            exceptionMessage
        ));
    }

    private void verifyLoggedLag(String backLog, String statisticsCode, long lagTimeMillis) {
        softly.assertThat(backLog).contains(String.format(
            LOG_MESSAGE_TEMPLATE,
            statisticsCode,
            lagTimeMillis,
            lagTimeMillis
        ));
    }
}
