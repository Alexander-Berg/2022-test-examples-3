package ru.yandex.market.http.util.listener;

import ru.yandex.market.http.listeners.RequestProcessorEventListener;

/**
 * @author dimkarp93
 */
public class TestRequestProcessorEventListener implements RequestProcessorEventListener, AutoCloseable {
    public int startCounter;
    public int contentCounter;

    public TestRequestProcessorEventListener() {
        reset();
    }

    @Override
    public Object onStart() {
        return ++startCounter;
    }

    @Override
    public void onContent(Object context, byte[] body) {
        ++contentCounter;
    }

    public void reset() {
        startCounter = 0;
        contentCounter = 0;
    }

    @Override
    public void close() throws Exception {
        reset();
    }
}
