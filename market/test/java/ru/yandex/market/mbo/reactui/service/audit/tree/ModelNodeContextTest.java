package ru.yandex.market.mbo.reactui.service.audit.tree;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.billing.counter.base.squash.GroupingSquashStrategy;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.statistic.model.SquashedUserActions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelParamAction;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelSingleParamAction;

/**
 * @author dergachevfv
 * @since 11/25/19
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelNodeContextTest {

    private static final long MODEL_ID = 1L;

    @Test
    public void whenOperatorChangedSingleParamThenHasParamsChangesPresent() {
        AuditAction auditAction = modelSingleParamAction(1L, "param1", "old", "new");
        SquashedUserActions.ModelActions operatorActions = new SquashedUserActions.ModelActions(
                CommonModelBuilder.newBuilder(MODEL_ID, 0).getModel())
            .setChangedSingleValueParams(
                createMap(GroupingSquashStrategy.SingleParamValue::new, auditAction));

        ModelNodeContext context = new ModelNodeContext(MODEL_ID, null, false, null, operatorActions, null);

        Optional<ModelNodeContext> optionalContex = context.returnIfHasParamsChanges();
        Assertions.assertThat(optionalContex).isPresent()
            .get().isEqualTo(context);
    }

    @Test
    public void whenOperatorChangedMultiParamThenHasParamsChangesPresent() {
        AuditAction auditAction =
            modelParamAction(1L, "param1", "old", "new", AuditAction.ActionType.CREATE);
        SquashedUserActions.ModelActions operatorActions = new SquashedUserActions.ModelActions(
                CommonModelBuilder.newBuilder(MODEL_ID, 0).getModel())
            .setChangedMultiValueParams(
                createMap(GroupingSquashStrategy.MultiParamValue::new, auditAction));

        ModelNodeContext context = new ModelNodeContext(MODEL_ID, null, false, null, operatorActions, null);

        Optional<ModelNodeContext> optionalContex = context.returnIfHasParamsChanges();
        Assertions.assertThat(optionalContex).isPresent()
            .get().isEqualTo(context);
    }

    @Test
    public void whenInspectorChangedSingleParamThenHasParamsChangesPresent() {
        AuditAction auditAction = modelSingleParamAction(1L, "param1", "old", "new");
        SquashedUserActions.ModelActions inspectorActions = new SquashedUserActions.ModelActions(
                CommonModelBuilder.newBuilder(MODEL_ID, 0).getModel())
            .setChangedSingleValueParams(
                createMap(GroupingSquashStrategy.SingleParamValue::new, auditAction));

        ModelNodeContext context = new ModelNodeContext(MODEL_ID, null, false, null, null, inspectorActions);

        Optional<ModelNodeContext> optionalContex = context.returnIfHasParamsChanges();
        Assertions.assertThat(optionalContex).isPresent()
            .get().isEqualTo(context);
    }

    @Test
    public void whenInspectorChangedMultiParamThenHasParamsChangesPresent() {
        AuditAction auditAction =
            modelParamAction(1L, "param1", "old", "new", AuditAction.ActionType.CREATE);
        SquashedUserActions.ModelActions inspectorActions = new SquashedUserActions.ModelActions(
                CommonModelBuilder.newBuilder(MODEL_ID, 0).getModel())
            .setChangedMultiValueParams(
                createMap(GroupingSquashStrategy.MultiParamValue::new, auditAction));

        ModelNodeContext context = new ModelNodeContext(MODEL_ID, null, false, null, null, inspectorActions);

        Optional<ModelNodeContext> optionalContex = context.returnIfHasParamsChanges();
        Assertions.assertThat(optionalContex).isPresent()
            .get().isEqualTo(context);
    }

    @Test
    public void whenBothChangedParamsThenHasParamsChangesPresent() {
        AuditAction operatorSingleAuditAction =
            modelSingleParamAction(1L, "param1", "old", "new");
        AuditAction operatorMultiAuditAction =
            modelParamAction(2L, "param2", "old", "new", AuditAction.ActionType.CREATE);

        SquashedUserActions.ModelActions operatorActions = new SquashedUserActions.ModelActions(
                CommonModelBuilder.newBuilder(MODEL_ID, 0).getModel())
            .setChangedSingleValueParams(
                createMap(GroupingSquashStrategy.SingleParamValue::new, operatorSingleAuditAction))
            .setChangedMultiValueParams(
                createMap(GroupingSquashStrategy.MultiParamValue::new, operatorMultiAuditAction));

        AuditAction inspectorSingleAuditAction =
            modelSingleParamAction(1L, "param1", "old", "new");
        AuditAction inspectorMultiAuditAction =
            modelParamAction(2L, "param2", "old", "new", AuditAction.ActionType.UPDATE);

        SquashedUserActions.ModelActions inspectorActions = new SquashedUserActions.ModelActions(
                CommonModelBuilder.newBuilder(MODEL_ID, 0).getModel())
            .setChangedSingleValueParams(
                createMap(GroupingSquashStrategy.SingleParamValue::new, inspectorSingleAuditAction))
            .setChangedMultiValueParams(
                createMap(GroupingSquashStrategy.MultiParamValue::new, inspectorMultiAuditAction));

        ModelNodeContext context = new ModelNodeContext(MODEL_ID, null, false, null, operatorActions, inspectorActions);

        Optional<ModelNodeContext> optionalContex = context.returnIfHasParamsChanges();
        Assertions.assertThat(optionalContex).isPresent()
            .get().isEqualTo(context);
    }

    private <K> Map<K, List<AuditAction>> createMap(Function<AuditAction, K> keyExtractor,
                                                    AuditAction... actions) {
        return Arrays.stream(actions)
            .collect(Collectors.toMap(keyExtractor, Collections::singletonList));
    }
}
