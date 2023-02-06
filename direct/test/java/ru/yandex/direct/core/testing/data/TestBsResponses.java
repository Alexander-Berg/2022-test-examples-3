package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;

import ru.yandex.direct.bsauction.BsAuctionBidItem;
import ru.yandex.direct.bsauction.BsCpcPrice;
import ru.yandex.direct.bsauction.BsRequestPhrase;
import ru.yandex.direct.bsauction.FullBsTrafaretResponsePhrase;
import ru.yandex.direct.bsauction.PositionalBsTrafaretResponsePhrase;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static java.util.Arrays.asList;
import static ru.yandex.direct.bsauction.BsTrafaretClient.PLACE_TRAFARET_MAPPING;
import static ru.yandex.direct.bsauction.BsTrafaretClient.Place.GUARANTEE1;
import static ru.yandex.direct.bsauction.BsTrafaretClient.Place.PREMIUM1;

public class TestBsResponses {
    private static final Long DEFAULT_PRICE_MICRO = 10_000_000L;

    private TestBsResponses() {
    }

    public static PositionalBsTrafaretResponsePhrase defaultPhraseResponse(CurrencyCode currencyCode, BsRequestPhrase requestPhrase) {
        PositionalBsTrafaretResponsePhrase response = new PositionalBsTrafaretResponsePhrase();
        response.setPremium(new BsCpcPrice[]{
                buildBsCpcPrice(currencyCode, DEFAULT_PRICE_MICRO),
                buildBsCpcPrice(currencyCode, DEFAULT_PRICE_MICRO),
                buildBsCpcPrice(currencyCode, DEFAULT_PRICE_MICRO)
        });
        response.setGuarantee(new BsCpcPrice[]{
                buildBsCpcPrice(currencyCode, DEFAULT_PRICE_MICRO),
                buildBsCpcPrice(currencyCode, DEFAULT_PRICE_MICRO),
                buildBsCpcPrice(currencyCode, DEFAULT_PRICE_MICRO),
                buildBsCpcPrice(currencyCode, DEFAULT_PRICE_MICRO)
        });
        return response;
    }

    public static FullBsTrafaretResponsePhrase defaultTrafaretResponsePhrase(CurrencyCode currencyCode,
                                                                             BsRequestPhrase requestPhrase) {
        FullBsTrafaretResponsePhrase response = new FullBsTrafaretResponsePhrase();
        response.withBidItems(asList(
                buildBsAuctionBidItem(1_000_000L, currencyCode, DEFAULT_PRICE_MICRO),
                buildBsAuctionBidItem(800_000L, currencyCode, DEFAULT_PRICE_MICRO)
        ));
        return response;
    }

    public static FullBsTrafaretResponsePhrase trafaretResponsePhraseFromOldPositions(
            CurrencyCode currencyCode, BigDecimal firstGuaranteePrice, BigDecimal firstPremiumPrice
    ) {
        FullBsTrafaretResponsePhrase response = new FullBsTrafaretResponsePhrase();
        response.withBidItems(asList(
                buildBsAuctionBidItem(PLACE_TRAFARET_MAPPING.get(PREMIUM1), currencyCode, micro(firstPremiumPrice)),
                buildBsAuctionBidItem(PLACE_TRAFARET_MAPPING.get(GUARANTEE1), currencyCode, micro(firstGuaranteePrice))
        ));
        return response;
    }

    private static BsCpcPrice buildBsCpcPrice(CurrencyCode currencyCode, long priceMicro) {
        return new BsCpcPrice(buildMoney(currencyCode, priceMicro), buildMoney(currencyCode, priceMicro));
    }

    private static BsAuctionBidItem buildBsAuctionBidItem(long positionCtrCorrection, CurrencyCode currencyCode,
                                                          long priceMicro) {
        return new BsAuctionBidItem(positionCtrCorrection, buildMoney(currencyCode, priceMicro),
                buildMoney(currencyCode, priceMicro));
    }

    private static Money buildMoney(CurrencyCode currencyCode, long priceMicro) {
        return Money.valueOfMicros(priceMicro, currencyCode);
    }

    private static Long micro(BigDecimal price) {
        return price.multiply(BigDecimal.valueOf(1_000_000)).longValue();
    }
}
