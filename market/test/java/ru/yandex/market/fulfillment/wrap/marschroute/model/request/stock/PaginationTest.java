package ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock;

import java.util.Collections;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.provider.Arguments;

class PaginationTest extends UrlVariablesProducerTest {

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                "empty",
                new Pagination(),
                wrapInMultiValueMap(Collections.emptyMap())
            ),
            Arguments.of(
                "limit",
                new Pagination(10, null),
                wrapInMultiValueMap(ImmutableMap.of("limit", "10"))
            ),
            Arguments.of(
                "offset",
                new Pagination(null, 10),
                wrapInMultiValueMap(ImmutableMap.of("offset", "10"))
            ),
            Arguments.of(
                "limit, offset",
                new Pagination(10, 10),
                wrapInMultiValueMap(ImmutableMap.of("limit", "10",
                    "offset", "10"))
            )
        );
    }
}
