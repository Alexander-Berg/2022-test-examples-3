package ru.yandex.market.abo.core.shopdata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.EmptyTest;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 *         @date 07.04.2008
 */
@Transactional("pgTransactionManager")
public class ComposeShopDataManagerTest extends EmptyTest {

    @Autowired
    private ComposeShopDataManager composeShopDataManager;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    public void testUpdateShop() {
        long shopId = -3;
        pgJdbcTemplate.update("INSERT INTO shop (id, domain) VALUES (?, 'yandex.ru')", shopId);
        pgJdbcTemplate.update("INSERT INTO ext_campaign_info(campaign_id, shop_id) VALUES (1, ?)", shopId);
        pgJdbcTemplate.update("INSERT INTO ext_organization_info (datasource_id) VALUES (?)", shopId);
        composeShopDataManager.updateShop(shopId);
        pgJdbcTemplate.update("DELETE FROM shop");
        pgJdbcTemplate.update("DELETE FROM ext_campaign_info");
        pgJdbcTemplate.update("DELETE FROM ext_organization_info");
    }
}
