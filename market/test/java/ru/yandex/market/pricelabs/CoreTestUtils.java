package ru.yandex.market.pricelabs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.opentest4j.AssertionFailedError;

import ru.yandex.common.util.csv.CSVProcessor;
import ru.yandex.common.util.csv.CSVRowMapper;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.market.pricelabs.bindings.csv.CSVMapper;
import ru.yandex.market.pricelabs.misc.PricelabsRuntimeException;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.yt.binding.YTBinder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class CoreTestUtils {

    private CoreTestUtils() {
        //
    }

    public static <T> List<T> readCsv(Class<T> clazz, String resource) {
        return readCsv(CSVMapper.mapper(clazz), resource);
    }

    public static <T> List<T> readCsv(CSVMapper<T> mapper, String resource) {
        return readCsv(mapper.csvRowMapper(), resource);
    }

    private static <T> List<T> readCsv(CSVRowMapper<T> mapper, String resource) {
        try (InputStream stream = new BufferedInputStream(Utils.getResourceStream(resource))) {
            return readCsv(mapper, stream, Utils.emptyConsumer());
        } catch (IOException e) {
            throw new PricelabsRuntimeException("Unexpected exception when reading " + resource, e);
        }
    }

    private static <T> List<T> readCsv(CSVRowMapper<T> mapper, InputStream stream, Consumer<CSVProcessor> init)
            throws IOException {
        CSVProcessor csv = new CSVProcessor(stream);
        init.accept(csv);
        return csv.process(mapper);
    }

    public static <T> void compare(List<T> expect, List<T> actual) {
        if (expect.size() != actual.size()) {
            printList(actual);
            assertEquals(expect.size(), actual.size());
        }

        for (int i = 0; i < expect.size(); i++) {
            T objectExpect = expect.get(i);
            T objectActual = actual.get(i);

            try {
                assertEquals(objectExpect, objectActual, "Invalid row " + i);
            } catch (AssertionFailedError e) {
                printList(actual);

                log.info("Expected: {}", objectExpect);
                log.info("  Actual: {}", objectActual);
                throw e;
            }
        }
        log.info("Expected and actual are same ({} in total)", expect.size());
    }

    public static Runnable emptyRunnable() {
        return () -> {

        };
    }

    public static Object[][] merge(Object[][] arrays, Object... withAllOf) {
        List<Object[]> result = new ArrayList<>(arrays.length * withAllOf.length);
        for (var value : withAllOf) {
            for (Object[] array : arrays) {
                result.add(ArrayUtils.add(array, value));
            }
        }
        return result.toArray(new Object[][]{});
    }

    @SuppressWarnings("unchecked")
    private static <T> void printList(List<T> actual) {
        try {
            String text;
            if (Utils.isEmpty(actual)) {
                text = "";
            } else {
                var first = actual.get(0);
                if (first.getClass().getAnnotation(YTreeObject.class) != null) {
                    YTBinder<T> binder = YTBinder.getBinder((Class<T>) first.getClass());
                    text = Utils.toYsonString(binder, actual);
                } else {
                    text = actual.toString();
                }
            }
            log.info("  Actual:\n{}", text);
        } catch (Exception e) {
            log.error("Cannot render actual object", e);
        }
    }
}
