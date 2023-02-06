package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.common.MarketType;
import ru.yandex.market.api.offer.GetOffersByModelRequest;

import static org.hamcrest.Matchers.allOf;

/**
 * @authror dimkarp93
 */
public class GetOffersByModelRequestMatcher {
    public static Matcher<GetOffersByModelRequest> offerByModelRequest(Matcher<GetOffersByModelRequest>... matchers) {
        return allOf(matchers);
    }

    public static Matcher<GetOffersByModelRequest> modelId(long modelId) {
        return ApiMatchers.map(
            GetOffersByModelRequest::getModelId,
            "'modelId",
            Matchers.is(modelId)
        );
    }

    public static Matcher<GetOffersByModelRequest> shopIds(Matcher<Iterable<? extends Long>> shopIds) {
        return ApiMatchers.map(
            GetOffersByModelRequest::getShopIds,
            "'shopIds",
            shopIds
        );
    }

    public static Matcher<GetOffersByModelRequest> withModel(boolean withModel) {
        return ApiMatchers.map(
            GetOffersByModelRequest::isWithModel,
            "'withModel",
            Matchers.is(withModel)
        );
    }

    public static Matcher<GetOffersByModelRequest> marketType(MarketType marketType) {
        return ApiMatchers.map(
            GetOffersByModelRequest::getMarketType,
            "'marketType'",
            Matchers.is(marketType)
        );
    }

    public static String toStr(GetOffersByModelRequest request) {
        if (null == request) {
            return "null";
        }
        return MoreObjects.toStringHelper(GetOffersByModelRequestMatcher.class)
            .add("modelId", request.getModelId())
            .add("shopIds", ApiMatchers.collectionToStr(request.getShopIds(), String::valueOf))
            .add("withModel", request.isWithModel())
            .add("marketType", request.getMarketType())
            .toString();
    }
}
