package ru.yandex.market.pers.tms.db;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.db.DbRatingService;
import ru.yandex.market.pers.grade.core.model.core.ShopRating;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 */
public class DbRatingServiceTest extends MockedPersTmsTest {
    private static final double DELTA = 0.000001;

    @Autowired
    private DbRatingService ratingService;

    @Test
    public void loadShopRatingTest() {
        long testShop = -128391;
        pgJdbcTemplate.update(
            "insert into shop_rating_history(" + DbRatingService.SHOP_FIELDS + ") " +
                "values (now(), ?, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)",
            testShop);
        pgJdbcTemplate.update(
            "insert into shop_rating_history(" + DbRatingService.SHOP_FIELDS + ") " +
                "values (now() + interval '1 day', ?, 0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)",
            testShop);
        pgJdbcTemplate.update(
            "insert into shop_rating_history(" + DbRatingService.SHOP_FIELDS + ") " +
                "values (now() - interval '1 day', ?, 0,2.2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)",
            testShop);

        List<ShopRating> shopRatings = ratingService.loadShopRating(
            testShop,
            new Date(new Date().getTime() - TimeUnit.DAYS.toMillis(2)),
            new Date(new Date().getTime() + TimeUnit.DAYS.toMillis(2))
        );

        assertEquals(3, shopRatings.size());

        ShopRating shopRating1 = shopRatings.get(0);
        assertEquals(2.2, shopRating1.getRating(), DELTA);

        ShopRating shopRating2 = shopRatings.get(1);
        assertEquals(0, shopRating2.getRating(), DELTA);

        ShopRating shopRating3 = shopRatings.get(2);
        assertEquals(1, shopRating3.getRating(), DELTA);
    }

    @Test
    public void loadShopRatingRowParsingTest() {
        long testShop = -128391;
        pgJdbcTemplate.update(
            "insert into shop_rating_history(" + DbRatingService.SHOP_FIELDS + ") " +
                "values (now(), ?, 1,2,3,4,5,1,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32)",
            testShop);

        List<ShopRating> shopRatings = ratingService.loadShopRating(
            testShop,
            new Date(new Date().getTime() - TimeUnit.DAYS.toMillis(1)),
            new Date(new Date().getTime() + TimeUnit.DAYS.toMillis(1))
        );

        assertEquals(1, shopRatings.size());

        ShopRating shopRating = shopRatings.get(0);

        assertEquals(1, shopRating.getType());
        assertEquals(2, shopRating.getRating(), DELTA);
        assertEquals(3, shopRating.getRating90Days(), DELTA);
        assertEquals(4, shopRating.getRatingAllTime(), DELTA);
        assertEquals(5, shopRating.getRatingOld(), DELTA);

        assertEquals(15, shopRating.getDist90Days().get(4).intValue());
        assertEquals(26, shopRating.getCountTextUnverified90Days().intValue());
        assertEquals(29, shopRating.getCountTextVerifiedAllTime().intValue());
        assertEquals(32, shopRating.getCountNotextUnverifiedAllTime().intValue());
    }

}
