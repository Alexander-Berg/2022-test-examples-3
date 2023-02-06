package ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDateTime;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonList;
import static org.springframework.util.CollectionUtils.toMultiValueMap;

class FilterTest extends UrlVariablesProducerTest {

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                "emptyFilter",
                new Filter(),
                wrapInMultiValueMap(Collections.emptyMap())
            ),
            Arguments.of(
                "itemId",
                new Filter(singletonList("ABC123"), null, null),
                wrapInMultiValueMap(of("filter[item_id]", "ABC123"))
            ),
            Arguments.of(
                "twoItemIds",
                new Filter(Arrays.asList("ABC123", "ABC321"), null, null),
                toMultiValueMap(of("filter[item_id][]", Arrays.asList("ABC123", "ABC321")))
            ),
            Arguments.of(
                "name",
                new Filter(null, "t-shirt", null),
                wrapInMultiValueMap(of("filter[name]", "t-shirt"))
            ),
            Arguments.of(
                "count",
                new Filter(null, null, 10),
                wrapInMultiValueMap(of("filter[cnt]", "10"))
            ),
            Arguments.of(
                "dateStockUpdate",
                new Filter(null, null, null)
                    .setDateStockUpdate(MarschrouteDate.create("10.10.2017")),
                wrapInMultiValueMap(of("filter[date_stock_update]", "10.10.2017"))
            ),
            Arguments.of(
                "dateStockUpdateRange date",
                new Filter(null, null, null)
                    .setDateStockUpdateRange(
                        MarschrouteDate.create("10.10.2017"),
                        MarschrouteDate.create("11.10.2017")
                    ),
                wrapInMultiValueMap(of("filter[date_stock_update]", "10.10.2017 - 11.10.2017"))
            ),
            Arguments.of(
                "dateStockUpdateRange datetime",
                new Filter(null, null, null)
                    .setDateStockUpdateRange(
                        MarschrouteDateTime.create("10.10.2017 12:00:00"),
                        MarschrouteDateTime.create("11.10.2017 12:59:59")
                    ),
                wrapInMultiValueMap(of("filter[date_stock_update]",
                    "10.10.2017 12:00:00 - 11.10.2017 12:59:59"))
            ),
            Arguments.of(
                "itemId, name",
                new Filter(singletonList("ABC123"), "t-shirt", null),
                wrapInMultiValueMap(of(
                    "filter[item_id]", "ABC123",
                    "filter[name]", "t-shirt")
                )
            ),
            Arguments.of(
                "itemId, name, count",
                new Filter(singletonList("ABC123"), "t-shirt", 10),
                wrapInMultiValueMap(of(
                    "filter[item_id]", "ABC123",
                    "filter[name]", "t-shirt",
                    "filter[cnt]", "10"
                ))
            )
        );
    }
}
