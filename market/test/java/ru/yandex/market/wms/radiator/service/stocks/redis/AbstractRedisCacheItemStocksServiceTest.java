package ru.yandex.market.wms.radiator.service.stocks.redis;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractRedisCacheItemStocksServiceTest {

    @Test
    void compose_regular() {
        List<Map.Entry<Long, List<Integer>>> pages = List.of(
                Map.entry(10L, List.of(10, 11, 12, 13, 14, 15, 16, 17, 18, 19)),
                Map.entry(20L, List.of(20, 21, 22, 23))
        );

        assertThat(
                AbstractRedisCacheItemStocksService.compose(pages, 12, 21, Function.identity(), 10),
                is(List.of(12, 13, 14, 15, 16, 17, 18, 19, 20))
        );
    }


    @Test
    void compose_emptyLastPage() {
        List<Map.Entry<Long, List<Integer>>> pages = List.of(
                Map.entry(10L, List.of(10, 11, 12, 13, 14, 15, 16, 17, 18, 19)),
                Map.entry(20L, List.of(20, 21, 22, 23, 24, 25, 26, 27, 28, 29)),
                Map.entry(30L, List.of())
        );

        assertThat(
                AbstractRedisCacheItemStocksService.compose(pages, 12, 31, Function.identity(), 10),
                is(List.of(12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29))
        );
    }


    @Test
    void compose_missingPage() {
        List<Map.Entry<Long, List<Integer>>> pages = List.of(
                Map.entry(10L, List.of(10, 11, 12, 13, 14, 15, 16, 17, 18, 19)),
                Map.entry(30L, List.of(30, 31, 32, 33, 34, 35, 36, 37, 38, 39))
        );

        assertThrows(
                IllegalStateException.class,
                () -> AbstractRedisCacheItemStocksService.compose(pages, 12, 25, Function.identity(), 10)
        );
    }
}
