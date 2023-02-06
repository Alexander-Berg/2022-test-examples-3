package ru.yandex.mbo.tool.jira.MBO23611;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.mbo.common.utils.LocalizedStringUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dergachevfv
 * @since 1/13/20
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class LoadBarCodesToolTest {

    private static final long CATEGORY_ID = 1L;
    private static final long MODEL_ID = 100L;
    private static final String TOOL_BARCODE = "0123456789";
    private static final String MODEL_BARCODE_1 = "0111111111";
    private static final String MODEL_BARCODE_2 = "0122222222";

    private static final int SOME_PARAM_ID = 99999;
    private static final String SOME_PARAM_XSL_NAME = "SomeParam";

    private LoadBarCodesTool tool;

    @Mock
    private Yt yt;
    @Mock
    private JdbcTemplate yqlJdbcTemplate;
    @Mock
    private YtSessionService ytSessionService;
    @Mock
    private ModelStorageService modelStorageService;

    @Before
    public void setUp() {
        tool = new LoadBarCodesTool(
            yt, yqlJdbcTemplate, ytSessionService, modelStorageService, modelStorageService, "stub");
    }

    @Test
    public void whenNewBarCodeThenAddStrValueToBarCodeParameter() {
        ModelStorage.Model.Builder model = ModelStorage.Model.newBuilder()
            .setId(MODEL_ID)
            .setCategoryId(CATEGORY_ID)
            .addAllParameterValues(List.of(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(LoadBarCodesTool.BARCODE_XSL_NAME)
                    .setParamId(LoadBarCodesTool.BARCODE_PARAM_ID)
                    .setValueType(MboParameters.ValueType.STRING)
                    .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                    .addAllStrValue(List.of(
                        LocalizedStringUtils.defaultString(MODEL_BARCODE_1),
                        LocalizedStringUtils.defaultString(MODEL_BARCODE_2)))
                    .build(),
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(SOME_PARAM_XSL_NAME)
                    .setParamId(SOME_PARAM_ID)
                    .setValueType(MboParameters.ValueType.NUMERIC)
                    .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                    .setNumericValue("1")
                    .build()
                )
            );

        LoadBarCodesTool.BarCodeInfoWithIsGroupCategory barCodeInfo =
            new LoadBarCodesTool.BarCodeInfoWithIsGroupCategory(
                Collections.emptyList(), CATEGORY_ID, MODEL_ID, TOOL_BARCODE, true);

        boolean modelChanged = tool.createWriteBarCodeProcessor().apply(model).apply(barCodeInfo);

        ModelStorage.Model.Builder expectedModel = model.build().toBuilder()
            .clearParameterValues()
            .addAllParameterValues(List.of(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(LoadBarCodesTool.BARCODE_XSL_NAME)
                    .setParamId(LoadBarCodesTool.BARCODE_PARAM_ID)
                    .setValueType(MboParameters.ValueType.STRING)
                    .setValueSource(ModelStorage.ModificationSource.TOOL)
                    .addAllStrValue(List.of(
                        LocalizedStringUtils.defaultString(MODEL_BARCODE_1),
                        LocalizedStringUtils.defaultString(MODEL_BARCODE_2),
                        LocalizedStringUtils.defaultString(TOOL_BARCODE)))
                    .build(),
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(SOME_PARAM_XSL_NAME)
                    .setParamId(SOME_PARAM_ID)
                    .setValueType(MboParameters.ValueType.NUMERIC)
                    .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                    .setNumericValue("1")
                    .build()
                )
            );

        assertThat(modelChanged).isTrue();
        assertThat(model.getParameterValuesList())
            .containsExactlyInAnyOrderElementsOf(expectedModel.getParameterValuesList());
    }

    @Test
    public void whenBarCodeIsPresentThenDoNothing() {
        ModelStorage.Model.Builder model = ModelStorage.Model.newBuilder()
            .setId(MODEL_ID)
            .setCategoryId(CATEGORY_ID)
            .addAllParameterValues(List.of(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(LoadBarCodesTool.BARCODE_XSL_NAME)
                    .setParamId(LoadBarCodesTool.BARCODE_PARAM_ID)
                    .setValueType(MboParameters.ValueType.STRING)
                    .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                    .addAllStrValue(List.of(
                        LocalizedStringUtils.defaultString(MODEL_BARCODE_1),
                        LocalizedStringUtils.defaultString(MODEL_BARCODE_2),
                        LocalizedStringUtils.defaultString(TOOL_BARCODE)))
                    .build(),
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(SOME_PARAM_XSL_NAME)
                    .setParamId(SOME_PARAM_ID)
                    .setValueType(MboParameters.ValueType.NUMERIC)
                    .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                    .setNumericValue("1")
                    .build()
                )
            );

        List<ModelStorage.ParameterValue> origParameterValues = model.build().getParameterValuesList();

        LoadBarCodesTool.BarCodeInfoWithIsGroupCategory barCodeInfo =
            new LoadBarCodesTool.BarCodeInfoWithIsGroupCategory(
                Collections.emptyList(), CATEGORY_ID, MODEL_ID, TOOL_BARCODE, true);

        boolean modelChanged = tool.createWriteBarCodeProcessor().apply(model).apply(barCodeInfo);

        assertThat(modelChanged).isFalse();
        assertThat(model.getParameterValuesList())
            .containsExactlyInAnyOrderElementsOf(origParameterValues);
    }

    @Test
    public void whenAutoBarCodeIsPresentThenChangeModificationSourceToTool() {
        ModelStorage.Model.Builder model = ModelStorage.Model.newBuilder()
            .setId(MODEL_ID)
            .setCategoryId(CATEGORY_ID)
            .addAllParameterValues(List.of(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(LoadBarCodesTool.BARCODE_XSL_NAME)
                    .setParamId(LoadBarCodesTool.BARCODE_PARAM_ID)
                    .setValueType(MboParameters.ValueType.STRING)
                    .setValueSource(ModelStorage.ModificationSource.AUTO)
                    .addAllStrValue(List.of(
                        LocalizedStringUtils.defaultString(MODEL_BARCODE_1),
                        LocalizedStringUtils.defaultString(MODEL_BARCODE_2),
                        LocalizedStringUtils.defaultString(TOOL_BARCODE)))
                    .build(),
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(SOME_PARAM_XSL_NAME)
                    .setParamId(SOME_PARAM_ID)
                    .setValueType(MboParameters.ValueType.NUMERIC)
                    .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                    .setNumericValue("1")
                    .build()
                )
            );

        LoadBarCodesTool.BarCodeInfoWithIsGroupCategory barCodeInfo =
            new LoadBarCodesTool.BarCodeInfoWithIsGroupCategory(
                Collections.emptyList(), CATEGORY_ID, MODEL_ID, TOOL_BARCODE, true);

        boolean modelChanged = tool.createWriteBarCodeProcessor().apply(model).apply(barCodeInfo);

        ModelStorage.Model.Builder expectedModel = model.build().toBuilder()
            .clearParameterValues()
            .addAllParameterValues(List.of(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(LoadBarCodesTool.BARCODE_XSL_NAME)
                    .setParamId(LoadBarCodesTool.BARCODE_PARAM_ID)
                    .setValueType(MboParameters.ValueType.STRING)
                    .setValueSource(ModelStorage.ModificationSource.TOOL)
                    .addAllStrValue(List.of(
                        LocalizedStringUtils.defaultString(MODEL_BARCODE_1),
                        LocalizedStringUtils.defaultString(MODEL_BARCODE_2),
                        LocalizedStringUtils.defaultString(TOOL_BARCODE)))
                    .build(),
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(SOME_PARAM_XSL_NAME)
                    .setParamId(SOME_PARAM_ID)
                    .setValueType(MboParameters.ValueType.NUMERIC)
                    .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                    .setNumericValue("1")
                    .build()
                )
            );

        assertThat(modelChanged).isTrue();
        assertThat(model.getParameterValuesList())
            .containsExactlyInAnyOrderElementsOf(expectedModel.getParameterValuesList());
    }
}
