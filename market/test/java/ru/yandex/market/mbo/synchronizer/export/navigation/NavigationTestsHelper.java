package ru.yandex.market.mbo.synchronizer.export.navigation;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.mbo.gwt.models.navigation.MergeStatus;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;

@ParametersAreNonnullByDefault
public class NavigationTestsHelper {

    public static final long TEST_HYPER_ID = 777;
    public static final long TEST_ID = 123;
    public static final long TEST_PARENT_ID = 666;
    public static final long TEST_UNCLE_ID = 555;
    public static final long TEST_GRAND_PARENT_ID = 333;
    public static final String TEST_NAME = "test_name";
    public static final String TEST_PARENT_NAME = "test_parent_name";
    public static final String TEST_GRANDPARENT_NAME = "test_grandparent_name";
    public static final String TEST_UNIQUE_NAME = "test_uniq_name";
    public static final boolean TEST_HIDDEN = false;
    public static final String TEST_PICTURE = "test_picture";

    private NavigationTestsHelper() {
    }

    public static SimpleNavigationNode getNode(long id, long parentId) {
        SimpleNavigationNode node = new SimpleNavigationNode();
        node.setIsHidden(TEST_HIDDEN);
        node.setHid(TEST_HYPER_ID);
        node.setId(id);
        node.setName(TEST_NAME);
        node.setUniqueName(TEST_UNIQUE_NAME);
        node.setParentId(parentId);
        node.setPicture(TEST_PICTURE);
        node.setType(NavigationNode.Type.MODEL_LIST);
        node.setPublished(true);
        node.setTovarCategory(new TovarCategory() {
            public boolean isPublished() {
                return true;
            }
        });
        return node;
    }

    public static SimpleNavigationNode getFullNode(long id, long parentId) {
        SimpleNavigationNode node = new SimpleNavigationNode();
        node.setIsHidden(TEST_HIDDEN);
        node.setHid(TEST_HYPER_ID);
        node.setId(id);
        node.setName(TEST_NAME);
        node.setUniqueName(TEST_UNIQUE_NAME);
        node.setParentId(parentId);
        node.setPicture(TEST_PICTURE);
        node.setPublished(true);
        node.setType(NavigationNode.Type.CATEGORY);
        node.setDisplayStyle(NavigationNode.DisplayStyle.DEFAULT);
        node.setMergeStatus(MergeStatus.UNCHANGED);
        return node;
    }


    public static SimpleNavigationNode getNodeNotPublished(long id, long parentId) {
        SimpleNavigationNode node = getNode(id, parentId);
        node.setPublished(false);
        return node;
    }

    public static SimpleNavigationNode getNodeNulls(long id, long parentId) {
        SimpleNavigationNode node = new SimpleNavigationNode();
        node.setIsHidden(null);
        node.setHid(null);
        node.setId(id);
        node.setUniqueName(TEST_UNIQUE_NAME);
        node.setParentId(parentId);
        node.setType(null);
        return node;
    }
}
