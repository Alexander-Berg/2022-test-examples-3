package ru.yandex.market.mbo.export.modelstorage;

import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipeContext;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipePart;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class DummyPipePart implements ModelPipePart {
    private ModelStorage.Model.Builder model;
    private Collection<ModelStorage.Model.Builder> modifications;
    private List<ModelStorage.Model.Builder> skus;

    @Override
    public void acceptModelsGroup(ModelPipeContext context) throws IOException {
        model = context.getModel();
        modifications = context.getModifications();
        skus = context.getSkus();
    }

    public CommonModel getModel() {
        return ModelProtoConverter.convert(model.build());
    }

    public List<CommonModel> getModifications() {
        return modifications.stream()
            .map(ModelStorage.Model.Builder::build)
            .map(ModelProtoConverter::convert)
            .collect(Collectors.toList());
    }

    public List<CommonModel> getSkus() {
        return skus.stream()
            .map(ModelStorage.Model.Builder::build)
            .map(ModelProtoConverter::convert)
            .collect(Collectors.toList());
    }
}
