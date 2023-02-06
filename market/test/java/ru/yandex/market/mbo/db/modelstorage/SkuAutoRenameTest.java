package ru.yandex.market.mbo.db.modelstorage;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class SkuAutoRenameTest {

    private static final String OLD_NAME = "Axe Effect";
    private static final String NEW_NAME = "Sword Effect";
    private static final long CATEGORY_ID = 900L;
    private static final long GURU_ID = 100010L;
    private static final long SKU_ID = 110001L;
    private static final long PARAM_ID = 499021L;

    private IndexedModelStorageService modelStorageService;
    private CategoryParam nameParam;
    private CommonModel guru;
    private CommonModel sku;
    private ModelSaveGroup modelSaveGroup;
    private ModelSaveContext saveContext;

    @Before
    public void before() {
        nameParam = createNameParam();
        guru = initializeGuruModel();
        sku = initializeSkuModel();
        guru.getRelation(SKU_ID).get().setModel(sku);
        modelStorageService = new ModelStorageServiceStub(guru, sku);
        saveContext = new ModelSaveContext(1);
        modelSaveGroup = ModelSaveGroup.fromModels(guru);
        modelSaveGroup.addBeforeModels(singletonList(new CommonModel(guru)));
    }

    @Test
    public void testRenameWorksForSku() {
        assertEquals(OLD_NAME, nameOf(guru));
        assertEquals(OLD_NAME, nameOf(sku));

        setGuruName(NEW_NAME);
        CommonModel savedModel = saveGuru();

        assertEquals(NEW_NAME, nameOf(savedModel));
        assertEquals(NEW_NAME, nameOf(sku));
    }

    private String nameOf(CommonModel model) {
        return model.getTitle();
    }

    private CommonModel saveGuru() {
        CommonModel saved = modelStorageService.saveModels(modelSaveGroup, saveContext)
            .getRequestedModel(GURU_ID).get();
        sku = modelStorageService.getModel(CATEGORY_ID, SKU_ID).get();
        return saved;
    }

    private void setGuruName(String name) {
        guru.getSingleParameterValue(XslNames.NAME).setStringValue(WordUtil.defaultWords(name));
    }

    private CommonModel initializeGuruModel() {
        return CommonModelBuilder.newBuilder()
            .startModel()

            .startModelRelation()
            .id(SKU_ID)
            .categoryId(CATEGORY_ID)
            .type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()

            .id(GURU_ID).category(CATEGORY_ID).source(CommonModel.Source.GURU).currentType(CommonModel.Source.GURU)
            .parameters(singletonList(nameParam)).param(XslNames.NAME).setString(OLD_NAME)
            .getModel();
    }

    private CommonModel initializeSkuModel() {
        return CommonModelBuilder.newBuilder()
            .startModel()

            .startModelRelation()
            .id(GURU_ID)
            .categoryId(CATEGORY_ID)
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()

            .id(SKU_ID).category(CATEGORY_ID).source(CommonModel.Source.SKU).currentType(CommonModel.Source.SKU)
            .parameters(singletonList(nameParam)).param(XslNames.NAME).setString(OLD_NAME)
            .getModel();
    }

    private CategoryParam createNameParam() {
        CategoryParam param = new Parameter();
        param.setId(PARAM_ID);
        param.setXslName(XslNames.NAME);
        param.addName(WordUtil.defaultWord("Name"));
        param.setType(Param.Type.STRING);
        return param;
    }
}
