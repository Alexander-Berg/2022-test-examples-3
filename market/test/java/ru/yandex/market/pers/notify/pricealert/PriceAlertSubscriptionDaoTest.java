package ru.yandex.market.pers.notify.pricealert;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.settings.YqlProperties;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 12.12.17
 */
public class PriceAlertSubscriptionDaoTest {
    @Disabled
    @Test
    public void getSubscriptionsWithOnstockModels() {
        PriceAlertSubscriptionDao priceAlertSubscriptionDao = realPriceAlertSubscriptionDao();
        System.out.println(priceAlertSubscriptionDao.getSubscriptionsWithOnstockModels());
    }

    private PriceAlertSubscriptionDao realPriceAlertSubscriptionDao() {
        YqlProperties properties = new YqlProperties();
        properties.setUser("use_your_user_name");
        properties.setPassword("use_your_oauth_token");
        YqlDataSource dataSource = new YqlDataSource("jdbc:yql://yql.yandex.net:443/hahn", properties);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return new PriceAlertSubscriptionDao(jdbcTemplate);
    }
}
