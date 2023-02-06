package ru.yandex.direct.core.testing.mock;

import java.math.BigDecimal;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;

import one.util.streamex.StreamEx;

import ru.yandex.direct.bsauction.BsRequest;
import ru.yandex.direct.bsauction.BsRequestPhrase;
import ru.yandex.direct.bsauction.BsResponse;
import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.bsauction.FullBsTrafaretResponsePhrase;
import ru.yandex.direct.currency.CurrencyCode;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestBsResponses.defaultTrafaretResponsePhrase;
import static ru.yandex.direct.core.testing.data.TestBsResponses.trafaretResponsePhraseFromOldPositions;

public class BsTrafaretClientMockUtils {

    public static void setDefaultMockOnBsTrafaretClient(BsTrafaretClient bsTrafaretClient) {
        when(bsTrafaretClient.getAuctionResultsWithPositionCtrCorrection(anyList())).thenAnswer(
                invocation -> {
                    List<BsRequest<BsRequestPhrase>> requests = invocation.getArgument(0);
                    return generateDefaultBsAuctionResponse(requests);
                }
        );
    }

    public static IdentityHashMap<BsRequest<BsRequestPhrase>, BsResponse<BsRequestPhrase, FullBsTrafaretResponsePhrase>> generateDefaultBsAuctionResponse(
            List<BsRequest<BsRequestPhrase>> requests) {
        return StreamEx.of(requests)
                .mapToEntry(r -> {
                    IdentityHashMap<BsRequestPhrase, FullBsTrafaretResponsePhrase> successResult =
                            StreamEx.of(r.getPhrases())
                                    .mapToEntry(phr -> defaultTrafaretResponsePhrase(CurrencyCode.RUB, phr))
                                    .toCustomMap(IdentityHashMap::new);
                    return BsResponse.success(successResult);
                })
                .toCustomMap(IdentityHashMap::new);
    }

    public static void setCustomMockOnBsTrafaretClient(
            BsTrafaretClient bsTrafaretClient, BigDecimal firstGuaranteePrice, BigDecimal firstPremiumPrice
    ) {
        Function<BsRequestPhrase, FullBsTrafaretResponsePhrase> responder = (phr) ->
                trafaretResponsePhraseFromOldPositions(CurrencyCode.RUB, firstGuaranteePrice, firstPremiumPrice);
        when(bsTrafaretClient.getAuctionResultsWithPositionCtrCorrection(anyList())).thenAnswer(
                invocation -> {
                    List<BsRequest<BsRequestPhrase>> requests = invocation.getArgument(0);
                    return generateCustomBsAuctionResponse(requests, responder);
                }
        );
    }

    public static IdentityHashMap<BsRequest<BsRequestPhrase>, BsResponse<BsRequestPhrase, FullBsTrafaretResponsePhrase>>
    generateCustomBsAuctionResponse(
            List<BsRequest<BsRequestPhrase>> requests,
            Function<BsRequestPhrase, FullBsTrafaretResponsePhrase> responder
    ) {
        return StreamEx.of(requests)
                .mapToEntry(r -> {
                    IdentityHashMap<BsRequestPhrase, FullBsTrafaretResponsePhrase> successResult =
                            StreamEx.of(r.getPhrases())
                                    .mapToEntry(responder)
                                    .toCustomMap(IdentityHashMap::new);
                    return BsResponse.success(successResult);
                })
                .toCustomMap(IdentityHashMap::new);
    }
}
