package ru.yandex.market.crm.campaign.http.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.crm.campaign.domain.messages.SendTestGncRequest;
import ru.yandex.market.crm.campaign.domain.promo.entities.TestPuid;
import ru.yandex.market.crm.campaign.domain.promo.entities.TestPuidsGroup;
import ru.yandex.market.crm.campaign.services.sql.TestPuidsDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.core.domain.messages.GncMessageConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateState;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateVar;
import ru.yandex.market.crm.core.services.external.gnc.domain.AddNotificationInfo;
import ru.yandex.market.crm.core.services.gnc.AddNotificationInfoFactory;
import ru.yandex.market.crm.core.services.messages.MessageTemplatesDAO;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.HttpResponse;
import ru.yandex.market.mcrm.http.ResponseMock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.mcrm.http.HttpRequest.post;

public class GncMessageTemplatesControllerTest extends AbstractControllerMediumTest {

    @Inject
    private TestPuidsDAO testUidsDao;

    @Inject
    private MessageTemplatesDAO messageTemplatesDao;

    @Inject
    private JsonSerializer jsonSerializer;

    private MessageTemplate<GncMessageConf> messageTemplate;

    @BeforeEach
    public void setUp() {
        messageTemplate = makeMessageTemplate();
    }

    @Test
    public void testSaveNewGroup() throws Exception {
        TestPuidsGroup group = new TestPuidsGroup()
                .setId("id_1")
                .setName("name_1")
                .setItems(Collections.singletonList(new TestPuid(123123L, "someName")));

        sendTest(Map.of("modelName", "iphoneXXX"), group);

        List<TestPuidsGroup> all = testUidsDao.getAll();
        Assertions.assertEquals(1, all.size());

        TestPuidsGroup actualGroup = all.get(0);
        Assertions.assertEquals(group.getId(), actualGroup.getId());
        Assertions.assertEquals(group.getName(), actualGroup.getName());
        Assertions.assertEquals(group.getItems().get(0), actualGroup.getItems().get(0));
    }

    @Test
    public void testUpdateGroup() throws Exception {
        TestPuidsGroup group = new TestPuidsGroup()
                .setId("id_1")
                .setName("name_1")
                .setItems(Collections.singletonList(new TestPuid(123123L, "someName")));

        testUidsDao.saveGroups(Collections.singletonList(group));

        group.setName("name_2");

        sendTest(Map.of("modelName", "iphoneXXX"), group);

        List<TestPuidsGroup> all = testUidsDao.getAll();
        Assertions.assertEquals(1, all.size());
        Assertions.assertEquals("id_1", all.get(0).getId());
        Assertions.assertEquals("name_2", all.get(0).getName());
    }

    @Test
    public void testGncClientCall() throws Exception {
        prepareGncClient("new-answer", 123123, 666666L, Map.of("model", "iphoneXXX"));

        TestPuidsGroup group = new TestPuidsGroup()
                .setId("id_1")
                .setName("name_1")
                .setItems(Collections.singletonList(new TestPuid(true, 123123L, "someName")));

        sendTest(
                Map.of(
                        "modelNameInProcess", "iphoneXXX",
                        "actorVar", 666666),
                group);
    }

    private void sendTest(Map<String, Object> vars, TestPuidsGroup... groups) throws Exception {
        SendTestGncRequest request = new SendTestGncRequest();
        request.setGroups(Arrays.asList(groups));
        request.setVars(vars);

        mockMvc.perform(post("/api/message-templates/gnc/by-id/{id}/send-test", messageTemplate.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(request))
                .characterEncoding("utf-8"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    private MessageTemplate<GncMessageConf> makeMessageTemplate() {
        GncMessageConf config = new GncMessageConf();
        config.setType("new-answer");
        config.setActorVar("actorVar");

        MessageTemplateVar modelVar = new MessageTemplateVar();
        modelVar.setName("modelNameInProcess");
        modelVar.setType(MessageTemplateVar.Type.STRING);

        config.setVars(Collections.singletonList(modelVar));
        config.setTemplateVars(Map.of("model", "modelNameInProcess"));

        var messageTemplate = new MessageTemplate<GncMessageConf>();
        messageTemplate.setType(MessageTemplateType.GNC);
        messageTemplate.setId("messageId");
        messageTemplate.setKey("messageKey");
        messageTemplate.setName("name");
        messageTemplate.setVersion(1);
        messageTemplate.setConfig(config);
        messageTemplate.setState(MessageTemplateState.DRAFT);

        messageTemplatesDao.save(messageTemplate);

        return messageTemplate;
    }

    private void prepareGncClient(
            String type,
            long puid,
            Long actor,
            Map<String, String> templateVars
    ) {
        AddNotificationInfo nInfo =
                AddNotificationInfoFactory.make(type, puid, actor, null, null, templateVars);
        httpEnvironment.when(
                post("https://bell-test.yandex.net/v1/notifier/service/add-notification")
                        .param("uid", nInfo.getPuid())
                        .param("actor", nInfo.getActor())
                        .param("type", nInfo.getType())
                        .param("service", nInfo.getService())
                        .param("group_key", nInfo.getGroupKey())
                        .param("meta", jsonSerializer.writeObjectAsString(nInfo.getMeta()))
        ).then(new HttpResponse(new ResponseMock(null)));
    }
}
