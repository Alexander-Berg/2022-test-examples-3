package ru.yandex.market.jmf.logic.def.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.def.test.impl.TestRetryTaskGroupingStrategy;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.queue.retry.internal.FastRetryTasksQueue;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskProcessor;
import ru.yandex.market.jmf.trigger.EntityEvent;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class TriggerTest {

    private static final Fqn FQN_0 = Fqn.of("triggerSimple");
    private static final Fqn FQN_1 = Fqn.of("triggerSimple$type1");
    private static final Fqn FQN_2 = Fqn.of("triggerSimple$type2");
    private static final Fqn FQN_3 = Fqn.of("triggerSimple$type3");
    private static final Fqn FQN_4 = Fqn.of("triggerSimple$type4");
    private static final Fqn FQN_WITH_REQUIRED = Fqn.of("triggerWithRequired");
    private static final Fqn FQN_ASYNC_FAILED = Fqn.of("triggerForAsyncFailTrigger");
    private static final Fqn FQN_GROUPING_STRATEGY = Fqn.of("triggerForTestGroupingStrategy");
    private static final Fqn FQN_ASYNC_DEDUPLICATION = Fqn.of("triggerForAsyncDeduplicatedTrigger");
    private static final Fqn FQN_ASYNC = Fqn.of("triggerForAsyncTrigger");
    private static final String ATTR_0 = "attr0";
    private static final String ATTR_1 = "attr1";
    private static final String ATTR_2 = "attr2";

    @Inject
    BcpService bcpService;
    @Inject
    DbService dbService;
    @Inject
    TxService txService;
    @Inject
    RetryTaskProcessor retryTaskProcessor;
    @Inject
    FastRetryTasksQueue queue;
    @Inject
    TriggerServiceImpl triggerService;

    @Inject
    TestRetryTaskGroupingStrategy testGroupingStrategy;

    @BeforeEach
    public void init() {
        dbService.createQuery("delete from triggerSimple").executeUpdate();
        queue.reset();
    }

    @AfterEach
    public void tearDown() {
        queue.reset();
    }

    @Test
    public void createTrigger() {
        Entity result = create();
        Assertions.assertEquals(
                "valueFromTrigger", result.getAttribute(ATTR_1),
                "Должны заполнить значение в триггере не создание объекта");
    }

    @Test
    public void editTrigger() {
        Entity result = create();

        bcpService.edit(result, ImmutableMap.of(ATTR_0, Randoms.string()));
        Assertions.assertEquals(
                "valueFromEditTrigger", result.getAttribute(ATTR_1), "Должны заполнить значение в триггере не " +
                        "создание объекта");
    }

    @Test
    public void deleteTrigger() {
        Entity entity = create();

        bcpService.delete(entity);

        List<Entity> result = dbService.list(Query.of(FQN_0));
        Assertions.assertEquals(1, result.size(), "Должен быть только один объект, созданный триггером на удаление");
        Assertions.assertEquals(
                "valueFromDeleteTrigger", result.get(0).getAttribute(ATTR_0), "Должны заполнить значение в триггере " +
                        "не создание объекта");
    }

    @Test
    public void stackExceed() {
        Entity result = bcpService.create(FQN_2, properties(Randoms.string()));

        bcpService.edit(result, ImmutableMap.of(ATTR_0, Randoms.string()));

        // скрипт написан с ошибкой в результате чего триггеры зацикливаются
        // (тригер на изменение объекта изменяет сам объект, что снова вызывает срабатывание триггера)
        // должна сработать защита от бесконечных зацикливании и глубина выполнения триггеров не должна превышать 16
        Assertions.assertEquals(
                "10000000000000000", result.getAttribute(ATTR_2), "Должны заполнить значение в триггере");
    }

    @Test
    public void asyncTrigger() {
        triggerService.withAsyncTriggersMode(() -> {
            Entity result = txService.doInNewTx(() -> bcpService.create(FQN_ASYNC, new HashMap<>()));
            Long value = result.getAttribute(ATTR_0);
            Assertions.assertEquals(Long.valueOf(0), value, "Триггер еще не должен сработать т.к. он ассинхронный и " +
                    "выполняется в отдельной " +
                    "транзакции");

            Thread.sleep(1100);
            txService.doInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(queue));

            Entity current = txService.doInNewTx(() -> dbService.get(result.getGid()));
            Long currentValue = current.getAttribute(ATTR_0);
            Assertions.assertEquals(Long.valueOf(1), currentValue, "Должен выполнится триггер на создание объекта");
        });
    }

    @Test
    public void asyncTrigger_defaultAlgorithm() {
        triggerService.withAsyncTriggersMode(() -> {
            Entity result = txService.doInNewTx(() -> {
                        Entity result2 = bcpService.create(FQN_ASYNC, new HashMap<>());
                        //редактируем 2 раза, но счетчик должен увеличиться тольно на 1
                        bcpService.edit(result2, new HashMap<>());
                        bcpService.edit(result2, new HashMap<>());
                        return result2;
                    }
            );
            Long value = result.getAttribute(ATTR_0);
            Assertions.assertEquals(Long.valueOf(0), value, "Триггер еще не должен сработать т.к. он ассинхронный и " +
                    "выполняется в отдельной " +
                    "транзакции");

            Thread.sleep(1100);
            txService.doInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(queue));

            Entity current = txService.doInNewTx(() -> dbService.get(result.getGid()));
            Long currentValue = current.getAttribute(ATTR_0);
            // результат == 2, т.к. 1 при создании + 1 при одном редактировании
            Assertions.assertEquals(Long.valueOf(2), currentValue, "Должен выполнится триггер на создание объекта");
        });
    }

    @Test
    public void asyncTrigger_noneAlgorithm() {
        triggerService.withAsyncTriggersMode(() -> {
            Entity result = txService.doInNewTx(() -> {
                        Entity result2 = bcpService.create(FQN_ASYNC_DEDUPLICATION, new HashMap<>());
                        //редактируем 2 раза
                        bcpService.edit(result2, new HashMap<>());
                        bcpService.edit(result2, new HashMap<>());
                        return result2;
                    }
            );
            Long value = result.getAttribute(ATTR_0);
            Assertions.assertEquals(Long.valueOf(0), value, "Триггер еще не должен сработать т.к. он ассинхронный и " +
                    "выполняется в отдельной " +
                    "транзакции");

            Thread.sleep(1100);
            txService.doInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(queue));

            Entity current = txService.doInNewTx(() -> dbService.get(result.getGid()));
            Long currentValue = current.getAttribute(ATTR_0);
            // результат == 3, т.к. 1 при создании + 1 при кажом редактировании редактировании, дедубликация включена
            Assertions.assertEquals(Long.valueOf(3), currentValue, "Должен выполнится триггер на создание объекта");
        });
    }

    /**
     * В тесте проверяется, что в асинхронном триггере с правильно заполненным атрибутом groupingStrategy выбирается
     * группа, взятая из соотвествующей стратегии, триггер выполняет действие
     */
    @Test
    public void asyncTrigger_shouldChooseRightGroupWhenGroupingStrategySpecified() {
        triggerService.withAsyncTriggersMode(() -> {
            Entity entity = txService.doInNewTx(() -> bcpService.create(FQN_GROUPING_STRATEGY, Map.of()));

            Assertions.assertEquals(0, testGroupingStrategy.getInvocations());

            txService.runInNewTx(() ->
                    triggerService.execute(new EntityEvent(
                            entity.getMetaclass(),
                            "eventForGroupingStrategySpecified",
                            entity,
                            entity)));

            Assertions.assertEquals(1, testGroupingStrategy.getInvocations(), "Должны были получить " +
                    "id группы из стратегии");
            Thread.sleep(1100);
            txService.doInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(queue));

            Entity actualEntity = txService.doInNewTx(() -> dbService.get(entity.getGid()));
            Assertions.assertEquals(1L, (Long) actualEntity.getAttribute(ATTR_1), "Должно выполниться действие " +
                    "триггера");

            testGroupingStrategy.resetInvocations();
        });
    }

    /**
     * В тесте проверяется, что асинхронный триггер с неправильно заполненным атрибутом groupingStrategy не выполняет
     * действие
     */
    @Test
    public void asyncTrigger_shouldChooseRightGroupWhenGroupingStrategySpecifiedButWrong() {
        triggerService.withAsyncTriggersMode(() -> {
            Entity entity = txService.doInNewTx(() -> bcpService.create(FQN_GROUPING_STRATEGY, Map.of()));

            txService.runInNewTx(() ->
                    triggerService.execute(new EntityEvent(entity.getMetaclass(),
                            "eventForGroupingStrategySpecifiedButWrong",
                            entity,
                            entity)));

            Thread.sleep(1100);
            txService.doInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(queue));

            Entity actualEntity = txService.doInNewTx(() -> dbService.get(entity.getGid()));
            Assertions.assertEquals(0L, (Long) actualEntity.getAttribute(ATTR_1), "Действие триггера не должно " +
                    "выполниться");
        });
    }

    @Test
    public void asyncTriggerWithFail() {
        triggerService.withAsyncTriggersMode(() -> {
            Entity result = txService.doInNewTx(() -> bcpService.create(FQN_ASYNC_FAILED,
                    new HashMap<>()));

            Thread.sleep(1100);
            retryTaskProcessor.processPendingTasksWithReset(queue);

            Entity current = txService.doInNewTx(() -> dbService.get(result.getGid()));
            String currentValue = current.getAttribute(ATTR_0);
            Assertions.assertEquals(
                    "async trigger error handled", currentValue,
                    "Должен выполнится обработчик ошибки выполнения триггера на создание объекта");
        });
    }

    @Test
    public void condition_true() {
        Entity result = bcpService.create(FQN_3, ImmutableMap.of(ATTR_0, "goodValue"));
        Assertions.assertEquals(
                "valueFromTrigger", result.getAttribute(ATTR_1), "Должны заполнить значение в триггере на создание " +
                        "объекта");
    }

    @Test
    public void condition_false() {
        Entity result = bcpService.create(FQN_3, ImmutableMap.of(ATTR_0, "badValue"));
        Assertions.assertNull(
                result.getAttribute(ATTR_1), "Триггер не должен выполнится т.к. условие не сработало");
    }

    @Test
    public void asyncCondition_true() throws Exception {
        Entity result = txService.doInNewTx(() -> bcpService.create(FQN_4, ImmutableMap.of(ATTR_0,
                "goodValue")));

        Thread.sleep(1100);
        retryTaskProcessor.processPendingTasksWithReset(queue);

        Entity current = txService.doInNewTx(() -> dbService.get(result.getGid()));
        Assertions.assertEquals(
                "valueFromTrigger", current.getAttribute(ATTR_1), "Должны заполнить значение в триггере на создание " +
                        "объекта");
    }

    @Test
    public void asyncCondition_false() throws Exception {
        Entity result = txService.doInNewTx(() -> bcpService.create(FQN_4, ImmutableMap.of(ATTR_0,
                "badValue")));

        Thread.sleep(1100);
        retryTaskProcessor.processPendingTasksWithReset(queue);

        Entity current = txService.doInNewTx(() -> dbService.get(result.getGid()));
        Assertions.assertNull(
                current.getAttribute(ATTR_1), "Триггер не должен выполнится т.к. условие не сработало");
    }

    /**
     * Проверяем, что проверка обязательности заполненности атрибутов не осуществляется для вложенных процессов.
     * Это позволяет разными перациями в триггерах заполнить все обязательные атрибуты, и не требовать
     * заполнять обязательные атрибуты в одной операции.
     * <p>На создание объекта есть триггер, которые первой операцией заполняет первый атрибут, а второй операцией
     * второй атрибут.</p>
     */
    @Test
    @Transactional
    public void nestedEdit() {
        Entity result = bcpService.create(FQN_WITH_REQUIRED, Maps.of());

        Assertions.assertEquals("value1", result.getAttribute(ATTR_1),
                "Должны заполнить триггером на создание объекта");
        Assertions.assertEquals("value2", result.getAttribute(ATTR_2),
                "Должны заполнить триггером на создание объекта");
    }

    @Test
    @Transactional
    public void resetRequiredAttribute() {
        Assertions.assertThrows(ValidationException.class, () -> {
            Entity result = bcpService.create(FQN_WITH_REQUIRED, Maps.of());
            bcpService.edit(result, Maps.of(ATTR_1, null));
        });
    }

    private Entity create(String attr0Value) {
        Map<String, Object> properties = properties(attr0Value);
        return bcpService.create(FQN_1, properties);
    }

    private Entity create() {
        return create(UUID.randomUUID().toString());
    }

    private Map<String, Object> properties(String attr0Value) {
        return ImmutableMap.of(ATTR_0, attr0Value);
    }

}
