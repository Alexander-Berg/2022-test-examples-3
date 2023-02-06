package ru.yandex.market.mbo.reactui.service.audit.tree;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.dto.billing.components.BaseComponent;
import ru.yandex.market.mbo.reactui.dto.billing.components.Text;
import ru.yandex.market.mbo.reactui.service.audit.BillingPricesRegistry;
import ru.yandex.market.mbo.reactui.service.audit.ColumnDefinition;
import ru.yandex.market.mbo.reactui.service.audit.MockUtils;

import java.util.Arrays;
import java.util.List;

public class CategoryParamCreateNodeMakerTest {

    private static final long UID = 1L;
    private static final long OPTION_ID = 1L;
    private static final String OPTION_NAME = "option1";

    private BillingPricesRegistry pricesRegistry = MockUtils.mockBillingPricesRegistry();

    @Test
    public void whenOperatorCreated() {
        creationTest(true);
    }

    @Test
    public void whenInspectorCreated() {
        creationTest(false);
    }

    public void creationTest(boolean createdByOperator) {
        CategoryParamCreateNodeContext context = new CategoryParamCreateNodeContext(createdByOperator,
            UID, new AuditAction(), OPTION_ID, OPTION_NAME, pricesRegistry);
        AuditNode result = new CategoryParamCreateNodeMaker().apply(context).build();
        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(makeParamCreateNodeTitle());

        Assertions.assertThat(result.getData()).containsOnlyKeys(
            createdByOperator ? ColumnDefinition.OPERATOR_ACTION.name() : ColumnDefinition.INSPECTOR_ACTION.name(),
            createdByOperator ? ColumnDefinition.OPERATOR_PRICE.name() : ColumnDefinition.INSPECTOR_PRICE.name()
        );
    }

    private List<BaseComponent> makeParamCreateNodeTitle() {
        return Arrays.asList(
            new Text(String.valueOf(OPTION_ID)),
            new Text(OPTION_NAME)
        );
    }

}
