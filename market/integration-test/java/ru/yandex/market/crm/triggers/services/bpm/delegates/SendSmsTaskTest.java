package ru.yandex.market.crm.triggers.services.bpm.delegates;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateState;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateVar;
import ru.yandex.market.crm.core.domain.messages.SmsMessageConf;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.external.ClckClient;
import ru.yandex.market.crm.core.services.external.smspassport.SendSmsResult;
import ru.yandex.market.crm.core.services.external.smspassport.SmsPassportClient;
import ru.yandex.market.crm.core.services.external.smspassport.domain.Platform;
import ru.yandex.market.crm.core.services.external.smspassport.domain.SendSmsRequestProperties;
import ru.yandex.market.crm.core.services.messages.MessageTemplatesDAO;
import ru.yandex.market.crm.core.services.staff.StaffService;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.core.services.trigger.ProcessErrorCodes;
import ru.yandex.market.crm.core.test.loggers.TestSentLogWriter;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.TriggerService;
import ru.yandex.market.crm.triggers.test.AbstractServiceTest;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper.ProcessInstance;
import ru.yandex.market.crm.triggers.test.helpers.builders.SendSmsTaskBuilder;
import ru.yandex.market.crm.util.Exceptions;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.mapreduce.domain.user.UidType.PHONE;
import static ru.yandex.market.crm.mapreduce.domain.user.UidType.PUID;

/**
 * @author vtarasoff
 * @since 12.05.2021
 */
public class SendSmsTaskTest extends AbstractServiceTest {
    private static final String SMS_TEXT = "Hello World!";
    private static final String TRIGGER_ID = "hello_world";
    private static final String SMS_TASK = "hello_world_sms";

    private final ArgumentCaptor<SendSmsRequestProperties> smsRequestArgumentCaptor =
            ArgumentCaptor.forClass(SendSmsRequestProperties.class);

    private final ArgumentCaptor<Uid> uidArgumentCaptor = ArgumentCaptor.forClass(Uid.class);

    @Inject
    private SmsPassportClient smsPassportClient;

    @Inject
    private MessageTemplatesDAO messageTemplatesDAO;

    @Inject
    private TriggerService triggerService;

    @Inject
    private TriggersHelper triggersHelper;

    @Inject
    private SendSmsTask sendSmsTask;

    @Inject
    private TestSentLogWriter testSentLogWriter;

    @Inject
    private ClckClient clckClient;

    @Inject
    private StaffService staffService;

    @Before
    public void setUp() {
        CompletableFuture<SendSmsResult> response = completedFuture(new SendSmsResult(123L, "", ""));

        doReturn(response).when(smsPassportClient).sendSms(any(), any());
    }

    @After
    public void tearDown() {
        reset(smsPassportClient);
    }

    @Test
    public void shouldSendSmsOnCommunicationIdAttributeIfPhone() throws Exception {
        String phone = preparePhone();
        Long puid = preparePuid();

        ProcessDefinition process = prepareProcess(
                builder -> builder.setCommunicationId("phone '" + phone + "'")
        );

        runTask(Uid.asPuid(puid), process);

        verify(smsPassportClient, times(1))
                .sendSms(uidArgumentCaptor.capture(), smsRequestArgumentCaptor.capture());
        SendSmsRequestProperties capturedProperties = smsRequestArgumentCaptor.getValue();
        Uid uid = uidArgumentCaptor.getValue();

        assertEquals(PHONE, uid.getType());
        assertEquals(phone, uid.getValue());
        assertEquals(SMS_TEXT, capturedProperties.getSmsText());
    }

    @Test
    public void shouldSendSmsOnCommunicationIdAttributeIfPuid() throws Exception {
        Long puid = preparePuid();

        ProcessDefinition process = prepareProcess(
                builder -> builder.setCommunicationId("puid '" + puid + "'")
        );

        runTask(Uid.asPuid(puid), process);

        verify(smsPassportClient, times(1))
                .sendSms(uidArgumentCaptor.capture(), smsRequestArgumentCaptor.capture());
        SendSmsRequestProperties capturedProperties = smsRequestArgumentCaptor.getValue();
        Uid uid = uidArgumentCaptor.getValue();

        assertEquals(PUID, uid.getType());
        assertEquals(puid.toString(), uid.getValue());
        assertEquals(SMS_TEXT, capturedProperties.getSmsText());
    }

    @Test
    public void shouldThrowsCommunicationErrorIfCommunicationIdAttributeReturnsNull() throws Exception {
        Long puid = preparePuid();

        ProcessDefinition process = prepareProcess(
                builder -> builder.setCommunicationId("null")
        );

        assertThrowsNoCommunicationIdsError(() -> runTask(Uid.asPuid(puid), process));
    }

    @Test
    public void shouldThrowsSmsLimitExceededException() throws Exception {
        Long puid = preparePuid();
        ProcessDefinition process = prepareProcess(
                builder -> builder.setCommunicationId("puid '" + puid + "'")
        );

        CompletableFuture<SendSmsResult> response =
                completedFuture(SendSmsResult.error("LIMITEXCEEDED", "error"));
        doReturn(response).when(smsPassportClient).sendSms(any(Uid.class), any(SendSmsRequestProperties.class));

        assertThrowsSmsLimitExceededError(() -> runTask(Uid.asPuid(puid), process));
    }

    @Test
    public void shouldThrowsUnsuitableUidErrorOnCommunicationIdAttributeIfNotPhoneOrPuid() throws Exception {
        ProcessDefinition process = prepareProcess(
                builder -> builder.setCommunicationId("email 'example@example.com'")
        );

        assertThrowsUnsuitableError(() -> runTask(Uid.asPuid(preparePuid()), process));
    }

    @Test
    public void shouldSendSmsOnPhoneVariable() throws Exception {
        String phone = preparePhone();
        Long puid = preparePuid();

        ProcessDefinition process = prepareProcess();

        runTask(Uid.asPuid(puid),
                process,
                processInstance -> processInstance.setVariable(ProcessVariablesNames.PHONE_NUMBER, phone));

        verify(smsPassportClient, times(1))
                .sendSms(uidArgumentCaptor.capture(), smsRequestArgumentCaptor.capture());
        SendSmsRequestProperties capturedProperties = smsRequestArgumentCaptor.getValue();
        Uid uid = uidArgumentCaptor.getValue();

        assertEquals(PHONE, uid.getType());
        assertEquals(phone, uid.getValue());
        assertEquals(SMS_TEXT, capturedProperties.getSmsText());
    }

    @Test
    public void shouldSendSmsOnContextUid() throws Exception {
        Long puid = preparePuid();

        ProcessDefinition process = prepareProcess();

        runTask(Uid.asPuid(puid), process);

        verify(smsPassportClient, times(1))
                .sendSms(uidArgumentCaptor.capture(), smsRequestArgumentCaptor.capture());
        SendSmsRequestProperties capturedProperties = smsRequestArgumentCaptor.getValue();
        Uid uid = uidArgumentCaptor.getValue();

        assertEquals(PUID, uid.getType());
        assertEquals(puid.toString(), uid.getValue());
        assertEquals(SMS_TEXT, capturedProperties.getSmsText());
    }

    @Test
    public void shouldSendSmsWithRightQueryParams() throws Exception {
        Long puid = preparePuid();

        ProcessDefinition process = prepareProcess();

        runTask(Uid.asPuid(puid), process);

        verify(smsPassportClient, times(1))
                .sendSms(uidArgumentCaptor.capture(), smsRequestArgumentCaptor.capture());
        SendSmsRequestProperties capturedProperties = smsRequestArgumentCaptor.getValue();
        Uid uid = uidArgumentCaptor.getValue();

        assertEquals(PUID, uid.getType());
        assertEquals(puid.toString(), uid.getValue());
        assertEquals(SMS_TEXT, capturedProperties.getSmsText());
        assertEquals(Platform.TRIGGERS_PLATFORM, capturedProperties.getPlatform());
        assertTrue(capturedProperties.getTriggerId().startsWith(TRIGGER_ID));
    }

    @Test
    public void shouldThrowsUnsuitableUidErrorIfContextUidIsNotPuid() throws Exception {
        ProcessDefinition process = prepareProcess();

        assertThrowsUnsuitableError(() -> runTask(Uid.asEmail("example@example.com"), process));
    }

    /**
     * Если в шаблоне смс присутствуют секретные переменные, то при логировании отправки смс секретные переменные
     * в тексте заменяются строкой из *, длина которой равна длине значения секретной переменной
     */
    @Test
    public void testHidingSecretVarsInLog() throws Exception {
        String text = "secretVar1: ${secretVar1} and notSecretVar1: ${notSecretVar1}" +
                " and secretVar2: ${secretVar2} and notSecretVar2: ${notSecretVar2}";

        List<MessageTemplateVar> vars = List.of(
                new MessageTemplateVar("secretVar1", MessageTemplateVar.Type.STRING, true),
                new MessageTemplateVar("secretVar2", MessageTemplateVar.Type.NUMBER, true),
                new MessageTemplateVar("notSecretVar1", MessageTemplateVar.Type.STRING, false),
                new MessageTemplateVar("notSecretVar2", MessageTemplateVar.Type.NUMBER, false)
        );

        var messageTemplate = prepareSmsMessageTemplate(conf ->
                conf.setTextTemplate(text).setVars(vars)
        );

        Long puid = preparePuid();

        ProcessDefinition process = prepareProcess(messageTemplate.getId(), taskBuilder -> {});

        runTask(Uid.asPuid(puid), process, processInstance ->
                processInstance
                        .setVariable("secretVar1", "secret_key")
                        .setVariable("secretVar2", 123)
                        .setVariable("notSecretVar1", "public_key")
                        .setVariable("notSecretVar2", 456)
        );

        String smsText = text
                .replace("${secretVar1}", "secret_key")
                .replace("${notSecretVar1}", "public_key")
                .replace("${secretVar2}", "***")
                .replace("${notSecretVar2}", "456");
        verify(smsPassportClient, times(1)).sendSms(any(), smsRequestArgumentCaptor.capture());
        SendSmsRequestProperties capturedProperties = smsRequestArgumentCaptor.getValue();

        assertNotEquals(smsText, capturedProperties.getSmsText());

        Queue<Map<String, String>> records = testSentLogWriter.getSmsLog();
        assertEquals(1, records.size());

        Map<String, String> record = records.poll();
        assertNotNull(record);

        assertEquals(
                text.replace("${secretVar1}", "*".repeat("secret_key".length()))
                        .replace("${notSecretVar1}", "public_key")
                        .replace("${secretVar2}", "***")
                        .replace("${notSecretVar2}", "456"),
                record.get("text")
        );
    }

    /**
     * Если не включен атрибут "не сокращать текст", то ссылки в смс сообщении сокращаются
     * с помощью клиента сокращения ссылок
     */
    @Test
    public void testShorteningUrlsInSmsMessage() throws Exception {
        var url1 = "http://www.yandex.ru";
        var url2 = "https://ffff.rarar.com";
        var shortUrl1 = "ya.cc/m/abc";
        var shortUrl2 = "ya.cc/m/def";
        var text = "Some text part one and link (${url1}.); Some text part two and link:${url2}";

        when(clckClient.getShortLink(url1)).thenReturn("http://" + shortUrl1);
        when(clckClient.getShortLink(url2)).thenReturn("https://" + shortUrl2);

        var vars = List.of(
                new MessageTemplateVar("url1", MessageTemplateVar.Type.STRING, false),
                new MessageTemplateVar("url2", MessageTemplateVar.Type.STRING, false)
        );

        var messageTemplate = prepareSmsMessageTemplate(conf ->
                conf.setTextTemplate(text).setVars(vars)
        );

        var puid = preparePuid();

        var process = prepareProcess(messageTemplate.getId(), x -> {});

        runTask(Uid.asPuid(puid), process, processInstance ->
                processInstance
                        .setVariable("url1", url1)
                        .setVariable("url2", url2)

        );

        var expectedSmsText = text
                .replace("${url1}", shortUrl1)
                .replace("${url2}", shortUrl2);
        verify(smsPassportClient, times(1)).sendSms(any(), smsRequestArgumentCaptor.capture());
        var capturedProperties = smsRequestArgumentCaptor.getValue();

        assertEquals(expectedSmsText, capturedProperties.getSmsText());

        var records = testSentLogWriter.getSmsLog();
        assertEquals(1, records.size());

        var record = records.poll();
        assertNotNull(record);

        assertEquals(expectedSmsText, record.get("text"));
    }

    /**
     * Если включен атрибут "не сокращать текст", то ссылки в смс сообщении не сокращаются
     */
    @Test
    public void testNotShorteningUrlsInSmsMessageWithNotShortenTextAttribute() throws Exception {
        var url1 = "http://www.yandex.ru";
        var url2 = "https://ffff.rarar.com";
        var text = "Some text part one and link (${url1}.); Some text part two and link:${url2}";

        when(clckClient.getShortLink(url1)).thenReturn("https://ya.cc/m/abc");
        when(clckClient.getShortLink(url2)).thenReturn("https://ya.cc/m/def");

        var vars = List.of(
                new MessageTemplateVar("url1", MessageTemplateVar.Type.STRING, false),
                new MessageTemplateVar("url2", MessageTemplateVar.Type.STRING, false)
        );

        var messageTemplate = prepareSmsMessageTemplate(conf ->
                conf.setTextTemplate(text).setVars(vars)
        );

        var puid = preparePuid();

        var process = prepareProcess(messageTemplate.getId(), SendSmsTaskBuilder::notShortenText);

        runTask(Uid.asPuid(puid), process, processInstance ->
                processInstance
                        .setVariable("url1", url1)
                        .setVariable("url2", url2)

        );

        verify(smsPassportClient, times(1)).sendSms(any(), smsRequestArgumentCaptor.capture());
        var capturedProperties = smsRequestArgumentCaptor.getValue();

        var expectedSmsText = text
                .replace("${url1}", url1)
                .replace("${url2}", url2);

        assertEquals(expectedSmsText, capturedProperties.getSmsText());

        var records = testSentLogWriter.getSmsLog();
        assertEquals(1, records.size());

        var record = records.poll();
        assertNotNull(record);

        assertEquals(expectedSmsText, record.get("text"));
    }

    @Test
    public void shouldThrowsCommunicationErrorIfStaffHasNotUid() throws Exception {
        Long puid = preparePuid();
        when(staffService.hasPuid(puid)).thenReturn(false);

        ProcessDefinition process = prepareProcess(
                builder -> builder.setCommunicationId("puid '" + puid + "'")
        );

        assertThrowsNoCommunicationIdsError(() -> runTask(Uid.asPuid(puid), process));
    }

    private MessageTemplate<SmsMessageConf> prepareSmsMessageTemplate() {
        return prepareSmsMessageTemplate(smsMessageConf -> {});
    }

    private MessageTemplate<SmsMessageConf> prepareSmsMessageTemplate(Consumer<SmsMessageConf> messageConfCustomizer) {
        SmsMessageConf config = new SmsMessageConf();
        config.setTextTemplate(SMS_TEXT);
        messageConfCustomizer.accept(config);

        var template = new MessageTemplate<SmsMessageConf>();
        template.setType(MessageTemplateType.SMS);
        template.setId(UUID.randomUUID().toString());
        template.setName("Test template");
        template.setVersion(1);
        template.setKey(UUID.randomUUID().toString());
        template.setState(MessageTemplateState.PUBLISHED);
        template.setConfig(config);

        messageTemplatesDAO.save(template);
        return template;
    }

    private ProcessDefinition prepareProcess() {
        return prepareProcess(builder -> {});
    }

    private ProcessDefinition prepareProcess(Consumer<SendSmsTaskBuilder> customizer) {
        String templateId = prepareSmsMessageTemplate().getId();

        return prepareProcess(templateId, customizer);
    }

    private ProcessDefinition prepareProcess(String messageTemplateId, Consumer<SendSmsTaskBuilder> customizer) {
        SendSmsTaskBuilder taskBuilder = TriggersHelper.triggerBuilder(TRIGGER_ID)
                .startEvent().message(MessageTypes.COIN_CREATED)
                .sendSmsTask(SMS_TASK)
                .templateId(messageTemplateId);

        customizer.accept(taskBuilder);

        BpmnModelInstance model = taskBuilder
                .endEvent()
                .done();

        return triggerService.addTrigger(model, null);
    }

    private void runTask(Uid uid, ProcessDefinition process) throws Exception {
        runTask(uid, process, processInstance -> {});
    }

    private void runTask(Uid uid,
                         ProcessDefinition process,
                         Consumer<ProcessInstance> customizer) throws Exception {
        ProcessInstance processInstance = new ProcessInstance(uid);
        customizer.accept(processInstance);

        triggersHelper.runTask(
                sendSmsTask,
                process.getId(),
                SMS_TASK,
                processInstance
        );
    }

    private String preparePhone() {
        return  "7" + RandomStringUtils.randomNumeric(10);
    }

    private Long preparePuid() {
        return RandomUtils.nextLong();
    }

    private void assertThrowsUnsuitableError(Exceptions.TrashRunnable runnable) throws Exception {
        assertThrowsBpmnError(runnable, ProcessErrorCodes.UNSUITABLE_UID, "Should throws unsuitable uid error");
    }

    private void assertThrowsNoCommunicationIdsError(Exceptions.TrashRunnable runnable) throws Exception {
        assertThrowsBpmnError(runnable, ProcessErrorCodes.NO_COMMUNICATION_IDS, "Should throws no communication ids error");
    }

    private void assertThrowsSmsLimitExceededError(Exceptions.TrashRunnable runnable) throws Exception {
        assertThrowsBpmnError(runnable, ProcessErrorCodes.SMS_LIMIT_EXCEEDED, "Should throws sms limits exceeded error");
    }

    private void assertThrowsBpmnError(Exceptions.TrashRunnable runnable,
                                             String errorCode,
                                             String failMessage) throws Exception {
        try {
            runnable.runWithExceptions();
        } catch (BpmnError error) {
            assertThat(error.getErrorCode(), equalTo(errorCode));
            return;
        }
        fail(failMessage);
    }
}
