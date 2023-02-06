package ru.yandex.market.jmf.module.angry.test;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.angry.AngrySpaceService;
import ru.yandex.market.jmf.module.angry.ItemInSmmObject;
import ru.yandex.market.jmf.module.angry.MessageInSmmObject;
import ru.yandex.market.jmf.module.angry.SmmObject;
import ru.yandex.market.jmf.module.angry.controller.v1.AngryV1Controller;
import ru.yandex.market.jmf.module.angry.controller.v1.model.AccountObject;
import ru.yandex.market.jmf.module.angry.controller.v1.model.EventType;
import ru.yandex.market.jmf.module.angry.controller.v1.model.Provider;
import ru.yandex.market.jmf.module.angry.controller.v1.model.SmmObjectEvent;
import ru.yandex.market.jmf.module.angry.impl.MockAngrySpaceClient;
import ru.yandex.market.jmf.module.angry.impl.SocialMessagingTestUtils;
import ru.yandex.market.jmf.security.AuthRunnerService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * В тестах проверяется, что вебхук-метод правильно понимает тип события и дергает правильные методы, создающие
 * правильные smmObject'ы, если это необходимо. В самих smmObject'ах проверяются только основные атрибуты
 * <p>
 * Правильность заполнения остальных атрибутов smmObject'а проверяется в тестах {@link AngrySpaceServiceTest}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleAngrySpaceTestConfiguration.class)
@Transactional
public class ModuleAngrySpaceTest {
    @Inject
    private AngrySpaceService angrySpaceService;

    @Inject
    private AuthRunnerService authRunnerService;

    @Inject
    private SocialMessagingTestUtils socialMessagingTestUtils;

    @Inject
    private EntityStorageService storage;

    @Inject
    private MockAngrySpaceClient mockAngrySpaceClient;

    private AccountObject accountObject;
    private AngryV1Controller controller;

    @AfterEach
    void tearDown() {
        mockAngrySpaceClient.clear();
    }

    @BeforeEach
    public void setUp() {
        accountObject = new AccountObject(42, Provider.VK, null, "VK provider", "screenName", "fizz.buzz/vk");
        socialMessagingTestUtils.createSmmAccount(accountObject, mockAngrySpaceClient);
        controller = new AngryV1Controller(angrySpaceService, authRunnerService);
    }

    @Test
    public void testCreateSimpleItem() throws Exception {
        var item = socialMessagingTestUtils
                .getFirstSmmObjectFromFile("/ru/yandex/market/jmf/module/angry/createItemEvent.json");
        var event = new SmmObjectEvent(EventType.ITEM_NEW, accountObject, item);
        String angryId = item.get("id").asText();

        controller.receiveEvent(event);

        var smmObject = getSmmObjectByAngryId(angryId);
        assertNotNull(smmObject);
        assertEquals(accountObject.getId(), smmObject.getAccount().getAccountId());
        assertTrue(smmObject.getMetaclass().equalsOrDescendantOf(ItemInSmmObject.FQN));
    }

    @Test
    public void testCreateSimpleMessage() throws Exception {
        var message = socialMessagingTestUtils
                .getFirstSmmObjectFromFile("/ru/yandex/market/jmf/module/angry/createMessageEvent.json");
        var event = new SmmObjectEvent(EventType.MESSAGE_NEW, accountObject, message);
        String angryId = message.get("id").asText();

        controller.receiveEvent(event);

        var smmObject = getSmmObjectByAngryId(angryId);
        assertNotNull(smmObject);
        assertEquals(accountObject.getId(), smmObject.getAccount().getAccountId());
        assertTrue(smmObject.getMetaclass().equalsOrDescendantOf(MessageInSmmObject.FQN));
    }

    @Test
    void dontCreateSmmObjectWithSameAngryId() throws Exception {
        var messages = socialMessagingTestUtils
                .getSmmObjectsListFromFile("/ru/yandex/market/jmf/module/angry/smmMessagesWithSameAngryId.json");

        for (JsonNode message : messages) {
            var event = new SmmObjectEvent(EventType.MESSAGE_NEW, accountObject, message);
            controller.receiveEvent(event);
        }

        var objects = storage.list(Query.of(SmmObject.FQN));
        assertEquals(1, objects.size());
    }

    @Test
    void editItem() throws Exception {
        var item = socialMessagingTestUtils.getSmmObjectsListFromFile(
                "/ru/yandex/market/jmf/module/angry/editItem.json");
        String angryId = item.get(0).get("id").asText();

        var createEvent = new SmmObjectEvent(EventType.ITEM_NEW, accountObject, item.get(0));
        controller.receiveEvent(createEvent);

        var smmObject = getSmmObjectByAngryId(angryId);
        assertNotNull(smmObject);
        assertNull(smmObject.getMessageEditingTime());
        assertFalse(smmObject.isDeleted());


        var editEvent = new SmmObjectEvent(EventType.ITEM_EDIT, accountObject, item.get(1));
        controller.receiveEvent(editEvent);
        smmObject = getSmmObjectByAngryId(angryId);
        assertNotNull(smmObject.getMessageEditingTime());
        assertFalse(smmObject.isDeleted());
    }

    @Test
    void deleteItem() throws Exception {
        var item = socialMessagingTestUtils.getSmmObjectsListFromFile(
                "/ru/yandex/market/jmf/module/angry/editItem.json");
        String angryId = item.get(0).get("id").asText();

        var createEvent = new SmmObjectEvent(EventType.ITEM_NEW, accountObject, item.get(0));
        controller.receiveEvent(createEvent);

        var smmObject = getSmmObjectByAngryId(angryId);
        assertNotNull(smmObject);
        assertNull(smmObject.getMessageEditingTime());
        assertFalse(smmObject.isDeleted());


        var deleteEvent = new SmmObjectEvent(EventType.ITEM_DELETE, accountObject, item.get(1));
        controller.receiveEvent(deleteEvent);
        smmObject = getSmmObjectByAngryId(angryId);
        assertNotNull(smmObject.getMessageEditingTime());
        assertTrue(smmObject.isDeleted());
    }

    private SmmObject getSmmObjectByAngryId(String angryId) {
        return storage.getByNaturalId(SmmObject.FQN, angryId);
    }
}
