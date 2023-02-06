package ru.yandex.market.mbo.synchronizer.export.navigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.utils.Pair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static ru.yandex.market.mbo.synchronizer.export.navigation.AllNavigationNodesYtExportUtil.HID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.AllNavigationNodesYtExportUtil.HIDDEN;
import static ru.yandex.market.mbo.synchronizer.export.navigation.AllNavigationNodesYtExportUtil.NID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.AllNavigationNodesYtExportUtil.PARENTS;
import static ru.yandex.market.mbo.synchronizer.export.navigation.AllNavigationNodesYtExportUtil.PICTURE_URL;
import static ru.yandex.market.mbo.synchronizer.export.navigation.AllNavigationNodesYtExportUtil.UNIQUE_NAME;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_GRANDPARENT_NAME;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_GRAND_PARENT_ID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_HYPER_ID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_ID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_PARENT_ID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_PARENT_NAME;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_PICTURE;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_UNIQUE_NAME;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.getFullNode;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.getNodeNulls;

@SuppressWarnings("checkstyle:magicnumber")
public class AllNavigationNodesYtExportUtilTest {

    @Test
    public void buildAllNavigationNodesYtTableAttrs() {
        MapF<String, YTreeNode> response = AllNavigationNodesYtExportUtil.buildAllNavigationNodesYtTableAttrs();
        assertEquals(response.getO("optimize_for").get().stringValue(), "scan");
        assertEquals(response.getO("schema").get().asList().size(), 44);
        assertEquals(response.getO("schema").get().asList().toString(), expectedSchema());
    }

    // fixme: add other fields
    @Test
    public void mapNavigationNodeToYt() {
        SimpleNavigationNode testNode = getFullNode(TEST_ID, TEST_PARENT_ID);

        Map<Long, List<Pair<Long, String>>> map = new HashMap<>();
        List<Pair<Long, String>> grandParentList = new ArrayList<>();
        List<Pair<Long, String>> parentList = new ArrayList<>();
        parentList.add(new Pair<>(TEST_GRAND_PARENT_ID, TEST_GRANDPARENT_NAME));
        List<Pair<Long, String>> nodeList = new ArrayList<>(parentList);
        nodeList.add(new Pair<>(TEST_PARENT_ID, TEST_PARENT_NAME));
        map.put(TEST_GRAND_PARENT_ID, grandParentList);
        map.put(TEST_PARENT_ID, parentList);
        map.put(TEST_ID, nodeList);
        YTreeMapNode mappedNode = AllNavigationNodesYtExportUtil.mapNavigationNodeToYt(testNode, map);
        assertEquals(mappedNode.get(NID).get().longValue(), TEST_ID);
        assertEquals(mappedNode.get(HID).get().longValue(), TEST_HYPER_ID);
        assertEquals(mappedNode.get(UNIQUE_NAME).get().stringValue(), TEST_UNIQUE_NAME);
        assertEquals(mappedNode.get(PARENTS).get().listNode().get(0).mapNode().get(NID).get().longValue(),
            TEST_GRAND_PARENT_ID);
        assertEquals(mappedNode.get(PARENTS).get().listNode().get(0).mapNode().get(UNIQUE_NAME).get().stringValue(),
            TEST_GRANDPARENT_NAME);
        assertFalse(mappedNode.get(HIDDEN).get().boolValue());
        assertEquals(mappedNode.get(PICTURE_URL).get().stringValue(), TEST_PICTURE);
    }

    @Test
    public void mapNavigationNodeToYtNotNPE() {
        SimpleNavigationNode testNode = getNodeNulls(TEST_ID, TEST_PARENT_ID);
        Map<Long, List<Pair<Long, String>>> map = new HashMap<>();
        List<Pair<Long, String>> grandParentList = new ArrayList<>();
        List<Pair<Long, String>> parentList = new ArrayList<>();
        parentList.add(new Pair<>(TEST_GRAND_PARENT_ID, TEST_GRANDPARENT_NAME));
        List<Pair<Long, String>> nodeList = new ArrayList<>(parentList);
        nodeList.add(new Pair<>(TEST_PARENT_ID, TEST_PARENT_NAME));
        map.put(TEST_GRAND_PARENT_ID, grandParentList);
        map.put(TEST_PARENT_ID, parentList);
        map.put(TEST_ID, nodeList);
        YTreeMapNode mappedNode = NavigationSuggestYtExportUtil.mapNavigationNodeToYt(testNode, map);
        assertEquals(mappedNode.get(NID).get().longValue(), TEST_ID);
        assertEquals(mappedNode.get(HID).get().longValue(), 0);
        assertEquals(mappedNode.get(UNIQUE_NAME).get().stringValue(), TEST_UNIQUE_NAME);
        assertEquals(mappedNode.get(PARENTS).get().listNode().get(0).mapNode().get(NID).get().longValue(),
            TEST_GRAND_PARENT_ID);
        assertEquals(mappedNode.get(PARENTS).get().listNode().get(0).mapNode().get(UNIQUE_NAME).get().stringValue(),
            TEST_GRANDPARENT_NAME);
        assertFalse(mappedNode.get(HIDDEN).get().boolValue());
    }

    private SimpleNavigationNode getNodeGeneric(long id, long parentId) {
        SimpleNavigationNode node = getFullNode(id, parentId);
        node.setType(NavigationNode.Type.GENERIC);
        return node;
    }

    private String expectedSchema() {
        return "[{\"name\"=\"nid\";\"type\"=\"uint64\"}, {\"name\"=\"hid\";\"type\"=\"uint64\"}, " +
            "{\"name\"=\"position\";\"type\"=\"uint16\"}, {\"name\"=\"name\";\"type\"=\"string\"}, " +
            "{\"name\"=\"unique_name\";\"type\"=\"string\"}, {\"name\"=\"parent_id\";\"type\"=\"uint64\"}, " +
            "{\"name\"=\"parents\";\"type\"=\"any\"}, {\"name\"=\"primary\";\"type\"=\"boolean\"}, " +
            "{\"name\"=\"main\";\"type\"=\"boolean\"}, {\"name\"=\"hidden\";\"type\"=\"boolean\"}, " +
            "{\"name\"=\"skipped\";\"type\"=\"boolean\"}, {\"name\"=\"type\";\"type\"=\"string\"}, " +
            "{\"name\"=\"display_style\";\"type\"=\"string\"}, {\"name\"=\"recipe_id\";\"type\"=\"uint64\"}, " +
            "{\"name\"=\"modified\";\"type\"=\"boolean\"}, {\"name\"=\"to_delete\";\"type\"=\"boolean\"}, " +
            "{\"name\"=\"tovar_category_id\";\"type\"=\"uint64\"}, {\"name\"=\"linked_tovar_published\";" +
            "\"type\"=\"boolean\"}, {\"name\"=\"tovar_id\";\"type\"=\"int64\"}, {\"name\"=\"merge_status\";" +
            "\"type\"=\"string\"}, {\"name\"=\"green\";\"type\"=\"boolean\"}, {\"name\"=\"blue\";" +
            "\"type\"=\"boolean\"}, {\"name\"=\"promo\";\"type\"=\"boolean\"}, {\"name\"=\"promo_children\";" +
            "\"type\"=\"boolean\"}, {\"name\"=\"blue_children\";\"type\"=\"boolean\"}, {\"name\"=\"link_id\";" +
            "\"type\"=\"uint64\"}, {\"name\"=\"published\";\"type\"=\"boolean\"}, {\"name\"=\"application_hide\";" +
            "\"type\"=\"boolean\"}, {\"name\"=\"application_name\";\"type\"=\"string\"}, {\"name\"=\"icon\";" +
            "\"type\"=\"string\"}, {\"name\"=\"icon_overriden\";\"type\"=\"boolean\"}, " +
            "{\"name\"=\"touch_general_name\";\"type\"=\"string\"}, {\"name\"=\"touch_hide\";\"type\"=\"boolean\"}, " +
            "{\"name\"=\"touch_name\";\"type\"=\"string\"}, {\"name\"=\"picture_url\";\"type\"=\"string\"}, " +
            "{\"name\"=\"picture_overriden\";\"type\"=\"boolean\"}, {\"name\"=\"picture_width\";\"type\"=\"int16\"}, " +
            "{\"name\"=\"picture_height\";\"type\"=\"int16\"}, {\"name\"=\"master_node_id\";\"type\"=\"uint64\"}, " +
            "{\"name\"=\"model_list_id\";\"type\"=\"uint64\"}, {\"name\"=\"show_models_in_parent\";" +
            "\"type\"=\"boolean\"}, {\"name\"=\"show_suggest\";\"type\"=\"boolean\"}, {\"name\"=\"filter_config_id\";" +
            "\"type\"=\"uint64\"}, {\"name\"=\"should_use_tovar_tag\";\"type\"=\"boolean\"}]";
    }
}
