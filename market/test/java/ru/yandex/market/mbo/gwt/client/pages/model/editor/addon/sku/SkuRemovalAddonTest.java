package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.DeleteSkuFromEditorEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.IsSkuUncheckRequestedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuDeletionConfirmedByUserEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuDeletionRequestedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuInfoByIdRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuMappingsLoadedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.SelectSkuSuccessorsWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.models.IdAndName;
import ru.yandex.market.mbo.gwt.models.Role;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRemovalType;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelTransition;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.MappingUpdateError;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.SupplierOffer;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static ru.yandex.market.mbo.db.modelstorage.SkuMappingsTestUtils.createMapping;
import static ru.yandex.market.mbo.db.modelstorage.SkuMappingsTestUtils.createStatus;

/**
 * Тестируем, что визард удаления SKU и предвалидация наличия картинок работают корректно.
 *
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuRemovalAddonTest extends BaseSkuAddonTest {

    private static final List<IdAndName> SUCCESSORS = Arrays.asList(
        new IdAndName(3L, "Sku3"),
        new IdAndName(4L, "Sku4")
    );

    private static final List<SupplierOffer> MAPPINGS = Arrays.asList(
        createMapping(100, "shopsku1", 3L),
        createMapping(100, "shopsku2", 4L),
        createMapping(200, "shopsku1", 2L),
        createMapping(300, "shopsku3", 1L)
    );

    private static final ModelInfo REQUESTED_SKU = new ModelInfo();

    @Override
    public void model() {
        // создаем модель и связанные с ней sku
        data.startModel()
            .title("Test model")
            .id(1).category(666).vendorId(777).currentType(CommonModel.Source.GURU)
            .startParameterValue()
                .xslName(XslNames.IS_SKU).paramId(7L).booleanValue(true, 1L)
            .endParameterValue()
            .startModelRelation()
                .id(2).categoryId(666).type(ModelRelation.RelationType.SKU_MODEL)
                .startModel()
                    // sku model содержит определяющие параметры
                    .id(2).category(666).currentType(CommonModel.Source.SKU)
                    .startModelRelation()
                       .id(1).categoryId(666).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                    .endModelRelation()
                .endModel()
            .endModelRelation()
            .endModel();
    }

    @Test
    public void testSkuRemovalWithSuccessors() throws Exception {
        // Запрашиваем удаление SKU
        ModelRelation relation = data.getModel().getRelation(2).orElse(null);
        bus.fireEvent(new SkuDeletionRequestedEvent(relation.getModel()));

        SelectSkuSuccessorsWidgetStub widget = viewFactory.getCurrentSelectSkuSuccessorsWidget();
        widget.clickContinue(SUCCESSORS);

        rpc.setSaveModel(1L, null);
        bus.fireEvent(new SaveModelRequest(false, false));

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertThat(savedTransitions).containsExactlyInAnyOrder(
            createSkuTransition(2L, 3L, ModelTransition.ModelTransitionType.SPLIT),
            createSkuTransition(2L, 4L, ModelTransition.ModelTransitionType.SPLIT)
        );

        bus.fireEvent(new SaveModelRequest(false, false));

        savedTransitions = rpc.getModelTransitionsToSave();

        assertEquals(0, savedTransitions.size());
    }

    @Test
    public void testSkuRemovalWithoutSuccessors() throws Exception {
        // Запрашиваем удаление SKU
        ModelRelation relation = data.getModel().getRelation(2).orElse(null);
        bus.fireEvent(new SkuDeletionRequestedEvent(relation.getModel()));

        SelectSkuSuccessorsWidgetStub widget = viewFactory.getCurrentSelectSkuSuccessorsWidget();
        widget.setModelRemovalType(ModelRemovalType.ERROR);
        widget.clickContinue(Collections.emptyList());

        rpc.setSaveModel(1L, null);
        bus.fireEvent(new SaveModelRequest(false, false));

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertThat(savedTransitions).containsExactlyInAnyOrder(
            createSkuTransition(2L, null, ModelTransition.ModelTransitionType.ERROR)
        );
    }

    @Test
    public void testSkuRemovalCancel() throws Exception {
        // Запрашиваем удаление SKU
        ModelRelation relation = data.getModel().getRelation(2).orElse(null);
        bus.fireEvent(new SkuDeletionRequestedEvent(relation.getModel()));

        SelectSkuSuccessorsWidgetStub widget = viewFactory.getCurrentSelectSkuSuccessorsWidget();
        widget.clickCancel();

        rpc.setSaveModel(1L, null);
        bus.fireEvent(new SaveModelRequest(false, false));

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertEquals(0, savedTransitions.size());
    }

    @Test
    public void testSkuRemovalHidePopup() throws Exception {
        // Запрашиваем удаление SKU
        ModelRelation relation = data.getModel().getRelation(2).orElse(null);
        bus.fireEvent(new SkuDeletionRequestedEvent(relation.getModel()));

        SelectSkuSuccessorsWidgetStub widget = viewFactory.getCurrentSelectSkuSuccessorsWidget();
        widget.hide();

        rpc.setSaveModel(1L, null);
        bus.fireEvent(new SaveModelRequest(false, false));

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertEquals(0, savedTransitions.size());
    }

    @Test
    public void testIsSkuUncheck() throws Exception {
        // Запрашиваем снятие галки IsSku
        editableModel.getEditableParameter(XslNames.IS_SKU)
            .getFirstEditableValue().getValueWidget().setValue(false, true);

        SelectSkuSuccessorsWidgetStub widget = viewFactory.getCurrentSelectSkuSuccessorsWidget();
        widget.clickContinue(SUCCESSORS);

        rpc.setSaveModel(1L, null);
        bus.fireEvent(new SaveModelRequest(false, false));

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertThat(savedTransitions).containsExactlyInAnyOrder(
            createSkuTransition(1L, 3L, ModelTransition.ModelTransitionType.SPLIT),
            createSkuTransition(1L, 4L, ModelTransition.ModelTransitionType.SPLIT)
        );

        // Галка снялась
        ValueWidget<?> isSkuWidget = editableModel.getEditableParameter(XslNames.IS_SKU)
            .getFirstEditableValue().getValueWidget();
        assertEquals(false, isSkuWidget.getValue());
    }

    @Test
    public void testIsSkuUncheckCancel() throws Exception {
        // Запрашиваем снятие галки IsSku
        editableModel.getEditableParameter(XslNames.IS_SKU)
            .getFirstEditableValue().getValueWidget().setValue(false, true);

        SelectSkuSuccessorsWidgetStub widget = viewFactory.getCurrentSelectSkuSuccessorsWidget();
        widget.clickCancel();

        // Галка не снялась
        ValueWidget<?> isSkuWidget = editableModel.getEditableParameter(XslNames.IS_SKU)
            .getFirstEditableValue().getValueWidget();
        assertEquals(true, isSkuWidget.getValue());
    }

    @Test
    public void testIsSkuUncheckAndCheckBack() throws Exception {
        // Запрашиваем снятие галки IsSku
        editableModel.getEditableParameter(XslNames.IS_SKU)
            .getFirstEditableValue().getValueWidget().setValue(false, true);

        SelectSkuSuccessorsWidgetStub widget = viewFactory.getCurrentSelectSkuSuccessorsWidget();
        widget.clickContinue(SUCCESSORS);

        // Ставим галку назад
        editableModel.getEditableParameter(XslNames.IS_SKU)
            .getFirstEditableValue().getValueWidget().setValue(true, true);

        rpc.setSaveModel(1L, null);
        bus.fireEvent(new SaveModelRequest(false, false));

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertEquals(0, savedTransitions.size());
    }

    @Test
    public void moveMappingsSuccess() {
        // Запрашиваем удаление SKU
        ModelRelation relation = data.getModel().getRelation(2).orElse(null);
        bus.fireEvent(new SkuDeletionRequestedEvent(relation.getModel()));

        SelectSkuSuccessorsWidgetStub widget = viewFactory.getCurrentSelectSkuSuccessorsWidget();
        widget.setMappings(MAPPINGS);
        rpc.setSkuMappingUpdateStatusProvider(mapping -> createStatus(2L, mapping,
            MappingUpdateError.ErrorKind.NONE));

        widget.clickContinue(SUCCESSORS);

        // Так как маппинги успешно сохранились - модель сохранилась с переездами
        rpc.setSaveModel(1L, null);
        bus.fireEvent(new SaveModelRequest(false, false));

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertThat(savedTransitions).containsExactlyInAnyOrder(
            createSkuTransition(2L, 3L, ModelTransition.ModelTransitionType.SPLIT),
            createSkuTransition(2L, 4L, ModelTransition.ModelTransitionType.SPLIT)
        );
    }

    @Test
    public void moveMappingsFailure() {
        // Запрашиваем удаление SKU
        ModelRelation relation = data.getModel().getRelation(2).orElse(null);
        bus.fireEvent(new SkuDeletionRequestedEvent(relation.getModel()));

        SelectSkuSuccessorsWidgetStub widget = viewFactory.getCurrentSelectSkuSuccessorsWidget();
        widget.setMappings(MAPPINGS);
        rpc.setSkuMappingUpdateStatusProvider(mapping -> createStatus(2L, mapping,
            MappingUpdateError.ErrorKind.OTHER));

        widget.clickContinue(SUCCESSORS);

        // Так как маппинги не сохранились успешно - модель сохранилась без переездов
        rpc.setSaveModel(1L, null);
        bus.fireEvent(new SaveModelRequest(false, false));

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertEquals(0, savedTransitions.size());
    }

    @Test
    public void moveMappingsPartialFailure() {
        // Запрашиваем удаление SKU
        ModelRelation relation = data.getModel().getRelation(2).orElse(null);
        bus.fireEvent(new SkuDeletionRequestedEvent(relation.getModel()));

        SelectSkuSuccessorsWidgetStub widget = viewFactory.getCurrentSelectSkuSuccessorsWidget();
        widget.setMappings(MAPPINGS);
        rpc.setSkuMappingUpdateStatusProvider(mapping -> {
            if (mapping.getShopSkuId().equals("shopsku1")) {
                return createStatus(2L, mapping, MappingUpdateError.ErrorKind.CONCURRENT_MODIFICATION);
            } else {
                return createStatus(2L, mapping, MappingUpdateError.ErrorKind.NONE);
            }
        });

        widget.clickContinue(SUCCESSORS);

        // Так как один из маппингов сохранился успешно - следующий клик попробует сохранить только отсавшийся
        assertThat(widget.getPendingMappings())
            .containsExactlyInAnyOrder(createMapping(100, "shopsku1", 3L));

        rpc.setSkuMappingUpdateStatusProvider(mapping -> createStatus(2L, mapping,
            MappingUpdateError.ErrorKind.NONE));

        widget.clickContinue(SUCCESSORS);

        // Так как маппинги не сохранились успешно - модель сохранилась с переездами
        rpc.setSaveModel(1L, null);
        bus.fireEvent(new SaveModelRequest(false, false));

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertThat(savedTransitions).containsExactlyInAnyOrder(
            createSkuTransition(2L, 3L, ModelTransition.ModelTransitionType.SPLIT),
            createSkuTransition(2L, 4L, ModelTransition.ModelTransitionType.SPLIT)
        );
    }

    @Test
    public void testSkuHasPicturesUserCancel() {
        // Запрашиваем удаление SKU с картинками, ожидается попап про наличие картинок у удаляемой СКУ
        CommonModel sku = data.getModel().getRelation(2).get().getModel();
        sku.addPicture(new Picture());
        viewFactory.whenAskedSayThis(false); //юзер не подтвердит удаление, ничего не произойдёт

        //Заранее подпишемся на следующий по цепочке эвент. В случае правильной отработки теста мы никогда не должны
        //попасть в эту подписку, т.к. юзер нажал кнопку "Нет" и удаление СКУ не состоялось:
        bus.subscribe(SkuDeletionConfirmedByUserEvent.class, event -> fail("Deletion should never occur in this case"));

        //Собственно, удаление СКУ красным крестиком, начало теста, запуск цепочки событий
        bus.fireEvent(new SkuDeletionRequestedEvent(sku));
    }

    @Test()
    public void testSkuHasPicturesUserConfirm() {
        // Запрашиваем удаление SKU с картинками, ожидается попап про наличие картинок у удаляемой СКУ
        CommonModel sku = data.getModel().getRelation(2).get().getModel();
        sku.addPicture(new Picture());
        viewFactory.whenAskedSayThis(true); //юзер подтвердит удаление
        List<CommonModel> processedSkus = new ArrayList<>(1);

        //Заранее подпишемся на следующий по цепочке эвент. В случае правильной отработки теста мы должны
        //попасть в эту подписку, т.к. юзер нажал кнопку "Да" и удаление СКУ состоялось:
        bus.subscribe(SkuDeletionConfirmedByUserEvent.class, event -> {
            processedSkus.add(event.getSku());
        });

        //Собственно, удаление СКУ красным крестиком, начало теста, запуск цепочки событий
        bus.fireEventSync(new SkuDeletionRequestedEvent(sku));

        //Проверяем, что мы условно попали в визард, попали ровно один раз и с тем самым СКУ:
        assertEquals(1, processedSkus.size());
        assertEquals(sku, processedSkus.get(0));
    }

    @Test()
    public void testSkuWithoutPicturesNoDialogShown() {
        // Запрашиваем удаление SKU без картинок, ожидается, что никаких лишних попапов не будет и сразу вылезет визард
        CommonModel sku = data.getModel().getRelation(2).get().getModel();
        //специально заставим юзера говорить "Нет". Тем самым, если у нас бага, и картиночный попап всё-таки появится,
        //то у нас сэмулируется отмена удаления, что приведёт к желаемому падению теста.
        viewFactory.whenAskedSayThis(false);
        sku.clearPictures();
        List<CommonModel> processedSkus = new ArrayList<>(1);

        //Заранее подпишемся на следующий по цепочке эвент. В случае правильной отработки теста мы должны
        //попасть в эту подписку, т.к. картиночного попапа не было и мы сразу попадаем в визард
        bus.subscribe(SkuDeletionConfirmedByUserEvent.class, event -> {
            processedSkus.add(event.getSku());
        });

        //Собственно, удаление СКУ красным крестиком, начало теста, запуск цепочки событий
        bus.fireEventSync(new SkuDeletionRequestedEvent(sku));

        //Проверяем, что мы условно попали в визард, попали ровно один раз и с тем самым СКУ:
        assertEquals(1, processedSkus.size());
        assertEquals(sku, processedSkus.get(0));
    }

    @Test
    public void testSkuRemovalRequestSku() throws Exception {
        // Запрашиваем удаление SKU
        rpc.setRequestedSku(REQUESTED_SKU);
        List<ModelInfo> returnedSku = new ArrayList<>();

        bus.fireEvent(new SkuInfoByIdRequestEvent(1L, returnedSku::add));

        assertThat(returnedSku).containsExactlyInAnyOrder(REQUESTED_SKU);
    }

    @Test
    public void testAdminNonEmptyMappingHasPermission() {
        CommonModel sku = data.getModel().getRelation(2).get().getModel();
        List<CommonModel> processedModels = new ArrayList<>();
        bus.subscribe(SkuMappingsLoadedEvent.class, event -> {
            processedModels.add(event.getModelToRemove());
        });
        rpc.setMappings(MAPPINGS);

        bus.fireEventSync(new SkuDeletionRequestedEvent(sku));
        bus.fireEventSync(new IsSkuUncheckRequestedEvent());
        bus.fireEventSync(new DeleteSkuFromEditorEvent());

        assertThat(processedModels).containsExactlyInAnyOrder(sku, model, model);
    }

    @Test
    public void testAdminEmptyMappingHasPermission() {
        CommonModel sku = data.getModel().getRelation(2).get().getModel();
        List<CommonModel> processedModels = new ArrayList<>();
        bus.subscribe(SkuMappingsLoadedEvent.class, event -> {
            processedModels.add(event.getModelToRemove());
        });

        bus.fireEventSync(new SkuDeletionRequestedEvent(sku));
        bus.fireEventSync(new IsSkuUncheckRequestedEvent());
        bus.fireEventSync(new DeleteSkuFromEditorEvent());

        assertThat(processedModels).containsExactlyInAnyOrder(sku, model, model);
    }

    @Test
    public void testOperatorHasSkuMappingOperatorRoleNonEmptyMappingHasPermission() {
        view.getUser().getRoles().clear();
        view.getUser().setRole(Role.OPERATOR);
        view.getUser().addRole(Role.SKU_MAPPING_OPERATOR);
        CommonModel sku = data.getModel().getRelation(2).get().getModel();
        List<CommonModel> processedModels = new ArrayList<>();
        bus.subscribe(SkuMappingsLoadedEvent.class, event -> {
            processedModels.add(event.getModelToRemove());
        });
        rpc.setMappings(MAPPINGS);

        bus.fireEventSync(new SkuDeletionRequestedEvent(sku));
        bus.fireEventSync(new IsSkuUncheckRequestedEvent());
        bus.fireEventSync(new DeleteSkuFromEditorEvent());

        assertThat(processedModels).containsExactlyInAnyOrder(sku, model, model);
    }

    @Test
    public void testOperatorEmptyMappingHasPermission() {
        view.getUser().getRoles().clear();
        view.getUser().setRole(Role.OPERATOR);
        CommonModel sku = data.getModel().getRelation(2).get().getModel();
        List<CommonModel> processedModels = new ArrayList<>();
        bus.subscribe(SkuMappingsLoadedEvent.class, event -> {
            processedModels.add(event.getModelToRemove());
        });

        bus.fireEventSync(new SkuDeletionRequestedEvent(sku));
        bus.fireEventSync(new IsSkuUncheckRequestedEvent());
        bus.fireEventSync(new DeleteSkuFromEditorEvent());

        assertThat(processedModels).containsExactlyInAnyOrder(sku, model, model);
    }

    @Test
    public void testOperatorNonEmptyBlueMappingHasNoPermission() {
        view.getUser().getRoles().clear();
        view.getUser().setRole(Role.OPERATOR);

        CommonModel sku = data.getModel().getRelation(2).get().getModel();
        List<CommonModel> skuToRemove = new ArrayList<>();
        bus.subscribe(SkuMappingsLoadedEvent.class, event -> {
            skuToRemove.add(event.getModelToRemove());
        });
        rpc.setMappings(MAPPINGS);

        bus.fireEventSync(new SkuDeletionRequestedEvent(sku));
        bus.fireEventSync(new IsSkuUncheckRequestedEvent());
        bus.fireEventSync(new DeleteSkuFromEditorEvent());

        assertThat(skuToRemove).isEmpty();
    }

    private ModelTransition createSkuTransition(long from, Long to, ModelTransition.ModelTransitionType type) {
        ModelTransition.ModelTransitionReason reason;
        switch (type) {
            case ERROR:
                reason = ModelTransition.ModelTransitionReason.ERROR_REMOVAL;
                break;
            case DUPLICATE:
                reason = ModelTransition.ModelTransitionReason.DUPLICATE_REMOVAL;
                break;
            case SPLIT:
                reason = ModelTransition.ModelTransitionReason.SKU_SPLIT;
                break;
            default: throw new IllegalArgumentException();
        }
        return new ModelTransition()
            .setType(type)
            .setReason(reason)
            .setEntityType(ModelTransition.EntityType.SKU)
            .setOldEntityId(from)
            .setOldEntityDeleted(true)
            .setNewEntityId(to)
            .setPrimaryTransition(type == ModelTransition.ModelTransitionType.DUPLICATE);
    }
}
