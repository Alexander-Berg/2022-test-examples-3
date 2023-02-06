package ru.yandex.market.abo.core.rating;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.rating.RatingRemovePeriod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author imelnikov
 * @since 24.08.2020
 */
public class RatingRemoveServiceTest extends EmptyTest {

    @Autowired
    RatingRemoveService ratingRemoveService;

    @Test
    public void infiniteRating() {
        long shopId = 155L;
        ratingRemoveService.removeShopRating(shopId, "text", true);
        RatingRemovePeriod period = ratingRemoveService.loadRemovePeriod(shopId);
        assert(period.getFinishTime().after(new Date()));

        assertFalse(ratingRemoveService.deleteShopRatingRemove(shopId));
        assertTrue(ratingRemoveService.deleteRatingRemoveByShop(shopId));
    }
}
