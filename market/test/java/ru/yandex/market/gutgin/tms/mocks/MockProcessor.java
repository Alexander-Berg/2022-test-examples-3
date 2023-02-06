package ru.yandex.market.gutgin.tms.mocks;

import ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.StepProcessor;
import ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Data;
import ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Template;
import ru.yandex.market.gutgin.tms.engine.problem.ProblemInfo;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.partner.content.common.engine.parameter.Param;

import java.util.function.Function;
import java.util.stream.Collectors;

public class MockProcessor<C_I extends Param, C_O extends Param>
    extends StepProcessor<C_I, C_O, Template<C_I, C_O>, Data<C_I, C_O>> {

    @Override
    public Data<C_I, C_O> internalProcess(long pipelineId,
                                          C_I inData,
                                          Template<C_I, C_O> step,
                                          Data<C_I, C_O> stepData) {

        ProcessTaskResult<C_O> outData = step.getFunction().apply(inData);

        if (stepData == null) {
            stepData = new Data<>();
        }

        if (!outData.hasResult()) {
            throw new IllegalStateException(
                outData.getProblems().stream()
                    .map(ProblemInfo::getDescription)
                    .collect(Collectors.joining())
            );
        }

        stepData.setOut(outData.getResult());
        stepData.setFinished(true);
        stepData.addCheckTime(System.currentTimeMillis());
        return stepData;
    }

    @Override
    public Function<? extends Param, ? extends ProcessTaskResult<? extends Param>> innerFindTaskFunction(
        long taskId, Template<C_I, C_O> step, Data<C_I, C_O> stepData
    ) {
        throw new UnsupportedOperationException();
    }
}
