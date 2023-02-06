package ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock;

import java.util.Collections;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.provider.Arguments;

import static java.util.Collections.singletonList;
import static ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.SortArgument.CNT;
import static ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.SortOrder.DESC;

class MarschrouteGetStocksRequestTest extends UrlVariablesProducerTest {

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                "emptyRequest",
                new MarschrouteProductsRequest(),
                wrapInMultiValueMap(Collections.emptyMap())
            ),
            Arguments.of(
                "pagination",
                new MarschrouteProductsRequest(
                    new Pagination(10, null),
                    null,
                    null
                ),
                wrapInMultiValueMap(ImmutableMap.of("limit", "10"))
            ),
            Arguments.of(
                "pagination, itemId",
                new MarschrouteProductsRequest(
                    new Pagination(10, null),
                    new Filter(singletonList("ABC123"), null, null),
                    null
                ),
                wrapInMultiValueMap(ImmutableMap.of("limit", "10",
                    "filter[item_id]", "ABC123"
                ))
            ),
            Arguments.of(
                "pagination, itemId, sorting",
                new MarschrouteProductsRequest(
                    new Pagination(10, null),
                    new Filter(singletonList("ABC123"), null, null),
                    new Sorting(ImmutableMap.of(CNT, DESC))
                ),
                wrapInMultiValueMap(ImmutableMap.of("limit", "10",
                    "filter[item_id]", "ABC123",
                    "sort[cnt]", "desc"
                ))
            ),
            Arguments.of(
                "itemId, name",
                new Filter(Collections.singletonList("ABC123"), "t-shirt", null),
                wrapInMultiValueMap(ImmutableMap.of("filter[item_id]", "ABC123",
                    "filter[name]", "t-shirt"))
            ),
            Arguments.of(
                "itemId, name, count",
                new Filter(singletonList("ABC123"), "t-shirt", 10),
                wrapInMultiValueMap(ImmutableMap.of(
                    "filter[item_id]", "ABC123",
                    "filter[name]", "t-shirt",
                    "filter[cnt]", "10"
                ))
            )
        );
    }
}
