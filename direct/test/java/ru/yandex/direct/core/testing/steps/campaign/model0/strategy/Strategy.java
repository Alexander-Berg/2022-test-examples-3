package ru.yandex.direct.core.testing.steps.campaign.model0.strategy;

public interface Strategy {

    default <T extends Strategy> T cast(Class<T> strategyType) {
        if (!strategyType.isAssignableFrom(this.getClass())) {
            throw new IllegalStateException(String.format("try to cast %s to %s", this.getClass(), strategyType));
        }
        @SuppressWarnings("unchecked")
        T ret = (T) this;
        return ret;
    }

    boolean isAutobudget();

    default boolean isManual() {
        return !isAutobudget();
    }
}
