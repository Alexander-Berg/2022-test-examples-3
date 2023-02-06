package ru.yandex.market.fulfillment.wrap.marschroute.model.converter.waybill.item;


import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteWaybillItem;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.SkuBox;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaybillItemBoxingEnricherTest {

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                new Item.ItemBuilder(null, null, null).build(),
                new MarschrouteWaybillItem(),
                null
            ),
            Arguments.of(
                new Item.ItemBuilder(null, null, null).setBoxCount(1).build(),
                new MarschrouteWaybillItem(),
                null
            ),
            Arguments.of(
                new Item.ItemBuilder(null, null, null).setBoxCapacity(1).build(),
                new MarschrouteWaybillItem(),
                null
            ),
            Arguments.of(
                new Item.ItemBuilder(null, null, null).setBoxCount(2).build(),
                new MarschrouteWaybillItem().setSkuBox(SkuBox.COMPOSITE_BOX).setCntInBox(2),
                null
            ),
            Arguments.of(
                new Item.ItemBuilder(null, null, null).setBoxCapacity(2).build(),
                new MarschrouteWaybillItem(),
                null
            ),

            Arguments.of(
                new Item.ItemBuilder(null, null, null).setBoxCapacity(0).setBoxCount(0).build(),
                new MarschrouteWaybillItem(),
                null
            ),
            Arguments.of(
                new Item.ItemBuilder(null, null, null).setBoxCapacity(0).setBoxCount(1).build(),
                new MarschrouteWaybillItem(),
                null
            ),
            Arguments.of(
                new Item.ItemBuilder(null, null, null).setBoxCapacity(1).setBoxCount(0).build(),
                new MarschrouteWaybillItem(),
                null
            ),
            Arguments.of(
                new Item.ItemBuilder(null, null, null).setBoxCapacity(1).setBoxCount(1).build(),
                new MarschrouteWaybillItem(),
                null
            ),
            Arguments.of(
                new Item.ItemBuilder(null, null, null).setBoxCapacity(1).setBoxCount(2).build(),
                new MarschrouteWaybillItem().setSkuBox(SkuBox.COMPOSITE_BOX).setCntInBox(2),
                null
            ),
            Arguments.of(
                new Item.ItemBuilder(null, null, null).setBoxCapacity(2).setBoxCount(1).build(),
                new MarschrouteWaybillItem(),
                null
            ),
            Arguments.of(
                new Item.ItemBuilder(null, null, null).setBoxCapacity(2).setBoxCount(2).build(),
                null,
                FulfillmentApiException.class
            )
        );
    }

    private final WaybillItemBoxingEnricher boxingEnricher = new WaybillItemBoxingEnricher();

    @MethodSource("data")
    @ParameterizedTest
    void testEnrichment(
        Item source,
        MarschrouteWaybillItem expectedWaybillItem,
        Class<Throwable> expectedException
    ) {
        MarschrouteWaybillItem actual = new MarschrouteWaybillItem();
        if (expectedException != null) {
            assertThatThrownBy(() -> boxingEnricher.enrich(actual, source)).isInstanceOf(expectedException);
        } else {
            boxingEnricher.enrich(actual, source);
            assertThat(actual).usingRecursiveComparison().isEqualTo(expectedWaybillItem);
        }
    }
}
