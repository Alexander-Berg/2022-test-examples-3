package ru.yandex.market.loyalty.core.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.loyalty.api.model.promocode.MarketLoyaltyPromocodeWarningCode;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeWarning;
import ru.yandex.market.loyalty.core.config.CoreTestConfig;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(LoyaltySpringTestRunner.class)
@ContextConfiguration(classes = CoreTestConfig.class)
public class MarketLoyaltyPromocodeWarningTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testWarningsSerialization() throws JsonProcessingException {
        PromocodeWarning source = new PromocodeWarning("promocode",
                MarketLoyaltyPromocodeWarningCode.MAX_DISCOUNT_REACHED);
        String s = objectMapper.writeValueAsString(source);
        PromocodeWarning promocodeWarning = objectMapper.readValue(s, PromocodeWarning.class);
        assertThat(promocodeWarning, allOf(
                hasProperty("promocode", equalTo("promocode")),
                hasProperty("warningCode", equalTo(MarketLoyaltyPromocodeWarningCode.MAX_DISCOUNT_REACHED)),
                hasProperty("userMessage", nullValue())
        ));

        source = new PromocodeWarning("promocode", MarketLoyaltyPromocodeWarningCode.MAX_DISCOUNT_REACHED, "user");
        s = objectMapper.writeValueAsString(source);
        promocodeWarning = objectMapper.readValue(s, PromocodeWarning.class);
        assertThat(promocodeWarning, allOf(
                hasProperty("promocode", equalTo("promocode")),
                hasProperty("warningCode", equalTo(MarketLoyaltyPromocodeWarningCode.MAX_DISCOUNT_REACHED)),
                hasProperty("userMessage", equalTo("user"))
        ));
    }
}
