package ru.yandex.market.crm.operatorwindow.socialmessaging;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.module.angry.MessageInSmmObject;
import ru.yandex.market.jmf.module.angry.Ticket;
import ru.yandex.market.jmf.timings.test.impl.TimerTestUtils;

public class SocialMessagingChatTicketTimersTest extends AbstractSocialMessagingChatTest {
    @Inject
    private TimerTestUtils timerTestUtils;

    // проверяем работу триггера resolveBeruSocialMessagingChatTicketWhenWaitingResponseTooLong
    // https://testpalm.yandex-team.ru/testcase/ocrm-1544
    @Test
    public void resolveBeruSocialMessagingChatTicketWhenWaitingResponseTooLong() throws Exception {
        String dataPath = "/social_messaging/message/fbResponseFromOperator.json";

        var smmAccount = utils.getAccount(VK_ACCOUNT_ID);
        var dataList = utils.getSmmObjectsListFromFile(dataPath);

        // Пользователь написал нам, должны создать первый тикет
        MessageInSmmObject receivedMessage = receiveMessage(dataList.get(0), smmAccount);
        Ticket ticket = getTicket(receivedMessage);

        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_PROCESSING);
        bcpService.edit(ticket, Map.of(Ticket.CATEGORIES, List.of(testBeruTicketCategory)));
        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_WAITING_RESPONSE);

        // Проверяем, что правильно отрабатывает триггер resolveBeruSocialMessagingChatTicketWhenWaitingResponseTooLong
        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "waitingResponseTimer");
        Ticket actualTicket = storage.get(ticket.getGid());

        Assertions.assertEquals(Ticket.STATUS_RESOLVED, actualTicket.getStatus());
    }
}
