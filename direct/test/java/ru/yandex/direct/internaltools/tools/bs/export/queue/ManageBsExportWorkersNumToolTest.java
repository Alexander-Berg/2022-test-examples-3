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
public class ManageBsExportWorkersNumToolTest {
    @Autowired
    private ManageBsExportStdWorkersNumTool stdWorkersNumTool;

    @Autowired
    private ManageBsExportFullLbExportWorkersNumTool fullLbWorkersNumTool;

    @Autowired
    private BsExportParametersService service;

    private ManageBsExportWorkersNumParameters request;

    private int testShard;
    private int oldWorkersNum;

    private boolean calcCondition;

    @Before
    public void before() {
        testShard = 2;
        calcCondition = service.isManualMode();
        service.enableManualControllingMode();

        request = new ManageBsExportWorkersNumParameters();
        request.setAllShards(false);
        request.setShard(testShard);
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
        oldWorkersNum = service.getWorkersNum(WorkerType.STD, testShard);
        Long overMaxNum = (long) (stdWorkersNumTool.workersMaxNum + 1);

        service.enableManualControllingMode();

        request.setWorkersNum(overMaxNum);

        ValidationResult<ManageBsExportWorkersNumParameters, Defect> validation = stdWorkersNumTool.validate(request);

        assertThat(validation, hasDefectWithDefinition(validationError(path(field(ShardAndWorkersNumInfo.WORKERS_NUM_INFO)),
                NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE)));
    }

    @Test
    public void checkWorkersNumExactlyMax() {
        oldWorkersNum = service.getWorkersNum(WorkerType.STD, testShard);
        Long overMaxNum = (long) (stdWorkersNumTool.workersMaxNum);

        service.enableManualControllingMode();

        request.setWorkersNum(overMaxNum);

        ValidationResult<ManageBsExportWorkersNumParameters, Defect> validation = stdWorkersNumTool.validate(request);

        assertThat("Максимум принимается без ошибок валидации",
                validation, hasNoDefectsDefinitions());
    }

    @Test
    public void checkManualControlDisabled() {
        oldWorkersNum = service.getWorkersNum(WorkerType.STD, testShard);
        Long newWorkersNum = 2L;

        service.disableManualControllingMode();

        request.setWorkersNum(newWorkersNum);


        ValidationResult<ManageBsExportWorkersNumParameters, Defect> validation = stdWorkersNumTool.validate(request);

        assertThat("Включен автоматический расчёт", validation,
                hasDefectWithDefinition(validationError(MANUAL_CONTROL_OF_WORKERS_NUM_IS_NOT_ENABLED)));
    }

    @Test
    public void checkWrongWorkersNum() {
        oldWorkersNum = service.getWorkersNum(WorkerType.STD, testShard);
        Long newWorkersNum = (oldWorkersNum != 2) ? 2L : 7L;

        service.enableManualControllingMode();

        request.setWorkersNum(newWorkersNum);

        stdWorkersNumTool.getMassData(request);

        assertThat("Установлено новое значение количества воркеров",
                newWorkersNum == service.getWorkersNum(WorkerType.STD, testShard));
    }

    @Test
    public void changeFullLBExportWorkersNumInAutoMode() {
        oldWorkersNum = service.getWorkersNum(WorkerType.FULL_LB_EXPORT, testShard);
        Long newWorkersNum = (oldWorkersNum != 1) ? 1L : 2L;

        service.disableManualControllingMode();

        request.setWorkersNum(newWorkersNum);

        ValidationResult<ManageBsExportWorkersNumParameters, Defect> validationResult = fullLbWorkersNumTool.validate(request);
        assertThat("Валидация успешна", validationResult.getErrors().isEmpty());

        fullLbWorkersNumTool.getMassData(request);

        assertThat("Установлено новое значение количества воркеров",
                newWorkersNum == service.getWorkersNum(WorkerType.FULL_LB_EXPORT, testShard));
    }
}
