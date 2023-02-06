package ru.yandex.market.abo.core.outlet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.outlet.model.Outlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author dstroganova, kukabara
 */
public class OutletManagerIntegrationTest extends EmptyTest {
    private static final long SHOP_ID = 774L;
    private static final long OUTLET_ID = 419546L;
    private static final long UID = 1L;
    @Autowired
    private OutletManager outletManager;

    @Test
    public void testLoadOutletByShop() {
        Page<Outlet> outlets = outletManager.loadOutletInfo(SHOP_ID, null, null, PageRequest.of(1, 10));
        assertTrue(outlets.iterator().hasNext());
    }

    @Test
    public void testLoadOutletById() {
        Outlet outlet = outletManager.loadOutletInfo(OUTLET_ID);
        assertFalse(outlet == null);
    }

    @Test
    @Disabled
    public void testTurnOffOutlets() {
        Page<Outlet> outlets = outletManager.loadOutletInfo(SHOP_ID, null, null, PageRequest.of(1, 10));
        assertNotNull(outlets);
        assertTrue(outlets.iterator().hasNext());
        List<Long> billingOutletIds = Arrays.asList(outlets.iterator().next().getBillingId());
        Map<Integer, String> reason2CommentMap = new HashMap<>();
        reason2CommentMap.put(1, "asbasd");
        reason2CommentMap.put(2, "asxzcv asbasd");
        outletManager.turnOffOutlets(billingOutletIds, "Subject for shop", "Body for shop", UID,
                "Comment for ABO", reason2CommentMap);
    }
}
