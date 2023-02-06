package ru.yandex.market.mbo.reactui.service.audit.tree;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.dto.billing.components.BaseComponent;
import ru.yandex.market.mbo.reactui.dto.billing.components.Text;
import ru.yandex.market.mbo.reactui.service.audit.BillingPricesRegistry;
import ru.yandex.market.mbo.reactui.service.audit.ColumnDefinition;
import ru.yandex.market.mbo.reactui.service.audit.MockUtils;

import java.util.Collections;
import java.util.List;

import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelSingleParamAction;

public class CutOffWordsNodeMakerTest {

    private static final long PARAM_ID = 1L;
    private static final String PARAM_NAME = "param1";
    private static final long OPERATOR_UID = 1L;
    private static final long INSPECTOR_UID = 1L;

    private BillingPricesRegistry pricesRegistry = MockUtils.mockBillingPricesRegistry();

    @Test
    public void whenOperatorAndInspectorChangedThenAllDataKeyPresent() {
        CutOffWordsNodeContext context = new CutOffWordsNodeContext(
            OPERATOR_UID, INSPECTOR_UID, pricesRegistry,
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new")),
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new"))
        );

        AuditNode result = new CutOffWordsNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(makeAliasTitleComponents());
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
        CutOffWordsNodeContext context = new CutOffWordsNodeContext(
            OPERATOR_UID, null, pricesRegistry,
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new")),
            null
        );

        AuditNode result = new CutOffWordsNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(makeAliasTitleComponents());
        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_CHANGES.name(),
            ColumnDefinition.OPERATOR_PRICE.name()
        );
    }

    @Test
    public void whenInspectorChangedThenOnlyInspectorDataKeyPresent() {
        CutOffWordsNodeContext context = new CutOffWordsNodeContext(
            null, INSPECTOR_UID, pricesRegistry,
            null,
            Collections.singletonList(modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new"))
        );

        AuditNode result = new CutOffWordsNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(makeAliasTitleComponents());
        Assertions.assertThat(result.getData()).containsOnlyKeys(
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_CHANGES.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    private List<BaseComponent> makeAliasTitleComponents() {
        return Collections.singletonList(new Text("Отсекающие слова"));
    }
}
