package ru.yandex.market.jmf.module.ticket.test;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.def.Contact;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketContactInComment;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
@SpringJUnitConfig(classes = ModuleTicketTestConfiguration.class)
public class TicketContactInCommentTest {

    @Inject
    private BcpService bcpService;

    @Inject
    private TicketTestUtils ticketTestUtils;

    /**
     * Проверка создания контакта во время создания комментария от партнера
     * {@link ru.yandex.market.jmf.module.ticket.operations.SetContactForTicketContactInComment}
     */
    @Test
    public void createContactWhenCreateComment() {
        Ticket ticket = ticketTestUtils.createTicket(TicketTestConstants.TICKET_TEST_FQN, Map.of());
        TicketContactInComment comment = bcpService.create(TicketContactInComment.FQN, Map.of(
                TicketContactInComment.ENTITY, ticket,
                TicketContactInComment.BODY, Randoms.string(),
                TicketContactInComment.CONTACT, Map.of(
                        Contact.TITLE, Randoms.string()
                )
        ));
        assertNotNull(comment.getContact());
        assertEquals(ticket.getGid(), comment.getContact().getParent());
    }
}
