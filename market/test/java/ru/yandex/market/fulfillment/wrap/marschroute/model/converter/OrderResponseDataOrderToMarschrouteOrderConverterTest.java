package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders.OrderResponseDataOrder;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteLocation;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteOrder;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteOrderOption;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteDeliveryInterval;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschroutePaymentType;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteTime;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteTemporalType.getMarschrouteZoneOffset;

public class OrderResponseDataOrderToMarschrouteOrderConverterTest extends BaseIntegrationTest {

    private final OrderResponseDataOrderToMarschrouteOrderConverter converter =
        new OrderResponseDataOrderToMarschrouteOrderConverter();

    private static Stream<Arguments> orderModifiers() {
        return Stream.of(
            Pair.<String, Consumer<OrderResponseDataOrder>>of(
                "Полностью заполненный заказ",
                orderResponseDataOrder -> {
                }
            ),
            Pair.<String, Consumer<OrderResponseDataOrder>>of(
                "Заказ без timeFrom",
                orderResponseDataOrder -> orderResponseDataOrder.setTimeFrom(null)
            ),
            Pair.<String, Consumer<OrderResponseDataOrder>>of(
                "Заказ без placeId",
                orderResponseDataOrder -> orderResponseDataOrder.setPlaceId(null)
            )
        )
            .map(pair -> Arguments.of(pair.getFirst(), pair.getSecond()));
    }

    @ParameterizedTest()
    @MethodSource("orderModifiers")
    void testConversion(String displayName, Consumer<OrderResponseDataOrder> orderModifier) {
        OrderResponseDataOrder order = createOrder();
        orderModifier.accept(order);

        MarschrouteOrder marschrouteOrder = converter.convert(order);

        softly.assertThat(marschrouteOrder).isNotNull();

        assertThatOrdersAreEqual(order, marschrouteOrder);
    }

    void assertThatOrdersAreEqual(OrderResponseDataOrder order, MarschrouteOrder marschrouteOrder) {
        softly.assertThat(marschrouteOrder.getId())
            .as("Asserting that converted yandexId is equal to expected value")
            .isEqualTo(order.getId());
        softly.assertThat(marschrouteOrder.getPaymentType())
            .as("Asserting that converted PaymentType is equal to expected value")
            .isEqualTo(order.getPaymentType());
        softly.assertThat(marschrouteOrder.getSendDate())
            .as("Asserting that converted SendDate is equal to expected value")
            .isEqualTo(order.getSendDate());
        softly.assertThat(Optional.ofNullable(marschrouteOrder.getPlaceId()))
            .as("Asserting that converted PlaceID is equal to expected value")
            .isEqualTo(Optional.ofNullable(order.getPlaceId()).map(Integer::parseInt));
        softly.assertThat(marschrouteOrder.getLocation())
            .as("Asserting that converted Location is equal to expected value")
            .isEqualTo(order.getLocation());
        softly.assertThat(marschrouteOrder.getConsignee())
            .as("Asserting that converted Consignee is equal to expected value")
            .isEqualTo(order.getConsignee());
        softly.assertThat(marschrouteOrder.getConsigneeDoc())
            .as("Asserting that converted ConsigneeDoc is equal to expected value")
            .isEqualTo(order.getConsigneeDoc());
        softly.assertThat(marschrouteOrder.getDeliveryInterval())
            .as("Asserting that converted DeliveryInterval is equal to expected value")
            .isEqualTo(order.getDeliveryInterval());
        softly.assertThat(marschrouteOrder.getDeliverySum())
            .as("Asserting that converted DeliverySum is equal to expected value")
            .isEqualTo(order.getDeliverySum());
        softly.assertThat(marschrouteOrder.getComment())
            .as("Asserting that converted Comment is equal to expected value")
            .isEqualTo(order.getComment());
        softly.assertThat(marschrouteOrder.getWeight())
            .as("Asserting that converted Weight is equal to expected value")
            .isEqualTo(order.getWeight());
        softly.assertThat(marschrouteOrder.getBarcode())
            .as("Asserting that converted Barcode is equal to expected value")
            .isEqualTo(order.getPostBarcode());
        softly.assertThat(marschrouteOrder.getOptions())
            .as("Asserting that converted Options is equal to expected value")
            .isEqualTo(order.getOptions());
        softly.assertThat(Optional.ofNullable(marschrouteOrder.getTimeFrom()).map(MarschrouteTime::getOffsetTime))
            .as("Asserting that converted TimeFrom is equal to expected value")
            .isEqualTo(
                Optional.ofNullable(order.getTimeFrom())
                    .map(localTime -> localTime.atOffset(getMarschrouteZoneOffset()))
            );
    }

    private OrderResponseDataOrder createOrder() {
        OrderResponseDataOrder order = new OrderResponseDataOrder();
        order.setId("123");
        order.setPaymentType(MarschroutePaymentType.CASH);
        order.setSendDate(MarschrouteDate.create(LocalDate.now()));
        order.setPlaceId("-1");
        order.setLocation(new MarschrouteLocation().setCityId("1000"));
        order.setConsignee("100F");
        order.setConsigneeDoc("cDoc");
        order.setDeliveryInterval(MarschrouteDeliveryInterval.DAY);
        order.setDeliverySum(123);
        order.setComment("comment");
        order.setWeight(123);
        order.setPostBarcode("barcode");

        MarschrouteOrderOption options = new MarschrouteOrderOption();
        options.setCanTry(2);
        order.setOptions(options);

        order.setTimeFrom(LocalTime.of(10, 10));

        return order;
    }

}
