package ru.yandex.market.loyalty.api;

import org.junit.Test;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class MarketLoyaltyErrorTest {

    @Test
    public void shouldCreateErrorWithoutUserMessage(){
        MarketLoyaltyError error = new MarketLoyaltyError(MarketLoyaltyErrorCode.OTHER_ERROR);
        assertThat(error.getUserMessage(), nullValue());
    }

    @Test
    public void shouldCreateErrorWithUserMessage(){
        MarketLoyaltyError error = new MarketLoyaltyError(MarketLoyaltyErrorCode.ALLOWED_FOR_FIRST_ORDER_ONLY);
        assertThat(error.getUserMessage(), notNullValue());
        assertThat(error.getUserMessage(), is(MarketLoyaltyErrorCode.ALLOWED_FOR_FIRST_ORDER_ONLY.getDefaultDescription()));
    }
}
