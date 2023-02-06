package ru.yandex.market.mbo.tms.modeltransfer.processor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.transfer.ModelTransferBuilder;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.CategoryPair;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelsConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelsResult;
import ru.yandex.market.mbo.gwt.models.transfer.step.ModelResultEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.ModelTransferList;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferJobContext;
import ru.yandex.market.mbo.tms.modeltransfer.ResultInfoBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.worker.CheckModelsInCategoryWorker;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dmserebr
 * @date 21.09.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class SelectModelsStepProcessorTest {

    private SelectModelsStepProcessor selectModelsStepProcessor;
    private Map<Long, CommonModel> modelsMap = new HashMap<>();

    @Before
    public void before() {
        ModelStorageServiceStub modelStorageService = new ModelStorageServiceStub();
        CheckModelsInCategoryWorker checkModelsInCategoryWorker = new CheckModelsInCategoryWorker(modelStorageService);

        modelStorageService.setModelsMap(modelsMap);
        selectModelsStepProcessor = new SelectModelsStepProcessor(checkModelsInCategoryWorker);
    }

    @Test
    public void testNoConfigFound() {
        ListOfModelsResult result = selectModelsStepProcessor.validateStep(
            getTestResultInfo(),
            getTestContext(null)
        );

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals("Не найдены настройки списка моделей", result.getResultInfo().getResultText());
    }

    @Test
    public void testNoListsPresent() {
        ListOfModelsResult result = selectModelsStepProcessor.validateStep(
            getTestResultInfo(),
            getTestContext(new ListOfModelsConfig())
        );

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals("Не задан ни один список моделей!", result.getResultInfo().getResultText());
    }

    @Test
    public void testEmptyModelsList() {
        ListOfModelsConfig config = new ListOfModelsConfig(
            Arrays.asList(
                new ModelTransferList(),
                new ModelTransferList(new CategoryPair(1L, 2L),
                    Arrays.asList(3L, 4L, 5L), null)));

        ListOfModelsResult result = selectModelsStepProcessor.validateStep(
            getTestResultInfo(),
            getTestContext(config)
        );

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals("Один из списков моделей пуст", result.getResultInfo().getResultText());
    }

    @Test
    public void testGoodModelsList() {
        ListOfModelsConfig config = new ListOfModelsConfig(
            Arrays.asList(
                new ModelTransferList(new CategoryPair(1L, 2L),
                    Arrays.asList(3L, 4L, 5L), null),
                new ModelTransferList(new CategoryPair(1L, 3L),
                    Arrays.asList(6L, 7L), null)));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .currentType(CommonModel.Source.GURU).getModel());
        modelsMap.put(4L, CommonModelBuilder.newBuilder(4L, 1L, 2L)
            .currentType(CommonModel.Source.GURU).getModel());
        modelsMap.put(5L, CommonModelBuilder.newBuilder(5L, 1L, 2L)
            .currentType(CommonModel.Source.GURU).getModel());
        modelsMap.put(6L, CommonModelBuilder.newBuilder(6L, 1L, 1L)
            .currentType(CommonModel.Source.GURU).getModel());
        modelsMap.put(7L, CommonModelBuilder.newBuilder(7L, 1L, 2L)
            .currentType(CommonModel.Source.GURU).getModel());

        ListOfModelsResult result = selectModelsStepProcessor.validateStep(
            getTestResultInfo(),
            getTestContext(config)
        );

        Assert.assertEquals(ResultInfo.Status.COMPLETED, result.getResultInfo().getStatus());
        Assert.assertEquals("Проверка 5 моделей прошла успешно", result.getResultInfo().getResultText());
        Assert.assertEquals(5, result.getResultEntries().size());
        Assert.assertTrue(result.getResultEntries().stream()
            .allMatch(r -> r.getStatus() == ModelResultEntry.Status.SUCCESS));
    }

    @Test
    public void testModelsListsWithDuplications() {
        ListOfModelsConfig config = new ListOfModelsConfig(
            Arrays.asList(
                new ModelTransferList(new CategoryPair(1L, 2L),
                    Arrays.asList(3L, 4L, 5L), null),
                new ModelTransferList(new CategoryPair(1L, 3L),
                    Arrays.asList(5L, 6L, 7L), null)));

        ListOfModelsResult result = selectModelsStepProcessor.validateStep(
            getTestResultInfo(),
            getTestContext(config)
        );

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals(
            "Найдены дублирующиеся модели в списках: 5 [категория 1 -> категория 2, категория 1 -> категория 3]",
            result.getResultInfo().getResultText());
        Assert.assertTrue(result.getResultEntries().isEmpty());
    }

    @Test
    public void testSkuAndModification() {
        ListOfModelsConfig config = new ListOfModelsConfig(
            Collections.singletonList(new ModelTransferList(new CategoryPair(1L, 2L),
                Arrays.asList(3L, 4L, 5L), null)));

        modelsMap.put(3L, CommonModelBuilder.newBuilder(3L, 1L, 1L)
            .currentType(CommonModel.Source.GURU).getModel());
        modelsMap.put(4L, CommonModelBuilder.newBuilder(4L, 1L, 2L)
            .currentType(CommonModel.Source.SKU).getModel());
        modelsMap.put(5L, CommonModelBuilder.newBuilder(5L, 1L, 2L)
            .currentType(CommonModel.Source.GURU).parentModelId(3L).getModel());

        ListOfModelsResult result = selectModelsStepProcessor.validateStep(
            getTestResultInfo(),
            getTestContext(config)
        );

        Assert.assertEquals(ResultInfo.Status.FAILED, result.getResultInfo().getStatus());
        Assert.assertEquals("Ошибки проверки для 2 моделей из 3", result.getResultInfo().getResultText());
        Assert.assertEquals(3, result.getResultEntries().size());
        Assert.assertEquals(1, result.getResultEntries().stream()
            .filter(r -> r.getStatus() == ModelResultEntry.Status.SUCCESS).count());
        Assert.assertEquals(2, result.getResultEntries().stream()
            .filter(r -> r.getStatus() == ModelResultEntry.Status.FAILURE).count());
    }

    private ResultInfo getTestResultInfo() {
        return ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED).resultType(ResultInfo.Type.VALIDATION).build();
    }

    private ModelTransferJobContext<ListOfModelsConfig> getTestContext(ListOfModelsConfig config) {
        ModelTransfer modelTransfer = ModelTransferBuilder.newBuilder().id(1L).build();
        ModelTransferStepInfo stepInfo = new ModelTransferStepInfo();
        stepInfo.setId(1L);
        return new ModelTransferJobContext<>(modelTransfer, stepInfo, Collections.singletonList(stepInfo),
            config, Collections.emptyList());
    }
}
