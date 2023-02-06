package ru.yandex.market.api.run.runner;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static ru.yandex.market.api.run.ApiRandomTestRunListener.API_RANDOM_TEST_RUN_LISTENER;

/**
 * @author dimkarp93
 */
public class RandomMockitoJUnitRunner extends BlockJUnit4ClassRunner implements RandomRunner {
    private final Random random;

    public RandomMockitoJUnitRunner(Random random, Class<?> testClass) throws Exception {
        super(testClass);
        this.random = random;
    }

    @Override
    protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
        MockitoAnnotations.initMocks(target);
        return super.withBefores(method, target, statement);
    }

    @Override
    protected List<FrameworkMethod> getChildren() {
        List<FrameworkMethod> methods = Arrays.asList(super.getChildren().toArray(new FrameworkMethod[0]));
        Collections.shuffle(methods, random);
        return methods;
    }

    @Override
    public void run(RunNotifier notifier) {
        if (!API_RANDOM_TEST_RUN_LISTENER.isUse()) {
            notifier.addListener(API_RANDOM_TEST_RUN_LISTENER);
            API_RANDOM_TEST_RUN_LISTENER.use();
        }
        super.run(notifier);
    }
}
