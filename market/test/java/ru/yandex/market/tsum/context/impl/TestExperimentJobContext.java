package ru.yandex.market.tsum.context.impl;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.tsum.context.ExperimentJobContext;

@ParametersAreNonnullByDefault
public class TestExperimentJobContext implements ExperimentJobContext {
    @Override
    public boolean isUnderExperiment(String experimentId, Object key) {
        return false;
    }

    @Override
    public boolean isPipeLaunchUnderExperiment(String experimentId) {
        return false;
    }
}
