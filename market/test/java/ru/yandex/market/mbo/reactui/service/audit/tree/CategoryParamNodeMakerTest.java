package ru.yandex.market.mbo.reactui.service.audit.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.billing.counter.base.squash.GroupingSquashStrategy;
import ru.yandex.market.mbo.db.params.guru.GuruVendorsReader;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.dto.billing.components.BaseComponent;
import ru.yandex.market.mbo.reactui.dto.billing.components.Link;
import ru.yandex.market.mbo.reactui.dto.billing.components.Text;
import ru.yandex.market.mbo.reactui.service.audit.AuditUrlUtils;
import ru.yandex.market.mbo.statistic.model.SquashedUserActions;

import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.statistic.AuditTestHelper.modelSingleParamAction;

public class CategoryParamNodeMakerTest {

    private static final long PARAM_ID = 1L;
    private static final long CATEGORY_ID = 1L;
    private static final long OPTION_ID = 1L;
    private static final String OPTION_NAME = "option1";
    private static final long USER_ID = 1L;
    private static final String PARAM_NAME = "param1";

    @Test
    public void withoutCreation() {
        AuditAction action = modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new");
        action.setEntityName(PARAM_NAME);

        List<SquashedUserActions.CategoryParamActions> actions = Collections.singletonList(
            new SquashedUserActions.CategoryParamActions()
                .setOptionId(OPTION_ID)
                .setChangedAliases(Map.of(new GroupingSquashStrategy.PropertyValue("", ""),
                    Collections.singletonList(action))));

        CategoryParamNodeContext context = new CategoryParamNodeContext(
            PARAM_ID,
            CATEGORY_ID,
            PARAM_NAME,
            actions,
            actions,
            null
        );
        Assert.assertFalse(context.isCreated());

        AuditNode result = new CategoryParamNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(makeCategoryParamNodeTitle());
        Assertions.assertThat(result.getData()).isEmpty();
    }

    @Test
    public void withCreation() {
        AuditAction action = modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new");
        action.setEntityName(PARAM_NAME);

        AuditAction creationAction = modelSingleParamAction(PARAM_ID, PARAM_NAME, "old", "new");
        creationAction.setEntityName(PARAM_NAME);
        creationAction.setUserId(USER_ID);

        List<SquashedUserActions.CategoryParamActions> actions = Collections.singletonList(
            new SquashedUserActions.CategoryParamActions()
                .setOptionId(OPTION_ID)
                .setChangedAliases(Map.of(new GroupingSquashStrategy.PropertyValue("", ""),
                    Collections.singletonList(action)))
                .setCreateAction(Optional.of(creationAction)));

        GuruVendorsReader guruVendorsReader = Mockito.mock(GuruVendorsReader.class);
        OptionImpl option = new OptionImpl(new OptionImpl(OPTION_ID, OPTION_NAME),
            Option.OptionType.VENDOR);
        when(guruVendorsReader.getVendor(Mockito.eq(OPTION_ID))).thenReturn(option);

        CategoryParamNodeContext context = new CategoryParamNodeContext(
            PARAM_ID,
            CATEGORY_ID,
            PARAM_NAME,
            actions,
            actions,
            guruVendorsReader
        );
        Assert.assertTrue(context.isCreated());

        AuditNode result = new CategoryParamNodeMaker().apply(context).build();

        Assertions.assertThat(result.getTitle()).usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(makeCategoryParamNodeTitle());
        Assertions.assertThat(result.getData()).isEmpty();
    }

    private List<BaseComponent> makeCategoryParamNodeTitle() {
        return Arrays.asList(
            new Text("Параметр"),
            new Link(String.valueOf(PARAM_ID), AuditUrlUtils.makeParameterUrl(PARAM_ID, CATEGORY_ID)),
            new Text(PARAM_NAME)
        );
    }

}
