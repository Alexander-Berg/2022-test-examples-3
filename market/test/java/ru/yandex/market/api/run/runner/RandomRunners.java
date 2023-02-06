package ru.yandex.market.api.run.runner;

import org.junit.runner.Runner;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Enum содержащий запускалки тестов в произвольном порядке
 * @author dimkarp93
 */
public enum RandomRunners {
    DEFAULT(null, RandomJUnit4ClassRunner.class),
    SPRING_JUNIT(SpringJUnit4ClassRunner.class, RandomSpringJUnit4ClassRunner.class),
    MOCK_JUNIT(MockitoJUnitRunner.class, RandomMockitoJUnitRunner.class);

    private Class<?> annotateRunner;
    private Class<? extends Runner> wrapperRunner;

    RandomRunners(Class<?> annotateRunner, Class<? extends Runner> wrapperRunner) {
        if (!(RandomRunner.class.isAssignableFrom(wrapperRunner))) {
            throw new IllegalArgumentException("Try added not RandomRunner class in RandomRunners enum");
        }
        this.annotateRunner = annotateRunner;
        this.wrapperRunner = wrapperRunner;
    }

    public Class<?> getAnnotateRunner() {
        return annotateRunner;
    }

    public Class<? extends Runner> getWrapperRunner() {
        return wrapperRunner;
    }

    public static RandomRunners valueOf(Class<?> clazz) {
        if (null == clazz) {
            return DEFAULT;
        }
        for (RandomRunners runner: values()) {
            if (null != runner.annotateRunner && runner.annotateRunner.isAssignableFrom(clazz)) {
                return runner;
            }
        }
        throw new IllegalArgumentException(
                "No enum constant " + clazz.getCanonicalName());
    }

}
