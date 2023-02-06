package ru.yandex.market.tsum.pipe.engine.runtime.state.calculator;

import org.junit.Before;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRefContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.BeanRegistrar;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchParameters;
import ru.yandex.market.tsum.pipe.engine.runtime.state.calculator.commands.ScheduleCommand;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunchRefImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Абстрактный класс для тестирования модуля пересчёта пайплайна.
 *
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 24.03.17
 */
public abstract class PipeStateCalculatorTest extends PipeStateCalculatorTestBase {
    private PipeLaunch pipeLaunch;
    private List<ScheduleCommand> triggeredJobs;

    /**
     * Этот метод нужно перегрузить, если пайплайн задан в виде бина.
     *
     * @return
     */
    protected String getPipeId() {
        return null;
    }

    /**
     * Этот метод нужно перегрузить, если пайплайн задаётся инлайн с помощью {@link PipelineBuilder}.
     * В этом случае пайплайн будет зарегиситрирован в рантайме.
     *
     * @return
     */
    protected Pipeline getPipeline() {
        return pipeProvider.get(getPipeId());
    }

    void recalc(JobEvent event) {
        triggeredJobs.addAll(
            pipeStateCalculator.recalc(pipeLaunch, event).stream()
                .filter(ScheduleCommand.class::isInstance)
                .map(ScheduleCommand.class::cast)
                .collect(Collectors.toList())
        );
    }

    List<ScheduleCommand> getTriggeredJobs() {
        return triggeredJobs;
    }

    PipeLaunch getPipeLaunch() {
        return pipeLaunch;
    }

    @Before
    public void setUp() throws Exception {
        Pipeline pipeline = getPipeline();
        String pipeId;

        if (getPipeId() == null) {
            pipeId = BeanRegistrar.registerNamedBean(pipeline, applicationContext);
        } else {
            pipeId = getPipeId();
        }

        pipeLaunch = pipeLaunchFactory.create(
            PipeLaunchParameters.builder()
                .withLaunchRef(PipeLaunchRefImpl.create(pipeId))
                .withManualResources(ResourceRefContainer.empty())
                .withTriggeredBy("user42")
                .withProjectId("prj")
                .build()
        );
        triggeredJobs = new ArrayList<>();
    }
}
