package ru.yandex.market.mbo.db.navigation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.recipes.RecipeService;
import ru.yandex.market.mbo.gwt.models.navigation.InheritedNavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.Link;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.SyncType;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilter;
import ru.yandex.market.mbo.gwt.models.tovartree.OutputType;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.utils.test.IntegrationTestInitializer;

/**
 * @author york
 * @since 13.08.2018
 */
@ActiveProfiles("navigation-test")
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
@ContextConfiguration(classes = NavigationServiceConfiguration.class,
    initializers = IntegrationTestInitializer.class)
public class NavigationServiceTest {
    private static final Random RANDOM = new Random(1);

    private static final Logger log = Logger.getLogger(NavigationTreeService.class);

    private static final String TREE_NAME = "Integration test tree name";
    private static final String INHERITED_TREE_NAME = NavigationTreeUtils.getCopyName(TREE_NAME);
    private static final Long INCORRECT_HID = 3_000_000L;

    @Resource(name = "navigationTreeServiceDraft")
    private NavigationTreeService navigationTreeServiceDraft;

    @Resource(name = "navigationTreeService")
    private NavigationTreeService navigationTreeService;

    @Resource
    private NavigationTreeCopyService navigationTreeCopyService;

    @Resource
    private NavigationTreePublishService navigationTreePublishService;

    @Resource
    private AutoUser autoUser;

    @Resource
    private RecipeService recipeService;

    @After
    @Before
    public void removeOldTrees() {
        List<NavigationTree> treeListDraft = navigationTreeServiceDraft.getNavigationTreesLazy();
        List<NavigationTree> treeList = navigationTreeService.getNavigationTreesLazy();
        //first deleting inherited
        deleteTreesByName(INHERITED_TREE_NAME, treeListDraft, treeList);
        //and now normal
        deleteTreesByName(TREE_NAME, treeListDraft, treeList);
    }

    private void deleteTreesByName(String inheritedTreeName, List<NavigationTree> treeListDraft,
                                   List<NavigationTree> treeList) {
        treeListDraft.stream().filter(t -> t.getName().equalsIgnoreCase(inheritedTreeName)).forEach(t -> {
            navigationTreeServiceDraft.deleteNavigationTree(autoUser.getId(), t.getId(), false);
        });
        treeList.stream().filter(t -> t.getName().equalsIgnoreCase(inheritedTreeName)).forEach(t -> {
            navigationTreeService.deleteNavigationTree(autoUser.getId(), t.getId(), false);
        });
    }

    @Test
    public void testTreeCodeSave() {
        NavigationTree tree = createBaseTree();
        navigationTreeServiceDraft.saveNavigationTree(autoUser.getId(), tree, true);
        tree = navigationTreeServiceDraft.getNavigationTree(tree.getId());
        Assert.assertNull(tree.getCode());
        tree.setCode("blabla");
        navigationTreeServiceDraft.saveNavigationTree(autoUser.getId(), tree, false);
        tree = navigationTreeServiceDraft.getNavigationTree(tree.getId());
        Assert.assertEquals("blabla", tree.getCode());
    }

    @Test
    public void testTreeSimpleCopy() {
        NavigationTree tree = createComplicatedTree();
        NavigationTree simpleCopy = navigationTreeServiceDraft.copyNavigationTree(autoUser.getId(), 0,
            tree.getRootNodeId(), null);

        simpleCopy = navigationTreeServiceDraft.getNavigationTree(simpleCopy.getId());
        Assert.assertNull(simpleCopy.getSyncTreeId());
        Assert.assertNull(simpleCopy.getSyncType());
        Assert.assertEquals(tree.getRoot().findAll((n) -> true).size(),
            simpleCopy.getRoot().findAll((n) -> true).size());
    }

    @Test
    public void testTreeCreationAndInheritance() {
        NavigationTree tree = createComplicatedTree();
        Map<Long, NavigationNode> nodeMap = tree.getRoot().findAll(a -> true)
            .stream()
            .collect(Collectors.toMap(nd -> nd.getData().getId(), nd -> nd.getData()));

        log.debug("copying canonical");
        NavigationTree canonicalCopy = navigationTreeServiceDraft.copyNavigationTree(autoUser.getId(), 0,
            tree.getRootNodeId(), SyncType.CANONICAL);
        canonicalCopy = navigationTreeServiceDraft.getNavigationTree(canonicalCopy.getId());
        Assert.assertNotNull(canonicalCopy);
        int canonicalNodes = canonicalCopy.getRoot().findAll(nd -> {
            Assert.assertTrue(nd instanceof InheritedNavigationNode);
            Assert.assertTrue(compareNodeValues(nd, nodeMap.get(nd.getMasterNodeId())));
            return true;
        }).size();
        Assert.assertEquals(getTovarCategoriesSize(), canonicalNodes);

        log.debug("copying full");
        NavigationTree fullCopy = navigationTreeServiceDraft.copyNavigationTree(autoUser.getId(), 0,
            tree.getRootNodeId(), SyncType.FULL);
        fullCopy = navigationTreeServiceDraft.getNavigationTree(fullCopy.getId());
        Assert.assertNotNull(fullCopy);
        Assert.assertEquals(SyncType.FULL, fullCopy.getSyncType());
        int fullnodes = fullCopy.getRoot().findAll(nd -> {
            Assert.assertTrue(nd instanceof InheritedNavigationNode);
            Assert.assertTrue(compareNodeValues(nd, nodeMap.get(nd.getMasterNodeId())));
            return true;
        }).size();
        Assert.assertEquals(getTovarCategoriesSize() + 3, fullnodes);

        NavigationNode newNode = virtualNode("virtual2");

        //testing inherited adding
        navigationTreeServiceDraft.addNavigationTreeNode(
            autoUser.getId(), tree.getRootNodeId(), new TreeNode<>(newNode));

        fullCopy = navigationTreeServiceDraft.getNavigationTree(fullCopy.getId());
        canonicalCopy = navigationTreeServiceDraft.getNavigationTree(canonicalCopy.getId());

        TreeNode<NavigationNode> infull = findInherited(fullCopy, newNode.getId());

        TreeNode<NavigationNode> incanonical = findInherited(canonicalCopy, newNode.getId());

        Assert.assertNotNull(infull);
        Assert.assertNull(incanonical);

        NavigationNode leaf = getLeafs(tree).get(0).getData();
        NavigationNode inheritedLeaf = findInherited(fullCopy, leaf.getId()).getData();

        //testing inherited moving
        navigationTreeServiceDraft.moveNode(autoUser.getId(), newNode, tree.getRootNodeId(), leaf.getId());

        NavigationNode newNodeAfterSave = navigationTreeServiceDraft.getNavigationNode(newNode.getId());
        Assert.assertEquals(leaf.getId(), newNodeAfterSave.getParentId());

        NavigationNode inheritedNewNode = navigationTreeServiceDraft.getNavigationNode(infull.getData().getId());
        Assert.assertEquals(inheritedLeaf.getId(), inheritedNewNode.getParentId());

        //testing inherited deleting
        NavigationNode firstDep = tree.getRoot().getChildren().get(0).getData();
        NavigationNode secondDep = tree.getRoot().getChildren().get(1).getData();

        TreeNode<NavigationNode> inheritedFirstDep = navigationTreeServiceDraft.loadSubTree(
            findInherited(fullCopy, firstDep.getId()).getData().getId());
        TreeNode<NavigationNode> inheritedSecondDep = navigationTreeServiceDraft.loadSubTree(
            findInherited(fullCopy, secondDep.getId()).getData().getId());

        List<Long> inhFirstIds = inheritedFirstDep.findAll((a) -> true).stream()
            .map(tn -> tn.getData().getId())
            .collect(Collectors.toList());

        List<Long> inhSecIds = inheritedSecondDep.findAll((a) -> true).stream()
            .map(tn -> tn.getData().getId())
            .collect(Collectors.toList());

        navigationTreeServiceDraft.deleteNavigationNode(autoUser.getId(), firstDep, true);
        fullCopy = navigationTreeServiceDraft.getNavigationTree(fullCopy.getId());
        Assert.assertEquals(0, findByIds(fullCopy, inhFirstIds).size());

        navigationTreeServiceDraft.deleteNavigationNode(autoUser.getId(), secondDep, false);
        fullCopy = navigationTreeServiceDraft.getNavigationTree(fullCopy.getId());
        List<TreeNode<NavigationNode>> inheritedSec = findByIds(fullCopy, inhSecIds);
        Assert.assertEquals(inhSecIds.size(), inheritedSec.size());
        inheritedSec.forEach(tn -> {
            Assert.assertTrue(tn.getData() instanceof SimpleNavigationNode);
        });
    }

    @Test
    public void testInheritedTreeEdit() {
        NavigationTree tree = createComplicatedTree();
        NavigationTree fullCopy = navigationTreeServiceDraft.copyNavigationTree(autoUser.getId(), 0,
            tree.getRootNodeId(), SyncType.FULL);

        NavigationNode root = tree.getRoot().getData();
        root.setName(root.getName() + "__Q");

        NavigationNode inheritRoot = fullCopy.getRoot().getData();
        navigationTreeServiceDraft.saveNavigationNode(autoUser.getId(), root.copy());

        inheritRoot = navigationTreeServiceDraft.getNavigationNode(inheritRoot.getId());
        Assert.assertEquals(root.getName(), inheritRoot.getName());

        String newname = root.getName() + "__V";
        inheritRoot.setName(newname);
        navigationTreeServiceDraft.saveNavigationNode(autoUser.getId(), inheritRoot.copy());

        inheritRoot = navigationTreeServiceDraft.getNavigationNode(inheritRoot.getId());
        Assert.assertEquals(newname, inheritRoot.getName());

        navigationTreeServiceDraft.resetOverride(autoUser.getId(), (InheritedNavigationNode) inheritRoot);
        inheritRoot = navigationTreeServiceDraft.getNavigationNode(inheritRoot.getId());
        Assert.assertEquals(root.getName(), inheritRoot.getName());

        NavigationNode firstDep = fullCopy.getRoot().getChildren().get(0).getData();
        navigationTreeServiceDraft.cancelInheritanceNode(autoUser.getId(), (InheritedNavigationNode) firstDep, true);

        navigationTreeServiceDraft.loadSubTree(firstDep.getId()).findAll(nd -> {
            Assert.assertTrue(nd instanceof SimpleNavigationNode);
            if (nd.getRecipeId() != null) {
                recipeService.deleteRecipe(autoUser.getId(), nd.getRecipeId());
            }
            return false;
        });

        NavigationNode secondDep = fullCopy.getRoot().getChildren().get(1).getData();
        navigationTreeServiceDraft.cancelInheritanceNode(autoUser.getId(), (InheritedNavigationNode) secondDep, false);
        navigationTreeServiceDraft.loadSubTree(secondDep.getId()).findAll(nd -> {
            if (nd.getId() == secondDep.getId()) {
                Assert.assertTrue(nd instanceof SimpleNavigationNode);
            } else {
                Assert.assertFalse(nd instanceof SimpleNavigationNode);
            }
            return false;
        });

        NavigationNode baseRecipeNode = tree.getRoot()
            .find(nd -> nd.getType() == NavigationNode.Type.GURULIGHT_RECIPE).getData();
        Assert.assertNotNull(baseRecipeNode.getRecipeId());
        Assert.assertTrue(baseRecipeNode.getRecipeId() > 0);

        NavigationNode inheritRecipeNode = findInherited(fullCopy, baseRecipeNode.getId()).getData();

        Assert.assertEquals(baseRecipeNode.getRecipeId(), inheritRecipeNode.getRecipeId());
        Recipe recipe = navigationTreeServiceDraft.getRecipe(baseRecipeNode.getRecipeId());
        RecipeFilter filter = recipe.getFilters().get(0);
        filter.setParamType(Param.Type.NUMERIC);
        filter.setMaxValue(BigDecimal.ONE);
        filter.setMinValue(BigDecimal.ONE);

        inheritRecipeNode.setRecipe(recipe);
        navigationTreeServiceDraft.saveNavigationNode(autoUser.getId(), inheritRecipeNode);
        inheritRecipeNode = navigationTreeServiceDraft.getNavigationNode(inheritRecipeNode.getId());
        Assert.assertTrue(inheritRecipeNode instanceof InheritedNavigationNode);
        Assert.assertNotEquals(baseRecipeNode.getRecipeId(), inheritRecipeNode.getRecipeId());
        Long newRecipeId = inheritRecipeNode.getRecipeId();
        Assert.assertNotNull(newRecipeId);
        Assert.assertTrue(newRecipeId > 0);

        inheritRecipeNode.setType(NavigationNode.Type.URL);
        inheritRecipeNode.setLink(new Link("qqq"));
        navigationTreeServiceDraft.saveNavigationNode(autoUser.getId(), inheritRecipeNode);

        inheritRecipeNode = navigationTreeServiceDraft.getNavigationNode(inheritRecipeNode.getId());
        Assert.assertTrue(inheritRecipeNode instanceof SimpleNavigationNode); //broken inheritance
        Assert.assertNull(inheritRecipeNode.getRecipeId());
        Assert.assertNotNull(inheritRecipeNode.getLinkId());
        Assert.assertNotNull(navigationTreeServiceDraft.getLink(inheritRecipeNode.getLinkId()));
        Assert.assertNull(navigationTreeServiceDraft.getRecipe(newRecipeId));
    }

    @Test
    public void testInheritedTreePositions() {
        NavigationTree tree = createComplicatedTree();
        NavigationTree fullCopy = navigationTreeServiceDraft.copyNavigationTree(autoUser.getId(), 0,
            tree.getRootNodeId(), SyncType.FULL);
        NavigationNode firstLeaf = getLeafs(tree).get(0).getData();

        NavigationNode virtual1 = virtualNode("virtx");
        navigationTreeServiceDraft.addNavigationTreeNode(autoUser.getId(),
            firstLeaf.getId(), new TreeNode<>(virtual1));

        virtual1 = navigationTreeServiceDraft.getNavigationNode(virtual1.getId());
        Assert.assertEquals(Integer.valueOf(0), virtual1.getPosition());

        NavigationNode virtual2 = virtualNode("virtx2");
        navigationTreeServiceDraft.addNavigationTreeNode(autoUser.getId(),
            firstLeaf.getId(), new TreeNode<>(virtual2));

        virtual2 = navigationTreeServiceDraft.getNavigationNode(virtual2.getId());
        Assert.assertEquals(Integer.valueOf(1), virtual2.getPosition());

        fullCopy = navigationTreeServiceDraft.getNavigationTree(fullCopy.getId());
        NavigationNode virtual1inh = findInherited(fullCopy, virtual1.getId()).getData();

        navigationTreeServiceDraft.deleteNavigationNode(autoUser.getId(), virtual1inh, true);
        fullCopy = navigationTreeServiceDraft.getNavigationTree(fullCopy.getId());

        NavigationNode virtual3 = virtualNode("virtx3");
        navigationTreeServiceDraft.addNavigationTreeNode(autoUser.getId(),
            findInherited(fullCopy, firstLeaf.getId()).getData().getId(), new TreeNode<>(virtual3));
        //now positions are [2, 3] in inherited

        fullCopy = navigationTreeServiceDraft.getNavigationTree(fullCopy.getId());

        NavigationNode virtual4 = virtualNode("virtx4");
        navigationTreeServiceDraft.addNavigationTreeNode(autoUser.getId(),
            firstLeaf.getId(), new TreeNode<>(virtual4));

        //positions shouldn't be reordered in inherited
        //position of node added only in inherited tree should be shifted
        fullCopy = navigationTreeServiceDraft.getNavigationTree(fullCopy.getId());
        NavigationNode virtual2inh = findInherited(fullCopy, virtual2.getId()).getData();
        virtual3 = navigationTreeServiceDraft.getNavigationNode(virtual3.getId());
        NavigationNode virtual4inh = findInherited(fullCopy, virtual4.getId()).getData();

        Assert.assertEquals(Integer.valueOf(1), virtual2inh.getPosition());
        Assert.assertFalse(((InheritedNavigationNode) virtual2inh).isPositionOverriden());

        Assert.assertEquals(Integer.valueOf(2), virtual4inh.getPosition());
        Assert.assertFalse(((InheritedNavigationNode) virtual4inh).isPositionOverriden());

        Assert.assertEquals(Integer.valueOf(3), virtual3.getPosition());
    }

    @Test
    public void testIfIncorrectTreePublished() {
        NavigationTree tree = createBaseTree();
        tree.setCode(KnownIds.IGNORE_TREE_CODE);
        navigationTreeServiceDraft.saveNavigationTree(autoUser.getId(), tree, false);
        navigationTreePublishService.publishTree(autoUser.getId(), tree.getId(), false);

        //Save incorrect tree manual
        tree = navigationTreeService.getNavigationTree(tree.getId());
        NavigationNode incorrectNode = SimpleNavigationNode.newValue();
        incorrectNode.setName("incorrect node");
        incorrectNode.setType(NavigationNode.Type.CATEGORY);
        incorrectNode.setHid(INCORRECT_HID);
        incorrectNode.setIsPrimary(false);
        incorrectNode.setPosition(0);
        tree.getRoot().addChild(incorrectNode);
        navigationTreeService.saveNavigationTree(autoUser.getId(), tree, true);

        //try to publish correct tree
        navigationTreePublishService.publishTree(autoUser.getId(), tree.getId(), false);
    }

    private NavigationNode virtualNode(String name) {
        NavigationNode newNode = SimpleNavigationNode.newValue();
        newNode.setType(NavigationNode.Type.GENERIC);
        newNode.setName(name);
        return newNode;
    }

    private List<TreeNode<NavigationNode>> findByIds(NavigationTree tree, Collection<Long> ids) {
        return tree.getRoot().findAll(tn -> ids.contains(tn.getId()));
    }

    private TreeNode<NavigationNode> findInherited(NavigationTree tree, long id) {
        return tree.getRoot()
            .find(nd -> Objects.equals(id, nd.getMasterNodeId()));
    }

    private boolean compareNodeValues(NavigationNode node1, NavigationNode node2) {
        if (node1 == null && node2 != null) {
            log.error("node1 is null for " + node2);
            return false;
        }
        if (node2 == null && node1 != null) {
            log.error("node2 is null for " + node1);
            return false;
        }
        SimpleNavigationNode sm1 = SimpleNavigationNode.copy(node1, new SimpleNavigationNode());
        SimpleNavigationNode sm2 = SimpleNavigationNode.copy(node2, new SimpleNavigationNode());
        sm1.setId(0);
        sm2.setId(0);
        return sm1.equals(sm2);
    }

    private NavigationTree createComplicatedTree() {
        NavigationTree tree = createBaseTree();

        NavigationNode virtual = virtualNode("virtual node");
        navigationTreeServiceDraft.addNavigationTreeNode(autoUser.getId(), tree.getRootNodeId(),
            new TreeNode<>(virtual));
        Assert.assertTrue(virtual.getId() > 0);

        NavigationNode recipeNode = SimpleNavigationNode.newValue();
        recipeNode.setName("recipe node");
        recipeNode.setType(NavigationNode.Type.GURULIGHT_RECIPE);
        Recipe recipe = createRecipe(recipeNode.getName());
        recipeNode.setRecipe(recipe);
        recipeNode.setHid(recipe.getHid());

        long randomHid = getRandomHid();
        log.debug("randomHid " + randomHid);
        TreeNode<NavigationNode> parent = tree.getRoot().find(nd -> nd.getHid().equals(randomHid));

        navigationTreeServiceDraft.addNavigationTreeNode(autoUser.getId(), parent.getData().getId(),
            new TreeNode<>(recipeNode));
        Assert.assertTrue(recipeNode.getId() > 0);
        recipeNode = navigationTreeServiceDraft.getNavigationNode(recipeNode.getId());
        Assertions.assertThat(recipeNode.getRecipeId()).isPositive();
        Assert.assertNull(recipeNode.getLinkId());
        log.debug("added recipe " + recipeNode.getRecipeId() + " to " + recipeNode.getId());

        NavigationNode linkNode = SimpleNavigationNode.newValue();
        linkNode.setName("link node");
        linkNode.setType(NavigationNode.Type.URL);
        linkNode.setLink(new Link("Url to somewhere"));

        navigationTreeServiceDraft.addNavigationTreeNode(autoUser.getId(), parent.getData().getId(),
            new TreeNode<>(linkNode));
        Assert.assertTrue(linkNode.getId() > 0);
        linkNode = navigationTreeServiceDraft.getNavigationNode(linkNode.getId());
        Assertions.assertThat(linkNode.getLinkId()).isPositive();
        Assert.assertNull(linkNode.getRecipeId());
        log.debug("added link " + linkNode.getLinkId() + " to " + linkNode.getId());
        return navigationTreeServiceDraft.getNavigationTree(tree.getId());
    }

    private NavigationTree createBaseTree() {
        TreeNode<NavigationNode> nodes = navigationTreeCopyService.getTovarTreeCopy();
        int size = nodes.findAll(a -> true).size();
        Assert.assertEquals(getTovarCategoriesSize(), size);

        NavigationTree tree = new NavigationTree();
        tree.setName(TREE_NAME);
        tree.setRoot(nodes);
        navigationTreeServiceDraft.saveNavigationTree(autoUser.getId(), tree, true);
        tree = navigationTreeServiceDraft.getNavigationTree(tree.getId());
        Assert.assertNotNull(tree);
        Assert.assertNull(tree.getSyncTreeId());
        Assert.assertNull(tree.getSyncType());

        tree.getRoot().findAll(nd -> {
            Assert.assertNotNull(nd.getHid());
            Assert.assertNotNull(nd.getName());
            Assert.assertTrue(nd instanceof SimpleNavigationNode);
            Assert.assertTrue(nd.getIsPrimary());
            Assert.assertEquals(NavigationNode.Type.CATEGORY, nd.getType());
            return false;
        });

        return tree;
    }

    private int getTovarCategoriesSize() {
        return navigationTreeServiceDraft.getCachedTovarTree().getHidMap().size();
    }

    private Recipe createRecipe(String nodeName) {
        Recipe recipe = new Recipe();
        recipe.setNavigation(true);
        recipe.setName(nodeName);
        recipe.setHid(getRandomGurulightHid());
        RecipeFilter recipeFilter = new RecipeFilter();
        recipeFilter.setParamId(KnownIds.VENDOR_PARAM_ID);
        recipeFilter.setParamType(Param.Type.ENUM);
        recipeFilter.setValueIds(Collections.singletonList((long) RANDOM.nextInt()));
        recipe.addFilter(recipeFilter);
        return recipe;
    }

    private List<TreeNode<NavigationNode>> getLeafs(NavigationTree tree) {
        return tree.getRoot().findAllByNodeCriteria(tn -> tn.getChildren().isEmpty());
    }

    private Long getRandomGurulightHid() {
        List<Long> nodes = navigationTreeServiceDraft.getCachedTovarTree().getHidMap().values()
            .stream()
            .filter(tcn -> tcn.getData().getOutputType() == OutputType.GURULIGHT ||
                tcn.getData().getOutputType() == OutputType.VISUAL)
            .map(tcn -> tcn.getHid())
            .collect(Collectors.toList());
        return nodes.get(RANDOM.nextInt(nodes.size()));
    }

    private Long getRandomHid() {
        List<Long> nodes = new ArrayList<>(navigationTreeServiceDraft.getCachedTovarTree().getHidMap().keySet());
        return nodes.get(RANDOM.nextInt(nodes.size()));
    }
}
