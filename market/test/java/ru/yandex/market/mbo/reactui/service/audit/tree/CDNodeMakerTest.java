package ru.yandex.market.mbo.reactui.service.audit.tree;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.service.audit.BillingPricesRegistry;
import ru.yandex.market.mbo.reactui.service.audit.ColumnDefinition;
import ru.yandex.market.mbo.reactui.service.audit.MockUtils;
import ru.yandex.market.mbo.statistic.AuditTestHelper;

public class CDNodeMakerTest {

    private static final long OPERATOR_UID = 1L;
    private static final long INSPECTOR_UID = 2L;

    private BillingPricesRegistry pricesRegistry = MockUtils.mockBillingPricesRegistry();

    @Test
    public void operatorCreateInspectorDelete() {
        CDNodeContext context = new CDNodeContext(
            auditAction(1L, OPERATOR_UID, AuditAction.ActionType.CREATE),
            null,
            null,
            auditAction(1L, INSPECTOR_UID, AuditAction.ActionType.DELETE),
            OPERATOR_UID,
            INSPECTOR_UID,
            pricesRegistry
        );

        AuditNode result = new CDNodeMaker().apply(context).build();

        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_PRICE.name(),
            ColumnDefinition.OPERATOR_ERROR.name(),
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    @Test
    public void operatorCreateInspectorCheck() {
        CDNodeContext context = new CDNodeContext(
            auditAction(1L, OPERATOR_UID, AuditAction.ActionType.CREATE),
            null,
            null,
            null,
            OPERATOR_UID,
            INSPECTOR_UID,
            pricesRegistry
        );

        AuditNode result = new CDNodeMaker().apply(context).build();

        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_PRICE.name(),
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    @Test
    public void operatorCreateWithoutInspector() {
        CDNodeContext context = new CDNodeContext(
            auditAction(1L, OPERATOR_UID, AuditAction.ActionType.CREATE),
            null,
            null,
            null,
            OPERATOR_UID,
            null,
            pricesRegistry
        );

        AuditNode result = new CDNodeMaker().apply(context).build();

        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_PRICE.name()
        );
    }

    @Test
    public void operatorDeleteInspectorCheck() {
        CDNodeContext context = new CDNodeContext(
            null,
            null,
            auditAction(1L, OPERATOR_UID, AuditAction.ActionType.DELETE),
            null,
            OPERATOR_UID,
            INSPECTOR_UID,
            pricesRegistry
        );

        AuditNode result = new CDNodeMaker().apply(context).build();

        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_PRICE.name(),
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    @Test
    public void operatorDeleteWithoutInspector() {
        CDNodeContext context = new CDNodeContext(
            null,
            null,
            auditAction(1L, OPERATOR_UID, AuditAction.ActionType.DELETE),
            null,
            OPERATOR_UID,
            null,
            pricesRegistry
        );

        AuditNode result = new CDNodeMaker().apply(context).build();

        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_PRICE.name()
        );
    }

    @Test
    public void inspectorCreate() {
        CDNodeContext context = new CDNodeContext(
            null,
            auditAction(1L, INSPECTOR_UID, AuditAction.ActionType.CREATE),
            null,
            null,
            OPERATOR_UID,
            INSPECTOR_UID,
            pricesRegistry
        );

        AuditNode result = new CDNodeMaker().apply(context).build();

        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    @Test
    public void inspectorDelete() {
        CDNodeContext context = new CDNodeContext(
            null,
            null,
            null,
            auditAction(1L, INSPECTOR_UID, AuditAction.ActionType.DELETE),
            OPERATOR_UID,
            INSPECTOR_UID,
            pricesRegistry
        );

        AuditNode result = new CDNodeMaker().apply(context).build();

        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    private AuditAction auditAction(long entityId, long uid, AuditAction.ActionType actionType) {
        return AuditTestHelper.modelAuditAction(entityId, AuditAction.EntityType.MODEL_GURU, actionType)
            .setUserId(uid);
    }
}
