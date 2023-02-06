package ru.yandex.direct.internaltools.tools.ppcproperties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.container.InternalToolResult;
import ru.yandex.direct.internaltools.tools.ppcproperties.container.PreprodUsagePctParameter;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.EXPORT_PREPROD_USAGE_PCT_PROP_NAME;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;


@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ManageBsPreprodStatUsagePctToolTest {

    @Autowired
    private ManageBsPreprodStatUsagePctTool manageBsPreprodStatUsagePctTool;
    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;

    private PpcProperty<Integer> property;

    @Before
    public void before() {
        this.property = ppcPropertiesSupport.get(EXPORT_PREPROD_USAGE_PCT_PROP_NAME);
    }

    /**
     * Если будет введено минусовое значение -> валидация не пройдет с ошибкой MUST_BE_IN_THE_INTERVAL_INCLUSIVE
     */
    @Test
    public void checkWhenMinusValue() {
        PreprodUsagePctParameter params = new PreprodUsagePctParameter()
                .withPreprodUsagePct(-1L);
        ValidationResult<PreprodUsagePctParameter, Defect> vr = manageBsPreprodStatUsagePctTool.validate(params);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(PreprodUsagePctParameter.PREPROD_USAGE_PCT)),
                MUST_BE_IN_THE_INTERVAL_INCLUSIVE)));
    }

    /**
     * Если будет введено значение, превышающее предел (>100) -> валидация не пройдет с ошибкой
     * MUST_BE_IN_THE_INTERVAL_INCLUSIVE
     */
    @Test
    public void checkWhenPlusOverlimitValue() {
        PreprodUsagePctParameter params = new PreprodUsagePctParameter()
                .withPreprodUsagePct(101L);
        ValidationResult<PreprodUsagePctParameter, Defect> vr = manageBsPreprodStatUsagePctTool.validate(params);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(PreprodUsagePctParameter.PREPROD_USAGE_PCT)),
                MUST_BE_IN_THE_INTERVAL_INCLUSIVE)));
    }

    /**
     * Если будет введено нижнее граничное значение (0) -> валидация пройдет и будет возвращено и сохранено в БД
     * данное значение
     */
    @Test
    public void checkLowerLimitValue() {
        PreprodUsagePctParameter params = new PreprodUsagePctParameter()
                .withPreprodUsagePct(0L);
        ValidationResult<PreprodUsagePctParameter, Defect> vr = manageBsPreprodStatUsagePctTool.validate(params);
        assertThat(vr, hasNoDefectsDefinitions());

        InternalToolResult processResult = manageBsPreprodStatUsagePctTool.process(params);
        assertThat(processResult.getMessage(), is(ManageBsPreprodStatUsagePctTool.TITLE + "0"));

        Integer value = property.get();
        assertThat(value, is(0));
    }


    /**
     * Если будет введено верхнее граничное значение (100) -> валидация пройдет и будет возвращено и сохранено в БД
     * данное значение
     */
    @Test
    public void checkUpperLimitValue() {
        PreprodUsagePctParameter params = new PreprodUsagePctParameter()
                .withPreprodUsagePct(100L);
        ValidationResult<PreprodUsagePctParameter, Defect> vr = manageBsPreprodStatUsagePctTool.validate(params);
        assertThat(vr, hasNoDefectsDefinitions());

        InternalToolResult processResult = manageBsPreprodStatUsagePctTool.process(params);
        assertThat(processResult.getMessage(), is(ManageBsPreprodStatUsagePctTool.TITLE + "100"));

        Integer value = property.get();
        assertThat(value, is(100));
    }


    /**
     * Если не будет введено значение -> валидация пройдет и будет возвращено текущее значение строки
     * BS_EXPORT_PREPROD_USAGE_PCT из таблицы PPC_PROPERTIES
     */
    @Test
    public void checkGetInfo() {
        Integer expected = property.get();

        PreprodUsagePctParameter params = new PreprodUsagePctParameter();
        ValidationResult<PreprodUsagePctParameter, Defect> vr = manageBsPreprodStatUsagePctTool.validate(params);
        assertThat(vr, hasNoDefectsDefinitions());

        InternalToolResult processResult = manageBsPreprodStatUsagePctTool.process(params);
        assertThat(processResult.getMessage(), is(ManageBsPreprodStatUsagePctTool.TITLE + expected));
    }

    /**
     * Если в таблице PPC_PROPERTIES нет строки с ключем BS_EXPORT_PREPROD_USAGE_PCT и значение не будет введено ->
     * валидация пройдет и будет возвращен null
     */
    @Test
    public void checkWhenParamIsNullAndWeGetInfo() {
        property.remove();

        PreprodUsagePctParameter params = new PreprodUsagePctParameter();
        ValidationResult<PreprodUsagePctParameter, Defect> vr = manageBsPreprodStatUsagePctTool.validate(params);
        assertThat(vr, hasNoDefectsDefinitions());

        InternalToolResult processResult = manageBsPreprodStatUsagePctTool.process(params);
        assertThat(processResult.getMessage(), is(ManageBsPreprodStatUsagePctTool.TITLE + "null"));
    }
}
