package ru.yandex.market.abo.core.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.ticket.model.TicketTag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author artemmz
 * @date 28.08.17.
 */
public class SimpleTicketTagServiceTest extends EmptyTest {
    @Autowired
    private SimpleTicketTagService simpleTicketTagService;

    @Test
    public void testTags() throws Exception {
        long yaUid = -1;

        TicketTag ticketTag =  simpleTicketTagService.createTag(yaUid);
        assertEquals(ticketTag.getYaUid(), yaUid);

        simpleTicketTagService.deleteTag(ticketTag);
        assertNull(simpleTicketTagService.loadTag(ticketTag.getId()));
    }
}