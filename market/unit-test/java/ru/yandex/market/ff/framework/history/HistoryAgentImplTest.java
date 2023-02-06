package ru.yandex.market.ff.framework.history;

import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class HistoryAgentImplTest {

    HistoryAgentImpl historyAgent = new HistoryAgentImpl(mock(HistoryAgencyDispatcher.class), mock(Supplier.class));

    @Test
    void spyCommitRunsItsGivenCodeBlock() {

        Assertions.assertEquals("Hello World!", historyAgent.spyCommit(
                null,
                () -> "Hello World!",
                null, null));
    }

    @Test
    void spyCreationRunsItsGivenCodeBlock() {

        Assertions.assertEquals("Hello World!", historyAgent.spyCreation(
                null,
                () -> "Hello World!",
                null));
    }

    @Test
    void spyHandlerRunsItsGivenCodeBlock() {
        Assertions.assertEquals("Hello World!", historyAgent.spyHandler(
                null,
                null,
                () -> "Hello World!",
                null));
    }

    @Test
    void spyMultiHandlerRunsItsGivenCodeBlock() {
        Runnable myCode = mock(Runnable.class);
        Assertions.assertEquals("Hello World!", historyAgent.spyMultiHandler(
                null,
                null,
                () -> {
                    myCode.run();
                    return "Hello World!";
                },
                null));
        verify(myCode, times(1)).run();
    }

    @Test
    void spyConditionRunsItsGivenCodeBlock() {
        Assertions.assertEquals("Hello World!", historyAgent.spyCondition(
                null,
                null,
                () -> "Hello World!"));
    }

    @Test
    void spyActionRunsItsGivenCodeBlock() {
        Assertions.assertEquals("Hello World!", historyAgent.spyAction(
                null,
                () -> "Hello World!",
                null));
    }
}
