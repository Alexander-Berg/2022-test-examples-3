package ru.yandex.market.mbo.tms.report;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.mbo.configs.yt.YtPoolConfig;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.SubType;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.mbo.utils.WordProtoUtils;

import static java.lang.Math.abs;
import static java.lang.Math.max;

@SuppressWarnings("checkstyle:MagicNumber")
public class ReplacementOfHypothesesExecutorTest {

    private static final Logger log = LogManager.getLogger();

    private static final long TEST_MODEL_CATEGORY_ID = 90549;
    private static final long TEST_MODEL_ID = 257845000;

    private static final Long SIZE_PARAM_ID = 234L;
    private static final Long SIZE_PARAM_OPTION = 987L;
    private static final String SIZE_PARAM_OPTION_NAME = "XS";
    private static final String SIZE_PARAM_XSL_NAME = "size_param";

    private ReplacementOfHypothesesExecutor replacementOfHypothesesExecutor;
    private ModelStorageServiceStub modelStorageServiceStub;
    private final ArrayList<Long> paramsIds = new ArrayList<>();

    @Before
    public void setup() {
        modelStorageServiceStub = new MyModelStorageServiceStub();

        CategoryEntities categoryEntities = new CategoryEntities(90549, Collections.singletonList(0L));

        CategoryParam param1 = new Parameter();
        param1.addOption(OptionBuilder.newBuilder()
            .setId(12104873)
            .addName("мультибитный")
            .build());
        param1.setId(4897629);
        param1.setXslName("DACType");
        param1.setCategoryHid(TEST_MODEL_CATEGORY_ID);
        param1.setHyperId(4897629);

        CategoryParam param2 = new Parameter();
        param2.setId(4897643);
        param2.setXslName("PowerSupply");
        param2.setCategoryHid(TEST_MODEL_CATEGORY_ID);
        param2.setHyperId(4897643);

        CategoryParam sizeParameter = new Parameter();
        sizeParameter.addOption(OptionBuilder.newBuilder()
            .setId(SIZE_PARAM_OPTION)
            .addName(SIZE_PARAM_OPTION_NAME)
            .build()
        );
        sizeParameter.setId(SIZE_PARAM_ID);
        sizeParameter.setXslName(SIZE_PARAM_XSL_NAME);
        sizeParameter.setCategoryHid(TEST_MODEL_CATEGORY_ID);
        sizeParameter.setHyperId(SIZE_PARAM_ID);
        sizeParameter.setSubtype(SubType.SIZE);

        categoryEntities.addParameter(param1);
        categoryEntities.addParameter(param2);
        categoryEntities.addParameter(sizeParameter);

        paramsIds.add(4897629L);
        paramsIds.add(4897643L);
        paramsIds.add(SIZE_PARAM_ID);

        IParameterLoaderService parameterLoader = new ParameterLoaderServiceStub(categoryEntities);

        YtPoolConfig ytPoolConfig = Mockito.mock(YtPoolConfig.class);
        Mockito.when(ytPoolConfig.commonYqlJdbcTemplate()).thenReturn(new JdbcTemplate() {
            @Override
            public void query(String sql, RowCallbackHandler rch) throws DataAccessException {

                ResultSet rs = Mockito.mock(ResultSet.class);

                try {
                    Mockito.when(rs.getString("category_id"))
                        .thenReturn(String.valueOf(TEST_MODEL_CATEGORY_ID));
                    Mockito.when(rs.getString("model_id")).thenReturn(String.valueOf(TEST_MODEL_ID));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }

                try {
                    rch.processRow(rs);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }

            }
        });

        replacementOfHypothesesExecutor = new ReplacementOfHypothesesExecutor(
                parameterLoader,
                Mockito.mock(Yt.class),
                Mockito.mock(YtReportReplacementOfHypotheses.class),
                ytPoolConfig,
                modelStorageServiceStub,
                "",
                "");

    }

    @Test
    public void generateTest() throws Exception {

        replacementOfHypothesesExecutor.doRealJob(null);

        // check parameterValues

        ModelStorage.Model modelAfter = modelStorageServiceStub.findModels(ModelStorage.FindModelsRequest.newBuilder()
            .addModelIds(TEST_MODEL_ID)
            .build()).getModels(0);

        log.debug(modelAfter.getParameterValuesList());
        log.debug(modelAfter.getParameterValueHypothesisList());

        List<ModelStorage.ParameterValue> parameterValuesAfter = modelAfter.getParameterValuesList().stream()
            .filter(param -> paramsIds.contains(param.getParamId())).collect(Collectors.toList());
        List<ModelStorage.ParameterValue> trueParameterValuesAfter = trueParameterValuesAfter();

        Comparator<ModelStorage.ParameterValue> comparatorParameterValues = (o1, o2) -> {
            if (o1.getParamId() == o2.getParamId()) {
                return o1.getOptionId() - o2.getOptionId();
            }
            long ret = o1.getParamId() - o2.getParamId();
            return (int) (ret / max(1, abs(ret)));
        };

        parameterValuesAfter.sort(comparatorParameterValues);
        trueParameterValuesAfter.sort(comparatorParameterValues);

        // comparison parameterValues

        assert (parameterValuesAfter.size() == trueParameterValuesAfter.size());

        for (int i = 0; i < trueParameterValuesAfter.size(); i++) {

            ModelStorage.ParameterValue model1 = trueParameterValuesAfter.get(i);
            ModelStorage.ParameterValue model2 = parameterValuesAfter.get(i);

            assert (model1.getParamId() == model2.getParamId());
            assert (model1.getOptionId() == model2.getOptionId());
            assert (model1.getXslName().equals(model2.getXslName()));
            assert (model1.getValueType() == model2.getValueType());

        }

        // check parameterValueHypotheses

        List<ModelStorage.ParameterValueHypothesis> parameterValueHypothesesAfter = new ArrayList<>(modelAfter
            .getParameterValueHypothesisList());
        List<ModelStorage.ParameterValueHypothesis> trueHypothesisAfter = trueHypothesisAfter();

        Comparator<ModelStorage.ParameterValueHypothesis> comparatorParameterValueHypothesis = (o1, o2) -> {
            if (o1.getParamId() == o2.getParamId()) {
                if (o1.getStrValueCount() == o2.getStrValueCount()) {
                    String model1 = o1.getStrValueList().stream().map(MboParameters.Word::getName)
                        .collect(Collectors.joining());
                    String model2 = o2.getStrValueList().stream().map(MboParameters.Word::getName)
                        .collect(Collectors.joining());
                    return model1.compareTo(model2);
                }
                return o1.getStrValueCount() - o2.getStrValueCount();
            }
            long ret = o1.getParamId() - o2.getParamId();
            return (int) (ret / max(1, abs(ret)));
        };

        parameterValueHypothesesAfter.sort(comparatorParameterValueHypothesis);
        trueHypothesisAfter.sort(comparatorParameterValueHypothesis);

        // comparison parameterValueHypotheses

        assert (parameterValueHypothesesAfter.size() == trueHypothesisAfter.size());

        for (int i = 0; i < parameterValueHypothesesAfter.size(); i++) {
            ModelStorage.ParameterValueHypothesis model1 = trueHypothesisAfter.get(i);
            ModelStorage.ParameterValueHypothesis model2 = parameterValueHypothesesAfter.get(i);

            assert (model1.getParamId() == model2.getParamId());
            assert (model1.getStrValueCount() == model2.getStrValueCount());
            assert (model1.getXslName().equals(model2.getXslName()));
            assert (model1.getValueType() == model2.getValueType());

            String model1S = model1.getStrValueList().stream().map(MboParameters.Word::getName)
                .collect(Collectors.joining());
            String model2S = model2.getStrValueList().stream().map(MboParameters.Word::getName)
                .collect(Collectors.joining());

            assert (model1S.equals(model2S));
        }

    }

    @Test
    public void checkChangeHypothesisPicker() throws Exception {

        ModelStorage.Model model = modelStorageServiceStub.findModels(ModelStorage.FindModelsRequest.newBuilder()
            .addModelIds(TEST_MODEL_ID)
            .build()).getModels(0);

        assert (model.getParameterValueLinksList().get(0).getValueType() == MboParameters.ValueType.HYPOTHESIS);
        assert (!model.getParameterValueLinksList().get(0).hasOptionId());
        assert (model.getParameterValueLinksList().get(0).getHypothesisValueList().size() == 1);

        replacementOfHypothesesExecutor.doRealJob(null);

        model = modelStorageServiceStub.findModels(ModelStorage.FindModelsRequest.newBuilder()
            .addModelIds(TEST_MODEL_ID)
            .build()).getModels(0);

        assert (model.getParameterValueLinksList().size() == 1);
        assert (model.getParameterValueLinksList().get(0).getValueType() == MboParameters.ValueType.ENUM);
        assert (model.getParameterValueLinksList().get(0).getOptionId() == 12104873);
        assert (model.getParameterValueLinksList().get(0).getHypothesisValueList().size() == 0);

    }

    @Test
    public void testNotReplaceSizeParameterHypothesis() throws Exception {
        replacementOfHypothesesExecutor.doRealJob(null);

        ModelStorage.Model modelAfter = modelStorageServiceStub.findModels(ModelStorage.FindModelsRequest.newBuilder()
            .addModelIds(TEST_MODEL_ID)
            .build()).getModels(0);

        Optional<ModelStorage.ParameterValueHypothesis> sizeHypothesisParam =
            modelAfter.getParameterValueHypothesisList()
            .stream()
            .filter(p -> p.getParamId() == SIZE_PARAM_ID)
            .findAny();

        Assert.assertTrue(sizeHypothesisParam.isPresent());
        Assert.assertEquals(SIZE_PARAM_OPTION_NAME, sizeHypothesisParam.get().getStrValue(0).getName());

        Assert.assertTrue(modelAfter.getParameterValuesList()
            .stream()
            .noneMatch(p -> p.getParamId() == SIZE_PARAM_ID)
        );
    }

    private List<ModelStorage.ParameterValueHypothesis> generateHypothesis() {

        List<ModelStorage.ParameterValueHypothesis> hypotheses = new ArrayList<>();

        hypotheses.add(ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(4897629)
            .setValueType(MboParameters.ValueType.ENUM)
            .setXslName("DACType")
            .addStrValue(WordProtoUtils.defaultWord("мультибитный"))
            .setUserId(274787729)
            .build());

        hypotheses.add(ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(4897643)
            .setValueType(MboParameters.ValueType.ENUM)
            .setXslName("PowerSupply")
            .addStrValue(WordProtoUtils.defaultWord("beliberda"))
            .setUserId(274787729)
            .build());

        hypotheses.add(ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(SIZE_PARAM_ID)
            .setValueType(MboParameters.ValueType.ENUM)
            .setXslName(SIZE_PARAM_XSL_NAME)
            .addStrValue(WordProtoUtils.defaultWord(SIZE_PARAM_OPTION_NAME))
            .setUserId(274787729)
            .build());

        return hypotheses;

    }

    private List<ModelStorage.ParameterValueHypothesis> trueHypothesisAfter() {
        return Stream.of(
            ModelStorage.ParameterValueHypothesis.newBuilder()
                .setParamId(4897643)
                .setValueType(MboParameters.ValueType.ENUM)
                .setXslName("PowerSupply")
                .addStrValue(WordProtoUtils.defaultWord("beliberda"))
                .setUserId(274787729)
                .build(),
            ModelStorage.ParameterValueHypothesis.newBuilder()
                .setParamId(SIZE_PARAM_ID)
                .setValueType(MboParameters.ValueType.ENUM)
                .setXslName(SIZE_PARAM_XSL_NAME)
                .addStrValue(WordProtoUtils.defaultWord(SIZE_PARAM_OPTION_NAME))
                .setUserId(274787729)
                .build()).collect(Collectors.toList()
        );
    }

    private List<ModelStorage.ParameterValue> trueParameterValuesAfter() {
        List<ModelStorage.ParameterValue> parameters = new ArrayList<>();
        parameters.add(ModelStorage.ParameterValue.newBuilder()
            .setParamId(4897629)
            .setUserId(274787729)
            .setTypeId(1)
            .setOptionId(12104873)
            .setXslName("DACType")
            .setValueType(MboParameters.ValueType.ENUM)
            .build());

        return parameters;

    }

    class MyModelStorageServiceStub extends ModelStorageServiceStub {

        private ModelStorage.Model model;

        MyModelStorageServiceStub() {
            model = ModelStorage.Model.newBuilder()
                .setId(TEST_MODEL_ID)
                .setCategoryId(TEST_MODEL_CATEGORY_ID)
                .addAllParameterValueHypothesis(generateHypothesis())
                .addParameterValueLinks(ModelStorage.ParameterValue.newBuilder()
                    .addHypothesisValue(WordProtoUtils.defaultWord("мультибитный"))
                    .setValueType(MboParameters.ValueType.HYPOTHESIS)
                    .setParamId(4897629)
                    .setXslName("DACType")
                    .build())
                .build();
        }

        @Override
        public ModelStorage.GetModelsResponse getModels(ModelStorage.GetModelsRequest getModelsRequest) {
            if (getModelsRequest.getModelIdsList().contains(model.getId())) {
                return ModelStorage.GetModelsResponse.newBuilder()
                    .addModels(model).build();
            }
            return null;
        }

        @Override
        public ModelStorage.GetModelsResponse findModels(ModelStorage.FindModelsRequest findModelsRequest) {
            if (findModelsRequest.getModelIdsList().contains(model.getId())) {
                return ModelStorage.GetModelsResponse.newBuilder().addModels(model).build();
            }
            return null;
        }

        @Override
        public ModelStorage.OperationResponse saveModels(ModelStorage.SaveModelsRequest saveModelsRequest) {

            if (saveModelsRequest.getModels(0).getId() == TEST_MODEL_ID) {
                model = model.toBuilder().clearParameterValueHypothesis().clearParameterValues()
                    .addAllParameterValueHypothesis(saveModelsRequest.getModels(0)
                        .getParameterValueHypothesisList())
                    .addAllParameterValues(saveModelsRequest.getModels(0).getParameterValuesList())
                    .clearParameterValueLinks()
                    .addAllParameterValueLinks(saveModelsRequest.getModels(0).getParameterValueLinksList())
                    .build();
            }

            return ModelStorage.OperationResponse.newBuilder()
                    .addStatuses(ModelStorage.OperationStatus.newBuilder()
                            .setModelId(model.getId())
                            .setStatus(ModelStorage.OperationStatusType.OK)
                            .setModel(model)
                            .setType(ModelStorage.OperationType.CHANGE)
                            .build()).build();
        }

    }

}
