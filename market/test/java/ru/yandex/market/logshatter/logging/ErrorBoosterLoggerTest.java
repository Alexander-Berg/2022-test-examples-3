package ru.yandex.market.logshatter.logging;

import org.json.JSONException;
import org.junit.Test;

import ru.yandex.market.health.configs.logshatter.config.ConfigValidationException;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.output.OutputQueueTest;
import ru.yandex.market.logshatter.reader.SourceContext;

import static org.junit.Assert.assertEquals;

public class ErrorBoosterLoggerTest {
    private final ErrorBoosterLogger errorBoosterLogger = new ErrorBoosterLoggerForTest(true, "test");

    @Test
    public void testSimpleError() throws JSONException {
        String logLine = "{\"stack\":\"java.lang.RuntimeException\\n\\tat class0.method0(type0:0)\\n\\tat " +
            "class1.method1(type1:1)\\n\",\"level\":\"error\",\"sourceType\":\"java.lang.RuntimeException\"," +
            "\"project\":\"test\",\"language\":\"java\",\"message\":\"java.lang.RuntimeException\"," +
            "\"timestamp\":1603907379852}";

        assertEquals(logLine, errorBoosterLogger.createLogLine(getException()));
    }

    @Test
    public void testErrorWithSourceContext() throws JSONException, ConfigValidationException {
        String logLine = "{\"stack\":\"java.lang.RuntimeException\\n\\tat class0.method0(type0:0)\\n\\tat " +
            "class1.method1(type1:1)\\n\",\"level\":\"error\",\"sourceType\":\"java.lang.RuntimeException\"," +
            "\"additional\":{\"parser\":\"TestParser\",\"line\":\"big lb line\"," +
            "\"sourceKeyId\":\"null-null-null-null\",\"config\":\"testConfig\",\"table\":\"my.table\"}," +
            "\"project\":\"test\",\"language\":\"java\",\"message\":\"java.lang.RuntimeException\"," +
            "\"timestamp\":1603907379852}";

        LogShatterConfig logShatterConfig = OutputQueueTest.createLogshatterConfig("testConfig");
        SourceContext sourceContext = OutputQueueTest.createEmptySourceContext(logShatterConfig);
        ErrorSample sample = new ErrorSample(getException(), "big lb line");

        assertEquals(logLine, errorBoosterLogger.createLogLine(sample, sourceContext));
    }

    private Exception getException() {
        Exception exception = new RuntimeException();
        StackTraceElement[] y = new StackTraceElement[2];
        y[0] = new StackTraceElement("class0", "method0", "type0", 0);
        y[1] = new StackTraceElement("class1", "method1", "type1", 1);
        exception.setStackTrace(y);

        return exception;
    }

    private static class ErrorBoosterLoggerForTest extends ErrorBoosterLogger {
        ErrorBoosterLoggerForTest(Boolean enabled, String project) {
            super(enabled, project);
        }

        @Override
        long getCurrentTimeMillis() {
            return 1603907379852L;
        }
    }
}
