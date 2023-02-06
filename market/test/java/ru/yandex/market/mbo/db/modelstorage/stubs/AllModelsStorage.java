package ru.yandex.market.mbo.db.modelstorage.stubs;

import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.Collection;
import java.util.Map;

/**
 * Интерфейс-маркер, который нужен для облегчения работы стабов в тест классах.
 *
 * @author s-ermakov
 */
public interface AllModelsStorage {

    Map<ModelIndexKey, CommonModel> getAllModelsMap();

    default Collection<CommonModel> getAllModels() {
        return getAllModelsMap().values();
    }
}
