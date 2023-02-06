package ru.yandex.market.tsum.pipe.engine.runtime.state.calculator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import ru.yandex.market.tsum.pipe.engine.runtime.PipeProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobScheduler;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobWaitingScheduler;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PrepareLaunchParameters;
import ru.yandex.market.tsum.pipe.engine.runtime.state.StageGroupDao;

/**
 * Базовый класс для тестов пайплайна
 *
 * @author Nikolay Firov
 * @date 15.12.2017
 */
public abstract class PipeStateCalculatorTestBase {
    public static final String USERNAME = "user42";

    @Autowired
    protected PipeProvider pipeProvider;

    @Autowired
    protected PipeStateCalculator pipeStateCalculator;

    @Autowired
    protected PipeLaunchFactory pipeLaunchFactory;

    @Autowired
    protected GenericApplicationContext applicationContext;

    @Autowired
    protected PipeLaunchDao pipeLaunchDao;

    @Autowired
    protected TestJobScheduler testJobScheduler;

    @Autowired
    protected TestJobWaitingScheduler testJobWaitingScheduler;

    @Autowired
    protected PipeStateService pipeStateService;

    @Autowired
    protected PipeTester pipeTester;

    @Autowired
    protected StageGroupDao stageService;

    protected String activateLaunch(String pipeId) {
        return this.activateLaunch(pipeId, null);
    }

    protected String activateLaunch(String pipeId, String stageGroupId) {
        return pipeStateService
            .activateLaunch(
                PrepareLaunchParameters.builder()
                    .withPipeId(pipeId)
                    .withStageGroupId(stageGroupId)
                    .withTriggeredBy(USERNAME)
                    .withProjectId("prj")
                    .build()
            ).getId().toString();
    }
}
