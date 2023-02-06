package ru.yandex.market.pers.grade.core.shop;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.framework.db.DataRow;
import ru.yandex.common.framework.filter.FilterConst;
import ru.yandex.market.pers.grade.core.MockedTest;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 */
public class DbShopInfoServiceTest extends MockedTest {
    private static List<Long> TEST_SHOP_IDS = new ArrayList<>();

    @Autowired
    private DbShopInfoService shopInfoService;

    static {
        TEST_SHOP_IDS.add(3393L);
        TEST_SHOP_IDS.add(1743L);
        TEST_SHOP_IDS.add(774L);
        TEST_SHOP_IDS.add(774L);
    }

    @Test
    public void testGetPublicShopInfo() throws Exception {
        pgJdbcTemplate.update("insert into ext_mbi_datasource(shop_id, name, domain) values(?,?,?)",
            720, "pleer.ru", "pleer.ru");

        for (Long shopId : List.of(720L)) {
            DataRow dataRow = shopInfoService.getPublicShopInfo(shopId);
            StringBuilder buf = new StringBuilder();
            dataRow.toXml(buf);
            System.out.println(buf.toString());
        }
    }

    @Test
    public void testGetShopInfo() throws Exception {
        Timestamp startPeriod = new Timestamp(System.currentTimeMillis() - 7 * FilterConst.MILLISECONDS_IN_DAY);
        for (Long shopId : TEST_SHOP_IDS) {
            OldShopInfo oldShopInfo = shopInfoService.getShopInfo(shopId, startPeriod);
            StringBuilder buf = new StringBuilder();
            oldShopInfo.toXml(buf);
            System.out.println(buf.toString());
        }
    }

}
