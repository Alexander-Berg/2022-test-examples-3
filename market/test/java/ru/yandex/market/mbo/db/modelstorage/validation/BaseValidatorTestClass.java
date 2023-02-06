package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.validation.dump.DumpValidationService;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.function.Consumer;

/**
 * @author s-ermakov
 */
public class BaseValidatorTestClass {
    protected static final long USER_ID = 1984;
    protected static final long CATEGORY_ID = 100500;
    protected ModelStorageServiceStub storage;
    protected ModelSaveContext saveContext = new ModelSaveContext(USER_ID);
    protected ModelValidationContextStub context;
    protected DumpValidationService dumpValidationService;

    @Before
    public void before() {
        storage = new ModelStorageServiceStub();
        dumpValidationService = new DumpValidationServiceStub();
        context = new ModelValidationContextStub(dumpValidationService);
    }

    protected CommonModel createModel(long id) {
        return createModel(id, builder -> {
        });
    }

    protected CommonModel createModel(long id, CommonModel.Source type) {
        return createModel(id, type, builder -> {
        });
    }

    protected CommonModel createModel(long id, Consumer<CommonModelBuilder> builder) {
        return createModel(id, CommonModel.Source.GURU, builder);
    }

    protected CommonModel createModel(long id, CommonModel.Source type, Consumer<CommonModelBuilder> builder) {
        CommonModelBuilder modelBuilder = CommonModelBuilder.newBuilder()
            .id(id).category(CATEGORY_ID)
            .source(type)
            .currentType(type);

        builder.accept(modelBuilder);

        return modelBuilder.getModel();
    }

    protected ModelChanges modelChanges(CommonModel after) {
        return after.isNewModel()
            ? new ModelChanges(null, after)
            : new ModelChanges(storage.searchById(after.getId()), after);
    }
}
