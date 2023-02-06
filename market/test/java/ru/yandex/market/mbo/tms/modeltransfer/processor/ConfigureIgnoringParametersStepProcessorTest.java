package ru.yandex.market.mbo.tms.modeltransfer.processor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.transfer.ModelTransferBuilder;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterValueBuilder;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelParameterLandingConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfParametersConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParameterInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParameterResultEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParametersResult;
import ru.yandex.market.mbo.gwt.models.transfer.step.ResultEntry;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.tms.modeltransfer.ListOfModelsConfigBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferJobContext;
import ru.yandex.market.mbo.tms.modeltransfer.ResultInfoBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dmserebr
 * @date 28.11.18
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.class)
public class ConfigureIgnoringParametersStepProcessorTest {

    private Map<Long, CommonModel> modelsMap = new HashMap<>();

    private static final long SOURCE_CATEGORY = 10L;
    private static final long TARGET_CATEGORY = 11L;

    @Mock
    private IParameterLoaderService parameterLoaderService;

    private ConfigureIgnoringParametersStepProcessor stepProcessor;

    @Before
    public void before() {
        ModelStorageServiceStub modelStorageService = new ModelStorageServiceStub();
        stepProcessor = new ConfigureIgnoringParametersStepProcessor(
            modelStorageService, parameterLoaderService);

        modelStorageService.setModelsMap(modelsMap);
    }

    @Test
    public void ignoringParametersAreOk() {
        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.addParameter(ParameterBuilder.builder().id(100L).xsl("param100").endParameter());

        Mockito.doReturn(categoryEntities)
            .when(parameterLoaderService).loadCategoryEntitiesByHid(Mockito.eq(SOURCE_CATEGORY));

        ListOfModelParameterLandingConfig config = config(Collections.singletonList(100L), 3L);

        ParametersResult result = stepProcessor.validateStep(getTestResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.COMPLETED, result.getResultInfo().getStatus());
        Assert.assertEquals("Список параметров для игнорирования успешно прошел валидацию",
            result.getResultInfo().getResultText());
        Assert.assertTrue(result.getResultEntries().isEmpty());
    }

    @Test
    public void ignoringParameterDoesntExist() {
        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.addParameter(ParameterBuilder.builder().id(100L).xsl("param100").endParameter());

        Mockito.doReturn(categoryEntities)
            .when(parameterLoaderService).loadCategoryEntitiesByHid(Mockito.eq(SOURCE_CATEGORY));

        ListOfModelParameterLandingConfig config = config(Collections.singletonList(101L), 3L);

        ParametersResult result = stepProcessor.validateStep(getTestResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals(1, result.getResultEntries().size());
        Assert.assertEquals("Ошибка проверки 1 параметров из 1",
            result.getResultInfo().getResultText());
        assertParameterResultEntryFailure(result.getResultEntries().get(0),
            new ParameterInfo(SOURCE_CATEGORY, 101L, "missingParam101", "Отсутствующий параметр 101"),
            "параметр отсутствует в категории 10");
    }

    @Test
    public void ignoringParametersContainInvalidParams() {
        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.addParameter(ParameterBuilder.builder().id(100L).xsl("name").endParameter());
        categoryEntities.addParameter(ParameterBuilder.builder().id(101L).xsl("serviceParam")
            .service(true).endParameter());
        categoryEntities.addParameter(ParameterBuilder.builder().id(102L).xsl("XL-Picture").endParameter());
        categoryEntities.addParameter(ParameterBuilder.builder().id(103L).xsl("XL-Picture_3").endParameter());
        categoryEntities.addParameter(ParameterBuilder.builder().id(104L).xsl("XLPictureUrl").endParameter());
        categoryEntities.addParameter(ParameterBuilder.builder().id(105L).xsl("testParam").endParameter());

        Mockito.doReturn(categoryEntities)
            .when(parameterLoaderService).loadCategoryEntitiesByHid(Mockito.eq(SOURCE_CATEGORY));

        ListOfModelParameterLandingConfig config = config(Arrays.asList(100L, 101L, 102L, 103L, 104L, 105L), 3L);

        ParametersResult result = stepProcessor.validateStep(getTestResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals(5, result.getResultEntries().size());
        Assert.assertEquals("Ошибка проверки 5 параметров из 6",
            result.getResultInfo().getResultText());
        assertParameterResultEntryFailure(result.getResultEntries().get(0),
            categoryEntities.getParameterById(100L),
            "параметр name не может быть проигнорирован");
        assertParameterResultEntryFailure(result.getResultEntries().get(1),
            categoryEntities.getParameterById(101L),
            "служебный параметр не может быть проигнорирован");
        assertParameterResultEntryFailure(result.getResultEntries().get(2),
            categoryEntities.getParameterById(102L),
            "параметр картинок не может быть проигнорирован");
        assertParameterResultEntryFailure(result.getResultEntries().get(3),
            categoryEntities.getParameterById(103L),
            "параметр картинок не может быть проигнорирован");
        assertParameterResultEntryFailure(result.getResultEntries().get(4),
            categoryEntities.getParameterById(104L),
            "параметр картинок не может быть проигнорирован");
    }

    @Test
    public void ignoringParametersContainSKUParams() {
        // Guru model with 2 SKUs - they have filled defining / informational params
        modelsMap.put(2L, CommonModelBuilder.newBuilder(2L, SOURCE_CATEGORY, 1L)
            .currentType(CommonModel.Source.GURU)
            .modelRelation(3L, SOURCE_CATEGORY, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(4L, SOURCE_CATEGORY, ModelRelation.RelationType.SKU_MODEL)
            .getModel());
        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, SOURCE_CATEGORY, 1L)
            .currentType(CommonModel.Source.SKU)
            .putParameterValue(ParameterValueBuilder.newBuilder().paramId(100L)
                .xslName("skuInformationalParam").num(18L).build())
            .putParameterValue(ParameterValueBuilder.newBuilder().paramId(101L)
                .xslName("skuInformationalParam2").num(18L).build())
            .putParameterValue(ParameterValueBuilder.newBuilder().paramId(102L)
                .xslName("skuDefiningParam").num(19L).build())
            .getModel());
        modelsMap.put(4L, CommonModelBuilder.newBuilder(4L, SOURCE_CATEGORY, 2L)
            .currentType(CommonModel.Source.SKU)
            .putParameterValue(ParameterValueBuilder.newBuilder().paramId(100L)
                .xslName("skuInformationalParam").num(14L).build())
            .putParameterValue(ParameterValueBuilder.newBuilder().paramId(101L)
                .xslName("skuDefiningParam2").num(15L).build())
            .getModel());

        // IsSKU model - has filled skuDefiningParam3, but it should not be taken into account
        modelsMap.put(5L, CommonModelBuilder.newBuilder(5L, SOURCE_CATEGORY, 2L)
            .currentType(CommonModel.Source.GURU)
            .putParameterValue(ParameterValueBuilder.newBuilder().paramId(110L)
                .xslName(XslNames.IS_SKU).build())
            .putParameterValue(ParameterValueBuilder.newBuilder().paramId(100L)
                .xslName("skuInformationalParam").num(14L).build())
            .putParameterValue(ParameterValueBuilder.newBuilder().paramId(101L)
                .xslName("skuDefiningParam3").num(13L).build())
            .getModel());

        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.addParameter(ParameterBuilder.builder().id(100L).xsl("skuInformationalParam")
            .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL).endParameter());
        categoryEntities.addParameter(ParameterBuilder.builder().id(101L).xsl("skuInformationalParam2")
            .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL).endParameter());
        categoryEntities.addParameter(ParameterBuilder.builder().id(102L).xsl("skuDefiningParam")
            .skuParameterMode(SkuParameterMode.SKU_DEFINING).endParameter());
        categoryEntities.addParameter(ParameterBuilder.builder().id(103L).xsl("skuDefiningParam2")
            .skuParameterMode(SkuParameterMode.SKU_DEFINING).endParameter());
        categoryEntities.addParameter(ParameterBuilder.builder().id(104L).xsl("skuDefiningParam3")
            .skuParameterMode(SkuParameterMode.SKU_DEFINING).endParameter());

        Mockito.doReturn(categoryEntities)
            .when(parameterLoaderService).loadCategoryEntitiesByHid(Mockito.eq(SOURCE_CATEGORY));

        ListOfModelParameterLandingConfig config = config(Arrays.asList(100L, 101L, 102L, 103L, 104L), 2L, 5L);

        ParametersResult result = stepProcessor.validateStep(getTestResultInfo(), getTestContext(config));

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals(4, result.getResultEntries().size());
        Assert.assertEquals("Ошибка проверки 4 параметров из 5",
            result.getResultInfo().getResultText());
        assertParameterResultEntryFailure(result.getResultEntries().get(0),
            categoryEntities.getParameterById(100L),
            "SKU-параметр, заполненный в SKU переносимых моделей, не может быть проигнорирован");
        assertParameterResultEntryFailure(result.getResultEntries().get(1),
            categoryEntities.getParameterById(101L),
            "SKU-параметр, заполненный в SKU переносимых моделей, не может быть проигнорирован");
        assertParameterResultEntryFailure(result.getResultEntries().get(2),
            categoryEntities.getParameterById(102L),
            "SKU-параметр, заполненный в SKU переносимых моделей, не может быть проигнорирован");
        assertParameterResultEntryFailure(result.getResultEntries().get(3),
            categoryEntities.getParameterById(103L),
            "SKU-параметр, заполненный в SKU переносимых моделей, не может быть проигнорирован");
    }

    private void assertParameterResultEntryFailure(ParameterResultEntry entry,
                                                   CategoryParam parameter, String errorMessage) {
        assertParameterResultEntryFailure(entry, ParameterInfo.from(parameter), errorMessage);
    }

    private void assertParameterResultEntryFailure(ParameterResultEntry entry,
                                                   ParameterInfo parameterInfo, String errorMessage) {
        Assert.assertEquals(ResultEntry.Status.FAILURE, entry.getStatus());
        Assert.assertEquals(parameterInfo, entry.getSourceParameter());
        Assert.assertEquals(errorMessage, entry.getStatusMessage());
    }

    private ResultInfo getTestResultInfo() {
        return ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED).resultType(ResultInfo.Type.VALIDATION).build();
    }

    private ModelTransferJobContext<ListOfModelParameterLandingConfig> getTestContext(
        ListOfModelParameterLandingConfig config) {

        ModelTransfer modelTransfer = ModelTransferBuilder.newBuilder().id(1L).build();
        ModelTransferStepInfo stepInfo = new ModelTransferStepInfo();
        stepInfo.setId(1L);
        return new ModelTransferJobContext<>(modelTransfer, stepInfo, Collections.singletonList(stepInfo),
            config, Collections.emptyList());
    }

    private ListOfModelParameterLandingConfig config(List<Long> parameterIds, long... modelIds) {
        return new ListOfModelParameterLandingConfig(
            ListOfModelsConfigBuilder.newBuilder()
                .models(SOURCE_CATEGORY, TARGET_CATEGORY, modelIds)
                .build(),
            new ListOfParametersConfig(parameterIds));
    }
}
