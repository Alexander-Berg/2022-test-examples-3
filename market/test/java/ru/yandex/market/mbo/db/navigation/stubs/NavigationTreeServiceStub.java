package ru.yandex.market.mbo.db.navigation.stubs;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.navigation.NavigationTreeService;
import ru.yandex.market.mbo.gwt.models.navigation.InheritedNavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationMenu;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
/**
 * @author york
 * @since 17.08.2018
 */
public class NavigationTreeServiceStub extends NavigationTreeService {

    private Map<Long, NavigationNode> nodeMap = new HashMap<>();

    private Map<Long, NavigationTree> treeMap = new HashMap<>();

    private Map<Long, NavigationMenu> menuMap = new HashMap<>();

    @Override
    public void addNavigationTreeNode(Long uid, long parentId, TreeNode<NavigationNode> newNode) {
        if (parentId > 0) {
            nodeMap.values().stream()
                .filter(n -> n.getId() == parentId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown parent" + parentId));
        }
        nodeMap.put(newNode.getData().getId(), newNode.getData());
    }

    @Override
    public long getTreeIdByNodeId(long nodeId) {
        NavigationNode node = nodeMap.get(nodeId);
        while (node.getParentId() > 0) {
            node = nodeMap.get(node.getParentId());
        }
        long rootId = node.getId();
        return treeMap.values().stream()
            .filter(t -> t.getRootNodeId() == rootId)
            .mapToLong(t -> t.getId())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No tree found for " + nodeId));
    }

    @Override
    public void saveNavigationTree(Long uid, NavigationTree tree, boolean saveChildNodes) {
        tree.setRootNodeId(tree.getRoot().getData().getId());
        treeMap.put(tree.getId(), copy(tree));
        if (saveChildNodes) {
            tree.getRoot().findAll((a) -> true).stream().forEach(tnd -> {
                NavigationNode nodeData = tnd.getData();
                if (tnd.getParent() != null) {
                    nodeData.setParentId(tnd.getParent().getData().getId());
                }
                nodeMap.put(tnd.getData().getId(), tnd.getData());
            });
        }
    }

    public List<Long> deleteNavigationNode(long id) {
        Multimap<Long, NavigationNode> nodeByParentId = ArrayListMultimap.create();

        nodeMap.values().forEach(n -> {
            nodeByParentId.put(n.getParentId(), n);
        });

        List<Long> removed = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(id);
        while (queue.size() > 0) {
            Long currentId = queue.pop();
            NavigationNode node = nodeMap.remove(currentId);
            if (node != null) {
                removed.add(node.getId());
                for (NavigationNode child : nodeByParentId.get(currentId)) {
                    queue.push(child.getId());
                }
            }
        }
        return removed;
    }

    private NavigationTree copy(NavigationTree tree) {
        NavigationTree copy = new NavigationTree();
        copy.setId(tree.getId());
        copy.setSyncTreeId(tree.getSyncTreeId());
        copy.setRootNodeId(tree.getRootNodeId());
        copy.setSyncType(tree.getSyncType());
        copy.setName(tree.getName());
        copy.setCode(tree.getCode());
        return copy;
    }

    @Override
    public List<NavigationTree> getNavigationTrees(Collection<Long> ids) {
        return loadTreesStructureByIds(ids);
    }

    @Override
    public List<NavigationTree> getNavigationTreesLazy() {
        List<NavigationTree> result = new ArrayList<>();
        for (NavigationTree tree : treeMap.values()) {
            NavigationTree treeN = new NavigationTree();
            treeN.setId(tree.getId());
            treeN.setName(tree.getName());
            treeN.setSyncTreeId(tree.getSyncTreeId());
            treeN.setSyncType(tree.getSyncType());
            treeN.setCode(tree.getCode());

            NavigationNode root = new SimpleNavigationNode();
            root.setId(tree.getRootNodeId());
            treeN.setRootNodeId(root.getId());

            TreeNode<NavigationNode> rootTreeNode = new TreeNode<>(root);
            treeN.setRoot(rootTreeNode);
            result.add(treeN);
        }
        return result;
    }

    @Override
    public NavigationNode getNavigationNodeLazy(long nodeId) {
        NavigationNode node = nodeMap.get(nodeId);
        if (node == null) {
            return null;
        }
        NavigationNode result = new SimpleNavigationNode();
        result.setId(node.getId());
        result.setHid(node.getHid());
        result.setName(node.getName());
        result.setParentId(node.getParentId());
        result.setPublished(node.isPublished());
        result.setIsHidden(node.getIsHidden());
        result.setIsSkipped(node.getIsSkipped());
        return result;
    }

    @Override
    public NavigationNode getMainNidByHid(long hid, NavigationTree tree, boolean published) {
        Optional<NavigationNode> mainNode =
            nodeMap.values()
                .stream()
                .filter(nn ->
                    nn.getHid() != null && nn.getHid() == hid && Boolean.TRUE.equals(nn.getIsMain()))
                .findFirst();
        return mainNode.orElse(null);
    }

    @Override
    public void saveNavigationMenu(NavigationMenu menu) {
        menuMap.put(menu.getId(), menu);
    }

    @Override
    public void deleteNavigationMenu(Long menuId) {
        if (menuMap.containsKey(menuId)) {
            menuMap.remove(menuId);
        }
    }

    @Override
    public List<NavigationMenu> getNavigationMenuList() {
        return new ArrayList<>(menuMap.values());
    }

    public Map<Long, TreeNode<NavigationNode>> loadSubTreesStructure(Collection<Long> rootIds) {
        return loadNodes(rootIds, (node) -> {
            assert node != null;
            Long masterNodeId = node.getMasterNodeId();
            NavigationNode result;
            if (masterNodeId != null && masterNodeId > 0) {
                SimpleNavigationNode inh = new SimpleNavigationNode();
                inh.setId(masterNodeId);
                result = new InheritedNavigationNode(inh);
            } else {
                result = new SimpleNavigationNode();
            }
            result.setId(node.getId());
            result.setParentId(node.getParentId());
            result.setPublished(node.isPublished());
            result.setIsSkipped(node.getIsSkipped());
            result.setIsHidden(node.getIsHidden());
            result.setHid(node.getHid());
            return result;
        });
    }

    private Map<Long, TreeNode<NavigationNode>> loadNodes(Collection<Long> rootIds,
                                                          Function<NavigationNode, NavigationNode> converter) {

        Map<Long, TreeNode<NavigationNode>> map = new HashMap<>();

        Multimap<Long, NavigationNode> nodeByParentId = ArrayListMultimap.create();

        nodeMap.values().forEach(n -> {
            nodeByParentId.put(n.getParentId(), n);
        });

        Deque<TreeNode<NavigationNode>> queue = new ArrayDeque<>();
        for (Long rootId : rootIds) {
            NavigationNode rootNode = nodeMap.get(rootId);
            if (rootNode == null) {
                continue;
            }
            TreeNode<NavigationNode> node = new TreeNode<>(converter.apply(rootNode));
            map.put(node.getData().getId(), node);
            queue.push(node);
        }
        while (queue.size() > 0) {
            TreeNode<NavigationNode> current = queue.pop();

            for (NavigationNode child : nodeByParentId.get(current.getData().getId())) {
                NavigationNode converted = converter.apply(child);
                TreeNode<NavigationNode> childTreeNode = new TreeNode<>(converted);
                map.put(converted.getId(), childTreeNode);
                current.addChild(childTreeNode);
                queue.add(childTreeNode);
            }
        }
        return map;
    }

    @Override
    public void validateNavigationMenu(long menuId) throws OperationException { }

    public Map<Long, NavigationNode> getNodeMap() {
        return nodeMap;
    }

    public Map<Long, NavigationTree> getTreeMap() {
        return treeMap;
    }
}
