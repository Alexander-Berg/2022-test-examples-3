package ru.yandex.market.abo.check;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.check.model.ShopCheck;
import ru.yandex.market.abo.check.model.ShopCheckParam;
import ru.yandex.market.abo.check.model.ShopCheckStatus;
import ru.yandex.market.abo.check.model.ShopCheckType;
import ru.yandex.market.abo.util.db.DbUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Ivan Anisimov valter@yandex-team.ru
 * @date 30.06.15
 */
public class ShopCheckServiceTest extends EmptyTest {
    @Autowired
    private ShopCheckService shopCheckService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    private static final int SHOP_ID = 1;
    private static final int GEN_ID = 1;
    private static final int YA_UID = 0;

    @Test
    public void testStoreCheck() {
        ShopCheck check = createShopCheck(ShopCheckType.SHOP_PROBLEM_RECHECK, SHOP_ID, null);
        shopCheckService.storeChecks(Collections.singletonList(check));

        List<ShopCheck> loaded = shopCheckService.getChecksForShopIdByTypeAndStatus(
                SHOP_ID, ShopCheckType.SHOP_PROBLEM_RECHECK, ShopCheckStatus.NEW);
        assertEquals(loaded.size(), 1);
    }

    @Test
    public void testStoreCheckWithParam() {
        final ShopCheckParam param = ShopCheckParam.PROMO_CODE;
        ShopCheck check = createShopCheck(
                ShopCheckType.SHOP_PROBLEM_RECHECK, SHOP_ID, new HashMap<>() {
                    {
                        put(param.name(), "value");
                    }
                });
        shopCheckService.storeChecks(Collections.singletonList(check));

        List<ShopCheck> loaded = shopCheckService.getChecksForShopIdByTypeAndStatus(
                SHOP_ID, ShopCheckType.SHOP_PROBLEM_RECHECK, ShopCheckStatus.NEW);
        assertEquals(loaded.size(), 1);
        assertEquals(loaded.get(0).getParam(param), "value");
    }

    @Test
    public void testGetNewChecksForGenerator() {
        insertShopCheck(ShopCheckType.SHOP_PROBLEM_RECHECK, SHOP_ID, false);
        insertShopCheck(ShopCheckType.SHOP_PROBLEM_RECHECK, SHOP_ID, false);
        insertShopCheck(ShopCheckType.SHOP_PROBLEM_RECHECK, SHOP_ID, true); // old
        List<ShopCheck> loaded = shopCheckService.getChecksForShopIdByTypeAndStatus(
                SHOP_ID, ShopCheckType.SHOP_PROBLEM_RECHECK, ShopCheckStatus.NEW);
        assertEquals(loaded.size(), 3);

        insertHyp(-102, loaded.get(0).getId(), GEN_ID, true); // hyp is failed -> chop check will be return
        insertHyp(-103, loaded.get(1).getId(), GEN_ID, false); // hyp is success -> chop check will not be return
        List<ShopCheck> checksForGen = shopCheckService.getNewChecksForGenerator(
                ShopCheckType.SHOP_PROBLEM_RECHECK, GEN_ID);
        assertEquals(1, checksForGen.size());
        assertEquals(loaded.get(0).getId(), checksForGen.get(0).getId());
    }

    @Test
    public void testGetCheckById() {
        long id = insertShopCheck(ShopCheckType.SHOP_PROBLEM_RECHECK, SHOP_ID, false);
        ShopCheck shopCheck = shopCheckService.getCheckById(id);
        assertNotNull(shopCheck);
    }

    @Test
    public void testUpdateCheckStatus() {
        int id = insertShopCheck(ShopCheckType.SHOP_PROBLEM_RECHECK, SHOP_ID, null);
        shopCheckService.updateCheckStatus(id, ShopCheckStatus.FINISHED, YA_UID);
        List<ShopCheck> loaded = shopCheckService.getChecksForShopIdByTypeAndStatus
                (SHOP_ID, ShopCheckType.SHOP_PROBLEM_RECHECK, ShopCheckStatus.FINISHED);
        assertEquals(loaded.size(), 1);
        assertEquals(loaded.get(0).getId(), id);
    }

    private long insertShopCheck(ShopCheckType type, long shopId, boolean old) {
        Date now = new Date(System.currentTimeMillis());
        Date date = old ? DateUtils.addDays(now, -100) : now;
        long id = DbUtils.getNextSequenceValuePg(pgJdbcTemplate, "s_shop_check");
        pgJdbcTemplate.update("INSERT INTO shop_check " +
                        "(id, type_id, shop_id, status_id, creation_time, modification_time, creation_uid, modification_uid, source_id, text_comment) " +
                        "VALUES (?, ?, ?, 0, ?, ?, 0, 0, -1, NULL)",
                id, type.getId(), shopId, date, date);
        return id;
    }

    private void insertHyp(int id, int sourceId, int genId, boolean failed) {
        pgJdbcTemplate.update("INSERT INTO hypothesis (id, shop_id, source_id, gen_id, failed)" +
                        "VALUES (?, ?, ?, ?, ?)",
                id, SHOP_ID, sourceId, genId, failed);
    }

    private int insertShopCheck(ShopCheckType type, long shopId, Map<String, String> params) {
        ShopCheck shopCheck = createShopCheck(type, shopId, params);
        shopCheckService.storeChecks(Collections.singletonList(shopCheck));
        shopCheck = shopCheckService.getChecksForShopIdByTypeAndStatus(SHOP_ID, type, ShopCheckStatus.NEW).get(0);
        return shopCheck.getId();
    }

    private static ShopCheck createShopCheck(ShopCheckType type, long shopId, Map<String, String> params) {
        return new ShopCheck(type, shopId, params, YA_UID, YA_UID, null);
    }
}
