package ru.yandex.market.mbo.tms.modeltransfer.worker;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.db.transfer.ModelMapper;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterBuilder;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author ayratgdl
 * @date 11.03.18
 */
public class ModelMapperTest {
    private static final Long CATEGORY_ID_1 = 101L;
    private static final Long CATEGORY_ID_2 = 102L;
    private static final Long PARAMETER_ID_1 = 201L;
    private static final Long PARAMETER_ID_2 = 202L;
    private static final Long OPTION_ID_1 = 301L;
    private static final Long OPTION_ID_2 = 302L;
    private static final Long MODEL_ID_1 = 501L;
    private static final ImmutableSet<Long> EMPTY_SKIP_PARAM_IDS = ImmutableSet.of();

    @Test(expected = RuntimeException.class)
    public void failMapWhenAbsentParam() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
                                              new OptionImpl(OPTION_ID_1, "option_name")
        );
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());

        ModelMapper modelMapper = new ModelMapper(entities1, entities2);

        CommonModel sourceModel = new CommonModel();
        sourceModel.setId(MODEL_ID_1);
        sourceModel.setCategoryId(CATEGORY_ID_1);
        sourceModel.addParameterValues(Arrays.asList(buildEnumParameterValue(parameter1, OPTION_ID_1)));

        modelMapper.map(sourceModel);
    }

    @Test(expected = RuntimeException.class)
    public void failMapWhenHypothesisAbsentParam() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
            new OptionImpl(OPTION_ID_1, "option_name")
        );
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());

        ModelMapper modelMapper = new ModelMapper(entities1, entities2);

        CommonModel sourceModel = new CommonModel();
        sourceModel.setId(MODEL_ID_1);
        sourceModel.setCategoryId(CATEGORY_ID_1);
        sourceModel.setParameterValueHypotheses(Arrays.asList(buildEnumParameterValueHypothesis(parameter1)));

        modelMapper.map(sourceModel);
    }

    @Test
    public void doNotFailMapWhenAbsentParamButSkipUnmapped() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
                                              new OptionImpl(OPTION_ID_1, "option_name")
        );
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());

        ModelMapper modelMapper = new ModelMapper(
            entities1,
            entities2,
            true,
            false,
            false,
            EMPTY_SKIP_PARAM_IDS,
            Collections.emptyList()
        );

        CommonModel sourceModel = new CommonModel();
        sourceModel.setId(MODEL_ID_1);
        sourceModel.setCategoryId(CATEGORY_ID_1);
        sourceModel.addParameterValues(Arrays.asList(buildEnumParameterValue(parameter1, OPTION_ID_1)));
        sourceModel.setParameterValueHypotheses(Arrays.asList(buildEnumParameterValueHypothesis(parameter1)));

        modelMapper.map(sourceModel);


        CommonModel expectedMappedModel = new CommonModel();
        expectedMappedModel.setId(0);
        expectedMappedModel.setCategoryId(CATEGORY_ID_2);

        Assert.assertTrue(modelCustomEquals(expectedMappedModel, modelMapper.map(sourceModel)));
    }

    @Test
    public void doNotFailMapWhenAbsentMinorParamAndSkipMinorUnmapped() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "minor_param",
            new OptionImpl(OPTION_ID_1, "option_name")
        );
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());

        ModelMapper modelMapper = new ModelMapper(
            entities1,
            entities2,
            false,
            true,
            false,
            EMPTY_SKIP_PARAM_IDS,
            Collections.emptyList()
        );

        CommonModel sourceModel = CommonModelBuilder.newBuilder()
            .id(MODEL_ID_1).category(CATEGORY_ID_1)
            .parameterValues(parameter1, OPTION_ID_1)
            .parameterValueHypothesis(parameter1)
            .getModel();
        CommonModel expectedMappedModel = CommonModelBuilder.newBuilder()
            .id(0).category(CATEGORY_ID_2)
            .getModel();

        Assert.assertTrue(modelCustomEquals(expectedMappedModel, modelMapper.map(sourceModel)));
    }

    @Test(expected = ModelMapper.ModelMapException.class)
    public void failMapWhenAbsentNameAndSkipMinorUnmapped() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter nameParam = buildParameter(Param.Type.STRING, PARAMETER_ID_1, XslNames.NAME);
        entities1.addParameter(nameParam);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());

        ModelMapper modelMapper = new ModelMapper(
            entities1,
            entities2,
            false,
            true,
            false,
            EMPTY_SKIP_PARAM_IDS,
            Collections.emptyList()
        );

        CommonModel sourceModel = CommonModelBuilder.newBuilder()
            .id(MODEL_ID_1).category(CATEGORY_ID_1)
            .parameterValues(nameParam, "Model name")
            .getModel();

        modelMapper.map(sourceModel);
    }

    @Test(expected = ModelMapper.ModelMapException.class)
    public void failMapWhenAbsentServiceParamAndSkipMinorUnmapped() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "service_param",
            new OptionImpl(OPTION_ID_1, "option_name")
        );
        parameter1.setService(true);
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());

        ModelMapper modelMapper = new ModelMapper(
            entities1,
            entities2,
            false,
            true,
            false,
            EMPTY_SKIP_PARAM_IDS,
            Collections.emptyList()
        );

        CommonModel sourceModel = CommonModelBuilder.newBuilder()
            .id(MODEL_ID_1).category(CATEGORY_ID_1)
            .parameterValues(parameter1, OPTION_ID_1)
            .getModel();

        modelMapper.map(sourceModel);
    }

    @Test(expected = ModelMapper.ModelMapException.class)
    public void failMapWhenAbsentServiceParamOptionAndSkipMinorUnmapped() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "service_param",
            new OptionImpl(OPTION_ID_1, "option_name")
        );
        parameter1.setService(true);
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());
        Parameter parameter2 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "service_param",
            new OptionImpl(OPTION_ID_2, "option_name_2")
        );
        parameter2.setService(true);
        entities2.addParameter(parameter2);

        ModelMapper modelMapper = new ModelMapper(
            entities1,
            entities2,
            false,
            true,
            false,
            EMPTY_SKIP_PARAM_IDS,
            Collections.emptyList()
        );

        CommonModel sourceModel = CommonModelBuilder.newBuilder()
            .id(MODEL_ID_1).category(CATEGORY_ID_1)
            .parameterValues(parameter1, OPTION_ID_1)
            .getModel();

        modelMapper.map(sourceModel);
    }

    @Test(expected = ModelMapper.ModelMapException.class)
    public void failMapWhenAbsentPictureParamAndSkipMinorUnmapped() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.STRING, PARAMETER_ID_1, "XL-Picture_2");
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());

        ModelMapper modelMapper = new ModelMapper(
            entities1,
            entities2,
            false,
            true,
            false,
            EMPTY_SKIP_PARAM_IDS,
            Collections.emptyList()
        );

        CommonModel sourceModel = CommonModelBuilder.newBuilder()
            .id(MODEL_ID_1).category(CATEGORY_ID_1)
            .parameterValues(parameter1, "http://url1")
            .getModel();

        modelMapper.map(sourceModel);
    }

    @Test(expected = ModelMapper.ModelMapException.class)
    public void failMapWhenAbsentSkuParamAndSkipMinorUnmapped() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        CategoryParam skuParam = ParameterBuilder.builder()
            .id(PARAMETER_ID_1)
            .xsl("skuParamXslName1")
            .type(Param.Type.STRING)
            .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .endParameter();
        entities1.addParameter(skuParam);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());

        List<String> filledSkuParams = Collections.singletonList("skuParamXslName1");
        ModelMapper modelMapper = new ModelMapper(
            entities1,
            entities2,
            false,
            true,
            false,
            EMPTY_SKIP_PARAM_IDS,
            filledSkuParams
        );

        CommonModel sourceModel = CommonModelBuilder.newBuilder()
            .id(MODEL_ID_1).category(CATEGORY_ID_1)
            .parameterValues(skuParam, "Sku parameter value")
            .getModel();

        modelMapper.map(sourceModel);
    }

    @Test
    public void mapModelWithParamAndHypothesys() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
                                              new OptionImpl(OPTION_ID_1, "option_name")
        );
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());
        Parameter parameter2 = buildParameter(Param.Type.ENUM, PARAMETER_ID_2, "xsl_name_param",
                                              new OptionImpl(OPTION_ID_2, "option_name")
        );
        entities2.addParameter(parameter2);

        ModelMapper modelMapper = new ModelMapper(entities1, entities2);

        CommonModel sourceModel = new CommonModel();
        sourceModel.setId(MODEL_ID_1);
        sourceModel.setCategoryId(CATEGORY_ID_1);
        sourceModel.addParameterValues(Arrays.asList(buildEnumParameterValue(parameter1, OPTION_ID_1)));
        sourceModel.setParameterValueHypotheses(Arrays.asList(buildEnumParameterValueHypothesis(parameter1)));

        CommonModel expectedMappedModel = new CommonModel();
        expectedMappedModel.setId(0);
        expectedMappedModel.setCategoryId(CATEGORY_ID_2);
        expectedMappedModel.addParameterValues(Arrays.asList(buildEnumParameterValue(parameter2, OPTION_ID_2)));
        expectedMappedModel.setParameterValueHypotheses(Arrays.asList(buildEnumParameterValueHypothesis(parameter2)));

        Assert.assertTrue(modelCustomEquals(expectedMappedModel, modelMapper.map(sourceModel)));
    }

    @Test(expected = RuntimeException.class)
    public void failMapWhenAbsentParamLink() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
                                              new OptionImpl(OPTION_ID_1, "option_name")
        );
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());

        ModelMapper modelMapper = new ModelMapper(entities1, entities2);

        CommonModel sourceModel = new CommonModel();
        sourceModel.setId(MODEL_ID_1);
        sourceModel.setCategoryId(CATEGORY_ID_1);
        sourceModel.addParameterValueLink(buildEnumParameterValue(parameter1, OPTION_ID_1));

        modelMapper.map(sourceModel);
    }

    @Test
    public void mapModelWithParamLink() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
                                              new OptionImpl(OPTION_ID_1, "option_name")
        );
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());
        Parameter parameter2 = buildParameter(Param.Type.ENUM, PARAMETER_ID_2, "xsl_name_param",
                                              new OptionImpl(OPTION_ID_2, "option_name")
        );
        entities2.addParameter(parameter2);

        ModelMapper modelMapper = new ModelMapper(entities1, entities2);

        CommonModel sourceModel = new CommonModel();
        sourceModel.setId(MODEL_ID_1);
        sourceModel.setCategoryId(CATEGORY_ID_1);
        sourceModel.addParameterValueLink(buildEnumParameterValue(parameter1, OPTION_ID_1));

        CommonModel expectedMappedModel = new CommonModel();
        expectedMappedModel.setId(0);
        expectedMappedModel.setCategoryId(CATEGORY_ID_2);
        expectedMappedModel.addParameterValueLink(buildEnumParameterValue(parameter2, OPTION_ID_2));

        Assert.assertTrue(modelCustomEquals(expectedMappedModel, modelMapper.map(sourceModel)));
    }

    @Test(expected = RuntimeException.class)
    public void failMapWhenAbsentPicture() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
                                              new OptionImpl(OPTION_ID_1, "option_name")
        );
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());

        ModelMapper modelMapper = new ModelMapper(entities1, entities2);

        CommonModel sourceModel = new CommonModel();
        sourceModel.setId(MODEL_ID_1);
        sourceModel.setCategoryId(CATEGORY_ID_1);
        Picture picture = new Picture();
        picture.addParameterValue(buildEnumParameterValue(parameter1, OPTION_ID_1));
        sourceModel.addPicture(picture);

        modelMapper.map(sourceModel);
    }

    @Test
    public void mapModelWithPicture() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
                                              new OptionImpl(OPTION_ID_1, "option_name")
        );
        entities1.addParameter(parameter1);

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());
        Parameter parameter2 = buildParameter(Param.Type.ENUM, PARAMETER_ID_2, "xsl_name_param",
                                              new OptionImpl(OPTION_ID_2, "option_name")
        );
        entities2.addParameter(parameter2);

        ModelMapper modelMapper = new ModelMapper(entities1, entities2);

        CommonModel sourceModel = new CommonModel();
        sourceModel.setId(MODEL_ID_1);
        sourceModel.setCategoryId(CATEGORY_ID_1);
        Picture picture1 = new Picture();
        picture1.addParameterValue(buildEnumParameterValue(parameter1, OPTION_ID_1));
        sourceModel.addPicture(picture1);

        CommonModel expectedMappedModel = new CommonModel();
        expectedMappedModel.setId(0);
        expectedMappedModel.setCategoryId(CATEGORY_ID_2);
        Picture picture2 = new Picture();
        picture2.addParameterValue(buildEnumParameterValue(parameter2, OPTION_ID_2));
        expectedMappedModel.addPicture(picture2);

        Assert.assertTrue(modelCustomEquals(expectedMappedModel, modelMapper.map(sourceModel)));
    }

    @Test
    public void mapModelAndSkipValueWhichAbsentInSourceCategory() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());

        ModelMapper modelMapper = new ModelMapper(entities1, entities2);

        CommonModel sourceModel = new CommonModel();
        sourceModel.setId(MODEL_ID_1);
        sourceModel.setCategoryId(CATEGORY_ID_1);
        Parameter excessParameter = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param");
        sourceModel.addParameterValues(Arrays.asList(buildEnumParameterValue(excessParameter, OPTION_ID_1)));

        CommonModel expectedMappedModel = new CommonModel();
        expectedMappedModel.setId(0);
        expectedMappedModel.setCategoryId(CATEGORY_ID_2);

        Assert.assertTrue(modelCustomEquals(expectedMappedModel, modelMapper.map(sourceModel)));
    }

    private static boolean modelCustomEquals(CommonModel model1, CommonModel model2) {
        return model1.getId() == model2.getId()
            && model1.getCategoryId() == model2.getCategoryId()
            && equalsCollectionAsList(model1.getParameterValues(), model2.getParameterValues())
            && equalsCollectionAsList(model1.getParameterValueLinks(), model2.getParameterValueLinks())
            && Objects.equals(model1.getPictures(), model2.getPictures());
    }

    private static <E> boolean equalsCollectionAsList(Collection<E> col1, Collection<E> col2) {
        if (col1 == col2) {
            return true;
        }
        if (col1 == null || col2 == null) {
            return false;
        }
        return new ArrayList<>(col1).equals(new ArrayList<>(col2));
    }

    private Parameter buildParameter(Param.Type type, Long id, String xslName, Option... options) {
        Parameter parameter = new Parameter();
        parameter.setType(type);
        parameter.setId(id);
        parameter.setXslName(xslName);
        parameter.setOptions(Arrays.asList(options));
        return parameter;
    }

    private ParameterValue buildEnumParameterValue(CategoryParam param, Long optionId) {
        ParameterValue parameterValue = new ParameterValue(param);
        parameterValue.setOptionId(optionId);
        return parameterValue;
    }

    private ParameterValueHypothesis buildEnumParameterValueHypothesis(CategoryParam param) {
        return new ParameterValueHypothesis(param, WordUtil.defaultWords("test"));
    }
}
