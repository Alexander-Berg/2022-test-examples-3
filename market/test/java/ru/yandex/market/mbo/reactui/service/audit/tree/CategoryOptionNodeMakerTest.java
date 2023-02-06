package ru.yandex.market.mbo.reactui.service.audit.tree;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.billing.counter.base.squash.GroupingSquashStrategy;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.dto.billing.components.BaseComponent;
import ru.yandex.market.mbo.reactui.dto.billing.components.Text;
import ru.yandex.market.mbo.statistic.model.SquashedUserActions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelSingleParamAction;

/**
 * @author dergachevfv
 * @since 11/25/19
 */
public class CategoryOptionNodeMakerTest {

    private static final long OPTION_ID = 1L;
    private static final long PARAM_ID = 1L;
    private static final String PARAM_NAME = "param1";

    @Test
    public void checkNameAndOptionIdWhenNameInBothListActions() {
        nameTest(true, true);
    }

    @Test
    public void checkNameAndOptionIdWhenOnlyOperatorActionsExists() {
        nameTest(true, false);
    }

    @Test
    public void checkNameAndOptionIdWhenOnlyInspectorActionsExists() {
        nameTest(false, true);
    }

    private void nameTest(boolean setOperatorActions, boolean setInspectorActions) {
        AuditAction action = modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new");
        action.setEntityName(PARAM_NAME);

        SquashedUserActions.CategoryParamActions actions = new SquashedUserActions.CategoryParamActions()
            .setOptionId(OPTION_ID)
            .setChangedAliases(Map.of(new GroupingSquashStrategy.PropertyValue("", ""),
                Collections.singletonList(action)));

        CategoryOptionNodeContext context = new CategoryOptionNodeContext(OPTION_ID,
            setOperatorActions ? actions : null,
            setInspectorActions ? actions : null);

        AuditNode result = new CategoryOptionNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(makeCategoryOptionTitle());
        Assertions.assertThat(result.getData()).isEmpty();
    }

    private List<BaseComponent> makeCategoryOptionTitle() {
        return Arrays.asList(
            new Text("Значение"),
            new Text(String.valueOf(OPTION_ID)),
            new Text(PARAM_NAME));
    }
}
