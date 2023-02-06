package ru.yandex.market.crm.operatorwindow.socialmessaging;

import com.fasterxml.jackson.databind.JsonNode;

import ru.yandex.market.jmf.module.angry.MessageInSmmObject;
import ru.yandex.market.jmf.module.angry.MessageOutSmmObject;
import ru.yandex.market.jmf.module.angry.SmmAccount;
import ru.yandex.market.jmf.module.angry.Ticket;
import ru.yandex.market.jmf.module.angry.controller.v1.model.Message;

public abstract class AbstractSocialMessagingChatTest extends AbstractSocialMessagingTest {

    private MessageInSmmObject createMessageIn(SmmAccount smmAccount, JsonNode content) {
        Message parsed = angrySpaceService.parseSmmObject(content, Message.class);
        return angrySpaceService.createInboundSmmMessage(smmAccount, content, parsed);
    }

    protected MessageInSmmObject receiveMessage(JsonNode content, SmmAccount smmAccount) {
        return verifyCreateSmmObject(() -> createMessageIn(smmAccount, content), smmAccount);
    }

    protected MessageOutSmmObject createCommentAndVerifySentSmmMessage(Ticket ticket,
                                                                       JsonNode contentToSend) {
        mockAngrySpaceClient.setupSendMessage(contentToSend);
        return createCommentAndVerifySentSmmObject(ticket, contentToSend);
    }
}
