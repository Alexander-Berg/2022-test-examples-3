package ru.yandex.market.mbo.reactui.service.audit.tree;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.billing.counter.base.squash.GroupingSquashStrategy;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.dto.billing.components.BaseComponent;
import ru.yandex.market.mbo.reactui.dto.billing.components.Link;
import ru.yandex.market.mbo.reactui.dto.billing.components.Text;
import ru.yandex.market.mbo.reactui.service.audit.AuditUrlUtils;
import ru.yandex.market.mbo.reactui.service.audit.BillingPricesRegistry;
import ru.yandex.market.mbo.reactui.service.audit.ColumnDefinition;
import ru.yandex.market.mbo.reactui.service.audit.MockUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelSingleParamAction;

/**
 * @author dergachevfv
 * @since 11/25/19
 */
public class ParameterNodeMakerTest {

    private static final long PARAM_ID = 1L;
    private static final String PARAM_NAME = "param1";
    private static final long CATEGORY_ID = 100L;
    private static final long USER_ID_1 = 101L;
    private static final long USER_ID_2 = 101L;

    private BillingPricesRegistry pricesRegistry = MockUtils.mockBillingPricesRegistry();

    @Test
    public void whenOperatorAndInspectorChangedThenAllDataKeyPresent() {
        ParameterNodeContext context = new ParameterNodeContext(
            new ParameterNodeContext.ParamValue(new GroupingSquashStrategy.SingleParamValue(PARAM_ID, PARAM_NAME)),
            CATEGORY_ID,
            USER_ID_1, USER_ID_2, pricesRegistry,
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new")),
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new")));

        AuditNode result = new ParameterNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(makeParamTitleComponents());
        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_CHANGES.name(),
            ColumnDefinition.OPERATOR_PRICE.name(),
            ColumnDefinition.OPERATOR_ERROR.name(),
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_CHANGES.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    @Test
    public void whenOperatorChangedThenOnlyOperatorDataKeyPresent() {
        ParameterNodeContext context = new ParameterNodeContext(
            new ParameterNodeContext.ParamValue(new GroupingSquashStrategy.SingleParamValue(PARAM_ID, PARAM_NAME)),
            CATEGORY_ID,
            USER_ID_1, null, pricesRegistry,
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new")),
            null);

        AuditNode result = new ParameterNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(makeParamTitleComponents());
        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_CHANGES.name(),
            ColumnDefinition.OPERATOR_PRICE.name()
        );
    }

    @Test
    public void whenInspectorChangedThenOnlyInspectorDataKeyPresent() {
        ParameterNodeContext context = new ParameterNodeContext(
            new ParameterNodeContext.ParamValue(new GroupingSquashStrategy.SingleParamValue(PARAM_ID, PARAM_NAME)),
            CATEGORY_ID,
            USER_ID_1, USER_ID_2, pricesRegistry,
            null,
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new")));

        AuditNode result = new ParameterNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(makeParamTitleComponents());
        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_CHANGES.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    private List<BaseComponent> makeParamTitleComponents() {
        return Arrays.asList(
            new Link(String.valueOf(PARAM_ID),
                AuditUrlUtils.makeParameterUrl(PARAM_ID, CATEGORY_ID),
                true),
            new Text(PARAM_NAME));
    }
}
