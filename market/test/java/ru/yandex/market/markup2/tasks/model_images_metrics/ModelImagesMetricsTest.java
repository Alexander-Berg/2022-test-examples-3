package ru.yandex.market.markup2.tasks.model_images_metrics;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.markup2.utils.Markup2TestUtils;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.resultMaker.ResultMakerContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static ru.yandex.market.markup2.tasks.model_images_metrics.OkBadSerializer.BAD;
import static ru.yandex.market.markup2.tasks.model_images_metrics.OkBadSerializer.OK;

/**
 * @author york
 * @since 03.08.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelImagesMetricsTest {
    private static Random random = new Random(1);

    @Test
    public void testResultsProcessing() {
        ModelImagesMetricsHitmanDataProcessor modelImagesMetricsHitmanDataProcessor =
            new ModelImagesMetricsHitmanDataProcessor(null);

        ResultMakerContext<ModelImagesMetricsTaskIdentity, ModelImagesMetricsTaskPayload,
            ModelImagesMetricsHitmanResponse> context = Markup2TestUtils.createResultMakerContext(
            Markup2TestUtils.createBasicTaskInfo(100, 2, Collections.emptyMap()));

        List<TaskDataItem<ModelImagesMetricsTaskPayload, ModelImagesMetricsHitmanResponse>> items = new ArrayList<>();
        items.add(createTDI(newResp(true, true, false, false, true, true, false, false)));
        items.add(createTDI(newResp(true, false, false, true, false, true, false, false)));
        items.add(createTDI(newResp(true, true, false, false, false, true, false, false)));
        items.add(createTDI(newResp(false, false, false, false, false, true, false, false)));
        items.add(createTDI(newResp(false, false, false, false, true, true, false, false)));

        context.addDataItems(items);

        modelImagesMetricsHitmanDataProcessor.makeResults(context);
        Assert.assertNotNull(context.getTaskConfigGroupInfo().getMetrics());
        ModelImagesMetricsData metricsData =
                (ModelImagesMetricsData) context.getTaskConfigGroupInfo().getMetrics().getMetricsData();
        Assert.assertEquals(items.size(), metricsData.getTotalCount());
        Assert.assertEquals(3, metricsData.getWatermarkCount());
        Assert.assertEquals(2, metricsData.getBlurCount());
        Assert.assertEquals(0, metricsData.getCroppedCount());
        Assert.assertEquals(1, metricsData.getMultiCount());
        Assert.assertEquals(3, metricsData.getRelevantCount());
        Assert.assertEquals(1, metricsData.getCleanCount());
        Assert.assertEquals(100, metricsData.getCategoryId());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private ModelImagesMetricsHitmanResponse newResp(boolean watermark,
                                                     boolean blur,
                                                     boolean cropped,
                                                     boolean multi,
                                                     boolean relevance,
                                                     boolean white,
                                                     boolean component,
                                                     boolean additional) {

        return new ModelImagesMetricsHitmanResponse(
            String.valueOf(random.nextLong()),
            String.valueOf(random.nextLong()),
            "url" + random.nextInt(),
            true, //might be main
            watermark ? BAD : OK,
            blur ? BAD : OK,
            cropped ? BAD : OK,
            multi ? BAD : OK,
            relevance ? BAD : OK,
            white ? BAD : OK,
            component ? BAD : OK,
            additional ? BAD : OK
        );
    }

    private TaskDataItem<ModelImagesMetricsTaskPayload, ModelImagesMetricsHitmanResponse> createTDI(
            ModelImagesMetricsHitmanResponse resp) {

        TaskDataItem<ModelImagesMetricsTaskPayload, ModelImagesMetricsHitmanResponse> item =
            new TaskDataItem<>(random.nextLong(), new ModelImagesMetricsTaskPayload(
                new ModelImagesMetricsTaskIdentity(random.nextLong(), "urur"), null
            ));

        item.setResponseInfo(resp);

        return item;
    }
}
