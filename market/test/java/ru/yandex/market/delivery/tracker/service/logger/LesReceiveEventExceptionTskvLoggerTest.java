package ru.yandex.market.delivery.tracker.service.logger;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LesReceiveEventExceptionTskvLoggerTest {

    @Mock
    private TskvLogger tskvLogger;

    @InjectMocks
    private LesReceiveEventExceptionTskvLogger logger;

    @Test
    void logException() {
        Throwable cause = new RuntimeException("cause ex").fillInStackTrace();
        Throwable t = new RuntimeException("exception", cause).fillInStackTrace();

        logger.logException(t);

        Map<String, String> expectedTskvMap = ImmutableMap.<String, String>builder()
            .put("message", "java.lang.RuntimeException: exception")
            .put("cause", "java.lang.RuntimeException: cause ex")
            .put("stackTrace", ExceptionUtils.getStackTrace(t))
            .build();

        verify(tskvLogger).log(expectedTskvMap);
    }
}
