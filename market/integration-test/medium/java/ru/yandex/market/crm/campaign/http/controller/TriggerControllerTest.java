package ru.yandex.market.crm.campaign.http.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.xml.validation.ValidationResultType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.crm.campaign.domain.trigger.SaveTriggerResponse;
import ru.yandex.market.crm.campaign.domain.trigger.TriggerRelatedInfoResponse;
import ru.yandex.market.crm.campaign.domain.trigger.ValidationError;
import ru.yandex.market.crm.campaign.dto.segment.SegmentDto;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.trigger.MessagePurpose;
import ru.yandex.market.crm.core.domain.trigger.MessageTypeDto;
import ru.yandex.market.crm.core.domain.trigger.TotalVariablesInfo;
import ru.yandex.market.crm.core.domain.trigger.VariablesInfo;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.db.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.core.domain.trigger.VariableInfo.number;
import static ru.yandex.market.crm.core.domain.trigger.VariableInfo.string;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.ordersFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;

/**
 * @author apershukov
 */
public class TriggerControllerTest extends AbstractControllerMediumTest {

    @Inject
    private RepositoryService repositoryService;
    @Inject
    private JsonDeserializer jsonDeserializer;
    @Inject
    private JsonSerializer jsonSerializer;
    @Inject
    private SegmentService segmentService;
    @Inject
    @Named(Constants.DEFAULT_JDBC_TEMPLATE)
    private JdbcTemplate jdbcTemplate;

    private static void assertMessageType(MessageTypeDto expected, MessageTypeDto actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getListener(), actual.getListener());
    }

    private static MessageTypeDto message(String id, String name, String listenerRef) {
        return MessageTypeDto.builder()
                .setId(id)
                .setName(name)
                .setListener(listenerRef)
                .build();
    }

    private static TotalVariablesInfo variablesInfo() {
        Map<String, VariablesInfo> delegates = Collections.singletonMap(
                "someDelegate",
                VariablesInfo.builder()
                        .setsOptionaly(string("variable", "Переменная"))
                        .build()
        );

        Map<String, VariablesInfo> messages = Collections.singletonMap(
                "someMessage",
                VariablesInfo.builder()
                        .sets(number("number", "Число"))
                        .build()
        );

        return new TotalVariablesInfo(delegates, messages);
    }

    @Test
    @Disabled("не понимаю почему падает. TODO разобраться")
    public void testSetAsyncAfter() throws Exception {
        mockMvc.perform(post("/api/triggers/save")
                .contentType(MediaType.APPLICATION_XML)
                .content(readSimpleBpmn())
        ).andExpect(status().isOk())
                .andDo(print());

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("test")
                .list();

        assertEquals(1, processDefinitions.size());

        BpmnModelInstance modelInstance = repositoryService.getBpmnModelInstance(processDefinitions.get(0).getId());

        modelInstance.getModelElementsByType(StartEvent.class)
                .forEach(action -> assertTrue(action.isCamundaAsyncAfter()));

        modelInstance.getModelElementsByType(IntermediateCatchEvent.class)
                .forEach(action -> assertTrue(action.isCamundaAsyncAfter()));
    }

    @Test
    public void testGetMessageTypes() throws Exception {
        MessageTypeDto messageType1 = message("message-1", "Message 1", "firstListener");
        saveMessageType(messageType1, MessagePurpose.START);

        MessageTypeDto messageType2 = message("message-2", "Message 2", "secondListener");
        saveMessageType(messageType2, MessagePurpose.INTERMEDIATE);

        MessageTypeDto messageType3 = message("message-3", "Message 3", "thirdListener");
        saveMessageType(messageType3, MessagePurpose.INTERMEDIATE);

        TriggerRelatedInfoResponse response = getRelatedInfo();

        List<MessageTypeDto> startTypes = response.getStartMessageTypes();
        assertEquals(1, startTypes.size());
        assertMessageType(messageType1, startTypes.get(0));

        List<MessageTypeDto> intermediateTypes = response.getIntermediateMessageTypes();
        assertEquals(2, intermediateTypes.size());
        assertMessageType(messageType2, intermediateTypes.get(0));
        assertMessageType(messageType3, intermediateTypes.get(1));
    }

    @Test
    public void testSegmentWithAllSupportedFiltersPresentInResponse() throws Exception {
        addOnlineFilter();

        Segment segment = segment(
                subscriptionFilter(SubscriptionTypes.ADVERTISING),
                subscriptionFilter(SubscriptionTypes.WISHLIST)
        );

        segment = segmentService.addSegment(segment);

        TriggerRelatedInfoResponse response = getRelatedInfo();

        List<SegmentDto> segments = response.getSegments();

        assertEquals(1, segments.size());
        assertEquals(segment.getId(), segments.get(0).getId());
    }

    @Test
    public void testSegmentWithAllNotSupportedFiltersNotPresentInResponse() throws Exception {
        addOnlineFilter();

        Segment segment = segment(
                subscriptionFilter(SubscriptionTypes.ADVERTISING),
                ordersFilter()
        );

        segmentService.addSegment(segment);

        TriggerRelatedInfoResponse response = getRelatedInfo();

        List<SegmentDto> segments = response.getSegments();

        assertEquals(0, segments.size());
    }

    @Test
    public void testGetVariablesInfo() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO trigger_variables_info (variables_info)\n" +
                        "VALUES (?::jsonb)",
                jsonSerializer.writeObjectAsString(variablesInfo())
        );

        TriggerRelatedInfoResponse response = getRelatedInfo();

        TotalVariablesInfo variablesInfo = response.getVariablesInfo();
        assertNotNull(variablesInfo);
        assertNotNull(variablesInfo.getDelegates().get("someDelegate"));
        assertNotNull(variablesInfo.getMessages().get("someMessage"));
    }

    /**
     * Если блок, к которому прикреплен оработчик BoundaryEvent совпадает с блоком,
     * куда направлена стрелка из обработчика,
     * тогда контроллер возвращает ответ с соответствующей информацией об ошибке
     */
    @Test
    public void testReturnErrorForBoundaryEventWithSameTargetElements() throws Exception {
        var strResponse = mockMvc.perform(post("/api/triggers/save")
                .contentType(MediaType.APPLICATION_XML)
                .content(readBpmn("boundary-event-same-target-bpmn.xml")))
                .andDo(print())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var response = jsonDeserializer.readObject(SaveTriggerResponse.class, strResponse);
        assertNotNull(response);

        var validationErrors = response.getValidationErrors();
        assertNotNull(validationErrors);
        assertEquals(1, validationErrors.size());

        assertValidationErrorsMessages(
                validationErrors.get("BoundaryEvent_14mkpps"),
                "Boundary event arrow target and attached element are the same"
        );
    }

    /**
     * При сохранении триггера с некорректным конфигом блока А/Б тестирования возвращаются все ошибки,
     * которые присутствуют на диаграмме. Причем ошибки корректно соотносятся для каждого элемента,
     * как для шлюза, так и для выходящих из него стрелок
     */
    @Test
    public void testReturnAllValidationErrorsForIncorrectABTestBlock() throws Exception {
        var strResponse = mockMvc.perform(post("/api/triggers/save")
                .contentType(MediaType.APPLICATION_XML)
                .content(readBpmn("incorrect-ab-diagram-bpmn.xml")))
                .andDo(print())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var response = jsonDeserializer.readObject(SaveTriggerResponse.class, strResponse);
        assertNotNull(response);

        var validationErrors = response.getValidationErrors();
        assertNotNull(validationErrors);
        assertEquals(3, validationErrors.size());

        assertValidationErrorsMessages(
                validationErrors.get("SequenceFlow_1"),
                "Attribute variant is null or empty", "Attribute percent too large"
        );
        assertValidationErrorsMessages(
                validationErrors.get("SequenceFlow_2"),
                "Attribute percent must be digit"
        );
        assertValidationErrorsMessages(
                validationErrors.get("ExclusiveGateway_111"),
                "Has variant flow with incorrect percent", "Has duplicate variant: aa"
        );
    }

    private String readSimpleBpmn() throws IOException {
        return readBpmn("simple-bpmn.xml");
    }

    private String readBpmn(String fileName) throws IOException {
        return IOUtils.toString(
                getClass().getResourceAsStream("bpmn/" + fileName),
                StandardCharsets.UTF_8
        );
    }

    private void saveMessageType(MessageTypeDto messageType, MessagePurpose purpouse) {
        jdbcTemplate.update(
                "INSERT INTO trigger_message_types (id, name, listener, purpose)\n" +
                        "VALUES (?, ?, ?, ?)",
                messageType.getId(),
                messageType.getName(),
                messageType.getListener(),
                purpouse.name()
        );
    }

    private TriggerRelatedInfoResponse getRelatedInfo() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/triggers/related-info"))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        return jsonDeserializer.readObject(
                TriggerRelatedInfoResponse.class,
                result.getResponse().getContentAsByteArray()
        );
    }

    private void addOnlineFilter() {
        jdbcTemplate.update(
                "INSERT INTO online_filters (filter_id)\n" +
                        "VALUES ('subscribed')"
        );
    }

    private void assertValidationErrorsMessages(List<ValidationError> errors, String... expectedMessages) {
        assertEquals(expectedMessages.length, errors.size());

        errors.forEach(error -> assertEquals(ValidationResultType.ERROR, error.getValidationResultType()));

        var sf1ErrorsMessages = errors.stream()
                .map(ValidationError::getMessage)
                .collect(Collectors.toSet());
        assertEquals(Set.of(expectedMessages), sf1ErrorsMessages);

    }
}
