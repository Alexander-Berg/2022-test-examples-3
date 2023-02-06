package ru.yandex.market.mbo.db.navigation;

import org.assertj.core.util.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author kravchenko-aa
 * @date 17.09.18
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class NavigationTreeConverterTest {

    private NavigationTreeConverter converter;
    @Mock
    private NavigationTreeCopyService copyService;

    @Before
    public void setUp() {
        when(copyService
            .getNavigationTreeCopy(
                any(NavigationTree.class), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
            .thenAnswer(request -> ((NavigationTree) request.getArguments()[0]).getRoot());
        converter = new NavigationTreeConverter();
        converter.setCopyService(copyService);
    }

    @Test
    public void testRemoveLeafNode() {
        TreeNode<NavigationNode> root = createNode(0, false, 1L);
        root.addChild(createNode(0, false, 2L));
        TreeNode<NavigationNode> tmpNode = createNode(1, false, 3L);
        root.addChild(tmpNode);
        tmpNode.addChild(createNode(0, false, 4L));
        tmpNode.addChild(createNode(1, true, 5L));
        tmpNode.addChild(createNode(2, false, 6L));
        tmpNode.addChild(createNode(3, false, 7L));

        NavigationTree tree = new NavigationTree();
        tree.setRoot(root);
        NavigationTree resultTree = converter.getNavigationTreeWithoutSkippedNodes(tree);

        assertEquals(root.getChildren().size(), 2);
        assertFalse(resultTree.getAllNodeIds().contains(5L));
        assertEquals(resultTree.getAllNodeIds().size(), 6);
        assertEquals(tmpNode.getChildren().size(), 3);
        Integer position = 0;
        for (TreeNode<NavigationNode> child : tmpNode.getChildren()) {
            assertEquals(child.getData().getPosition(), position);
            position++;
        }

        assertFalse(haveIntersections(resultTree.getRoot()));
    }

    @Test
    public void testRemoveNodeWithChild() {
        TreeNode<NavigationNode> root = createNode(0, false, 1L);
        root.addChild(createNode(0, false, 2L));
        TreeNode<NavigationNode> tmpNode = createNode(1, true, 3L);
        root.addChild(tmpNode);
        root.addChild(createNode(2, false, 4L));
        tmpNode.addChild(createNode(0, false, 5L));
        tmpNode.addChild(createNode(1, true, 6L));
        tmpNode.addChild(createNode(2, false, 7L));
        tmpNode.addChild(createNode(3, false, 8L));

        NavigationTree tree = new NavigationTree();
        tree.setRoot(root);
        NavigationTree resultTree = converter.getNavigationTreeWithoutSkippedNodes(tree);

        assertEquals(root.getChildren().size(), 5);
        assertFalse(resultTree.getAllNodeIds().contains(3L));
        assertFalse(resultTree.getAllNodeIds().contains(6L));
        assertEquals(resultTree.getAllNodeIds().size(), 6);

        for (TreeNode<NavigationNode> child : tmpNode.getChildren()) {
            assertEquals(child.getParent(), root);
        }

        Integer position = 0;
        for (TreeNode<NavigationNode> child : root.getChildren()) {
            assertEquals(child.getData().getPosition(), position);
            position++;
        }
        assertEquals(root.getChildren().get(2).getData().getId(), 7L);
        assertEquals(root.getChildren().get(4).getData().getId(), 4L);

        assertFalse(haveIntersections(resultTree.getRoot()));
    }

    @Test
    public void testPositionsIntersections() {
        TreeNode<NavigationNode> root = createNode(0, false, 1L);
        root.addChild(createNode(0, false, 2L));
        TreeNode<NavigationNode> tmpNode = createNode(1, true, 3L);
        root.addChild(tmpNode);
        root.addChild(createNode(2, false, 4L));
        tmpNode.addChild(createNode(0, false, 5L));
        tmpNode.addChild(createNode(1, true, 6L));
        tmpNode.addChild(createNode(2, false, 7L));
        tmpNode.addChild(createNode(4, false, 8L));

        NavigationTree tree = new NavigationTree();
        tree.setRoot(root);
        NavigationTree resultTree = converter.getNavigationTreeWithoutSkippedNodes(tree);
        assertFalse(haveIntersections(resultTree.getRoot()));
    }

    @Test
    public void testCodeSafety() {
        TreeNode<NavigationNode> root = createNode(0, false, 1L);
        NavigationTree tree = new NavigationTree();
        tree.setRoot(root);
        tree.setCode("test");

        NavigationTree resultTree = converter.getNavigationTreeWithoutSkippedNodes(tree);

        assertTrue(!Strings.isNullOrEmpty(resultTree.getCode()));
        assertEquals(tree.getCode(), resultTree.getCode());
    }

    private boolean haveIntersections(TreeNode<NavigationNode> node) {
        List<Boolean> results = new ArrayList<>();
        haveIntersectionsRecursively(node, results);

        return results.stream().reduce((x, y) -> x || y).get();
    }

    private void haveIntersectionsRecursively(TreeNode<NavigationNode> node, List<Boolean> results) {
        List<NavigationNode> nodes = node.getChildren().stream().map(TreeNode::getData).collect(Collectors.toList());
        Set<Integer> positions = new HashSet<>();
        boolean haveIntersection = nodes.stream().anyMatch(n -> !positions.add(n.getPosition()));
        results.add(haveIntersection);

        node.getChildren().forEach(n -> haveIntersectionsRecursively(n, results));
    }

    private TreeNode<NavigationNode> createNode(int position, boolean isSkipped, long id) {
        NavigationNode node = new SimpleNavigationNode();
        node.setPosition(position);
        node.setIsSkipped(isSkipped);
        node.setId(id);
        return new TreeNode<>(node);
    }
}
