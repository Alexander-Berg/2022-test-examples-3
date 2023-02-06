package ru.yandex.autotests.innerpochta.util;

import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.model.StepResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.Objects;

public class AllureStepLogger implements StepLifecycleListener {

    private final Logger logger = Logger.getLogger(AllureStepLogger.class);

    private static final int PADDING_MULTIPLIER = 3; //Inherited from old logger. As for me, looks better than 2 or 4.
    private ThreadLocal<Counter> position = ThreadLocal.withInitial(() -> (new Counter()));

    @Override
    public void beforeStepStart(StepResult result) {
        String eventName = StringUtils.defaultIfBlank(result.getDescription(), result.getName());
        logger.info(getPadding() + " [ -> ] " + eventName);
        position.get().increment();
    }

    @Override
    public void afterStepStop(StepResult event) {
        position.get().decrement();
        logger.info(getPadding() + " [ <- ] Step Finished!");
    }

    private String getPadding() {
        return StringUtils.repeat(' ', (position.get().value >= 0 ? position.get().value : 0)  * PADDING_MULTIPLIER );
    }

    private static class Counter {
        private int value = 0;

        void increment() {
            value++;
        }

        void decrement() {
            value--;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Counter counter = (Counter) o;
            return value == counter.value;
        }

        @Override
        public int hashCode() {

            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "Counter{" +
                "value=" + value +
                '}';
        }

    }
}
