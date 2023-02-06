package ru.yandex.market.checkout.util.loyalty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;

import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.INVALID_REQUEST;

public class SpendV3ResponseTransformer extends AbstractLoyaltyBundleResponseTransformer {

    public SpendV3ResponseTransformer(ObjectMapper marketLoyaltyObjectMapper) {
        super(marketLoyaltyObjectMapper);
    }

    @Override
    protected ResponseDefinition validateRequest(
            MultiCartWithBundlesDiscountRequest discountRequest,
            LoyaltyParameters loyaltyParameters) {
        discountRequest.getOrders().forEach(
                o -> {
                    if (o.getDeliveries().stream().noneMatch(DeliveryRequest::isSelected)) {
                        throw new ValidationException(new MarketLoyaltyError(INVALID_REQUEST.name(),
                                "No selected delivery discount request: " + o.getDeliveries(),
                                null));
                    }
                }
        );
        return super.validateRequest(discountRequest, loyaltyParameters);
    }

    @Override
    public String getName() {
        return "loyalty-spend-v3-transformer";
    }
}
