package ru.yandex.market.jmf.module.comment.test.strategy;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.impl.ui.table.batch.CreateInternalCommentMultiActionStrategy;
import ru.yandex.market.jmf.module.comment.test.InternalModuleCommentTestConfiguration;
import ru.yandex.market.jmf.security.action.SecurityService;
import ru.yandex.market.jmf.ui.api.content.toolbar.Action;

import static org.mockito.Mockito.when;

@Transactional
@SpringJUnitConfig(InternalModuleCommentTestConfiguration.class)
public class CreateInternalCommentMultiActionStrategyTest {

    private static final Fqn ROOT_MC_FQN = Fqn.of("rootMC");
    private static final Fqn CHILD_1_FQN = Fqn.of("rootMC$child1");
    private static final Fqn CHILD_2_FQN = Fqn.of("rootMC$child2");

    @Inject
    private BcpService bcpService;

    @Inject
    private MetadataService metadataService;

    @Mock
    private SecurityService securityService;

    private CreateInternalCommentMultiActionStrategy strategy;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        strategy = new CreateInternalCommentMultiActionStrategy(securityService);
    }

    @Test
    public void testHasPermissions() {
        Entity entity1 = bcpService.create(CHILD_1_FQN, Collections.emptyMap());
        Entity entity2 = bcpService.create(CHILD_2_FQN, Collections.emptyMap());

        when(securityService.hasPermission(entity1, Comment.Security.CREATE_INTERNAL_COMMENT_ACTION))
                .thenReturn(true);
        when(securityService.hasPermission(entity2, Comment.Security.CREATE_INTERNAL_COMMENT_ACTION))
                .thenReturn(true);

        assertActions(ROOT_MC_FQN, Set.of(entity1, entity2), true);
    }

    @Test
    public void testOnlyOneEntityHasPermission() {
        Entity entity1 = bcpService.create(CHILD_1_FQN, Collections.emptyMap());
        Entity entity2 = bcpService.create(CHILD_2_FQN, Collections.emptyMap());

        when(securityService.hasPermission(entity1, Comment.Security.CREATE_INTERNAL_COMMENT_ACTION))
                .thenReturn(true);
        when(securityService.hasPermission(entity2, Comment.Security.CREATE_INTERNAL_COMMENT_ACTION))
                .thenReturn(false);

        assertActions(ROOT_MC_FQN, Set.of(entity1, entity2), false);
    }

    @Test
    public void testNoPermission() {
        Entity entity1 = bcpService.create(CHILD_1_FQN, Collections.emptyMap());
        Entity entity2 = bcpService.create(CHILD_2_FQN, Collections.emptyMap());

        when(securityService.hasPermission(entity1, Comment.Security.CREATE_INTERNAL_COMMENT_ACTION))
                .thenReturn(false);
        when(securityService.hasPermission(entity2, Comment.Security.CREATE_INTERNAL_COMMENT_ACTION))
                .thenReturn(false);

        assertActions(ROOT_MC_FQN, Set.of(entity1, entity2), false);
    }

    private void assertActions(Fqn fqn, Collection<Entity> entities, boolean available) {
        Collection<Action> actions = strategy.getActions(metadataService.getMetaclass(fqn), entities);
        Assertions.assertNotNull(actions);
        if (available) {
            Assertions.assertEquals(1, actions.size());
            Action action = actions.iterator().next();
            Assertions.assertEquals("addComment", action.getAction());
            Assertions.assertEquals("добавить комментарий", action.getTitle());
        } else {
            Assertions.assertTrue(actions.isEmpty());
        }
    }
}
