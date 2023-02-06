package ru.yandex.market.gutgin.tms.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import ru.yandex.market.mbo.http.ModelStorage;

/**
 * @author s-ermakov
 */
public class ModelTransitionAssertions
    extends AbstractObjectAssert<ModelTransitionAssertions, ModelStorage.ModelTransition> {

    public ModelTransitionAssertions(ModelStorage.ModelTransition modelTransition) {
        super(modelTransition, ModelTransitionAssertions.class);
    }

    public ModelTransitionAssertions hasId(long id) {
        super.isNotNull();
        if (actual.getId() != id) {
            failWithMessage("Expected transition (%s) to have equal id. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), id, actual.getId());
        }
        return myself;
    }

    public ModelTransitionAssertions hasOldEntityId(long oldEntityId) {
        super.isNotNull();
        if (actual.getOldEntityId() != oldEntityId) {
            failWithMessage("Expected transition (%s) to have equal oldEntityId. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), oldEntityId, actual.getOldEntityId());
        }
        return myself;
    }

    public ModelTransitionAssertions hasNewEntityId(long newEntityId) {
        super.isNotNull();
        if (actual.getNewEntityId() != newEntityId) {
            failWithMessage("Expected transition (%s) to have equal newEntityId. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), newEntityId, actual.getNewEntityId());
        }
        return myself;
    }

    public ModelTransitionAssertions hasReason(ModelStorage.ModelTransition.TransitionReason reason) {
        super.isNotNull();
        if (actual.getReason() != reason) {
            failWithMessage("Expected transition (%s) to have equal reason. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), reason, actual.getReason());
        }
        return myself;
    }

    public ModelTransitionAssertions hasModelType(ModelStorage.ModelTransition.ModelType modelType) {
        super.isNotNull();
        if (actual.getModelType() != modelType) {
            failWithMessage("Expected transition (%s) to have equal modelType. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), modelType, actual.getModelType());
        }
        return myself;
    }

    public ModelTransitionAssertions hasType(ModelStorage.ModelTransition.TransitionType transitionType) {
        super.isNotNull();
        if (actual.getType() != transitionType) {
            failWithMessage("Expected transition (%s) to have equal transitionType. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), transitionType, actual.getType());
        }
        return myself;
    }

    public ModelTransitionAssertions isOldEntityDeleted() {
        super.isNotNull();
        if (!actual.getOldEntityDeleted()) {
            failWithMessage("Expected transition (%s) to be oldEntityDeleted. Actual is NOT.",
                toString(actual));
        }
        return myself;
    }

    public ModelTransitionAssertions isPrimaryTransition() {
        super.isNotNull();
        if (!actual.getPrimaryTransition()) {
            failWithMessage("Expected transition (%s) to be primary. Actual is NOT.",
                toString(actual));
        }
        return myself;
    }

    private static String toString(ModelStorage.ModelTransitionOrBuilder transition) {
        return "id:" + transition.getId()
            + ", oldEntity:" + transition.getOldEntityId()
            + ", newEntity:" + transition.getNewEntityId();
    }
}
