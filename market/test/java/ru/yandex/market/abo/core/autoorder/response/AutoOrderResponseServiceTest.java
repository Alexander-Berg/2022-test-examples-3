package ru.yandex.market.abo.core.autoorder.response;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.ticket.TicketType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.abo.core.autoorder.AutoOrderManager.MINUTES_DELAY;

/**
 * @author antipov93.
 */
public class AutoOrderResponseServiceTest extends EmptyTest {

    @Autowired
    private AutoOrderResponseService autoOrderResponseService;

    @Autowired
    private AutoOrderResponseRepo autoOrderResponseRepo;

    @Test
    public void testAdd() {
        long hypId = 1;
        autoOrderResponseService.add(hypId, TicketType.CORE);
        flushAndClear();

        AutoOrderResponse loaded = autoOrderResponseService.load(hypId);
        assertEquals(hypId, loaded.getHypId());
        assertNotNull(loaded.getCreationTime());
        assertNull(loaded.getReceiveTime());
        assertEquals(AutoOrderStatus.PROCESSING, loaded.getStatus());
    }

    @Test
    public void testLoadLastWeek() {
        var created = LocalDateTime.now().minusMinutes(MINUTES_DELAY);
        var weekAgo = created.minusDays(8);

        autoOrderResponseRepo.save(createOrder(1, created, AutoOrderStatus.PROCESSING));
        autoOrderResponseRepo.save(createOrder(2, weekAgo, AutoOrderStatus.PROCESSING));
        autoOrderResponseRepo.save(createOrder(3, created, AutoOrderStatus.SUCCESS));
        autoOrderResponseRepo.save(createOrder(4, created, AutoOrderStatus.FAILED));

        List<AutoOrderResponse> lastWeek = autoOrderResponseService.loadTicketsForCheck();
        assertEquals(1, lastWeek.size());
        assertEquals(1, lastWeek.get(0).getHypId());
    }

    private static AutoOrderResponse createOrder(long hypId, LocalDateTime creationTime, AutoOrderStatus status) {
        var order = new AutoOrderResponse(hypId, TicketType.CORE);
        order.setCreationTime(creationTime);
        order.setStatus(status);
        return order;
    }
}
