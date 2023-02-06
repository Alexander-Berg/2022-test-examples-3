package ru.yandex.market.mbo.db.modelstorage.stubs;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.YtModelIndexByIdPayload;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByIdReader;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author apluhin
 * @created 11/26/20
 */
public class YtModelIndexByIdReaderStub extends YtModelIndexByIdReader {

    private Supplier<AllModelsStorage> allModelsStorage;

    public YtModelIndexByIdReaderStub(Supplier<AllModelsStorage> allModelsStorage) {
        super(null, null, true);
        this.allModelsStorage = allModelsStorage;
    }

    @Override
    public List<YtModelIndexByIdPayload> selectFromIndex(MboIndexesFilter filter) {
        return allModelsStorage.get().getAllModels().stream()
            .filter(it -> checkInCollection(filter.getModelIds(), it.getId()))
            .filter(it -> checkInCollection(filter.getCategoryIds(), it.getCategoryId()))
            .filter(it -> checkInVariable(filter.getDeleted(), it.isDeleted()))
            .map(this::mapModel)
            .collect(Collectors.toList());
    }

    @NotNull
    private YtModelIndexByIdPayload mapModel(ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel it) {
        return new YtModelIndexByIdPayload(
            it.getId(),
            it.getCategoryId(),
            it.isDeleted(),
            0,
            it.getParentModelId(),
            it.getCurrentType(),
            false,
            it.isPublished()
        );
    }

    @Override
    public Long getCategoryId(long id, ReadStats readStats) {
        Map<ModelIndexKey, CommonModel> allModelsMap = allModelsStorage.get().getAllModelsMap();
        CommonModel model = allModelsMap.entrySet().stream()
            .filter(entry -> entry.getKey().getModelId().equals(id))
            .map(Map.Entry::getValue)
            .filter(Objects::nonNull)
            .max(Comparator.comparingLong(c -> c.getModificationDate().getTime()))
            .orElse(null);
        if (model != null) {
            return model.getCategoryId();
        }
        return null;
    }

    private <T> boolean checkInCollection(Collection<T> collection, T value) {
        if (!CollectionUtils.isEmpty(collection)) {
            return collection.contains(value);
        }
        return true;
    }

    private <T> boolean checkInVariable(T variableFromFilter, T value) {
        if (variableFromFilter != null) {
            return variableFromFilter.equals(value);
        }
        return true;
    }

}
