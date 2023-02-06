package ru.yandex.market.tsum.pipe.engine.definition;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.tsum.pipe.engine.definition.context.impl.LaunchJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.variables.JobVariablesProvider;
import ru.yandex.market.tsum.pipe.engine.definition.variables.StringVariableIsTooLongException;
import ru.yandex.market.tsum.pipe.engine.definition.variables.TooMuchVariablesException;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobVariablesChangedEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 05.07.18
 */
public class JobVariablesProviderTest {
    private JobLaunch jobLaunch;
    private JobVariablesProvider jobVariablesProvider;

    @Before
    public void setUp() {
        String jobId = "jobId";

        jobLaunch = new JobLaunch(1, "user42", Collections.emptyList(), Collections.emptyList());

        JobState jobState = Mockito.mock(JobState.class);
        Mockito.when(jobState.getLastLaunch()).thenReturn(jobLaunch);

        PipeLaunch pipeLaunch = Mockito.mock(PipeLaunch.class);
        String pipeLaunchId = "pipeLaunchId";
        Mockito.when(pipeLaunch.getIdString()).thenReturn(pipeLaunchId);
        Mockito.when(pipeLaunch.getJobState(jobId)).thenReturn(jobState);

        PipeStateService pipeStateService = Mockito.mock(PipeStateService.class);
        Mockito.when(pipeStateService.recalc(Mockito.eq(pipeLaunchId), Mockito.any()))
            .then(
                (Answer<PipeLaunch>) invocation -> {
                    JobVariablesChangedEvent event = invocation.getArgument(1);
                    jobLaunch.getVariableMap().put(event.getVariableKey(), event.getVariableValue());

                    return pipeLaunch;
                }
            );

        LaunchJobContext jobContext = Mockito.mock(LaunchJobContext.class);
        Mockito.when(jobContext.getJobId()).thenReturn(jobId);
        Mockito.when(jobContext.getJobState()).thenReturn(jobState);
        Mockito.when(jobContext.getPipeLaunch()).thenReturn(pipeLaunch);
        Mockito.doCallRealMethod().when(jobContext).updatePipeLaunch(Mockito.any());

        jobVariablesProvider = new JobVariablesProvider(jobContext, pipeStateService);
    }

    @Test
    public void noTooMuchVariablesOnUpdate() {
        createFullJobVariables(jobVariablesProvider, "var");
        jobVariablesProvider.saveValue("var_1", "updating");
    }

    @Test(expected = TooMuchVariablesException.class)
    public void tooMuchVariablesTest() {
        createFullJobVariables(jobVariablesProvider, "var");
        jobVariablesProvider.saveValue("var_11", true);
    }

    @Test(expected = StringVariableIsTooLongException.class)
    public void saveStringValue() {
        jobVariablesProvider.saveValue("tooLongString", StringUtils.repeat("x", 1001));
    }

    private void createFullJobVariables(JobVariablesProvider jobVariablesProvider, String prefix) {
        jobVariablesProvider.saveValue(prefix + "_1", true);
        jobVariablesProvider.saveValue(prefix + "_2", 1);
        jobVariablesProvider.saveValue(prefix + "_3", 1L);
        jobVariablesProvider.saveValue(prefix + "_4", 1.1);
        jobVariablesProvider.saveValue(prefix + "_5", "string");
        jobVariablesProvider.saveValue(prefix + "_6", true);
        jobVariablesProvider.saveValue(prefix + "_7", 1);
        jobVariablesProvider.saveValue(prefix + "_8", 1L);
        jobVariablesProvider.saveValue(prefix + "_9", 1.1);
        jobVariablesProvider.saveValue(prefix + "_10", "string");
    }
}