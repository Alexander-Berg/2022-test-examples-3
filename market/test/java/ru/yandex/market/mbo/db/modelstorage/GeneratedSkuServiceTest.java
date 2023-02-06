package ru.yandex.market.mbo.db.modelstorage;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import ru.yandex.market.mbo.db.ConverterUtils;
import ru.yandex.market.mbo.db.modelstorage.data.GroupOperationStatusException;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.merge.ModelMergeServiceImpl;
import ru.yandex.market.mbo.db.modelstorage.params.ModelParamsService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStoreInterfaceStub;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.params.ParameterProtoConverter;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersConfig;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.ThinCategoryParam;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterValueBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.ParameterValueMatchers.paramOption;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 19.01.2018
 */
@SuppressWarnings({"unchecked", "checkstyle:magicnumber"})
@RunWith(MockitoJUnitRunner.class)
public class GeneratedSkuServiceTest {

    private StatsModelStorageServiceStub modelStorageService;

    @Mock
    private CategoryParametersServiceClient parametersServiceClient;

    private GeneratedSkuService skuService;

    private static final int INITIAL_VALUE = 9123;
    private static final AtomicInteger ID_HOLDER = new AtomicInteger(INITIAL_VALUE);

    private static final long CATEGORY_ID = uniqId();
    private static final long GURU_MODEL_ID = uniqId();
    private static final long GENERATED_MODEL_ID = uniqId();
    private static final long SKU_MODEL_ID_1 = uniqId();
    private static final long GENERATED_SKU_MODEL_ID_1 = uniqId();
    private static final long GENERATED_SKU_MODEL_ID_2 = uniqId();
    private static final long GENERATED_SKU_MODEL_ID_3 = uniqId();
    private static final long USER_ID = uniqId();

    private static final long PARAM_SKU_DEFINING_ID = uniqId();
    private static final long PARAM_SKU_NONE_ID = uniqId();
    private static final long PARAM_SKU_INFORMATIONAL = uniqId();
    private static final long PARAM_SKU_DEFINING_NUMERIC_ID = uniqId();
    private static final long PARAM_NUMERIC_ID = uniqId();

    private static final ModelSaveContext CONTEXT = new ModelSaveContext(USER_ID);
    private static final GeneratedSkuService.GeneratedSkuSyncContext SYNC_CONTEXT_VENDOR =
        new GeneratedSkuService.GeneratedSkuSyncContext()
            .setConfirmValues(true)
            .setMergePictures(true);

    private List<MboParameters.Parameter> protoParameters;
    private List<ThinCategoryParam> parameters;

    @Before
    public void before() {
        modelStorageService = spy(new StatsModelStorageServiceStub());
        ModelStoreInterfaceStub modelStore = new ModelStoreInterfaceStub(modelStorageService);

        protoParameters = new ArrayList<>();
        parameters = new ArrayList<>();
        ModelMergeServiceImpl mergeService = new ModelMergeServiceImpl(new ModelParamsService(), modelStore);

        skuService = new GeneratedSkuService();
        skuService.setMergeService(mergeService);
        skuService.setStorageService(modelStorageService);
        skuService.setParametersServiceClient(parametersServiceClient);

        // Answer user as protoParameters fields initializes after before method
        when(parametersServiceClient.getCategoryParameters(CATEGORY_ID)).thenAnswer((Answer<CategoryParametersConfig>)
            invocation -> new CategoryParametersConfig(
                MboParameters.Category.newBuilder()
                    .setHid(CATEGORY_ID)
                    .addAllParameter(protoParameters)
                    .build()));

        ParametersBuilder
                .startParameters(parametersCollector())
                    .startParameter()
                        .id(PARAM_SKU_DEFINING_ID)
                        .type(Param.Type.ENUM)
                        .xsl("param-sku-defining")
                        .option(1, "param-sku-defining-option-1",
                            Collections.singleton("param-sku-defining-option-1-alias")
                        )
                        .option(2, "param-sku-defining-option-2")
                        .skuParameterMode(SkuParameterMode.SKU_DEFINING)
                        .extractInSkubd(true)
                    .endParameter()
                    .startParameter()
                        .id(PARAM_SKU_NONE_ID)
                        .type(Param.Type.ENUM)
                        .xsl("param-sku-none")
                        .option(1, "param-sku-none-option-1")
                        .option(2, "param-sku-none-option-2")
                        .skuParameterMode(SkuParameterMode.SKU_NONE)
                        .extractInSkubd(true)
                    .endParameter()
                    .startParameter()
                        .id(PARAM_SKU_INFORMATIONAL)
                        .type(Param.Type.ENUM)
                        .xsl("param-sku-informational")
                        .option(1, "param-sku-informational-option-1")
                        .option(2, "param-sku-informational-option-2")
                        .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
                        .extractInSkubd(true)
                    .endParameter()
                    .startParameter()
                        .id(PARAM_SKU_DEFINING_NUMERIC_ID)
                        .type(Param.Type.NUMERIC)
                        .xsl("param-sku-defining-numeric")
                        .skuParameterMode(SkuParameterMode.SKU_DEFINING)
                        .extractInSkubd(true)
                    .endParameter()
                    .startParameter()
                        .id(PARAM_NUMERIC_ID)
                        .type(Param.Type.NUMERIC)
                        .xsl("param-numeric")
                    .endParameter()
                .endParameters();
    }

    private CommonModelBuilder buildModel(long id, CommonModel.Source type) {
        return buildModel(id, type, parameters);
    }

    private CommonModelBuilder buildModel(long id,
                                          CommonModel.Source type,
                                          Collection<ThinCategoryParam> params) {
        return CommonModelBuilder.newBuilder()
            .parameters(params)
            .id(id)
            .currentType(type)
            .source(type)
            .category(CATEGORY_ID);
    }

    @Test
    public void createNew() {
        CommonModel guru = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU).getModel();

        CommonModel generated = buildModel(GENERATED_MODEL_ID, CommonModel.Source.GENERATED)
            .modelRelation(GENERATED_SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel generatedSku = buildModel(GENERATED_SKU_MODEL_ID_1, CommonModel.Source.GENERATED_SKU)
            .param("param-sku-defining")
            .modificationSource(ModificationSource.AUTO)
            .setOption(2)
            .modelRelation(GENERATED_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        modelStorageService.initializeWithModels(guru, generated, generatedSku);

        GroupOperationStatus result = skuService.createOrUpdateSku(generated, guru, CONTEXT, SYNC_CONTEXT_VENDOR);


        assertThat(result.isOk(), is(true));
        assertThat(modelStorageService.getAllModels(), hasSize(4));

        List<CommonModel> skuList = loadModelsOfType(CommonModel.Source.SKU);
        assertThat(skuList, hasSize(1));

        CommonModel sku = skuList.get(0);
        assertThat(sku.isPublished(), is(true));
        assertThat(sku.getParameterValues(), hasSize(1));
        assertThat(sku.getParameterValues("param-sku-defining"), is(notNullValue()));
        assertThat(sku.getSingleParameterValue("param-sku-defining").getOptionId(), is(2L));

        List<CommonModel> generatedSkuList = loadModelsOfType(CommonModel.Source.GENERATED_SKU);
        assertThat(generatedSkuList, hasSize(1));

        assertRelatedAsSourceToSource(generatedSkuList.get(0), skuList.get(0));
    }

    private static void assertRelatedAsSourceToSource(CommonModel source, CommonModel target) {
        Assertions.assertThat(target.getRelations(ModelRelation.RelationType.SYNC_SOURCE))
            .containsOnly(
                new ModelRelation(source.getId(), source.getCategoryId(), ModelRelation.RelationType.SYNC_SOURCE)
            );

        Assertions.assertThat(source.getRelations(ModelRelation.RelationType.SYNC_TARGET))
            .containsOnly(
                new ModelRelation(target.getId(), target.getCategoryId(), ModelRelation.RelationType.SYNC_TARGET)
            );
    }

    @Test
    public void remoteValidationError() {
        CommonModel guru = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU).getModel();

        CommonModel generated = buildModel(GENERATED_MODEL_ID, CommonModel.Source.GENERATED)
            .modelRelation(GENERATED_SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel generatedSku = buildModel(GENERATED_SKU_MODEL_ID_1, CommonModel.Source.GENERATED_SKU)
            .param("param-sku-defining")
            .modificationSource(ModificationSource.AUTO)
            .setOption(2)
            .modelRelation(GENERATED_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        modelStorageService.initializeWithModels(guru, generated, generatedSku);

        // prepare validation error
        ModelValidationError error = new ModelValidationError(GENERATED_SKU_MODEL_ID_1,
            ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE,
            ModelValidationError.ErrorSubtype.MISSING_VALUE);
        error.addParam(ModelStorage.ErrorParamName.PARAMETER_NAME, "param-sku-defining");

        OperationStatus status = new OperationStatus(OperationStatusType.VALIDATION_ERROR, OperationType.CREATE,
            GENERATED_SKU_MODEL_ID_1);
        status.addValidationErrors(Collections.singleton(error));
        GroupOperationStatus saveFailure = new GroupOperationStatus(status);

        doReturn(saveFailure)
            .when(modelStorageService)
            .saveModels(ArgumentMatchers.anyCollection(), ArgumentMatchers.any(ModelSaveContext.class));

        GroupOperationStatus result = skuService.createOrUpdateSku(generated, guru, CONTEXT, SYNC_CONTEXT_VENDOR);
        Assertions.assertThat(result.isOk()).isFalse();
        Assertions.assertThat(result.getStatusMessage())
            .isEqualTo("Validation error occurred");
    }

    @Test
    public void addInformationParameters() {
        CommonModel guru = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU)
            .modelRelation(SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel existSku = buildModel(SKU_MODEL_ID_1, CommonModel.Source.SKU)
            .param("param-sku-defining")
            .setOption(1)
            .modificationSource(ModificationSource.AUTO)
            .modelRelation(GURU_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        CommonModel generated = buildModel(GENERATED_MODEL_ID, CommonModel.Source.GENERATED)
            .modelRelation(GENERATED_SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel generatedSku = buildModel(GENERATED_SKU_MODEL_ID_1, CommonModel.Source.GENERATED_SKU)
            .param("param-sku-defining")
            .setOption(1)
            .modificationSource(ModificationSource.AUTO)
            .param("param-sku-informational")
            .setOption(2)
            .modificationSource(ModificationSource.AUTO)
            .modelRelation(GENERATED_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        modelStorageService.initializeWithModels(guru, existSku, generated, generatedSku);


        GroupOperationStatus result = skuService.createOrUpdateSku(generated, guru, CONTEXT, SYNC_CONTEXT_VENDOR);


        assertThat(result.isOk(), is(true));
        assertThat(modelStorageService.getAllModels(), hasSize(4));

        List<CommonModel> skuList = loadModelsOfType(CommonModel.Source.SKU);
        assertThat(skuList, hasSize(1));

        CommonModel sku = skuList.get(0);
        assertThat(sku.getId(), is(SKU_MODEL_ID_1));
        assertThat(sku.isPublished(), is(true));
        Collection<ParameterValue> flatParameterValues = sku.getFlatParameterValues();
        assertThat(flatParameterValues,
            containsInAnyOrder(
                paramOption("param-sku-defining", 1),
                paramOption("param-sku-informational", 2)
            )
        );
    }

    @Test
    public void replaceExistInformationalValue() {
        CommonModel guru = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU)
            .modelRelation(SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel existSku = buildModel(SKU_MODEL_ID_1, CommonModel.Source.SKU)
            .param("param-sku-defining")
            .setOption(1)
            .modificationSource(ModificationSource.AUTO)
            .param("param-sku-informational")
            .setOption(1)
            .modificationSource(ModificationSource.AUTO)
            .modelRelation(GURU_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        CommonModel generated = buildModel(GENERATED_MODEL_ID, CommonModel.Source.GENERATED)
            .modelRelation(GENERATED_SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel generatedSku = buildModel(GENERATED_SKU_MODEL_ID_1, CommonModel.Source.GENERATED_SKU)
            .param("param-sku-defining")
            .setOption(1)
            .modificationSource(ModificationSource.AUTO)
            .param("param-sku-informational")
            .setOption(2)
            .modificationSource(ModificationSource.AUTO)
            .modelRelation(GENERATED_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        modelStorageService.initializeWithModels(guru, existSku, generated, generatedSku);


        GroupOperationStatus result = skuService.createOrUpdateSku(generated, guru, CONTEXT, SYNC_CONTEXT_VENDOR);


        assertThat(result.isOk(), is(true));
        assertThat(modelStorageService.getAllModels(), hasSize(4));

        List<CommonModel> skuList = loadModelsOfType(CommonModel.Source.SKU);
        assertThat(skuList, hasSize(1));

        CommonModel sku = skuList.get(0);
        assertThat(sku.getId(), is(SKU_MODEL_ID_1));
        assertThat(sku.isPublished(), is(true));
        assertThat(sku.getFlatParameterValues(),
            containsInAnyOrder(
                paramOption("param-sku-defining", 1),
                paramOption("param-sku-informational", 2)
            )
        );
    }


    @Test
    public void removeExistsInformationalValue() {
        CommonModel guru = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU)
            .modelRelation(SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel existSku = buildModel(SKU_MODEL_ID_1, CommonModel.Source.SKU)
            .param("param-sku-defining")
            .setOption(1)
            .modificationSource(ModificationSource.AUTO)
            .param("param-sku-informational")
            .setOption(2)
            .modificationSource(ModificationSource.AUTO)
            .modelRelation(GURU_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        CommonModel generated = buildModel(GENERATED_MODEL_ID, CommonModel.Source.GENERATED)
            .modelRelation(GENERATED_SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel generatedSku = buildModel(GENERATED_SKU_MODEL_ID_1, CommonModel.Source.GENERATED_SKU)
            .param("param-sku-defining")
            .setOption(1)
            .modificationSource(ModificationSource.AUTO)
            .modelRelation(GENERATED_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        modelStorageService.initializeWithModels(guru, existSku, generated, generatedSku);


        GroupOperationStatus result = skuService.createOrUpdateSku(generated, guru, CONTEXT, SYNC_CONTEXT_VENDOR);


        assertThat(result.isOk(), is(true));
        assertThat(modelStorageService.getAllModels(), hasSize(4));

        List<CommonModel> skuList = loadModelsOfType(CommonModel.Source.SKU);
        assertThat(skuList, hasSize(1));

        CommonModel sku = skuList.get(0);
        assertThat(sku.getId(), is(SKU_MODEL_ID_1));
        assertThat(sku.isPublished(), is(true));
        assertThat(sku.getFlatParameterValues(),
            contains(
                paramOption("param-sku-defining", 1)
            )
        );
    }

    @Test
    public void createOption() {
        CommonModel guru = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU).getModel();

        CommonModel generated = buildModel(GENERATED_MODEL_ID, CommonModel.Source.GENERATED)
            .modelRelation(GENERATED_SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel generatedSku = buildModel(GENERATED_SKU_MODEL_ID_1, CommonModel.Source.GENERATED_SKU)
            .param("param-sku-defining")
            .setOption(1)
            .modificationSource(ModificationSource.AUTO)
            .modelRelation(GENERATED_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        ParameterValueHypothesis hypothesis = new ParameterValueHypothesis();
        hypothesis.setXslName("param-sku-defining");
        hypothesis.setType(Param.Type.ENUM);
        hypothesis.setParamId(PARAM_SKU_DEFINING_ID);
        Word newOption = new Word(Word.DEFAULT_LANG_ID, "param-sku-defining-new-option");
        hypothesis.setStringValue(Collections.singletonList(newOption));
        generatedSku.putParameterValueHypothesis(hypothesis);

        final long createdOptionId = 111222333;

        modelStorageService.initializeWithModels(guru, generated, generatedSku);

        when(parametersServiceClient.addOptions(eq(CATEGORY_ID), eq(PARAM_SKU_DEFINING_ID), anyCollection(),
            eq(USER_ID)))
            .thenAnswer((Answer<List<Long>>) invocation -> {
                // mock option create
                Collection<MboParameters.Option> options = invocation.getArgument(2);
                return options.stream()
                    .map(option -> {
                        if (option.getNameCount() > 0 && option.getName(0)
                            .getName().equals("param-sku-defining-new-option")) {
                            return createdOptionId;
                        } else {
                            return -1L;
                        }
                    }).collect(Collectors.toList());
            });


        GroupOperationStatus result = skuService.createOrUpdateSku(generated, guru, CONTEXT, SYNC_CONTEXT_VENDOR);


        assertThat(result.isOk(), is(true));
        assertThat(modelStorageService.getAllModels(), hasSize(4));

        List<CommonModel> skuList = loadModelsOfType(CommonModel.Source.SKU);
        assertThat(skuList, hasSize(1));

        CommonModel sku = skuList.get(0);
        assertThat(sku.isPublished(), is(true));
        assertThat(sku.getFlatParameterValues(),
            contains(
                paramOption("param-sku-defining", 1),
                paramOption("param-sku-defining", createdOptionId)
            )
        );
    }

    @Test
    public void matchOptionByAlias() {
        CommonModel guru = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU).getModel();

        CommonModel generated = buildModel(GENERATED_MODEL_ID, CommonModel.Source.GENERATED)
            .modelRelation(GENERATED_SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel generatedSku = buildModel(GENERATED_SKU_MODEL_ID_1, CommonModel.Source.GENERATED_SKU)
            .modelRelation(GENERATED_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        ParameterValueHypothesis hypothesis = new ParameterValueHypothesis();
        hypothesis.setXslName("param-sku-defining");
        hypothesis.setType(Param.Type.ENUM);
        hypothesis.setParamId(PARAM_SKU_DEFINING_ID);
        hypothesis.setStringValue(WordUtil.defaultWords("param-sku-defining-option-1-alias"));
        generatedSku.putParameterValueHypothesis(hypothesis);

        modelStorageService.initializeWithModels(guru, generated, generatedSku);


        GroupOperationStatus result = skuService.createOrUpdateSku(generated, guru, CONTEXT, SYNC_CONTEXT_VENDOR);


        MboAssertions.assertThat(result).isOk();
        assertThat(modelStorageService.getAllModels(), hasSize(4));

        List<CommonModel> skuList = loadModelsOfType(CommonModel.Source.SKU);
        assertThat(skuList, hasSize(1));

        CommonModel sku = skuList.get(0);
        assertThat(sku.isPublished(), is(true));
        assertThat(sku.getFlatParameterValues(),
            contains(
                paramOption("param-sku-defining", 1)
            )
        );
    }

    @Test
    public void testNumericDefiningParameter() {
        String paramName = "param-sku-defining-numeric";

        CommonModel guru = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU)
            .modelRelation(SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel existSku = buildModel(SKU_MODEL_ID_1, CommonModel.Source.SKU)
            .param(paramName)
            .modificationSource(ModificationSource.AUTO)
            .setNumeric(new BigDecimal("400.0"))
            .modelRelation(GURU_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        CommonModel generated = buildModel(GENERATED_MODEL_ID, CommonModel.Source.GENERATED)
            .modelRelation(GENERATED_SKU_MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        CommonModel generatedSku = buildModel(GENERATED_SKU_MODEL_ID_1, CommonModel.Source.GENERATED_SKU)
            .param(paramName)
            .modificationSource(ModificationSource.AUTO)
            .setNumeric(new BigDecimal("400"))
            .modelRelation(GENERATED_MODEL_ID, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        modelStorageService.initializeWithModels(guru, existSku, generated, generatedSku);

        GroupOperationStatus result = skuService.createOrUpdateSku(generated, guru, CONTEXT, SYNC_CONTEXT_VENDOR);


        assertThat(result.isOk(), is(true));
        assertThat(modelStorageService.getAllModels(), hasSize(4));

        List<CommonModel> skuList = loadModelsOfType(CommonModel.Source.SKU);
        assertThat(skuList, hasSize(1));

        CommonModel sku = skuList.get(0);
        assertThat(sku.getId(), is(SKU_MODEL_ID_1));
        assertThat(sku.isPublished(), is(true));
    }

    @Test
    public void testGeneratedWithoutSkus() {
        CommonModel generated = buildModel(GENERATED_MODEL_ID, CommonModel.Source.GENERATED).getModel();

        CommonModel guru = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU).getModel();

        modelStorageService.initializeWithModels(generated, guru);


        CommonModel guruUpdated = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU)
            .startParameterValue()
            .paramId(PARAM_NUMERIC_ID)
            .num(1L)
            .endParameterValue()
            .picture("https://my.url")
            .getModel();

        GroupOperationStatus result =
            skuService.createOrUpdateSku(generated, guruUpdated, CONTEXT, SYNC_CONTEXT_VENDOR);


        assertThat(result.isOk(), is(true));
        assertThat(modelStorageService.getAllModels(), hasSize(2));

        CommonModel guruFromStorage = modelStorageService.getModel(guru.getCategoryId(), guru.getId())
            .orElse(null);

        assertThat(guruFromStorage, is(notNullValue()));
        assertEquals(guruUpdated, guruFromStorage);
    }

    @Test
    public void testCreateAndAddSkuToGuruModel() throws GroupOperationStatusException {
        String paramName = "param-sku-defining-numeric";
        CommonModel generated = buildModel(GENERATED_MODEL_ID, CommonModel.Source.GENERATED)
            .withSkuRelations(CATEGORY_ID, GENERATED_SKU_MODEL_ID_1, GENERATED_SKU_MODEL_ID_2, GENERATED_SKU_MODEL_ID_3)
            .getModel();
        CommonModel guru = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU)
            .withSkuRelations(CATEGORY_ID, SKU_MODEL_ID_1)
            .getModel();

        CommonModel existSku1 = buildModel(SKU_MODEL_ID_1, CommonModel.Source.SKU)
            .withSkuParentRelation(guru)
            .param(paramName)
            .modificationSource(ModificationSource.AUTO)
            .setNumeric(new BigDecimal("400.0"))
            .getModel();

        CommonModel generatedSku1 = buildModel(GENERATED_SKU_MODEL_ID_1, CommonModel.Source.GENERATED_SKU)
            .withSkuParentRelation(generated)
            .param(paramName)
            .modificationSource(ModificationSource.AUTO)
            .setNumeric(new BigDecimal("400"))
            .getModel();
        CommonModel generatedSku2 = buildModel(GENERATED_SKU_MODEL_ID_2, CommonModel.Source.GENERATED_SKU)
            .withSkuParentRelation(generated)
            .param(paramName)
            .modificationSource(ModificationSource.AUTO)
            .setNumeric(new BigDecimal("500"))
            .getModel();
        CommonModel generatedSku3 = buildModel(GENERATED_SKU_MODEL_ID_3, CommonModel.Source.GENERATED_SKU)
            .withSkuParentRelation(generated)
            .param(paramName)
            .modificationSource(ModificationSource.AUTO)
            .setNumeric(new BigDecimal("600"))
            .getModel();

        modelStorageService.initializeWithModels(guru, generated, existSku1,
            generatedSku1, generatedSku2, generatedSku3);

        List<CommonModel> updatedModels = skuService.createAndAddSkuToGuruModel(generated, guru,
            Collections.singletonList(generatedSku2),
            CONTEXT,
            SYNC_CONTEXT_VENDOR
        );
        GroupOperationStatus result = modelStorageService.saveModels(updatedModels, CONTEXT);

        Assertions.assertThat(result.isOk()).isTrue();

        // assert created sku contains relations to gsku
        List<OperationStatus> createStatuses = result.getRequestedModelStatuses().stream()
            .filter(status -> status.getType() == OperationType.CREATE)
            .collect(Collectors.toList());
        Assertions.assertThat(createStatuses).hasSize(1);
        OperationStatus operationStatus = createStatuses.get(0);
        CommonModel sku = operationStatus.getModel();
        Assertions.assertThat(sku.isPublished()).isTrue();
        Assertions.assertThat(sku.getCurrentType()).isEqualByComparingTo(CommonModel.Source.SKU);
        Assertions.assertThat(sku.getRelation(ModelRelation.RelationType.SYNC_SOURCE))
            .contains(new ModelRelation(generatedSku2.getId(), generatedSku2.getCategoryId(),
                ModelRelation.RelationType.SYNC_SOURCE));

        Assertions.assertThat(modelStorageService.getAllModels()).hasSize(7);
        List<CommonModel> skuList = loadModelsOfType(CommonModel.Source.SKU);
        Assertions.assertThat(skuList).hasSize(2);
    }

    @Test
    public void testCreateAndAddSkuToGuruModelWithCustomAndHypothesisImagePicker()
        throws GroupOperationStatusException {
        String numericParamName = "param-sku-defining-numeric";
        String enumParamName = "param-sku-defining";

        CommonModel guru = buildModel(GURU_MODEL_ID, CommonModel.Source.GURU)
            .withSkuRelations(CATEGORY_ID, SKU_MODEL_ID_1)
            .getModel();

        CommonModel sku = buildModel(SKU_MODEL_ID_1, CommonModel.Source.SKU)
            .withSkuParentRelation(guru)
            .param(numericParamName)
            .modificationSource(ModificationSource.AUTO)
            .setNumeric(new BigDecimal("400.0"))
            .getModel();

        CommonModel generated = buildModel(GENERATED_MODEL_ID, CommonModel.Source.GENERATED)
            .withSkuRelations(CATEGORY_ID, GENERATED_SKU_MODEL_ID_1)
            .getModel();

        CommonModel generatedSku = buildModel(GENERATED_SKU_MODEL_ID_1, CommonModel.Source.GENERATED_SKU)
            .withSkuParentRelation(generated)
            .param(numericParamName)
            .startParameterValueLink()
                .paramId(1L)
                .optionId(1L)
                .pickerImage("http://picker1")
                .pickerImageSource(ModificationSource.AUTO)
                .endParameterValue()
            .modificationSource(ModificationSource.AUTO)
            .setNumeric(new BigDecimal("500"))
            .parameterValueHypothesis(
                PARAM_SKU_DEFINING_ID,
                enumParamName,
                Param.Type.ENUM,
                "param-sku-defining-option-1-alias")
            .startParameterValueLink()
                .paramId(PARAM_SKU_DEFINING_ID)
                .type(Param.Type.HYPOTHESIS)
                .xslName(enumParamName)
                .pickerImage("http://picker2")
                .pickerImageSource(ModificationSource.AUTO)
                .hypothesis("param-sku-defining-option-1-alias")
                .endParameterValue()
            .getModel();

        modelStorageService.initializeWithModels(guru, generated, sku, generatedSku);

        List<CommonModel> updatedModels = skuService.createAndAddSkuToGuruModel(generated, guru,
            Collections.singletonList(generatedSku),
            CONTEXT,
            SYNC_CONTEXT_VENDOR
        );

        GroupOperationStatus result = modelStorageService.saveModels(updatedModels, CONTEXT);

        Assertions.assertThat(result.isOk()).isTrue();

        // assert created sku contains relations to gsku
        List<OperationStatus> createStatuses = result.getRequestedModelStatuses().stream()
            .filter(status -> status.getType() == OperationType.CREATE)
            .collect(Collectors.toList());
        Assertions.assertThat(createStatuses).hasSize(1);
        OperationStatus operationStatus = createStatuses.get(0);

        CommonModel skuAfter = operationStatus.getModel();
        Assertions.assertThat(skuAfter.getParameterValueLinks())
            .usingElementComparatorOnFields("pickerImage", "type")
            .containsExactlyInAnyOrder(
                ParameterValueBuilder.newBuilder()
                    .pickerImage("http://picker1")
                    .type(Param.Type.ENUM)
                    .build(),
                ParameterValueBuilder.newBuilder()
                    .pickerImage("http://picker2")
                    .type(Param.Type.HYPOTHESIS)
                    .build(),
                ParameterValueBuilder.newBuilder()
                    .pickerImage("http://picker2")
                    .type(Param.Type.ENUM)
                    .build()
            );

        Assertions.assertThat(skuAfter.isPublished()).isTrue();
        Assertions.assertThat(skuAfter.getCurrentType()).isEqualByComparingTo(CommonModel.Source.SKU);
        Assertions.assertThat(skuAfter.getRelation(ModelRelation.RelationType.SYNC_SOURCE))
            .contains(new ModelRelation(generatedSku.getId(), generatedSku.getCategoryId(),
                ModelRelation.RelationType.SYNC_SOURCE));

        List<CommonModel> guruModels = loadModelsOfType(CommonModel.Source.GURU);
        Assertions.assertThat(guruModels.size()).isEqualTo(1);

        CommonModel guruAfter = guruModels.get(0);
        Assertions.assertThat(guruAfter.getParameterValueLinks())
            .usingElementComparatorOnFields("pickerImage", "type")
            .containsExactlyInAnyOrder(
                ParameterValueBuilder.newBuilder()
                    .pickerImage("http://picker1")
                    .type(Param.Type.ENUM)
                    .build(),
                ParameterValueBuilder.newBuilder()
                    .pickerImage("http://picker2")
                    .type(Param.Type.ENUM)
                    .build()
            );
    }

    private List<CommonModel> loadModelsOfType(CommonModel.Source type) {
        return modelStorageService.getAllModels()
            .stream()
            .filter(m -> m.getCurrentType() == type)
            .collect(Collectors.toList());
    }

    @NotNull
    private Function<List<CategoryParam>, Void> parametersCollector() {
        return p -> {
            this.protoParameters.addAll(
                p.stream()
                    .map(this::convertParameter)
                    .collect(Collectors.toList())
            );
            parameters.addAll(p);
            return null;
        };
    }

    private MboParameters.Parameter convertParameter(CategoryParam parameter) {
        return MboParameters.Parameter.newBuilder()
            .setId(parameter.getId())
            .setXslName(parameter.getXslName())
            .setPrecision(parameter.getPrecision())
            .setValueType(ParameterProtoConverter.convert(parameter.getType()))
            .addAllOption(ConverterUtils.convertList(parameter.getOptions(), this::convertOption))
            .setSkuMode(ParameterProtoConverter.convert(parameter.getSkuParameterMode()))
            .setExtractInSkubd(parameter.isExtractInSkubd())
            .build();
    }

    private MboParameters.Option convertOption(Option o) {
        return ParameterProtoConverter.convert(o, null, false, false, false).build();
    }

    private static int uniqId() {
        return ID_HOLDER.incrementAndGet();
    }
}
