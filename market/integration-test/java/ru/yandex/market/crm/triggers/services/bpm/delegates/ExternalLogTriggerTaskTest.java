package ru.yandex.market.crm.triggers.services.bpm.delegates;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateState;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.core.domain.messages.SmsMessageConf;
import ru.yandex.market.crm.core.domain.segment.export.IdType;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.external.smspassport.SendSmsResult;
import ru.yandex.market.crm.core.services.external.smspassport.SmsPassportClient;
import ru.yandex.market.crm.core.services.external.smspassport.domain.SendSmsRequestProperties;
import ru.yandex.market.crm.core.services.messages.MessageTemplatesDAO;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.TriggerService;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.delegates.log.TriggerExternalLog;
import ru.yandex.market.crm.triggers.test.AbstractTriggerTest;
import ru.yandex.market.crm.triggers.test.helpers.MockTriggerExternalLogger;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

public class ExternalLogTriggerTaskTest extends AbstractTriggerTest {
    private static final String TRIGGER_ID = "test_trigger";
    private static final String SMS_TASK = "sms_task";
    private static final String LOG_TASK = "log_task";
    private static final String LOG_EVENT_TYPE = "log_event_type";
    private static final String SMS_TEXT_TEMPLATE = "TEST12345";
    private static final String START_MESSAGE = MessageTypes.CART_ITEM_ADDED;
    private static final long PUID = 112233;

    @Inject
    private MessageTemplatesDAO messageTemplatesDAO;
    @Inject
    private TriggerService triggerService;
    @Inject
    private SmsPassportClient smsPassportClient;
    @Inject
    private MockTriggerExternalLogger triggerExternalLogger;

    private MessageTemplate<SmsMessageConf> smsMessageTemplate;

    @Before
    public void setUp() {
        triggerExternalLogger.clearHistory();

        smsMessageTemplate = prepareSmsMessageTemplate();

        doReturn(
                CompletableFuture.completedFuture(new SendSmsResult(111L, "", ""))
        ).when(smsPassportClient).sendSms(any(Uid.class), any(SendSmsRequestProperties.class));
    }

    /**
     * В лог для внешних потребителей, если не указаны конкретные переменные процесса, попадут абсолютно все переменные
     * которые присутствовали на момент выполнения блока логирования
     */
    @Test
    public void testLogProcessData() {
        Map<String, Object> variables = new HashMap<>() {
            {
                put("var_int", 123);
                put("var_string", "12345");
            }
        };
        prepareTrigger();

        List<MessageCorrelationResult> correlationResults = sendPuidMessage(variables);
        assertEquals(1, correlationResults.size());

        ProcessInstance processInstance = correlationResults.get(0).getProcessInstance();
        processSpy.waitProcessEnd(processInstance.getId());

        TriggerExternalLog log = triggerExternalLogger.getLogHistory().get(0);

        assertEquals(LOG_TASK, log.getActivityId());
        assertEquals(LOG_EVENT_TYPE, log.getEventType());
        assertTrue(log.getTriggerId().contains(TRIGGER_ID));

        Map<String, Object> logVars = log.getVars();

        variables.put(ProcessVariablesNames.SMS_TEXT, SMS_TEXT_TEMPLATE);
        variables.put(ProcessVariablesNames.ID_TYPE, IdType.PUID.name());
        variables.put(ProcessVariablesNames.ID_VALUE, String.valueOf(PUID));

        assertEquals(variables, logVars);
    }

    /**
     * При логировании события должно сохраняться время, близкое к времени выполнения этого события
     */
    @Test
    public void testCorrectLogEventTime() {
        AtomicReference<Long> sendingSmsTime = new AtomicReference<>();
        doAnswer(invocation -> {
            sendingSmsTime.set(Instant.now().getEpochSecond());
            return CompletableFuture.completedFuture(new SendSmsResult(111L, "", ""));
        }).when(smsPassportClient).sendSms(any(Uid.class), any(SendSmsRequestProperties.class));

        prepareTrigger();

        List<MessageCorrelationResult> correlationResults = sendPuidMessage(Map.of());
        assertEquals(1, correlationResults.size());

        ProcessInstance processInstance = correlationResults.get(0).getProcessInstance();
        processSpy.waitProcessEnd(processInstance.getId());

        TriggerExternalLog log = triggerExternalLogger.getLogHistory().get(0);

        assertEquals(sendingSmsTime.get(), Instant.parse(log.getDate()).getEpochSecond(), 1);
        assertEquals(sendingSmsTime.get(), log.getTimestamp(), 1);
    }

    /**
     * Если в конфигурации блока логирования указаны переменные процесса, то только они будут отображены в логе
     * в поле vars
     */
    @Test
    public void testLogProcessDataWithSpecifiedProcessVarables() {
        Map<String, Object> variables = new HashMap<>() {
            {
                put("var_int", 123);
                put("var_string", "12345");
            }
        };
        prepareTrigger("var_int", ProcessVariablesNames.SMS_TEXT);

        List<MessageCorrelationResult> correlationResults = sendPuidMessage(variables);
        assertEquals(1, correlationResults.size());

        ProcessInstance processInstance = correlationResults.get(0).getProcessInstance();
        processSpy.waitProcessEnd(processInstance.getId());

        TriggerExternalLog log = triggerExternalLogger.getLogHistory().get(0);

        assertEquals(LOG_TASK, log.getActivityId());
        assertEquals(LOG_EVENT_TYPE, log.getEventType());
        assertTrue(log.getTriggerId().contains(TRIGGER_ID));

        Map<String, Object> logVars = log.getVars();

        Map<String, Object> expectedVars = Map.of(
                ProcessVariablesNames.SMS_TEXT, SMS_TEXT_TEMPLATE,
                "var_int", 123
        );
        assertEquals(expectedVars, logVars);
    }

    /**
     * Если в конфигурации блока логирования указаны переменные процесса, которые на момент отправки лога отсутствуют
     * в контексте, то такие переменные не попадают в поле vars
     */
    @Test
    public void testLogProcessDataWithMissingProcessVarables() {
        Map<String, Object> variables = new HashMap<>() {
            {
                put("var_int", 123);
                put("var_string", "12345");
            }
        };
        prepareTrigger("var_int", "var_unknown_param", ProcessVariablesNames.SMS_TEXT);

        List<MessageCorrelationResult> correlationResults = sendPuidMessage(variables);
        assertEquals(1, correlationResults.size());

        ProcessInstance processInstance = correlationResults.get(0).getProcessInstance();
        processSpy.waitProcessEnd(processInstance.getId());

        TriggerExternalLog log = triggerExternalLogger.getLogHistory().get(0);

        assertEquals(LOG_TASK, log.getActivityId());
        assertEquals(LOG_EVENT_TYPE, log.getEventType());
        assertTrue(log.getTriggerId().contains(TRIGGER_ID));

        Map<String, Object> logVars = log.getVars();

        Map<String, Object> expectedVars = Map.of(
                ProcessVariablesNames.SMS_TEXT, SMS_TEXT_TEMPLATE,
                "var_int", 123
        );
        assertEquals(expectedVars, logVars);
    }

    private void prepareTrigger(String... logProcessVariables) {
        BpmnModelInstance instance = TriggersHelper.triggerBuilder(TRIGGER_ID)
                .startEvent().message(START_MESSAGE)
                .sendSmsTask(SMS_TASK)
                .templateId(smsMessageTemplate.getId())
                .triggerExternalLogTask(LOG_TASK)
                .setLogProcessVariables(String.join(", ", logProcessVariables))
                .setLogEventType(LOG_EVENT_TYPE)
                .endEvent()
                .done();

        ProcessDefinition processDefinition = triggerService.addTrigger(instance, null);
        triggerService.changeStateByKey(processDefinition.getKey(), false);
    }

    private MessageTemplate<SmsMessageConf> prepareSmsMessageTemplate() {
        SmsMessageConf config = new SmsMessageConf();
        config.setTextTemplate(SMS_TEXT_TEMPLATE);

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

    private List<MessageCorrelationResult> sendPuidMessage(Map<String, Object> vars) {
        return triggerService.sendBpmMessage(
                new UidBpmMessage(START_MESSAGE, Uid.asPuid(PUID), vars)
        );
    }
}
