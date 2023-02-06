package ru.yandex.market.checkout.util.loyalty;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CalcV3ResponseTransformer extends AbstractLoyaltyBundleResponseTransformer {

    public CalcV3ResponseTransformer(ObjectMapper marketLoyaltyObjectMapper) {
        super(marketLoyaltyObjectMapper);
    }

    @Override
    public String getName() {
        return "loyalty-calc-v3-transformer";
    }
}
