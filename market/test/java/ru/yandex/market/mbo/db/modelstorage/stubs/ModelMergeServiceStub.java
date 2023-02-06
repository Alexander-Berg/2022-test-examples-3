package ru.yandex.market.mbo.db.modelstorage.stubs;

import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.db.modelstorage.merge.ModelMergeServiceImpl;
import ru.yandex.market.mbo.db.modelstorage.params.ModelParamsService;

/**
 * @author york
 * @since 10.10.2017
 */
public class ModelMergeServiceStub extends ModelMergeServiceImpl {

    public ModelMergeServiceStub(ModelStoreInterface modelStore) {
        super(new ModelParamsService(), modelStore);
    }
}
