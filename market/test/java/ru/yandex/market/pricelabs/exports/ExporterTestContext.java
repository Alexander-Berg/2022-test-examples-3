package ru.yandex.market.pricelabs.exports;

public interface ExporterTestContext<T> {

    ListExporter exporter();

    void verify(String expectResource, byte[] actualBytes);
}
