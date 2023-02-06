package ru.yandex.market.jmf.module.automation.test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.logic.wf.bcp.WfConstants;
import ru.yandex.market.jmf.module.automation.AutomationRule;
import ru.yandex.market.jmf.module.automation.ScheduleAutomationRule;
import ru.yandex.market.jmf.module.automation.test.utils.AutomationRuleTestUtils;
import ru.yandex.market.jmf.module.entity.snapshot.SnapshottedByStatusLogic;
import ru.yandex.market.jmf.module.scheduling.CronSchedulePlanning;
import ru.yandex.market.jmf.module.scheduling.EntityScheduler;
import ru.yandex.market.jmf.module.scheduling.SchedulePlanning;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskDao;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskProcessor;
import ru.yandex.market.jmf.queue.retry.internal.SlowRetryTasksQueue;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScheduleAutomationRuleTest extends AbstractAutomationRuleTest {

    private static final String ACTIVE_STATUS = "active";
    private static final String INT_ATTR = "intAttr";

    @Inject
    TxService txService;

    @Inject
    RetryTaskProcessor retryTaskProcessor;

    @Inject
    SlowRetryTasksQueue retryTasksQueue;

    @Inject
    RetryTaskDao retryTaskDao;

    @Inject
    AutomationRuleTestUtils automationRuleTestUtils;

    @Inject
    ConfigurationService configurationService;

    @BeforeEach
    public void setUp() {
        txService.runInNewTx(() -> {
            ouTestUtils.createOu();
            dbService.createQuery("delete from simple1").executeUpdate();
            retryTasksQueue.reset();
            txService.runInNewTx(() -> retryTaskDao.deleteTasks());
            configurationService.setValue("scheduleAutomationRuleEntitiesLimit", 2);
        });
    }

    /**
     * Тестирование работы правила автоматизации по расписанию
     * <br>
     * Подготовка:
     * <ul>
     *     <li>Устанавливаем в кофигурации "Максимальное кол-во объектов, на которых будет выполнено правило
     *     автоматизации по расписанию" в значение 2</li>
     *     <li>Создаем entity1 (тип simple1, значение атрибута intAttr 100)</li>
     *     <li>Создаем entity2 (тип simple1, значение атрибута intAttr 101)</li>
     *     <li>Создаем entity3 (тип simple1, значение атрибута intAttr 102)</li>
     *     <li>Создаем entity4 (тип simple1, значение атрибута intAttr 103)</li>
     *     <li>Создаем правило автоматизации по расписанию (расписание: каждые 2 секунды, Тип объектов: simple1,
     *     фильтры: объекты с intAttr больше 100, сортировка по intAttr, правило меняет значение атрибута ruleResult
     *     на success)</li>
     *     <li></li>
     *     <li></li>
     * </ul>
     * <p>
     * Ждем  2 секунды.
     * Проверки:
     * <ul>
     *     <li>У entity1 значение ruleResult - null (т.к. intAttr не больше 100)</li>
     *     <li>У entity2 значение ruleResult - success</li>
     *     <li>У entity3 значение ruleResult - success</li>
     *     <li>У entity4 значение ruleResult - null (т.к. Максимальное кол-во объектов - 2)</li>
     * </ul>
     */
    @Test
    public void testScheduleAutomationRule() {
        List<Entity> entities = createEntities();
        ScheduleAutomationRule rule = createScheduleAutomationRule();
        assertStatus(rule, SnapshottedByStatusLogic.Statuses.DRAFT);

        Exceptions.sneakyRethrow(() -> Thread.sleep(2100));
        txService.runInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        assertEntities(entities, null, null, null, null);

        changeRuleStatus(rule, SnapshottedByStatusLogic.Statuses.REVIEW);

        Exceptions.sneakyRethrow(() -> Thread.sleep(2100));
        txService.runInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        assertEntities(entities, null, null, null, null);

        changeRuleStatus(rule, SnapshottedByStatusLogic.Statuses.APPROVED);

        Exceptions.sneakyRethrow(() -> Thread.sleep(2100));
        txService.runInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        assertEntities(entities, null, SUCCESS, SUCCESS, null);
    }

    @Test
    public void testActiveRuleExecution() {
        List<Entity> entities = createEntities();
        ScheduleAutomationRule rule = createScheduleAutomationRule();
        forceChangeRuleStatus(rule, AutomationRule.Statuses.ACTIVE);

        Exceptions.sneakyRethrow(() -> Thread.sleep(2100));
        txService.runInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        assertEntities(entities, null, SUCCESS, SUCCESS, null);
    }

    @Test
    public void testSnapshotActiveRuleExecution() {
        List<Entity> entities = createEntities();
        ScheduleAutomationRule rule = createScheduleAutomationRule();
        forceChangeRuleStatus(rule, AutomationRule.Statuses.ACTIVE);

        txService.doInNewTx(() -> automationRuleTestUtils.editScheduleRuleFilters(rule,
                "/test/jmf/module/automation/rules/schedule/scheduleFilters.json", "99"));
        assertStatus(rule, SnapshottedByStatusLogic.Statuses.DRAFT);

        Exceptions.sneakyRethrow(() -> Thread.sleep(2100));
        txService.runInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        assertEntities(entities, null, SUCCESS, SUCCESS, null);
    }

    @Test
    public void testSnapshotRuleExecution() {
        List<Entity> entities = createEntities();
        ScheduleAutomationRule rule = createScheduleAutomationRule();

        changeRuleStatus(rule, SnapshottedByStatusLogic.Statuses.REVIEW);
        changeRuleStatus(rule, SnapshottedByStatusLogic.Statuses.APPROVED);

        resetResultValues(entities);
        txService.doInNewTx(() -> automationRuleTestUtils.editScheduleRuleFilters(rule,
                "/test/jmf/module/automation/rules/schedule/scheduleFilters.json", "99"));
        assertStatus(rule, SnapshottedByStatusLogic.Statuses.DRAFT);

        Exceptions.sneakyRethrow(() -> Thread.sleep(2100));
        txService.runInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        assertEntities(entities, null, SUCCESS, SUCCESS, null);

        resetResultValues(entities);
        changeRuleStatus(rule, SnapshottedByStatusLogic.Statuses.REVIEW);

        Exceptions.sneakyRethrow(() -> Thread.sleep(2100));
        txService.runInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        assertEntities(entities, null, SUCCESS, SUCCESS, null);

        resetResultValues(entities);
        changeRuleStatus(rule, SnapshottedByStatusLogic.Statuses.APPROVED);

        Exceptions.sneakyRethrow(() -> Thread.sleep(2100));
        txService.runInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        assertEntities(entities, SUCCESS, SUCCESS, null, null);
    }

    private List<Entity> createEntities() {
        return txService.doInNewTx(() -> List.of(
                bcpService.create(FQN_1, Map.of(INT_ATTR, "100")),
                bcpService.create(FQN_1, Map.of(INT_ATTR, "101")),
                bcpService.create(FQN_1, Map.of(INT_ATTR, "102")),
                bcpService.create(FQN_1, Map.of(INT_ATTR, "103"))
        ));
    }

    private ScheduleAutomationRule createScheduleAutomationRule() {
        return txService.doInNewTx(() -> {
            SchedulePlanning schedulePlanning = bcpService.create(CronSchedulePlanning.FQN, Map.of(
                    CronSchedulePlanning.CODE, Randoms.string(),
                    CronSchedulePlanning.TITLE, Randoms.string(),
                    CronSchedulePlanning.EXPRESSION, "0/2 * * * * ?"
            ));
            var scheduleRule = automationRuleTestUtils.createDraftScheduleRule(
                    "/test/jmf/module/automation/rules/schedule/scheduleRule.json",
                    FQN_1,
                    "/test/jmf/module/automation/rules/schedule/scheduleFilters.json",
                    Set.of(),
                    Set.of(ouTestUtils.getAnyCreatedOu()),
                    "100"
            );
            EntityScheduler entityScheduler = scheduleRule.getScheduler();
            bcpService.edit(entityScheduler, Map.of(
                    EntityScheduler.STATUS, ACTIVE_STATUS,
                    EntityScheduler.SCHEDULE_PLANNING, schedulePlanning
            ));
            return scheduleRule;
        });
    }

    private void assertResultAttr(Entity entity, String value) {
        Entity actual = txService.doInNewTx(() -> dbService.get(entity.getGid()));
        assertEquals(value, actual.getAttribute(RULE_RESULT_ATTR));
    }

    private void assertStatus(HasWorkflow entity, String status) {
        HasWorkflow actual = txService.doInNewTx(() -> dbService.get(entity.getGid()));
        assertEquals(status, actual.getStatus());
    }

    private void assertEntities(List<Entity> entities, String value1, String value2, String value3, String value4) {
        assertResultAttr(entities.get(0), value1);
        assertResultAttr(entities.get(1), value2);
        assertResultAttr(entities.get(2), value3);
        assertResultAttr(entities.get(3), value4);
    }

    private void resetResultValues(List<Entity> entities) {
        txService.runInNewTx(() -> {
            for (Entity entity : entities) {
                bcpService.edit(entity, Maps.of(RULE_RESULT_ATTR, null));
            }
        });
    }

    private void changeRuleStatus(ScheduleAutomationRule rule, String status) {
        txService.doInNewTx(() ->
                bcpService.edit(rule, Map.of(AutomationRule.STATUS, status))
        );
    }

    private void forceChangeRuleStatus(ScheduleAutomationRule rule, String status) {
        txService.doInNewTx(() ->
                bcpService.edit(rule,
                        Map.of(AutomationRule.STATUS, status),
                        Map.of(WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true)
                )
        );
    }
}
