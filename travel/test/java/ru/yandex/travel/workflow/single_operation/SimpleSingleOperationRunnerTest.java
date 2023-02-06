package ru.yandex.travel.workflow.single_operation;

import com.google.common.base.Preconditions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleSingleOperationRunnerTest {
    @Test
    public void testRunnerWithResult() {
        RunnerWithResult runner = new RunnerWithResult();
        assertThat(runner.getInputClass()).isEqualTo(String.class);
        assertThat(runner.runOperation("9")).isEqualTo(9);
    }

    @Test
    public void testVoidRunner() {
        VoidRunner runner = new VoidRunner();
        assertThat(runner.getInputClass()).isEqualTo(String.class);
        //noinspection ConstantConditions
        assertThat(runner.runOperation("9")).isNull();
    }

    private static class RunnerWithResult extends SimpleSingleOperationRunner<String, Number> {
        @Override
        public Number runOperation(String params) {
            return Integer.valueOf(params);
        }
    }

    private static class VoidRunner extends SimpleSingleOperationRunner.Void<String> {
        @Override
        protected void runVoidOperation(String params) {
            Preconditions.checkNotNull(params);
        }
    }
}
