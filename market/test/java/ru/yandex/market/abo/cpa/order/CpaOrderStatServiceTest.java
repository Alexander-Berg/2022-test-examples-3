package ru.yandex.market.abo.cpa.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.order.model.CpaOrderStat;
import ru.yandex.market.abo.cpa.order.service.CpaOrderStatRepo;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;

/**
 * @author kukabara
 */
public class CpaOrderStatServiceTest extends EmptyTest {

    @Autowired
    private CpaOrderStatService cpaOrderStatService;
    @Autowired
    private CpaOrderStatRepo cpaOrderStatRepo;

    @Test
    void testSave() {
        CpaOrderStat cpaOrderStat = TestHelper.generateCpaOrderStat(
                1, TestHelper.generateShopId(), true, LocalDateTime.now(), BLUE);
        cpaOrderStatService.save(Collections.singletonList(cpaOrderStat));
        flushAndClear();

        CpaOrderStat dbCpaOrderStat = cpaOrderStatRepo.findByIdOrNull(cpaOrderStat.getOrderId());

        assertEquals(cpaOrderStat.getOrderId(), dbCpaOrderStat.getOrderId());

        cpaOrderStat.setCancelled(NOW);
        cpaOrderStat.setCancelledSubstatus(OrderSubstatus.SHOP_PENDING_CANCELLED);
        cpaOrderStat.setCancelledRole(ClientRole.SHOP);

        cpaOrderStatService.save(Collections.singletonList(cpaOrderStat));
        flushAndClear();
        CpaOrderStat dbCpaOrderStatUpdate = cpaOrderStatRepo.findByIdOrNull(cpaOrderStat.getOrderId());

        assertEquals(cpaOrderStat.getCancelled(), dbCpaOrderStatUpdate.getCancelled());
        assertEquals(cpaOrderStat.getCancelledSubstatus(), dbCpaOrderStatUpdate.getCancelledSubstatus());
        assertEquals(cpaOrderStat.getCancelledRole(), dbCpaOrderStatUpdate.getCancelledRole());
    }

    @Test
    void testFirstOrderSince() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime hourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);
        LocalDateTime halfHourAgo = now.minusMinutes(30);

        List<CpaOrderStat> cpaOrderStatList = new ArrayList<>();
        cpaOrderStatList.add(TestHelper.generateCpaOrderStat(1, 1, true, hourAgo, BLUE));
        cpaOrderStatList.add(TestHelper.generateCpaOrderStat(2, 1, true, now, BLUE));
        cpaOrderStatList.add(TestHelper.generateCpaOrderStat(3, 2, true, twoHoursAgo, BLUE));
        cpaOrderStatList.add(TestHelper.generateCpaOrderStat(4, 2, true, now, BLUE));
        cpaOrderStatList.add(TestHelper.generateCpaOrderStat(5, 3, true, now, BLUE));
        cpaOrderStatList.add(TestHelper.generateCpaOrderStat(6, 4, true, null, BLUE));
        cpaOrderStatService.save(cpaOrderStatList);
    }
}
