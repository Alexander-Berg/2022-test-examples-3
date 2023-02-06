package ru.yandex.market.mbo.db;

import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author anmalysh
 */
public class TovarTreeDaoMock extends TovarTreeDao {

    private final Collection<TovarCategory> tovarCategories;
    public TovarTreeDaoMock() {
        super(null, null, null, null, null, null);
        tovarCategories = new ArrayList<>();
    }

    public TovarTreeDaoMock(TovarCategory... tovarCategories) {
        this(Arrays.asList(tovarCategories));
    }

    public TovarTreeDaoMock(Collection<TovarCategory> tovarCategories) {
        this();
        this.tovarCategories.addAll(tovarCategories);
    }

    public TovarTreeDaoMock addCategory(long hid) {
        return addCategory(hid, 0);
    }

    public TovarTreeDaoMock addCategory(long hid, long parentHid) {
        return addCategory(new TovarCategory("Category: " + hid, hid, parentHid));
    }

    public TovarTreeDaoMock addCategory(TovarCategory category) {
        tovarCategories.add(category);
        return this;
    }

    @Override
    public Map<Long, String> getCategoryNames(Collection<Long> categoryIds) {
        return tovarCategories.stream()
            .filter(tc -> categoryIds.contains(tc.getHid()))
            .collect(Collectors.toMap(TovarCategory::getHid, TovarCategory::getName));
    }

    @Override
    public TovarCategory loadCategoryByHid(long hid) {
        return tovarCategories.stream()
                .filter(tc -> tc.getHid() == hid)
                .findFirst()
                .orElse(null);
    }

    @Override
    public TovarTree loadTovarTree() {
        Map<Long, TovarCategoryNode> nodes = tovarCategories.stream()
            .map(TovarCategoryNode::new)
            .collect(Collectors.toMap(TovarCategoryNode::getHid, Function.identity()));

        for (TovarCategoryNode node : nodes.values()) {
            if (nodes.containsKey(node.getParentHid())) {
                TovarCategoryNode parentNode = nodes.get(node.getParentHid());
                if (parentNode != node) {
                    parentNode.addChild(node);
                }
            }
        }

        TovarCategoryNode rootNode = nodes.values().stream().filter(node -> node.getParent() == null).findFirst().get();
        TovarTree tree = new TovarTree(rootNode);
        tree.levelTree();
        return tree;
    }

    @Override
    public TovarTree loadTreeScheme() {
        return loadTovarTree();
    }
}
