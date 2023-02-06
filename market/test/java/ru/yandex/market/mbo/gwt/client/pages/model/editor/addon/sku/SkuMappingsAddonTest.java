package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.EditorTabSwitchedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SaveSkuMappingsEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.SkuMappingsWidgetStub;
import ru.yandex.market.mbo.gwt.models.Role;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.MappingUpdateError;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.SupplierOffer;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mbo.db.modelstorage.SkuMappingsTestUtils.createMapping;
import static ru.yandex.market.mbo.db.modelstorage.SkuMappingsTestUtils.createStatus;

/**
 * Тестируем, что таба редактирования маппингов SKU работает корректно.
 *
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuMappingsAddonTest extends BaseSkuAddonTest {

    private static final SupplierOffer INITIAL_MAPPING1 = createMapping(100, "shopsku1", 1L);
    private static final SupplierOffer INITIAL_MAPPING2 = createMapping(100, "shopsku2", 1L);
    private static final SupplierOffer INITIAL_MAPPING3 = createMapping(200, "shopsku1", 1L);

    private static final List<SupplierOffer> INITIAL_MAPPINGS = Arrays.asList(
        INITIAL_MAPPING1,
        INITIAL_MAPPING2,
        INITIAL_MAPPING3
    );

    private static final SupplierOffer MAPPING1 = createMapping(100, "shopsku1", 3L);
    private static final SupplierOffer MAPPING2 = createMapping(100, "shopsku2", 4L);
    private static final SupplierOffer MAPPING3 = createMapping(200, "shopsku1", 1L);

    private static final List<SupplierOffer> MAPPINGS = Arrays.asList(
        MAPPING1,
        MAPPING2,
        MAPPING3
    );

    @Override
    public void model() {
        // создаем модель
        data.startModel()
            .title("Test model")
            .id(1).category(666).vendorId(777).currentType(CommonModel.Source.GURU)
            .startParameterValue()
                .xslName(XslNames.IS_SKU).paramId(7L).booleanValue(true, 1L)
            .endParameterValue()
            .endModel();
    }

    @Test
    public void moveMappingsSuccess() {

        rpc.setMappings(INITIAL_MAPPINGS);
        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.MAPPINGS.getDisplayName()));

        assertFalse(view.isSaveButtonVisible());
        assertFalse(view.isSaveAndPublishButtonVisible());
        assertTrue(view.isSaveMappingsButtonVisible());

        SkuMappingsWidgetStub widget =
            (SkuMappingsWidgetStub) view.getTab(EditorTabs.MAPPINGS.getDisplayName());
        widget.setMappings(MAPPINGS);
        rpc.setSkuMappingUpdateStatusProvider(mapping -> createStatus(1L, mapping,
            MappingUpdateError.ErrorKind.NONE));

        bus.fireEvent(new SaveSkuMappingsEvent());

        // Так как маппинги успешно сохранились - виджет отображает только оставшийся неперемещенным маппинг
        assertThat(widget.getMappings()).containsExactlyInAnyOrder(INITIAL_MAPPING3);
    }

    @Test
    public void moveMappingsFailure() {
        rpc.setMappings(INITIAL_MAPPINGS);
        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.MAPPINGS.getDisplayName()));

        SkuMappingsWidgetStub widget =
            (SkuMappingsWidgetStub) view.getTab(EditorTabs.MAPPINGS.getDisplayName());
        widget.setMappings(MAPPINGS);
        rpc.setSkuMappingUpdateStatusProvider(mapping -> createStatus(1L, mapping,
            MappingUpdateError.ErrorKind.OTHER));

        bus.fireEvent(new SaveSkuMappingsEvent());

        // Так как маппинги не сохранились успешно - виджет отображает перенесенные маппинги с ошибкой
        assertThat(widget.getMappings()).containsExactlyInAnyOrder(
            INITIAL_MAPPING1, INITIAL_MAPPING2, INITIAL_MAPPING3
        );
    }

    @Test
    public void moveMappingsPartialFailure() {
        rpc.setMappings(INITIAL_MAPPINGS);
        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.MAPPINGS.getDisplayName()));

        SkuMappingsWidgetStub widget =
            (SkuMappingsWidgetStub) view.getTab(EditorTabs.MAPPINGS.getDisplayName());
        widget.setMappings(MAPPINGS);
        rpc.setSkuMappingUpdateStatusProvider(mapping -> {
            if (mapping.getShopSkuId().equals("shopsku1")) {
                return createStatus(1L, mapping, MappingUpdateError.ErrorKind.CONCURRENT_MODIFICATION);
            } else {
                return createStatus(1L, mapping, MappingUpdateError.ErrorKind.NONE);
            }
        });

        bus.fireEvent(new SaveSkuMappingsEvent());

        // Так как не все маппинги сохранились успешно - виджет отображает только
        // неперенесенные успешно маппинги
        assertThat(widget.getMappings()).containsExactlyInAnyOrder(INITIAL_MAPPING1, INITIAL_MAPPING3);

        // Переносим маппинги снова и сохраняем
        widget.setMappings(Arrays.asList(MAPPING1, MAPPING3));
        rpc.setSkuMappingUpdateStatusProvider(mapping -> createStatus(1L, mapping,
            MappingUpdateError.ErrorKind.NONE));

        bus.fireEvent(new SaveSkuMappingsEvent());

        // Если последующее сохранение прошло успешно - показывается только маппинг который не переносили.
        assertThat(widget.getMappings()).containsExactlyInAnyOrder(INITIAL_MAPPING3);
    }

    @Test
    public void controlsHiddenIfUserHasNoPermissions() {
        view.getUser().getRoles().clear();
        view.getUser().setRole(Role.OPERATOR);

        MutableObject<Boolean> actionCompleted = new MutableObject<>(false);
        SkuMappingsWidgetStub widget =
            (SkuMappingsWidgetStub) view.getTab(EditorTabs.MAPPINGS.getDisplayName());
        widget.setHideControlsAction(() -> actionCompleted.setValue(true));

        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.MAPPINGS.getDisplayName()));

        assertThat(actionCompleted.getValue()).isTrue();
    }

    @Test
    public void controlsVisibleIfUserHasPermissions() {
        MutableObject<Boolean> actionCompleted = new MutableObject<>(false);
        SkuMappingsWidgetStub widget =
            (SkuMappingsWidgetStub) view.getTab(EditorTabs.MAPPINGS.getDisplayName());
        widget.setHideControlsAction(() -> actionCompleted.setValue(true));

        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.MAPPINGS.getDisplayName()));

        assertThat(actionCompleted.getValue()).isFalse();
    }
}
