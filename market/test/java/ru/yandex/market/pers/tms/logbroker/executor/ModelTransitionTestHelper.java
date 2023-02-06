package ru.yandex.market.pers.tms.logbroker.executor;

import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ru.yandex.market.mbo.http.ModelStorage;

public class ModelTransitionTestHelper {
    private ModelTransitionTestHelper() {
    }

    public static final long id = 1;
    public static final long actionId = 2;
    public static final boolean primaryTransition = true;
    public static final Timestamp timestamp = new Timestamp(new Date().getTime() - TimeUnit.MINUTES.toMillis(30));
    public static final long newEntityId = 100012;
    public static final boolean oldEntityDeleted = false;
    public static final long oldEntityId = 123;
    public static final ModelStorage.ModelTransition.TransitionReason transitionReason =
        ModelStorage.ModelTransition.TransitionReason.MODEL_SPLIT_TO_MODIF;
    public static ModelStorage.ModelTransition.TransitionType transitionType =
        ModelStorage.ModelTransition.TransitionType.SPLIT;

    public static ModelStorage.ModelTransition generateModelTransition(long id,
                                                                       ModelStorage.ModelTransition.ModelType modelType,
                                                                       ModelStorage.ModelTransition.TransitionReason transitionReason,
                                                                       ModelStorage.ModelTransition.TransitionType transitionType,
                                                                       boolean primaryTransition) {
        return generateModelTransition(id, modelType, transitionReason, transitionType, primaryTransition, oldEntityId + id, newEntityId + id);
    }

    public static ModelStorage.ModelTransition generateModelTransition(long id,
                                                                       ModelStorage.ModelTransition.ModelType modelType,
                                                                       ModelStorage.ModelTransition.TransitionReason transitionReason,
                                                                       ModelStorage.ModelTransition.TransitionType transitionType,
                                                                       boolean primaryTransition,
                                                                       long oldEntityId,
                                                                       long newEntityId) {
        // идентификаторы моделей однозначно определены переездом
        return ModelStorage.ModelTransition.newBuilder()
            .setActionId(actionId)
            .setPrimaryTransition(primaryTransition)
            .setModelType(modelType)
            .setDate(timestamp.getTime())
            .setId(id)
            .setNewEntityId(newEntityId)
            .setOldEntityDeleted(oldEntityDeleted)
            .setOldEntityId(oldEntityId)
            .setReason(transitionReason)
            .setType(transitionType)
            .build();
    }

    public static ModelStorage.ModelTransition generateModelTransition(long id,
                                                                       ModelStorage.ModelTransition.ModelType modelType,
                                                                       ModelStorage.ModelTransition.TransitionReason transitionReason,
                                                                       ModelStorage.ModelTransition.TransitionType transitionType) {
        return generateModelTransition(id, modelType, transitionReason, transitionType, primaryTransition);
    }

    public static ModelStorage.ModelTransition generateSimpleModelTransition(long id) {
        return generateModelTransition(id,
            ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            ModelStorage.ModelTransition.TransitionType.DUPLICATE);
    }

    public static ModelStorage.ModelTransition generateSimpleModelTransition(long id, long oldEntityId, long newEntityId) {
        return generateModelTransition(id,
            ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            ModelStorage.ModelTransition.TransitionType.DUPLICATE,
            primaryTransition,
            oldEntityId,
            newEntityId);
    }

    public static ModelStorage.ModelTransition generateSimpleModelRevertTransition(long id, long oldEntityId, long newEntityId) {
        return generateModelTransition(id,
            ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            ModelStorage.ModelTransition.TransitionType.REVERT,
            primaryTransition,
            oldEntityId,
            newEntityId);
    }
}
