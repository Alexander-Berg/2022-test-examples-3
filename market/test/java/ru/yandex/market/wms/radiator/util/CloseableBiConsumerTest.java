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

class CloseableBiConsumerTest {

    @Test
    void paginatingBy() {
        var log = new ArrayList<Map.Entry<Long, List<Map.Entry<String, String>>>>();
        BiConsumer<Long, List<Map.Entry<String, String>>> biConsumer = biConsumer(log::add);
        CloseableBiConsumer<String, String> sut = CloseableBiConsumer.paginatingBy(2, biConsumer);

        try (sut) {
            List.of(
                    Map.entry("key1", "value1"),
                    Map.entry("key2", "value2"),
                    Map.entry("key3", "value3")
            ).forEach(consumer(sut));
        }

        assertThat(
                log,
                is(equalTo(
                        List.of(
                            Map.entry(0L, List.of(Map.entry("key1", "value1"), Map.entry("key2", "value2"))),
                            Map.entry(2L, List.of(Map.entry("key3", "value3")))
                        )
                ))
        );
    }

    static <U, V> BiConsumer<U, V> biConsumer(Consumer<Map.Entry<U, V>> c) {
        return (u, v) -> c.accept(Map.entry(u, v));
    }

    static <U, V> Consumer<Map.Entry<U, V>> consumer(BiConsumer<U, V> c) {
        return e -> c.accept(e.getKey(), e.getValue());
    }
}
