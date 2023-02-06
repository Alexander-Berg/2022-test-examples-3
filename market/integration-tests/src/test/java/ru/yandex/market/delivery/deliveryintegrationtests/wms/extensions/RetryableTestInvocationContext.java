package ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions;

import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

public class RetryableTestInvocationContext implements TestTemplateInvocationContext {

    private final int currentRepetition;
    private final int totalTestRuns;
    private final String displayName;

    public RetryableTestInvocationContext(int currentRepetition, int totalRepetitions,
                                          String displayName) {
        this.currentRepetition = currentRepetition;
        this.totalTestRuns = totalRepetitions;
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
        //Первый запуск ещё не ретрай
        if (invocationIndex == 1) {
            return displayName;
        }
        return displayName + " (Retry " + (currentRepetition - 1) + " of " + (totalTestRuns - 1) + ")";
    }
}
