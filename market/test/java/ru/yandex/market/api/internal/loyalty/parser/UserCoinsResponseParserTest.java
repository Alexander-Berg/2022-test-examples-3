package ru.yandex.market.api.internal.loyalty.parser;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.loyalty.CoinCreationReason;
import ru.yandex.market.api.domain.v2.loyalty.UserCoinResponse;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.matchers.UserCoinResponseMatcher;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * @authror dimkarp93
 */
public class UserCoinsResponseParserTest extends UnitTestBase {
    @Test
    public void simple() {
        UserCoinResponse response = parse("coins-user-response.json");
        Assert.assertThat(
            response,
            UserCoinResponseMatcher.coinResponse(
                UserCoinResponseMatcher.id(Matchers.is(12L)),
                UserCoinResponseMatcher.title(Matchers.is("Большая скидка в 100%")),
                UserCoinResponseMatcher.reason(Matchers.is(CoinCreationReason.ORDER)),
                UserCoinResponseMatcher.reasonParam(Matchers.is("123")),
                UserCoinResponseMatcher.reasonOrderIds(Matchers.containsInAnyOrder("order1", "order2", "order3"))
            )
        );
        Assert.assertNull(response.getBonusLink());
        Assert.assertNull(response.getOutgoingLink());
        Assert.assertNull(response.getInactiveDescription());
        Assert.assertNull(response.getIsCategoryBonus());
    }

    private UserCoinResponse parse(String filename) {
        UserCoinResponseParser parser = new UserCoinResponseParser();
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
