package ru.yandex.market.mbo.db.modelstorage.stubs;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.ModelOrBuilder;
import ru.yandex.market.mbo.solr.update.UpdateModelIndexException;
import ru.yandex.market.mbo.solr.update.UpdateModelIndexInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author moskovkin@yandex-team.ru
 * @since 26.04.18
 */
public class UpdateModelIndexInterfaceStub implements UpdateModelIndexInterface {
    private List<ModelStorage.Model> indexed = new ArrayList<>();

    @Override
    public void index(Collection<? extends ModelStorage.ModelOrBuilder> models) throws UpdateModelIndexException {
        List<ModelStorage.Model> copy = models.stream()
            .map(model -> {
                if (model instanceof ModelStorage.Model.Builder) {
                    return ((ModelStorage.Model.Builder) model).build();
                } else {
                    return ModelStorage.Model.newBuilder((ModelStorage.Model) model).build();
                }
            })
            .collect(Collectors.toList());
        indexed.addAll(copy);
    }

    @Override
    public void removeIndex(Collection<? extends ModelOrBuilder> models) throws UpdateModelIndexException {
        List<ModelStorage.Model> copy = models.stream()
            .map(model -> {
                if (model instanceof ModelStorage.Model.Builder) {
                    return ((ModelStorage.Model.Builder) model).build();
                } else {
                    return ModelStorage.Model.newBuilder((ModelStorage.Model) model).build();
                }
            })
            .collect(Collectors.toList());
        indexed.removeAll(copy);
    }

    public List<ModelStorage.Model> getIndexed() {
        return new ArrayList<>(indexed);
    }
}
