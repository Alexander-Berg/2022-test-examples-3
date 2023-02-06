package ru.yandex.market.api.internal.loyalty.parser;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.v2.loyalty.BaseCoinResponse;
import ru.yandex.market.api.domain.v2.loyalty.BonusLink;
import ru.yandex.market.api.domain.v2.loyalty.CoinCreationReason;
import ru.yandex.market.api.domain.v2.loyalty.CoinsForPerson;
import ru.yandex.market.api.domain.v2.loyalty.UserCoinResponse;
import ru.yandex.market.api.matchers.FutureCoinResponseMatcher;
import ru.yandex.market.api.matchers.UserCoinResponseMatcher;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.ApiMatchers.entry;
import static ru.yandex.market.api.matchers.CoinRestrictionMatcher.coinRestriction;
import static ru.yandex.market.api.matchers.CoinRestrictionMatcher.restrictionType;
import static ru.yandex.market.api.matchers.CoinRestrictionMatcher.skuId;
import static ru.yandex.market.api.matchers.FutureCoinResponseMatcher.coinType;
import static ru.yandex.market.api.matchers.FutureCoinResponseMatcher.promoId;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.id;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.reason;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.reasonParam;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.title;

/**
 * Created by fettsery on 07.09.18.
 */
public class CoinsForPersonParserTest {
    @Test
    public void shouldParseCoinsForPerson() {
        CoinsForPerson result = new CoinsForPersonParser().parse(ResourceHelpers.getResource("get-coins-for-person.json"));

        assertEquals(1, result.getCoins().size());

        UserCoinResponse userCoinResponse = result.getCoins().get(0);

        assertThat(userCoinResponse, UserCoinResponseMatcher.coinResponse(
            id(is(12345L)),
            title(is("Большая скидка в 100%")),
            reason(is(CoinCreationReason.EMAIL_COMPANY)),
            reasonParam(is("some reason"))
        ));

        assertThat(userCoinResponse.getImages().entrySet(), containsInAnyOrder(
            entry("additionalProp1", "prop1"),
            entry("additionalProp2", "prop2"),
            entry("additionalProp3", "prop3")
        ));

        Assert.assertEquals(1, userCoinResponse.getBonusLink().size());
        final BonusLink link = userCoinResponse.getBonusLink().get(0);
        Assert.assertEquals("some_ref", link.getRef());
        Assert.assertEquals("title_string", link.getTitle());
        Assert.assertThat(link.getStates(), containsInAnyOrder(
                UserCoinResponse.CoinStatus.REQUIRE_AUTH,
                UserCoinResponse.CoinStatus.EXPIRED
        ));

        Assert.assertEquals("https://test.link/outgoing", userCoinResponse.getOutgoingLink());
        Assert.assertEquals("Неактивная обычная монетка", userCoinResponse.getInactiveDescription());
        Assert.assertEquals("-8ozbhmckNcmcNUOtlDUKg", userCoinResponse.getPromoKey());
        Assert.assertEquals(true, userCoinResponse.getIsCategoryBonus());

        assertThat(result.getFutureCoins(), containsInAnyOrder(
            FutureCoinResponseMatcher.coinResponse(
                promoId(is(10319L)),
                FutureCoinResponseMatcher.title(is("Скидка на 100 рублей")),
                coinType(is(BaseCoinResponse.CoinType.FIXED))
            ),
            FutureCoinResponseMatcher.coinResponse(
                promoId(is(10322L)),
                FutureCoinResponseMatcher.title(is("заголовок тута vot")),
                coinType(is(BaseCoinResponse.CoinType.FIXED)),
                FutureCoinResponseMatcher.coinRestrictions(cast(Matchers.contains(
                    coinRestriction(
                        skuId(is("100131946414")),
                        restrictionType(is("MSKU"))
                    )
                )))
            )
        ));
    }

    @Test
    public void shouldParseCoinForUserAction() {
        CoinsForPerson result = new CoinsForPersonParser().parse(ResourceHelpers.getResource("get-coins-for-person-for-user-action.json"));
        Assert.assertThat(
                result.getCoins(),
                Matchers.hasItems(
                        UserCoinResponseMatcher.reason(Matchers.is(CoinCreationReason.FOR_USER_ACTION_DYNAMIC))
                )
        );
    }

    private static <T> Matcher<T> cast(Matcher<?> matcher) {
        return (Matcher<T>) matcher;
    }
}
