package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class WriterUtils {
    private WriterUtils() {
    }

    static void checkArgument(Strategy strategy, Class<? extends Strategy> supportedType) {
        checkNotNull(strategy, "strategy to write is null");
        checkState(supportedType.isAssignableFrom(strategy.getClass()),
                "unexpected strategy type " + strategy.getClass().getCanonicalName());
    }
}
