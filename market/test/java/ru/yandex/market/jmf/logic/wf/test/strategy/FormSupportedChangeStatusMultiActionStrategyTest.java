package ru.yandex.market.jmf.logic.wf.test.strategy;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.logic.wf.WfSecurityService;
import ru.yandex.market.jmf.logic.wf.WfService;
import ru.yandex.market.jmf.logic.wf.impl.WfUtils;
import ru.yandex.market.jmf.logic.wf.impl.ui.table.batch.ChangeStatusMultiActionUtils;
import ru.yandex.market.jmf.logic.wf.impl.ui.table.batch.FormSupportedChangeStatusMultiActionStrategy;
import ru.yandex.market.jmf.logic.wf.test.InternalLogicWfTestConfiguration;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.ui.api.content.toolbar.Action;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Transactional
@SpringJUnitConfig(InternalLogicWfTestConfiguration.class)
public class FormSupportedChangeStatusMultiActionStrategyTest {

    private static final Fqn ROOT_MC_FQN = Fqn.of("root2MC");
    private static final Fqn CHILD_FQN = Fqn.of("root2MC$child");
    private static final Fqn CHILD2_FQN = Fqn.of("root2MC$child2");
    private static final Fqn CHILD_SAME_WORKFLOW_FQN = Fqn.of("root2MC$sameWorkflow");
    private static final Fqn CHILD_SAME_WORKFLOW_2_FQN = Fqn.of("root2MC$sameWorkflow2");
    private static final Fqn WITHOUT_WF_FQN = Fqn.of("withoutWf$type1");

    private static final String STATUS_1 = "s1";
    private static final String STATUS_2 = "s2";
    private AutoCloseable mocks;

    @Inject
    private WfService wfService;
    @Inject
    private BcpService bcpService;
    @Inject
    private WfUtils wfUtils;
    @Inject
    private MetadataService metadataService;
    @Inject
    private ChangeStatusMultiActionUtils changeStatusMultiActionUtils;
    @Inject
    private ConfigurationService configurationService;
    @Mock
    private WfSecurityService wfSecurityService;
    private FormSupportedChangeStatusMultiActionStrategy strategy;

    @BeforeEach
    public void setUp() {

        mocks = MockitoAnnotations.openMocks(this);

        strategy = new FormSupportedChangeStatusMultiActionStrategy(wfService, wfSecurityService,
                changeStatusMultiActionUtils);

        when(wfSecurityService.couldPerformTransition(any(), any())).thenReturn(true);
    }

    @AfterEach
    public void tearDown() throws Exception {
        mocks.close();
    }

    // должны показывать
    @Test
    public void testEntitiesSameTypesSameStatus() {
        Entity entity1 = bcpService.create(CHILD_FQN, Collections.emptyMap());
        Entity entity2 = bcpService.create(CHILD_FQN, Collections.emptyMap());

        assertActions(ROOT_MC_FQN, Set.of(entity1, entity2), Set.of(STATUS_1));
    }

    // В текущей реализации должно быть пусто т.к. для разных типов считаем workflow разными,
    // даже если workflow определен только для класса-родителя, а для потомков явно не определен
    @Test
    public void testEntitiesDifferentTypesSameWorkflowSameStatuses() {
        Entity entity1 = bcpService.create(CHILD_SAME_WORKFLOW_FQN, Collections.emptyMap());
        Entity entity2 = bcpService.create(CHILD_SAME_WORKFLOW_2_FQN, Collections.emptyMap());

        assertActions(ROOT_MC_FQN, Set.of(entity1, entity2), Set.of());
    }

    // Тут должно быть пусто т.к. разные начальные статусы и поэтому могут отличаться необходимые атрибуты
    @Test
    public void testEntitiesDifferentTypesSameWorkflowDifferentStatuses() {
        Entity entity1 = bcpService.create(CHILD_SAME_WORKFLOW_FQN, Collections.emptyMap());
        Entity entity2 = bcpService.create(CHILD_SAME_WORKFLOW_2_FQN, Collections.emptyMap());
        bcpService.edit(entity2, Map.of(HasWorkflow.STATUS, STATUS_1));

        assertActions(ROOT_MC_FQN, Set.of(entity1, entity2), Set.of());
    }

    // Тут должно быть пусто т.к. разные workFlow и поэтому могут отличаться необходимые атрибуты
    @Test
    public void testEntitiesDifferentTypesDifferentWorkflowSameStatuses() {
        Entity entity1 = bcpService.create(CHILD2_FQN, Collections.emptyMap());
        Entity entity2 = bcpService.create(CHILD_FQN, Collections.emptyMap());

        assertActions(ROOT_MC_FQN, Set.of(entity1, entity2), Set.of());
    }

    @Test
    public void testCheckEditPermissions() {
        Entity entity1 = bcpService.create(CHILD_FQN, Collections.emptyMap());
        when(wfSecurityService.couldPerformTransition(eq((HasWorkflow) entity1), any())).thenReturn(false);

        assertActions(ROOT_MC_FQN, Set.of(entity1), Set.of());
    }

    @Test
    public void testEntitiesWithoutWf() {
        Entity entity1 = bcpService.create(WITHOUT_WF_FQN, Collections.emptyMap());
        Entity entity2 = bcpService.create(WITHOUT_WF_FQN, Collections.emptyMap());

        assertActions(WITHOUT_WF_FQN, Set.of(entity1, entity2), Set.of());
    }

    private void assertActions(Fqn fqn, Collection<Entity> entities, Set<String> titles) {
        Collection<Action> actions = strategy.getActions(metadataService.getMetaclass(fqn), entities);
        Assertions.assertNotNull(actions);
        Assertions.assertEquals(Set.copyOf(titles), Set.copyOf(CrmCollections.transform(actions, Action::getTitle)));
        MatcherAssert.assertThat(CrmCollections.transform(actions, Action::getAction), everyItem(is("edit")));
    }
}
