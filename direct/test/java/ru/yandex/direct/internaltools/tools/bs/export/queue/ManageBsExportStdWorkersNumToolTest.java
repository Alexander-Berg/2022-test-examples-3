package ru.yandex.direct.internaltools.tools.bs.export.queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bs.export.BsExportParametersService;
import ru.yandex.direct.core.entity.bs.export.model.WorkerType;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.bs.export.queue.container.ShardAndWorkersNumInfo;
import ru.yandex.direct.internaltools.tools.bs.export.queue.model.ManageBsExportWorkersNumParameters;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.internaltools.tools.bs.export.queue.validation.BsExportQueueDefectIds.MANUAL_CONTROL_OF_WORKERS_NUM_IS_NOT_ENABLED;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ManageBsExportStdWorkersNumToolTest {
    @Autowired
    private ManageBsExportStdWorkersNumTool tool;
    @Autowired
    private BsExportParametersService service;

    private ManageBsExportWorkersNumParameters request;

    private int testShard;
    private int oldWorkersNum;

    private boolean calcCondition;

    @Before
    public void before() {
        calcCondition = service.isManualMode();
        service.enableManualControllingMode();
        request = new ManageBsExportWorkersNumParameters();

        testShard = 2;
        oldWorkersNum = service.getWorkersNum(WorkerType.STD, testShard);
    }

    @After
    public void after() {
        service.enableManualControllingMode();
        service.setWorkersNumInShard(WorkerType.STD, testShard, oldWorkersNum);
        if (!calcCondition) {
            service.disableManualControllingMode();
        }
    }

    @Test
    public void checkWorkersNumMoreThanMax() {
        Long overMaxNum = (long) (tool.workersMaxNum + 1);

        service.enableManualControllingMode();

        request.setAllShards(false);

        request.setShard(testShard);
        request.setWorkersNum(overMaxNum);

        ValidationResult<ManageBsExportWorkersNumParameters, Defect> validation = tool.validate(request);

        assertThat(validation, hasDefectWithDefinition(validationError(path(field(ShardAndWorkersNumInfo.WORKERS_NUM_INFO)),
                NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE)));
    }

    @Test
    public void checkWorkersNumExactlyMax() {
        Long overMaxNum = (long) (tool.workersMaxNum);

        service.enableManualControllingMode();

        request.setAllShards(false);

        request.setShard(testShard);
        request.setWorkersNum(overMaxNum);

        ValidationResult<ManageBsExportWorkersNumParameters, Defect> validation = tool.validate(request);

        assertThat("Максимум принимается без ошибок валидации",
                validation, hasNoDefectsDefinitions());
    }

    @Test
    public void checkManualControlDisabled() {
        Long newWorkersNum = 2L;

        service.disableManualControllingMode();

        request.setAllShards(false);
        request.setShard(testShard);
        request.setWorkersNum(newWorkersNum);


        ValidationResult<ManageBsExportWorkersNumParameters, Defect> validation = tool.validate(request);

        assertThat("Включен автоматический расчёт", validation,
                hasDefectWithDefinition(validationError(MANUAL_CONTROL_OF_WORKERS_NUM_IS_NOT_ENABLED)));
    }

    @Test
    public void checkWrongWorkersNum() {
        Long newWorkersNum = (oldWorkersNum != 2) ? 2L : 7L;

        service.enableManualControllingMode();

        request.setAllShards(false);
        request.setShard(testShard);
        request.setWorkersNum(newWorkersNum);

        tool.getMassData(request);

        assertThat("Установлено новое значение количества воркеров",
                newWorkersNum == service.getWorkersNum(WorkerType.STD, testShard));
    }

}
