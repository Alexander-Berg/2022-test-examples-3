package ru.yandex.market.abo.core.export.rating;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.rating.RatingMode;

/**
 * @author artemmz
 * @date 28.06.18.
 */
public abstract class RatingCalculationCurrentTest extends EmptyTest {
    static final long SHOP_ID = 774L;
    static final int RATING = 5;
    static final RatingMode INITIAL_R_MODE = RatingMode.NEWSHOP;
    static final int INITIAL_R_MODE_ID = RatingMode.NEWSHOP.getId();
    static final Date R_DATE = new Date();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUpRatingCalculation() {
        jdbcTemplate.update("insert into shop(id, cpc, cpa) values (?, 'OFF', 'OFF')", SHOP_ID);
        jdbcTemplate.update("insert into rating(eventtime, shop_id, calc_time, rating) " +
                " values (?, ?, ?, ?)", R_DATE, SHOP_ID, R_DATE, RATING);
        jdbcTemplate.update("insert into rating_calculation (eventtime, shop_id, rating, total, rating_mode) " +
                "values (?, ?, ?, ?, ?)", R_DATE, SHOP_ID, RATING, RATING, INITIAL_R_MODE_ID);
    }
}
