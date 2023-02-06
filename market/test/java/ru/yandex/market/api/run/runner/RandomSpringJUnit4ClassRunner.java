package ru.yandex.market.api.run.runner;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static ru.yandex.market.api.run.ApiRandomTestRunListener.API_RANDOM_TEST_RUN_LISTENER;

/**
 * @author dimkarp93
 */
public class RandomSpringJUnit4ClassRunner extends SpringJUnit4ClassRunner implements RandomRunner {

    private final Random random;

    public RandomSpringJUnit4ClassRunner(Random random, Class<?> testClass) throws Exception {
        super(testClass);
        this.random = random;
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
