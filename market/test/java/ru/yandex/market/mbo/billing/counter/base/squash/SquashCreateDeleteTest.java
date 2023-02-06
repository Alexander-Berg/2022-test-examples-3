package ru.yandex.market.mbo.billing.counter.base.squash;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.billing.AuditActionTestFactory;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction.ActionType;

import java.util.Arrays;
import java.util.List;

/**
 * @author yuramalinov
 * @created 09.11.18
 */
@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class SquashCreateDeleteTest {
    private static final String VALUE = "TEST";
    private SquashCreateDelete squash;
    private AuditActionTestFactory actionFactory;

    @Before
    public void setUp() throws Exception {
        squash = new SquashCreateDelete();
        actionFactory = new AuditActionTestFactory(AuditAction.EntityType.CATEGORY);
    }

    @Test
    public void testSimpleOkCase() {
        List<AuditAction> actions = Arrays.asList(
            actionFactory.createAction(1, 1, ActionType.CREATE, null, VALUE));
        List<AuditAction> squashed = squash.apply(actions);
        Assertions.assertThat(squashed).containsExactly(actions.get(0));
    }

    @Test
    public void testCreateDelete() {
        List<AuditAction> actions = Arrays.asList(
            actionFactory.createAction(1, 1, ActionType.CREATE, null, VALUE),
            actionFactory.createAction(1, 1, ActionType.DELETE, VALUE, null)
        );
        List<AuditAction> squashed = squash.apply(actions);
        Assertions.assertThat(squashed).isEmpty();
    }

    @Test
    public void testCreateThenOtherDeleteCreate() {
        List<AuditAction> actions = Arrays.asList(
            actionFactory.createAction(1, 1, ActionType.CREATE, null, VALUE),
            actionFactory.createAction(1, 2, ActionType.DELETE, VALUE, null),
            actionFactory.createAction(1, 2, ActionType.CREATE, null, VALUE)
        );
        List<AuditAction> squashed = squash.apply(actions);
        Assertions.assertThat(squashed).containsExactly(actions.get(0)); // First is recorded
    }

    @Test
    public void testCreateDeleteThenOtherCreate() {
        List<AuditAction> actions = Arrays.asList(
            actionFactory.createAction(1, 1, ActionType.CREATE, null, VALUE),
            actionFactory.createAction(1, 1, ActionType.DELETE, VALUE, null),
            actionFactory.createAction(1, 2, ActionType.CREATE, null, VALUE)
        );
        List<AuditAction> squashed = squash.apply(actions);
        Assertions.assertThat(squashed).containsExactly(actions.get(2)); // Last is recorded
    }

    @Test
    public void testCreateThenOtherDeleteCreateDeleteCreate() {
        List<AuditAction> actions = Arrays.asList(
            actionFactory.createAction(1, 1, ActionType.CREATE, null, VALUE),
            actionFactory.createAction(1, 2, ActionType.DELETE, VALUE, null),
            actionFactory.createAction(1, 2, ActionType.CREATE, null, VALUE),
            actionFactory.createAction(1, 2, ActionType.DELETE, VALUE, null),
            actionFactory.createAction(1, 2, ActionType.CREATE, null, VALUE)
        );
        List<AuditAction> squashed = squash.apply(actions);
        Assertions.assertThat(squashed).containsExactly(actions.get(0)); // No matter what - first is the winner
    }

    @Test
    public void testStrangeSequenceCreateCreate() {
        List<AuditAction> actions = Arrays.asList(
            actionFactory.createAction(1, 1, ActionType.CREATE, null, VALUE),
            actionFactory.createAction(1, 2, ActionType.CREATE, null, VALUE)
        );
        List<AuditAction> squashed = squash.apply(actions);
        Assertions.assertThat(squashed).containsExactly(actions.get(0)); // First is the winner
    }

    @Test
    public void testStrangeSequenceDeleteDelete() {
        List<AuditAction> actions = Arrays.asList(
            actionFactory.createAction(1, 1, ActionType.DELETE, VALUE, null),
            actionFactory.createAction(1, 2, ActionType.DELETE, VALUE, null)
        );
        List<AuditAction> squashed = squash.apply(actions);
        Assertions.assertThat(squashed).containsExactly(actions.get(0)); // First is the winner ?
    }
}
