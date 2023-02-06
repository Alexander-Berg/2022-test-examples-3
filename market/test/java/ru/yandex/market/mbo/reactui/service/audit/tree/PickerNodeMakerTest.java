package ru.yandex.market.mbo.reactui.service.audit.tree;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.billing.counter.base.squash.GroupingSquashStrategy;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.dto.billing.components.BaseComponent;
import ru.yandex.market.mbo.reactui.dto.billing.components.Text;
import ru.yandex.market.mbo.reactui.service.audit.BillingPricesRegistry;
import ru.yandex.market.mbo.reactui.service.audit.ColumnDefinition;
import ru.yandex.market.mbo.reactui.service.audit.MockUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelSingleParamAction;

public class PickerNodeMakerTest {

    private static final GroupingSquashStrategy.ParameterProperty PARAMETER_PROPERTY =
        new GroupingSquashStrategy.ParameterProperty(1L, "propertyName");
    private static final List<BaseComponent> TITLE_COMPONENTS = Arrays.asList(
        new Text("Пикер"),
        new Text(String.valueOf(PARAMETER_PROPERTY.getParameterId())),
        new Text(PARAMETER_PROPERTY.getPropertyName())
    );
    private static final long PARAM_ID = 1L;
    private static final String PARAM_NAME = "param1";
    private static final long USER_ID_1 = 101L;
    private static final long USER_ID_2 = 101L;

    private BillingPricesRegistry pricesRegistry = MockUtils.mockBillingPricesRegistry();

    @Test
    public void whenOperatorAndInspectorChangedThenAllDataKeyPresent() {
        PickerNodeContext context = new PickerNodeContext(
            PARAMETER_PROPERTY,
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new")),
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new")),
            USER_ID_1, USER_ID_2, pricesRegistry);

        AuditNode result = new PickerNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(TITLE_COMPONENTS);
        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_CHANGES.name(),
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_CHANGES.name(),
            ColumnDefinition.OPERATOR_PRICE.name(),
            ColumnDefinition.OPERATOR_ERROR.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    @Test
    public void whenOperatorChangedThenOnlyOperatorDataKeyPresent() {
        PickerNodeContext context = new PickerNodeContext(
            PARAMETER_PROPERTY,
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new")),
            null,
            USER_ID_1, USER_ID_2, pricesRegistry);

        AuditNode result = new PickerNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(TITLE_COMPONENTS);
        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_CHANGES.name(),
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.OPERATOR_PRICE.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    @Test
    public void whenInspectorChangedThenOnlyInspectorDataKeyPresent() {
        PickerNodeContext context = new PickerNodeContext(
            PARAMETER_PROPERTY,
            null,
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new")),
            USER_ID_1, USER_ID_2, pricesRegistry);

        AuditNode result = new PickerNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(TITLE_COMPONENTS);
        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_CHANGES.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }
}
