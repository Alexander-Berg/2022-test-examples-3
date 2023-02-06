package ru.yandex.market.mbo.db.modelstorage.stubs;

import java.util.Collection;
import java.util.List;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.db.modelstorage.StatsIndexedModelQueryService;
import ru.yandex.market.mbo.db.modelstorage.YtSaasIndexesWrapper;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

public class StatsIndexedModelQueryServiceStub extends StatsIndexedModelQueryService {

    public StatsIndexedModelQueryServiceStub(ModelStoreInterface modelStore,
                                             YtSaasIndexesWrapper ytSaasIndexesWrapper) {
        super(modelStore, ytSaasIndexesWrapper);
    }

    @Override
    public List<CommonModel> getModelsPage(MboIndexesFilter mboIndexesFilter, ReadStats readStats) {
        if (modelStore instanceof ModelStoreInterfaceStub) {
            ModelStoreInterfaceStub storeInterfaceStub = (ModelStoreInterfaceStub) modelStore;
            Collection<Long> modelIds = mboIndexesFilter.getModelIds();
            if (CollectionUtils.isNonEmpty(modelIds)) {
                return storeInterfaceStub.getModelById(modelIds);
            }
            Collection<Long> parentIds = mboIndexesFilter.getParentIds();
            if (CollectionUtils.isNonEmpty(parentIds)) {
                return storeInterfaceStub.getModelByParentId(parentIds);
            }
        }
        return super.getModelsPage(mboIndexesFilter, readStats);
    }

}
