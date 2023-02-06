package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.picture_parameter_value_relation.PictureParameterValueRelationChanged;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestModelPicturesFromParameterValuesAddon extends AbstractModelTest {

    private static final long PARAM_ID_11 = 11L;
    private static final long PARAM_ID_22 = 22L;

    private static final long OPTION_ID_1 = 1L;
    private static final long OPTION_ID_2 = 2L;
    private static final long OPTION_ID_3 = 3L;
    private static final long OPTION_ID_4 = 4L;
    private static final long OPTION_ID_5 = 5L;
    private static final long OPTION_ID_6 = 6L;

    private static final int PARAM_SIZE_4 = 4;
    private static final int PARAM_SIZE_5 = 5;

    CommonModel model;

    @Override
    public void parameters() {
        data.startParameters()
            .startParameter()
                .id(PARAM_ID_11).xsl("XL-Picture").type(Param.Type.ENUM).name("Enum1")
                .forImages(true)
                .option(OPTION_ID_1, "Option1")
                .option(OPTION_ID_2, "Option2")
                .option(OPTION_ID_3, "Option3")
            .endParameter()
            .startParameter()
                .id(PARAM_ID_22).xsl("XL-Picture_2").type(Param.Type.ENUM).name("Enum2")
                .forImages(true)
                .option(OPTION_ID_4, "Option4")
                .option(OPTION_ID_5, "Option5")
                .option(OPTION_ID_6, "Option6")
            .endParameter()
            .endParameters();
    }

    @Override
    public void model() {
        data.startModel()
            .currentType(CommonModel.Source.GURU)
            .param("XL-Picture").setString("url1")
            .param("XL-Picture_2").setString("url2")
            .picture("XL-Picture", "url1")
            .picture("XL-Picture_2", "url2")
            .endModel();

        model = data.getModel();
        model.getPictures().get(0).addParameterValue(createParameterValue(data.getParamMap(), PARAM_ID_11, 0));
        model.getPictures().get(0).addParameterValue(createParameterValue(data.getParamMap(), PARAM_ID_11, 1));
        model.getPictures().get(0).addParameterValue(createParameterValue(data.getParamMap(), PARAM_ID_22, 0));
        model.getPictures().get(0).addParameterValue(createParameterValue(data.getParamMap(), PARAM_ID_22, 1));

        model.getPictures().get(1).addParameterValue(createParameterValue(data.getParamMap(), PARAM_ID_11, 0));
        model.getPictures().get(1).addParameterValue(createParameterValue(data.getParamMap(), PARAM_ID_11, 1));
        model.getPictures().get(1).addParameterValue(createParameterValue(data.getParamMap(), PARAM_ID_22, 0));
        model.getPictures().get(1).addParameterValue(createParameterValue(data.getParamMap(), PARAM_ID_22, 1));
    }

    @Test
    public void testChangeParamValueLinks() {
        List<ParameterValue> parameterValues = new ArrayList<>(model.getPictures().get(0).getParameterValues());
        parameterValues.add(createParameterValue(data.getParamMap(), PARAM_ID_11, 2));
        bus.fireEvent(new PictureParameterValueRelationChanged("XL-Picture", parameterValues));

        Assert.assertEquals(parameterValues, model.getPictures().get(0).getParameterValues());
        Assert.assertEquals(PARAM_SIZE_5, model.getPictures().get(0).getParameterValues().size());
        Assert.assertEquals(PARAM_SIZE_4, model.getPictures().get(1).getParameterValues().size());
    }

    private ParameterValue createParameterValue(Map<Long, CategoryParam> parameters, Long paramId, int optionIndex) {
        return createParameterValue(
            parameters.get(paramId).getOptions().get(optionIndex),
            parameters.get(paramId)
        );
    }

    private ParameterValue createParameterValue(Option option, CategoryParam parameter) {
        return new ParameterValue(
            option.getParamId(),
            parameter.getXslName(),
            Param.Type.ENUM,
            null,
            null,
            option.getId(),
            null,
            null
        );
    }
}
