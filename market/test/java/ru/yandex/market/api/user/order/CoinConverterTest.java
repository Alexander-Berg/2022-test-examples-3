package ru.yandex.market.api.user.order;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.checkout.checkouter.order.CoinError;
import ru.yandex.market.checkout.checkouter.order.CoinInfo;
import ru.yandex.market.loyalty.api.model.coin.CoinCreationReason;
import ru.yandex.market.loyalty.api.model.coin.CoinRestriction;
import ru.yandex.market.loyalty.api.model.coin.CoinRestrictionType;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.api.model.coin.CoinType;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.CoinErrorMatcher.code;
import static ru.yandex.market.api.matchers.CoinErrorMatcher.coinError;
import static ru.yandex.market.api.matchers.CoinErrorMatcher.coinId;
import static ru.yandex.market.api.matchers.CoinErrorMatcher.message;
import static ru.yandex.market.api.matchers.CoinInfoMatcher.allCoins;
import static ru.yandex.market.api.matchers.CoinInfoMatcher.coinErrors;
import static ru.yandex.market.api.matchers.CoinInfoMatcher.coinInfo;
import static ru.yandex.market.api.matchers.CoinInfoMatcher.unusedCoinIds;
import static ru.yandex.market.api.matchers.CoinRestrictionMatcher.categoryId;
import static ru.yandex.market.api.matchers.CoinRestrictionMatcher.coinRestriction;
import static ru.yandex.market.api.matchers.CoinRestrictionMatcher.restrictionType;
import static ru.yandex.market.api.matchers.CoinRestrictionMatcher.skuId;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.coinResponse;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.coinRestrictions;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.creationDate;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.endDate;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.id;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.reason;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.reasonParam;
import static ru.yandex.market.api.matchers.UserCoinResponseMatcher.title;

/**
 * Created by fettsery on 06.09.18.
 */
public class CoinConverterTest extends BaseTest {

    private CoinConverter coinConverter = new CoinConverter();
    @Test
    public void shouldConvertCoinInfo() throws ParseException {
        ru.yandex.market.api.user.order.CoinInfo coinInfo = coinConverter.convertCoinInfo(createCheckouterCoinInfo());

        assertThat(
            coinInfo,
            coinInfo(
                unusedCoinIds(cast(Matchers.containsInAnyOrder(10060L))),
                coinErrors(cast(Matchers.containsInAnyOrder(
                    coinError(
                        coinId(is(10061L)),
                        code(is("NOT APPLICABLE COIN")),
                        message(is("TEST"))
                    ))
                )),
                allCoins(cast(Matchers.containsInAnyOrder(
                    coinResponse(
                        id(is(10060L)),
                        title(is("100")),
                        creationDate(is("20-02-2017 14:40:00")),
                        endDate(is("20-02-2017 15:40:00")),
                        coinRestrictions(cast(Matchers.containsInAnyOrder(
                            coinRestriction(
                                categoryId(is(100500L)),
                                skuId(is("100500")),
                                restrictionType(is("CATEGORY"))
                            )
                        ))),
                        reason(is(ru.yandex.market.api.domain.v2.loyalty.CoinCreationReason.ORDER)),
                        reasonParam(is("123"))
                    ),
                    coinResponse(
                        id(is(10061L)),
                        title(is("500")),
                        creationDate(is("10-01-2018 09:10:00")),
                        endDate(is("10-01-2018 10:10:00")),
                        reason(is(ru.yandex.market.api.domain.v2.loyalty.CoinCreationReason.PARTNER)),
                        reasonParam(nullValue(String.class))
                    )
                )))
            )
        );

    }

    private CoinInfo createCheckouterCoinInfo() throws ParseException {
        CoinInfo coinInfo = new CoinInfo();

        coinInfo.setUnusedCoinIds(Collections.singletonList(10060L));
        coinInfo.setCoinErrors(Collections.singletonList(new CoinError(10061L, "NOT APPLICABLE COIN", "TEST")));
        coinInfo.setAllCoins(Arrays.asList(
                new UserCoinResponse(10060L,
                        "100",
                        "",
                        CoinType.FIXED,
                        BigDecimal.valueOf(100),
                        "100 рублей",
                        null,
                        new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("20-02-2017 14:40:00"),
                        new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("20-02-2017 15:40:00"),
                        "https://avatars.mdst.yandex.net/get-smart_shopping/1823/2080eff0-2a8e-4679-926c-032dfb3b29b8/",
                        null,
                        null,
                        CoinStatus.ACTIVE,
                        false,
                        null,
                        Collections.singletonList(new CoinRestriction(CoinRestrictionType.CATEGORY, "100500", 100500)),
                        CoinCreationReason.ORDER,
                        "123",
                        false,
                        null,
                        null,
                        false,
                        null,
                        null
                ),
                new UserCoinResponse(10061L,
                        "500",
                        "",
                        CoinType.FREE_DELIVERY,
                        BigDecimal.valueOf(500),
                        "500 рублей",
                        null,
                        new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("10-01-2018 09:10:00"),
                        new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("10-01-2018 10:10:00"),
                        "https://avatars.mdst.yandex.net/get-smart_shopping/1823/2080eff0-2a8e-4679-926c-032dfb3b29b8/",
                        null,
                        null,
                        CoinStatus.ACTIVE,
                        false,
                        null,
                        Collections.emptyList(),
                        CoinCreationReason.PARTNER,
                        null,
                        false,
                        null,
                        null,
                        false,
                        null,
                        null
                )
        ));

        return coinInfo;
    }

    private static <T> Matcher<T> cast(Matcher<?> matcher) {
        return (Matcher<T>) matcher;
    }
}
