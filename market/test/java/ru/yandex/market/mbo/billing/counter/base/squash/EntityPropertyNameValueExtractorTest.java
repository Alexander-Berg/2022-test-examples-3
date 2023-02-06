package ru.yandex.market.mbo.billing.counter.base.squash;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.billing.AuditActionTestFactory;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 20.11.2018
 */
public class EntityPropertyNameValueExtractorTest {

    private static final long USER_ID = 7724059375L;

    private AuditActionTestFactory actionFactory;

    @Before
    public void before() {
        actionFactory = new AuditActionTestFactory(AuditAction.EntityType.CATEGORY);
    }

    @Test
    public void useOldValueIfNewMissed() {
        AuditAction action1 = actionFactory.createAction(1, USER_ID, AuditAction.ActionType.CREATE, "some-value", "");
        AuditAction action2 = actionFactory.createAction(1, USER_ID, AuditAction.ActionType.CREATE, "", "some-value");

        assertThat(SquashStrategy.entityPropertyNameValueExtractor(action1))
            .isEqualTo(
                SquashStrategy.entityPropertyNameValueExtractor(action2)
            );
    }

    @Test
    public void actionTypeDontAffectKey() {
        AuditAction action1 = actionFactory.createAction(1, USER_ID, AuditAction.ActionType.CREATE, "", "some-value");
        AuditAction action2 = actionFactory.createAction(1, USER_ID, AuditAction.ActionType.DELETE, "", "some-value");

        assertThat(SquashStrategy.entityPropertyNameValueExtractor(action1))
            .isEqualTo(
                SquashStrategy.entityPropertyNameValueExtractor(action2)
            );
    }

    @Test
    public void differentValuesHaveDifferentKeys() {
        AuditAction action1 = actionFactory.createAction(1, USER_ID, AuditAction.ActionType.CREATE, "", "valueA");
        AuditAction action2 = actionFactory.createAction(1, USER_ID, AuditAction.ActionType.CREATE, "", "valueB");

        assertThat(SquashStrategy.entityPropertyNameValueExtractor(action1))
            .isNotEqualTo(
                SquashStrategy.entityPropertyNameValueExtractor(action2)
            );
    }

    @Test
    public void differentPropertyNamesHaveDifferentKeys() {
        AuditAction action1 = actionFactory.createAction(1, USER_ID, AuditAction.ActionType.CREATE, "", "value");
        AuditAction action2 = actionFactory.createAction(1, USER_ID, AuditAction.ActionType.CREATE, "", "value");
        action1.setPropertyName("prop-name-A");
        action2.setPropertyName("prop-name-B");

        assertThat(SquashStrategy.entityPropertyNameValueExtractor(action1))
            .isNotEqualTo(
                SquashStrategy.entityPropertyNameValueExtractor(action2)
            );
    }
}
