package ru.yandex.market.mbo.db.navigation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.recipes.RecipeService;
import ru.yandex.market.mbo.gwt.models.model_list.ModelList;
import ru.yandex.market.mbo.gwt.models.model_list.ModelListValidator;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.navigation.Action;
import ru.yandex.market.mbo.gwt.models.navigation.FilterConfigValidator;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.SyncType;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.tovartree.ValidationError;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author commince
 * @date 31.08.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
@RunWith(MockitoJUnitRunner.class)
public class NavigationTreeValidatorTest {

    private TovarTreeServiceMock tovarTreeService;
    private NavigationTreeValidator validator = new NavigationTreeValidator();

    @Mock
    private NavigationTreeService navigationTreeServiceDraft;
    @Mock
    private NavigationTreeService navigationTreeService;
    @Mock
    private NavigationTreeConverter navigationTreeConverter;
    @Mock
    private RecipeService recipeService;

    @Before
    public void setup() {
        TovarCategory root = new TovarCategory("Все товары", 0);
        root.setHid(90401);
        root.setPublished(true);
        TovarCategory cat1 = new TovarCategory("Хлам", 90402, 90401);
        TovarCategory cat2 = new TovarCategory("Шлак", 90403, 90401);
        TovarCategory cat21 = new TovarCategory("Злак", 90404, 90403);
        tovarTreeService = new TovarTreeServiceMock(root, cat1, cat2, cat21);

        when(navigationTreeConverter.getNavigationTreeWithoutSkippedNodes(any(NavigationTree.class)))
            .thenAnswer(request -> request.getArguments()[0]);

        validator.setNavigationTreeService(navigationTreeService);
        validator.setNavigationTreeServiceDraft(navigationTreeServiceDraft);
        validator.setNavigationTreeConverter(navigationTreeConverter);
        validator.setRecipeService(recipeService);
        validator.setModelListValidator(new ModelListValidator());
        validator.setFilterConfigValidator(new FilterConfigValidator());
        CommonModel model1 = CommonModelBuilder.newBuilder(1, 10, 100)
            .currentType(CommonModel.Source.GURU)
            .getModel();
        CommonModel model2 = CommonModelBuilder.newBuilder(2, 10, 100)
            .currentType(CommonModel.Source.SKU)
            .getModel();
        CommonModel model3 = CommonModelBuilder.newBuilder(3, 10, 100)
            .currentType(CommonModel.Source.PARTNER)
            .getModel();
        CommonModel model4 = CommonModelBuilder.newBuilder(4, 10, 100)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .getModel();
        CommonModel model5 = CommonModelBuilder.newBuilder(5, 10, 100)
            .currentType(CommonModel.Source.CLUSTER)
            .getModel();
        validator.setModelStorageService(new ModelStorageServiceStub(model1, model2, model3, model4, model5));
    }

    @Test
    public void testValidateMainNidUniquenessFailed() {
        NavigationTree tree = createTree(1L, null);
        tree.getRoot().find(o -> o.getId() == 1111).getData().setIsMain(true);
        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidateMainNidUniquenessSuccess() {
        NavigationTree tree = createTree(1L, null);
        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateInheritanceFailed() {
        NavigationTree tree1 = createTree(1L, null);
        NavigationTree tree2 = createTree(2L, null);
        NavigationTree tree3 = createTree(3L, null);

        tree1.setSyncTreeId(tree2.getId());

        tree2.setSyncTreeId(tree3.getId());
        when(navigationTreeService.getNavigationTree(Mockito.eq(2L))).thenReturn(tree2);
        List<ValidationError> errors = validator.validateTree(tree1, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidateInheritanceSuccess() {
        NavigationTree tree1 = createTree(1L, null);
        NavigationTree tree2 = createTree(2L, null);

        tree1.setSyncTreeId(tree2.getId());

        when(navigationTreeService.getNavigationTree(Mockito.eq(2L))).thenReturn(tree2);
        List<ValidationError> errors = validator.validateTree(tree1, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidatePicturesSuccess() {
        NavigationTree tree = createTree(1L, null);
        tree.setIllustrationDepth(1); // Узлы до первого должны быть проиллюстрированы
        tree.getRoot().getData().setPicture("http://www.ya.ru/h.png");
        tree.getRoot().getChildren().forEach(o -> o.getData().setPicture("http://www.ya.ru/h.png"));

        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidatePicturesNotPublishedSuccess() {
        NavigationTree tree = createTree(1L, null);
        tree.setIllustrationDepth(1); // Узлы до первого должны быть проиллюстрированы
        tree.getRoot().getData().setPicture("http://www.ya.ru/h.png");
        tree.getRoot().getChildren().forEach(o -> {
            //Неопубликованные должны иметь возможность быть без картинки
            if (o.getData().getId() != 11) {
                o.getData().setPicture("http://www.ya.ru/h.png");
            } else {
                o.getData().setPublished(false);
                o.findAll(n -> true).forEach(n -> n.getData().setPublished(false));
            }
        });

        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);

        assertEquals(errors.size(), 2);
    }

    @Test
    public void testEmptyCodeFailed() {
        NavigationTree tree = createTree(1L, null);
        tree.setIllustrationDepth(2); // Узлы до второго должны быть проиллюстрированы
        tree.getRoot().getData().setPicture("http://www.ya.ru/h.png");
        tree.getRoot().getChildren().forEach(o -> {
            if (o.getData().getId() != 11) {
                o.getData().setPicture("http://www.ya.ru/h.png");
            }
        });

        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);

        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidatePicturesFailed() {
        NavigationTree tree = createTree(1L, null);
        tree.setCode("");

        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);

        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidateNotPublishedDepartmentChildsSuccess() {
        NavigationTree tree = createTree(1L, null);
        TreeNode<NavigationNode> dep = tree.getRoot().find(o -> o.getId() == 13L);
        dep.getData().setPublished(false);
        dep.find(o -> o.getId() == 131L).getData().setPublished(false);

        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);

        assertEquals(errors.size(), 0);
    }

    @Test
    public void testValidateNotPublishedDepartmentChildsFailed() {
        NavigationTree tree = createTree(1L, null);
        TreeNode<NavigationNode> dep = tree.getRoot().find(o -> o.getId() == 13L);
        dep.getData().setPublished(true);
        dep.getData().setIsSkipped(true);
        dep.find(o -> o.getId() == 131L).getData().setPublished(true);

        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);

        assertEquals(errors.size(), 1);
    }

    @Test
    public void testValidationWarningsFiltering() {
        NavigationTree tree = createTree(1L, null);
        TreeNode<NavigationNode> dep = tree.getRoot().find(o -> o.getId() == 11L);
        dep.getData().setPublished(true);
        dep.getData().setIsSkipped(true);
        dep.find(o -> o.getId() == 111L).getData().setPublished(true);

        TreeNode<NavigationNode> subTreeRoot = tree.getRoot().find(o -> o.getId() == 12L);

        List<ValidationError> errors =
            validator.validateSubtree(tree.getId(), subTreeRoot, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        errors = errors.stream().filter(e -> e.getNid() != null).collect(Collectors.toList());

        assertTrue(errors.isEmpty());

        errors =
            validator.validateSubtree(tree.getId(), dep, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        errors = errors.stream().filter(e -> e.getNid() != null).collect(Collectors.toList());

        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidateCanonicalStructureOnlyForTovarTreeSync() {
        NavigationTree tree = createTree(1L, null);
        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertTrue(errors.isEmpty());
        tree.setTovarTreeSyncStatus(true);
        errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidateCanonicalStructureSuccess() {
        NavigationTree tree = createCanonicalTree();
        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateCanonicalUniquenessFailed() {
        NavigationTree tree = createCanonicalTree();
        tree.getRoot().addChild(
            new TreeNode<>(createNode(42, 90404,
                NavigationNode.Type.CATEGORY, true, false, false, 0))
        );
        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertEquals(errors.size(), 1);
    }

    @Test
    public void testValidateMissingCanonicalNidWhenNodeExisted() {
        NavigationTree tree = createCanonicalTree();
        tree.findNodeById(121).getData().setIsPrimary(false);
        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertEquals(errors.size(), 1);
    }

    @Test
    public void testValidateMissingCanonicalNidWhenNodeRemoved() {
        NavigationTree tree = createCanonicalTree();
        TreeNode<NavigationNode> nodeById = tree.findNodeById(121);
        nodeById.getParent().removeChild(nodeById);
        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertEquals(errors.size(), 1);
    }

    @Test
    public void testValidateCanonicalStructureFailed() {
        NavigationTree tree = createCanonicalTree();
        TreeNode<NavigationNode> node21 = tree.findNodeById(121);
        node21.getParent().removeChild(node21);
        TreeNode<NavigationNode> node1 = tree.findNodeById(11);
        node1.addChild(node21);

        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertEquals(errors.size(), 1);
    }

    @Test
    public void testValidateMainNidMandatory() {
        NavigationTree tree = createTree(1L, null);
        tree.setMainNidsMandatory(true);
        TovarCategory missCat = new TovarCategory("Супер шлак", 90322, 90401);
        missCat.setPublished(true);
        tovarTreeService.addCategory(missCat);
        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        assertEquals(errors.size(), 1);
    }

    @Test
    public void testTovarTreeIsNotCorrupted() {
        NavigationTree tree = createTree(1L, null);
        tree.setMainNidsMandatory(true);
        TovarTree tovarTree = tovarTreeService.loadTovarTree();
        assertNotNull(tovarTree.findByHid(90402L));
        validator.validateTree(tree, tovarTree, Action.PUBLISH);
        assertNotNull(tovarTree.findByHid(90402L));
    }

    @Test
    public void testWhenCorrectModelListThenOk() {
        NavigationTree tree = createTreeWithNodeListNavigationNode(Arrays.asList(1L, 2L, 3L, 4L));

        List<ValidationError> errors = validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testWhenEmptyModelListThenFail() {
        NavigationTree tree = createTreeWithNodeListNavigationNode(Collections.emptyList());

        Assertions.assertThatExceptionOfType(OperationException.class)
            .isThrownBy(() -> {
                validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
            }).withMessage("Список моделей пуст");
    }

    @Test
    public void testWhenZeroInModelListThenFail() {
        NavigationTree tree = createTreeWithNodeListNavigationNode(Collections.singletonList(0L));

        Assertions.assertThatExceptionOfType(OperationException.class)
            .isThrownBy(() -> {
                validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
            }).withMessage("Все идентификаторы моделей должны быть больше нуля");
    }

    @Test
    public void testWhenNullInModelListThenFail() {
        NavigationTree tree = createTreeWithNodeListNavigationNode(Collections.singletonList(null));

        Assertions.assertThatExceptionOfType(OperationException.class)
            .isThrownBy(() -> {
                validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
            }).withMessage("Список моделей содержит идентификатор модели равный NULL");
    }

    @Test
    public void testWhenRepeatedIdsInModelListThenFail() {
        NavigationTree tree = createTreeWithNodeListNavigationNode(Arrays.asList(1L, 1L));

        Assertions.assertThatExceptionOfType(OperationException.class)
            .isThrownBy(() -> {
                validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
            }).withMessageContaining("Список моделей содержит повторяющийся идентификатор модели");
    }

    @Test
    public void testWhenWrongTypeIDInModelListThenFail() {
        NavigationTree tree = createTreeWithNodeListNavigationNode(Arrays.asList(1L, 2L, 5L));

        Assertions.assertThatExceptionOfType(OperationException.class)
            .isThrownBy(() -> {
                validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
            }).withMessageContaining("Список содержит неизвестные идентификаторы моделей/SKU");
    }

    @Test
    public void testWhenUnknownIDInModelListThenFail() {
        NavigationTree tree = createTreeWithNodeListNavigationNode(Arrays.asList(2L, 3L, 7L));

        Assertions.assertThatExceptionOfType(OperationException.class)
            .isThrownBy(() -> {
                validator.validateTree(tree, tovarTreeService.loadTovarTree(), Action.PUBLISH);
            }).withMessageContaining("Список содержит неизвестные идентификаторы моделей/SKU");
    }

    private NavigationNode createNode(long id, long hid, NavigationNode.Type type,
                                      boolean primary, boolean main, boolean hidden, int position) {
        NavigationNode node = new SimpleNavigationNode();
        node.setId(id);
        node.setHid(hid);
        node.setType(type);
        node.setName("Узел " + id + " hid " + hid);
        node.setIsPrimary(primary);
        node.setIsHidden(hidden);
        node.setPromo(false);
        node.setTouchHide(false);
        node.setApplicationHide(false);
        node.setGreen(true);
        node.setPublished(true);
        node.setPosition(position);
        node.setIsMain(main);
        node.setIsSkipped(false);
        return node;
    }

    private NavigationTree createTree(Long id, Long syncTreeId) {
        NavigationTree navigationTree = new NavigationTree();

        TreeNode<NavigationNode> node1 = new TreeNode<>(createNode(1, 90401,
            NavigationNode.Type.CATEGORY, true, true, false, 0));
        TreeNode<NavigationNode> node11 = new TreeNode<>(createNode(11, 90402,
            NavigationNode.Type.CATEGORY, true, true, false, 1));
        node11.getData().setParentId(1);
        TreeNode<NavigationNode> node12 = new TreeNode<>(createNode(12, 90403,
            NavigationNode.Type.CATEGORY, true, true, false, 2));
        node12.getData().setParentId(1);
        TreeNode<NavigationNode> node111 = new TreeNode<>(createNode(111, 90404,
            NavigationNode.Type.CATEGORY, true, true, false, 3));
        node111.getData().setParentId(11);
        TreeNode<NavigationNode> node1111 = new TreeNode<>(createNode(1111, 90402,
            NavigationNode.Type.CATEGORY, true, false, false, 4));
        node1111.getData().setParentId(111);
        TreeNode<NavigationNode> node13 = new TreeNode<>(createNode(13, 90402,
            NavigationNode.Type.CATEGORY, false, false, false, 1));
        node11.getData().setParentId(1);
        TreeNode<NavigationNode> node131 = new TreeNode<>(createNode(131, 90402,
            NavigationNode.Type.CATEGORY, false, false, false, 1));
        node11.getData().setParentId(131);


        node1.addChild(node11);
        node1.addChild(node12);
        node1.addChild(node13);
        node11.addChild(node111);
        node111.addChild(node1111);
        node13.addChild(node131);

        navigationTree.setId(id);
        navigationTree.setCode("TEST_TREE");
        navigationTree.setTovarTreeSyncStatus(false);
        navigationTree.setMainNidsMandatory(false);
        navigationTree.setRootNodeId(node1.getData().getId());
        navigationTree.setRoot(node1);
        navigationTree.setName("Дерево " + id);
        if (syncTreeId != null) {
            navigationTree.setSyncType(SyncType.FULL);
            navigationTree.setSyncTreeId(1L);
        }
        navigationTree.setDomains(Arrays.asList("1", "2", "3"));

        doReturn(navigationTree).when(navigationTreeService).getNavigationTree(ArgumentMatchers.eq(id));

        return navigationTree;
    }

    private NavigationTree createCanonicalTree() {
        NavigationTree navigationTree = new NavigationTree();
        navigationTree.setId(1L);
        navigationTree.setCode("TEST_TREE");
        navigationTree.setTovarTreeSyncStatus(true);
        navigationTree.setMainNidsMandatory(false);

        TreeNode<NavigationNode> root = new TreeNode<>(createNode(1, 90401,
            NavigationNode.Type.CATEGORY, true, true, false, 0));
        TreeNode<NavigationNode> node1 = new TreeNode<>(createNode(11, 90402,
            NavigationNode.Type.CATEGORY, true, true, false, 1));
        TreeNode<NavigationNode> node2 = new TreeNode<>(createNode(12, 90403,
            NavigationNode.Type.CATEGORY, true, true, false, 2));
        TreeNode<NavigationNode> node21 = new TreeNode<>(createNode(121, 90404,
            NavigationNode.Type.CATEGORY, true, true, false, 3));

        root.addChild(node1);
        root.addChild(node2);
        node2.addChild(node21);

        navigationTree.setRoot(root);
        navigationTree.setRootNodeId(root.getData().getId());

        return navigationTree;
    }

    private NavigationTree createTreeWithNodeListNavigationNode(List<Long> modelIds) {
        NavigationTree tree = createTree(1L, null);
        NavigationNode node = createNode(42, 0,
            NavigationNode.Type.MODEL_LIST, false, false, false, 0);
        ModelList modelList = new ModelList();
        modelList.setId(1L);
        modelList.setModelIds(modelIds);
        node.setModelList(modelList);
        node.setModelListId(1L);
        node.setHid(null);
        tree.getRoot().addChild(new TreeNode<>(node));
        return tree;
    }
}
