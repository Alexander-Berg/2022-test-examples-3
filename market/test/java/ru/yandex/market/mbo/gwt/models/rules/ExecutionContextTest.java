package ru.yandex.market.mbo.gwt.models.rules;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.FullModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 29.11.2018
 */
public class ExecutionContextTest {

    private static final long OPTION_1_1 = 81954907;
    private static final long OPTION_1_2 = 27768629;
    private static final long OPTION_1_3 = 64267675;

    private static final long OPTION_2_1 = 15684603;
    private static final long OPTION_2_2 = 15377806;
    private static final long OPTION_2_3 = 84847410;

    private static final long OPTION_2_1_VALUE = 603;
    private static final long OPTION_2_2_VALUE = 806;
    private static final long OPTION_2_3_VALUE = 410;
    private static final String ENUM_MULTIFIED = "enum_multified";
    private static final String NUMERIC_ENUM_MULTIFIELD = "numeric_enum_multifield";
    private List<CategoryParam> parameters;

    @Before
    public void before() {
        // @formatter:off
        parameters = ParametersBuilder.startParameters()
            .startParameter()
                .xslAndName(ENUM_MULTIFIED)
                .type(Param.Type.ENUM)
                .multifield(true)
                .option(OPTION_1_1, "option_1")
                .option(OPTION_1_2, "option_2")
                .option(OPTION_1_3, "option_3")
            .endParameter()
            .startParameter()
                .xslAndName(NUMERIC_ENUM_MULTIFIELD)
                .type(Param.Type.NUMERIC_ENUM)
                .multifield(true)
                .option(OPTION_2_1, OPTION_2_1_VALUE)
                .option(OPTION_2_2, OPTION_2_2_VALUE)
                .option(OPTION_2_3, OPTION_2_3_VALUE)
            .endParameter()
            .endParameters();

        // @formatter:on
    }

    @Test
    public void enumShouldBeAnArray() {
        FullModel model = new FullModel(CommonModelBuilder.model(parameters)
            .param(ENUM_MULTIFIED).setOption(OPTION_1_2)
            .param(ENUM_MULTIFIED).setOption(OPTION_1_3)
            .param(NUMERIC_ENUM_MULTIFIELD).setOption(OPTION_2_1)
            .getModel());
        ExecutionContext context = new ExecutionContext(parameters, model, ModificationSource.ASSESSOR);

        Object value = context.getParamValueByXslName(ENUM_MULTIFIED);
        assertThat(value).isInstanceOf(String[].class);
        assertThat((String[]) value).containsExactly("option_2", "option_3");
    }

    @Test
    public void enumShouldBeAnArrayEvenIfSingle() {
        FullModel model = new FullModel(CommonModelBuilder.model(parameters)
            .param(ENUM_MULTIFIED).setOption(OPTION_1_2)
            .param(NUMERIC_ENUM_MULTIFIELD).setOption(OPTION_2_1)
            .getModel());
        ExecutionContext context = new ExecutionContext(parameters, model, ModificationSource.ASSESSOR);

        Object value = context.getParamValueByXslName(ENUM_MULTIFIED);
        assertThat(value).isInstanceOf(String[].class);
        assertThat((String[]) value).containsExactly("option_2");
    }

    @Test
    public void numericEnumShouldBeAnArray() {
        FullModel model = new FullModel(CommonModelBuilder.model(parameters)
            .param(NUMERIC_ENUM_MULTIFIELD).setOption(OPTION_2_1)
            .param(NUMERIC_ENUM_MULTIFIELD).setOption(OPTION_2_3)
            .getModel());
        ExecutionContext context = new ExecutionContext(parameters, model, ModificationSource.ASSESSOR);

        Object value = context.getParamValueByXslName(NUMERIC_ENUM_MULTIFIELD);
        assertThat(value).isInstanceOf(String[].class);
        assertThat((String[]) value).containsExactly(String.valueOf(OPTION_2_1_VALUE),
            String.valueOf(OPTION_2_3_VALUE));
    }

    @Test
    public void numericEnumShouldBeAnArrayEvenIfSingle() {
        FullModel model = new FullModel(CommonModelBuilder.model(parameters)
            .param(NUMERIC_ENUM_MULTIFIELD).setOption(OPTION_2_2)
            .getModel());
        ExecutionContext context = new ExecutionContext(parameters, model, ModificationSource.ASSESSOR);

        Object value = context.getParamValueByXslName(NUMERIC_ENUM_MULTIFIELD);
        assertThat(value).isInstanceOf(String[].class);
        assertThat((String[]) value).containsExactly(String.valueOf(OPTION_2_2_VALUE));

    }
}
