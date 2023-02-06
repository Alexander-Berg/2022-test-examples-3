package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import jdk.jfr.Description;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.ticket.B2bLeadTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.b2bcrm.module.ticket.test.utils.B2bTicketTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.jmf.module.ticket.Ticket.STATUS_PROCESSING;
import static ru.yandex.market.jmf.module.ticket.Ticket.STATUS_RESOLVED;

@B2bTicketTests
public class B2bLeadTicketNoPartnerTest {
    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private B2bTicketTestUtils b2bTicketTestUtils;

    @Inject
    private BcpService bcpService;

    @Test
    @Description("""
            Для обращений b2b лидов партнер не обязателен
            https://testpalm.yandex-team.ru/testcase/ocrm-1396
            """)
    @DisplayName("Проверка корректной смены статуса на resolved")
    public void b2bOutgoingTicketMailSubjectTest() {
        Entity category = ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand());
        Entity partner = bcpService.create(Fqn.of("account$shop"), Map.of(
                "title", "Test Shop",
                "shopId", "111111",
                "emails", List.of("test1@ya.ru"),
                "campaignId", "1000661967"
        ));
        B2bLeadTicket ticket = b2bTicketTestUtils.createB2bLead(
                Map.of(
                        B2bLeadTicket.PARTNER,
                        partner,
                        B2bLeadTicket.CATEGORIES,
                        List.of(category)
                )
        );
        bcpService.edit(ticket, B2bLeadTicket.PARTNER, null);

        ticketTestUtils.editTicketStatus(ticket, STATUS_PROCESSING);
        ticketTestUtils.editTicketStatus(ticket, STATUS_RESOLVED);

        assertThat(ticket.getStatus()).isEqualTo(STATUS_RESOLVED);
    }
}
