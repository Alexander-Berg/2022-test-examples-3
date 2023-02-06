package ru.yandex.market.api.internal.loyalty.parser;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.v2.loyalty.BonusLink;
import ru.yandex.market.api.domain.v2.loyalty.UserCoinResponse;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.hamcrest.Matchers.containsInAnyOrder;

public class BonusLinkParserTest extends UnitTestBase {
    @Test
    public void simple() {
        BonusLink response = parse("link-response.json");
        Assert.assertEquals("some_ref", response.getRef());
        Assert.assertEquals("title_string", response.getTitle());
        Assert.assertThat(response.getStates(), containsInAnyOrder(
                UserCoinResponse.CoinStatus.REQUIRE_AUTH,
                UserCoinResponse.CoinStatus.EXPIRED
        ));
    }

    private BonusLink parse(String filename) {
        BonusLinkParser parser = new BonusLinkParser();
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
