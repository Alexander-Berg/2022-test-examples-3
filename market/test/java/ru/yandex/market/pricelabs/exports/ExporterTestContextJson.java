package ru.yandex.market.pricelabs.exports;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.market.pricelabs.CoreTestUtils;
import ru.yandex.market.pricelabs.misc.Utils;

@Slf4j
public class ExporterTestContextJson<T> implements ExporterTestContext<T> {

    private final ListExporter exporter = JsonListExporter.getInstance();
    private final ObjectMapper mapper = Utils.getJsonMapper();
    private final Class<T> clazz;

    public ExporterTestContextJson(@NonNull Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public ListExporter exporter() {
        return exporter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void verify(String expectResource, byte[] actualBytes) {
        log.info("Json: {}", new String(actualBytes, StandardCharsets.UTF_8));

        List<T> expect;
        List<T> actual;
        try {
            try (var stream = Utils.getResourceStream(expectResource)) {
                expect = (List<T>) mapper.readerFor(clazz)
                        .readValues(stream)
                        .readAll();
            }
            actual = (List<T>) mapper.readerFor(clazz)
                    .readValues(new ByteArrayInputStream(actualBytes))
                    .readAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CoreTestUtils.compare(expect, actual);
    }

    @Override
    public String toString() {
        return exporter().getFileType().toString();
    }

}
