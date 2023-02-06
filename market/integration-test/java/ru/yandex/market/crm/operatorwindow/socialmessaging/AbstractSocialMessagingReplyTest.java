package ru.yandex.market.crm.operatorwindow.socialmessaging;

import com.fasterxml.jackson.databind.JsonNode;

import ru.yandex.market.jmf.module.angry.ItemInSmmObject;
import ru.yandex.market.jmf.module.angry.ItemOutSmmObject;
import ru.yandex.market.jmf.module.angry.SmmAccount;
import ru.yandex.market.jmf.module.angry.Ticket;
import ru.yandex.market.jmf.module.angry.controller.v1.model.EventType;

public abstract class AbstractSocialMessagingReplyTest extends AbstractSocialMessagingTest {

    private ItemInSmmObject processCreateEvent(SmmAccount smmAccount, JsonNode content) {
        return (ItemInSmmObject) angrySpaceService.processCreateEvent(EventType.ITEM_NEW, smmAccount, content);
    }

    public void processEditEvent(SmmAccount smmAccount, JsonNode content) {
        angrySpaceService.processEditEvent(EventType.ITEM_EDIT, smmAccount, content);
    }

    public void processDeleteEvent(SmmAccount smmAccount, JsonNode content) {

        angrySpaceService.processDeleteEvent(EventType.ITEM_DELETE, smmAccount, content);
    }

    protected ItemInSmmObject receiveAndVerifyItem(JsonNode content, SmmAccount smmAccount) {
        return verifyCreateSmmObject(() -> processCreateEvent(smmAccount, content), smmAccount);
    }

    protected ItemOutSmmObject createCommentAndVerifySentSmmItem(Ticket ticket,
                                                                 JsonNode contentToSend) {
        mockAngrySpaceClient.setupSendItem(contentToSend);
        return createCommentAndVerifySentSmmObject(ticket, contentToSend);
    }


}
