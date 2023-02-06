package ru.yandex.market.mbo.export.modelstorage.pipe;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleOption;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleParameter;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleSizeMeasure;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.apache.logging.log4j.core.Core.CATEGORY_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnitedSizeTemplatePipePartTest {
    private static final int A_PARAMETER_OPTION_ID = 1;
    private static final int B_PARAMETER_OPTION_ID = 2;
    private static final int C_PARAMETER_OPTION_ID = 3;
    private static final int UNITED_SIZE_OPTION_ID = 101;

    private static final int A_PARAMETER_ID = 1;
    private static final int B_PARAMETER_ID = 2;
    private static final int C_PARAMETER_ID = 3;
    private static final int UNITED_SIZE_ID = 100;

    private static final String A_PARAMETER_XSL_NAME = "a";
    private static final String B_PARAMETER_XSL_NAME = "b";
    private static final String C_PARAMETER_XSL_NAME = "c";

    private static final String UNITED_SIZE_VALUE = "US_Value";

    private static final Long CATEGORY_ID = 1L;

    private static final Long IS_SKU_ID = 101L;


    private final ModelStorage.Model protoModel = ModelStorage.Model.newBuilder()
        .setCurrentType(CommonModel.Source.GURU.name())
        .setCategoryId(CATEGORY_ID)
        .build();

    private final ModelStorage.Model sku = ModelStorage.Model.newBuilder()
        .setCurrentType(CommonModel.Source.SKU.name())
        .setCategoryId(CATEGORY_ID)
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(A_PARAMETER_ID)
            .setOptionId(A_PARAMETER_OPTION_ID)
            .setXslName(A_PARAMETER_XSL_NAME)
            .build())
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(B_PARAMETER_ID)
            .setOptionId(B_PARAMETER_OPTION_ID)
            .setXslName(B_PARAMETER_XSL_NAME)
            .build())
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(C_PARAMETER_ID)
            .setOptionId(C_PARAMETER_OPTION_ID)
            .setXslName(C_PARAMETER_XSL_NAME)
            .build())
        .build();

    private final ModelStorage.Model skuWithHypothesis = ModelStorage.Model.newBuilder()
        .setCurrentType(CommonModel.Source.SKU.name())
        .setCategoryId(CATEGORY_ID)
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(A_PARAMETER_ID)
            .setOptionId(A_PARAMETER_OPTION_ID)
            .setXslName(A_PARAMETER_XSL_NAME)
            .build())
        .addParameterValueHypothesis(ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(B_PARAMETER_ID)
            .addStrValue(MboParameters.Word.newBuilder().setName("hyp_b").build())
            .setXslName(B_PARAMETER_XSL_NAME)
            .build())
        .addParameterValueHypothesis(ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(C_PARAMETER_ID)
            .addStrValue(MboParameters.Word.newBuilder().setName("hyp_c").build())
            .setXslName(C_PARAMETER_XSL_NAME)
            .build())
        .build();

    private final ModelStorage.Model skuWithUSHypothesis = ModelStorage.Model.newBuilder()
        .setCurrentType(CommonModel.Source.SKU.name())
        .setCategoryId(CATEGORY_ID)
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(A_PARAMETER_ID)
            .setOptionId(A_PARAMETER_OPTION_ID)
            .setXslName(A_PARAMETER_XSL_NAME)
            .build())
        .addParameterValueHypothesis(ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(B_PARAMETER_ID)
            .addStrValue(MboParameters.Word.newBuilder().setName("hyp_b").build())
            .setXslName(B_PARAMETER_XSL_NAME)
            .build())
        .addParameterValueHypothesis(ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(C_PARAMETER_ID)
            .addStrValue(MboParameters.Word.newBuilder().setName("hyp_c").build())
            .setXslName(C_PARAMETER_XSL_NAME)
            .build())
        .addParameterValueHypothesis(ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(UNITED_SIZE_ID)
            .addStrValue(MboParameters.Word.newBuilder().setName(UNITED_SIZE_VALUE).build())
            .setXslName(XslNames.UNITED_SIZE)
            .build())
        .build();

    private final ModelStorage.Model skuWithUSValue = ModelStorage.Model.newBuilder()
        .setCurrentType(CommonModel.Source.SKU.name())
        .setCategoryId(CATEGORY_ID)
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(A_PARAMETER_ID)
            .setOptionId(A_PARAMETER_OPTION_ID)
            .setXslName(A_PARAMETER_XSL_NAME)
            .build())
        .addParameterValueHypothesis(ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(B_PARAMETER_ID)
            .addStrValue(MboParameters.Word.newBuilder().setName("hyp_b").build())
            .setXslName(B_PARAMETER_XSL_NAME)
            .build())
        .addParameterValueHypothesis(ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(C_PARAMETER_ID)
            .addStrValue(MboParameters.Word.newBuilder().setName("hyp_c").build())
            .setXslName(C_PARAMETER_XSL_NAME)
            .build())
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(UNITED_SIZE_ID)
            .setOptionId(UNITED_SIZE_OPTION_ID)
            .setXslName(XslNames.UNITED_SIZE)
            .build())
        .build();

    private final ModelStorage.Model modelIsSku = ModelStorage.Model.newBuilder()
        .setCurrentType(CommonModel.Source.GURU.name())
        .setCategoryId(CATEGORY_ID)
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(IS_SKU_ID)
            .setBoolValue(true)
            .setXslName(XslNames.IS_SKU)
            .build())
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(A_PARAMETER_ID)
            .setOptionId(A_PARAMETER_OPTION_ID)
            .setXslName(A_PARAMETER_XSL_NAME)
            .build())
        .addParameterValueHypothesis(ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(B_PARAMETER_ID)
            .addStrValue(MboParameters.Word.newBuilder().setName("hyp_b").build())
            .setXslName(B_PARAMETER_XSL_NAME)
            .build())
        .build();

    @Test
    public void setUnitedSizeByTemplateTest() throws IOException {
        final String expectedValue = "ab/c";
        final String template = "{a}{b}/{c}{c}^";

        List<String> definingParameters = Stream.of(
            A_PARAMETER_XSL_NAME, B_PARAMETER_XSL_NAME
        ).collect(Collectors.toList());

        List<ForTitleParameter> forTitleParameters = Stream.of(
            createForTitleParam(A_PARAMETER_ID, A_PARAMETER_OPTION_ID, "a", A_PARAMETER_XSL_NAME, definingParameters),
            createForTitleParam(B_PARAMETER_ID, B_PARAMETER_OPTION_ID, "b", B_PARAMETER_XSL_NAME, definingParameters),
            createForTitleParam(C_PARAMETER_ID, C_PARAMETER_OPTION_ID, "c", C_PARAMETER_XSL_NAME, definingParameters),
            createForTitleParam(0, 0, null, XslNames.UNITED_SIZE, definingParameters)
        ).collect(Collectors.toList());

        CategoryInfo categoryInfo = createCategoryInfo(template, forTitleParameters);

        UnitedSizeTemplatePipePart unitedSizeTemplatePipePart =
            new UnitedSizeTemplatePipePart(categoryInfo);

        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoModel, Collections.emptyList(), Collections.singletonList(sku)
        );

        unitedSizeTemplatePipePart.acceptModelsGroup(modelPipeContext);

        ModelStorage.ParameterValueHypothesis parameterValueHypothesis
            = modelPipeContext.getSkus().get(0).getParameterValueHypothesisList().stream()
                .filter(e -> e.getXslName().equals(XslNames.UNITED_SIZE))
                .findAny()
                .get();

        assertEquals(expectedValue, parameterValueHypothesis.getStrValue(0).getName());
    }

    @Test
    public void setUnitedSizeWithoutTemplateAndWithSomeDefiningParametersTest() throws IOException {
        final String expectedValue = "a/b";

        List<String> definingParameters = Stream.of(
            A_PARAMETER_XSL_NAME, B_PARAMETER_XSL_NAME
        ).collect(Collectors.toList());

        setUnitedSizeWithoutTemplateAndWithDefiningParametersTest(expectedValue, definingParameters);
    }

    @Test
    public void setUnitedSizeWithoutTemplateAndWithOneDefiningParametersTest() throws IOException {
        final String expectedValue = "a";

        List<String> definingParameters = Stream.of(
            A_PARAMETER_XSL_NAME
        ).collect(Collectors.toList());

        setUnitedSizeWithoutTemplateAndWithDefiningParametersTest(expectedValue, definingParameters);
    }

    @Test
    public void setUnitedSizeWithTemplateIfOptionDoesNotExist() throws IOException {
        final String template = "{a}/{b}/{c}";
        final String expectedValue = "a/c";

        List<String> definingParameters = Stream.of(
            A_PARAMETER_XSL_NAME, B_PARAMETER_XSL_NAME
        ).collect(Collectors.toList());

        List<ForTitleParameter> forTitleParameters = Stream.of(
            createForTitleParam(A_PARAMETER_ID, A_PARAMETER_OPTION_ID, "a", A_PARAMETER_XSL_NAME, definingParameters),
            createForTitleParam(C_PARAMETER_ID, C_PARAMETER_OPTION_ID, "c", C_PARAMETER_XSL_NAME, definingParameters),
            createForTitleParam(0, 0, null, XslNames.UNITED_SIZE, definingParameters)
        ).collect(Collectors.toList());

        CategoryInfo categoryInfo = createCategoryInfo(template, forTitleParameters);

        UnitedSizeTemplatePipePart unitedSizeTemplatePipePart =
            new UnitedSizeTemplatePipePart(categoryInfo);

        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoModel, Collections.emptyList(), Collections.singletonList(sku)
        );

        unitedSizeTemplatePipePart.acceptModelsGroup(modelPipeContext);

        ModelStorage.ParameterValueHypothesis parameterValueHypothesis
            = modelPipeContext.getSkus().get(0).getParameterValueHypothesisList().stream()
                .filter(e -> e.getXslName().equals(XslNames.UNITED_SIZE))
                .findAny()
                .get();

        assertEquals(expectedValue, parameterValueHypothesis.getStrValue(0).getName());
    }

    @Test
    public void setUnitedSizeByTemplateAndWithHypothesisTest() throws IOException {
        final String expectedValue = "ahyp_b/hyp_c";
        final String template = "{a}{b}/{c}{c}^";

        List<String> definingParameters = Stream.of(
            A_PARAMETER_XSL_NAME, B_PARAMETER_XSL_NAME
        ).collect(Collectors.toList());

        List<ForTitleParameter> forTitleParameters = Stream.of(
            createForTitleParam(A_PARAMETER_ID, A_PARAMETER_OPTION_ID, "a", A_PARAMETER_XSL_NAME,
                definingParameters),
            createForTitleParam(B_PARAMETER_ID, B_PARAMETER_OPTION_ID, "b", B_PARAMETER_XSL_NAME,
                definingParameters),
            createForTitleParam(C_PARAMETER_ID, C_PARAMETER_OPTION_ID, "c", C_PARAMETER_XSL_NAME,
                definingParameters),
            createForTitleParam(0, 0, null, XslNames.UNITED_SIZE, definingParameters)
        ).collect(Collectors.toList());

        CategoryInfo categoryInfo = createCategoryInfo(template, forTitleParameters);

        UnitedSizeTemplatePipePart unitedSizeTemplatePipePart =
            new UnitedSizeTemplatePipePart(categoryInfo);

        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoModel, Collections.emptyList(), Collections.singletonList(skuWithHypothesis)
        );

        unitedSizeTemplatePipePart.acceptModelsGroup(modelPipeContext);

        ModelStorage.ParameterValueHypothesis parameterValueHypothesis
            = modelPipeContext.getSkus().get(0).getParameterValueHypothesisList().stream()
                .filter(e -> e.getXslName().equals(XslNames.UNITED_SIZE))
                .findAny()
                .get();

        assertEquals(expectedValue, parameterValueHypothesis.getStrValue(0).getName());
    }

    @Test
    public void setUnitedSizeWithoutTemplateAndWithHypothesisTest() throws IOException {
        final String expectedValue = "a/hyp_b";

        List<String> definingParameters = Stream.of(
            A_PARAMETER_XSL_NAME, B_PARAMETER_XSL_NAME
        ).collect(Collectors.toList());

        setUnitedSizeWithoutTemplateAndWithDefiningParametersTest(expectedValue, definingParameters, skuWithHypothesis);
    }

    @Test
    public void setUnitedSizeByTemplateAndWithHypothesisOnIsSkuModelTest() throws IOException {
        final String expectedValue = "a-hyp_b";
        final String template = "{a}-{b}";

        List<String> definingParameters = Stream.of(
            A_PARAMETER_XSL_NAME, B_PARAMETER_XSL_NAME
        ).collect(Collectors.toList());

        List<ForTitleParameter> forTitleParameters = Stream.of(
            createForTitleParam(A_PARAMETER_ID, A_PARAMETER_OPTION_ID, "a", A_PARAMETER_XSL_NAME,
                definingParameters),
            createForTitleParam(B_PARAMETER_ID, B_PARAMETER_OPTION_ID, "b", B_PARAMETER_XSL_NAME,
                definingParameters),
            createForTitleParam(0, 0, null, XslNames.UNITED_SIZE, definingParameters)
        ).collect(Collectors.toList());

        CategoryInfo categoryInfo = createCategoryInfo(template, forTitleParameters);

        UnitedSizeTemplatePipePart unitedSizeTemplatePipePart =
            new UnitedSizeTemplatePipePart(categoryInfo);

        ModelPipeContext modelPipeContext = new ModelPipeContext(
            modelIsSku, Collections.emptyList(), Collections.emptyList()
        );

        unitedSizeTemplatePipePart.acceptModelsGroup(modelPipeContext);

        Optional<ModelStorage.ParameterValueHypothesis> parameterValueHypothesis
            = modelPipeContext.getModel().getParameterValueHypothesisList()
                .stream()
                .filter(e -> e.getXslName().equals(XslNames.UNITED_SIZE))
                .findAny();

        assertTrue(parameterValueHypothesis.isPresent());

        assertEquals(expectedValue, parameterValueHypothesis.get().getStrValue(0).getName());
    }

    @Test
    public void whenSkuHasUnitedSizeHypothesisThenDoNotAddUnitedSize() throws IOException {
        final String template = "{a}/{b}/{c}";
        final String expectedValue = "a/c";

        List<String> definingParameters = Stream.of(
            A_PARAMETER_XSL_NAME, B_PARAMETER_XSL_NAME
        ).collect(Collectors.toList());

        List<ForTitleParameter> forTitleParameters = Stream.of(
            createForTitleParam(A_PARAMETER_ID, A_PARAMETER_OPTION_ID, "a", A_PARAMETER_XSL_NAME, definingParameters),
            createForTitleParam(C_PARAMETER_ID, C_PARAMETER_OPTION_ID, "c", C_PARAMETER_XSL_NAME, definingParameters),
            createForTitleParam(0, 0, null, XslNames.UNITED_SIZE, definingParameters)
        ).collect(Collectors.toList());

        CategoryInfo categoryInfo = createCategoryInfo(template, forTitleParameters);

        UnitedSizeTemplatePipePart unitedSizeTemplatePipePart =
            new UnitedSizeTemplatePipePart(categoryInfo);

        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoModel, Collections.emptyList(), Collections.singletonList(skuWithUSHypothesis)
        );

        unitedSizeTemplatePipePart.acceptModelsGroup(modelPipeContext);

        List<ModelStorage.ParameterValueHypothesis> parameterValueHypothesis
            = modelPipeContext.getSkus().get(0).getParameterValueHypothesisList().stream()
            .filter(e -> e.getXslName().equals(XslNames.UNITED_SIZE))
            .collect(Collectors.toList());

        assertEquals(1, parameterValueHypothesis.size());
        assertEquals(UNITED_SIZE_VALUE, parameterValueHypothesis.get(0).getStrValue(0).getName());
    }

    @Test
    public void whenSkuHasUnitedSizeValueThenDoNotAddUnitedSize() throws IOException {
        final String template = "{a}/{b}/{c}";
        final String expectedValue = "a/c";

        List<String> definingParameters = Stream.of(
            A_PARAMETER_XSL_NAME, B_PARAMETER_XSL_NAME
        ).collect(Collectors.toList());

        List<ForTitleParameter> forTitleParameters = Stream.of(
            createForTitleParam(A_PARAMETER_ID, A_PARAMETER_OPTION_ID, "a", A_PARAMETER_XSL_NAME, definingParameters),
            createForTitleParam(C_PARAMETER_ID, C_PARAMETER_OPTION_ID, "c", C_PARAMETER_XSL_NAME, definingParameters),
            createForTitleParam(0, 0, null, XslNames.UNITED_SIZE, definingParameters)
        ).collect(Collectors.toList());

        CategoryInfo categoryInfo = createCategoryInfo(template, forTitleParameters);

        UnitedSizeTemplatePipePart unitedSizeTemplatePipePart =
            new UnitedSizeTemplatePipePart(categoryInfo);

        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoModel, Collections.emptyList(), Collections.singletonList(skuWithUSValue)
        );

        unitedSizeTemplatePipePart.acceptModelsGroup(modelPipeContext);

        List<ModelStorage.ParameterValueHypothesis> parameterValueHypothesis
            = modelPipeContext.getSkus().get(0).getParameterValueHypothesisList().stream()
            .filter(e -> e.getXslName().equals(XslNames.UNITED_SIZE))
            .collect(Collectors.toList());

        assertTrue(parameterValueHypothesis.isEmpty());
    }

    private void setUnitedSizeWithoutTemplateAndWithDefiningParametersTest(String expected,
                                                                           List<String> definingParameters)
        throws IOException {

        setUnitedSizeWithoutTemplateAndWithDefiningParametersTest(expected, definingParameters, sku);
    }

    private void setUnitedSizeWithoutTemplateAndWithDefiningParametersTest(String expected,
                                                                           List<String> definingParameters,
                                                                           ModelStorage.Model sku)
        throws IOException {
        List<ForTitleParameter> forTitleParameters = Stream.of(
            createForTitleParam(A_PARAMETER_ID, A_PARAMETER_OPTION_ID, "a", A_PARAMETER_XSL_NAME,
                definingParameters),
            createForTitleParam(B_PARAMETER_ID, B_PARAMETER_OPTION_ID, "b", B_PARAMETER_XSL_NAME,
                definingParameters),
            createForTitleParam(0, 0, null, XslNames.UNITED_SIZE, definingParameters)
        ).collect(Collectors.toList());

        CategoryInfo categoryInfo = createCategoryInfo(null, forTitleParameters);

        UnitedSizeTemplatePipePart unitedSizeTemplatePipePart =
            new UnitedSizeTemplatePipePart(categoryInfo);

        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoModel, Collections.emptyList(), Collections.singletonList(sku)
        );

        unitedSizeTemplatePipePart.acceptModelsGroup(modelPipeContext);

        ModelStorage.ParameterValueHypothesis parameterValueHypothesis
            = modelPipeContext.getSkus().get(0).getParameterValueHypothesisList().stream()
                .filter(e -> e.getXslName().equals(XslNames.UNITED_SIZE))
                .findAny()
                .get();

        assertEquals(expected, parameterValueHypothesis.getStrValue(0).getName());

    }

    private ForTitleParameter createForTitleParam(int paramId, int optionId, String optionName, String xslName,
                                                  List<String> definingParameters) {
        ForTitleOption forTitleOption = new ForTitleOption();
        forTitleOption.setId(optionId);
        forTitleOption.setName(optionName);

        ForTitleParameter forTitleParameter = new ForTitleParameter();
        forTitleParameter.setXslName(xslName);
        forTitleParameter.setId(paramId);
        forTitleParameter.setOptions(Collections.singletonList(forTitleOption));

        if (definingParameters.contains(xslName)) {
            forTitleParameter.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        }

        return forTitleParameter;
    }

    private CategoryInfo createCategoryInfo(String unitSizeTemplate, List<ForTitleParameter> forTitleParameters) {
        List<ForTitleSizeMeasure> forTitleSizeMeasures = Stream.of(
            createForTitleSizeMeasureParam(A_PARAMETER_ID),
            createForTitleSizeMeasureParam(B_PARAMETER_ID),
            createForTitleSizeMeasureParam(C_PARAMETER_ID)
        ).collect(Collectors.toList());

        return new CategoryInfo(
            CATEGORY_ID,
            false,
            Collections.emptyList(),
            Collections.singletonList(CommonModel.Source.GURU),
            Collections.emptySet(),
            CATEGORY_NAME,
            new TMTemplate(),
            unitSizeTemplate,
            forTitleParameters,
            forTitleSizeMeasures,
            null,
            null);
    }

    private ForTitleSizeMeasure createForTitleSizeMeasureParam(long id) {
        return ForTitleSizeMeasure.from(0, "", 0, id, 0, 0);
    }

}
