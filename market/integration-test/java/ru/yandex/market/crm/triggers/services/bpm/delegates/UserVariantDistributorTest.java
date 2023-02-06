package ru.yandex.market.crm.triggers.services.bpm.delegates;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateState;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.core.domain.messages.SmsMessageConf;
import ru.yandex.market.crm.core.services.control.LocalControlSaltModifier;
import ru.yandex.market.crm.core.services.external.smspassport.SendSmsResult;
import ru.yandex.market.crm.core.services.external.smspassport.SmsPassportClient;
import ru.yandex.market.crm.core.services.external.smspassport.domain.SendSmsRequestProperties;
import ru.yandex.market.crm.core.services.messages.MessageTemplatesDAO;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.TriggerService;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.test.AbstractTriggerTest;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.market.crm.util.Dates.MOSCOW_ZONE;

public class UserVariantDistributorTest extends AbstractTriggerTest {
    private static final String TRIGGER_ID = "test_trigger";
    private static final String SMS_TASK = "sms_task";
    private static final String VARIANT_DISTRIBUTOR_TASK = "variant_distributor_task";
    private static final String SMS_TEXT_TEMPLATE = "TEST12345";
    private static final String START_MESSAGE = MessageTypes.CART_ITEM_ADDED;

    @Inject
    private MessageTemplatesDAO messageTemplatesDAO;
    @Inject
    private TriggerService triggerService;
    @Inject
    private SmsPassportClient smsPassportClient;

    private CollectPuidsWithSmsAnswer collectPuidsWithSmsAnswer;
    private MessageTemplate<SmsMessageConf> smsMessageTemplate;

    @Before
    public void setUp() {
        collectPuidsWithSmsAnswer = new CollectPuidsWithSmsAnswer();
        doAnswer(collectPuidsWithSmsAnswer).when(smsPassportClient)
                .sendSms(any(Uid.class), any(SendSmsRequestProperties.class));
        smsMessageTemplate = prepareSmsMessageTemplate();
    }

    @After
    public void tearDown() {
        LocalControlSaltModifier.setClock(Clock.system(MOSCOW_ZONE));
    }

    /**
     * При прохождении блока разбиения по вариантам пользователи разбиваются в зависимости от указанного процента
     */
    @Test
    public void testVariantsDistributionsDependingOnSettings () {
        var usersCount = 100;
        var controlPercent = 30;
        prepareTrigger(controlPercent);

        for (var puid = 1; puid <= usersCount; puid++) {
            List<MessageCorrelationResult> correlationResults = sendPuidMessage(puid);
            assertEquals(1, correlationResults.size());

            var processInstance = correlationResults.get(0).getProcessInstance();
            processSpy.waitProcessEnd(processInstance.getId());
        }

        assertUsersCounts(collectPuidsWithSmsAnswer.notifiedPuids.size(), usersCount, controlPercent);
    }

    /**
     * При прохождении блока разбиения по вариантам, если указанный процент контрольной группы равен 0,
     * то всем пользователям отправляется коммуникация
     */
    @Test
    public void testZeroControlIfControlPercentIsZero() {
        prepareTrigger(0);

        var usersCount = 100;

        for (var puid = 1; puid <= usersCount; puid++) {
            List<MessageCorrelationResult> correlationResults = sendPuidMessage(puid);
            assertEquals(1, correlationResults.size());

            var processInstance = correlationResults.get(0).getProcessInstance();
            processSpy.waitProcessEnd(processInstance.getId());
        }

        assertEquals(usersCount, collectPuidsWithSmsAnswer.notifiedPuids.size());
    }

    /**
     * При прохождении блока разбиения по вариантам пользователи, попадающие в контрольную группу, меняются каждый месяц
     */
    @Test
    public void testDifferentVariantsDistributionsInDifferentMonthes() {
        var usersCount = 50;
        var controlPercent = 30;
        prepareTrigger(controlPercent);

        for (var puid = 1; puid <= usersCount; puid++) {
            List<MessageCorrelationResult> correlationResults = sendPuidMessage(puid);
            assertEquals(1, correlationResults.size());

            var processInstance = correlationResults.get(0).getProcessInstance();
            processSpy.waitProcessEnd(processInstance.getId());
        }

        var notifiedPuids1 = Set.of(collectPuidsWithSmsAnswer.notifiedPuids);
        assertUsersCounts(collectPuidsWithSmsAnswer.notifiedPuids.size(), usersCount, controlPercent);

        var clock = Clock.fixed(LocalDateTime.now().plusMonths(1).toInstant(ZoneOffset.UTC), MOSCOW_ZONE);
        LocalControlSaltModifier.setClock(clock);
        collectPuidsWithSmsAnswer.clear();

        for (var puid = 1; puid <= usersCount; puid++) {
            List<MessageCorrelationResult> correlationResults = sendPuidMessage(puid);
            assertEquals(1, correlationResults.size());

            var processInstance = correlationResults.get(0).getProcessInstance();
            processSpy.waitProcessEnd(processInstance.getId());
        }

        var notifiedPuids2 = Set.of(collectPuidsWithSmsAnswer.notifiedPuids);
        assertUsersCounts(collectPuidsWithSmsAnswer.notifiedPuids.size(), usersCount, controlPercent);

        assertNotEquals(notifiedPuids1, notifiedPuids2);
    }

    /**
     * При прохождении блока разбиения по вариантам пользователи, попавшие в контрольную группу, всегда будут попадать
     * в неё в рамках одного месяца
     */
    @Test
    public void testSimilarVariantsDistributionsInOneMonth() {
        var clock = Clock.fixed(LocalDateTime.now().withDayOfMonth(1).toInstant(ZoneOffset.UTC), MOSCOW_ZONE);
        LocalControlSaltModifier.setClock(clock);

        var usersCount = 50;
        var controlPercent = 30;
        prepareTrigger(controlPercent);

        for (var puid = 1; puid <= usersCount; puid++) {
            List<MessageCorrelationResult> correlationResults = sendPuidMessage(puid);
            assertEquals(1, correlationResults.size());

            var processInstance = correlationResults.get(0).getProcessInstance();
            processSpy.waitProcessEnd(processInstance.getId());
        }

        var notifiedPuids1 = Set.of(collectPuidsWithSmsAnswer.notifiedPuids);
        assertUsersCounts(collectPuidsWithSmsAnswer.notifiedPuids.size(), usersCount, controlPercent);

        collectPuidsWithSmsAnswer.clear();

        for (var puid = 1; puid <= usersCount; puid++) {
            List<MessageCorrelationResult> correlationResults = sendPuidMessage(puid);
            assertEquals(1, correlationResults.size());

            var processInstance = correlationResults.get(0).getProcessInstance();
            processSpy.waitProcessEnd(processInstance.getId());
        }

        var notifiedPuids2 = Set.of(collectPuidsWithSmsAnswer.notifiedPuids);
        assertUsersCounts(collectPuidsWithSmsAnswer.notifiedPuids.size(), usersCount, controlPercent);

        assertEquals(notifiedPuids1, notifiedPuids2);
    }

    private void assertUsersCounts(int actualCount, int usersCount, int controlPercent) {
        var min = (int) (usersCount - usersCount * (controlPercent + 10.0) / 100);
        var max = (int) (usersCount - usersCount * (controlPercent - 10.0) / 100);
        assertThat(actualCount, allOf(greaterThan(min), lessThan(max)));
    }

    private void prepareTrigger(int controlPercent) {
        var instance = TriggersHelper.triggerBuilder(TRIGGER_ID)
                .startEvent().message(START_MESSAGE)
                .userVariantDistributor(VARIANT_DISTRIBUTOR_TASK)
                .createVariantSequenceFlow("a", 100 - controlPercent, newFlow ->
                    newFlow.sendSmsTask(SMS_TASK)
                            .templateId(smsMessageTemplate.getId())
                            .endEvent()
                )
                .createVariantSequenceFlow("control", controlPercent, AbstractFlowNodeBuilder::endEvent)
                .done();

        var processDefinition = triggerService.addTrigger(instance, null);
        triggerService.changeStateByKey(processDefinition.getKey(), false);
    }

    private MessageTemplate<SmsMessageConf> prepareSmsMessageTemplate() {
        var config = new SmsMessageConf();
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

    private List<MessageCorrelationResult> sendPuidMessage(long puid) {
        return triggerService.sendBpmMessage(
                new UidBpmMessage(START_MESSAGE, Uid.asPuid(puid), Map.of())
        );
    }

    private static class CollectPuidsWithSmsAnswer implements Answer {

        private List<Long> notifiedPuids = new ArrayList<>();

        @Override
        public Object answer(InvocationOnMock invocation) {
            Uid argument = invocation.getArgument(0, Uid.class);
            notifiedPuids.add(Long.parseLong(argument.getValue()));
            return completedFuture(new SendSmsResult(123L, "", ""));
        }

        private void clear() {
            notifiedPuids = new ArrayList<>();
        }
    }
}