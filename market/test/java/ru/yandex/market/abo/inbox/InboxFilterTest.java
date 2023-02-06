package ru.yandex.market.abo.inbox;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.abo.core.inbox.InboxFilterService;
import ru.yandex.market.abo.core.queue.entity.QueueTicket;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

/**
 * @author Ivan Anisimov valter@yandex-team.ru
 * @date 20.12.2013
 */
public class InboxFilterTest {
    private static final long SHOP_ID = 774L;
    private static final long REGION_ID = Regions.RUSSIA;
    private int testCount = 1000;
    private Random rnd = new Random(System.currentTimeMillis());
    private Long userId = 1L;

    @Test
    public void testAllFilter() throws Exception {
        InboxFilterService externalServices = Mockito.mock(InboxFilterService.class);
        InboxFilter.setInboxFilterService(externalServices);

        for (int i = 0; i < testCount; i++) {
            CheckMethod checkMethod = CheckMethod.get(rnd.nextInt(5));
            QueueTicket queueTicket = new QueueTicket(i, -1, SHOP_ID, checkMethod, REGION_ID);
            assertTrue(InboxFilter.ALL.getTicketFilter().accept(queueTicket, userId));
        }
    }

    @Test
    public void testWithPhoneCheckFilter() throws Exception {
        Set<CheckMethod> ALLOWED_CHECK_METHODS = new HashSet<>(
                Arrays.asList(
                        CheckMethod.DEFAULT,
                        CheckMethod.PHONE)
        );

        InboxFilterService externalServices = Mockito.mock(InboxFilterService.class);
        doReturn(true).when(externalServices).isWorkingTime(anyLong());
        InboxFilter.setInboxFilterService(externalServices);

        for (int i = 0; i < testCount; i++) {
            CheckMethod checkMethod = CheckMethod.get(rnd.nextInt(5));
            QueueTicket queueTicket = new QueueTicket(i, -1, SHOP_ID, checkMethod, REGION_ID);
            assertEquals(ALLOWED_CHECK_METHODS.contains(checkMethod),
                    InboxFilter.WITH_PHONE_CHECK.getTicketFilter().accept(queueTicket, userId));
        }
    }

    @Test
    public void testWithDelivery() throws Exception {
        InboxFilterService externalServices = Mockito.mock(InboxFilterService.class);
        InboxFilter.setInboxFilterService(externalServices);
        Random rnd = new Random(System.currentTimeMillis());
        for (int i = 0; i < testCount; i++) {
            boolean withDelivery = rnd.nextBoolean();
            Mockito.when(externalServices.ticketCheckedWithDelivery(i)).thenReturn(withDelivery);

            QueueTicket queueTicket = new QueueTicket(i, -1, SHOP_ID, CheckMethod.DEFAULT, REGION_ID);
            assertEquals(withDelivery, InboxFilter.WITH_DELIVERY.getTicketFilter().accept(queueTicket, userId));
        }
    }
}
