package ru.yandex.market.abo.core.shop;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.mbi.api.client.entity.shops.ProgramState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 * @date 12.03.2008
 */
public class ShopInfoServiceTest extends EmptyTest {

    private static final long SHOP_ID = RND.nextInt();
    private static final String SHOP_NAME = "test.yandex.ru";

    @Autowired
    private ShopInfoService shopInfoService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        pgJdbcTemplate.update("INSERT INTO shop (id, name, cpc) VALUES (?, ?, 'ON')", SHOP_ID, SHOP_NAME);
        pgJdbcTemplate.update("INSERT INTO ext_campaign_info (campaign_id, shop_id) VALUES (1, ?)", SHOP_ID);
        pgJdbcTemplate.update("INSERT INTO ext_organization_info (datasource_id) VALUES (?)", SHOP_ID);
    }

    @Test
    public void testLoadShopInfo() {
        ShopInfo shopInfo = shopInfoService.loadShopInfo(SHOP_NAME);
        assertEquals(SHOP_NAME, shopInfo.getName());
        assertEquals(SHOP_ID, shopInfo.getId());

        assertTrue(shopInfoService.getShopInfoOptional(SHOP_ID).isPresent());
        assertFalse(shopInfoService.getShopInfoOptional(SHOP_ID - 1).isPresent());
    }

    @Test
    public void updateShopInfo() {
        ShopInfo shopInfo = shopInfoService.loadShopInfo(SHOP_NAME);
        assertSame(ProgramState.ON, shopInfo.getCpc());

        shopInfoService.updateShop(SHOP_ID, ProgramState.OFF);

        shopInfo = shopInfoService.loadShopInfo(SHOP_NAME);
        assertSame(ProgramState.OFF, shopInfo.getCpc());
    }

    @Test
    public void testGetShopCountries() {
        assertTrue(shopInfoService.getShopCountries(Collections.emptyList()).isEmpty());

        long regionId = 213L;
        long countryId = 225;
        pgJdbcTemplate.update("INSERT INTO ext_shop_region (datasource_id, region_id, country_id) " +
                "VALUES (?, ?, ?)", SHOP_ID, regionId, countryId);
        pgJdbcTemplate.update("INSERT INTO region (id, parent_id) VALUES (?, -1)", regionId);
        pgJdbcTemplate.update("INSERT INTO region (id, parent_id) VALUES (?, -1)", countryId);
        Map<Long, Long> shopCountries = shopInfoService.getShopCountries(Collections.singleton(SHOP_ID));
        assertEquals(1, shopCountries.size());
        assertEquals(countryId, shopCountries.get(SHOP_ID).longValue());
    }

}
