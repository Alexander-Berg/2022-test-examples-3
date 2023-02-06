package ru.yandex.market.markup2.utils.cards;

import junit.framework.TestCase;
import ru.yandex.market.markup2.utils.ParameterTestUtils;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;

/**
 * @author inenakhov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryParamsHelperTest extends TestCase {
    private final ArrayList<MboParameters.Parameter> categoryParams = new ArrayList<>();
    {
        MboParameters.Parameter.Builder stringParamBuilder = MboParameters.Parameter.newBuilder();
        stringParamBuilder.setValueType(MboParameters.ValueType.STRING);

        stringParamBuilder.addName(0, ParameterTestUtils.createWord("String param1"));
        stringParamBuilder.addName(1, ParameterTestUtils.createWord("String param2"));
        stringParamBuilder.setId(1L);
        stringParamBuilder.setAdvFilterIndex(1);
        stringParamBuilder.setCommonFilterIndex(1);
        categoryParams.add(stringParamBuilder.build());

        MboParameters.Parameter.Builder numericParamBuilder = MboParameters.Parameter.newBuilder();
        numericParamBuilder.setValueType(MboParameters.ValueType.NUMERIC);
        numericParamBuilder.addName(0, ParameterTestUtils.createWord("Num param1"));
        numericParamBuilder.addName(1, ParameterTestUtils.createWord("Num param2"));
        numericParamBuilder.setId(2L);
        numericParamBuilder.setAdvFilterIndex(1);
        numericParamBuilder.setCommonFilterIndex(0);
        categoryParams.add(numericParamBuilder.build());

        MboParameters.Parameter.Builder hiddenParam = MboParameters.Parameter.newBuilder();
        hiddenParam.setValueType(MboParameters.ValueType.NUMERIC);
        hiddenParam.addName(0, ParameterTestUtils.createWord("Hidden param1"));
        hiddenParam.addName(1, ParameterTestUtils.createWord("Hidden param2"));
        hiddenParam.setId(3L);
        hiddenParam.setAdvFilterIndex(-1);
        hiddenParam.setCommonFilterIndex(-1);
        categoryParams.add(hiddenParam.build());
    }

    private ModelStorage.Model model;
    {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
        builder.addTitles(0, ParameterTestUtils.createLocalizedString("title"));
        builder.addDescriptions(0, ParameterTestUtils.createLocalizedString("descriptions"));
        builder.addPictures(0, ModelStorage.Picture.newBuilder().setUrl("//bad").build());


        builder.addParameterValues(0, ParameterTestUtils.createStringParamValue(1L, "value"));
        builder.addParameterValues(1, ParameterTestUtils.createNumericParamValue(2L, "2"));
        builder.addParameterValues(1, ParameterTestUtils.createStringParamValue(3L, "3"));


        ModelStorage.ParameterValue.Builder imageUrlParam = ModelStorage.ParameterValue.newBuilder();
        imageUrlParam.addStrValue(0, ParameterTestUtils.createLocalizedString("//good"));
        imageUrlParam.setParamId(4L);
        imageUrlParam.setXslName(ParamUtils.XL_PICTURE_XLS_NAME);
        builder.addParameterValues(3, imageUrlParam);
        model = builder.build();
    }

    private ModelStorage.Model modelWithEmptyParams;
    {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
        builder.addTitles(0, ParameterTestUtils.createLocalizedString("title"));
        builder.addDescriptions(0, ParameterTestUtils.createLocalizedString("descriptions"));
        builder.addPictures(0, ModelStorage.Picture.newBuilder().setUrl("//bad").build());


        ModelStorage.ParameterValue.Builder imageUrlParam = ModelStorage.ParameterValue.newBuilder();
        imageUrlParam.addStrValue(0, ParameterTestUtils.createLocalizedString("//good"));
        imageUrlParam.setParamId(4L);
        imageUrlParam.setXslName(ParamUtils.XL_PICTURE_XLS_NAME);
        builder.addParameterValues(0, imageUrlParam);
        modelWithEmptyParams = builder.build();
    }

    private ModelStorage.Model modelWithEmptyImage;
    {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
        builder.addTitles(0, ParameterTestUtils.createLocalizedString("title"));
        builder.addDescriptions(0, ParameterTestUtils.createLocalizedString("descriptions"));
        builder.addPictures(0, ModelStorage.Picture.newBuilder().setUrl("//bad").build());


        builder.addParameterValues(0, ParameterTestUtils.createStringParamValue(1L, "value"));
        builder.addParameterValues(1, ParameterTestUtils.createNumericParamValue(2L, "2"));
        builder.addParameterValues(1, ParameterTestUtils.createStringParamValue(3L, "3"));


        modelWithEmptyImage = builder.build();
    }

    private CategoryParamsHelper categoryParamsHelper = CategoryParamsHelper.newInstance(categoryParams);

    public void testGenerateDescription() throws Exception {
        assertEquals("String param1: value, Num param1: 2", categoryParamsHelper.generateDescription(model));
    }

    public void testGenerateDescriptionNoModelParams() throws Exception {
        assertEquals("", categoryParamsHelper.generateDescription(modelWithEmptyParams));
    }

    public void testExtractImageUrl() throws Exception {
        assertEquals("https://good", categoryParamsHelper.extractImageUrl(model));
    }

    public void testExtractImageUrlNoModelImage() throws Exception {
        assertEquals("", categoryParamsHelper.extractImageUrl(modelWithEmptyImage));
    }
}
