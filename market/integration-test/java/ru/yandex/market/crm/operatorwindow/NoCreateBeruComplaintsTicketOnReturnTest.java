package ru.yandex.market.crm.operatorwindow;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.ocrm.module.complaints.BeruComplaintsTicket;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

@Transactional
public class NoCreateBeruComplaintsTicketOnReturnTest extends AbstractModuleOwTest {

    @Inject
    private DbService dbService;

    @Inject
    private OrderTestUtils orderTestUtils;

    @Test
    public void testNoOrderReturnId() {
        Order order = orderTestUtils.createOrder(Map.of(Order.NUMBER, Randoms.longValue()));
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.USER, HistoryEventType.ORDER_RETURN_CREATED);
        assertNoBeruComplaintsTickets();
    }

    @Test
    public void testOrderReturnNotFound() {
        Order order = orderTestUtils.createOrder(Map.of(Order.NUMBER, Randoms.longValue()));
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.USER, HistoryEventType.ORDER_RETURN_CREATED,
                Randoms.longValue());
        assertNoBeruComplaintsTickets();
    }

    private void assertNoBeruComplaintsTickets() {
        Assertions.assertFalse(dbService.any(Query.of(BeruComplaintsTicket.FQN)));
    }
}
