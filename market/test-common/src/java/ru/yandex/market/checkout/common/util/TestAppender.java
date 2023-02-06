package ru.yandex.market.checkout.common.util;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class TestAppender extends AppenderBase<ILoggingEvent> {

    private final List<ILoggingEvent> log = new ArrayList<>();

    @Override
    public synchronized void doAppend(ILoggingEvent eventObject) {
        log.add(eventObject);
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        log.add(eventObject);
    }

    public List<ILoggingEvent> getLog() {
        return new ArrayList<>(log);
    }
}
