package ru.yandex.market.logistics.cs.logbroker;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.logistics.cs.logbroker.checkouter.OrderHistoryEventParser;
import ru.yandex.market.logistics.cs.logbroker.checkouter.OrderHistoryEventParser.RouteEventPair;
import ru.yandex.market.logistics.cs.util.TestDtoFactory;
import ru.yandex.market.logistics.cs.util.TestDtoFactory.SingleServiceOrderHistoryEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.logistics.cs.util.OrderEventUtils.SELF_DELIVERY_SERVICE;
import static ru.yandex.market.logistics.cs.util.OrderEventUtils.TANK_EMAIL;

class OrderHistoryEventParserTest {

    private static Stream<Arguments> data() {
        return Stream.of(
            // pass-through if marking is not enabled
            Stream.of(
                //           fake,  tank,  gold,  process, empty, dummy
                Arguments.of(false, false, false, false, false, false, Function.identity()),
                Arguments.of(false, false, false, false, false, false, makeFake()),
                Arguments.of(false, false, false, false, false, false, makeTank()),
                Arguments.of(false, false, false, false, false, false, makeGold()),
                Arguments.of(false, false, false, false, false, false, makeFake().andThen(makeTank())),
                Arguments.of(false, false, false, false, false, false, makeFake().andThen(makeGold())),
                Arguments.of(false, false, false, false, false, false, makeGold().andThen(makeTank())),
                Arguments.of(false, false, false, false, false, false,
                    makeFake().andThen(makeTank()).andThen(makeGold())
                )
            ),
            // filtering if processing is disabled
            Stream.of(
                Arguments.of(true, false, false, false, true, false, makeFake()),
                Arguments.of(false, true, false, false, true, false, makeTank()),
                Arguments.of(false, false, true, false, true, false, makeGold()),
                Arguments.of(true, false, false, false, true, false, makeFake().andThen(makeTank())),
                Arguments.of(false, true, false, false, true, false, makeFake().andThen(makeTank())),
                Arguments.of(true, false, false, false, true, false, makeFake().andThen(makeGold())),
                Arguments.of(false, false, true, false, true, false, makeFake().andThen(makeGold())),
                Arguments.of(false, true, false, false, true, false, makeGold().andThen(makeTank())),
                Arguments.of(false, false, true, false, true, false, makeGold().andThen(makeTank())),
                Arguments.of(true, false, false, false, true, false,
                    makeFake().andThen(makeTank()).andThen(makeGold())
                ),
                Arguments.of(false, true, false, false, true, false,
                    makeFake().andThen(makeTank()).andThen(makeGold())
                ),
                Arguments.of(false, false, true, false, true, false,
                    makeFake().andThen(makeTank()).andThen(makeGold())
                )
            ),
            // pass-through for enabled events
            Stream.of(
                Arguments.of(true, false, false, true, false, true, makeFake()),
                Arguments.of(false, true, false, true, false, true, makeTank()),
                Arguments.of(false, false, true, true, false, true, makeGold()),

                Arguments.of(false, true, false, true, false, false, makeFake()),
                Arguments.of(true, false, false, true, false, false, makeTank()),
                Arguments.of(false, true, false, false, false, false, makeGold())
            )
        ).flatMap(Function.identity());
    }

    @ParameterizedTest
    @MethodSource("data")
    void testOrderHistoryEventsParser(
        boolean fake,
        boolean tank,
        boolean gold,
        boolean filter,
        boolean empty,
        boolean dummy,
        Function<OrderHistoryEvent, OrderHistoryEvent> processor
    ) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        SingleServiceOrderHistoryEvent dto =
            TestDtoFactory.singleServiceWithParcelOrder(HistoryEventType.NEW_ORDER, OrderStatus.RESERVED);
        OrderHistoryEvent event = processor.apply(dto.getEvent());
        OrderHistoryEventParser parser = new OrderHistoryEventParser(mapper, fake, tank, gold, filter);

        Optional<RouteEventPair> result = parser.parse(event, "key", dto.getRouteNode());
        assertEquals(result.isEmpty(), empty);
        result.ifPresent(eventWithRouteDto -> assertEquals(eventWithRouteDto.getEvent().isDummy(), dummy));
    }

    private static Function<OrderHistoryEvent, OrderHistoryEvent> makeFake() {
        return e -> {
            e.getOrderAfter().setFake(true);
            return e;
        };
    }

    private static Function<OrderHistoryEvent, OrderHistoryEvent> makeTank() {
        return e -> {
            Buyer buyer = new Buyer();
            buyer.setEmail(TANK_EMAIL);
            e.getOrderAfter().setBuyer(buyer);
            return e;
        };
    }

    private static Function<OrderHistoryEvent, OrderHistoryEvent> makeGold() {
        return e -> {
            e.getOrderAfter().getDelivery().setDeliveryServiceId(SELF_DELIVERY_SERVICE);
            return e;
        };
    }
}
