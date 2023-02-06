package ru.yandex.market.mbi.bpmn.function;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.bpmn.FunctionalTest;

/**
 * Тесты для {@link DelegateExecutionUtils}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DelegateExecutionUtilsTest extends FunctionalTest {

    @Autowired
    private DelegateExecutionUtils delegateExecutionUtils;

    @Test
    @DisplayName("Получение рутовой ноды. Передаем рут")
    void testRoot() {
        ExecutionImpl root = new ExecutionImpl();
        DelegateExecution actual = delegateExecutionUtils.getRootDelegateExecution(root);

        Assertions.assertEquals(root, actual);
    }

    @Test
    @DisplayName("Получение рутовой ноды. Передаем дочернюю")
    void testChild() {
        ExecutionImpl root = new ExecutionImpl();
        ExecutionImpl child1 = new ExecutionImpl();
        ExecutionImpl child2 = new ExecutionImpl();

        child2.setSuperExecution(child1);
        child1.setSuperExecution(root);

        DelegateExecution actual = delegateExecutionUtils.getRootDelegateExecution(child2);

        Assertions.assertEquals(root, actual);
    }
}
