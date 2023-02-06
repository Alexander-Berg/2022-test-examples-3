package ru.yandex.market.http.util;

import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author dimkarp93
 */
public class HttpConfig {
    public LinkedHashMap<Predicate<HttpServletRequest>, BiConsumer<HttpServletRequest, HttpServletResponse>>
            processors = new LinkedHashMap<>();

    public BiConsumer<HttpServletRequest, HttpServletResponse> defaultProcessor = (req, resp) -> {};

    public int timeout = -1;

    public HttpConfig addProcessor(Predicate<HttpServletRequest> key,
                                   BiConsumer<HttpServletRequest, HttpServletResponse> value) {
        this.processors.put(key, value);
        return this;
    }

    public HttpConfig defaultProcessor(BiConsumer<HttpServletRequest, HttpServletResponse> defaultProcessor) {
        this.defaultProcessor = defaultProcessor;
        return this;
    }

    public HttpConfig timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

}
