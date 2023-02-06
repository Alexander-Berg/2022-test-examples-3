package ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock;

import java.util.Collections;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.provider.Arguments;

import static ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.SortArgument.CNT;
import static ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.SortArgument.STOCK_A;
import static ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.SortArgument.STOCK_D;
import static ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.SortOrder.ASC;
import static ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.SortOrder.DESC;

class SortingTest extends UrlVariablesProducerTest {
    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                "empty",
                new Sorting(),
                wrapInMultiValueMap(Collections.emptyMap())
            ),
            Arguments.of(
                "CNT, DESC",
                new Sorting(ImmutableMap.of(CNT, DESC)),
                wrapInMultiValueMap(ImmutableMap.of("sort[cnt]", "desc"))
            ),
            Arguments.of(
                "CNT, ASC",
                new Sorting(ImmutableMap.of(CNT, ASC)),
                wrapInMultiValueMap(ImmutableMap.of("sort[cnt]", "asc"))
            ),
            Arguments.of(
                "STOCK_A, ASC, STOCK_D, DESC",
                new Sorting(ImmutableMap.of(STOCK_A, ASC, STOCK_D, DESC)),
                wrapInMultiValueMap(ImmutableMap.of("sort[stock_a]", "asc", "sort[stock_d]", "desc"))
            )
        );
    }
}
