package ru.yandex.market.gutgin.tms.pipeline.good.taskaction;

import ru.yandex.market.gutgin.tms.engine.problem.ProblemInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author danfertev
 * @since 29.05.2019
 */
public class ProblemInfoAssert {
    public static void assertProblemInfoDescription(ProblemInfo actual, ProblemInfo expected) {
        assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    }

    public static void assertProblemInfoThrowable(ProblemInfo actual, ProblemInfo expected) {
        assertThat(actual.getThrowable()).isEqualTo(expected.getThrowable());
    }
}
