package ru.yandex.market.mbo.statistic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Condition;
import org.assertj.core.data.MapEntry;
import org.junit.Test;

import ru.yandex.market.mbo.billing.counter.base.squash.GroupingSquashStrategy;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.statistic.model.SquashedUserActions;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.createAuditAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.createParameter;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelAuditAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelParamAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelParamHypothesisAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelParamValueMetadataAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelPickerAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelSingleParamAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelSingleParamHypothesisAction;

/**
 * @author kravchenko-aa
 * @date 26/06/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SquashingServiceTest {
    @Test
    public void squasherShouldComputeCreatedAndDeletedFlags() {
        List<AuditAction> actions = new ArrayList<>();
        actions.add(modelAuditAction(1L, AuditAction.EntityType.MODEL_GURU, AuditAction.ActionType.CREATE));
        actions.add(modelAuditAction(2L, AuditAction.EntityType.MODEL_GURU, AuditAction.ActionType.DELETE));
        actions.add(modelAuditAction(3L, AuditAction.EntityType.MODEL_SKU, AuditAction.ActionType.CREATE));
        actions.add(modelAuditAction(4L, AuditAction.EntityType.MODEL_SKU, AuditAction.ActionType.DELETE));
        actions.add(modelAuditAction(5L, AuditAction.EntityType.MODEL_SKU, AuditAction.ActionType.UPDATE));
        SquashedUserActions squashedUserActions =
            SquashingService.squashedUserActions(MboParameters.Category.newBuilder().build(), actions,
                this::createModelsForActions);

        assertThat(squashedUserActions.getModelActionsById(1L)).has(new Condition<>(modelActions ->
            modelActions.getModelId() == 1L &&
                modelActions.isCreated() && !modelActions.isDeleted(), ""));
        assertThat(squashedUserActions.getModelActionsById(2L)).has(new Condition<>(modelActions ->
            modelActions.getModelId() == 2L &&
                !modelActions.isCreated() && modelActions.isDeleted(), ""));
        assertThat(squashedUserActions.getModelActionsById(3L)).has(new Condition<>(modelActions ->
            modelActions.getModelId() == 3L &&
                modelActions.isCreated() && !modelActions.isDeleted(), ""));
        assertThat(squashedUserActions.getModelActionsById(4L)).has(new Condition<>(modelActions ->
            modelActions.getModelId() == 4L &&
                !modelActions.isCreated() && modelActions.isDeleted(), ""));
        assertThat(squashedUserActions.getModelActionsById(5L)).has(new Condition<>(modelActions ->
            modelActions.getModelId() == 5L &&
                !modelActions.isCreated() && !modelActions.isDeleted(), ""));
    }

    @Test
    public void squasherShouldIgnorePictureParameters() {
        List<AuditAction> actions = new ArrayList<>(Arrays.asList(
            modelSingleParamAction(1L, "XL-Picture", "old", "new"),
            modelSingleParamAction(2L, "XL-Picture_1", "old", "new"),
            modelSingleParamAction(3L, "XL-Picture_2", "old", "new")
        ));
        MboParameters.Category category = MboParameters.Category.newBuilder()
            .addParameter(createParameter("XL-Picture", false, 1L))
            .addParameter(createParameter("XL-Picture_1", false, 2L))
            .addParameter(createParameter("XL-Picture_2", false, 3L))
            .build();
        SquashedUserActions squashedUserActions = SquashingService.squashedUserActions(category, actions,
            this::createModelsForActions);
        assertThat(squashedUserActions.getModelActionsById(1L).getChangedSingleValueParams())
            .isEmpty();
        assertThat(squashedUserActions.getModelActionsById(1L).getChangedMultiValueParams())
            .isEmpty();
    }

    @Test
    public void squasherShouldProcessBothModelAndSkuParams() {
        List<AuditAction> actions = new ArrayList<>();

        AuditAction modelOkAction = createAuditAction(1L, AuditAction.EntityType.MODEL_PARAM,
            AuditAction.ActionType.UPDATE,
            1L, "param1", "old", "new", 1L, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM);
        actions.add(
            // ok
            modelOkAction);

        AuditAction skuOkAction = createAuditAction(2L, AuditAction.EntityType.SKU_PARAM, AuditAction.ActionType.UPDATE,
            1L, "param1", "old", "new", 1L, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM);
        actions.add(
            // ok
            skuOkAction);
        SquashedUserActions squashedUserActions = SquashingService.squashedUserActions(
            MboParameters.Category.newBuilder()
                .addParameter(createParameter("param1", false, 1L))
                .build(), actions, this::createModelsForActions);

        SquashedUserActions.ModelActions modelActions = squashedUserActions.getModelActionsById(1L);
        assertThat(getLast(modelActions.getChangedSingleValueParams()))
            .containsExactly(MapEntry.entry(new GroupingSquashStrategy.SingleParamValue(1, "param1"), modelOkAction));
        SquashedUserActions.ModelActions skuActions = squashedUserActions.getModelActionsById(2L);
        assertThat(getLast(skuActions.getChangedSingleValueParams()))
            .containsExactly(MapEntry.entry(new GroupingSquashStrategy.SingleParamValue(1, "param1"), skuOkAction));
    }

    @Test
    public void squasherShouldSquashSingleParameterValuesCorrectly() {
        List<AuditAction> actions = new ArrayList<>();
        AuditAction modelOk1Action = modelSingleParamAction(1L, "param1", "old", "new");
        AuditAction modelOk2Action = modelSingleParamAction(2L, "param2", "new2", "newest");
        actions.addAll(Arrays.asList(
            modelOk1Action,

            modelSingleParamAction(2L, "param2", "old", "new"),
            modelSingleParamAction(2L, "param2", "new", "new2"),
            modelOk2Action,

            modelSingleParamAction(3L, "param3", "old", "old"),

            modelSingleParamAction(4L, "param4", "old", "new"),
            modelSingleParamAction(4L, "param4", "new", "new2"),
            modelSingleParamAction(4L, "param4", "new", "old")
        ));
        SquashedUserActions squashedUserActions =
            SquashingService.squashedUserActions(MboParameters.Category.newBuilder()
            .addParameter(createParameter("param1", false, 1L))
            .addParameter(createParameter("param2", false, 2L))
            .addParameter(createParameter("param3", false, 3L))
            .addParameter(createParameter("param4", false, 4L))
            .build(), actions, this::createModelsForActions);

        SquashedUserActions.ModelActions modelActions = squashedUserActions.getModelActionsById(1L);
        assertThat(getLast(modelActions.getChangedSingleValueParams()))
            .containsOnly(MapEntry.entry(new GroupingSquashStrategy.SingleParamValue(1, "param1"), modelOk1Action),
                MapEntry.entry(new GroupingSquashStrategy.SingleParamValue(2, "param2"), modelOk2Action));
    }

    @Test
    public void squasherShouldSquashMultiParameterValuesCorrectly() {
        List<AuditAction> actions = new ArrayList<>();
        AuditAction modelOkCreate1Action = modelParamAction(1L, "param1", "", "new", AuditAction.ActionType.CREATE);
        AuditAction modelOkCreate2Action = modelParamAction(1L, "param1", "", "new2", AuditAction.ActionType.CREATE);
        AuditAction modelOkCreate3Action = modelParamAction(1L, "param1", "", "newest", AuditAction.ActionType.CREATE);
        AuditAction modelOkDelete1Action = modelParamAction(2L, "param2", "old", "", AuditAction.ActionType.DELETE);
        AuditAction modelOkDelete2Action = modelParamAction(2L, "param2", "old2", "", AuditAction.ActionType.DELETE);
        AuditAction modelOkDelete3Action = modelParamAction(2L, "param2", "oldst", "", AuditAction.ActionType.DELETE);
        actions.addAll(Arrays.asList(
            // ok
            modelOkCreate1Action,
            modelOkCreate2Action,

            modelParamAction(1L, "param1", "", "newest", AuditAction.ActionType.CREATE),
            modelParamAction(1L, "param1", "newest", "", AuditAction.ActionType.DELETE),
            modelParamAction(1L, "param1", "", "newest2", AuditAction.ActionType.CREATE),
            modelParamAction(1L, "param1", "newest2", "", AuditAction.ActionType.DELETE),
            modelOkCreate3Action,

            modelOkDelete1Action,
            modelOkDelete2Action,

            modelParamAction(2L, "param2", "oldst", "", AuditAction.ActionType.DELETE),
            modelParamAction(2L, "param2", "", "oldst2", AuditAction.ActionType.CREATE),
            modelParamAction(2L, "param2", "oldst2", "", AuditAction.ActionType.DELETE),
            modelParamAction(2L, "param2", "", "oldst", AuditAction.ActionType.CREATE),
            modelOkDelete3Action,

            modelParamAction(3L, "param3", "", "lol", AuditAction.ActionType.CREATE),
            modelParamAction(3L, "param3", "lol", "", AuditAction.ActionType.DELETE),
            modelParamAction(3L, "param3", "", "lol2", AuditAction.ActionType.CREATE),
            modelParamAction(3L, "param3", "lol2", "", AuditAction.ActionType.DELETE),

            modelParamAction(4L, "param4", "lol", "", AuditAction.ActionType.DELETE),
            modelParamAction(4L, "param4", "", "lol2", AuditAction.ActionType.CREATE),
            modelParamAction(4L, "param4", "lol2", "", AuditAction.ActionType.DELETE),
            modelParamAction(4L, "param4", "", "lol", AuditAction.ActionType.CREATE)
        ));
        SquashedUserActions squashedUserActions =
            SquashingService.squashedUserActions(MboParameters.Category.newBuilder()
            .addParameter(createParameter("param1", true, 1L))
            .addParameter(createParameter("param2", true, 2L))
            .addParameter(createParameter("param3", true, 3L))
            .addParameter(createParameter("param4", true, 4L))
            .build(), actions, this::createModelsForActions);

        SquashedUserActions.ModelActions modelActions = squashedUserActions.getModelActionsById(1L);
        assertThat(getLast(modelActions.getChangedMultiValueParams()))
            .containsOnly(
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(1, "param1", "new"), modelOkCreate1Action),
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(1, "param1", "new2"), modelOkCreate2Action),
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(1, "param1", "newest"), modelOkCreate3Action),
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(2, "param2", "old"), modelOkDelete1Action),
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(2, "param2", "old2"), modelOkDelete2Action),
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(2, "param2", "oldst"), modelOkDelete3Action));
    }

    @Test
    public void squasherShouldSquashPickersCorrectly() {
        List<AuditAction> actions = new ArrayList<>();
        AuditAction modelOkAction1 = modelPickerAction(1L, "option1", "old", "new");
        AuditAction modelOkAction2 = modelPickerAction(1L, "option2", "old2", "new2");

        AuditAction modelOkAction3 = modelPickerAction(2L, "option", "new2", "newest");
        actions.addAll(Arrays.asList(

            modelOkAction1,
            modelOkAction2,

            modelPickerAction(2L, "option", "old", "new"),
            modelPickerAction(2L, "option", "new", "new2"),
            modelOkAction3,

            modelPickerAction(3L, "option1", "old", "old"),

            modelPickerAction(4L, "option1", "old", "new"),
            modelPickerAction(4L, "option1", "new", "new2"),
            modelPickerAction(4L, "option1", "new2", "old")
        ));
        SquashedUserActions squashedUserActions = SquashingService.squashedUserActions(
            MboParameters.Category.newBuilder().build(), actions, this::createModelsForActions);

        SquashedUserActions.ModelActions modelActions = squashedUserActions.getModelActionsById(1L);
        assertThat(getLast(modelActions.getChangedPickers()))
            .containsOnly(MapEntry.entry(new GroupingSquashStrategy.ParameterProperty(1L, "option1"), modelOkAction1),
                MapEntry.entry(new GroupingSquashStrategy.ParameterProperty(1L, "option2"), modelOkAction2),
                MapEntry.entry(new GroupingSquashStrategy.ParameterProperty(2L, "option"), modelOkAction3));
    }

    @Test
    public void squasherShouldSquashSingleValueParamHypothesisCorrectly() {
        List<AuditAction> actions = new ArrayList<>();
        AuditAction modelOk1Action = modelSingleParamHypothesisAction(1L, "param1", "old", "new");
        AuditAction modelOk2Action = modelSingleParamHypothesisAction(2L, "param2", "new2", "newest");
        actions.addAll(Arrays.asList(
            modelOk1Action,

            modelSingleParamHypothesisAction(2L, "param2", "old", "new"),
            modelSingleParamHypothesisAction(2L, "param2", "new", "new2"),
            modelOk2Action,

            modelSingleParamHypothesisAction(3L, "param3", "old", "old"),

            modelSingleParamHypothesisAction(4L, "param4", "old", "new"),
            modelSingleParamHypothesisAction(4L, "param4", "new", "new2"),
            modelSingleParamHypothesisAction(4L, "param4", "new", "old")
        ));
        SquashedUserActions squashedUserActions =
            SquashingService.squashedUserActions(MboParameters.Category.newBuilder()
                .addParameter(createParameter("param1", false, 1L))
                .addParameter(createParameter("param2", false, 2L))
                .addParameter(createParameter("param3", false, 3L))
                .addParameter(createParameter("param4", false, 4L))
                .build(), actions, this::createModelsForActions);

        SquashedUserActions.ModelActions modelActions = squashedUserActions.getModelActionsById(1L);
        assertThat(getLast(modelActions.getChangedSingleValueParamHypothesises()))
            .containsOnly(MapEntry.entry(new GroupingSquashStrategy.SingleParamValue(1, "param1"), modelOk1Action),
                MapEntry.entry(new GroupingSquashStrategy.SingleParamValue(2, "param2"), modelOk2Action));
    }

    @Test
    public void squasherShouldSquashMultiValueParamHypothesisCorrectly() {
        List<AuditAction> actions = new ArrayList<>();
        AuditAction modelOkCreate1Action = modelParamHypothesisAction(1L, "param1", "", "new",
            AuditAction.ActionType.CREATE);
        AuditAction modelOkCreate2Action = modelParamHypothesisAction(1L, "param1", "", "new2",
            AuditAction.ActionType.CREATE);
        AuditAction modelOkCreate3Action = modelParamHypothesisAction(1L, "param1", "", "newest",
            AuditAction.ActionType.CREATE);
        AuditAction modelOkDelete1Action = modelParamHypothesisAction(2L, "param2", "old", "",
            AuditAction.ActionType.DELETE);
        AuditAction modelOkDelete2Action = modelParamHypothesisAction(2L, "param2", "old2", "",
            AuditAction.ActionType.DELETE);
        AuditAction modelOkDelete3Action = modelParamHypothesisAction(2L, "param2", "oldst", "",
            AuditAction.ActionType.DELETE);
        actions.addAll(Arrays.asList(
            // ok
            modelOkCreate1Action,
            modelOkCreate2Action,

            modelParamHypothesisAction(1L, "param1", "", "newest", AuditAction.ActionType.CREATE),
            modelParamHypothesisAction(1L, "param1", "newest", "", AuditAction.ActionType.DELETE),
            modelParamHypothesisAction(1L, "param1", "", "newest2", AuditAction.ActionType.CREATE),
            modelParamHypothesisAction(1L, "param1", "newest2", "", AuditAction.ActionType.DELETE),
            modelOkCreate3Action,

            modelOkDelete1Action,
            modelOkDelete2Action,

            modelParamHypothesisAction(2L, "param2", "oldst", "", AuditAction.ActionType.DELETE),
            modelParamHypothesisAction(2L, "param2", "", "oldst2", AuditAction.ActionType.CREATE),
            modelParamHypothesisAction(2L, "param2", "oldst2", "", AuditAction.ActionType.DELETE),
            modelParamHypothesisAction(2L, "param2", "", "oldst", AuditAction.ActionType.CREATE),
            modelOkDelete3Action,

            modelParamHypothesisAction(3L, "param3", "", "lol", AuditAction.ActionType.CREATE),
            modelParamHypothesisAction(3L, "param3", "lol", "", AuditAction.ActionType.DELETE),
            modelParamHypothesisAction(3L, "param3", "", "lol2", AuditAction.ActionType.CREATE),
            modelParamHypothesisAction(3L, "param3", "lol2", "", AuditAction.ActionType.DELETE),

            modelParamHypothesisAction(4L, "param4", "lol", "", AuditAction.ActionType.DELETE),
            modelParamHypothesisAction(4L, "param4", "", "lol2", AuditAction.ActionType.CREATE),
            modelParamHypothesisAction(4L, "param4", "lol2", "", AuditAction.ActionType.DELETE),
            modelParamHypothesisAction(4L, "param4", "", "lol", AuditAction.ActionType.CREATE)
        ));
        SquashedUserActions squashedUserActions =
            SquashingService.squashedUserActions(MboParameters.Category.newBuilder()
                .addParameter(createParameter("param1", true, 1L))
                .addParameter(createParameter("param2", true, 2L))
                .addParameter(createParameter("param3", true, 3L))
                .addParameter(createParameter("param4", true, 4L))
                .build(), actions, this::createModelsForActions);

        SquashedUserActions.ModelActions modelActions = squashedUserActions.getModelActionsById(1L);
        assertThat(getLast(modelActions.getChangedMultiValueParamHypothesises()))
            .containsOnly(
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(1, "param1", "new"), modelOkCreate1Action),
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(1, "param1", "new2"), modelOkCreate2Action),
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(1, "param1", "newest"), modelOkCreate3Action),
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(2, "param2", "old"), modelOkDelete1Action),
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(2, "param2", "old2"), modelOkDelete2Action),
                MapEntry.entry(new GroupingSquashStrategy.MultiParamValue(2, "param2", "oldst"), modelOkDelete3Action));
    }

    @Test
    public void squasherShouldSquashParamValueMetadataCorrectly() {
        List<AuditAction> actions = new ArrayList<>();
        AuditAction modelOk1Action = modelParamValueMetadataAction(1L, "param1", "old", "new");
        AuditAction modelOk2Action = modelParamValueMetadataAction(2L, "param2", "new2", "newest");
        actions.addAll(Arrays.asList(
            modelOk1Action,

            modelParamValueMetadataAction(2L, "param2", "old", "new"),
            modelParamValueMetadataAction(2L, "param2", "new", "new2"),
            modelOk2Action,

            modelParamValueMetadataAction(3L, "param3", "old", "old"),

            modelParamValueMetadataAction(4L, "param4", "old", "new"),
            modelParamValueMetadataAction(4L, "param4", "new", "new2"),
            modelParamValueMetadataAction(4L, "param4", "new", "old")
        ));
        SquashedUserActions squashedUserActions =
            SquashingService.squashedUserActions(
                MboParameters.Category.newBuilder().build(), actions, this::createModelsForActions);

        SquashedUserActions.ModelActions modelActions = squashedUserActions.getModelActionsById(1L);
        assertThat(getLast(modelActions.getChangedParamValueMetadata()))
            .containsOnly(MapEntry.entry(new GroupingSquashStrategy.ParameterProperty(1L, "param1"), modelOk1Action),
                MapEntry.entry(new GroupingSquashStrategy.ParameterProperty(2L, "param2"), modelOk2Action));
    }

    @Test
    public void squasherShouldSquashParamValueMetadataCorrectlyDifferentPropertyNames() {
        List<AuditAction> actions = new ArrayList<>();
        // same param_id, different property_name
        AuditAction modelOk1Action = modelParamValueMetadataAction(1L, "property1", "old", "new");
        AuditAction modelOk2Action = modelParamValueMetadataAction(1L, "property2", "new2", "newest");
        actions.addAll(Arrays.asList(
            modelOk1Action,

            modelParamValueMetadataAction(1L, "property2", "old", "new"),
            modelParamValueMetadataAction(1L, "property2", "new", "new2"),
            modelOk2Action
        ));
        SquashedUserActions squashedUserActions =
            SquashingService.squashedUserActions(
                MboParameters.Category.newBuilder().build(), actions, this::createModelsForActions);

        SquashedUserActions.ModelActions modelActions = squashedUserActions.getModelActionsById(1L);
        assertThat(getLast(modelActions.getChangedParamValueMetadata()))
            .containsOnly(
                MapEntry.entry(new GroupingSquashStrategy.ParameterProperty(1L, "property1"), modelOk1Action),
                MapEntry.entry(new GroupingSquashStrategy.ParameterProperty(1L, "property2"), modelOk2Action));
    }

    private Map<Long, CommonModel> createModelsForActions(Collection<Long> ids) {
        Map<Long, CommonModel> models = new HashMap<>();
        ids.forEach(id -> models.put(id, CommonModelBuilder.newBuilder(id, 1L).getModel()));
        return models;
    }

    private static <K> Map<K, AuditAction> getLast(Map<K, List<AuditAction>> actions) {
        return actions.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, o -> o.getValue().get(o.getValue().size() - 1)));
    }
}
