package ru.yandex.travel.commons.logging.ydb;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.MessageFormatMessage;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class YdbLogRecordFactoryTest {
    @Test
    public void testMessageId() {
        YdbLogRecordFactory subject = new YdbLogRecordFactory("test.com");
        // nullable fields test
        assertThat(subject.messageId(new Log4jLogEvent(null, null, null, null,
                new MessageFormatMessage("message", "asd"), null, null)))
                .isEqualTo("bc14799be883a001f76f72b83170beb7");

        // fully initialized
        assertThat(subject.messageId(new Log4jLogEvent("Logger", MarkerManager.getMarker("Marker"),
                "some.LoggerClass", Level.INFO, new MessageFormatMessage("message", new RuntimeException("a")),
                null, new RuntimeException("b"))))
                .isEqualTo("e8fffbd6f62c5d5be158cba4a6dc3039");
    }
}
