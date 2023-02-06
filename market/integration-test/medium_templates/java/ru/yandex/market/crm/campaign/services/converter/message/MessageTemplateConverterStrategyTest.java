package ru.yandex.market.crm.campaign.services.converter.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.campaign.domain.trigger.MessageTypes;
import ru.yandex.market.crm.campaign.dto.message.SmsMessageTemplateDto;
import ru.yandex.market.crm.campaign.services.bpm.MessageTypesDAO;
import ru.yandex.market.crm.campaign.services.messages.MessageTemplatesService;
import ru.yandex.market.crm.campaign.services.security.MessageTemplatePermissionsService;
import ru.yandex.market.crm.campaign.services.trigger.TriggerService;
import ru.yandex.market.crm.campaign.services.trigger.TriggersDAO;
import ru.yandex.market.crm.campaign.test.AbstractServiceMediumWithoutYtTest;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateState;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.core.domain.messages.SmsMessageConf;
import ru.yandex.market.crm.core.domain.trigger.ElementInfo;
import ru.yandex.market.crm.core.domain.trigger.MessageTypeDto;
import ru.yandex.market.crm.core.domain.trigger.TriggerInfo;
import ru.yandex.market.crm.core.services.messages.MessageTemplatesDAO;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author zloddey
 */
@ContextConfiguration(classes = {MessageTemplateConverterStrategyTest.Configuration.class})
public class MessageTemplateConverterStrategyTest extends AbstractServiceMediumWithoutYtTest {

    private static final MessageTypeDto MESSAGE_TYPE = message("coin_created", "coin_created", "firstListener");
    @Inject
    private MessageTemplatesService templateService;
    @Inject
    private MessageTemplatesDAO messageTemplatesDAO;
    @Inject
    TriggerService triggerService;
    @Inject
    TriggersDAO triggersDAO;
    @Inject
    SmsMessageTemplateConverterStrategy templateConverterStrategy;            // system under test

    public static class Configuration {
        @Bean
        public MessageTemplatePermissionsService messageTemplatePermissionsService() {
            return mock(MessageTemplatePermissionsService.class);
        }

        @Bean
        public MessageTypesDAO messageTypesDAO() {
            MessageTypesDAO dao = mock(MessageTypesDAO.class);
            when(dao.getMessageTypes()).thenReturn(new MessageTypes(
                    Collections.singletonList(MESSAGE_TYPE),
                    Collections.emptyList()
            ));
            return dao;
        }
    }

    private static MessageTypeDto message(String id, String name, String listenerRef) {
        return MessageTypeDto.builder()
                .setId(id)
                .setName(name)
                .setListener(listenerRef)
                .build();
    }

    /**
     * В light-режиме генерации списки связанных триггеров равны null
     */
    @Test
    public void nullDtoFieldsWithLightOption() {
        var messageTemplate = prepareSmsTemplate();
        SmsMessageTemplateDto dto = templateConverterStrategy.convert(messageTemplate, true);
        Assertions.assertNull(dto.getTriggersElementsWithCurrentVersion());
        Assertions.assertNull(dto.getTriggersWithOldVersions());
        Assertions.assertFalse(dto.canDelete());
    }

    /**
     * Если нет связанных триггеров, то списки связанных триггеров пусты, и шаблон можно удалить
     */
    @Test
    public void noRelatedTriggers() {
        var messageTemplate = prepareSmsTemplate();
        SmsMessageTemplateDto dto = convertToDTO(messageTemplate);
        Assertions.assertTrue(dto.getTriggersElementsWithCurrentVersion().isEmpty());
        Assertions.assertTrue(dto.getTriggersWithOldVersions().isEmpty());
        Assertions.assertTrue(dto.canDelete());
    }

    /**
     * Один связанный триггер с одним элементом - он должен показываться в списке, а шаблон нельзя удалять (и далее
     * тоже)
     */
    @Test
    public void oneTrigger() throws IOException {
        var messageTemplate = prepareSmsTemplate();
        createTrigger("Process_1", messageTemplate);

        SmsMessageTemplateDto dto = convertToDTO(messageTemplate);
        List<ElementInfo> elements = dto.getTriggersElementsWithCurrentVersion();
        Assertions.assertEquals(1, elements.size());
        ElementInfo elementInfo = elements.get(0);
        Assertions.assertTrue(elementInfo.getTriggerId().startsWith("Process_1:1:"));
        Assertions.assertEquals("fdc", elementInfo.getTriggerName());
        Assertions.assertEquals("Task_0q05qsh", elementInfo.getElementId());
        Assertions.assertTrue(dto.getTriggersWithOldVersions().isEmpty());
        Assertions.assertFalse(dto.canDelete());
    }

    /**
     * Если триггер удалён, то связь тоже должна пропадать
     */
    @Test
    public void deletedTrigger() throws IOException {
        var messageTemplate = prepareSmsTemplate();
        createTrigger("Process_2", messageTemplate);
        deleteTrigger("Process_2");

        SmsMessageTemplateDto dto = convertToDTO(messageTemplate);
        List<ElementInfo> elements = dto.getTriggersElementsWithCurrentVersion();
        Assertions.assertEquals(0, elements.size());
        Assertions.assertTrue(dto.getTriggersWithOldVersions().isEmpty());
        Assertions.assertTrue(dto.canDelete());
    }

    /**
     * Если несколько версий триггера ссылаются на шаблон, надо показать их все
     */
    @Test
    public void oneTriggerWithSeveralVersions() throws IOException {
        var messageTemplate = prepareSmsTemplate();
        createTrigger("Process_3", messageTemplate);
        updateTrigger("Process_3", messageTemplate, "2");
        updateTrigger("Process_3", messageTemplate, "3");

        SmsMessageTemplateDto dto = convertToDTO(messageTemplate);
        List<ElementInfo> elements = dto.getTriggersElementsWithCurrentVersion();
        Assertions.assertEquals(3, elements.size());
        ElementInfo elementInfo = elements.get(2);
        Assertions.assertTrue(elementInfo.getTriggerId().startsWith("Process_3:3:"));
        Assertions.assertEquals("fdc", elementInfo.getTriggerName());
        Assertions.assertEquals("Task_0q05qsh", elementInfo.getElementId());
        Assertions.assertTrue(dto.getTriggersWithOldVersions().isEmpty());
        Assertions.assertFalse(dto.canDelete());
    }

    /**
     * Если старые версии триггера не были опубликованы, то они не должны попасть в список "триггеров со старыми
     * версиями"
     */
    @Test
    public void unpublishedTriggerIsNotShownInPreviousVersions() throws IOException {
        var messageTemplate = prepareSmsTemplate();
        createTrigger("Process_4", messageTemplate);

        var templateV2 = createNewVersion(messageTemplate);

        SmsMessageTemplateDto dto = convertToDTO(messageTemplate);
        List<ElementInfo> elements = dto.getTriggersElementsWithCurrentVersion();
        // Триггер использует первую версию шаблона, поэтому не должен появиться здесь
        Assertions.assertEquals(0, elements.size());
        List<TriggerInfo> triggersWithOldVersions = dto.getTriggersWithOldVersions();
        Assertions.assertEquals(0, triggersWithOldVersions.size());
        Assertions.assertFalse(dto.canDelete());
    }

    /**
     * Если старая версия триггера была опубликована, она должна появиться в списке "триггеров со старыми версиями"
     */
    @Test
    public void publishedTriggerIsShownInPreviousVersions() throws IOException {
        var messageTemplate = prepareSmsTemplate();
        createTrigger("Process_5", messageTemplate);
        triggersDAO.updateDraftStateByKey("Process_5", false);

        createNewVersion(messageTemplate);

        SmsMessageTemplateDto dto = convertToDTO(messageTemplate);
        List<ElementInfo> elements = dto.getTriggersElementsWithCurrentVersion();
        // Триггер использует первую версию шаблона, поэтому не должен появиться здесь
        Assertions.assertEquals(0, elements.size());
        List<TriggerInfo> triggersWithOldVersions = dto.getTriggersWithOldVersions();
        Assertions.assertEquals(1, triggersWithOldVersions.size());
        TriggerInfo oldTriggerInfo = triggersWithOldVersions.get(0);
        Assertions.assertEquals("Process_5", oldTriggerInfo.getKey());
        Assertions.assertEquals(1, oldTriggerInfo.getVersion());
        Assertions.assertFalse(dto.canDelete());
    }

    /**
     * Если триггер содержит несколько элементов, ссылающихся на шаблон, надо вернуть информацию о всех этих элементах
     */
    @Test
    public void triggerWithSeveralElements() throws IOException {
        var messageTemplate = prepareSmsTemplate();
        createTriggerWithTwoElements("Process_6", messageTemplate, messageTemplate);

        SmsMessageTemplateDto dto = convertToDTO(messageTemplate);
        List<ElementInfo> elements = dto.getTriggersElementsWithCurrentVersion();
        Assertions.assertEquals(2, elements.size());
        ElementInfo element1 = elements.get(0);
        Assertions.assertTrue(element1.getTriggerId().startsWith("Process_6:1:"));
        Assertions.assertEquals("Two elements with template", element1.getTriggerName());
        Assertions.assertEquals("postcard1", element1.getElementId());
        ElementInfo element2 = elements.get(1);
        Assertions.assertTrue(element2.getTriggerId().startsWith("Process_6:1:"));
        Assertions.assertEquals("Two elements with template", element2.getTriggerName());
        Assertions.assertEquals("postcard2", element2.getElementId());
        Assertions.assertTrue(dto.getTriggersWithOldVersions().isEmpty());
        Assertions.assertFalse(dto.canDelete());
    }

    /**
     * Если элемент триггера ссылается на другой шаблон, его показывать не надо
     */
    @Test
    public void eachElementRelatesToDifferentTemplate() throws IOException {
        var messageTemplate = prepareSmsTemplate();
        var anotherTemplate = prepareSmsTemplate();
        createTriggerWithTwoElements("Process_7", anotherTemplate, messageTemplate);

        SmsMessageTemplateDto dto = convertToDTO(messageTemplate);
        List<ElementInfo> elements = dto.getTriggersElementsWithCurrentVersion();
        Assertions.assertEquals(1, elements.size());
        ElementInfo elementDto = elements.get(0);
        Assertions.assertEquals("Two elements with template", elementDto.getTriggerName());
        Assertions.assertEquals("postcard2", elementDto.getElementId());
        Assertions.assertTrue(dto.getTriggersWithOldVersions().isEmpty());
        Assertions.assertFalse(dto.canDelete());
    }

    /**
     * Если старые версии шаблона были в последних опубликованных триггерах, то они должны появиться в
     * поле triggersWithOldVersions.
     */
    @Test
    public void showNewPublishedTriggersHavingOldTemplateVersions() throws IOException {
        var messageTemplate = prepareSmsTemplate();
        createTrigger("Process_8", messageTemplate);
        updateTrigger("Process_8", messageTemplate, "2");
        triggersDAO.updateDraftStateByKey("Process_8", false);

        var templateV2 = createNewVersion(messageTemplate);

        SmsMessageTemplateDto dto = convertToDTO(templateV2);
        List<ElementInfo> elements = dto.getTriggersElementsWithCurrentVersion();
        // Триггер использует первую версию шаблона, поэтому не должен появиться здесь
        Assertions.assertEquals(0, elements.size());
        // Но триггер должен появиться в этом списке
        List<TriggerInfo> triggersWithOldVersions = dto.getTriggersWithOldVersions();
        Assertions.assertEquals(1, triggersWithOldVersions.size());
        TriggerInfo oldTriggerInfo = triggersWithOldVersions.get(0);
        Assertions.assertEquals("Process_8", oldTriggerInfo.getKey());
        Assertions.assertEquals(2, oldTriggerInfo.getVersion());
        Assertions.assertFalse(dto.canDelete());
    }

    /**
     * Надо возвращать все триггеры, которые ссылаются на шаблон, отсортированные по ключу
     */
    @Test
    public void severalTriggers() throws IOException {
        var messageTemplate = prepareSmsTemplate();
        createTrigger("Trigger2", messageTemplate);
        createTrigger("Trigger3", messageTemplate);
        createTrigger("Trigger1", messageTemplate);

        SmsMessageTemplateDto dto = convertToDTO(messageTemplate);
        List<ElementInfo> elements = dto.getTriggersElementsWithCurrentVersion();
        Assertions.assertEquals(3, elements.size());

        Assertions.assertTrue(elements.get(0).getTriggerId().startsWith("Trigger1:1:"));
        Assertions.assertTrue(elements.get(1).getTriggerId().startsWith("Trigger2:1:"));
        Assertions.assertTrue(elements.get(2).getTriggerId().startsWith("Trigger3:1:"));

        Assertions.assertTrue(dto.getTriggersWithOldVersions().isEmpty());
        Assertions.assertFalse(dto.canDelete());
    }

    /**
     * Наличие других тасков - например, логирования - не должно ломать процесс сохранения
     */
    @Test
    public void oneTriggerWithLoggerTask() throws IOException {
        var messageTemplate = prepareSmsTemplate();
        createTrigger("trigger3.xml", "Process_8", messageTemplate);

        SmsMessageTemplateDto dto = convertToDTO(messageTemplate);
        List<ElementInfo> elements = dto.getTriggersElementsWithCurrentVersion();
        Assertions.assertEquals(1, elements.size());
        ElementInfo elementInfo = elements.get(0);
        Assertions.assertTrue(elementInfo.getTriggerId().startsWith("Process_8:1:"));
    }

    private SmsMessageTemplateDto convertToDTO(MessageTemplate<SmsMessageConf> messageTemplate) {
        return templateConverterStrategy.convert(messageTemplate, false);
    }

    private void createTrigger(String triggerKey, MessageTemplate<?> messageTemplate) throws IOException {
        createTrigger("trigger.xml", triggerKey, messageTemplate);
    }

    private void createTrigger(String fileName, String triggerKey, MessageTemplate<?> messageTemplate) throws IOException {
        HashMap<String, String> replacements = new HashMap<>() {{
            put("{triggerKey}", triggerKey);
            put("{triggerVersion}", "1");
            put("{templateId}", messageTemplate.getId());
        }};
        triggerService.addTrigger(makeTrigger(fileName, replacements), null);
    }

    private void createTriggerWithTwoElements(String triggerKey,
                                              MessageTemplate<?> template1,
                                              MessageTemplate<?> template2) throws IOException {
        HashMap<String, String> replacements = new HashMap<>() {{
            put("{triggerKey}", triggerKey);
            put("{triggerVersion}", "1");
            put("{templateId1}", template1.getId());
            put("{templateId2}", template2.getId());
        }};
        triggerService.addTrigger(makeTrigger("trigger2.xml", replacements), null);
    }

    private void updateTrigger(String triggerKey, MessageTemplate<?> messageTemplate, String newVersion) throws IOException {
        HashMap<String, String> replacements = new HashMap<>() {{
            put("{triggerKey}", triggerKey);
            put("{triggerVersion}", newVersion);
            put("{templateId}", messageTemplate.getId());
        }};
        triggerService.updateTrigger(makeTrigger("trigger.xml", replacements), null);
    }

    private void deleteTrigger(String triggerKey) {
        triggerService.deleteTrigger(triggerKey);
    }

    private BpmnModelInstance makeTrigger(String fileName, Map<String, String> replacements) throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream(fileName);
        var writer = new StringWriter();
        IOUtils.copy(resourceAsStream, writer, StandardCharsets.UTF_8);
        String body = writer.toString();
        for (String key : replacements.keySet()) {
            body = body.replace(key, replacements.get(key));
        }
        return Bpmn.readModelFromStream(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Nonnull
    private MessageTemplate<SmsMessageConf> buildSmsTemplate() {
        SmsMessageConf config = new SmsMessageConf();
        config.setTextTemplate("Message template");
        var template = new MessageTemplate<SmsMessageConf>();
        template.setType(MessageTemplateType.SMS);
        template.setName("Test template");
        template.setConfig(config);
        return template;
    }

    private MessageTemplate<SmsMessageConf> prepareSmsTemplate() {
        return (MessageTemplate<SmsMessageConf>) templateService.addTemplate(buildSmsTemplate());
    }

    private MessageTemplate<SmsMessageConf> createNewVersion(MessageTemplate<SmsMessageConf> template) {
        template.setState(MessageTemplateState.PUBLISHED);
        messageTemplatesDAO.update(template);
        return (MessageTemplate<SmsMessageConf>) templateService.update(template.getKey(), template);
    }
}
