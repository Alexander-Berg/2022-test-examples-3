package ru.yandex.market.mbo.reactui.controller.data;

import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.common.gwt.security.AccessControlManager;
import ru.yandex.market.mbo.core.guru.GuruCategoryService;
import ru.yandex.market.mbo.db.CachedTreeService;
import ru.yandex.market.mbo.db.RedisCachedTreeService;
import ru.yandex.market.mbo.db.TovarTreeForVisualService;
import ru.yandex.market.mbo.gwt.models.IdAndName;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.reactui.dto.DisplayCategory;
import ru.yandex.market.mbo.reactui.errors.EmptyTovarTreeRedisCacheException;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author yuramalinov
 * @created 15.10.2019
 */
public class CategoryTreeControllerTest {

    private static final long DEFAULT_HID = -1L;
    private static final long DEFAULT_GURU_ID = 0L;
    private static final long ROOT_HID = 1L;
    private static final long CHILD1_HID = 2L;
    private static final long CHILD2_HID = 3L;
    private static final long ROOT_GURU_ID = 4L;
    private static final long CHILD1_GURU_ID = 5L;

    private RedisCachedTreeService redisCachedTreeService;
    private CategoryTreeController categoryTreeController;

    @Before
    public void setup() {
        CachedTreeService cachedTreeService = mock(CachedTreeService.class);
        redisCachedTreeService = mock(RedisCachedTreeService.class);
        GuruCategoryService guruCategoryService = mock(GuruCategoryService.class);
        AccessControlManager accessControlManager = mock(AccessControlManager.class);
        NamedParameterJdbcTemplate namedContentJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        TovarTreeForVisualService visualServiceGoodsTree = new TovarTreeForVisualService(
            namedContentJdbcTemplate,
            cachedTreeService
        );
        categoryTreeController = new CategoryTreeController(
            redisCachedTreeService,
            visualServiceGoodsTree,
            guruCategoryService,
                accessControlManager
        );
        when(cachedTreeService.getTovarTree()).thenReturn(getGoodsTree());
        when(cachedTreeService.loadWholePublishedTree()).thenReturn(getVisualGoodsTree());
        when(guruCategoryService.getGuruCategoryNames(eq(""))).thenReturn(getGuruIdsAndNames());
    }

    @Test
    public void testGetAllCategories() throws EmptyTovarTreeRedisCacheException {
        when(redisCachedTreeService.getTovarTree()).thenReturn(getGoodsTree());

        List<DisplayCategory> categories = categoryTreeController.getCategories();
        assertReturnedCategories(categories,
            Tuple.tuple(ROOT_HID, "Root", DEFAULT_HID, ROOT_GURU_ID, "GuruRoot"),
            Tuple.tuple(CHILD1_HID, "Child", ROOT_HID, CHILD1_GURU_ID, "GuruChild"),
            Tuple.tuple(CHILD2_HID, "Child2", ROOT_HID, DEFAULT_GURU_ID, null));
    }

    @Test(expected = EmptyTovarTreeRedisCacheException.class)
    public void testGetAllCategoriesEmptyRedisCache() throws EmptyTovarTreeRedisCacheException {
        List<DisplayCategory> categories = categoryTreeController.getCategories();
    }

    @Test
    public void testGetPublishedCategories() {
        List<DisplayCategory> categories = categoryTreeController.getVisualCategories();
        assertReturnedDisplayCategories(categories,
            Tuple.tuple(CHILD1_HID, "Child", ROOT_HID, true),
            Tuple.tuple(ROOT_HID, "Root", DEFAULT_HID, true));
    }

    private static void assertReturnedDisplayCategories(List<DisplayCategory> categories, Tuple... thatMatch) {
        Assertions.assertThat(categories)
            .hasSize(thatMatch.length)
            .extracting(DisplayCategory::getHid, DisplayCategory::getName,
                DisplayCategory::getParentHid,
                DisplayCategory::isPublished)
            .containsExactlyInAnyOrder(thatMatch);
    }

    private static void assertReturnedCategories(List<DisplayCategory> categories, Tuple... thatMatch) {
        Assertions.assertThat(categories)
            .hasSize(thatMatch.length)
            .extracting(DisplayCategory::getHid, DisplayCategory::getName, DisplayCategory::getParentHid,
                DisplayCategory::getGuruCategoryId, DisplayCategory::getGuruCategoryName)
            .containsExactlyInAnyOrder(thatMatch);
    }

    @NotNull
    private static TovarTree getGoodsTree() {
        TovarCategory root = new TovarCategory("Root", ROOT_HID, DEFAULT_HID);
        TovarCategory child = new TovarCategory("Child", CHILD1_HID, ROOT_HID);
        TovarCategory child2 = new TovarCategory("Child2", CHILD2_HID, ROOT_HID);
        root.setGuruCategoryId(ROOT_GURU_ID);
        child.setGuruCategoryId(CHILD1_GURU_ID);
        TovarCategoryNode rootNode = new TovarCategoryNode(root);
        TovarCategoryNode goodsNode = new TovarCategoryNode(child);
        TovarCategoryNode goodsNode2 = new TovarCategoryNode(child2);
        rootNode.addChildren(Arrays.asList(goodsNode, goodsNode2));
        return new TovarTree(rootNode);
    }

    @NotNull
    private static List<IdAndName> getGuruIdsAndNames() {
        return Arrays.asList(
            new IdAndName(ROOT_GURU_ID, "GuruRoot"),
            new IdAndName(CHILD1_GURU_ID, "GuruChild")
        );
    }

    @NotNull
    private static TovarCategoryNode getVisualUsed(TovarCategory goodItem) {
        TovarCategoryNode node = new TovarCategoryNode(goodItem);
        node.getData().setVisual(true);
        node.getData().setNotUsed(false);
        node.getData().setPublished(true);
        return node;
    }

    @NotNull
    private static TovarTree getVisualGoodsTree() {
        TovarCategoryNode rootNode = getVisualUsed(new TovarCategory("Root", ROOT_HID, DEFAULT_HID));
        TovarCategoryNode goodsNode = getVisualUsed(new TovarCategory("Child", CHILD1_HID, ROOT_HID));
        rootNode.addChild(goodsNode);
        return new TovarTree(rootNode);
    }
}
