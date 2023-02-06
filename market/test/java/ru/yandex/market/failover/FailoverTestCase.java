package ru.yandex.market.failover;

public class FailoverTestCase {

    public static FailoverState initFailoverState() throws NoSuchFieldException, IllegalAccessException {
        FailoverState failoverState = new FailoverState();
        FailoverTestUtils.setPrivate(failoverState, "contextRunningFlagTimeout", 120_000L);
        FailoverTestUtils.setPrivate(failoverState, "httpRunningFlagTimeout", 10_000L);
        FailoverTestUtils.setPrivate(failoverState, "leaderFlagTimeout", 10_000L);
        FailoverTestUtils.setPrivate(failoverState, "startupTimeMonitoringLimit", 1_200_000L);
        failoverState.afterPropertiesSet();

        return failoverState;
    }
}
