package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.Collections;

import javax.inject.Inject;

import jdk.jfr.Description;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.ticket.B2bOutgoingTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@B2bTicketTests
public class B2bTicketCommonTest {

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Test
    @Description("""
            В исходящем B2B обращении тема письма равна теме обращения
            https://testpalm.yandex-team.ru/testcase/ocrm-1514
            """)
    public void b2bOutgoingTicketMailSubjectTest() {
        B2bOutgoingTicket ticket = ticketTestUtils.createTicket(B2bOutgoingTicket.FQN, Collections.emptyMap());
        assertThat(ticket.getMailSubject()).isEqualTo(ticket.getTitle());
    }
}
