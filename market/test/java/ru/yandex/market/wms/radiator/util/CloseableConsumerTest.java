package ru.yandex.market.wms.radiator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class CloseableConsumerTest {

    @Test
    void paginatingBy() {
        var log = new ArrayList<Map.Entry<Long, List<String>>>();
        BiConsumer<Long, List<String>> biConsumer = biConsumer(log::add);
        CloseableConsumer<String> sut = CloseableConsumer.paginatingBy(2, biConsumer);

        try (sut) {
            List.of(
                    "value1",
                    "value2",
                    "value3"
            ).forEach(sut);
        }

        assertThat(
                log,
                is(equalTo(
                        List.of(
                                Map.entry(0L, List.of("value1", "value2")),
                                Map.entry(2L, List.of("value3"))
                        )
                ))
        );
    }

    @Test
    void paginatingBy_no_dupes() {
        var log = new ArrayList<Map.Entry<Long, List<String>>>();
        BiConsumer<Long, List<String>> biConsumer = biConsumer(log::add);
        CloseableConsumer<String> sut = CloseableConsumer.paginatingBy(2, s -> s, s -> {}, biConsumer);

        try (sut) {
            List.of(
                    "value1",
                    "value1",
                    "value2",
                    "value3"
            ).forEach(sut);
        }

        assertThat(
                log,
                is(equalTo(
                        List.of(
                                Map.entry(0L, List.of("value1", "value2")),
                                Map.entry(2L, List.of("value3"))
                        )
                ))
        );
    }

    static <U, V> BiConsumer<U, V> biConsumer(Consumer<Map.Entry<U, V>> c) {
        return (u, v) -> c.accept(Map.entry(u, v));
    }
}
