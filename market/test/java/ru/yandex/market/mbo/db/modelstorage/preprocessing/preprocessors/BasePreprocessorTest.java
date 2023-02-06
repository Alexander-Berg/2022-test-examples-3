package ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors;

import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;

import java.util.Date;
import java.util.function.Consumer;

public class BasePreprocessorTest {
    protected static final long USER_ID = 100;
    protected static final long CATEGORY_HID = 666;

    ParametersBuilder<CommonModelBuilder<CommonModel>> parametersBuilder;
    CategoryParametersServiceClient categoryParametersServiceClient;
    ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);

    protected void createParametersBuilder() {
        parametersBuilder = ParametersBuilder.defaultBuilder();
    }

    protected void createCategoryParametersServiceClient() {
        categoryParametersServiceClient = CategoryParametersServiceClientStub
            .ofCategoryParams(CATEGORY_HID, parametersBuilder.getParameters());
    }

    protected void before() {
        createParametersBuilder();
        createCategoryParametersServiceClient();
    }

    protected CommonModel model(long id) {
        return model(id, null);
    }

    protected CommonModel modification(long id,
                                       long parentId) {
        return modification(id, parentId, null);
    }

    protected CommonModel sku(long id,
                              CommonModel commonModel) {
        return sku(id, commonModel, null);
    }

    protected CommonModel model(long id, Consumer<CommonModelBuilder<CommonModel>> builderConsumer) {
        return model(id, CATEGORY_HID, builderConsumer);
    }

    protected CommonModel modification(long id,
                                       long parentId,
                                       Consumer<CommonModelBuilder<CommonModel>> builderConsumer) {
        return model(id, CATEGORY_HID, builder -> {
            builder.parentModelId(parentId);
            if (builderConsumer != null) {
                builderConsumer.accept(builder);
            }
        });
    }

    protected CommonModel sku(long id,
                              CommonModel parentModel,
                              Consumer<CommonModelBuilder<CommonModel>> builderConsumer) {
        return model(id, CATEGORY_HID, builder -> {
            builder.modelRelation(parentModel.getId(), parentModel.getCategoryId(),
                ModelRelation.RelationType.SKU_PARENT_MODEL);
            builder.currentType(CommonModel.Source.SKU);
            parentModel.addRelation(new ModelRelation(id, parentModel.getCategoryId(),
                ModelRelation.RelationType.SKU_MODEL));
            if (builderConsumer != null) {
                builderConsumer.accept(builder);
            }
        });
    }

    protected CommonModel model(long id, long categoryId, Consumer<CommonModelBuilder<CommonModel>> builderConsumer) {
        CommonModelBuilder<CommonModel> commonModelBuilder = parametersBuilder.endParameters();
        commonModelBuilder
            .id(id)
            .category(categoryId)
            .createdDate(new Date());
        if (builderConsumer != null) {
            builderConsumer.accept(commonModelBuilder);
        }
        return commonModelBuilder.endModel();
    }
}
