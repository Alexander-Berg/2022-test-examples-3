package ru.yandex.market.mbo.flume.channel;

import java.util.HashMap;
import java.util.Map;

import org.apache.flume.event.SimpleEvent;

/**
 * @author moskovkin@yandex-team.ru
 * @since 16.08.17
 */
public final class SimpleEventBuilder {
    private Map<String, String> headers = new HashMap<>();
    private byte[] body;

    private SimpleEventBuilder() {
    }

    public static SimpleEventBuilder aSimpleEvent() {
        return new SimpleEventBuilder();
    }

    public SimpleEventBuilder addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public SimpleEventBuilder setBody(byte[] body) {
        this.body = body;
        return this;
    }

    public SimpleEvent build() {
        SimpleEvent simpleEvent = new SimpleEvent();
        simpleEvent.setHeaders(headers);
        simpleEvent.setBody(body);
        return simpleEvent;
    }
}
