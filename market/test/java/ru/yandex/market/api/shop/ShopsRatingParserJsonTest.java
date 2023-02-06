package ru.yandex.market.api.shop;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.ShopRatingInfo;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.matchers.RatingV2Matcher;
import ru.yandex.market.api.matchers.ShopRatingStatusMatcher;
import ru.yandex.market.api.util.ResourceHelpers;

import java.math.BigDecimal;

public class ShopsRatingParserJsonTest extends UnitTestBase {
    private static final BigDecimal HAS_NO_RATING = BigDecimal.ONE.negate();

    private ShopsRatingJsonParser parser = new ShopsRatingJsonParser();

    @Test
    public void parseFields() {
        Long2ObjectMap<ShopRatingInfo> map = parse("shop_rating_fields.json");

        Matcher<ShopRatingInfo> rating = RatingV2Matcher.rating(
            RatingV2Matcher.count(111),
            RatingV2Matcher.value(new BigDecimal("4.1")),
            RatingV2Matcher.status(
                ShopRatingStatusMatcher.id("ACTUAL"),
                ShopRatingStatusMatcher.name("Рейтинг нормально рассчитан")
            )
        );

        Assert.assertThat(
            map.entrySet(),
            Matchers.contains(
                ApiMatchers.entry(180028L, rating)
            )
        );
    }

    @Test
    public void parseRatingEmptyIfOldShop() {
        Long2ObjectMap<ShopRatingInfo> map = parse("shop_ratings_statuses.json");

        Matcher<ShopRatingInfo> rating1 = RatingV2Matcher.rating(
            RatingV2Matcher.value(HAS_NO_RATING),
            RatingV2Matcher.status(
                ShopRatingStatusMatcher.id("OLDSHOP"),
                ShopRatingStatusMatcher.name("Магазин выключен больше месяца")
            )
        );
        Matcher<ShopRatingInfo> rating2 = RatingV2Matcher.rating(
            RatingV2Matcher.value(new BigDecimal("4.0")),
            RatingV2Matcher.status(
                ShopRatingStatusMatcher.id("NEWSHOP"),
                ShopRatingStatusMatcher.name("Магазин работает меньше месяца")
            )
        );

        Matcher<ShopRatingInfo> rating3 = RatingV2Matcher.rating(
            RatingV2Matcher.value(HAS_NO_RATING),
            RatingV2Matcher.status(
                ShopRatingStatusMatcher.id("REMOVED"),
                ShopRatingStatusMatcher.name("Рейтинг снят службой качества")
            )
        );

        Matcher<ShopRatingInfo> rating4 = RatingV2Matcher.rating(
            RatingV2Matcher.value(new BigDecimal("4.0")),
            RatingV2Matcher.status(
                ShopRatingStatusMatcher.id("ACTUAL"),
                ShopRatingStatusMatcher.name("Рейтинг нормально рассчитан")
            )
        );

        Matcher<ShopRatingInfo> rating5 = RatingV2Matcher.rating(
            RatingV2Matcher.value(new BigDecimal("4.0")),
            RatingV2Matcher.status(
                ShopRatingStatusMatcher.id("custom_for_test"),
                ShopRatingStatusMatcher.name(null)
            )
        );

        Assert.assertThat(
            map.entrySet(),
            Matchers.containsInAnyOrder(
                ApiMatchers.entry(
                    1L,
                    rating1
                ),
                ApiMatchers.entry(
                    2L,
                    rating2
                ),
                ApiMatchers.entry(
                    3L,
                    rating3
                ),
                ApiMatchers.entry(
                    4L,
                    rating4
                ),
                ApiMatchers.entry(
                    5L,
                    rating5
                )
            )
        );
    }

    private Long2ObjectMap<ShopRatingInfo> parse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
