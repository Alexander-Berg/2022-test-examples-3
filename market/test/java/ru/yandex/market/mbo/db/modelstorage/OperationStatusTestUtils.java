package ru.yandex.market.mbo.db.modelstorage;

import org.junit.Assert;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.List;

/**
 * @author dmserebr
 * @date 27/04/2019
 */
public class OperationStatusTestUtils {

    private OperationStatusTestUtils() { }

    public static void assertGroupOperationStatus(GroupOperationStatus actual, OperationStatusType type,
                                                  String message, List<Long> failedModelIds,
                                                  int requestedCount, int additionalCount) {
        Assert.assertEquals(type, actual.getStatus());
        Assert.assertEquals(message, actual.getStatusMessage());
        Assert.assertEquals(failedModelIds, actual.getFailedModelIds());
        Assert.assertEquals(requestedCount, actual.getRequestedModelStatuses().size());
        Assert.assertEquals(additionalCount, actual.getAdditionalModelStatues().size());
    }

    public static void assertOperationStatus(OperationStatus actual,
                                             OperationType operationType, OperationStatusType type, String message,
                                             Long modelId, Long newModelId, CommonModel model) {
        Assert.assertEquals(type, actual.getStatus());
        Assert.assertEquals(operationType, actual.getType());
        Assert.assertEquals(message, actual.getStatusMessage());
        Assert.assertEquals(modelId, actual.getModelId());
        Assert.assertEquals(newModelId, actual.getNewModelId());
        Assert.assertEquals(model, actual.getModel());
    }
}
