package ru.yandex.chemodan.util.test;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import ru.yandex.devtools.test.YaTest;

/**
 * @author tolmalev
 */
public class SkipInYaTestRule implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {
        if (YaTest.insideYaTest) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    throw new AssumptionViolatedException("Tests are skipped in ya make");
                }
            };
        } else {
            return base;
        }
    }
}
