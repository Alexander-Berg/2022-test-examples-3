package ru.yandex.market.mbo.db.navigation;

import com.google.common.collect.Collections2;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.core.audit.AuditService;
import ru.yandex.market.mbo.db.navigation.stubs.NavigationTreeNodeRedirectServiceH2MemImpl;
import ru.yandex.market.mbo.db.navigation.stubs.NavigationTreePublishServiceStub;
import ru.yandex.market.mbo.db.navigation.stubs.NavigationTreeServiceStub;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationMenu;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.SyncType;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.market.mbo.db.navigation.CopySubTreeNodesData.getAllIds;
import static ru.yandex.market.mbo.db.navigation.NavigationTreePublishService.DRAFT_SCHEME_NAME;
import static ru.yandex.market.mbo.db.navigation.NavigationTreePublishService.PUBLISHED_SCHEME_NAME;

/**
 * @author york
 * @since 17.08.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class NavigationTreePublishTest {
    private static final String TEST_CODE = "test";
    private Long idseq = 1L;
    private static final int INHERITED_OFFSET = 100;

    private NavigationTreePublishServiceStub navigationTreePublishService;
    private NavigationTreeServiceStub published = new NavigationTreeServiceStub();
    private NavigationTreeServiceStub draft = new NavigationTreeServiceStub();
    private NavigationTreeNodeRedirectService redirectService;
    private NavigationTreeCopyService copyService = new NavigationTreeCopyService();

    private static final String PUB = PUBLISHED_SCHEME_NAME;
    private static final String DRAFT = DRAFT_SCHEME_NAME;

    @Before
    public void init() {
        navigationTreePublishService = new NavigationTreePublishServiceStub();
        navigationTreePublishService.setAuditService(Mockito.mock(AuditService.class));

        redirectService = new NavigationTreeNodeRedirectServiceH2MemImpl();

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = createNamedParameterJdbcTemplate();
        TransactionTemplate transactionTemplate = createTransactionTemplateMock();

        navigationTreePublishService.setSiteCatalogNamedJdbcTemplate(namedParameterJdbcTemplate);
        navigationTreePublishService.setSiteCatalogTransactionTemplate(transactionTemplate);
        navigationTreePublishService.setNavigationTreeService(published);
        navigationTreePublishService.setNavigationTreeServiceDraft(draft);
        navigationTreePublishService.setNavigationNodeRedirectService(redirectService);

    }

    @NotNull
    private NamedParameterJdbcTemplate createNamedParameterJdbcTemplate() {
        JdbcOperations jdbcOperations = Mockito.mock(JdbcOperations.class);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        Mockito.when(namedParameterJdbcTemplate.getJdbcOperations()).thenReturn(jdbcOperations);
        return namedParameterJdbcTemplate;
    }

    @NotNull
    private TransactionTemplate createTransactionTemplateMock() {
        TransactionTemplate transactionTemplate = Mockito.mock(TransactionTemplate.class);
        Mockito.when(transactionTemplate.execute(Mockito.any())).then(invocationOnMock -> {
            TransactionCallback callback = invocationOnMock.getArgument(0);
            Object result = callback.doInTransaction(null);
            return result;
        });
        return transactionTemplate;
    }

    @Test
    public void testSimplePublish() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);

        int nodeCount = getNodesCount(tree);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        Assert.assertEquals(0, navigationTreePublishService.getRemoved(PUB).size());
        Assert.assertEquals(nodeCount, navigationTreePublishService.getCopied(PUB).size());
        Assert.assertEquals(0, navigationTreePublishService.getInheritedAsNormal(PUB).size());
        Assert.assertEquals(0, navigationTreePublishService.getInheritedEmpty(PUB).size());
    }


    @Test
    public void testNewRemoveOutgoingRedirectsNone() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        int nodeCount = getNodesCount(tree);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        Set<Long> removedOutgoingRedirects =
            navigationTreePublishService.getRemovedOutgoingRedirects(tree.getCode());
        Assert.assertEquals(0,
            removedOutgoingRedirects.size());
    }

    @Test
    public void testPublishRemoveOutgoingRedirectsNone() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        published.saveNavigationTree(null, tree, true);
        int nodeCount = getNodesCount(tree);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        Set<Long> removedOutgoingRedirects =
            navigationTreePublishService.getRemovedOutgoingRedirects(tree.getCode());
        Assert.assertEquals(0,
            removedOutgoingRedirects.size());
    }

    @Test
    public void testPublishRemoveOutgoingRedirectsOneUnpublished() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        idseq = 1L;
        NavigationTree old = createSampleTree();
        old.getRoot().getChildren().get(0).getData().setPublished(false);
        published.saveNavigationTree(null, old, true);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        Set<Long> removedOutgoingRedirects =
            navigationTreePublishService.getRemovedOutgoingRedirects(tree.getCode());
        Assert.assertEquals(1,
            removedOutgoingRedirects.size());
    }

    @Test
    public void testSimplePublishRemoveOutgoingRedirectsOneUnpublished2() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        tree.getRoot().getData().setPublished(false);
        idseq = 1L;
        NavigationTree old = createSampleTree();
        old.getRoot().getData().setPublished(false);
        old.getRoot().getChildren().get(0).getData().setPublished(false);
        published.saveNavigationTree(null, old, true);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        Set<Long> removedOutgoingRedirects =
            navigationTreePublishService.getRemovedOutgoingRedirects(tree.getCode());
        Assert.assertEquals(0,
            removedOutgoingRedirects.size());
    }

    @Test
    public void testSimplePublishRemoveOutgoingRedirectsOneHidden() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        tree.getRoot().getData().setPublished(false);
        idseq = 1L;
        NavigationTree old = createSampleTree();
        old.getRoot().getData().setPublished(false);
        old.getRoot().getChildren().get(0).getData().setIsHidden(true);
        published.saveNavigationTree(null, old, true);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        Set<Long> removedOutgoingRedirects =
            navigationTreePublishService.getRemovedOutgoingRedirects(tree.getCode());
        Assert.assertEquals(0,
            removedOutgoingRedirects.size());
    }


    @Test
    public void testSimplePublishRemoveOutgoingRedirectsOneHidden2() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        tree.getRoot().getData().setPublished(true);
        idseq = 1L;
        NavigationTree old = createSampleTree();
        old.getRoot().getData().setPublished(true);
        old.getRoot().getChildren().get(0).getData().setIsHidden(true);
        published.saveNavigationTree(null, old, true);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        Set<Long> removedOutgoingRedirects =
            navigationTreePublishService.getRemovedOutgoingRedirects(tree.getCode());
        Assert.assertEquals(1,
            removedOutgoingRedirects.size());
    }

    @Test
    public void testSimplePublishRemoveOutgoingRedirectsOneHidden3() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        tree.getRoot().getData().setPublished(true);
        tree.getRoot().getChildren().get(0).getData().setIsHidden(true);
        idseq = 1L;
        NavigationTree old = createSampleTree();
        old.getRoot().getData().setPublished(false);
        published.saveNavigationTree(null, old, true);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        Set<Long> removedOutgoingRedirects =
            navigationTreePublishService.getRemovedOutgoingRedirects(tree.getCode());
        Assert.assertEquals(5,
            removedOutgoingRedirects.size());
    }

    @Test
    public void testSimplePublishRemoveOutgoingRedirectsOneHidden4() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        tree.getRoot().getData().setPublished(true);
        tree.getRoot().getChildren().get(0).getData().setIsHidden(true);
        idseq = 1L;
        NavigationTree old = createSampleTree();
        old.getRoot().getData().setPublished(false);
        tree.getRoot().getChildren().get(0).getData().setIsHidden(true);
        published.saveNavigationTree(null, old, true);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        Set<Long> removedOutgoingRedirects =
            navigationTreePublishService.getRemovedOutgoingRedirects(tree.getCode());
        Assert.assertEquals(5,
            removedOutgoingRedirects.size());
    }

    @Test
    public void testSimplePublishRemoveOutgoingRedirectsAllPublish() {
        NavigationTree tree = createSampleTree();
        tree.getRoot().getData().setPublished(true);
        draft.saveNavigationTree(null, tree, true);
        idseq = 1L;
        NavigationTree old = createSampleTree();
        old.getRoot().getData().setPublished(false);
        published.saveNavigationTree(null, old, true);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        Set<Long> removedOutgoingRedirects =
            navigationTreePublishService.getRemovedOutgoingRedirects(tree.getCode());
        Assert.assertEquals(5,
            removedOutgoingRedirects.size());
    }

    @Test
    public void testSimplePublishRemoveOutgoingRedirectsOneHidden1() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        idseq = 1L;
        NavigationTree tree2 = createSampleTree();
        tree2.getRoot().getChildren().get(0).getData().setIsHidden(true);
        published.saveNavigationTree(null, tree2, true);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        Set<Long> removedOutgoingRedirects =
            navigationTreePublishService.getRemovedOutgoingRedirects(tree.getCode());
        Assert.assertEquals(1,
            removedOutgoingRedirects.size());
    }


    @Test
    public void testSimplePublish2() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        published.saveNavigationTree(null, tree, true);

        NavigationNode n0 = tree.getRoot().getChild(0).getData();
        NavigationNode n1 = tree.getRoot().getChild(1).getData();

        List<Long> added = published.deleteNavigationNode(n1.getId());
        List<Long> deleted = draft.deleteNavigationNode(n0.getId());

        navigationTreePublishService.publishTree(0, tree.getId(), true);
        MatcherAssert.assertThat(navigationTreePublishService.getRemoved(PUB),
            containsInAnyOrder(minus(tree, added)));
        MatcherAssert.assertThat(navigationTreePublishService.getCopied(PUB),
            containsInAnyOrder(minus(tree, deleted)));
        Assert.assertEquals(0, navigationTreePublishService.getInheritedAsNormal(PUB).size());
        Assert.assertEquals(0, navigationTreePublishService.getInheritedEmpty(PUB).size());
    }

    @Test
    public void testNotPublishInheritedIfAbsent() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        NavigationTree inherited = inherit(tree);
        draft.saveNavigationTree(null, inherited, true);

        int nodeCount = getNodesCount(tree);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        //inherited tree will not be published cause it is not existed in published scheme
        Assert.assertEquals(0, navigationTreePublishService.getRemoved(PUB).size());
        Assert.assertEquals(nodeCount, navigationTreePublishService.getCopied(PUB).size());
        Assert.assertEquals(0, navigationTreePublishService.getInheritedAsNormal(PUB).size());
        Assert.assertEquals(0, navigationTreePublishService.getInheritedEmpty(PUB).size());
    }

    @Test
    public void testPublishAddedWithInherited() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        published.saveNavigationTree(null, tree, true);
        NavigationTree inherited = inherit(tree);
        draft.saveNavigationTree(null, inherited, true);
        published.saveNavigationTree(null, inherited, true);

        NavigationNode inh = inherited.getRoot().getChild(1).getData();
        List<Long> absentInheritedInTarget =
            published.deleteNavigationNode(inh.getId());
        List<Long> absentInTarget =
            published.deleteNavigationNode(inh.getMasterNodeId());

        //adding simple node to inherited
        draft.addNavigationTreeNode(null, inh.getParentId(), new TreeNode<>(simple()));

        //publishing base tree with some added nodes => inherited tree will have added inherited empty nodes
        int nodeCount = getNodesCount(tree);
        navigationTreePublishService.publishTree(0, tree.getId(), true);
        //removing only tree from target
        MatcherAssert.assertThat(navigationTreePublishService.getRemoved(PUB),
            containsInAnyOrder(minus(tree, absentInTarget)));

        Assert.assertEquals(nodeCount, navigationTreePublishService.getCopied(PUB).size());
        Assert.assertEquals(0, navigationTreePublishService.getInheritedAsNormal(PUB).size());

        MatcherAssert.assertThat(navigationTreePublishService.getInheritedEmpty(PUB),
            containsInAnyOrder(idsArray(absentInheritedInTarget)));
    }

    /**
     * Inherited node will be deleted foreach deleted node in parent tree.
     */
    @Test
    public void testPublishWithInheritedDeleting() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        published.saveNavigationTree(null, tree, true);
        NavigationTree inherited = inherit(tree);
        draft.saveNavigationTree(null, inherited, true);
        published.saveNavigationTree(null, inherited, true);

        NavigationNode node = tree.getRoot().getChildren().get(1).getData();
        List<Long> deleted = draft.deleteNavigationNode(node.getId());
        List<Long> inheritedToDelete = inherited.getRoot().findAll(nd -> deleted.contains(nd.getMasterNodeId()))
            .stream().map(nd -> nd.getData().getId()).collect(Collectors.toList());
        Assert.assertEquals(deleted.size(), inheritedToDelete.size());

        navigationTreePublishService.publishTree(0, tree.getId(), true);

        MatcherAssert.assertThat(navigationTreePublishService.getRemoved(PUB),
            containsInAnyOrder(idsArray(tree.getAllNodeIds(), inheritedToDelete)));

        MatcherAssert.assertThat(navigationTreePublishService.getCopied(PUB),
            containsInAnyOrder(minus(tree, deleted)));

        Assert.assertEquals(0, navigationTreePublishService.getInheritedAsNormal(PUB).size());
        Assert.assertEquals(0, navigationTreePublishService.getInheritedEmpty(PUB).size());
    }


    /**
     * All inherited nodes will be added as simple (if source is missing).
     */
    @Test
    public void testInheritedPublishing() {
        NavigationTree tree = createSampleTree();
        draft.saveNavigationTree(null, tree, true);
        NavigationTree inherited = inherit(tree);
        inherited.getRoot().addChild(simple());
        draft.saveNavigationTree(null, inherited, true);
        published.saveNavigationTree(null, tree, true);

        NavigationNode node = tree.getRoot().getChild(1).getData();
        List<Long> deleted = published.deleteNavigationNode(node.getId());
        List<Long> inheritedWithAbsentSource = getInheritedIds(inherited, deleted);

        int nodeCount = getNodesCount(inherited);

        navigationTreePublishService.publishTree(0, inherited.getId(), true);

        //tree is not published yet
        Assert.assertEquals(0, navigationTreePublishService.getRemoved(PUB).size());

        Assert.assertEquals(nodeCount - inheritedWithAbsentSource.size(),
            navigationTreePublishService.getCopied(PUB).size());

        Assert.assertEquals(inheritedWithAbsentSource.size(),
            navigationTreePublishService.getInheritedAsNormal(PUB).size());

        MatcherAssert.assertThat(navigationTreePublishService.getInheritedAsNormal(PUB),
            containsInAnyOrder(idsArray(inheritedWithAbsentSource)));

        Assert.assertEquals(0, navigationTreePublishService.getInheritedEmpty(PUB).size());
    }

    @Test
    public void testPublishSubtree() {
        NavigationTree tree = createSampleTree();
        TreeNode<NavigationNode> n0 = tree.getRoot().getChild(0);
        TreeNode<NavigationNode> n1 = tree.getRoot().getChild(1).getChild(1);
        NavigationNode listNode = simple();
        n1.addChild(listNode);

        draft.saveNavigationTree(null, tree, true);
        published.saveNavigationTree(null, tree, true);
        NavigationTree inherited = inherit(tree);
        //adding more child to inherited
        NavigationNode addedToInherited = simple();
        inherited.getRoot().find(nd -> nd.getMasterNodeId() == listNode.getId()).addChild(addedToInherited);
        published.saveNavigationTree(null, inherited, true);
        draft.saveNavigationTree(null, inherited, true);

        List<Long> deleted0 = draft.deleteNavigationNode(n0.getData().getId());
        List<Long> deleted1 = draft.deleteNavigationNode(n1.getData().getId());
        List<Long> inherited1 = getInheritedIds(inherited, deleted1);

        navigationTreePublishService.publishSubTree(0, n1.getData().getParentId(), true);

        List<Long> shouldBeDeleted = new ArrayList<>(getAllIds(n1.getParent()));
        shouldBeDeleted.addAll(inherited1);
        shouldBeDeleted.add(addedToInherited.getId());

        MatcherAssert.assertThat(navigationTreePublishService.getRemoved(PUB),
            containsInAnyOrder(idsArray(shouldBeDeleted)));

        MatcherAssert.assertThat(navigationTreePublishService.getCopied(PUB),
            containsInAnyOrder(minus(getAllIds(n1.getParent()), deleted1)));

        Assert.assertEquals(0, navigationTreePublishService.getInheritedAsNormal(PUB).size());

        Assert.assertEquals(0, navigationTreePublishService.getInheritedEmpty(PUB).size());
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testPublishMenuWithDeletion() {
        NavigationTree tree = createSampleTree();
        published.saveNavigationTree(null, tree, true);
        long id = 1234;
        NavigationMenu menu = new NavigationMenu();
        menu.setId(id);
        menu.setDomain("ru");
        menu.setName("name");
        menu.setNavigationTreeId(tree.getId());
        published.saveNavigationMenu(menu);
        List<NavigationMenu> menus = published.getNavigationMenuList();
        Assertions.assertThat(menus).hasSize(1).extracting(NavigationMenu::getId).containsExactly(id);

        navigationTreePublishService.publishMenu(id);

        menus = published.getNavigationMenuList();
        Assertions.assertThat(menus).isEmpty();
    }

    private List<Long> getInheritedIds(NavigationTree inherited, List<Long> nodeIds) {
        return inherited.getRoot()
            .findAll(nd -> nodeIds.contains(nd.getMasterNodeId()))
            .stream()
            .map(tnd -> tnd.getData().getId())
            .collect(Collectors.toList());
    }

    private Long[] idsArray(Collection<Long>... collections) {
        return Arrays.stream(collections)
            .flatMap(c -> c.stream())
            .collect(Collectors.toList())
            .toArray(new Long[0]);
    }

    private Long[] minus(NavigationTree tree, List<Long> deleted) {
        return minus(tree.getAllNodeIds(), deleted);
    }

    private Long[] minus(Collection<Long> ids, List<Long> deleted) {
        return Collections2.filter(ids, id -> !deleted.contains(id))
            .toArray(new Long[0]);
    }


    private int getNodesCount(NavigationTree tree) {
        return getAllIds(tree.getRoot()).size();
    }

    /*
          2
         / \
       3   4
          / \
         5   6
     */
    private NavigationTree createSampleTree() {
        NavigationTree tree = new NavigationTree();
        tree.setId(idseq++);
        tree.setName("tree " + tree.getId());
        tree.setCode(TEST_CODE);
        TreeNode<NavigationNode> root = new TreeNode<>(simple());
        tree.setRoot(root);
        root.addChild(simple());
        root.addChild(simple());
        root.getChildren().get(1).addChild(simple());
        root.getChildren().get(1).addChild(simple());
        return tree;
    }

    private NavigationTree inherit(NavigationTree tree) {
        TreeNode<NavigationNode> newRoot = copyService.getNavigationTreeCopy(tree,
            tree.getRootNodeId(),
            NavigationTreeCopyService.NodeCopyType.INHERIT,
            false,
            null,
            null,
            null,
            null);
        newRoot.findAll(nd -> {
            nd.setId(nd.getMasterNodeId() + INHERITED_OFFSET);
            return true;
        });
        NavigationTree result = new NavigationTree();
        result.setRoot(newRoot);
        result.setId(tree.getId() + INHERITED_OFFSET);
        result.setCode("inherited_" + tree.getCode());
        result.setSyncTreeId(tree.getId());
        result.setSyncType(SyncType.FULL);
        result.setName("inherited " + tree.getName());
        return result;
    }

    private NavigationNode simple() {
        SimpleNavigationNode result = SimpleNavigationNode.newValue();
        result.setId(idseq++);
        result.setName("node" + result.getId());
        result.setPublished(true);
        result.setIsHidden(false);
        return result;
    }
}
