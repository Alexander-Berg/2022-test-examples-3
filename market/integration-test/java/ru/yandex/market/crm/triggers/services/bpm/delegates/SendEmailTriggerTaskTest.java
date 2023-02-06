package ru.yandex.market.crm.triggers.services.bpm.delegates;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateVar;
import ru.yandex.market.crm.core.services.staff.StaffService;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.core.services.trigger.ProcessErrorCodes;
import ru.yandex.market.crm.core.test.loggers.TestSentLogWriter;
import ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.util.UserEmail;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.TriggerService;
import ru.yandex.market.crm.triggers.services.bpm.delegates.exceptions.CommunicationErrorException;
import ru.yandex.market.crm.triggers.services.control.IdSplitter;
import ru.yandex.market.crm.triggers.test.AbstractServiceTest;
import ru.yandex.market.crm.triggers.test.BpmnErrorMatcher;
import ru.yandex.market.crm.triggers.test.helpers.BigBTestHelper;
import ru.yandex.market.crm.triggers.test.helpers.EmailMessageTemplatesTestHelper;
import ru.yandex.market.crm.triggers.test.helpers.MarketUtilsHelper;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper.ProcessInstance;
import ru.yandex.market.crm.triggers.test.helpers.builders.SendEmailTaskBuilder;
import ru.yandex.market.crm.util.Randoms;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.uniformSplitEntry;
import static ru.yandex.market.crm.triggers.test.helpers.BigBTestHelper.profile;
import static ru.yandex.market.crm.triggers.test.helpers.EmailMessageTemplatesTestHelper.SENDER_CAMPAIGN_SLUG;

/**
 * @author apershukov
 */
public class SendEmailTriggerTaskTest extends AbstractServiceTest {

    private static final String TRIGGER_ID = "test_trigger";
    private static final String SEND_TASK = "send_task";

    private static final String EMAIL = "user@yandex.ru";

    private static final long CRYPTA_ID = 111;

    private static final long PUID = 112233;

    @Inject
    private SendEmailTriggerTask task;

    @Inject
    private TriggerService triggerService;

    @Inject
    private TriggersHelper triggersHelper;

    @Inject
    private YaSenderHelper yaSenderHelper;

    @Inject
    private MarketUtilsHelper marketUtilsHelper;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private GlobalSplitsTestHelper globalSplitsTestHelper;

    @Inject
    private EmailMessageTemplatesTestHelper messageTemplatesTestHelper;

    @Inject
    private TestSentLogWriter testSentLogWriter;

    @Inject
    private BigBTestHelper bigBTestHelper;

    @Inject
    private IdSplitter idSplitter;

    @Value("${global.control.percent}")
    private int controlPercent;

    @Inject
    private StaffService staffService;

    @Before
    public void setUp() {
        ytSchemaTestHelper.prepareGlobalControlSplitsTable();
    }

    @Test
    public void testThrowBpmnErrorIdCommunicationIdAttributeReturnsNull() throws Exception {
        ProcessDefinition process = prepareProcess(
                builder -> builder.setCommunicationId("null")
        );

        try {
            runTask(Uid.asEmail(EMAIL), process);
        } catch (BpmnError error) {
            assertThat(error.getErrorCode(), equalTo(ProcessErrorCodes.NO_COMMUNICATION_IDS));
            return;
        }
        fail("Should throws no communication ids error");
    }

    @Test
    public void testThrowBpmnErrorIfStaffHasNotEmail() throws Exception {
        String email = Randoms.email();
        when(staffService.hasEmail(email)).thenReturn(false);

        ProcessDefinition process = prepareProcess(
                builder -> builder.setCommunicationId("email '" + email + "'")
        );

        Uid uid = Uid.asEmail(email);
        try {
            runTask(uid, process);
        } catch (CommunicationErrorException error) {
            assertThat(error.getMessage(), equalTo("COMMUNICATION_ERROR: " + uid + " is not allowed"));
            return;
        }
        fail("Should throws communication error");
    }

    @Test
    public void testSendMessageToEmailFromCommunicationIdAttribute() throws Exception {
        String email = "test@example.com";

        ProcessDefinition process = prepareProcess(
                builder -> builder.setCommunicationId("email '" + email + "'")
        );

        yaSenderHelper.expectTransactionals(SENDER_CAMPAIGN_SLUG);

        runTask(Uid.asEmail(EMAIL), process);

        yaSenderHelper.verifySent(email);
    }

    @Test
    public void testSendMessageToEmailFromContext() throws Exception {
        ProcessDefinition process = prepareProcess();

        yaSenderHelper.expectTransactionals(SENDER_CAMPAIGN_SLUG);

        runTask(Uid.asEmail(EMAIL), process);

        yaSenderHelper.verifySent(EMAIL);
    }

    /**
     * Для зеленого email берется из настроек уведомлений на Маркете
     */
    @Test
    public void testSendMessageToMainEmailForGreen() throws Exception {
        ProcessDefinition process = prepareProcess();

        yaSenderHelper.expectTransactionals(SENDER_CAMPAIGN_SLUG);

        marketUtilsHelper.setEmailsForNotification(
                PUID,
                Arrays.asList(
                        new UserEmail("user2@yandex.ru", false),
                        new UserEmail("user3@yandex.ru", false),
                        new UserEmail(EMAIL, true)
                )
        );

        runTask(Uid.asPuid(PUID), process);

        yaSenderHelper.verifySent(EMAIL);
    }

    /**
     * В случае если в блок с включенным вычитанием глобального контроля приходит
     * email, принадлежащий глобальному контролю отправка на него не произодится.
     * Вместо этого выбрасывается ошибка "IN_GLOBAL_CONTROL"
     */
    @Test
    public void testDoNotSendMessageToEmailFromGlobalControl() throws Exception {
        bigBTestHelper.prepareProfile(Uid.asEmail(EMAIL), profile(CRYPTA_ID));

        globalSplitsTestHelper.prepareGlobalControlSplits(
                uniformSplitEntry(String.valueOf(CRYPTA_ID), false)
        );

        ProcessDefinition process = prepareProcess(builder -> builder.globalControlEnabled(true));

        yaSenderHelper.expectTransactionals(SENDER_CAMPAIGN_SLUG);

        runAndAssertControlError(Uid.asEmail(EMAIL), process);
    }

    /**
     * В случае если в блок с включенным вычитанием глобального контроля приходит
     * email, которого нет в глобальном сплите, принадлежность к этого email
     * к сплиту определяется по его хешу, при этом в таблицу глобальных сплитов ничего не дописывается
     */
    @Test
    public void testSplitUnknownEmail() throws Exception {
        ProcessDefinition process = prepareProcess(builder -> builder.globalControlEnabled(true));

        bigBTestHelper.prepareNotFound(Uid.asEmail(EMAIL));

        yaSenderHelper.expectTransactionals(SENDER_CAMPAIGN_SLUG);

        runExpectingControlError(Uid.asEmail(EMAIL), process);

        List<YTreeMapNode> splitTableRows = globalSplitsTestHelper.getGlobalSplitsRows();
        assertTrue(splitTableRows.isEmpty());

        if (idSplitter.isInTarget(EMAIL, controlPercent)) {
            yaSenderHelper.verifySent(EMAIL);
        }
    }

    /**
     * Если в шаблоне email сообщения присутствуют секретные переменные, то при логировании email отправки
     * секретные переменные в заголовке заменяются строкой из *, длина которой равна длине значения секретной переменной
     */
    @Test
    public void testHidingSecretVarsInLog() throws Exception {
        String email = "test@example.com";
        String subject = "secretVar1: ${secretVar1} and notSecretVar1: ${notSecretVar1}" +
                " and secretVar2: ${secretVar2} and notSecretVar2: ${notSecretVar2}";

        List<MessageTemplateVar> vars = List.of(
                new MessageTemplateVar("secretVar1", MessageTemplateVar.Type.STRING, true),
                new MessageTemplateVar("secretVar2", MessageTemplateVar.Type.NUMBER, true),
                new MessageTemplateVar("notSecretVar1", MessageTemplateVar.Type.STRING, false),
                new MessageTemplateVar("notSecretVar2", MessageTemplateVar.Type.NUMBER, false)
        );

        String templateId = messageTemplatesTestHelper.prepareMessageTemplate(conf -> {
            conf.setSubject(subject);
            conf.setVars(vars);
        });

        ProcessDefinition process = prepareProcess(
                templateId,
                builder -> builder.setCommunicationId("email '" + email + "'")
        );

        yaSenderHelper.expectTransactionals(SENDER_CAMPAIGN_SLUG);

        runTask(Uid.asEmail(EMAIL), process, processInstance ->
                processInstance
                        .setVariable("secretVar1", "secret_key")
                        .setVariable("secretVar2", 123)
                        .setVariable("notSecretVar1", "public_key")
                        .setVariable("notSecretVar2", 456)
        );

        yaSenderHelper.verifySent(email);

        Queue<Map<String, String>> records = testSentLogWriter.getEmailLog();
        assertEquals(1, records.size());

        Map<String, String> record = records.poll();
        assertNotNull(record);

        assertEquals(
                subject.replace("${secretVar1}", "*".repeat("secret_key".length()))
                        .replace("${notSecretVar1}", "public_key")
                        .replace("${secretVar2}", "***")
                        .replace("${notSecretVar2}", "456"),
                record.get("subject")
        );
    }

    private void runTask(Uid uid, ProcessDefinition process) throws Exception {
        runTask(uid, process, processInstance -> {
        });
    }

    private void runTask(Uid uid,
                         ProcessDefinition process,
                         Consumer<ProcessInstance> processInstanceCustomizer) throws Exception {
        ProcessInstance processInstance = new ProcessInstance(uid);
        processInstanceCustomizer.accept(processInstance);

        triggersHelper.runTask(
                task,
                process.getId(),
                SEND_TASK,
                processInstance
        );
    }

    private ProcessDefinition prepareProcess(Consumer<SendEmailTaskBuilder> customizer) {
        String templateId = messageTemplatesTestHelper.prepareMessageTemplate();

        return prepareProcess(templateId, customizer);
    }

    private ProcessDefinition prepareProcess(String messageTemplateId, Consumer<SendEmailTaskBuilder> customizer) {
        SendEmailTaskBuilder taskBuilder = TriggersHelper.triggerBuilder(TRIGGER_ID)
                .startEvent().message(MessageTypes.COIN_CREATED)
                .sendEmailTask(SEND_TASK)
                .templateId(messageTemplateId)
                .useDefaultEmail();

        customizer.accept(taskBuilder);

        BpmnModelInstance model = taskBuilder
                .endEvent()
                .done();

        return triggerService.addTrigger(model, null);
    }

    private ProcessDefinition prepareProcess() {
        return prepareProcess(builder -> {
        });
    }

    private void runAndAssertControlError(Uid uid, ProcessDefinition process) throws Exception {
        assertTrue(
                "Expected error did not happen",
                runExpectingControlError(uid, process)
        );
    }

    private boolean runExpectingControlError(Uid uid, ProcessDefinition process) throws Exception {
        try {
            runTask(uid, process);
        } catch (Exception e) {
            if (BpmnErrorMatcher.expectCode(ProcessErrorCodes.IN_GLOBAL_CONTROL).matches(e)) {
                return true;
            }
            throw e;
        }
        return false;
    }
}
