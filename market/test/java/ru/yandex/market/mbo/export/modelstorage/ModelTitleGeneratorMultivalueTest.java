package ru.yandex.market.mbo.export.modelstorage;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.StringAssert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.core.title.ModelTitleGenerator;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleParameter;
import ru.yandex.market.mbo.gwt.models.titlemaker.ModelTitle;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 20.04.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ModelTitleGeneratorMultivalueTest {

    private List<ForTitleParameter> forTitleParameters;
    private List<CategoryParam> categoryParameters;

    @Before
    public void before() {
        categoryParameters = ParametersBuilder
            .startParameters()

            .startParameter()
            .id(1).xsl("age").name("Age").type(Param.Type.NUMERIC)
            .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .startUnit().name("возраст").reportName("лет").scale(0).measureId(0).id(0).endUnit()
            .endParameter()

            .startParameter()
            .id(2).xsl("name").name("Name").type(Param.Type.STRING)
            .endParameter()

            .startParameter()
            .id(3).xsl("vendor").name("Vendor").type(Param.Type.ENUM)
            .option(11, "AAA")
            .option(12, "BBB")
            .endParameter()

            .startParameter()
            .id(4).xsl("is_universal").name("Universal").type(Param.Type.BOOLEAN)
            .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .endParameter()

            .startParameter()
            .id(5).xsl("voltage").name("Voltage").type(Param.Type.NUMERIC_ENUM)
            .startUnit().name("напряжение").reportName("В").scale(1).measureId(1).id(2).endUnit()
            .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .option(13, 220)
            .option(14, 380)
            .endParameter()

            .startParameter()
            .id(6).xsl("color").name("Цвет").type(Param.Type.STRING)
            .endParameter()

            .endParameters();

        forTitleParameters = categoryParameters
            .stream()
            .map(ForTitleParameter::fromCategoryParam)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Test
    public void joinDefault() {
        assertThatTitleForPattern("[true, join(s1)]", modelWithAges(49, 14, 17)).isEqualTo("49, 14, 17");
    }

    @Test
    public void joinStrings() {
        // first char in the title always capitalized
        assertThatTitleForPattern("[true, join(s6)]", modelWithColors("red", "green")).isEqualTo("Red, green");
    }

    @Test
    public void joinEmpty() {
        assertThatTitleForPattern("[true, join(s1)]", bareModel()).isEqualTo("");
        assertThatTitleForPattern("[true, join(s5)]", bareModel()).isEqualTo("");
    }

    @Test
    public void emptyMandatory() {
        assertThatTitleForPattern("[true, join(s1), \"false-result\", true]", bareModel())
            .hasError()
            .isEqualTo("Error: Mandatory field <Age> is undefined");

        assertThatTitleForPattern("[true, join(s6), \"false-result\", true]", bareModel())
            .hasError()
            .isEqualTo("Error: Mandatory field <Цвет> is undefined");
    }

    @Test
    public void joinWithDelimiter() {
        assertThatTitleForPattern("[true, join(s1, '==')]", modelWithAges(49, 14, 17)).isEqualTo("49==14==17");
    }

    @Test
    public void joinWithSuffix() {
        assertThatTitleForPattern("[true, join(s1, null, ' лет')]", modelWithAges(49, 14, 17))
            .isEqualTo("49 лет, 14 лет, 17 лет");
    }

    @Test
    public void joinWithDelimiterSuffix() {
        assertThatTitleForPattern("[true, join(s1, ' и ', ' лет')]", modelWithAges(49, 14, 17))
            .isEqualTo("49 лет и 14 лет и 17 лет");
    }

    @Test
    public void joinFirst() {
        assertThatTitleForPattern("[true, joinFirst(s1, 2)]", modelWithAges(49, 14, 17)).isEqualTo("49, 14");
        assertThatTitleForPattern("[true, joinFirst(s1, 99)]", modelWithAges(49, 14, 17)).isEqualTo("49, 14, 17");
    }

    @Test
    public void joinFirstWithDelimiter() {
        assertThatTitleForPattern("[true, joinFirst(s1, 2, '==')]", modelWithAges(49, 14, 17)).isEqualTo("49==14");
        assertThatTitleForPattern("[true, joinFirst(s1, 99, '==')]", modelWithAges(49, 14, 17)).isEqualTo("49==14==17");
    }

    @Test
    public void joinFirstWithSuffix() {
        assertThatTitleForPattern("[true, joinFirst(s1, 2, '_', '#')]", modelWithAges(49, 14, 17)).isEqualTo("49#_14#");
    }

    @Test
    public void joinFirstWrongCount() {
        assertThatTitleForPattern("[true, joinFirst(s1, 0)]", modelWithAges(49, 14, 17))
            .hasError()
            .isEqualTo("Error: joinFirst got count <= 0");
    }

    @Test
    public void contains() {
        assertThatTitleForPattern("[true, contains(s1, '14')]", modelWithAges(49, 14, 17)).isEqualTo("True");
        assertThatTitleForPattern("[true, contains(s1, '47')]", modelWithAges(49, 14, 17)).isEqualTo("False");
    }

    @Test
    public void containsString() {
        assertThatTitleForPattern("[true, contains(s6, 'blue')]", modelWithColors("red", "blue")).isEqualTo("True");
        assertThatTitleForPattern("[true, contains(s6, 'green')]", modelWithColors("red", "blue")).isEqualTo("False");
    }

    @Test
    public void containsEmpty() {
        assertThatTitleForPattern("[true, contains(s1, '15')]", bareModel()).isEqualTo("False");
        assertThatTitleForPattern("[true, contains(s6, 'blue')]", bareModel()).isEqualTo("False");
    }

    @Test
    public void containsNull() {
        assertThatTitleForPattern("[true, contains(s1, null)]", modelWithAges(49, 14, 17)).isEqualTo("False");
        assertThatTitleForPattern("[true, contains(s6, null)]", modelWithColors("red", "blue")).isEqualTo("False");
    }

    @Test
    public void containsConvertsToString() {
        assertThatTitleForPattern("[true, contains(s1, 17)]", modelWithAges(49, 14, 17)).isEqualTo("True");
        assertThatTitleForPattern("[true, contains(s1, 19)]", modelWithAges(49, 14, 17)).isEqualTo("False");
    }

    @Test
    public void containsMultipleOptions() {
        assertThatTitleForPattern("[true, contains(s1, 17, 14)]", modelWithAges(49, 14, 17)).isEqualTo("True");
        assertThatTitleForPattern("[true, contains(s1, 17, 1)]", modelWithAges(49, 14, 17)).isEqualTo("False");
    }

    @Test
    public void defaultRepresentation() {
        assertThatTitleForPattern("[true, s1]", modelWithAges(49, 14, 17)).isEqualTo("49, 14, 17");
        assertThatTitleForPattern("[true, s1]", bareModel()).isEqualTo("");
    }

    @Test
    public void count() {
        assertThatTitleForPattern("[true, count(s1)]", modelWithAges(1)).isEqualTo("1");
        assertThatTitleForPattern("[true, count(s1)]", modelWithAges(1, 4, 5)).isEqualTo("3");
    }

    @Test
    public void countStrings() {
        assertThatTitleForPattern("[true, count(s6)]", modelWithColors("red")).isEqualTo("1");
        assertThatTitleForPattern("[true, count(s6)]", modelWithColors("green", "beige")).isEqualTo("2");
    }

    @Test
    public void countEmpty() {
        assertThatTitleForPattern("[true, count(s1)]", bareModel()).isEqualTo("0");
        assertThatTitleForPattern("[true, count(s6)]", bareModel()).isEqualTo("0");
    }

    @Test
    public void wrongArgumentError() {
        assertThatTitleForPattern("[true, count(125)]", bareModel())
            .hasError()
            .isEqualTo("Error: count expects array as first argument, got number");

        assertThatTitleForPattern("[true, join('str')]", bareModel())
            .hasError()
            .isEqualTo("Error: join expects array as first argument, got string");

        assertThatTitleForPattern("[true, contains(true)]", bareModel())
            .hasError()
            .isEqualTo("Error: contains expects array as first argument, got boolean");

        assertThatTitleForPattern("[true, contains()]", bareModel())
            .hasError()
            .isEqualTo("Error: contains expects array as first argument, got undefined");
    }

    private TitleAssert assertThatTitleForPattern(String template, CommonModel model) {
        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, forTitleParameters, emptyList());
        ModelTitle title = titleGenerator.createTitle(model);
        return new TitleAssert(title);
    }

    private static class TitleAssert extends StringAssert {
        private final ModelTitle title;

        TitleAssert(ModelTitle title) {
            super(title.getTitle());
            this.title = title;
        }

        public StringAssert hasError() {
            Assertions.assertThat(title.getError()).withFailMessage("error is empty").isNotEmpty();
            return new StringAssert(title.getError());
        }
    }

    private CommonModel modelWithAges(int... ages) {
        CommonModelBuilder<CommonModel> builder = model();

        for (int age : ages) {
            builder.param("age").setNumeric(age);
        }
        return builder
            .param("name").setString("Model Name")
            .getModel();
    }

    private CommonModel modelWithColors(String... colors) {
        CommonModelBuilder<CommonModel> builder = model();

        builder.param("color").setString(colors);

        return builder
            .param("name").setString("Model Name")
            .getModel();
    }

    private CommonModel bareModel() {
        return model().getModel();
    }

    private CommonModelBuilder<CommonModel> model() {
        return CommonModelBuilder.newBuilder()
            .currentType(CommonModel.Source.GURU)
            .parameters(categoryParameters);
    }

}
