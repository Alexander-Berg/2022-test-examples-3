package ru.yandex.market.mbo.db.modelstorage.yt.fast.delivery;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.KeyValueMapService;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;

import static ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source.GURU;
import static ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source.SKU;

/**
 * @author apluhin
 * @created 4/21/21
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class YtCardQueueTest {

    private YtCardQueue ytCardQueue;
    private YtCardQueueTableConfig ytCardQueueTableConfig;
    private KeyValueMapService keyValueMapService;
    private YtTableRpcApi rpcApi;

    @Before
    public void setUp() throws Exception {
        keyValueMapService = Mockito.mock(KeyValueMapService.class);
        ytCardQueueTableConfig = Mockito.mock(YtCardQueueTableConfig.class);
        rpcApi = Mockito.mock(YtTableRpcApi.class);
        Mockito.when(ytCardQueueTableConfig.renderQueueTableModel()).thenReturn(new YtTableModel().setTabletCount(2));
        ytCardQueue = new YtCardQueue(
            null,
            ytCardQueueTableConfig,
            keyValueMapService);
        Mockito.when(rpcApi.createModifyRowRequest()).thenReturn(Mockito.mock(ModifyRowsRequest.class));
        ReflectionTestUtils.setField(ytCardQueue, "rpcApi", rpcApi);
        ReflectionTestUtils.setField(ytCardQueue, "maxBatchSize", 100);
    }

    private static Map<Long, ModelStorage.ModelOrBuilder> convertModels(List<ModelStorage.ModelOrBuilder> models) {
        return models.stream().collect(Collectors.toMap(ModelStorage.ModelOrBuilder::getId, it -> it));
    }

    private static Map<Long, Event> extractEventFromRaw(List<Map> rawMaps) {
        Map<Long, Event> collect = rawMaps.stream().map(Event::new).collect(Collectors.toMap(Event::getModelId,
            it -> it));
        return collect;
    }

    private static Map<Long, Event> extractEventFromRaw(Map rawMap) {
        return extractEventFromRaw(Arrays.asList(rawMap));
    }

    @Test
    public void testSimpleCallRender() {
        Mockito.when(keyValueMapService.getBoolean(Mockito.eq("yt_render_queue_enabled"))).thenReturn(true);

        ModelStorage.Model.Builder deletedBuilder = buildModel(GURU, 1, false, false);
        ModelStorage.Model.Builder deletedAfter = deletedBuilder.clone().setDeleted(true);
        ModelStorage.Model.Builder deletedBefore = deletedBuilder.setDeleted(true);
        ModelStorage.Model.Builder createdAfter = buildModel(GURU, 2, true, false);
        ModelStorage.Model.Builder updatedBuilder = buildModel(GURU, 3, true, false);
        ModelStorage.Model.Builder updatedAfter =
            buildModel(GURU, 3, true, false).setBluePublished(true);
        RenderContext<ModelStorage.ModelOrBuilder> context = new RenderContext<>(
            Arrays.asList(deletedAfter, createdAfter, updatedAfter),
            convertModels(Arrays.asList(deletedBefore, updatedBuilder))
        );

        QueueExportResult queueExportResult = ytCardQueue.addModels(context);
        Assertions.assertThat(queueExportResult.isEnable()).isTrue();
        ModifyRowsRequest rowsRequest = queueExportResult.getBatchRequest().get(0);
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(rowsRequest, Mockito.times(3)).addInsert(captor.capture());
        List<Map> allValues = captor.getAllValues();
        Map<Long, Event> groupByModel = extractEventFromRaw(allValues);
        Assertions.assertThat(groupByModel.get(1L).cardEvent).isEqualTo(CardEvent.DELETE);
        Assertions.assertThat(groupByModel.get(2L).cardEvent).isEqualTo(CardEvent.UPDATE);
        Assertions.assertThat(groupByModel.get(3L).cardEvent).isEqualTo(CardEvent.UPDATE);
    }

    @Test
    public void testSkipChildModelsWithGroupModel() {
        Mockito.when(keyValueMapService.getBoolean(Mockito.eq("yt_render_queue_enabled"))).thenReturn(true);

        ModelStorage.Model.Builder updatedGroupBefore = buildModel(GURU, 2, true, false);
        ModelStorage.Model.Builder updatedGroupAfter = updatedGroupBefore.clone().setBluePublished(true);
        ModelStorage.Model.Builder updatedChildBefore = buildModelWithGroupModel(GURU, 3, 2, true, false);
        ModelStorage.Model.Builder updatedChildAfter = updatedChildBefore.clone().setBluePublished(true);
        RenderContext<ModelStorage.ModelOrBuilder> context = new RenderContext<>(
            Arrays.asList(updatedGroupAfter, updatedChildAfter),
            convertModels(Arrays.asList(updatedGroupBefore, updatedChildBefore))
        );
        QueueExportResult queueExportResult = ytCardQueue.addModels(context);

        Assertions.assertThat(queueExportResult.isEnable()).isTrue();
        ModifyRowsRequest rowsRequest = queueExportResult.getBatchRequest().get(0);
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(rowsRequest, Mockito.times(1)).addInsert(captor.capture());
        Map groupModel = captor.getValue();
        Event event = extractEventFromRaw(groupModel).get(2L);
        Assertions.assertThat(event.cardEvent).isEqualTo(CardEvent.UPDATE);
        Assertions.assertThat(event.modelId).isEqualTo(2L);
    }

    @Test
    public void testSkipSkuWithParentUpdate() {
        Mockito.when(keyValueMapService.getBoolean(Mockito.eq("yt_render_queue_enabled"))).thenReturn(true);

        ModelStorage.Model.Builder updateParentBefore = buildModelWithGroupModel(GURU, 5, 5, true, false);
        ModelStorage.Model updateParentAfter = updateParentBefore.clone().setBluePublished(true).build();
        ModelStorage.Model.Builder updatedSku1Before = buildSkuWithParentRelation(3, 2, 5, true, false);
        ModelStorage.Model.Builder updatedSku1After = updatedSku1Before.clone().setBluePublished(true);
        ModelStorage.Model.Builder updatedSku2Before = buildSkuWithParentRelation(4, 6, 5, true, false);
        ModelStorage.Model.Builder updatedSku2After = updatedSku2Before.clone().setBluePublished(true);
        RenderContext<ModelStorage.ModelOrBuilder> context = new RenderContext<>(
            Arrays.asList(updateParentAfter, updatedSku1After, updatedSku2After),
            convertModels(Arrays.asList(updateParentBefore, updatedSku1Before, updatedSku2Before))
        );
        QueueExportResult queueExportResult = ytCardQueue.addModels(context);

        Assertions.assertThat(queueExportResult.isEnable()).isTrue();
        ModifyRowsRequest rowsRequest = queueExportResult.getBatchRequest().get(0);
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(rowsRequest, Mockito.times(1)).addInsert(captor.capture());
        Map<Long, Event> allValues = extractEventFromRaw(captor.getAllValues());
        Assertions.assertThat(allValues.get(5L).cardEvent).isEqualTo(CardEvent.UPDATE);
    }

    @Test
    public void testSplitBatch() {
        Mockito.when(keyValueMapService.getBoolean(Mockito.eq("yt_render_queue_enabled"))).thenReturn(true);

        ModelStorage.Model.Builder deletedBuilder = buildModel(GURU, 1, false, false);
        ModelStorage.Model.Builder deletedAfter = deletedBuilder.clone().setDeleted(true);
        ModelStorage.Model.Builder deletedBefore = deletedBuilder.setDeleted(true);
        ModelStorage.Model.Builder createdAfter = buildModel(GURU, 2, true, false);
        ModelStorage.Model.Builder updatedBuilder = buildModel(GURU, 3, true, false);
        ModelStorage.Model.Builder updatedAfter =
            buildModel(GURU, 3, true, false).setBluePublished(true);
        RenderContext<ModelStorage.ModelOrBuilder> context = new RenderContext<>(
            Arrays.asList(deletedAfter, createdAfter, updatedAfter),
            convertModels(Arrays.asList(deletedBefore, updatedBuilder))
        );

        ReflectionTestUtils.setField(ytCardQueue, "maxBatchSize", 1);

        QueueExportResult queueExportResult = ytCardQueue.addModels(context);
        Assertions.assertThat(queueExportResult.isEnable()).isTrue();
        Assertions.assertThat(queueExportResult.getBatchRequest().size()).isEqualTo(3);

    }

    @Test
    public void testDisabledQueue() {
        ModelStorage.Model.Builder convert1 = buildModel(GURU, 1, true, false);
        Mockito.when(keyValueMapService.getBoolean(Mockito.eq("yt_render_queue_enabled"))).thenReturn(false);
        RenderContext<ModelStorage.ModelOrBuilder> modelRenderContext = new RenderContext<>(
            Arrays.asList(convert1),
            convertModels(Arrays.asList(convert1))
        );
        QueueExportResult queueExportResult = ytCardQueue.addModels(modelRenderContext);
        Assertions.assertThat(queueExportResult.isEnable()).isFalse();
    }

    @Test
    public void testEmptyContext() {
        Mockito.when(keyValueMapService.getBoolean(Mockito.eq("yt_render_queue_enabled"))).thenReturn(true);
        RenderContext<ModelStorage.Model> modelRenderContext = new RenderContext<>(Collections.emptyList(), null);
        QueueExportResult queueExportResult = ytCardQueue.addModels(modelRenderContext);
        Assertions.assertThat(queueExportResult.isEnable()).isFalse();
    }

    @Test
    public void testDisabledType() {
        ModelStorage.Model.Builder updatedBuilder = buildModel(GURU, 3, true, false);
        ModelStorage.Model.Builder updatedAfter =
            buildModel(GURU, 3, true, false).setBluePublished(true);
        RenderContext<ModelStorage.ModelOrBuilder> context = new RenderContext<>(
            Arrays.asList(updatedAfter),
            convertModels(Arrays.asList(updatedBuilder))
        );

        Mockito.when(keyValueMapService.getBoolean(Mockito.eq("yt_render_queue_enabled"))).thenReturn(true);
        Mockito.when(keyValueMapService.getString(Mockito.eq("yt_render_queue_disabled_source")))
            .thenReturn("[\"MBO\"]");
        QueueExportResult queueExportResult = ytCardQueue.addModels(context);
        Assertions.assertThat(queueExportResult.isEnable()).isFalse();
    }

    @Test
    public void testPassIgnoredType() {
        ModelStorage.Model.Builder updatedBuilder = buildModel(GURU, 3, true, false);
        ModelStorage.Model.Builder updatedAfter =
            buildModel(GURU, 3, true, false).setBluePublished(true);
        RenderContext<ModelStorage.ModelOrBuilder> context = new RenderContext<>(
            Arrays.asList(updatedAfter),
            convertModels(Arrays.asList(updatedBuilder))
        );

        Mockito.when(keyValueMapService.getBoolean(Mockito.eq("yt_render_queue_enabled"))).thenReturn(true);
        Mockito.when(keyValueMapService.getString(Mockito.eq("yt_render_queue_disabled_source")))
            .thenReturn("[\"MDM\"]");
        QueueExportResult queueExportResult = ytCardQueue.addModels(context);
        Assertions.assertThat(queueExportResult.isEnable()).isTrue();
    }

    private ModelStorage.Model.Builder buildModel(CommonModel.Source type, long modelId, boolean published,
                                                  boolean bluePublished) {
        return buildModelWithGroupModel(type, modelId, modelId, published, bluePublished);
    }

    private ModelStorage.Model.Builder buildModelWithGroupModel(CommonModel.Source type, long modelId,
                                                                long groupModelId, boolean published,
                                                                boolean bluePublished) {
        CommonModel model = new CommonModel();
        model.setCurrentType(type);
        model.setId(modelId);
        model.setCategoryId(1);
        model.setPublished(published);
        model.setBluePublished(bluePublished);
        return ModelProtoConverter.convert(model).toBuilder().setGroupModelId(groupModelId);
    }

    private ModelStorage.Model.Builder buildSkuWithParentRelation(long modelId, long parentId, long groupModelId,
                                                                  boolean published, boolean bluePublished) {
        CommonModel model = new CommonModel();
        model.setCurrentType(SKU);
        model.setId(modelId);
        model.setCategoryId(1);
        model.setPublished(published);
        model.setBluePublished(bluePublished);
        model.setRelations(
            Collections.singleton(new ModelRelation(parentId, 1, ModelRelation.RelationType.SKU_PARENT_MODEL))
        );
        return ModelProtoConverter.convert(model).toBuilder().setGroupModelId(groupModelId);
    }

    private static class Event {
        private final Long modelId;
        private final CardEvent cardEvent;

        private Event(Map rawEvent) {
            Map<String, YTreeNode> event = ((YTreeNode) rawEvent.get("event")).asMap();
            this.modelId = event.get("model_id").longValue();
            this.cardEvent = CardEvent.valueOf(event.get("event_type").stringValue());
        }

        public Long getModelId() {
            return modelId;
        }

        public CardEvent getCardEvent() {
            return cardEvent;
        }
    }
}
