package ru.yandex.market.fulfillment.wrap.marschroute.model.request.delivery.city;

import java.util.Collections;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteDimensions;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschroutePaymentType;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.UrlVariablesProducerTest;

class MarschrouteDeliveryCityRequestTest extends UrlVariablesProducerTest {

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                "empty",
                new MarschrouteDeliveryCityRequest(),
                wrapInMultiValueMap(Collections.emptyMap())
            ),
            Arguments.of(
                "cityKladr",
                new MarschrouteDeliveryCityRequest().setCityKladr("555"),
                wrapInMultiValueMap(ImmutableMap.of("kladr", "555"))
            ),
            Arguments.of(
                "cityKladr, weight",
                new MarschrouteDeliveryCityRequest().setCityKladr("555").setWeight(100),
                wrapInMultiValueMap(ImmutableMap.of("kladr", "555", "weight", "100"))
            ),
            Arguments.of(
                "cityKladr, weight, ordersSum",
                new MarschrouteDeliveryCityRequest().setCityKladr("555").setWeight(100).setOrderSum(500),
                wrapInMultiValueMap(ImmutableMap.of(
                    "kladr", "555",
                    "weight", "100",
                    "order_sum", "500"
                ))
            ),
            Arguments.of(
                "cityKladr, weight, ordersSum, dimensions",
                new MarschrouteDeliveryCityRequest()
                    .setCityKladr("555")
                    .setWeight(100)
                    .setOrderSum(500)
                    .setDimensions(new MarschrouteDimensions(10, 10, 10)),
                wrapInMultiValueMap(ImmutableMap.of(
                    "kladr", "555",
                    "weight", "100",
                    "order_sum", "500",
                    "parcel_size", "[10, 10, 10]"
                ))
            ),
            Arguments.of(
                "cityKladr, weight, ordersSum, dimensions, paymentType",
                new MarschrouteDeliveryCityRequest()
                    .setPaymentType(MarschroutePaymentType.CASH)
                    .setCityKladr("555")
                    .setWeight(100)
                    .setOrderSum(500)
                    .setDimensions(new MarschrouteDimensions(10, 10, 10)),
                wrapInMultiValueMap(ImmutableMap.of(
                    "payment_type", "1",
                    "kladr", "555",
                    "weight", "100",
                    "order_sum", "500",
                    "parcel_size", "[10, 10, 10]"
                ))
            ),
            Arguments.of(
                "cityKladr, weight, ordersSum, dimensions, paymentType, index",
                new MarschrouteDeliveryCityRequest()
                    .setPaymentType(MarschroutePaymentType.CASH)
                    .setCityKladr("555")
                    .setWeight(100)
                    .setOrderSum(500)
                    .setDimensions(new MarschrouteDimensions(10, 10, 10))
                    .setIndex("123456"),
                wrapInMultiValueMap(ImmutableMap.builder()
                    .put("payment_type", "1")
                    .put("kladr", "555")
                    .put("weight", "100")
                    .put("order_sum", "500")
                    .put("parcel_size", "[10, 10, 10]")
                    .put("index", "123456")
                    .build()
                )
            )
        );
    }
}
