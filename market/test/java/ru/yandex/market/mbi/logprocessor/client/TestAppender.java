package ru.yandex.market.mbi.logprocessor.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

/**
 * Аппендер для unit тестов.
 */
public class TestAppender extends AbstractAppender {
    private List<String> messages = new ArrayList<>();

    public TestAppender() {
        super("testAppender", null, null, false, null);
    }

    @Override
    public void append(LogEvent event) {
        messages.add(event.getMessage().toString());
    }

    public List<String> getMessages() {
        return messages;
    }
}
