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
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationSuggestYtExportUtil.HID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationSuggestYtExportUtil.HIDDEN;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationSuggestYtExportUtil.IMAGE_URL;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationSuggestYtExportUtil.NID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationSuggestYtExportUtil.PARENTS;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationSuggestYtExportUtil.UNIQUE_NAME;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_GRANDPARENT_NAME;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_GRAND_PARENT_ID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_HYPER_ID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_ID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_PARENT_ID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_PARENT_NAME;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_PICTURE;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_UNIQUE_NAME;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.getNode;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.getNodeNulls;

@SuppressWarnings("checkstyle:magicnumber")
public class NavigationSuggestYtExportUtilTest {

    @Test
    public void buildNavigationSuggestYtTableAttrs() {
        MapF<String, YTreeNode> response = NavigationSuggestYtExportUtil.buildNavigationSuggestYtTableAttrs();
        assertEquals(response.getO("optimize_for").get().stringValue(), "scan");
        assertEquals(response.getO("schema").get().asList().size(), 6);
        assertEquals(response.getO("schema").get().asList().toString(), expectedSchema());
    }

    @Test
    public void mapNavigationNodeToYt() {
        SimpleNavigationNode testNode = getNode(TEST_ID, TEST_PARENT_ID);

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
        assertEquals(mappedNode.get(HID).get().longValue(), TEST_HYPER_ID);
        assertEquals(mappedNode.get(UNIQUE_NAME).get().stringValue(), TEST_UNIQUE_NAME);
        assertEquals(mappedNode.get(PARENTS).get().listNode().get(0).mapNode().get(NID).get().longValue(),
            TEST_GRAND_PARENT_ID);
        assertEquals(mappedNode.get(PARENTS).get().listNode().get(0).mapNode().get(UNIQUE_NAME).get().stringValue(),
            TEST_GRANDPARENT_NAME);
        assertFalse(mappedNode.get(HIDDEN).get().boolValue());
        assertEquals(mappedNode.get(IMAGE_URL).get().stringValue(), TEST_PICTURE);
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

    @Test
    public void mapNavigationNodeToTypeGeneric() {
        SimpleNavigationNode testNode = getNodeGeneric(TEST_ID, TEST_PARENT_ID);

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
        assertEquals(mappedNode.get(IMAGE_URL).get().stringValue(), TEST_PICTURE);
    }


    private SimpleNavigationNode getNodeGeneric(long id, long parentId) {
        SimpleNavigationNode node = getNode(id, parentId);
        node.setType(NavigationNode.Type.GENERIC);
        return node;
    }

    private String expectedSchema() {
        return "[{\"name\"=\"nid\";\"type\"=\"uint64\"}, {\"name\"=\"hid\";\"type\"=\"uint64\"}, " +
            "{\"name\"=\"unique_name\";\"type\"=\"string\"}, {\"name\"=\"parents\";\"type\"=\"any\"}, " +
            "{\"name\"=\"hidden\";\"type\"=\"boolean\"}, {\"name\"=\"image_url\";\"type\"=\"string\"}]";
    }
}
