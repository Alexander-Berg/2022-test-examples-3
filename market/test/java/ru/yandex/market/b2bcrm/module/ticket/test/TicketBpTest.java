package ru.yandex.market.b2bcrm.module.ticket.test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Description;

import ru.yandex.market.b2bcrm.module.business.process.Bp;
import ru.yandex.market.b2bcrm.module.business.process.BpState;
import ru.yandex.market.b2bcrm.module.business.process.BpStatus;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.b2bcrm.module.ticket.test.utils.BpTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;

@B2bTicketTests
public class TicketBpTest {
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private BpTestUtils bpTestUtils;

    @Test
    @Description("""
            Проверяем дату заполнения при создании тикета, ее изменение при переходе в другой статус и возможность
            выполнить перевод в другой статус
            https://testpalm.yandex-team.ru/testcase/ocrm-1522
            """)
    public void checkFillBpStartStatusDatetime() {
        BpStatus startStatus = bpTestUtils.createBpStatus();
        BpStatus secondStatus = bpTestUtils.createBpStatus();
        BpStatus thirdStatus = bpTestUtils.createBpStatus();

        BpState firstState = bpTestUtils.createBpState(startStatus, Arrays.asList(secondStatus, thirdStatus));
        BpState secondState = bpTestUtils.createBpState(secondStatus, Arrays.asList(thirdStatus));

        Bp bp = bpTestUtils.createBp("bp", Arrays.asList(firstState, secondState));

        B2bTicket ticket = ticketTestUtils.createTicket(B2bTicket.FQN, Map.of(
                B2bTicket.TITLE, "original title",
                B2bTicket.BP, bp,
                B2bTicket.CURRENT_STATUS, startStatus
        ));

        OffsetDateTime time1 = ticket.getBpStartStatusDatetime();
        Assertions.assertNotNull(time1, "Дата старта в статусе бизнес-процесса не пуста");

        bcpService.edit(ticket, Map.of(
                B2bTicket.CURRENT_STATUS, secondStatus
        ));
        OffsetDateTime time2 = ticket.getBpStartStatusDatetime();

        Assertions.assertTrue(time2.isAfter(time1), "Новая дата перехода в статус больше предыдущей");
    }

    @Test
    @Description("Проверяем что при изменении статуса на такой же время входа в статус не меняется")
    public void checkBpStartStatusTimeNotChangedWheStatusNotChanged() {
        BpStatus status = bpTestUtils.createBpStatus();
        BpState state = bpTestUtils.createBpState(status, List.of(status));
        Bp bp = bpTestUtils.createBp("bp", List.of(state));

        B2bTicket ticket = ticketTestUtils.createTicket(B2bTicket.FQN, Map.of(
                B2bTicket.TITLE, "title",
                B2bTicket.BP, bp,
                B2bTicket.CURRENT_STATUS, status
        ));
        OffsetDateTime time = ticket.getBpStartStatusDatetime();
        Assertions.assertNotNull(time, "Дата старта в статусе бизнес-процесса не пуста");

        bcpService.edit(ticket, Map.of(B2bTicket.CURRENT_STATUS, status));
        Assertions.assertEquals(time, ticket.getBpStartStatusDatetime());
    }
}
