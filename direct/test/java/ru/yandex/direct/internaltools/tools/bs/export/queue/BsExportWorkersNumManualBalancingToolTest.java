package ru.yandex.direct.internaltools.tools.bs.export.queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.bs.export.BsExportParametersService;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.container.InternalToolResult;
import ru.yandex.direct.internaltools.tools.bs.export.queue.model.BsExportBalancerMode;
import ru.yandex.direct.internaltools.tools.bs.export.queue.model.BsExportWorkersNumManualBalancingParameter;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.common.db.PpcPropertyNames.BSEXPORT_WORKERS_NUM_CONTROLLED_MANUALLY;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BsExportWorkersNumManualBalancingToolTest {
    private static final String MANUAL_CONTROLLING_WORKERS_NUM_PROP_NAME = "bsexport_workers_num_controlled_manually";
    private static final Boolean MANUAL_CONTROL = Boolean.TRUE;
    private static final Boolean AUTO_CONTROL = Boolean.FALSE;

    private BsExportWorkersNumManualBalancingTool tool;
    private BsExportWorkersNumManualBalancingParameter request;
    private String beforeValueInDB;
    private Boolean currentValueInDB;
    private PpcProperty<Boolean> currentValueProperty;

    @Autowired
    BsExportParametersService service;

    @Autowired
    PpcPropertiesSupport support;

    @Before
    public void before() {
        tool = new BsExportWorkersNumManualBalancingTool(service);
        request = new BsExportWorkersNumManualBalancingParameter();
        beforeValueInDB = support.get(MANUAL_CONTROLLING_WORKERS_NUM_PROP_NAME);
        currentValueProperty = support.get(BSEXPORT_WORKERS_NUM_CONTROLLED_MANUALLY);
    }

    @After
    public void after() {
        support.set(MANUAL_CONTROLLING_WORKERS_NUM_PROP_NAME, beforeValueInDB);
    }

    @Test
    public void checkGetMode() {
        Boolean modeInDB = currentValueProperty.getOrDefault(false);

        assertThat("Значение, полученное из метода, совпадает со значением в базе",
                modeInDB == service.isManualMode());
    }

    @Test
    public void checkProcessResultManual() {
        request.setMode(BsExportBalancerMode.MANUAL);
        InternalToolResult expectedResult = new InternalToolResult()
                .withMessage(BsExportWorkersNumManualBalancingTool.MANUAL_CONTROL_MESSAGE);
        assertThat(BsExportWorkersNumManualBalancingTool.MANUAL_CONTROL_MESSAGE, tool.process(request), beanDiffer(expectedResult));

        currentValueInDB = currentValueProperty.getOrDefault(false);
        assertThat("В базе проперти переключена в режим ручного управления", currentValueInDB.equals(MANUAL_CONTROL));
    }

    @Test
    public void checkProcessResultAuto() {
        request.setMode(BsExportBalancerMode.AUTO);
        InternalToolResult expectedResult = new InternalToolResult()
                .withMessage(BsExportWorkersNumManualBalancingTool.AUTO_CONTROL_MESSAGE);
        assertThat(BsExportWorkersNumManualBalancingTool.AUTO_CONTROL_MESSAGE, tool.process(request), beanDiffer(expectedResult));

        currentValueInDB = currentValueProperty.getOrDefault(false);
        assertThat("В базе проперти переключена в режим автоматического управления", currentValueInDB.equals(AUTO_CONTROL));
    }
}
