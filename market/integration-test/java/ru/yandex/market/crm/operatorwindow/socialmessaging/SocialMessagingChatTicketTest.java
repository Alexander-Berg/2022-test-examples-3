package ru.yandex.market.crm.operatorwindow.socialmessaging;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.module.angry.MessageInSmmObject;
import ru.yandex.market.jmf.module.angry.MessageOutSmmObject;
import ru.yandex.market.jmf.module.angry.Ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SocialMessagingChatTicketTest extends AbstractSocialMessagingChatTest {

    private static final String BERU_SOCIAL_MESSAGING_VK_CHAT_SERVICE = "beruSocialMessagingVkChat";
    private static final String BERU_SOCIAL_MESSAGING_OK_CHAT_SERVICE = "beruSocialMessagingOkChat";
    private static final String BERU_SOCIAL_MESSAGING_FACEBOOK_CHAT_SERVICE = "beruSocialMessagingFacebookChat";

    private static Stream<Arguments> twoFirstLevelCommentsFromClientShouldCreateTwoTicketsData() {
        return Stream.of(
                Arguments.of("twoMessagesFromClientShouldAppearInOneTicketVk",
                        "/social_messaging/message/vkTwoMessages.json",
                        VK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_VK_CHAT_SERVICE),
                Arguments.of("twoMessagesFromClientShouldAppearInOneTicketFacebook",
                        "/social_messaging/message/fbTwoMessages.json",
                        FACEBOOK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_FACEBOOK_CHAT_SERVICE),
                Arguments.of("twoMessagesFromClientShouldAppearInOneTicketOk",
                        "/social_messaging/message/okTwoMessages.json",
                        OK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_OK_CHAT_SERVICE)
        );
    }

    private static Stream<Arguments> responseFromUserShouldBeReceivedByAngrySpaceData() {
        return Stream.of(
                Arguments.of("responseFromOperatorShouldBeReceivedByAngrySpaceVk",
                        "/social_messaging/message/vkResponseFromOperator.json",
                        VK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_VK_CHAT_SERVICE),
                Arguments.of("responseFromOperatorShouldBeReceivedByAngrySpaceFacebook",
                        "/social_messaging/message/fbResponseFromOperator.json",
                        FACEBOOK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_FACEBOOK_CHAT_SERVICE),
                Arguments.of("responseFromOperatorShouldBeReceivedByAngrySpaceOk",
                        "/social_messaging/message/okResponseFromOperator.json",
                        OK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_OK_CHAT_SERVICE)
        );
    }


    /**
     * В тесте проверяется сценарий:
     * 1. Пишем в соц.сети личное сообщение - ожидаем, что у нас в системе создастся обращение
     * 2. Отвечаем на это сообщение - ожидаем, что в обращении из прошлого п. появился комментарий
     * <p>
     * тест-кейсы в testPalm:
     * https://testpalm.yandex-team.ru/testcase/ocrm-1478 (Одноклассники)
     * https://testpalm.yandex-team.ru/testcase/ocrm-1476 (Вконтакте)
     * нет (Facebook)
     */
    @MethodSource("twoFirstLevelCommentsFromClientShouldCreateTwoTicketsData")
    @ParameterizedTest(name = "{0}")
    void twoMessagesFromClientShouldAppearInOneTicket(String name,
                                                      String dataPath,
                                                      long accountId,
                                                      String expectedService) throws Exception {
        var smmAccount = utils.getAccount(accountId);
        var dataList = utils.getSmmObjectsListFromFile(dataPath);

        // Клиент написал первое сообщение, ожидаем, что создастся новый тикет
        MessageInSmmObject firstReceivedMessage = verifyTicketCreatedByReceivedSmmObject(
                () -> receiveMessage(dataList.get(0), smmAccount),
                expectedService);
        verifyInboundComment(firstReceivedMessage, "Первое сообщение в чат");

        // Написал второе сообщение, ожидаем, что оно попадёт в то же обращение
        MessageInSmmObject secondReceivedMessage = verifyCommentCreatedInExistedTicketByReceivedObject(
                getTicket(firstReceivedMessage),
                () -> receiveMessage(dataList.get(1), smmAccount));
        verifyInboundComment(secondReceivedMessage, "Второе сообщение в чат");

    }


    /**
     * В тесте проверяется сценарий:
     * 1. Пишем в соц.сети личное сообщение- ожидаем, что у нас в системе создастся обращение
     * 2. В созданном обращении пишем комментарий - ожидаем, что оно отправилось в соц.сеть
     * <p>
     * Тест-кейсы в testPalm:
     * https://testpalm.yandex-team.ru/testcase/ocrm-1482 (Одноклассники)
     * https://testpalm.yandex-team.ru/testcase/ocrm-1483 (Вконтакте)
     * нет (Facebook)
     */
    @MethodSource("responseFromUserShouldBeReceivedByAngrySpaceData")
    @ParameterizedTest(name = "{0}")
    void responseFromOperatorShouldBeReceivedByAngrySpace(String name,
                                                          String dataPath,
                                                          long accountId,
                                                          String expectedService) throws Exception {
        var smmAccount = utils.getAccount(accountId);
        var dataList = utils.getSmmObjectsListFromFile(dataPath);

        // Пользователь написал нам, должны создать первый тикет
        MessageInSmmObject receivedItem = verifyTicketCreatedByReceivedSmmObject(
                () -> receiveMessage(dataList.get(0), smmAccount), expectedService);
        ru.yandex.market.jmf.module.angry.Ticket createdTicket = getTicket(receivedItem);
        verifyInboundComment(receivedItem, "Сообщение в чат");

        // Отвечаем пользователю
        MessageOutSmmObject sentMessage = createCommentAndVerifySentSmmMessage(createdTicket, dataList.get(1));
    }

    /**
     * Тест проверяет создание обращения и комментариев к нему, в случае, когда Оператор не пишет сообщения
     */
    @Test
    @Transactional
    void createCommentsOnTicketWithoutOperatorInteraction() throws Exception {
        var smmAccount = utils.getAccount(VK_ACCOUNT_ID);
        var dataList = utils.getSmmObjectsListFromFile(
                "/social_messaging/socialMessagingSimpleChatDialogData.json");

        dataList.forEach(data -> receiveMessage(data, smmAccount));

        var createdTickets = ticketTestUtils.getAllActiveTickets(Ticket.FQN);

        assertEquals(1, createdTickets.size(), "Все сообщения из одного чата " +
                "должны попадать в одно обращение");
        var createdTicket = createdTickets.get(0);

        var comments = commentTestUtils.getComments(createdTicket);
        assertEquals(3, comments.size());
    }

    /**
     * В тесте проверяется, что эскалация или деэскалация переоткрывает обращение и переводит на соответствующую линию
     * Тест-кейс в testPalm:
     * https://testpalm.yandex-team.ru/testcase/ocrm-1484
     */
    @Test
    void testTicketEscalationDeescalation() throws Exception {
        var smmAccount = utils.getAccount(VK_ACCOUNT_ID);
        var dataList = utils.getSmmObjectsListFromFile(
                "/social_messaging/message/vkEscalationDeescalation.json");

        // Клиент написал первое сообщение, ожидаем, что создастся новый тикет
        MessageInSmmObject firstReceivedMessage = verifyTicketCreatedByReceivedSmmObject(
                () -> receiveMessage(dataList.get(0), smmAccount),
                BERU_SOCIAL_MESSAGING_VK_CHAT_SERVICE);

        Ticket ticket = getTicket(firstReceivedMessage);

        assertCurrentTicketResponsibleTeam(ticket, SOCIAL_MESSAGING_FIRST_LINE);
        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_PROCESSING);
        ticketTestUtils.editTicketStatus(ticket, STATUS_ESCALATED);
        assertCurrentTicketStatus(ticket, Ticket.STATUS_REOPENED);
        assertCurrentTicketResponsibleTeam(ticket, SOCIAL_MESSAGING_SECOND_LINE);

        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_PROCESSING);
        ticketTestUtils.editTicketStatus(ticket, STATUS_DEESCALATED);
        assertCurrentTicketStatus(ticket, Ticket.STATUS_REOPENED);
        assertCurrentTicketResponsibleTeam(ticket, SOCIAL_MESSAGING_FIRST_LINE);
    }

    void assertCurrentTicketStatus(Ticket ticket, String expectedStatus) {
        Ticket actualTicket = storage.get(ticket.getGid());
        assertEquals(expectedStatus, actualTicket.getStatus());
    }

    void assertCurrentTicketResponsibleTeam(Ticket ticket, String expectedResponsibleTeam) {
        Ticket actualTicket = storage.get(ticket.getGid());
        assertEquals(expectedResponsibleTeam, actualTicket.getResponsibleTeam().getCode());
    }
}
