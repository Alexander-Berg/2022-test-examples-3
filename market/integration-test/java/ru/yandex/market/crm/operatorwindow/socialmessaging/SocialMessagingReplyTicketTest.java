package ru.yandex.market.crm.operatorwindow.socialmessaging;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.module.angry.ItemInSmmObject;
import ru.yandex.market.jmf.module.angry.ItemOutSmmObject;
import ru.yandex.market.jmf.module.angry.Ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SocialMessagingReplyTicketTest extends AbstractSocialMessagingReplyTest {

    private static final String BERU_SOCIAL_MESSAGING_VK_REPLY_SERVICE = "beruSocialMessagingVkReply";
    private static final String BERU_SOCIAL_MESSAGING_FACEBOOK_REPLY_SERVICE = "beruSocialMessagingFacebookReply";
    private static final String BERU_SOCIAL_MESSAGING_OK_REPLY_SERVICE = "beruSocialMessagingOkReply";

    private static Stream<Arguments> twoFirstLevelCommentsFromClientShouldCreateTwoTicketsData() {
        return Stream.of(
                Arguments.of("twoFirstLevelCommentsFromClientShouldCreateTwoTicketsFacebook",
                        "/social_messaging/item/fbTwoFirstLevelComments.json",
                        FACEBOOK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_FACEBOOK_REPLY_SERVICE),
                Arguments.of("twoFirstLevelCommentsFromClientShouldCreateTwoTicketsOk",
                        "/social_messaging/item/okTwoFirstLevelComments.json",
                        OK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_OK_REPLY_SERVICE),
                Arguments.of("twoFirstLevelCommentsFromClientShouldCreateTwoTicketsVk",
                        "/social_messaging/item/vkTwoFirstLevelComments.json",
                        VK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_VK_REPLY_SERVICE)
        );
    }

    private static Stream<Arguments> responseFromUserShouldBeReceivedByAngrySpaceData() {
        return Stream.of(
                Arguments.of("responseFromUserShouldBeReceivedByAngrySpaceFacebook",
                        "/social_messaging/item/fbResponseFromOperator.json",
                        FACEBOOK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_FACEBOOK_REPLY_SERVICE),
                Arguments.of("responseFromUserShouldBeReceivedByAngrySpaceOk",
                        "/social_messaging/item/okResponseFromOperator.json",
                        OK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_OK_REPLY_SERVICE),
                Arguments.of("responseFromUserShouldBeReceivedByAngrySpaceVk",
                        "/social_messaging/item/vkResponseFromOperator.json",
                        VK_ACCOUNT_ID,
                        BERU_SOCIAL_MESSAGING_VK_REPLY_SERVICE)
        );
    }

    /**
     * Тест проверяет правильность простого диалога между клиентом и оператором через Facebook
     * <p>
     * Нет тест-кейса в testPalm
     */
    @Test
    void facebookDialogWithCorrectRepliesWithInvocation() throws Exception {
        var smmAccount = utils.getAccount(FACEBOOK_ACCOUNT_ID);
        var dataList = utils.getSmmObjectsListFromFile(
                "/social_messaging/socialMessagingSimpleFacebookReplyDialogData.json");

        // Пользователь написал нам, должны создать тикет
        ItemInSmmObject receivedItem = verifyTicketCreatedByReceivedSmmObject(
                () -> receiveAndVerifyItem(dataList.get(0), smmAccount), BERU_SOCIAL_MESSAGING_FACEBOOK_REPLY_SERVICE);
        Ticket createdTicket = getTicket(receivedItem);

        // Ответим пользователю
        createCommentAndVerifySentSmmItem(createdTicket, dataList.get(1));

        // Пользователь отвечает нам
        ItemInSmmObject receivedItemTwo = verifyCommentCreatedInExistedTicketByReceivedObject(createdTicket,
                () -> receiveAndVerifyItem(dataList.get(2), smmAccount));
    }

    /**
     * Тест провряет правильность форматрирования комментария с призывом пользователя (пример на facebook, angry
     * .space должен выводить примерно в едином формате)
     * <p>
     * Нет тест-кейса в testPalm
     */
    @Test
    void commentWithInvocationHasCorrectFormatting() throws Exception {
        var smmAccount = utils.getAccount(FACEBOOK_ACCOUNT_ID);
        var content = utils.getFirstSmmObjectFromFile(
                "/social_messaging/socialMessagingSingleFacebookReplyWithInvocation.json");
        var receivedItem = verifyTicketCreatedByReceivedSmmObject(
                () -> receiveAndVerifyItem(content, smmAccount), BERU_SOCIAL_MESSAGING_FACEBOOK_REPLY_SERVICE);
        verifyInboundComment(receivedItem,
                "<a href=\"https://sba.yandex.net/redirect?url&#61;https%3A%2F%2Fwww.facebook.com%2F102191528673262",
                ">Madbunny software</a> Будем жить и процветать!<br />Добро пожаловать!");

    }

    /**
     * В тесте проверяется сценарий:
     * 1. Пишем в соц.сети к посту комментарий первого уровня - ожидаем, что у нас в системе создастся обращение
     * 2. Отвечаем на этот комментарий первого уровня - ожидаем, что в обращении из прошлого п. появился комментарий
     * 3. Пишем в соц.сети к тому же посту другой комментарий первого уровня - ожидаем, что создастся новое обращение
     * <p>
     * тест-кейсы в testPalm:
     * https://testpalm.yandex-team.ru/testcase/ocrm-1479 (Одноклассники)
     * https://testpalm.yandex-team.ru/testcase/ocrm-1477 (Вконтакте)
     * нет (Facebook)
     */
    @MethodSource("twoFirstLevelCommentsFromClientShouldCreateTwoTicketsData")
    @ParameterizedTest(name = "{0}")
    void twoFirstLevelCommentsFromClientShouldCreateTwoTickets(String name,
                                                               String dataPath,
                                                               long accountId,
                                                               String expectedService) throws Exception {
        var smmAccount = utils.getAccount(accountId);
        var dataList = utils.getSmmObjectsListFromFile(dataPath);

        // Пользователь написал нам, должны создать первый тикет
        ItemInSmmObject receivedItem = verifyTicketCreatedByReceivedSmmObject(
                () -> receiveAndVerifyItem(dataList.get(0), smmAccount), expectedService);
        Ticket createdTicket = getTicket(receivedItem);
        verifyInboundComment(receivedItem, "Первый комментарий первого уровня к посту");

        // Пользователь пишет в новом комменте первого уровня, должны в тот же тикет ответить
        ItemInSmmObject receivedItemTwo = verifyCommentCreatedInExistedTicketByReceivedObject(createdTicket,
                () -> receiveAndVerifyItem(dataList.get(1), smmAccount));
        verifyInboundComment(receivedItemTwo, "Ответ на комментарий первого уровня");

        // Пользователь написал нам еще один комментарий первого уровня, должны создать другой тикет
        ItemInSmmObject receivedItemThree = verifyTicketCreatedByReceivedSmmObject(
                () -> receiveAndVerifyItem(dataList.get(2), smmAccount), expectedService);
        assertTicketCount(2);
        verifyInboundComment(receivedItemThree, "Еще один комментарий первого уровня");
    }

    /**
     * В тесте проверяется сценарий:
     * 1. Пишем в соц.сети к посту комментарий первого уровня - ожидаем, что у нас в системе создастся обращение
     * 2. В созданном обращении пишем комментарий - ожидаем, что оно отправилось в соц.сеть
     * <p>
     * Тест-кейсы в testPalm:
     * https://testpalm.yandex-team.ru/testcase/ocrm-1481 (Одноклассники)
     * https://testpalm.yandex-team.ru/testcase/ocrm-1480 (Вконтакте)
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
        ItemInSmmObject receivedItem = verifyTicketCreatedByReceivedSmmObject(
                () -> receiveAndVerifyItem(dataList.get(0), smmAccount), expectedService);
        Ticket createdTicket = getTicket(receivedItem);
        verifyInboundComment(receivedItem, "Комментарий первого уровня к посту");

        // Отвечаем пользователю
        ItemOutSmmObject sentItem = createCommentAndVerifySentSmmItem(createdTicket, dataList.get(1));
    }

    /**
     * В тесте проверяется, что по новому созданному посту в соц.сети не создается обращение
     * <p>
     * Нет тест-кейса в testPalm
     */
    @Test
    @Transactional
    void dontCreateTicketByNewPost() throws Exception {
        var smmAccount = utils.getAccount(FACEBOOK_ACCOUNT_ID);
        var dataList = utils.getSmmObjectsListFromFile(
                "/social_messaging/socialMessagingSimpleReplyDialogData.json");
        var postData = dataList.get(0);

        receiveAndVerifyItem(postData, smmAccount);
        assertTicketCount(0);
    }

    /**
     * Тест проверяет, что комментарий первого уровня и ответы разного уровня на этот комментарий попадают
     * в одно и то же обращение
     * <p>
     * Нет тест-кейса в testPalm
     */
    @Test
    @Transactional
    void createCommentsOnTicketWithoutOperatorInteraction() throws Exception {
        var smmAccount = utils.getAccount(FACEBOOK_ACCOUNT_ID);
        var dataList = utils.getSmmObjectsListFromFile(
                "/social_messaging/socialMessagingSimpleReplyDialogData.json");

        dataList.forEach(data -> receiveAndVerifyItem(data, smmAccount));

        //Все сообщения - комментарий первого уровня и ответы на этот комментарий должны попадать в одно обращение
        var createdTicket = ticketTestUtils.getSingleOpenedTicket(Ticket.FQN);
        var comments = commentTestUtils.getComments(createdTicket);
        assertEquals(5, comments.size());
    }

    /**
     * Тест проверяет случай, когда обращение, где происходил диалог перешло в статус closed, ожидаем, что в таком
     * случае создастся новое обращение со ссылкой на предыдущее
     * <p>
     * Нет тест-кейса в testPalm
     */
    @Test
    void okDialogWithClosingTicketLeavesCommentsOnlyInActiveTicket() throws Exception {
        var smmAccount = utils.getAccount(FACEBOOK_ACCOUNT_ID);
        var dataList = utils.getSmmObjectsListFromFile(
                "/social_messaging/socialMessagingOkSimpleReplyDialogData.json");

        // Пользователь написал нам
        var firstClientComment = dataList.get(0);
        receiveAndVerifyItem(firstClientComment, smmAccount);
        // Тут должен создаться первый тикет
        Ticket firstCreatedTicket = ticketTestUtils.getSingleOpenedTicket(Ticket.FQN);


        // Ответим пользователю
        var firstAnswer = dataList.get(1);
        createCommentAndVerifySentSmmItem(firstCreatedTicket, firstAnswer);
        // И закроем первый тикет
        ticketTestUtils.editTicketStatus(firstCreatedTicket, "resolved");
        ticketTestUtils.editTicketStatus(firstCreatedTicket, "closed");


        // Пользователь отвечает нам (на наш ответ dataList[1] из первого тикета)
        var secondClientComment = dataList.get(2);
        receiveAndVerifyItem(secondClientComment, smmAccount);
        // Тут должен создаться второй тикет
        Ticket secondCreatedTicket = ticketTestUtils.getSingleOpenedTicket(Ticket.FQN);

        Ticket actualFirstCreatedTicket = storage.get(firstCreatedTicket.getGid());
        assertNotEquals(actualFirstCreatedTicket.getGid(), secondCreatedTicket.getGid(),
                "Должно создаться новое обращение");
        assertEquals(secondCreatedTicket.getGid(), actualFirstCreatedTicket.getNextSmmTicket().getGid(),
                "Должна создаться ссылка с первого обращение на второе (на продолжение обсуждения)");
        assertEquals(actualFirstCreatedTicket.getGid(), secondCreatedTicket.getPreviousSmmTicket().getGid(),
                "Должна создаться ссылка с нового обращение на первое (на начало обсуждения)");
        assertEquals(actualFirstCreatedTicket.getInitialSmmObject().getGid(),
                secondCreatedTicket.getInitialSmmObject().getGid());

        // Пользователь снова ответил нам на сообщение из первого (закрытого) тикета, ожидаем,
        // что ответ придет уже в активный
        var thirdClientComment = dataList.get(3);
        var createdItemIn = receiveAndVerifyItem(thirdClientComment, smmAccount);

        Ticket activeTicket = ticketTestUtils.getSingleOpenedTicket(Ticket.FQN);
        assertEquals(secondCreatedTicket.getGid(), activeTicket.getGid(),
                "Новый тикет не должен создаться");
        assertEquals(createdItemIn.getEntity(), activeTicket.getGid(),
                "Созданный smmItem должен привязаться к активному тикету");
    }

    /**
     * Тест проверяет случай, когда пользователь написал комментарий, а после его изменил
     *
     * @throws Exception
     */
    @Test
    void editingSmmObjectShouldInvokeCommentEditing() throws Exception {
        var smmAccount = utils.getAccount(FACEBOOK_ACCOUNT_ID);
        var dataList = utils.getSmmObjectsListFromFile(
                "/social_messaging/item/fbSimpleEdit.json");

        var firstClientComment = dataList.get(0);
        var smmObject = receiveAndVerifyItem(firstClientComment, smmAccount);
        Ticket firstCreatedTicket = ticketTestUtils.getSingleOpenedTicket(Ticket.FQN);
        assertEquals(1, getComments(firstCreatedTicket).size());
        var comment = verifyInboundComment(smmObject, "Комментарий");
        assertFalse(comment.isEdited());

        processEditEvent(smmAccount, dataList.get(1));
        assertEquals(1, getComments(firstCreatedTicket).size());
        comment = verifyInboundComment(smmObject, "Комментарий после редактирования");
        assertTrue(comment.isEdited());
    }

    /**
     * Тест проверяет случай, когда пользователь написал комментарий, а после его удалил
     *
     * @throws Exception
     */
    @Test
    void deletingSmmObjectShouldInvokeCommentMarkingAsDeleted() throws Exception {
        var smmAccount = utils.getAccount(FACEBOOK_ACCOUNT_ID);
        var dataList = utils.getSmmObjectsListFromFile(
                "/social_messaging/item/fbSimpleEdit.json");

        var firstClientComment = dataList.get(0);
        var smmObject = receiveAndVerifyItem(firstClientComment, smmAccount);
        Ticket firstCreatedTicket = ticketTestUtils.getSingleOpenedTicket(Ticket.FQN);
        assertEquals(1, getComments(firstCreatedTicket).size());
        var comment = verifyInboundComment(smmObject, "Комментарий");
        assertFalse(comment.isEdited());

        processDeleteEvent(smmAccount, dataList.get(1));
        assertEquals(1, getComments(firstCreatedTicket).size());
        comment = verifyInboundComment(smmObject, "<i>Сообщение удалено</i>");
        assertTrue(comment.isEdited());
    }
}
