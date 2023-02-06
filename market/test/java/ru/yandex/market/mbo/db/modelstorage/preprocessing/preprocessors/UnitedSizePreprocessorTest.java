package ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors;

import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersConfig;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class UnitedSizePreprocessorTest {

    private static final long HID = 1L;
    private static final long VENDOR_ID = 10L;

    private static final String HYPOTHESIS_VALUE = "XS";

    private static final String PARAMETER_XSL_NAME_1 = "param_name_1";
    private static final String PARAMETER_XSL_NAME_2 = "param_name_2";

    private static final long PARAMETER_ID_1 = 1L;
    private static final long PARAMETER_ID_2 = 2L;

    private final ModelSaveContext modelSaveContext = new ModelSaveContext(1L, null);

    @Mock
    private CategoryParametersServiceClient categoryParametersServiceClient;
    @InjectMocks
    private UnitedSizePreprocessor unitedSizePreprocessor;

    private final ParameterValueHypothesis unitedSizeParam = new ParameterValueHypothesis();

    @Before
    public void setUp() {
        unitedSizeParam.setParamId(6L);
        unitedSizeParam.setXslName(XslNames.UNITED_SIZE);
        unitedSizeParam.setStringValue(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, HYPOTHESIS_VALUE)));
    }

    @Test
    @DisplayName("Проставление гипотезы, если вендорская сетка не найдена")
    public void setHypothesisValueIntoDefiningParametersTestIfUnitNotFound() {
        List<MboParameters.Parameter> categoryParameters = Stream.of(
            MboParameters.Parameter.newBuilder()
                .setSubType(MboParameters.SubType.SIZE)
                .setId(PARAMETER_ID_2)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .setXslName(PARAMETER_XSL_NAME_2)
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(10L)
                .setSubType(MboParameters.SubType.COLOR)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(11L)
                .setSubType(MboParameters.SubType.SIZE)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                .build()
        ).collect(Collectors.toList());


        MboParameters.Category.Builder categoryBuilder = MboParameters.Category.newBuilder();
        categoryParameters.forEach(categoryBuilder::addParameter);
        MboParameters.Category category = categoryBuilder.build();

        when(categoryParametersServiceClient.getCategoryParameters(HID))
            .thenReturn(new CategoryParametersConfig(category));


        CommonModel model = createModelSaveGroup(Collections.emptyList(),
            Stream.of(unitedSizeParam).collect(Collectors.toList()));
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(model);

        unitedSizePreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        assertEquals(
            HYPOTHESIS_VALUE,
            model.getParameterValueHypotheses().stream()
                .filter(e -> e.getXslName().equals(PARAMETER_XSL_NAME_2))
                .findAny()
                .get()
                .getStringValue()
                .get(0)
                .getWord()
        );
    }

    @Test
    @DisplayName("Проставление гипотезы, если параметр присутствует в модели, но не содержит значений")
    public void setHypothesisValueIntoDefiningParametersTestIfParamValueEmpty() {
        List<MboParameters.Parameter> categoryParameters = Stream.of(
            MboParameters.Parameter.newBuilder()
                .setSubType(MboParameters.SubType.SIZE)
                .setId(PARAMETER_ID_2)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .setXslName(PARAMETER_XSL_NAME_2)
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(10L)
                .setSubType(MboParameters.SubType.COLOR)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(11L)
                .setSubType(MboParameters.SubType.SIZE)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                .build()
        ).collect(Collectors.toList());


        MboParameters.Category.Builder categoryBuilder = MboParameters.Category.newBuilder();
        categoryParameters.forEach(categoryBuilder::addParameter);
        MboParameters.Category category = categoryBuilder.build();

        when(categoryParametersServiceClient.getCategoryParameters(HID))
            .thenReturn(new CategoryParametersConfig(category));

        ParameterValue existingParam = new ParameterValue();
        existingParam.setParamId(PARAMETER_ID_2);
        existingParam.setXslName(PARAMETER_XSL_NAME_2);

        CommonModel model = createModelSaveGroup(
            Collections.singletonList(existingParam),
            Stream.of(unitedSizeParam).collect(Collectors.toList())
        );

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(model);

        unitedSizePreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        assertTrue(model.getParameterValueHypotheses().stream()
            .anyMatch(e -> e.getXslName().equals(PARAMETER_XSL_NAME_2)));
        assertEquals(
            HYPOTHESIS_VALUE,
            model.getParameterValueHypotheses().stream()
                .filter(e -> e.getXslName().equals(PARAMETER_XSL_NAME_2))
                .findAny()
                .get()
                .getStringValue()
                .get(0)
                .getWord()
        );

    }

    @Test
    @DisplayName("Если определяющий параметр уже имеет значение, " +
        "его значение затирается и меняется на значение параметра единый размер"
    )
    public void replaceExistedParameterHypothesisValue() {
        List<MboParameters.Parameter> categoryParameters = Stream.of(
            MboParameters.Parameter.newBuilder()
                .setSubType(MboParameters.SubType.SIZE)
                .setXslName(PARAMETER_XSL_NAME_1)
                .setId(PARAMETER_ID_1)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .build()
        ).collect(Collectors.toList());

        MboParameters.Category.Builder categoryBuilder = MboParameters.Category.newBuilder();
        categoryParameters.forEach(categoryBuilder::addParameter);
        MboParameters.Category category = categoryBuilder.build();

        final Long existingParamOption = 2L;
        ParameterValue existingParam = new ParameterValue();
        existingParam.setParamId(PARAMETER_ID_1);
        existingParam.setXslName(PARAMETER_XSL_NAME_1);
        existingParam.setOptionId(existingParamOption);

        when(categoryParametersServiceClient.getCategoryParameters(HID))
            .thenReturn(new CategoryParametersConfig(category));

        CommonModel model = createModelSaveGroup(
            Collections.singletonList(existingParam),
            Stream.of(unitedSizeParam).collect(Collectors.toList())
        );
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(model);

        unitedSizePreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        assertTrue(model.getParameterValueHypotheses().stream()
            .anyMatch(e -> e.getXslName().equals(PARAMETER_XSL_NAME_1)));
        assertEquals(
            HYPOTHESIS_VALUE,
            model.getParameterValueHypotheses().stream()
                .filter(e -> e.getXslName().equals(PARAMETER_XSL_NAME_1))
                .findAny()
                .get()
                .getStringValue()
                .get(0)
                .getWord()
        );
    }

    private CommonModel createModelSaveGroup(List<ParameterValue> params,
                                             List<ParameterValueHypothesis> parameterValueHypotheses) {
        ParameterValue vendorParam = new ParameterValue();
        vendorParam.setParamId(VENDOR_ID);
        vendorParam.setXslName(XslNames.VENDOR);

        CommonModel model = new CommonModel();
        model.setCurrentType(CommonModel.Source.SKU);
        model.setSource(CommonModel.Source.PARTNER_SKU);
        model.setCategoryId(HID);
        model.addParameterValue(vendorParam);
        parameterValueHypotheses.forEach(model::putParameterValueHypothesis);
        return model;
    }

}
