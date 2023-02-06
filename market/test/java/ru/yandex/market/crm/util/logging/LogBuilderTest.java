package ru.yandex.market.crm.util.logging;

import com.google.common.collect.Range;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LogBuilderTest {

    @Test
    public void appendDuration() {
        String result = LogBuilder.builder("#test")
                .appendDuration()
                .build();

        Assertions.assertTrue(result.matches("#test, DURATION: \\d+, #tm100"));
    }

    @Test
    public void appendExceptionWithNullMessage() {
        Throwable t = new NullPointerException();

        String result = LogBuilder.builder("#test")
                .appendError(t)
                .build();
        Assertions.assertEquals("#test, ERROR: java.lang.NullPointerException", result);
    }

    @Test
    public void appendKeyValue() {
        String result = LogBuilder.builder("#test")
                .append("key", "text")
                .build();
        Assertions.assertEquals("#test, key: text", result);
    }

    @Test
    public void appendKeyValueNull() {
        String result = LogBuilder.builder("#test")
                .append("key", null)
                .build();
        Assertions.assertEquals("#test", result, "Не должны добавлять информацию, если значение null");
    }

    @Test
    public void appendKeyValueNullObject() {
        String result = LogBuilder.builder("#test")
                .append("key", (Object) null)
                .build();
        Assertions.assertEquals("#test", result, "Не должны добавлять информацию, если значение null");
    }

    @Test
    public void appendKeyValueObject() {
        String result = LogBuilder.builder("#test")
                .append("key", Range.closed(2, 3))
                .build();
        Assertions.assertEquals("#test, key: [2..3]", result, "Должны у значения вызвать toString()");
    }

    @Test
    public void appendKeyValueTemplate() {
        String result = LogBuilder.builder("#test")
                .append("key", "%s", "text")
                .build();
        Assertions.assertEquals("#test, key: text", result);
    }

    @Test
    public void appendNullException() {
        String result = LogBuilder.builder("#test")
                .appendError(null)
                .build();
        Assertions.assertEquals("#test", result);
    }

    @Test
    public void appendRootException() {
        Throwable root = new NullPointerException("error 1");
        Throwable t = new RuntimeException("error 2", root);

        String result = LogBuilder.builder("#test")
                .appendError(t)
                .build();
        Assertions.assertEquals("#test, ERROR: java.lang.RuntimeException, ERROR_MSG: \"error 2\", PRIMARY_EXCEPTION:" +
                " " +
                "java.lang.NullPointerException, PRIMARY_MSG: \"error 1\"", result);
    }

    @Test
    public void appendRootExceptionWithNullMesage() {
        Throwable root = new NullPointerException();
        Throwable t = new RuntimeException("error 2", root);

        String result = LogBuilder.builder("#test")
                .appendError(t)
                .build();
        Assertions.assertEquals("#test, ERROR: java.lang.RuntimeException, ERROR_MSG: \"error 2\", PRIMARY_EXCEPTION:" +
                " " +
                "java.lang.NullPointerException", result);
    }

    @Test
    public void appendSimpleText() {
        String result = LogBuilder.builder("#test")
                .append("text")
                .build();
        Assertions.assertEquals("#test, text", result);
    }

    @Test
    public void appendTemplate() {
        String result = LogBuilder.builder("#test")
                .appendTemplate("%s", "text")
                .build();
        Assertions.assertEquals("#test, text", result);
    }

    @Test
    public void empty() {
        String result = LogBuilder.builder("#test").build();
        Assertions.assertEquals("#test", result);
    }

    @Test
    public void doubleAppendOfTheSameKey() {
        String result = LogBuilder.builder("#test")
                .append("same-key", "value1")
                .append("same-key", "value2")
                .build();
        Assertions.assertEquals("#test, same-key: value1, same-key: value2", result);
    }
}
