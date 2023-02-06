package ru.yandex.market.mbo.db.navigation;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbo.gwt.models.navigation.CheckMainNidConstrainResponse;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.mbo.gwt.models.navigation.CheckMainNidConstrainResponse.Action.OVERRIDE;
import static ru.yandex.market.mbo.gwt.models.navigation.CheckMainNidConstrainResponse.Action.PICK;
import static ru.yandex.market.mbo.gwt.models.navigation.CheckMainNidConstrainResponse.Action.SET_THIS;
import static ru.yandex.market.mbo.gwt.models.navigation.CheckMainNidConstrainResponse.Action.WARNING;
import static ru.yandex.market.mbo.gwt.models.navigation.NavigationNode.Type.CATEGORY;
import static ru.yandex.market.mbo.gwt.models.navigation.NavigationNode.Type.GENERIC;

/**
 * @author kravchenko-aa
 * @date 17.10.18
 */
@RunWith(MockitoJUnitRunner.class)
public class MainNidConstraintsValidatorTest {
    private static final long TREE_ID = 42L;
    private static final long ROOT_ID = 0L;

    private static final long CHILD1_ID = 1L;
    private static final long CHILD2_ID = 2L;
    private static final long CHILD3_ID = 3L;

    private static final long CHILD11_ID = 11L;
    private static final long CHILD12_ID = 12L;
    private static final long CHILD13_ID = 13L;

    private static final long CHILD21_ID = 21L;
    private static final long CHILD22_ID = 22L;
    private static final long CHILD23_ID = 23L;

    private static final long HID1 = 43L;
    private static final long HID2 = 44L;
    private static final long HID3 = 45L;
    private static final long HID4 = 46L;
    private static final long HID5 = 47L;

    private static final long NEW_NODE_ID = 2000L;
    private static final long NEW_HID = 4000L;

    @Spy
    private NavigationTreeService navigationTreeServiceDraft;
    private JdbcTemplate contentDraftPgJdbcTemplate;
    private NavigationTree tree;
    private MainNidConstraintsValidator mainNidConstraintsValidator;

    @Before
    public void setUp() {
        initTables();
        NavigationTreeService navigationTreeService = new NavigationTreeService();
        navigationTreeService.setNavigationJdbcTemplate(contentDraftPgJdbcTemplate);
        navigationTreeServiceDraft = Mockito.spy(navigationTreeService);
        doReturn(tree).when(navigationTreeServiceDraft).getNavigationTree(anyLong());
        doAnswer(invocation -> {
            TreeNode<NavigationNode> nodeById = tree.findNodeById(invocation.getArgument(0));
            if (nodeById == null) {
                return null;
            }
            return nodeById.getData();
        }).when(navigationTreeServiceDraft).getNavigationNode(anyLong());

        mainNidConstraintsValidator = new MainNidConstraintsValidator();
        mainNidConstraintsValidator.setNavigationTreeServiceDraft(navigationTreeServiceDraft);
    }

    @Test
    public void testNewHidCreation() {
        NavigationNode node = createNode(NEW_NODE_ID, CHILD1_ID, NEW_HID, CATEGORY, true, false, false);
        CheckMainNidConstrainResponse checkMainNidConstrainResponse
            = mainNidConstraintsValidator.checkMainNidConstrainViolation(node, TREE_ID);
        assertEquals(checkMainNidConstrainResponse.getAction(), SET_THIS);
    }

    @Test
    public void testNewHidUnpublishedCreation() {
        NavigationNode node = createNode(NEW_NODE_ID, CHILD3_ID, NEW_HID, CATEGORY, true, false, false);
        CheckMainNidConstrainResponse checkMainNidConstrainResponse
            = mainNidConstraintsValidator.checkMainNidConstrainViolation(node, TREE_ID);
        assertEquals(checkMainNidConstrainResponse.getAction(), SET_THIS);
    }

    @Test
    public void testOverrideMainNid() {
        NavigationNode node = tree.findNodeById(CHILD21_ID).getData().copy();
        node.setIsMain(true);
        CheckMainNidConstrainResponse checkMainNidConstrainResponse
            = mainNidConstraintsValidator.checkMainNidConstrainViolation(node, TREE_ID);
        assertEquals(checkMainNidConstrainResponse.getAction(), OVERRIDE);
        assertTrue(checkMainNidConstrainResponse.getMainNid().getId() == CHILD11_ID);
    }

    @Test
    public void testPickNewMain() {
        NavigationNode node = tree.findNodeById(CHILD11_ID).getData().copy();
        node.setIsMain(false);
        CheckMainNidConstrainResponse checkMainNidConstrainResponse
            = mainNidConstraintsValidator.checkMainNidConstrainViolation(node, TREE_ID);
        assertEquals(checkMainNidConstrainResponse.getAction(), PICK);
        assertTrue(
            checkMainNidConstrainResponse.getMainNidsOptions()
                .stream()
                .map(NavigationNode::getId)
                .collect(Collectors.toList())
                .containsAll(Arrays.asList(CHILD23_ID, CHILD21_ID)));
        assertEquals(checkMainNidConstrainResponse.getMainNidsOptions().size(), 2);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testBatchOverriding() {
        NavigationNode node = tree.findNodeById(CHILD1_ID).getData().copy();
        node.setPublished(false);
        CheckMainNidConstrainResponse checkMainNidConstrainResponse
            = mainNidConstraintsValidator.checkMainNidConstrainViolation(node, TREE_ID);
        assertEquals(checkMainNidConstrainResponse.getAction(), WARNING);
        Map<NavigationNode, NavigationNode> batchNidsUpdate = checkMainNidConstrainResponse.getBatchNidsUpdate();
        assertEquals(batchNidsUpdate.entrySet().size(), 4);
        assertTrue(
            batchNidsUpdate.keySet().stream()
                .map(NavigationNode::getHid)
                .collect(Collectors.toList())
                .containsAll(Arrays.asList(HID1, HID2, HID3, HID4)));
        assertNull(batchNidsUpdate.get(tree.findNodeById(CHILD13_ID).getData()));
        assertEquals(batchNidsUpdate.get(tree.findNodeById(CHILD11_ID).getData()).getId(), CHILD21_ID);
    }

    private void initTables() {
        String dbName = UUID.randomUUID().toString();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(
            "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL"
        );
        contentDraftPgJdbcTemplate = new JdbcTemplate(dataSource);
        contentDraftPgJdbcTemplate.update("create table navigation_tree_node (" +
            "  id int, " +
            "  parent_node_id  int, " +
            "  hid  int, " +
            "  type text, " +
            "  published boolean default true," +
            "  is_skipped  boolean default false, " +
            "  is_main  boolean )");
        contentDraftPgJdbcTemplate.update("create table navigation_tree (" +
            " id int, " +
            " root_node_id int)");

        tree = createTree();
    }

    private NavigationTree createTree() {
        NavigationTree treeResult = new NavigationTree();
        treeResult.setRootNodeId(ROOT_ID);
        treeResult.setId(TREE_ID);
        contentDraftPgJdbcTemplate.update(
            "insert into NAVIGATION_TREE(ID, ROOT_NODE_ID) values (" + TREE_ID + "," + ROOT_ID + ")");

        TreeNode<NavigationNode> root = createTreeNode(ROOT_ID, 0, 0, CATEGORY, true, false, true);
        treeResult.setRoot(root);

        TreeNode<NavigationNode> child1 = createTreeNode(CHILD1_ID, ROOT_ID, HID1, CATEGORY, true, false, true);
        TreeNode<NavigationNode> child2 = createTreeNode(CHILD2_ID, ROOT_ID, HID1, CATEGORY, true, false, false);
        TreeNode<NavigationNode> child3 = createTreeNode(CHILD3_ID, ROOT_ID, HID5, CATEGORY, false, false, false);
        TreeNode<NavigationNode> child11 = createTreeNode(CHILD11_ID, CHILD1_ID, HID2, CATEGORY, true, false, true);
        TreeNode<NavigationNode> child12 = createTreeNode(CHILD12_ID, CHILD1_ID, HID3, CATEGORY, true, false, true);
        TreeNode<NavigationNode> child13 = createTreeNode(CHILD13_ID, CHILD1_ID, HID4, CATEGORY, true, false, true);
        TreeNode<NavigationNode> child21 = createTreeNode(CHILD21_ID, CHILD2_ID, HID2, CATEGORY, true, false, false);
        TreeNode<NavigationNode> child22 = createTreeNode(CHILD22_ID, CHILD2_ID, HID3, CATEGORY, true, false, false);
        TreeNode<NavigationNode> child23 = createTreeNode(CHILD23_ID, CHILD2_ID, HID2, GENERIC, true, false, false);

        child1.setParent(root);
        child2.setParent(root);
        child3.setParent(root);
        root.addChildren(Arrays.asList(child1, child2, child3));

        child11.setParent(child1);
        child12.setParent(child1);
        child13.setParent(child1);
        child1.addChildren(Arrays.asList(child11, child12, child13));

        child21.setParent(child2);
        child22.setParent(child2);
        child23.setParent(child2);
        child1.addChildren(Arrays.asList(child21, child22, child23));

        return treeResult;
    }

    private TreeNode<NavigationNode> createTreeNode(long id, long parentNodeId, long hid, NavigationNode.Type type,
                                                    boolean published, boolean isSkipped, boolean isMain) {
        NavigationNode node = new SimpleNavigationNode();
        node.setId(id);
        node.setParentId(parentNodeId);
        node.setHid(hid);
        node.setType(type);
        node.setPublished(published);
        node.setIsSkipped(isSkipped);
        node.setIsMain(isMain);

        contentDraftPgJdbcTemplate.update(
            "insert into NAVIGATION_TREE_NODE (ID, PARENT_NODE_ID, HID, TYPE, PUBLISHED, IS_SKIPPED, IS_MAIN) " +
                "values (" +
                id + ", " +
                parentNodeId + ", " +
                hid + ", '" +
                type + "', " +
                (published ? 1 : 0) + ", " +
                (isSkipped ? 1 : 0) + ", " +
                (isMain ? 1 : 0) + ")");

        return new TreeNode<>(node);
    }

    private NavigationNode createNode(long id, long parentNodeId, long hid, NavigationNode.Type type,
                                          boolean published, boolean isSkipped, boolean isMain) {
        NavigationNode node = new SimpleNavigationNode();
        node.setId(id);
        node.setParentId(parentNodeId);
        node.setHid(hid);
        node.setType(type);
        node.setPublished(published);
        node.setIsSkipped(isSkipped);
        node.setIsMain(isMain);

        return node;
    }
}
