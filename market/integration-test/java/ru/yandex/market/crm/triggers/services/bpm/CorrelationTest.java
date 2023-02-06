package ru.yandex.market.crm.triggers.services.bpm;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.MessageCorrelationResultType;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.segments.SegmentsDAO;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.core.test.utils.PlatformHelper;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.triggers.BpmnModelTransformer;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.platform.api.Edge;
import ru.yandex.market.crm.platform.api.IdsGraph;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.triggers.test.AbstractTriggerTest;
import ru.yandex.market.crm.triggers.test.helpers.EmailMessageTemplatesTestHelper;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper;
import ru.yandex.market.crm.triggers.test.helpers.builders.CrmProcessBuilder;
import ru.yandex.market.mcrm.db.Constants;
import ru.yandex.market.mcrm.tx.TxService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.accessMarketFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.allUsers;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.emailsFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.YaSenderHelper.sendTxResult;
import static ru.yandex.market.crm.triggers.test.helpers.EmailMessageTemplatesTestHelper.SENDER_CAMPAIGN_SLUG;

/**
 * Тесты на корреляцию событий в триггерах
 */
public class CorrelationTest extends AbstractTriggerTest {

    private class SimpleTriggerBuilder {
        private String key;

        private String startMessage;
        private String intermediateMessage;
        private String segmentId;

        private boolean activate = true;

        SimpleTriggerBuilder setKey(String key) {
            this.key = key;
            return this;
        }

        SimpleTriggerBuilder setStartMessage(String startMessage) {
            this.startMessage = startMessage;
            return this;
        }

        SimpleTriggerBuilder setIntermediateMessage(String intermediateMessage) {
            this.intermediateMessage = intermediateMessage;
            return this;
        }

        SimpleTriggerBuilder doNotActivate() {
            this.activate = false;
            return this;
        }

        SimpleTriggerBuilder setSegmentId(String segmentId) {
            this.segmentId = segmentId;
            return this;
        }

        ProcessDefinition build() {
            if (key == null) {
                key = TRIGGER_KEY;
            }

            if (startMessage == null) {
                startMessage = START_MESSAGE;
            }

            AbstractFlowNodeBuilder builder = TriggersHelper.triggerBuilder(key)
                    .name(TRIGGER_NAME)
                    .startEvent().message(startMessage).segment(segmentId);

            if (null != intermediateMessage) {
                builder = builder.intermediateCatchEvent(INTER_MESSAGE_ID)
                        .message(intermediateMessage);
            }

            BpmnModelInstance modelInstance = builder.endEvent()
                    .done();

            return prepareTrigger(modelInstance, activate);
        }
    }

    private SimpleTriggerBuilder triggerBuilder() {
        return new SimpleTriggerBuilder();
    }

    private static final Logger LOG = LoggerFactory.getLogger(CorrelationTest.class);

    private static final String TRIGGER_KEY = "corr_test";
    private static final String TRIGGER_NAME = "Correlation Test";
    private static final String START_MESSAGE = MessageTypes.CART_ITEM_ADDED;
    private static final String SUBPROCESS_START_MESSAGE = MessageTypes.COIN_CREATED;
    private static final String EVENT_SUBPROCESS_ID = "EventSubProcess_kjd784fdv";
    private static final String INTER_MESSAGE = MessageTypes.CART_ITEM_REMOVED;
    private static final String INTER_MESSAGE_ID = "IntermediateCatchEvent_sf3854fkj";

    private static final Uid PUID = Uid.asPuid(999L);
    private static final ru.yandex.market.crm.platform.commons.Uid PLATFORM_PUID
            = ru.yandex.market.crm.platform.commons.Uid.newBuilder()
                                                       .setType(UidType.PUID)
                                                       .setIntValue(999L)
                                                       .build();

    @Inject
    private TriggerService triggerService;
    @Inject
    private SegmentsDAO segmentsDAO;
    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;
    @Inject
    private TransactionTemplate transactionTemplate;
    @Inject
    private EmailMessageTemplatesTestHelper messageTemplatesTestHelper;
    @Inject
    private YaSenderHelper yaSenderHelper;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private TxService txService;
    @Inject
    private PlatformHelper platformHelper;

    @Inject
    @Named(Constants.DEFAULT_JDBC_TEMPLATE)
    private JdbcTemplate jdbcTemplate;

    private static ru.yandex.market.crm.platform.api.User user() {
        return ru.yandex.market.crm.platform.api.User.newBuilder()
                .setId("user-" + PUID)
                .setIdsGraph(
                        IdsGraph.newBuilder()
                            .addNode(PLATFORM_PUID)
                            .addNode(
                                    ru.yandex.market.crm.platform.commons.Uid.newBuilder()
                                            .setType(UidType.YANDEXUID)
                                            .setStringValue(String.valueOf(System.currentTimeMillis())).build()
                            )
                            .addEdge(
                                    Edge.newBuilder()
                                            .setNode1(0)
                                            .setNode2(1)
                            )
                )
                .build();
    }

    @After
    public void tearDown() {
        processSpy.waitProcessesEnd(Duration.ofSeconds(30));
    }

    /**
     * Проверка запуска процесса по стартовому событию
     */
    @Test
    public void simpleStartProcess() {
        ProcessDefinition processDefinition = triggerBuilder().build();
        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);

        ProcessInstance processInstance = assertStartProcess(correlationResults, processDefinition);
        assertEquals(processDefinition.getId(), processInstance.getProcessDefinitionId());
        processSpy.waitProcessEnd(processInstance.getId());
    }

    /**
     * Если на стартовое событие, настроены несколько разных триггеров,
     * то должны запуститься процессы по каждому триггеру
     */
    @Test
    public void doubleStartProcesses() {
        ProcessDefinition pd1 = triggerBuilder()
                .setKey(TRIGGER_KEY + "_1")
                .build();

        ProcessDefinition pd2 = triggerBuilder()
                .setKey(TRIGGER_KEY + "_2")
                .build();

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);

        assertEquals(2, correlationResults.size());
        ProcessInstance pi1 = assertStartProcess(correlationResults.get(0));
        ProcessInstance pi2 = assertStartProcess(correlationResults.get(1));

        assertEquals(
                Set.of(pd1.getId(), pd2.getId()),
                Set.of(pi1.getProcessDefinitionId(), pi2.getProcessDefinitionId())
        );

        processSpy.waitProcessEnd(pi1.getId());
        processSpy.waitProcessEnd(pi2.getId());
    }

    /**
     * Сообщение ProcessInstanceBpmMessage не должно запускать процесс
     */
    @Test
    public void processInstanceMessageDoesntStartProcess() {
        triggerBuilder().build();

        List<MessageCorrelationResult> correlationResults = triggerService.sendBpmMessage(
                new ProcessInstanceBpmMessage(
                        UUID.randomUUID().toString(),
                        START_MESSAGE,
                        Collections.emptyMap()
                )
        );
        assertEquals(0, correlationResults.size());
        assertTrue(processSpy.getCurrentActivityStates().isEmpty());
    }

    /**
     * ProcessInstanceBpmMessage сообщение должно правильно скоррелировать
     * на IntermediateCatchEvent и продолжить выполнение процесса
     */
    @Test
    public void processInstanceMessageContinueExistProcess() {
        ProcessDefinition processDefinition = triggerBuilder()
                .setIntermediateMessage(INTER_MESSAGE)
                .build();

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);

        ProcessInstance processInstance = assertStartProcess(correlationResults, processDefinition);
        processSpy.waitActivityStart(processInstance.getId(), INTER_MESSAGE_ID);
        correlationResults = triggerService.sendBpmMessage(
                new ProcessInstanceBpmMessage(
                        processInstance.getId(),
                        INTER_MESSAGE,
                        Collections.emptyMap()
                )
        );
        Execution execution = assertCorrelateToExecution(correlationResults);
        assertEquals(processInstance.getId(), execution.getProcessInstanceId());
    }

    /**
     * Если после отправки письма приходит сообщение об его открытии оно коррелируется с процессом
     * из которого произошла отправка
     */
    @Test
    public void correlateGenericMessageContinueProcess() {
        platformHelper.prepareUser(UidType.EMAIL, "user@yandex.ru", null);

        String templateId = messageTemplatesTestHelper.prepareMessageTemplate();
        String messageId = UUID.randomUUID().toString();

        yaSenderHelper.onSendTransactional(SENDER_CAMPAIGN_SLUG, request -> sendTxResult(messageId));

        String sendingBlockId = "SendEmailTask";

        BpmnModelInstance model = TriggersHelper.triggerBuilder(TRIGGER_KEY)
                .startEvent().message(START_MESSAGE)
                .sendEmailTask(sendingBlockId).templateId(templateId)
                .intermediateEvent(INTER_MESSAGE_ID, MessageTypes.LETTER_OPENED)
                        .attribute(CustomAttributesNames.SENDING_BLOCK_ID, sendingBlockId)
                .endEvent()
                .done();

        ProcessDefinition processDefinition = prepareTrigger(model, true);

        List<MessageCorrelationResult> correlationResults = triggerService.sendBpmMessage(
                new UidBpmMessage(
                        START_MESSAGE,
                        Uid.asEmail("user@yandex.ru"),
                        Map.of()
                )
        );

        ProcessInstance pi = assertStartProcess(correlationResults, processDefinition);
        processSpy.waitActivityStart(pi.getId(), INTER_MESSAGE_ID);

        triggerService.sendGenericMessage(
                new GenericBpmMessage(MessageTypes.LETTER_OPENED, messageId)
        );

        processSpy.waitProcessEnd(pi.getId());
    }

    /**
     * Переменные процесса переданные в стартовом событии должны быть размещены в контексте процесса
     */
    @Test
    public void startMessageVariablesCheckInProcess() {
        Map<String, Object> variables = new HashMap<>() {
            {
                put("var_int", 17);
                put("var_long", 31L);
                put("var_string", "string_value_jhdj");
                put("null", null);
                put("var_obj", new User(UUID.randomUUID().toString()));
            }
        };

        ProcessDefinition processDefinition = triggerBuilder()
                .setIntermediateMessage(INTER_MESSAGE)
                .build();

        List<MessageCorrelationResult> correlationResults = triggerService.sendBpmMessage(
                new UidBpmMessage(START_MESSAGE, PUID, variables)
        );
        ProcessInstance processInstance = assertStartProcess(correlationResults, processDefinition);
        processSpy.waitActivityStart(processInstance.getId(), INTER_MESSAGE_ID);

        variables.put(ProcessVariablesNames.ID_TYPE, PUID.getType().name());
        variables.put(ProcessVariablesNames.ID_VALUE, PUID.getValue());
        Map<String, Object> runtimeVariables = processEngine.getRuntimeService().getVariables(processInstance.getId());
        assertEquals(variables, runtimeVariables);
        sendBluePuidMessage(INTER_MESSAGE);
    }

    /**
     * Процесс должен запуститься в активной предпоследней версии, если последняя в статусе draft
     */
    @Test
    public void startProcessNotDraftVersion() {
        ProcessDefinition activeVersion = triggerBuilder().build();
        ProcessDefinition draftVersion = updateTrigger(activeVersion, TRIGGER_NAME + " v2", false);
        assertNotNull("New draft version has not been created", draftVersion);

        assertNotEquals(activeVersion.getId(), draftVersion.getId());

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        ProcessInstance processInstance = assertStartProcess(correlationResults, activeVersion);
        processSpy.waitProcessEnd(processInstance.getId());
    }

    /**
     * Процесс не должен запускаться, если единственная версия в статусе draft
     */
    @Test
    public void draftNotStartProcess() {
        triggerBuilder()
                .doNotActivate()
                .build();

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        assertEquals(0, correlationResults.size());
        assertTrue(processSpy.getCurrentActivityStates().isEmpty());
    }

    /**
     * Процесс не должен запускаться, если триггер приостановлен
     */
    @Test
    public void suspendTriggerDontStartProcess() {
        ProcessDefinition processDefinition = triggerBuilder().build();
        triggerService.changeStateByKey(processDefinition.getKey(), true);

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        assertEquals(0, correlationResults.size());
        assertTrue(processSpy.getCurrentActivityStates().isEmpty());
    }

    /**
     * Процесс не должен запускаться, если все версии триггера в статусе draft
     */
    @Test
    public void allVersionsDraftsDontStartProcess() {
        ProcessDefinition triggerV1 = triggerBuilder()
                .doNotActivate()
                .build();

        ProcessDefinition triggerV2 = updateTrigger(triggerV1, TRIGGER_NAME + " v2", false);
        assertNotNull("New version has not been created", triggerV2);

        ProcessDefinition triggerV3 = updateTrigger(triggerV2, TRIGGER_NAME + " v3", false);
        assertNotNull("New version has not been created", triggerV3);

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        assertEquals(0, correlationResults.size());
        assertTrue(processSpy.getCurrentActivityStates().isEmpty());
    }

    /**
     * Процесс должен запускаться, даже если цвет события отличается от цвета триггера
     */
    @Test
    public void differentMessageColorStartsProcess() {
        triggerBuilder().build();

        List<MessageCorrelationResult> correlationResults = triggerService.sendBpmMessage(
                new UidBpmMessage(START_MESSAGE, PUID, Collections.emptyMap())
        );

        assertEquals(1, correlationResults.size());
        ProcessInstance pi1 = assertStartProcess(correlationResults.get(0));
        assertEquals(TRIGGER_KEY, pi1.getProcessDefinitionId().split(":")[0]);
        processSpy.waitProcessEnd(pi1.getId());
    }

    /**
     * Не должен запускаться новый процесс, если процесс для указанных correlation keys уже запущен.
     */
    @Test
    public void dontStartNewProcessIfSameKeysProcessExist() {
        ProcessDefinition processDefinition = triggerBuilder()
                .setIntermediateMessage(INTER_MESSAGE)
                .build();

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        ProcessInstance processInstance = assertStartProcess(correlationResults, processDefinition);
        processSpy.waitActivityStart(processInstance.getId(), INTER_MESSAGE_ID);

        correlationResults = sendBluePuidMessage(START_MESSAGE);
        assertEquals(0, correlationResults.size());

        sendBluePuidMessage(INTER_MESSAGE);
    }

    /**
     * Не должен создаваться новый процесс, если внутри той же транзакции
     * процесс для указанного идентификатора пользователя уже создан.
     */
    @Test
    public void dontCreateNewProcessIfSameUidProcessExistInOneTransaction() {
        ProcessDefinition processDefinition = triggerBuilder()
                .setIntermediateMessage(INTER_MESSAGE)
                .build();

        transactionTemplate.execute(status -> {
            List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
            assertStartProcess(correlationResults, processDefinition);
            correlationResults = sendBluePuidMessage(START_MESSAGE);
            assertEquals(0, correlationResults.size());
            return null;
        });
        sendBluePuidMessage(INTER_MESSAGE);
    }

    /**
     * Не должен создаваться новый процесс, если
     * внутри той же транзакции процесс для указанных correlation keys уже создан.
     */
    @Test
    public void dontCreateNewProcessIfSameKeysProcessExistInOneTransaction() {
        ProcessDefinition processDefinition = triggerBuilder()
                .setIntermediateMessage(INTER_MESSAGE)
                .build();

        transactionTemplate.execute(status -> {
            List<MessageCorrelationResult> correlationResults = triggerService.sendBpmMessage(
                    new UidBpmMessage(START_MESSAGE, PUID, Map.of(), Map.of("model_id", 179))
            );
            assertStartProcess(correlationResults, processDefinition);
            correlationResults = triggerService.sendBpmMessage(
                    new UidBpmMessage(START_MESSAGE, PUID, Map.of(), Map.of("model_id", 179))
            );
            assertEquals(0, correlationResults.size());
            return null;
        });
        sendBluePuidMessage(INTER_MESSAGE);

        processSpy.waitProcessesEnd(Duration.ofSeconds(30));

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM locked_correlation_keys)",
                Boolean.class
        );

        assertEquals(Boolean.FALSE, exists);
    }

    /**
     * Процесс должен запускаться в последней активной версии, даже если существует предыдущая активная версия
     */
    @Test
    public void startNewProcessInLastActiveVersion() {
        ProcessDefinition firstVersion = triggerBuilder().build();

        ProcessDefinition lastVersion = updateTrigger(firstVersion, TRIGGER_NAME + " v2", true);
        assertNotNull("New version has not been prepared", lastVersion);

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        ProcessInstance processInstance = assertStartProcess(correlationResults, lastVersion);
        processSpy.waitProcessEnd(processInstance.getId());
    }

    /**
     * Если puid есть в таблице пользователей и стартовое событие настроено на сегмент
     * с условием "Все пользователи" должен запуститься процесс по событию с этим puid
     */
    @Test
    public void allUsersSegmentStartNewProcess() {
        platformHelper.prepareUser(PLATFORM_PUID.getType(), String.valueOf(PLATFORM_PUID.getIntValue()), user());
        Segment segment = segment(allUsers());
        segmentsDAO.createOrUpdateSegment(segment);

        ProcessDefinition processDefinition = triggerBuilder()
                .setSegmentId(segment.getId())
                .build();

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        ProcessInstance processInstance = assertStartProcess(correlationResults, processDefinition);
        processSpy.waitProcessEnd(processInstance.getId());
    }

    /**
     * Процесс не должен запускаться, если настроен сегмент на стартовом событии, но пользователя нет в БД
     */
    @Test
    public void segmentButUserIsAbsentDontStartNewProcess() {
        platformHelper.prepareUser(PLATFORM_PUID.getType(), String.valueOf(PLATFORM_PUID.getIntValue()), null);

        Segment segment = segment(allUsers());
        segmentsDAO.createOrUpdateSegment(segment);

        triggerBuilder()
                .setSegmentId(segment.getId())
                .build();

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        assertEquals(0, correlationResults.size());
        assertTrue(processSpy.getCurrentActivityStates().isEmpty());
    }

    /**
     * При попытке выполнить процесс на сегменте с офлайн условием, должно выкидывать исключение
     */
    @Test(expected = IllegalArgumentException.class)
    public void offlineSegmentNotStartNewProcess() {
        platformHelper.prepareUser(PLATFORM_PUID.getType(), String.valueOf(PLATFORM_PUID.getIntValue()), user());
        Segment segment = segment(accessMarketFilter());
        segmentsDAO.createOrUpdateSegment(segment);

        triggerBuilder()
                .setSegmentId(segment.getId())
                .build();

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        assertEquals(0, correlationResults.size());
        assertTrue(processSpy.getCurrentActivityStates().isEmpty());
    }

    /**
     * Процесс не должен запускаться, если пользователь не попадает в сегмент стартового события
     */
    @Test
    public void userNotInSegmentDontStartNewProcess() {
        platformHelper.prepareUser(PLATFORM_PUID.getType(), String.valueOf(PLATFORM_PUID.getIntValue()), user());
        Segment segment = segment(emailsFilter("test@beru.ru"));
        segmentsDAO.createOrUpdateSegment(segment);

        triggerBuilder()
                .setSegmentId(segment.getId())
                .build();

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        assertEquals(0, correlationResults.size());
        assertTrue(processSpy.getCurrentActivityStates().isEmpty());
    }

    /**
     * Если в триггере настроен подпроцесс слушающий события,
     * то при отправке такого события подпроцесс должен отработать
     */
    @Test
    public void checkListenerSubProcessCorrelation() {
        CrmProcessBuilder triggerBuilder = TriggersHelper.triggerBuilder(TRIGGER_KEY);

        triggerBuilder
                .startEvent().message(START_MESSAGE)
                .intermediateCatchEvent(INTER_MESSAGE_ID).message(INTER_MESSAGE)
                .endEvent();

        triggerBuilder.eventSubProcess(EVENT_SUBPROCESS_ID)
                .startEvent().message(SUBPROCESS_START_MESSAGE)
                .endEvent();

        BpmnModelInstance modelInstance = triggerBuilder.done();
        ProcessDefinition processDefinition = prepareTrigger(modelInstance, true);

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        ProcessInstance pi = assertStartProcess(correlationResults, processDefinition);
        processSpy.waitActivityStart(pi.getId(), INTER_MESSAGE_ID);

        correlationResults = sendBluePuidMessage(SUBPROCESS_START_MESSAGE);
        assertCorrelateToExecution(correlationResults);
        processSpy.waitActivityEnd(pi.getId(), EVENT_SUBPROCESS_ID);

        sendBluePuidMessage(INTER_MESSAGE);
    }

    /**
     * Если в триггере настроен подпроцесс слушающий событие и одновременно
     * такое же событие слушает основной процесс,
     * то событие коррелируется только на основной процесс
     */
    @Test
    public void sameMessageNoSubProcessCorrelation() {
        CrmProcessBuilder triggerBuilder = TriggersHelper.triggerBuilder(TRIGGER_KEY);

        triggerBuilder
                .startEvent().message(START_MESSAGE)
                .intermediateCatchEvent(INTER_MESSAGE_ID).message(INTER_MESSAGE)
                .endEvent();

        triggerBuilder.eventSubProcess(EVENT_SUBPROCESS_ID)
                .startEvent().message(INTER_MESSAGE)
                .endEvent();

        BpmnModelInstance modelInstance = triggerBuilder.done();
        ProcessDefinition processDefinition = prepareTrigger(modelInstance, true);

        List<MessageCorrelationResult> correlationResults = sendBluePuidMessage(START_MESSAGE);
        ProcessInstance pi = assertStartProcess(correlationResults, processDefinition);
        processSpy.waitActivityStart(pi.getId(), INTER_MESSAGE_ID);

        correlationResults = sendBluePuidMessage(INTER_MESSAGE);
        assertCorrelateToExecution(correlationResults);

        assertNull(processSpy.getCurrentActivityStates().get(pi.getId()).get(EVENT_SUBPROCESS_ID));
    }

    /**
     * В случае если помимо идентификатора пользователя в корреляции участвует
     * еще одна переменная (productItemId), при совпадении значений переменных id пользователя и productItemId
     * в сообщении с переменными процесса, сообщение коррелируется с процессом
     */
    @Test
    public void testMessageIsCorrelatedIfAllVarsMatch() {
        ProcessDefinition processDefinition = triggerBuilder()
                .setStartMessage(MessageTypes.WISHLIST_ITEM_ADDED)
                .setIntermediateMessage(MessageTypes.WISHLIST_ITEM_REMOVED)
                .build();

        List<MessageCorrelationResult> correlationResults = sendProductItemMessage(
                MessageTypes.WISHLIST_ITEM_ADDED,
                "111"
        );

        ProcessInstance pi = assertStartProcess(correlationResults, processDefinition);
        processSpy.waitActivityStart(pi.getId(), INTER_MESSAGE_ID);

        correlationResults = sendProductItemMessage(
                MessageTypes.WISHLIST_ITEM_REMOVED,
                "111"
        );

        assertCorrelateToExecution(correlationResults);
    }

    /**
     * В случае если помимо идентификатора пользователя в корреляции участвует
     * еще одна переменная (productItemId), при неравенстве переменной productItemId
     * в сообщении с переменной процесса, сообщение не коррелируется с процессом
     */
    @Test
    public void testMessageIsNotCorrelatedIfNotAllVarsMatch() {
        ProcessDefinition processDefinition = triggerBuilder()
                .setStartMessage(MessageTypes.WISHLIST_ITEM_ADDED)
                .setIntermediateMessage(MessageTypes.WISHLIST_ITEM_REMOVED)
                .build();

        List<MessageCorrelationResult> correlationResults = sendProductItemMessage(
                MessageTypes.WISHLIST_ITEM_ADDED,
                "111"
        );

        ProcessInstance pi = assertStartProcess(correlationResults, processDefinition);
        processSpy.waitActivityStart(pi.getId(), INTER_MESSAGE_ID);

        correlationResults = sendProductItemMessage(
                MessageTypes.WISHLIST_ITEM_REMOVED,
                "222"
        );

        assertTrue("Message has been correlated", correlationResults.isEmpty());

        correlationResults = sendProductItemMessage(
                MessageTypes.WISHLIST_ITEM_REMOVED,
                "111"
        );
        assertFalse(correlationResults.isEmpty());
    }

    private List<MessageCorrelationResult> sendBluePuidMessage(String messageType) {
        return txService.doInNewTx(() -> triggerService.sendBpmMessage(
                new UidBpmMessage(messageType, PUID, Collections.emptyMap())
        ));
    }

    private ProcessInstance assertStartProcess(List<MessageCorrelationResult> correlationResults,
                                               ProcessDefinition pd) {
        assertEquals(1, correlationResults.size());
        return assertStartProcess(correlationResults.get(0), pd);
    }

    private ProcessInstance assertStartProcess(MessageCorrelationResult cr, ProcessDefinition pd) {
        ProcessInstance pi = assertStartProcess(cr);
        assertEquals(pd.getId(), pi.getProcessDefinitionId());
        return pi;
    }

    private ProcessInstance assertStartProcess(MessageCorrelationResult cr) {
        assertEquals(MessageCorrelationResultType.ProcessDefinition, cr.getResultType());
        ProcessInstance pi = cr.getProcessInstance();
        assertNotNull(pi);
        return pi;
    }

    private Execution assertCorrelateToExecution(List<MessageCorrelationResult> correlationResults) {
        assertEquals(1, correlationResults.size());
        MessageCorrelationResult correlationResult = correlationResults.get(0);
        assertEquals(MessageCorrelationResultType.Execution, correlationResult.getResultType());
        Execution execution = correlationResult.getExecution();
        assertNotNull(execution);
        return execution;
    }

    @Nullable
    private ProcessDefinition updateTrigger(ProcessDefinition trigger, String newName, boolean activate) {
        BpmnModelInstance model = repositoryService.getBpmnModelInstance(trigger.getId());
        model.getModelElementsByType(Process.class).forEach(process -> process.setName(newName));

        ProcessDefinition newVersion = triggerService.updateTrigger(model, null);

        if (null != newVersion && activate) {
            triggerService.changeStateById(newVersion.getId(), false);
        }

        return newVersion;
    }

    private ProcessDefinition prepareTrigger(BpmnModelInstance model, boolean activate) {
        LOG.info("Create trigger:\n{}", BpmnModelTransformer.transformModelToXml(model));
        ProcessDefinition processDefinition = triggerService.addTrigger(model, null);
        if (activate) {
            triggerService.changeStateByKey(processDefinition.getKey(), false);
        }

        return processDefinition;
    }

    private List<MessageCorrelationResult> sendProductItemMessage(String messageType, String productItemId) {
        return triggerService.sendBpmMessage(
                new UidBpmMessage(
                        messageType,
                        PUID,
                        Map.of(ProcessVariablesNames.PRODUCT_ITEM_ID, productItemId),
                        Map.of(ProcessVariablesNames.PRODUCT_ITEM_ID, productItemId)
                )
        );
    }
}
