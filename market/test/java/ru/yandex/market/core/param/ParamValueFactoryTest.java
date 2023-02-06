package ru.yandex.market.core.param;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.AuctionType;
import ru.yandex.market.core.param.model.ParamType;

/**
 * Тесты для {@link ParamValueFactory}.
 *
 * @author Ilya Reznikov richard@yandex-team.ru
 */
class ParamValueFactoryTest {

    @ParameterizedTest
    @EnumSource(AuctionType.class)
    void testMakeParamAuctionType(AuctionType auctionType) {
        Assertions.assertDoesNotThrow(() -> ParamValueFactory.makeParam(
                ParamType.AUCTION_TYPE,
                -1,
                auctionType.getId())
        );
    }

    @Test
    void testMakeParamAuctionTypeInvalid() {
        final IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ParamValueFactory.makeParam(
                        ParamType.AUCTION_TYPE,
                        -1,
                        "some_unsupported_auction_type"
                )
        );
        Assertions.assertEquals(
                "Value \"some_unsupported_auction_type\" is not compatible with param type: AUCTION_TYPE",
                ex.getMessage()
        );
    }

    @ParameterizedTest
    @EnumSource(AuctionOfferIdType.class)
    void testMakeParamAuctionOfferIdType(AuctionOfferIdType auctionOfferIdType) {
        Assertions.assertDoesNotThrow(() -> ParamValueFactory.makeParam(
                ParamType.AUCTION_OFFER_ID_TYPE,
                -1,
                auctionOfferIdType.getId())
        );
    }

    @Test
    void testMakeParamAuctionOfferIdTypeInvalid() {
        final IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ParamValueFactory.makeParam(
                        ParamType.AUCTION_OFFER_ID_TYPE,
                        -1,
                        "some_unsupported_auction_offer_id_type"
                )
        );
        Assertions.assertEquals(
                "Value \"some_unsupported_auction_offer_id_type\" is not compatible with param type: AUCTION_OFFER_ID_TYPE",
                ex.getMessage()
        );
    }
}
