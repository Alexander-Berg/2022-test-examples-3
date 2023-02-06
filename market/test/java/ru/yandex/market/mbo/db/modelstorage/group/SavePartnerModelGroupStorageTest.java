package ru.yandex.market.mbo.db.modelstorage.group;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.db.modelstorage.validation.DeletedModelValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelTitleValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationService;
import ru.yandex.market.mbo.db.modelstorage.validation.NameValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.ParameterValueValidator;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.Arrays;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class SavePartnerModelGroupStorageTest extends BaseGroupStorageUpdatesTest {

    @Override
    protected ModelValidationService createModelValidationService() {
        ModelValidationService modelValidationService = super.createModelValidationService(
            Arrays.asList(
                new ModelTitleValidator(),
                new NameValidator(),
                new ParameterValueValidator(),
                new DeletedModelValidator()
            )
        );
        return modelValidationService;
    }

    @Test
    public void testCreateEmptyPartnerModel() {
        CommonModel emptyModel = createModel(0, 1, 1, CommonModel.Source.PARTNER, b -> {
        });

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(emptyModel), context);
        MboAssertions.assertThat(status).isOk();
    }

    @Test
    public void testUpdateEmptyPartnerModel() {
        CommonModel model = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
        });
        model.setDoubtful(false);
        putToStorage(model);

        CommonModel copy = new CommonModel(model);
        copy.setDoubtful(true);

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(copy), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel actual = searchById(1L);
        MboAssertions.assertThat(model).hasDoubtful(false);
        MboAssertions.assertThat(actual).hasDoubtful(true);
    }

    @Test
    public void testDeletePartnerModel() {
        CommonModel model = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("partner model");
        });
        putToStorage(model);

        CommonModel copy = new CommonModel(model);
        copy.setDeleted(true);

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(copy), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel actual = searchById(1L);
        Assertions.assertThat(actual.isDeleted()).isTrue();
    }

    @Test
    public void testAddSkuToPartnerModel() {
        CommonModel model = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("partner model");
        });
        putToStorage(model);

        CommonModel sku = createModel(0, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("partner sku")
                .withSkuParentRelation(model);
        });

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(sku), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel actualModel = searchById(model.getId());
        CommonModel actualSku = searchById(sku.getId());
        MboAssertions.assertThat(actualModel).containsSkuRelation(actualSku);
        MboAssertions.assertThat(actualSku).containsSkuParentRelation(actualModel);
    }

    @Test
    public void testCreatePartnerModelWithSku() {
        CommonModel model = createModel(-1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("partner model")
                .withSkuRelations(1, -2);
        });
        CommonModel sku = createModel(-2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("partner sku")
                .withSkuParentRelation(1, -1);
        });

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(model, sku), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel actualModel = searchById(model.getId());
        CommonModel actualSku = searchById(sku.getId());
        MboAssertions.assertThat(actualModel).containsSkuRelation(actualSku);
        MboAssertions.assertThat(actualSku).containsSkuParentRelation(actualModel);
    }

    @Test
    public void testCreatePartnerModelWillCreateIdInCorrectRange() {
        CommonModel model = createModel(-1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("partner model")
                .withSkuRelations(1, -2);
        });
        CommonModel sku = createModel(-2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("partner sku")
                .withSkuParentRelation(1, -1);
        });

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(model, sku), context);
        MboAssertions.assertThat(status).isOk();

        MboAssertions.assertThat(model).hasIdInIntRange();
        MboAssertions.assertThat(sku).hasIdInLongRange();
    }

    @Test
    public void testDeleteModelWillAlsoDeleteSku() {
        CommonModel model = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("partner model")
                .withSkuRelations(1, 2);
        });
        CommonModel sku = createModel(2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("partner sku")
                .withSkuParentRelation(1, 1);
        });
        putToStorage(model, sku);

        model.setDeleted(true);

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(model), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel actualModel = searchById(model.getId());
        CommonModel actualSku = searchById(sku.getId());
        MboAssertions.assertThat(actualModel).isDeleted();
        MboAssertions.assertThat(actualSku).isDeleted();
    }

    @Test
    public void testDeleteSku() {
        CommonModel model = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("partner model")
                .withSkuRelations(1, 2);
        });
        CommonModel sku = createModel(2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("partner sku")
                .withSkuParentRelation(1, 1);
        });
        putToStorage(model, sku);

        sku.setDeleted(true);

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(sku), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel actualSku = searchById(sku.getId());
        MboAssertions.assertThat(actualSku).isDeleted();
    }

    @Test
    public void testSavePartnerWithoutTitle() {
        CommonModel model = createModel(-1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.withSkuRelations(1, -2);
        });
        CommonModel sku = createModel(-2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("Model")
                .withSkuParentRelation(1, -1);
        });

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(model, sku), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel actualModel = searchById(model.getId());
        MboAssertions.assertThat(actualModel).hasTitle("Model");
    }

    @Test
    public void testSavePartnerWithoutTitleAndNotEmptyMostCommonTitle() {
        CommonModel model = createModel(-1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.withSkuRelations(1, -2, -3);
        });
        CommonModel sku1 = createModel(-2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("Model #1")
                .withSkuParentRelation(1, -1);
        });
        CommonModel sku2 = createModel(-3, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("Model #2")
                .withSkuParentRelation(1, -1);
        });

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(model, sku1, sku2), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel actualModel = searchById(model.getId());
        MboAssertions.assertThat(actualModel).hasTitle("Model");
    }

    @Test
    public void testSavePartnerWithoutTitleAndEmptyMostCommonTitle() {
        CommonModel model = createModel(-1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.withSkuRelations(1, -2, -3);
        });
        CommonModel sku1 = createModel(-2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("Model#1")
                .withSkuParentRelation(1, -1);
        });
        CommonModel sku2 = createModel(-3, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("Model#2")
                .withSkuParentRelation(1, -1);
        });

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(model, sku1, sku2), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel actualModel = searchById(model.getId());
        MboAssertions.assertThat(actualModel).hasTitle("");
    }

    /**
     * Тест проверят, что переезд psku из одной pmodel в другую pmodel успешен.
     * Начальное состояние:
     * pmodel (id: 1) <-> psku (id: 2)
     * Конечное состояние:
     * pmodel (id: 1)
     * pmodel (id: -1) <-> psku (id: 2)
     */
    @Test
    public void testMovePskuFromOnePModelToAnother() {
        CommonModel pmodel = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel (old)").withSkuRelations(1, 2);
        });
        CommonModel psku = createModel(2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku").withSkuParentRelation(pmodel);
        });

        putToStorage(pmodel, psku);

        CommonModel newPmodel = createModel(-1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel (new)").withSkuRelations(psku);
        });
        psku.clearRelations();
        psku.addRelation(new ModelRelation(newPmodel.getId(), 1, ModelRelation.RelationType.SKU_PARENT_MODEL));

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(newPmodel, psku), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel oldPmodelActual = searchById(pmodel.getId());
        CommonModel newPmodelActual = searchById(newPmodel.getId());
        CommonModel pskuActual = searchById(psku.getId());

        MboAssertions.assertThat(oldPmodelActual).doesNotContainRelations();
        MboAssertions.assertThat(newPmodelActual).containsOnlySkuRelation(pskuActual);
        MboAssertions.assertThat(pskuActual).containsSkuParentRelation(newPmodelActual);
    }

    /**
     * Тест проверят, что переезд psku из одной pmodel в другую pmodel успешен.
     * <p>
     * Начальное состояние:
     * pmodel (id: 1) <-> psku (id: 2), psku (id: 3)
     * pmodel (id: 4)
     * Конечное состояние:
     * pmodel (id: 1) <-> psku (id: 2)
     * pmodel (id: 4) <-> psku (id: 3)
     */
    @Test
    public void testMovePSKuFromOnePModelWithMultiplePSkuToAnother() {
        CommonModel pmodel1 = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel1").withSkuRelations(1, 2, 3);
        });
        CommonModel psku12 = createModel(2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku12").withSkuParentRelation(pmodel1);
        });
        CommonModel psku22 = createModel(3, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku22").withSkuParentRelation(pmodel1);
        });
        CommonModel pmodel2 = createModel(4, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel2");
        });
        putToStorage(pmodel1, psku22, psku12, pmodel2);

        psku22.clearRelations();
        psku22.addRelation(new ModelRelation(pmodel2.getId(), 1, ModelRelation.RelationType.SKU_PARENT_MODEL));

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(psku22), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel pmodel1Actual = searchById(pmodel1.getId());
        CommonModel psku12Actual = searchById(psku12.getId());
        CommonModel psku22Actual = searchById(psku22.getId());
        CommonModel pmodel2Actual = searchById(pmodel2.getId());

        MboAssertions.assertThat(pmodel1Actual).containsOnlySkuRelation(psku12);
        MboAssertions.assertThat(pmodel2Actual).containsOnlySkuRelation(psku22);

        MboAssertions.assertThat(psku12Actual).containsSkuParentRelation(pmodel1Actual);
        MboAssertions.assertThat(psku22Actual).containsSkuParentRelation(pmodel2Actual);
    }

    /**
     * Тест проверят, что переезд psku из одной pmodel в другую pmodel успешен.
     * Старую модель мы пересохраняем тоже.
     * <p>
     * Начальное состояние:
     * pmodel (id: 1) <-> psku (id: 2)
     * pmodel (id: 3)
     * Конечное состояние:
     * pmodel (id: 1, changed)
     * pmodel (id: 3) <-> psku (id: 2)
     */
    @Test
    public void testMovePskuFromOneModelToAnother() {
        CommonModel pmodel1 = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel1").withSkuRelations(1, 2);
        });
        CommonModel psku2 = createModel(2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku2").withSkuParentRelation(pmodel1);
        });
        CommonModel pmodel3 = createModel(3, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel3");
        });
        putToStorage(pmodel1, psku2, pmodel3);

        pmodel1.putParameterValues(new ParameterValues(1, "xsl-name", Param.Type.ENUM, 100L));

        psku2.clearRelations();
        psku2.addRelation(new ModelRelation(pmodel3.getId(), 1, ModelRelation.RelationType.SKU_PARENT_MODEL));

        GroupOperationStatus status1 = storage.saveModels(ModelSaveGroup.fromModels(pmodel1, psku2), context);
        MboAssertions.assertThat(status1).isOk();

        CommonModel pmodel1Actual = searchById(pmodel1.getId());
        CommonModel psku2Actual = searchById(psku2.getId());
        CommonModel pmodel3Actual = searchById(pmodel3.getId());

        MboAssertions.assertThat(pmodel3Actual).containsOnlySkuRelation(psku2Actual);
        MboAssertions.assertThat(psku2Actual).containsSkuParentRelation(pmodel3Actual);
        MboAssertions.assertThat(pmodel1Actual).doesNotContainRelations();
    }

    /**
     * Тест проверят, что переезд psku из одной pmodel в другую pmodel успешен.
     * <p>
     * Начальное состояние:
     * pmodel (id: 1) <-> psku (id: 2)
     * pmodel (id: 3)
     * Конечное состояние:
     * pmodel (id: 1, deleted)
     * pmodel (id: 3) <-> psku (id: 2)
     */
    @Test
    public void testMovePskuFromOneModelToAnotherAndDeletePModel() {
        CommonModel pmodel1 = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel1").withSkuRelations(1, 2);
        });
        CommonModel psku2 = createModel(2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku2").withSkuParentRelation(pmodel1);
        });
        CommonModel pmodel3 = createModel(3, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel3");
        });
        putToStorage(pmodel1, psku2, pmodel3);

        pmodel1.setDeleted(true);

        psku2.clearRelations();
        psku2.addRelation(new ModelRelation(pmodel3.getId(), 1, ModelRelation.RelationType.SKU_PARENT_MODEL));

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(pmodel1, psku2), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel pmodel1Actual = searchById(pmodel1.getId());
        CommonModel psku2Actual = searchById(psku2.getId());
        CommonModel pmodel3Actual = searchById(pmodel3.getId());

        MboAssertions.assertThat(pmodel3Actual).containsOnlySkuRelation(psku2Actual);
        MboAssertions.assertThat(psku2Actual).containsSkuParentRelation(pmodel3Actual);
        // проверяем только pmodel1 удалена, сохранилась ли у нее связь до psku2 нам не важно
        MboAssertions.assertThat(pmodel1Actual).isDeleted();
    }

    /**
     * Тест проверят, что переезд psku из одной pmodel в одной категории в другую pmodel другой категории успешен.
     * <p>
     * Начальное состояние:
     * pmodel (id: 1, category: 1) <-> psku (id: 2, category: 1)
     * pmodel (id: 3, category: 2)
     * Конечное состояние:
     * pmodel (id: 1, category: 1)
     * pmodel (id: 3, category: 2) <-> psku (id: 2, category: 2)
     */
    @Test
    public void testMovePskuFromOneCategoryToAnother() {
        CommonModel pmodel1 = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel1").withSkuRelations(1, 2);
        });
        CommonModel psku2 = createModel(2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku2").withSkuParentRelation(pmodel1);
        });
        CommonModel pmodel3 = createModel(3, 2, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel3");
        });
        putToStorage(pmodel1, psku2, pmodel3);

        psku2.setCategoryId(2);
        psku2.clearRelations();
        psku2.addRelation(new ModelRelation(pmodel3.getId(), 2, ModelRelation.RelationType.SKU_PARENT_MODEL));

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(psku2), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel pmodel1Actual = searchById(pmodel1.getId());
        CommonModel psku2Actual = searchById(psku2.getId());
        CommonModel pmodel3Actual = searchById(pmodel3.getId());

        MboAssertions.assertThat(pmodel1Actual).doesNotContainRelations().hasCategoryid(1);
        MboAssertions.assertThat(pmodel3Actual).containsOnlySkuRelation(psku2Actual).hasCategoryid(2);
        MboAssertions.assertThat(psku2Actual).containsSkuParentRelation(pmodel3Actual).hasCategoryid(2);
    }

    /**
     * Тест проверят большое разделение psku по несколько pmodel.
     * <p>
     * Начальное состояние:
     * pmodel (id: 1, category: 1) <-> psku (id: 11, category: 1), psku (id: 12, category: 1)
     * pmodel (id: 2, category: 1) <-> psku (id: 21, category: 1), psku (id: 22, category: 1)
     * pmodel (id: 3, category: 1) <-> psku (id: 31, category: 1)
     * pmodel (id: 4, category: 2) <-> psku (id: 41, category: 2), psku (id: 42, category: 2)
     * Конечное состояние:
     * pmodel (id: 1, category: 1) <-> psku(id: 11, category: 1),psku (id: 22, category: 1)*,psku (id: 42, category: 1)*
     * pmodel (id: 2, category: 2)*<-> psku(id: 21, category: 2)*,psku (id: 31, category: 2)*
     * pmodel (id: 3, category: 1)
     * pmodel (id: 4, category: 2) <-> psku(id: 41, category: 2),psku (id: 12, category: 2)*
     * ---
     * * (звездочка) означает, что модель была изменена
     */
    @Test
    public void testComplexSplitPskuToNewPModels() {
        // assume
        CommonModel pmodel1 = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel1").withSkuRelations(1, 11, 12);
        });
        CommonModel pmodel2 = createModel(2, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel2").withSkuRelations(1, 21, 22);
        });
        CommonModel pmodel3 = createModel(3, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel3").withSkuRelations(1, 31);
        });
        CommonModel pmodel4 = createModel(4, 2, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel4").withSkuRelations(2, 41, 42);
        });

        CommonModel psku11 = createModel(11, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku11").withSkuParentRelation(pmodel1);
        });
        CommonModel psku12 = createModel(12, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku11").withSkuParentRelation(pmodel1);
        });
        CommonModel psku21 = createModel(21, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku11").withSkuParentRelation(pmodel2);
        });
        CommonModel psku22 = createModel(22, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku11").withSkuParentRelation(pmodel2);
        });
        CommonModel psku31 = createModel(31, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku11").withSkuParentRelation(pmodel3);
        });
        CommonModel psku41 = createModel(41, 2, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku11").withSkuParentRelation(pmodel4);
        });
        CommonModel psku42 = createModel(42, 2, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku11").withSkuParentRelation(pmodel4);
        });

        putToStorage(pmodel1, pmodel2, pmodel3, pmodel4, psku11, psku12, psku21, psku22, psku31, psku41, psku42);

        psku22.clearRelations();
        psku22.addRelation(new ModelRelation(1, 1, ModelRelation.RelationType.SKU_PARENT_MODEL));

        psku42.setCategoryId(1);
        psku42.clearRelations();
        psku42.addRelation(new ModelRelation(1, 1, ModelRelation.RelationType.SKU_PARENT_MODEL));

        pmodel2.setCategoryId(2);
        pmodel2.clearRelations();
        pmodel2.addRelation(new ModelRelation(21, 2, ModelRelation.RelationType.SKU_MODEL));
        pmodel2.addRelation(new ModelRelation(31, 2, ModelRelation.RelationType.SKU_MODEL));

        psku21.setCategoryId(2);
        psku21.clearRelations();
        psku21.addRelation(new ModelRelation(2, 2, ModelRelation.RelationType.SKU_PARENT_MODEL));

        psku31.setCategoryId(2);
        psku31.clearRelations();
        psku31.addRelation(new ModelRelation(2, 2, ModelRelation.RelationType.SKU_PARENT_MODEL));

        psku12.setCategoryId(2);
        psku12.clearRelations();
        psku12.addRelation(new ModelRelation(4, 2, ModelRelation.RelationType.SKU_PARENT_MODEL));

        // act
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(psku22, psku42, pmodel2, psku21, psku31, psku12);
        GroupOperationStatus status = storage.saveModels(saveGroup, context);
        MboAssertions.assertThat(status).isOk();

        // assert
        MboAssertions.assertThat(searchById(1)).hasCategoryid(1).containsExactlyInAnyOrderRelations(
            new ModelRelation(11, 1, ModelRelation.RelationType.SKU_MODEL),
            new ModelRelation(22, 1, ModelRelation.RelationType.SKU_MODEL),
            new ModelRelation(42, 1, ModelRelation.RelationType.SKU_MODEL)
        );
        MboAssertions.assertThat(searchById(2)).hasCategoryid(2).containsExactlyInAnyOrderRelations(
            new ModelRelation(21, 2, ModelRelation.RelationType.SKU_MODEL),
            new ModelRelation(31, 2, ModelRelation.RelationType.SKU_MODEL)
        );
        MboAssertions.assertThat(searchById(3)).hasCategoryid(1).doesNotContainRelations();
        MboAssertions.assertThat(searchById(4)).hasCategoryid(2).containsExactlyInAnyOrderRelations(
            new ModelRelation(41, 2, ModelRelation.RelationType.SKU_MODEL),
            new ModelRelation(12, 2, ModelRelation.RelationType.SKU_MODEL)
        );

        MboAssertions.assertThat(searchById(11)).hasCategoryid(1).containsSkuParentRelation(1, 1);
        MboAssertions.assertThat(searchById(22)).hasCategoryid(1).containsSkuParentRelation(1, 1);
        MboAssertions.assertThat(searchById(42)).hasCategoryid(1).containsSkuParentRelation(1, 1);

        MboAssertions.assertThat(searchById(21)).hasCategoryid(2).containsSkuParentRelation(2, 2);
        MboAssertions.assertThat(searchById(31)).hasCategoryid(2).containsSkuParentRelation(2, 2);

        MboAssertions.assertThat(searchById(41)).hasCategoryid(2).containsSkuParentRelation(4, 2);
        MboAssertions.assertThat(searchById(12)).hasCategoryid(2).containsSkuParentRelation(4, 2);
    }

    @Test
    public void testMovePskuFromOneCategoryToAnotherAndBack() {
        CommonModel pmodel1 = createModel(1, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel1").withSkuRelations(1, 2);
        });
        CommonModel psku2 = createModel(2, 1, 1, CommonModel.Source.PARTNER_SKU, b -> {
            b.title("psku2").withSkuParentRelation(pmodel1);
        });
        CommonModel pmodel3 = createModel(3, 2, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel3");
        });
        CommonModel pmodel4 = createModel(4, 1, 1, CommonModel.Source.PARTNER, b -> {
            b.title("pmodel4");
        });
        putToStorage(pmodel1, psku2, pmodel3, pmodel4);

        psku2.setCategoryId(2);
        psku2.clearRelations();
        psku2.addRelation(new ModelRelation(pmodel3.getId(), 2, ModelRelation.RelationType.SKU_PARENT_MODEL));

        GroupOperationStatus status = storage.saveModels(ModelSaveGroup.fromModels(psku2), context);
        MboAssertions.assertThat(status).isOk();

        CommonModel pmodel1Actual = searchById(pmodel1.getId());
        CommonModel psku2Actual = searchById(psku2.getId());
        CommonModel pmodel3Actual = searchById(pmodel3.getId());
        CommonModel pmodel4Actual = searchById(pmodel4.getId());

        MboAssertions.assertThat(pmodel1Actual).doesNotContainRelations().hasCategoryid(1);
        MboAssertions.assertThat(pmodel3Actual).containsOnlySkuRelation(psku2Actual).hasCategoryid(2);
        MboAssertions.assertThat(psku2Actual).containsSkuParentRelation(pmodel3Actual).hasCategoryid(2);
        MboAssertions.assertThat(psku2Actual).isNotDeleted();
        MboAssertions.assertThat(pmodel4Actual).doesNotContainRelations().hasCategoryid(1);

        CommonModel deletedInOldCategory = storage.getModel(1, 2, null).orElseThrow(IllegalStateException::new);
        MboAssertions.assertThat(deletedInOldCategory).isDeleted();

        // moving back
        psku2Actual.setCategoryId(1);
        psku2Actual.clearRelations();
        psku2Actual.addRelation(new ModelRelation(pmodel4.getId(), 1, ModelRelation.RelationType.SKU_PARENT_MODEL));

        status = storage.saveModels(ModelSaveGroup.fromModels(psku2Actual), context);
        MboAssertions.assertThat(status).isOk();

        pmodel1Actual = searchById(pmodel1.getId());
        psku2Actual = searchById(psku2.getId());
        pmodel3Actual = searchById(pmodel3.getId());
        pmodel4Actual = searchById(pmodel4.getId());

        MboAssertions.assertThat(pmodel3Actual).doesNotContainRelations().hasCategoryid(2);
        MboAssertions.assertThat(pmodel1Actual).doesNotContainRelations().hasCategoryid(1);
        MboAssertions.assertThat(psku2Actual).containsSkuParentRelation(pmodel4Actual).hasCategoryid(1);
        MboAssertions.assertThat(pmodel4Actual).containsOnlySkuRelation(psku2Actual).hasCategoryid(1);
        Assert.assertFalse(psku2Actual.isDeleted());

        deletedInOldCategory = storage.getModel(2, 2, null).orElseThrow(IllegalStateException::new);
        MboAssertions.assertThat(deletedInOldCategory).isDeleted();
    }
}
