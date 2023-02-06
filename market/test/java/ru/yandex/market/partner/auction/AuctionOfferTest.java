package ru.yandex.market.partner.auction;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.partner.auction.view.SerializationGate;

/**
 * Тесты для {@link AuctionOffer}.
 *
 * @author vbudnev
 */
class AuctionOfferTest {

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(
                        "valid title",
                        (Supplier<AuctionOffer>) () -> new AuctionOffer(new AuctionOfferId("someTitle")),
                        "someTitle"
                ),
                Arguments.of(
                        "raw feed-offer-id",
                        (Supplier<AuctionOffer>) () -> new AuctionOffer(new AuctionOfferId(1234L, "someId")),
                        "1234-someId"
                ),
                Arguments.of(
                        "feed-offer-id with explicit name",
                        (Supplier<AuctionOffer>) () -> {
                            final AuctionOffer offer = new AuctionOffer(new AuctionOfferId(1234L, "someId"));
                            offer.setOfferName("explicitNameForIdOffer");
                            return offer;
                        },
                        "explicitNameForIdOffer"
                ),
                Arguments.of(
                        "feed-offer-id with explicit empty name",
                        (Supplier<AuctionOffer>) () -> {
                            final AuctionOffer offer = new AuctionOffer(new AuctionOfferId(1234L, "someId"));
                            offer.setOfferName("");
                            return offer;
                        },
                        "1234-someId"
                )
        );
    }

    /**
     * Проверка поведеиния {@link SerializationGate.AuctionOffer#getOfferName()}.
     * Так как это не простой геттер, то поведение зафкисировано в тесте.
     */
    @MethodSource("args")
    @ParameterizedTest(name = "{0}")
    void test_offerNameCheck(String description, Supplier<AuctionOffer> producer, String expectedOfferName) {
        Assertions.assertEquals(expectedOfferName, producer.get().getOfferName());
    }
}