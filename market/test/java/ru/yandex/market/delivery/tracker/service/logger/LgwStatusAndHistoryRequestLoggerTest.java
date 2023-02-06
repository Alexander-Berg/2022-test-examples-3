package ru.yandex.market.delivery.tracker.service.logger;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LgwStatusAndHistoryRequestLoggerTest {

    private static final String FIXED_TIME_STR = "2021-10-11 12:13:14.000";
    private static final long PARTNER_ID = 124125;

    private TskvLogger tskvLogger;
    private LgwStatusAndHistoryRequestLogger lgwStatusAndHistoryRequestLogger;

    @BeforeEach
    void setUp() {
        tskvLogger = mock(TskvLogger.class);
        lgwStatusAndHistoryRequestLogger = new LgwStatusAndHistoryRequestLogger(
            tskvLogger,
            Clock.fixed(Instant.now(), ZoneId.systemDefault())
        );
        when(tskvLogger.formatDate(any())).thenReturn(FIXED_TIME_STR);
    }

    @Test
    void logRequestSuccess() {
        int expected = 141525;
        int actual = logRequest(() -> expected);
        assertThat(actual).isEqualTo(expected);
        verifyTskvLogger(true);
    }

    @Test
    void logRequestFail() {
        String msg = "supplier exception test message";
        assertThatThrownBy(() ->
            logRequest(
                () -> {
                    throw new RuntimeException(msg);
                }
            )
        ).hasMessage(msg);
        verifyTskvLogger(false);
    }

    private <T> T logRequest(Supplier<T> supplier) {
        return lgwStatusAndHistoryRequestLogger.logRequest(
            RequestType.INBOUND_STATUS_HISTORY,
            ApiVersion.DS,
            PARTNER_ID,
            supplier
        );
    }

    private void verifyTskvLogger(boolean success) {
        verify(tskvLogger).log(
            ImmutableMap.<String, String>builder()
                .put("requestType", "INBOUND_STATUS_HISTORY")
                .put("apiVersion", "DS")
                .put("partnerId", String.valueOf(PARTNER_ID))
                .put("startTs", FIXED_TIME_STR)
                .put("endTs", FIXED_TIME_STR)
                .put("success", String.valueOf(success))
                .build()
        );
    }
}
