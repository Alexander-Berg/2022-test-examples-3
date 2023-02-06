package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.http.YangTaskEnums;
import ru.yandex.market.mbo.statistic.model.YangTaskConverter;

import static org.junit.Assert.assertEquals;

public class SaveModelsWithYangTaskSource extends BaseGroupStorageUpdatesTest {

    private static final long CATEGORY_ID = 123L;
    private static final String SOURCE_ID = "SOURCE_ID";

    @Test
    public void saveModelFromYangTask() {
        CommonModel newModel = defaultModel();
        context.setSourceYangTask(YangTaskEnums.YangTask.YANG_TASK_DEEPMATCHER_LOGS);
        context.setSourceId(SOURCE_ID);
        context.setSource(AuditAction.Source.YANG_TASK);
        GroupOperationStatus groupOperationStatus = storage.saveModel(newModel, context);
        assertEquals(OperationStatusType.OK, groupOperationStatus.getStatus());
        CommonModel model = groupOperationStatus.getRequestedModelByIndex(0)
                .orElseThrow(() -> new AssertionError("model is empty"));
        assertSource(YangTaskEnums.YangTask.YANG_TASK_DEEPMATCHER_LOGS, AuditAction.Source.YANG_TASK, SOURCE_ID,
                model);
    }

    @Test
    public void saveModelFromMBO() {
        CommonModel newModel = defaultModel();
        context.setSource(AuditAction.Source.MBO);
        GroupOperationStatus groupOperationStatus = storage.saveModel(newModel, context);
        assertEquals(OperationStatusType.OK, groupOperationStatus.getStatus());
        CommonModel model = groupOperationStatus.getRequestedModelByIndex(0)
                .orElseThrow(() -> new AssertionError("model is empty"));
        assertSource(null, AuditAction.Source.MBO, null, model);
    }

    private void assertSource(YangTaskEnums.YangTask yangTaskType, AuditAction.Source source, String sourceId,
                              CommonModel model) {
        assertEquals(YangTaskConverter.convertFromProto(yangTaskType), model.getSourceYangTask());
        assertEquals(source, model.getCreationSource());
        assertEquals(sourceId, model.getSourceId());
    }

    private CommonModel defaultModel() {
        return CommonModelBuilder.newBuilder()
            .id(0L)
            .category(CATEGORY_ID)
            .currentType(CommonModel.Source.GURU)
            .getModel();
    }

}
