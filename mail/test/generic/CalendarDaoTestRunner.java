package ru.yandex.calendar.test.generic;

import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.calendar.util.rr.CalendarRandomValueGenerator;
import ru.yandex.commune.random.RandomValueGenerator;
import ru.yandex.commune.test.random.RunWithRandomMethod;
import ru.yandex.commune.test.random.RunWithRandomTest;
import ru.yandex.commune.test.random.RunWithRandomTestRunner;

/**
 * @author Stepan Koltsov
 *
 * @see SpringJUnit4ClassRunner
 * @see RunWithRandomTest
 */
public class CalendarDaoTestRunner extends RunWithRandomTestRunner {


    public CalendarDaoTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    protected CalendarTestContextManager createTestContextManager(Class<?> clazz) {
        return new CalendarTestContextManager(clazz);
    }

    @Override
    protected void runChild(RunWithRandomMethod child, RunNotifier notifier) {
        EachTestNotifier eachNotifier = new EachTestNotifier(notifier, describeChild(child));

        eachNotifier.fireTestStarted();
        try {
            methodBlock(child).evaluate();
        } catch (AssumptionViolatedException e) {
            eachNotifier.addFailedAssumption(e);
        } catch (DataIntegrityViolationException e) {
            if (!e.getMessage().contains("violates foreign key constraint")) {
                eachNotifier.addFailure(e);
            }
        } catch (Throwable e) {
            eachNotifier.addFailure(e);
        } finally {
            eachNotifier.fireTestFinished();
        }
    }

    @Override
    protected RandomValueGenerator getRandomValueGenerator() {
        return CalendarRandomValueGenerator.R;
    }

} //~
