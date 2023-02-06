package ru.yandex.market.checker.check;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.TestHelper;
import ru.yandex.market.checker.EmptyTest;
import ru.yandex.market.checker.check.model.Checker;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author komarovns
 * @date 08.02.19
 */
public abstract class EscapedSymbolsCheckerTest extends EmptyTest {
    @Test
    public void testEscapedSymbols() {
        byte[] contest = "10&#8239;772".getBytes();
        var taskBody = "10772";
        var task = TestHelper.createTask("yandex.ru");
        assertTrue(checker().doCheck(task, contest, null, taskBody).isResult());
    }

    abstract Checker checker();
}
