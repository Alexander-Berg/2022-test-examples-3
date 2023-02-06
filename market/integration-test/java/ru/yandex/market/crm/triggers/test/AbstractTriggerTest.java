package ru.yandex.market.crm.triggers.test;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.Execution;
import org.junit.After;

import ru.yandex.market.crm.triggers.test.helpers.BpmProcessSpy;

/**
 * Базовый класс для интеграционных тестов запускающих триггеры
 * После каждого теста уничтожает активные процессы
 */
public abstract class AbstractTriggerTest extends AbstractServiceTest {

    @Inject
    protected ProcessEngine processEngine;
    @Inject
    protected BpmProcessSpy processSpy;

    @After
    public void triggersTearDown() {
        List<String> runningPids =  processEngine.getRuntimeService()
                .createProcessInstanceQuery()
                .list().stream()
                .map(Execution::getId)
                .collect(Collectors.toList());

        processEngine.getRuntimeService().deleteProcessInstances(
                runningPids,
                "After test cleanup",
                false,
                true
        );
    }
}
