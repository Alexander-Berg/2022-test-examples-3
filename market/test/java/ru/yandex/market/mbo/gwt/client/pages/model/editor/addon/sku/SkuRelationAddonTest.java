package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import javolution.testing.AssertionException;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ModelDataLoadedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ModelModifiedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ModelRenderedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ModelUIGeneratedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.RemoteModelChangedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.save.PopulateModelSaveSyncEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuDeletionConfirmedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuRelationChangedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuRelationCreationEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuRelationReorderEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableModel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.SkuRelationWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Тестируем, что вкладка по редактированию SKU работает корректно.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuRelationAddonTest extends BaseSkuAddonTest {

    private static final ModelRelation EXPERIMENTAL_RELATION =
        new ModelRelation(66, 33, ModelRelation.RelationType.EXPERIMENTAL_MODEL);

    @Test
    public void testRelationAdded() throws Exception {
        // добавляем новый sku
        bus.fireEvent(new SkuRelationCreationEvent());

        CommonModel editedModel = editableModel.getModel();
        CommonModel originalModel = editableModel.getOriginalModel();
        List<ModelRelation> newRelations = editedModel.getRelations();
        List<ModelRelation> oldRelations = originalModel.getRelations();

        assertEquals("New relations size doesn't add to model.",
            oldRelations.size() + 1, newRelations.size());

        ModelRelation newRelation = newRelations.stream()
            .filter(ModelRelation::isNewRelation)
            .findFirst()
            .orElseThrow(() -> new AssertionException("Failed to find created relation"));

        // у новой связи обязательно должен быть выставлена модель
        CommonModel createdSku = newRelation.getModel();
        Assert.assertNotNull(createdSku);

        // у модели должна быть выставлена связь до родительской модели
        ModelRelation relationToBaseModel = createdSku.getRelation(editedModel.getId())
            .orElseThrow(() -> new AssertionException("No relation to base model: " + editedModel.getId()));

        List<ModelRelation> otherRelations = newRelations.stream()
            .filter(relation -> !relation.isNewRelation())
            .collect(Collectors.toList());
        assertEqualRelations(oldRelations, otherRelations);

        // обновляем созданный sku
        createdSku.removeAllParameterValues("param1");
        bus.fireEvent(new SkuRelationChangedEvent(createdSku));

        ModelRelation createdUpdatedRelation = editedModel.getRelations().stream()
            .filter(ModelRelation::isNewRelation)
            .findFirst()
            .orElseThrow(() -> new AssertionException("Failed to find created and then updated relation"));

        Assert.assertNotNull(createdUpdatedRelation.getModel());

        // удаляем созданный sku
        bus.fireEvent(new SkuDeletionConfirmedEvent(createdSku));

        // убеждаемся, что итоговый список sku совпадает с изначальным
        assertEqualRelations(originalModel.getRelations(), editedModel.getRelations());
    }

    @Test
    public void testRelationDeleted() throws Exception {
        // удаляем relation
        ModelRelation relation = data.getModel().getRelation(2).orElse(null);
        bus.fireEvent(new SkuDeletionConfirmedEvent(relation.getModel()));

        CommonModel editedModel = editableModel.getModel();
        CommonModel originalModel = editableModel.getOriginalModel();
        List<ModelRelation> newRelations = editedModel.getRelations();
        List<ModelRelation> oldRelations = originalModel.getRelations();

        assertEquals(3, newRelations.size());
        assertEquals(oldRelations.size(), newRelations.size() + 1);
    }

    @Test
    public void testSkuChanges() throws Exception {
        // меняем существующий SKU (удаляем у него параметр)
        ModelRelation modelRelation = editableModel.getModel().getRelations().get(0);
        CommonModel sku = modelRelation.getModel();
        sku.removeAllParameterValues("param1");
        bus.fireEvent(new SkuRelationChangedEvent(sku));

        CommonModel editedModel = editableModel.getModel();
        CommonModel originalModel = editableModel.getOriginalModel();
        List<ModelRelation> newRelations = editedModel.getRelations();
        List<ModelRelation> oldRelations = originalModel.getRelations();

        assertEquals(4, newRelations.size());
        assertEquals(oldRelations.size(), newRelations.size());

        ModelRelation updatedRelation = newRelations.get(0);
        Assert.assertNotNull(updatedRelation.getModel());
    }

    @Test
    public void testFiringModelChangedEventOnAnyRelationChange() {
        AtomicInteger modelModifiedCallCount = new AtomicInteger();
        bus.subscribe(ModelModifiedEvent.class, event -> {
            modelModifiedCallCount.incrementAndGet();
        });

        bus.fireEvent(new SkuRelationCreationEvent());
        Assert.assertEquals(1, modelModifiedCallCount.get());

        CommonModel model = editableModel.getModel();
        List<ModelRelation> relations = model.getRelations();
        bus.fireEvent(new SkuRelationChangedEvent(relations.get(0).getModel()));
        Assert.assertEquals(2, modelModifiedCallCount.get());

        bus.fireEvent(new SkuDeletionConfirmedEvent(relations.get(0).getModel()));
        Assert.assertEquals(3, modelModifiedCallCount.get());
    }

    @Test
    public void testNotFiringModelChangedEventOnSystemCalls() {
        AtomicInteger modelModifiedCallCount = new AtomicInteger();
        bus.subscribe(ModelModifiedEvent.class, event -> {
            modelModifiedCallCount.incrementAndGet();
        });

        CommonModel model = editableModel.getModel();
        bus.fireEvent(new PopulateModelSaveSyncEvent(model));
        Assert.assertEquals(0, modelModifiedCallCount.get());

        bus.fireEvent(new RemoteModelChangedEvent(model, RemoteModelChangedEvent.Cause.SAVED));
        Assert.assertEquals(0, modelModifiedCallCount.get());
    }

    @Test
    public void testPopulateModelSaveSyncEvent() {
        CommonModel model = editableModel.getModel();
        ModelRelation skuRelation = model.getRelation(2).orElse(null);

        CommonModel modelToSave = new CommonModel(model);
        modelToSave.addRelation(EXPERIMENTAL_RELATION);

        // populate without change
        bus.fireEvent(new PopulateModelSaveSyncEvent(modelToSave));
        Assert.assertEquals(5, modelToSave.getRelations().size());
        modelToSave.getRelations().stream()
            .filter(r -> r.getId() != EXPERIMENTAL_RELATION.getId())
            .forEach(r -> Assert.assertNull(r.getModel()));

        // change relation
        bus.fireEvent(new SkuRelationChangedEvent(skuRelation.getModel()));
        bus.fireEvent(new PopulateModelSaveSyncEvent(modelToSave));
        Assert.assertEquals(5, modelToSave.getRelations().size());
        ModelRelation relationWithModel = modelToSave.getRelation(2)
            .orElseThrow(() -> new AssertionException("Expected relation: " + 2));
        Assert.assertNotNull(relationWithModel.getModel());

        // delete relation
        bus.fireEvent(new SkuDeletionConfirmedEvent(skuRelation.getModel()));
        bus.fireEvent(new PopulateModelSaveSyncEvent(modelToSave));
        Assert.assertEquals(5, modelToSave.getRelations().size());
        Assert.assertTrue(modelToSave.getRelation(2).isPresent());
        Assert.assertTrue(modelToSave.getRelation(2).get().getModel().isDeleted());

        // create new relation
        bus.fireEvent(new SkuRelationCreationEvent());
        bus.fireEvent(new PopulateModelSaveSyncEvent(modelToSave));
        Assert.assertEquals(6, modelToSave.getRelations().size());
        ModelRelation createdRelationWithModel = modelToSave.getRelation(0)
            .orElseThrow(() -> new AssertionException("Expected to find new relation"));
        Assert.assertNotNull(createdRelationWithModel.getModel());
    }

    @Test
    public void testClearInnerStateAfterRemoteModelChangedEvent() {
        CommonModel model = new CommonModel(editableModel.getModel());
        ModelRelation skuRelation = model.getRelation(2).orElse(null);

        // change state by firing relation changed event
        bus.fireEvent(new SkuRelationChangedEvent(skuRelation.getModel()));

        // assert inner state changed
        bus.fireEvent(new PopulateModelSaveSyncEvent(model));
        ModelRelation relationWithModel = model.getRelation(2)
            .orElseThrow(() -> new AssertionException("Expected relation: " + 2));
        Assert.assertNotNull(relationWithModel.getModel());

        // fire RemoteModelChangedEvent
        bus.fireEvent(new RemoteModelChangedEvent(model, RemoteModelChangedEvent.Cause.SAVED));
        // assert inner state cleared
        bus.fireEvent(new PopulateModelSaveSyncEvent(model));
        ModelRelation relationWithModel2 = model.getRelation(2)
            .orElseThrow(() -> new AssertionException("Expected relation: " + 2));
        Assert.assertNull(relationWithModel2.getModel());
    }

    @Test
    public void testSettingOnlyMandatoryAndRequiredParams() {
        data.clearParameters()
            .startParameters()
            .startParameter()
                .xsl("param1").type(Param.Type.NUMERIC).name("Enum1")
            .endParameter()
            .startParameter()
                .xsl("param2").type(Param.Type.NUMERIC).name("Enum2")
                .skuParameterMode(SkuParameterMode.SKU_NONE)
            .endParameter()
            .startParameter()
                .xsl("param3").type(Param.Type.NUMERIC).name("Enum3")
                .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .endParameter()
            .startParameter()
                .xsl("param4").type(Param.Type.NUMERIC).name("Enum4")
                .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .endParameter()

            .startParameter()
                .xsl("param5").type(Param.Type.NUMERIC).name("Enum5")
                .mandatory(true)
            .endParameter()
            .startParameter()
                .xsl("param6").type(Param.Type.NUMERIC).name("Enum6")
                .skuParameterMode(SkuParameterMode.SKU_NONE).mandatory(true)
            .endParameter()
            .startParameter()
                .xsl("param7").type(Param.Type.NUMERIC).name("Enum7")
                .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL).mandatory(true)
            .endParameter()
            .startParameter()
                .xsl("param8").type(Param.Type.NUMERIC).name("Enum8")
                .skuParameterMode(SkuParameterMode.SKU_DEFINING).mandatory(true)
            .endParameter()
        .endParameters();

        // инициализируем движок
        editableModel = new EditableModel(bus);
        editableModel.setOriginalModel(data.getModel());
        editableModel.setModel(new CommonModel(data.getModel()));

        bus.fireEvent(new ModelDataLoadedEvent(data.getModelData()));
        bus.fireEvent(new ModelRenderedEvent(editableModel));
        bus.fireEvent(new ModelUIGeneratedEvent(editableModel));

        SkuRelationWidget skuRelationWidget = viewFactory.getSkuRelationWidget();
        List<String> params = skuRelationWidget.getParams().stream()
            .map(CategoryParam::getXslName)
            .collect(Collectors.toList());

        List<String> expected = Arrays.asList("param4", "param7", "param8");
        Assert.assertEquals(expected, params);
    }

    @Test
    public void testSkuRelationReorder() {
        CommonModel model = editableModel.getModel();
        List<ModelRelation> relations = model.getRelations(ModelRelation.RelationType.SKU_MODEL);
        ModelRelation r0 = relations.get(0);
        ModelRelation r1 = relations.get(1);
        ModelRelation r2 = relations.get(2);
        ModelRelation r3 = relations.get(3);
        bus.fireEvent(new SkuRelationReorderEvent(r2.getModel(), SkuRelationReorderEvent.ReorderType.UP));
        List<ModelRelation> updatedRelationList = model.getRelations(ModelRelation.RelationType.SKU_MODEL);
        //1 и 2 поменяются местами
        Assert.assertTrue(updatedRelationList.get(0).equals(r0));
        Assert.assertTrue(updatedRelationList.get(1).equals(r2));
        Assert.assertTrue(updatedRelationList.get(2).equals(r1));
        Assert.assertTrue(updatedRelationList.get(3).equals(r3));

        bus.fireEvent(new SkuRelationReorderEvent(r3.getModel(), SkuRelationReorderEvent.ReorderType.DOWN));
        updatedRelationList = model.getRelations(ModelRelation.RelationType.SKU_MODEL);
        //ничего не поменяется
        Assert.assertTrue(updatedRelationList.get(0).equals(r0));
        Assert.assertTrue(updatedRelationList.get(1).equals(r2));
        Assert.assertTrue(updatedRelationList.get(2).equals(r1));
        Assert.assertTrue(updatedRelationList.get(3).equals(r3));

        bus.fireEvent(new SkuRelationReorderEvent(r1.getModel(), SkuRelationReorderEvent.ReorderType.DOWN));
        updatedRelationList = model.getRelations(ModelRelation.RelationType.SKU_MODEL);
        //1 и 3 поменяются местами
        Assert.assertTrue(updatedRelationList.get(0).equals(r0));
        Assert.assertTrue(updatedRelationList.get(1).equals(r2));
        Assert.assertTrue(updatedRelationList.get(2).equals(r3));
        Assert.assertTrue(updatedRelationList.get(3).equals(r1));

        bus.fireEvent(new SkuRelationReorderEvent(r1.getModel(), SkuRelationReorderEvent.ReorderType.TO_TOP));
        updatedRelationList = model.getRelations(ModelRelation.RelationType.SKU_MODEL);
        //1 станет первым
        Assert.assertTrue(updatedRelationList.get(0).equals(r1));
        Assert.assertTrue(updatedRelationList.get(1).equals(r0));
        Assert.assertTrue(updatedRelationList.get(2).equals(r2));
        Assert.assertTrue(updatedRelationList.get(3).equals(r3));

        bus.fireEvent(new SkuRelationReorderEvent(r0.getModel(), SkuRelationReorderEvent.ReorderType.TO_BOTTOM));
        updatedRelationList = model.getRelations(ModelRelation.RelationType.SKU_MODEL);
        //0 станет последним
        Assert.assertTrue(updatedRelationList.get(0).equals(r1));
        Assert.assertTrue(updatedRelationList.get(1).equals(r2));
        Assert.assertTrue(updatedRelationList.get(2).equals(r3));
        Assert.assertTrue(updatedRelationList.get(3).equals(r0));
    }

    private static void assertEqualRelations(Collection<ModelRelation> expectedRelations,
                                             Collection<ModelRelation> actualRelations) {
        assertEquals(expectedRelations.size(), actualRelations.size());

        Iterator<ModelRelation> expectedIterator = expectedRelations.iterator();
        Iterator<ModelRelation> actualIterator = actualRelations.iterator();

        while (expectedIterator.hasNext() && actualIterator.hasNext()) {
            ModelRelation expected = expectedIterator.next();
            ModelRelation actual = actualIterator.next();
            assertEqualsRelation(expected, actual);
        }
    }

    private static void assertEqualsRelation(ModelRelation expectedRelation, ModelRelation actualRelation) {
        assertEquals(expectedRelation.getId(), actualRelation.getId());
        assertEquals(expectedRelation.getCategoryId(), actualRelation.getCategoryId());
        assertEquals(expectedRelation.getType(), actualRelation.getType());

        if (expectedRelation.getModel() != null || actualRelation.getModel() != null) {
            assertEqualsModel(expectedRelation.getModel(), actualRelation.getModel());
        }
    }

    private static void assertEqualsModel(CommonModel expectedModel, CommonModel actualModel) {
        assertEquals(expectedModel.getId(), actualModel.getId());
        assertEquals(expectedModel.getCategoryId(), actualModel.getCategoryId());
        assertEquals(expectedModel.isPublished(), actualModel.isPublished());
        assertEquals(expectedModel.isDeleted(), actualModel.isDeleted());
        assertEqualRelations(expectedModel.getRelations(), actualModel.getRelations());
    }
}
