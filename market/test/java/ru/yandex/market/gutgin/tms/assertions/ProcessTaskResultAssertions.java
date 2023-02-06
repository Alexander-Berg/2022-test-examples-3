package ru.yandex.market.gutgin.tms.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import ru.yandex.market.gutgin.tms.engine.problem.ProblemInfo;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;

import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class ProcessTaskResultAssertions extends AbstractObjectAssert<ProcessTaskResultAssertions, ProcessTaskResult> {
    public ProcessTaskResultAssertions(ProcessTaskResult processTaskResult) {
        super(processTaskResult, ProcessTaskResultAssertions.class);
    }

    public ProcessTaskResultAssertions hasProblems() {
        super.isNotNull();
        if (!actual.hasProblems()) {
            failWithMessage("Expected process task:\n%s to contain problems, but problem list is empty",
                actual
            );
        }
        return myself;
    }

    public ProcessTaskResultAssertions doesntHaveProblems() {
        super.isNotNull();
        if (actual.hasProblems()) {
            failWithMessage("Expected process task:\n%s not to contain problems.Actual problems are:\n%s",
                actual, actual.getProblems().stream().map(s -> s.toString()).collect(Collectors.joining("\n"))
            );
        }
        return myself;
    }

    public ListAssert<ProblemInfo> getProblems() {
        super.isNotNull();
        return Assertions.assertThat(actual.getProblems());
    }

    public Object getResult() {
        return Assertions.assertThat(actual.getResult());
    }
}
