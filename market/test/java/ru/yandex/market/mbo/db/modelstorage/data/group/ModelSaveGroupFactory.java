package ru.yandex.market.mbo.db.modelstorage.data.group;

import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransition;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.Collection;

/**
 * Used for testing purposes (to share private constructors).
 * @author york
 * @since 04.07.2018
 */
public class ModelSaveGroupFactory {

    private ModelSaveGroupFactory() { }

    public static ModelSaveGroup create(Collection<CommonModel> requestedModels,
                                 Collection<CommonModel> additionalModels) {
        return new ModelSaveGroupExt(requestedModels, additionalModels);
    }

    public static ModelSaveGroup create(Collection<CommonModel> requestedModels,
                                 Collection<CommonModel> additionalModels,
                                 Collection<ModelTransition> modelTransitions) {
        return new ModelSaveGroupExt(requestedModels, additionalModels, modelTransitions);
    }

    private static class ModelSaveGroupExt extends ModelSaveGroup {

        private ModelSaveGroupExt(Collection<CommonModel> requestedModels,
                                  Collection<CommonModel> additionalModels) {
            super(requestedModels, additionalModels);
        }

        private ModelSaveGroupExt(Collection<CommonModel> requestedModels,
                                  Collection<CommonModel> additionalModels,
                                  Collection<ModelTransition> modelTransitions) {
            super(requestedModels, additionalModels, modelTransitions);
        }
    }
}
