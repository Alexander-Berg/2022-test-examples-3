package ru.yandex.market.mbo.synchronizer.export.navigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.db.CategoryRestrictionService;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.navigation.NavigationTreeService;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;
import ru.yandex.market.mbo.synchronizer.export.ExportRegistry;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportMapReduceService;
import ru.yandex.utils.Pair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_GRAND_PARENT_ID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_ID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_PARENT_ID;
import static ru.yandex.market.mbo.synchronizer.export.navigation.NavigationTestsHelper.TEST_UNCLE_ID;

@SuppressWarnings("checkstyle:magicnumber")
public class NavigationSuggestExtractorTest {

    private final YtExportMapReduceService ytExportMapReduceService = mock(YtExportMapReduceService.class);
    private final NavigationTreeService navigationTreeService = mock(NavigationTreeService.class);
    private final TovarTreeService tovarTreeService = mock(TovarTreeService.class);
    private final ExportRegistry registry = mock(ExportRegistry.class);
    private final CategoryRestrictionService categoryRestrictionService = mock(CategoryRestrictionService.class);
    private NavigationSuggestExtractor navigationSuggestExtractor;

    @Before
    public void init() {
        navigationSuggestExtractor = new NavigationSuggestExtractor(ytExportMapReduceService, navigationTreeService,
                tovarTreeService, categoryRestrictionService);
        navigationSuggestExtractor.setRegistry(registry);
        navigationSuggestExtractor.setYtExtractorPath("string");
    }

    /**
     * Should return null because path will be null.
     */
    @Test
    public void writeToYtNullPath() {
        assertNull(navigationSuggestExtractor.writeToYt(new ArrayList<>(), new HashMap<>()));
    }

    @Test
    public void writeToYtNormalPath() {
        YPath yPath = mock(YPath.class);
        doReturn(yPath).when(yPath).child(any());
        doReturn(yPath).when(registry).getYtFolderPath();
        assertNotNull(navigationSuggestExtractor.writeToYt(new ArrayList<>(), new HashMap<>()));
    }

    @Test
    public void fillNodesInfoFromTreeNormalFlow() {
        TreeNode<NavigationNode> grandParent = new TreeNode<>();
        grandParent.setData(NavigationTestsHelper.getNode(TEST_GRAND_PARENT_ID, 0));
        TreeNode<NavigationNode> parent = new TreeNode<>();
        parent.setData(NavigationTestsHelper.getNode(TEST_PARENT_ID, TEST_GRAND_PARENT_ID));
        TreeNode<NavigationNode> uncle = new TreeNode<>();
        uncle.setData(NavigationTestsHelper.getNode(TEST_UNCLE_ID, TEST_GRAND_PARENT_ID));
        TreeNode<NavigationNode> node = new TreeNode<>();
        node.setData(NavigationTestsHelper.getNode(TEST_ID, TEST_PARENT_ID));
        parent.addChild(node);
        grandParent.addChild(parent);
        grandParent.addChild(uncle);
        NavigationTree tree = new NavigationTree();
        tree.setRoot(grandParent);
        doReturn(tree).when(navigationTreeService).getNavigationTree(anyLong());

        Map<Long, NavigationNode> nodes = new HashMap<>();
        Map<Long, List<Pair<Long, String>>> parentsIdsUniqueNames = new HashMap<>();
        navigationSuggestExtractor.fillNodesInfoFromTree(nodes, 1, parentsIdsUniqueNames);
        assertEquals(3, nodes.size());
    }

    @Test
    public void fillNodesInfoFromTreeNotPublishedNode() {
        TreeNode<NavigationNode> grandParent = new TreeNode<>();
        grandParent.setData(NavigationTestsHelper.getNode(TEST_GRAND_PARENT_ID, 0));
        TreeNode<NavigationNode> parent = new TreeNode<>();
        parent.setData(NavigationTestsHelper.getNode(TEST_PARENT_ID, TEST_GRAND_PARENT_ID));
        TreeNode<NavigationNode> uncle = new TreeNode<>();
        uncle.setData(NavigationTestsHelper.getNodeNotPublished(TEST_UNCLE_ID, TEST_GRAND_PARENT_ID));
        TreeNode<NavigationNode> node = new TreeNode<>();
        node.setData(NavigationTestsHelper.getNode(TEST_UNCLE_ID, TEST_PARENT_ID));
        parent.addChild(node);
        grandParent.addChild(parent);
        grandParent.addChild(uncle);
        NavigationTree tree = new NavigationTree();
        tree.setRoot(grandParent);
        doReturn(tree).when(navigationTreeService).getNavigationTree(anyLong());

        Map<Long, NavigationNode> nodes = new HashMap<>();
        Map<Long, List<Pair<Long, String>>> parentsIdsUniqueNames = new HashMap<>();
        navigationSuggestExtractor.fillNodesInfoFromTree(nodes, 1, parentsIdsUniqueNames);
        assertEquals(2, nodes.size());
    }


}
