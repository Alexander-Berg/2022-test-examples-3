package ru.yandex.market.mbo.reactui.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.db.navigation.NavigationTreeFinder;
import ru.yandex.market.mbo.db.navigation.NavigationTreeService;
import ru.yandex.market.mbo.gwt.models.params.ParamNode;
import ru.yandex.market.mbo.gwt.models.tovartree.LinkedNavigationNode;
import ru.yandex.market.mbo.gwt.models.tovartree.LinkedNavigationNodeWithPath;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;
import ru.yandex.market.mbo.reactui.service.impl.LinkedNavigationServiceRemoteImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LinkedNavigationServiceRemoteTest {
    @InjectMocks
    private LinkedNavigationServiceRemoteImpl linkedNavigationServiceRemote;
    @Mock
    private NavigationTreeService navigationTreeService;
    @Mock
    private ParameterServiceRemote parameterServiceRemote;
    @Mock
    private NavigationTreeFinder navigationTreeFinder;

    @SuppressWarnings("checkstyle:magicNumber")
    @Test
    public void testSearchLowestLeafs() {
        //hierarchy 1 -> 2 -> 3 -> 4
        //          1 -> 5
        //          1 -> 6 -> 7
        //          1 -> 8

        TreeNode<ParamNode> four = new TreeNode<>(new ParamNode(4, 4, 3));
        TreeNode<ParamNode> three = new TreeNode<>(new ParamNode(3, 3, 2));
        three.addChild(four);
        TreeNode<ParamNode> two = new TreeNode<>(new ParamNode(2, 2, 1));
        two.addChild(three);
        TreeNode<ParamNode> seven = new TreeNode<>(new ParamNode(7, 7, 6));
        TreeNode<ParamNode> six = new TreeNode<>(new ParamNode(6, 6, 1));
        six.addChild(seven);
        TreeNode<ParamNode> five = new TreeNode<>(new ParamNode(5, 5, 1));
        TreeNode<ParamNode> eight = new TreeNode<>(new ParamNode(7, 8, 1));
        TreeNode<ParamNode> root = new TreeNode<>(new ParamNode(0, 1, -1));
        root.addChild(two);
        root.addChild(five);
        root.addChild(six);
        root.addChild(eight);
        when(parameterServiceRemote.getParametersTree(0, 1L))
            .thenReturn(root);

        List<LinkedNavigationNodeWithPath> fourNavigationNode = Arrays.asList(
            new LinkedNavigationNodeWithPath(4, 2, "Tree-4", "Node-1", Collections.emptyList()),
            new LinkedNavigationNodeWithPath(4, 3, "Tree-4", "Node-2", Collections.emptyList())
        );

        List<LinkedNavigationNodeWithPath> fiveNavigationNode = Collections.singletonList(
            new LinkedNavigationNodeWithPath(5, 4, "Tree-5", "Node-3", Collections.emptyList())
        );

        List<LinkedNavigationNodeWithPath> sevenNavigationNode = Collections.singletonList(
            new LinkedNavigationNodeWithPath(7, 5, "Tree-7", "Node-4", Collections.emptyList())
        );
        when(navigationTreeFinder.findNodesWithPathByHids(Arrays.asList(4L, 5L, 7L)))
            .thenReturn(Stream.of(fourNavigationNode,
                fiveNavigationNode,
                sevenNavigationNode)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));

        List<LinkedNavigationNodeWithPath> nodes = linkedNavigationServiceRemote.findByGlobalParameterId(1L);

        assertEquals(4, nodes.size());

        LinkedNavigationNode node = findNodeByNodeId(nodes, 2);
        assertNotNull(node);
        assertEquals(4, node.getTreeId());
        assertEquals("Tree-4", node.getTreeName());
        assertEquals("Node-1", node.getNodeName());

        node = findNodeByNodeId(nodes, 3);
        assertNotNull(node);
        assertEquals(4, node.getTreeId());
        assertEquals("Tree-4", node.getTreeName());
        assertEquals("Node-2", node.getNodeName());

        node = findNodeByNodeId(nodes, 4);
        assertNotNull(node);
        assertEquals(5, node.getTreeId());
        assertEquals("Tree-5", node.getTreeName());
        assertEquals("Node-3", node.getNodeName());

        node = findNodeByNodeId(nodes, 5);
        assertNotNull(node);
        assertEquals(7, node.getTreeId());
        assertEquals("Tree-7", node.getTreeName());
        assertEquals("Node-4", node.getNodeName());

        verify(navigationTreeFinder, times(1))
            .findNodesWithPathByHids(Arrays.asList(4L, 5L, 7L));
    }

    private LinkedNavigationNodeWithPath findNodeByNodeId(List<LinkedNavigationNodeWithPath> nodes, long nodeId) {
        return nodes.stream()
            .filter(n -> n.getNodeId() == nodeId)
            .findFirst()
            .orElse(null);
    }
}
