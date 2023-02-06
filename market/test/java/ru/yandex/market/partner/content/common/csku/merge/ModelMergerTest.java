package ru.yandex.market.partner.content.common.csku.merge;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.ModelGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModelMergerTest {

    private ModelStorage.Model baseModel;
    private ModelStorage.Model incomingModel;
    private CategoryData categoryData = mock(CategoryData.class);

    private static final long PARAM_ID_1 = 1L;
    private static final long PARAM_ID_2 = 2L;
    private static final long PARAM_ID_3 = 3L;
    private static final long PARAM_ID_4 = 4L;
    private static final long PARAM_ID_5 = 5L;
    private static final long PARAM_ID_6 = 6L;
    private static final long PARAM_ID_7 = 7L;
    private static final long PARAM_ID_8 = 8L;

    private static final String VALUE11 = "VALUE11";
    private static final String VALUE12 = "VALUE12";
    private static final String VALUE21 = "VALUE21";
    private static final String VALUE22 = "VALUE22";
    private static final String VALUE23 = "VALUE23";
    private static final String VALUE31 = "VALUE31";
    private static final String VALUE32 = "VALUE32";
    private static final String VALUE41 = "VALUE41";
    private static final String VALUE51 = "VALUE51";
    private static final String VALUE52 = "VALUE52";
    private static final String VALUE61 = "VALUE61";
    private static final String VALUE71 = "VALUE71";
    private static final String VALUE81 = "VALUE81";
    private static final String VALUE82 = "VALUE82";

    private static final long INCOMING_OWNER_ID = 1L;
    private static final long BASE_OWNER_ID = 2L;

    //Incoming
    private static final ModelStorage.ParameterValue pv11 = ModelGenerator.generateStringParam(
            PARAM_ID_1, String.valueOf(PARAM_ID_1), VALUE11, INCOMING_OWNER_ID,
            ModelStorage.ModificationSource.OPERATOR_FILLED);
    private static final ModelStorage.ParameterValue pv21 = ModelGenerator.generateStringParam(
            PARAM_ID_2, String.valueOf(PARAM_ID_2), VALUE21, INCOMING_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);
    private static final ModelStorage.ParameterValue pv22 = ModelGenerator.generateStringParam(
            PARAM_ID_2, String.valueOf(PARAM_ID_2), VALUE22, INCOMING_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);
    private static final ModelStorage.ParameterValue pv31 = ModelGenerator.generateStringParam(
            PARAM_ID_3, String.valueOf(PARAM_ID_3), VALUE31, INCOMING_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);
    private static final ModelStorage.ParameterValue pv51 = ModelGenerator.generateStringParam(
            PARAM_ID_5, String.valueOf(PARAM_ID_2), VALUE51, INCOMING_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);
    private static final ModelStorage.ParameterValue pv71 = ModelGenerator.generateStringParam(
            PARAM_ID_7, String.valueOf(PARAM_ID_7), VALUE71, INCOMING_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);
    private static final ModelStorage.ParameterValue pv81 = ModelGenerator.generateStringParam(
            PARAM_ID_8, String.valueOf(PARAM_ID_8), VALUE81, INCOMING_OWNER_ID,
            ModelStorage.ModificationSource.FORMALIZATION, 10);

    //Base
    private static final ModelStorage.ParameterValue pv12 = ModelGenerator.generateStringParam(
            PARAM_ID_1, String.valueOf(PARAM_ID_1), VALUE12, BASE_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);
    private static final ModelStorage.ParameterValue pv211 = ModelGenerator.generateStringParam(
            PARAM_ID_2, String.valueOf(PARAM_ID_2), VALUE21, BASE_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);
    private static final ModelStorage.ParameterValue pv23 = ModelGenerator.generateStringParam(
            PARAM_ID_2, String.valueOf(PARAM_ID_2), VALUE23, BASE_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);
    private static final ModelStorage.ParameterValue pv32 = ModelGenerator.generateStringParam(
            PARAM_ID_3, String.valueOf(PARAM_ID_3), VALUE32, BASE_OWNER_ID,
            ModelStorage.ModificationSource.OPERATOR_FILLED);
    private static final ModelStorage.ParameterValue pv41 = ModelGenerator.generateStringParam(
            PARAM_ID_4, String.valueOf(PARAM_ID_4), VALUE41, BASE_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);
    private static final ModelStorage.ParameterValue pv52 = ModelGenerator.generateStringParam(
            PARAM_ID_5, String.valueOf(PARAM_ID_5), VALUE52, BASE_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);
    private static final ModelStorage.ParameterValue pv61 = ModelGenerator.generateStringParam(
            PARAM_ID_6, String.valueOf(PARAM_ID_6), VALUE61, BASE_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);
    private static final ModelStorage.ParameterValue pv82 = ModelGenerator.generateStringParam(
            PARAM_ID_8, String.valueOf(PARAM_ID_8), VALUE82, BASE_OWNER_ID,
            ModelStorage.ModificationSource.VENDOR_OFFICE);

    @Before
    public void setup() {
        when(categoryData.getParamById(anyLong())).thenAnswer(new Answer<MboParameters.Parameter>() {
            @Override
            public MboParameters.Parameter answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Long id = (Long) args[0];
                return MboParameters.Parameter.newBuilder()
                        .setId(id)
                        .build();
            }
        });
        when(categoryData.getParamById(PARAM_ID_2))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setMultivalue(true)
                        .build()
                );

        when(categoryData.getParamById(PARAM_ID_5))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setMultivalue(true)
                        .build()
                );
        when(categoryData.getParamById(PARAM_ID_7))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setService(true)
                        .build()
                );
    }

    @Test
    public void whenOperatorFilledAndMultiValueParamsThenNoConflict() {
        incomingModel = createIncomingModel();
        baseModel = createBaseModel();
        ModelMerger modelMerger = new ModelMerger(baseModel, categoryData);
        ModelStorage.Model mergedModel = modelMerger.addModel(incomingModel).merge();

        List<ModelStorage.ParameterValue> valuesList = mergedModel.getParameterValuesList();
        assertThat(valuesList).containsExactlyInAnyOrder(pv11, pv211, pv22, pv23, pv32, pv41, pv51, pv61, pv71);
        assertThat(mergedModel.getParameterValuesMetadataList()).hasSize(1);
    }

    @Test
    public void whenOperatorFilledAndMultiValueParamsThenNoConflictEvenWithFormalization() {
        // pv11, pv21, pv22, pv31, pv51, pv81
        incomingModel = createIncomingModelWithFormalization();
        // pv12, pv211, pv23, pv32, pv41, pv52, pv61, pv82
        baseModel = createBaseModelWithFormalization();
        ModelMerger modelMerger = new ModelMerger(baseModel, categoryData);
        ModelStorage.Model mergedModel = modelMerger.addModel(incomingModel).merge();

        List<ModelStorage.ParameterValue> valuesList = mergedModel.getParameterValuesList();
        assertThat(valuesList).containsExactlyInAnyOrder(pv11, pv211, pv22, pv23, pv32, pv41, pv51, pv61, pv82);
    }

    @Test
    public void correctlyMergeMetaParameters() {
        ModelStorage.Model baseModel = ModelGenerator.generateModelBuilder(Collections.emptyList())
                .addParameterValuesMetadata(ModelStorage.ParameterValueMetadata.newBuilder()
                        .setParamId(PARAM_ID_5)
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build())
                .build();
        ModelStorage.Model otherModel = ModelGenerator.generateModelBuilder(Collections.emptyList())
                .addParameterValuesMetadata(ModelStorage.ParameterValueMetadata.newBuilder()
                        .setParamId(PARAM_ID_5)
                        .setValueSource(ModelStorage.ModificationSource.TOOL)
                        .build())
                .build();
        ModelStorage.Model otherModel2 = ModelGenerator.generateModelBuilder(Collections.emptyList())
                .addParameterValuesMetadata(ModelStorage.ParameterValueMetadata.newBuilder()
                        .setParamId(PARAM_ID_5)
                        .setValueSource(ModelStorage.ModificationSource.BACKEND_RULE)
                        .build())
                .build();

        ModelMerger modelMerger = new ModelMerger(baseModel, categoryData);
        ModelStorage.Model mergedModel = modelMerger
                .addModel(otherModel)
                .addModel(otherModel2)
                .merge();

        List<ModelStorage.ParameterValueMetadata> parameterValuesMetadataList =
                mergedModel.getParameterValuesMetadataList();

        //в результирующей модели не возникает дублей, по возможности оставляем параметр из базовой модели
        assertThat(parameterValuesMetadataList).hasSize(1);
        assertThat(parameterValuesMetadataList).extracting(ModelStorage.ParameterValueMetadata::getValueSource)
                .containsOnly(ModelStorage.ModificationSource.OPERATOR_FILLED);
    }

    @Test
    public void correctlyMergeMetaParametersWithHigherPriority() {
        ModelStorage.Model baseModel = ModelGenerator.generateModelBuilder(Collections.emptyList())
                .addParameterValuesMetadata(ModelStorage.ParameterValueMetadata.newBuilder()
                        .setParamId(PARAM_ID_5)
                        .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                        .build())
                .build();
        ModelStorage.Model otherModel = ModelGenerator.generateModelBuilder(Collections.emptyList())
                .addParameterValuesMetadata(ModelStorage.ParameterValueMetadata.newBuilder()
                        .setParamId(PARAM_ID_5)
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build())
                .build();

        ModelMerger modelMerger = new ModelMerger(baseModel, categoryData);
        ModelStorage.Model mergedModel = modelMerger
                .addModel(otherModel)
                .merge();

        List<ModelStorage.ParameterValueMetadata> parameterValuesMetadataList =
                mergedModel.getParameterValuesMetadataList();

        //в результирующей модели не возникает дублей, по возможности оставляем параметр из базовой модели
        assertThat(parameterValuesMetadataList).hasSize(1);
        assertThat(parameterValuesMetadataList).extracting(ModelStorage.ParameterValueMetadata::getValueSource)
                .containsOnly(ModelStorage.ModificationSource.OPERATOR_FILLED);
    }

    public ModelStorage.Model createIncomingModel() {

        List<ModelStorage.ParameterValue> incomingParams = Arrays.asList(pv11, pv21, pv22, pv31, pv51, pv71);

        return ModelGenerator.generateModelBuilder(incomingParams)
                .addParameterValuesMetadata(ModelStorage.ParameterValueMetadata.newBuilder()
                        .setParamId(PARAM_ID_5)
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build())
                .build();
    }

    public ModelStorage.Model createBaseModel() {
        List<ModelStorage.ParameterValue> incomingParams = Arrays.asList(pv12, pv211, pv23, pv32, pv41, pv52, pv61);

        return ModelGenerator.generateModelBuilder(incomingParams).build();
    }

    public ModelStorage.Model createIncomingModelWithFormalization() {

        List<ModelStorage.ParameterValue> incomingParams = Arrays.asList(pv11, pv21, pv22, pv31, pv51, pv81);

        return ModelGenerator.generateModelBuilder(incomingParams)
                .addParameterValuesMetadata(ModelStorage.ParameterValueMetadata.newBuilder()
                        .setParamId(PARAM_ID_5)
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                )
                .addParameterValuesMetadata(ModelStorage.ParameterValueMetadata.newBuilder()
                        .setParamId(PARAM_ID_8)
                        .setValueSource(ModelStorage.ModificationSource.FORMALIZATION)
                )
                .build();
    }

    public ModelStorage.Model createBaseModelWithFormalization() {
        List<ModelStorage.ParameterValue> incomingParams = Arrays.asList(pv12, pv211, pv23, pv32, pv41, pv52, pv61,
                pv82);

        return ModelGenerator.generateModelBuilder(incomingParams)
                .build();
    }
}
