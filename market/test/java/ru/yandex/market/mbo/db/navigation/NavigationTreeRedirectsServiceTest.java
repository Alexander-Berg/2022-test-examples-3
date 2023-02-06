package ru.yandex.market.mbo.db.navigation;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.core.navigation.NavigationNodeRedirect;
import ru.yandex.market.mbo.db.navigation.stubs.NavigationTreeNodeRedirectServiceH2MemImpl;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * @author moskovkin@yandex-team.ru
 * @since 25.10.18
 */
@SuppressWarnings("checkstyle:magicnumber")
public class NavigationTreeRedirectsServiceTest {
    private static final String TREE_CODE_1 = "tree1";
    private static final String TREE_CODE_2 = "tree2";
    private static final String TREE_CODE_3 = "tree3";

    private NavigationTreeNodeRedirectService service;
    private NavigationTreeService navigationTreeService;

    private List<NavigationTree> registeredTree = new ArrayList<>();
    private Long idseq = 1L;

    @Before
    public void setUp() {
        navigationTreeService = Mockito.mock(NavigationTreeService.class);
        when(navigationTreeService.getNavigationTrees()).thenReturn(registeredTree);

        createEmptyNavigationTree(TREE_CODE_1);
        createEmptyNavigationTree(TREE_CODE_2);

        service = new NavigationTreeNodeRedirectServiceH2MemImpl();
        service.setNavigationTreeService(navigationTreeService);
    }

    @Test
    public void testGetRedirectsByTreeCode() {
        addTestRecord(TREE_CODE_1, 1L, 2L);
        addTestRecord(TREE_CODE_2, 2L, 3L);

        List<NavigationNodeRedirect> redirect = service.getPermanentRedirects(TREE_CODE_1);
        Assertions.assertThat(redirect).allMatch(r -> TREE_CODE_1.equals(r.getTreeCode()));
    }

    @Test
    public void testGetCollapsedRedirectsCollapse() {
        addTestRecord(TREE_CODE_1, 1L, 2L);
        addTestRecord(TREE_CODE_1, 2L, 3L);

        List<NavigationNodeRedirect> redirect = service.getCollapsedRedirects();
        Assertions.assertThat(redirect).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
            new NavigationNodeRedirect(TREE_CODE_1, 1L, 3L),
            new NavigationNodeRedirect(TREE_CODE_1, 2L, 3L)
        );
    }

    @Test
    public void testGetCollapsedRedirectsSplitTrees() {
        addTestRecord(TREE_CODE_1, 1L, 2L);
        addTestRecord(TREE_CODE_2, 2L, 3L);

        List<NavigationNodeRedirect> redirect = service.getCollapsedRedirects();
        Assertions.assertThat(redirect).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
            new NavigationNodeRedirect(TREE_CODE_1, 1L, 2L),
            new NavigationNodeRedirect(TREE_CODE_2, 2L, 3L)
        );
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testCreateRedirectsForUnpublishedListNode() {
        NavigationTree tree = createSampleTree(TREE_CODE_3);
        tree.findNodeById(6L).getData().setPublished(false);

        List<NavigationNodeRedirect> redirects = service.getCollapsedRedirects();

        Assertions.assertThat(redirects).usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new NavigationNodeRedirect(TREE_CODE_3, 6L, 3L));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testCreateRedirectsForUnpublishedNotListNode() {
        NavigationTree tree = createSampleTree(TREE_CODE_3);
        tree.findNodeById(3L).getData().setPublished(false);

        List<NavigationNodeRedirect> redirects = service.getCollapsedRedirects();

        Assertions.assertThat(redirects).usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new NavigationNodeRedirect(TREE_CODE_3, 3L, 2L),
                new NavigationNodeRedirect(TREE_CODE_3, 5L, 2L),
                new NavigationNodeRedirect(TREE_CODE_3, 6L, 2L));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testCreateRedirectsForUnpublishedNotListNodeAndPermanent() {
        addTestRecord(TREE_CODE_3, 6L, 3L);
        NavigationTree tree = createSampleTree(TREE_CODE_3);
        tree.findNodeById(3L).getData().setPublished(false);

        List<NavigationNodeRedirect> redirects = service.getCollapsedRedirects();

        Assertions.assertThat(redirects).usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new NavigationNodeRedirect(TREE_CODE_3, 3L, 2L),
                new NavigationNodeRedirect(TREE_CODE_3, 5L, 2L),
                new NavigationNodeRedirect(TREE_CODE_3, 6L, 2L));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testCreateRedirectsForUnpublishedTovarNode() {
        addTestRecord(TREE_CODE_3, 6L, 3L);
        NavigationTree tree = createSampleTree(TREE_CODE_3);
        NavigationNode navToUnpublished = tree.findNodeById(3L).getData();
        TovarCategory tovarCategory = new TovarCategory();
        tovarCategory.setPublished(false);
        navToUnpublished.setTovarCategory(tovarCategory);

        List<NavigationNodeRedirect> redirects = service.getCollapsedRedirects();

        Assertions.assertThat(redirects).usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new NavigationNodeRedirect(TREE_CODE_3, 3L, 2L),
                new NavigationNodeRedirect(TREE_CODE_3, 5L, 2L),
                new NavigationNodeRedirect(TREE_CODE_3, 6L, 2L));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testCreateRedirectsForSkipped() {
        NavigationTree tree = createSampleTree(TREE_CODE_3);

        tree.findNodeById(3L).getData().setIsSkipped(true);

        List<NavigationNodeRedirect> redirects = service.getCollapsedRedirects();
        Assertions.assertThat(redirects).usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new NavigationNodeRedirect(TREE_CODE_3, 3L, 2L));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testCreateRedirectOnMainNid() {
        NavigationTree tree = createSampleTree(TREE_CODE_3);
        NavigationNode skippedNode = tree.findNodeById(8L).getData();
        skippedNode.setIsSkipped(true);
        skippedNode.setHid(1L);
        NavigationNode mainNode = tree.findNodeById(3L).getData();
        when(navigationTreeService.getMainNidByHid(1L, tree, true))
            .thenReturn(mainNode);

        List<NavigationNodeRedirect> redirects = service.getCollapsedRedirects();
        Assertions.assertThat(redirects).usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new NavigationNodeRedirect(TREE_CODE_3, 8L, 3L));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testCreateCollapsedRedirects() {
        NavigationTree tree = createSampleTree(TREE_CODE_3);
        tree.findNodeById(8L).getData().setIsSkipped(true);
        tree.findNodeById(4L).getData().setIsSkipped(true);
        addTestRecord(TREE_CODE_3, 42L, 8L);

        List<NavigationNodeRedirect> redirects = service.getCollapsedRedirects();
        Assertions.assertThat(redirects).usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new NavigationNodeRedirect(TREE_CODE_3, 8L, 2L),
                new NavigationNodeRedirect(TREE_CODE_3, 4L, 2L),
                new NavigationNodeRedirect(TREE_CODE_3, 42L, 2L)
            );
    }

    private void addTestRecord(String treeCode, Long sourceNid, Long targetNid) {
        service.addRedirect(
            new NavigationNodeRedirect(treeCode, sourceNid, targetNid)
        );
    }

    private void createEmptyNavigationTree(String treeCode) {
        NavigationTree tree = new NavigationTree();
        tree.setCode(treeCode);
        tree.setRoot(new TreeNode<>());
        registeredTree.add(tree);
    }

    /*
          2
         / \
       3     4
      / \   / \
     5   6 7   8
     */
    private NavigationTree createSampleTree(String treeCode) {
        NavigationTree tree = new NavigationTree();
        tree.setId(idseq++);
        tree.setName("tree " + tree.getId());
        tree.setCode(treeCode);
        TreeNode<NavigationNode> root = new TreeNode<>(simple());
        tree.setRoot(root);
        root.addChild(simple());
        root.addChild(simple());
        root.getChildren().get(0).addChild(simple());
        root.getChildren().get(0).addChild(simple());
        root.getChildren().get(1).addChild(simple());
        root.getChildren().get(1).addChild(simple());
        registeredTree.add(tree);
        return tree;
    }

    private NavigationNode simple() {
        SimpleNavigationNode result = SimpleNavigationNode.newValue();
        result.setId(idseq++);
        result.setName("node" + result.getId());
        result.setPublished(true);
        result.setIsSkipped(false);
        result.setIsMain(false);
        return result;
    }
}
