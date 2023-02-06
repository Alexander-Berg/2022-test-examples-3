package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.Map;

import javax.inject.Inject;

import jdk.jfr.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.ticket.B2bTelephony;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.b2bcrm.module.ticket.test.utils.B2bTicketTestUtils;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.telephony.voximplant.TelephonyService;
import ru.yandex.market.jmf.timings.test.impl.TimerTestUtils;

@B2bTicketTests
public class B2bTelephonyTest {

    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private TimerTestUtils timerTestUtils;
    @Inject
    private B2bTicketTestUtils b2bTicketTestUtils;

    private TelephonyService defaultTelephonyService;

    @BeforeEach
    public void setUp() {
        defaultTelephonyService = b2bTicketTestUtils.createTelephonyService("b2bTelephonyShop");
    }

    @Test
    @Description("""
            Проверяем, что по истечению таймера, обращение будет закрыто
            https://testpalm.yandex-team.ru/testcase/ocrm-1525
            """)
    public void b2bOutgoingTicketMailSubjectTest() {
        B2bTelephony ticket = ticketTestUtils.createTicket(B2bTelephony.FQN, Map.of(
                B2bTelephony.TITLE, "original title",
                B2bTelephony.SERVICE, defaultTelephonyService
        ));
        ticketTestUtils.editTicketStatus(ticket, "resolved");

        timerTestUtils.simulateTimerExpiration(ticket.getGid(), B2bTelephony.ALLOWANCE_CLOSE_TIMER);

        Assertions.assertEquals("closed", ticket.getStatus(), "Статус тикета должен быть closed");
    }
}
