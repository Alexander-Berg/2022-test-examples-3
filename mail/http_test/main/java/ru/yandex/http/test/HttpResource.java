package ru.yandex.http.test;

import java.util.List;

import org.apache.http.protocol.HttpRequestHandler;

public interface HttpResource {
    HttpRequestHandler next();

    int accessCount();

    List<Throwable> exceptions();

    void exception(Throwable t);
}

