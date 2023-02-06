package ru.yandex.market.jmf.module.angry.test;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.lock.LockService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.angry.AngrySpaceService;
import ru.yandex.market.jmf.module.angry.ItemInSmmObject;
import ru.yandex.market.jmf.module.angry.ItemOutSmmObject;
import ru.yandex.market.jmf.module.angry.MessageInSmmObject;
import ru.yandex.market.jmf.module.angry.MessageOutSmmObject;
import ru.yandex.market.jmf.module.angry.SmmAccount;
import ru.yandex.market.jmf.module.angry.SmmObject;
import ru.yandex.market.jmf.module.angry.Ticket;
import ru.yandex.market.jmf.module.angry.controller.v1.model.Chat;
import ru.yandex.market.jmf.module.angry.controller.v1.model.Item;
import ru.yandex.market.jmf.module.angry.controller.v1.model.Message;
import ru.yandex.market.jmf.module.angry.controller.v1.model.SmmObjectBase;
import ru.yandex.market.jmf.module.angry.impl.AngrySpaceServiceImpl;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;

public class AngrySpaceServiceTest {
    private static final String IDENTIFIER = "identifier";
    private static final String CHAT_ID = "chatId";
    private static final String POST_ID = "postId";

    private AngrySpaceService angrySpaceService;
    private ObjectSerializeService serializeService;
    private BcpService bcpService;

    @BeforeEach
    void setUp() {
        EntityStorageService storageService = Mockito.mock(EntityStorageService.class);

        bcpService = Mockito.mock(BcpService.class);
        serializeService = Mockito.mock(ObjectSerializeService.class);
        var lockService = Mockito.mock(LockService.class);

        angrySpaceService = new AngrySpaceServiceImpl(storageService, bcpService, serializeService, lockService);
    }

    /**
     * Тест проверяет правильность выбора FQN и подтягивания в smmObject правильных атрибутов при вызове
     * {@link AngrySpaceService#createInboundSmmMessage}
     * для пришедшего сообщения соц.сети
     */
    @Test
    void createOutboundSmmObjectForMessage() {
        var message = Mockito.mock(Message.class);
        var chat = Mockito.mock(Chat.class);
        Mockito.when(chat.getId()).thenReturn(CHAT_ID);
        Mockito.when(message.getId()).thenReturn(IDENTIFIER);
        Mockito.when(message.getChat()).thenReturn(chat);
        Mockito.when(message.getParentId()).thenCallRealMethod();
        Mockito.when(serializeService.treeToValue(Mockito.any(), Mockito.eq(Message.class)))
                .thenReturn(message);
        Mockito.when(message.getCreatedAt()).thenReturn(Instant.now());

        testCreateOutboundSmmObject(MessageOutSmmObject.FQN, false, CHAT_ID);
    }

    /**
     * Тест проверяет правильность выбора FQN и подтягивания в smmObject правильных атрибутов при вызове
     * {@link AngrySpaceService#createOutboundSmmItem}
     * для пришедшего комментария соц.сети
     */
    @Test
    void createOutboundSmmObjectForItem() {
        var item = Mockito.mock(Item.class);
        Mockito.when(item.getId()).thenReturn(IDENTIFIER);
        Mockito.when(serializeService.treeToValue(Mockito.any(), Mockito.eq(Item.class)))
                .thenReturn(item);
        Mockito.when(item.getCreatedAt()).thenReturn(Instant.now());

        testCreateOutboundSmmObject(ItemOutSmmObject.FQN, true, null);
    }

    /**
     * Тест проверяет правильность выбора FQN и подтягивания в smmObject правильных атрибутов при вызове
     * {@link AngrySpaceService#createInboundSmmMessage}
     * для пришедшего сообщения соц.сети
     */
    @Test
    void createInboundSmmObjectForMessage() {
        var message = new Message(IDENTIFIER, null, null, 1625231508L,
                null, null, null, null, null, null);

        testCreateInboundSmmObject(MessageInSmmObject.FQN, false, null, message);
    }

    /**
     * Тест проверяет правильность выбора FQN и подтягивания в smmObject правильных атрибутов при вызове
     * {@link AngrySpaceService#createInboundSmmItem}
     * для пришедшего комментария соц.сети
     */
    @Test
    void createInboundSmmObjectForItem() {
        var item = Mockito.mock(Item.class);
        var parentItem = Mockito.mock(Item.class);
        Mockito.when(parentItem.getId()).thenReturn(POST_ID);
        Mockito.when(item.getParentItem()).thenReturn(parentItem);
        Mockito.when(item.getId()).thenReturn(IDENTIFIER);
        Mockito.when(item.getParentId()).thenCallRealMethod();
        Mockito.when(item.getCreatedAt()).thenReturn(Instant.now());

        testCreateInboundSmmObject(ItemInSmmObject.FQN, true, POST_ID, item);
    }

    void testCreateOutboundSmmObject(Fqn expectedFqn, boolean isItem, String expectedParentAngryId) {
        var account = Mockito.mock(SmmAccount.class);
        var smmObjectData = Mockito.mock(JsonNode.class);
        var relatedTicket = Mockito.mock(Ticket.class);

        var fqnCaptor = ArgumentCaptor.forClass(Fqn.class);
        var mapCaptor = ArgumentCaptor.forClass(Map.class);

        if (isItem) {
            angrySpaceService.createOutboundSmmItem(account, smmObjectData, relatedTicket);
        } else {
            angrySpaceService.createOutboundSmmMessage(account, smmObjectData, relatedTicket);
        }

        Mockito.verify(bcpService, Mockito.atLeastOnce())
                .create(fqnCaptor.capture(), mapCaptor.capture());

        var actualFqn = fqnCaptor.getValue();
        Assertions.assertEquals(expectedFqn, actualFqn);

        var actualCreateMap = mapCaptor.getValue();
        Assertions.assertEquals(IDENTIFIER, actualCreateMap.get(SmmObject.ANGRY_ID));
        Assertions.assertEquals(account, actualCreateMap.get(SmmObject.ACCOUNT));
        Assertions.assertEquals(smmObjectData, actualCreateMap.get(SmmObject.CONTENT));
        Assertions.assertEquals(relatedTicket, actualCreateMap.get(SmmObject.ENTITY));

        if (expectedParentAngryId != null) {
            Assertions.assertEquals(expectedParentAngryId, actualCreateMap.get(SmmObject.PARENT_ANGRY_ID));
        }
    }

    void testCreateInboundSmmObject(Fqn expectedFqn, boolean isItem,
                                    String expectedParentAngryId,
                                    SmmObjectBase smmObjectModel) {
        var account = Mockito.mock(SmmAccount.class);
        var smmObjectData = Mockito.mock(JsonNode.class);
        var fqnCaptor = ArgumentCaptor.forClass(Fqn.class);
        var mapCaptor = ArgumentCaptor.forClass(Map.class);

        if (isItem) {
            angrySpaceService.createInboundSmmItem(account, smmObjectData, (Item) smmObjectModel);
        } else {
            angrySpaceService.createInboundSmmMessage(account, smmObjectData, (Message) smmObjectModel);
        }


        Mockito.verify(bcpService, Mockito.atLeastOnce())
                .create(fqnCaptor.capture(), mapCaptor.capture());

        var actualFqn = fqnCaptor.getValue();
        Assertions.assertEquals(expectedFqn, actualFqn);

        var actualCreateMap = mapCaptor.getValue();
        Assertions.assertEquals(IDENTIFIER, actualCreateMap.get(SmmObject.ANGRY_ID));
        Assertions.assertEquals(account, actualCreateMap.get(SmmObject.ACCOUNT));
        Assertions.assertEquals(smmObjectData, actualCreateMap.get(SmmObject.CONTENT));
        if (expectedParentAngryId != null) {
            Assertions.assertEquals(expectedParentAngryId, actualCreateMap.get(SmmObject.PARENT_ANGRY_ID));
        }
    }
}
