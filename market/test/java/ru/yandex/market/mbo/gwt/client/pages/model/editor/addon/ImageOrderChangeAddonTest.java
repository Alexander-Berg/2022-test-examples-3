package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableModel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.ModelData;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Collections;

import static org.mockito.Mockito.when;

/**
 * Absent values in ParameterValue mean that user has not yet uploaded picture, but for correct rendering
 * ParameterValue has to be added to ParameterValues with Param.Type, xslName and paramId.
 * After user uploads picture stringValue will be filled in ParameterValue
 */
@RunWith(MockitoJUnitRunner.class)
public class ImageOrderChangeAddonTest {

    @Mock
    private EditableModel editableModel;
    @Mock
    private ModelData modelData;

    @InjectMocks
    private ImageOrderChangeAddon imageOrderChangeAddon;

    @Before
    public void init() throws IllegalAccessException {
        FieldUtils.writeField(imageOrderChangeAddon, "editableModel", editableModel, true);
        FieldUtils.writeField(imageOrderChangeAddon, "modelData", modelData, true);
    }

    @Test
    public void sourceAndTargetPicturesAbsentShouldReturnNotEmptyListParamValues() {
        String targetXslName = XslNames.XL_PICTURE;
        String sourceXslName = XslNames.XL_PICTURE + "_2";
        long targetParamId = 1L;
        long sourceParamId = 2L;

        ParameterValue targetParameterValue = new ParameterValue(targetParamId, targetXslName, Param.Type.STRING);
        ParameterValues targetParameterValues = new ParameterValues(targetParamId, targetXslName, Param.Type.STRING,
            Collections.singletonList(targetParameterValue));

        ParameterValue sourceParameterValue = new ParameterValue(sourceParamId, sourceXslName, Param.Type.STRING);
        ParameterValues sourceParameterValues = new ParameterValues(sourceParamId, sourceXslName, Param.Type.STRING,
            Collections.singletonList(sourceParameterValue));

        CommonModel model = new CommonModel();
        model.putParameterValues(targetParameterValues);
        model.putParameterValues(sourceParameterValues);

        ParameterValues result = imageOrderChangeAddon.createParameterValuesFrom(model, targetXslName, sourceXslName);

        Assertions.assertThat(result.getValues()).isNotEmpty();
        Assertions.assertThat(result.getValues().get(0).getStringValue())
            .isEqualTo(sourceParameterValue.getStringValue());
        Assertions.assertThat(result.getXslName()).isEqualTo(targetXslName);
        Assertions.assertThat(result.getParamId()).isEqualTo(targetParamId);
    }

    @Test
    public void whenTargetAbsentButCategoryParamExistShouldReturnNotEmptyListParamValues() {
        String targetXslName = XslNames.XL_PICTURE;
        String sourceXslName = XslNames.XL_PICTURE + "_2";
        long targetParamId = 1L;
        long sourceParamId = 2L;

        ParameterValue sourceParameterValue = new ParameterValue(sourceParamId, sourceXslName, Param.Type.STRING);
        ParameterValues sourceParameterValues = new ParameterValues(sourceParamId, sourceXslName, Param.Type.STRING,
            Collections.singletonList(sourceParameterValue));

        CommonModel model = new CommonModel();
        model.putParameterValues(sourceParameterValues);

        CategoryParam param = CategoryParamBuilder.newBuilder()
            .setId(targetParamId)
            .setXslName(targetXslName)
            .setType(Param.Type.STRING)
            .build();

        when(modelData.getParam(targetXslName)).thenReturn(param);

        ParameterValues result = imageOrderChangeAddon.createParameterValuesFrom(model, targetXslName, sourceXslName);

        Assertions.assertThat(result.getValues()).isNotEmpty();
        Assertions.assertThat(result.getValues().get(0).getStringValue())
            .isEqualTo(sourceParameterValue.getStringValue());
        Assertions.assertThat(result.getXslName()).isEqualTo(targetXslName);
        Assertions.assertThat(result.getParamId()).isEqualTo(targetParamId);
    }

    @Test
    public void whenSourceAbsentButTargetExistShouldReturnNotEmptyListParamValues() {
        String targetXslName = XslNames.XL_PICTURE;
        String sourceXslName = XslNames.XL_PICTURE + "_2";
        long targetParamId = 1L;

        ParameterValue targetParameterValue = new ParameterValue(targetParamId, targetXslName, Param.Type.STRING);
        ParameterValues targetParameterValues = new ParameterValues(targetParamId, targetXslName, Param.Type.STRING,
            Collections.singletonList(targetParameterValue));

        CommonModel model = new CommonModel();
        model.putParameterValues(targetParameterValues);

        ParameterValues result = imageOrderChangeAddon.createParameterValuesFrom(model, targetXslName, sourceXslName);

        Assertions.assertThat(result.getValues()).isNotEmpty();
        Assertions.assertThat(result.getValues().get(0).getStringValue()).isEqualTo(null);
        Assertions.assertThat(result.getXslName()).isEqualTo(targetXslName);
        Assertions.assertThat(result.getParamId()).isEqualTo(targetParamId);
    }
}
