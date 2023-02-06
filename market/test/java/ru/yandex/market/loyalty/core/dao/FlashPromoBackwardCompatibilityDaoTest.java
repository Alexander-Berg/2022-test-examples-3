package ru.yandex.market.loyalty.core.dao;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import ru.yandex.market.loyalty.core.dao.flash.FlashPromoDao;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.sql.Connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


public class FlashPromoBackwardCompatibilityDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private FlashPromoDao flashPromoDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("classpath:sql/flash-promo-backward-compatibility.sql")
    private Resource backwardCompatibilityScript;

    @Before
    public void configure() {
        jdbcTemplate.execute((Connection con) -> {
            ScriptUtils.executeSqlScript(con, backwardCompatibilityScript);
            return null;
        });
    }

    @Test
    public void shouldGetOldFlashEntry() {
        FlashPromoDescription flashPromoDescription = flashPromoDao.get(1001L);
        assertThat(flashPromoDescription, notNullValue());
        assertThat(flashPromoDescription.getFeedId(), comparesEqualTo(123L));
        assertThat(flashPromoDescription.getShopPromoId(), is("some promo"));
        assertThat(flashPromoDescription.getPromoKey(), is("some_promo_key"));
    }
}
