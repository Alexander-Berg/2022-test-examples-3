package ru.yandex.market.pricelabs.exports;

import java.nio.charset.Charset;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.market.pricelabs.exports.params.CsvParameters;
import ru.yandex.market.pricelabs.misc.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ExporterTestContextCsv<T> implements ExporterTestContext<T> {
    private final ListExporter exporter = CsvListExporter.getInstance();

    private final Charset encoding;

    public ExporterTestContextCsv() {
        this(CsvParameters.WIN1251);
    }

    public ExporterTestContextCsv(@NonNull Charset encoding) {
        this.encoding = encoding;
    }

    @Override
    public ListExporter exporter() {
        return exporter;
    }

    @Override
    public void verify(String expectResource, byte[] actualBytes) {
        var actual = new String(actualBytes, encoding);
        log.info("Actual CSV: {}", actual);
        assertEquals(Utils.readResource(expectResource).trim(), actual.trim());
    }

    @Override
    public String toString() {
        return exporter().getFileType().toString();
    }
}
