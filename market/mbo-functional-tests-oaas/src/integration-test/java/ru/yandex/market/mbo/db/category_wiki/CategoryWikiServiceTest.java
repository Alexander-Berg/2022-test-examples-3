package ru.yandex.market.mbo.db.category_wiki;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.common.imageservice.avatars.AvatarImageDepotService;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.MboDbSelector;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.visual.CategoryWiki;
import ru.yandex.market.mbo.gwt.models.visual.CategoryWikiPictureRow;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author eremeevvo
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CategoryWikiServiceTest extends BaseIntegrationTest {

    private static final long CATEGORY_ID = -1;
    private static final long ROOT = 1;
    private static final long CHILD = 2;
    private static final long CHILD_1 = 3;
    private static final long EMPTY_ROOT = 4;

    private static final CategoryWikiPictureRow PICTURE_ROW = new CategoryWikiPictureRow()
        .setComments("comments");

    private static final CategoryWiki CATEGORY_WIKI_ROOT = new CategoryWiki()
        .setCategoryId(ROOT)
        .setInCategory("inCategory")
        .setOutOfCategory("outOfCategory")
        .setDefiningParamsComment("definingParamsComment")
        .setWayOfCreatingSku("wayOfCreatingSku")
        .setTicketsLink("ticketsLink")
        .setCategoryWikiPictureRows(Collections.singletonList(PICTURE_ROW));

    private static final CategoryWiki CATEGORY_WIKI_CHILD = new CategoryWiki()
        .setCategoryId(CHILD).setInCategory("child");

    private static final CategoryWiki CATEGORY_WIKI_CHILD_2 = new CategoryWiki()
        .setCategoryId(CHILD_1).setInCategory("child1");

    private TovarCategoryNode child;
    private TovarCategoryNode child1;
    private TovarCategoryNode root;
    private TovarCategoryNode emptyRoot;

    @Autowired
    @Qualifier("categoryWikiDbSelector")
    private MboDbSelector categoryWikiDbSelector;


    @Autowired
    private NamedParameterJdbcOperations namedContentJdbcTemplate;

    @Autowired
    private TransactionTemplate contentTransactionTemplate;

    private CategoryWikiService categoryWikiService;

    @Before
    public void setUp() {

        IParameterLoaderService parameterLoaderService = Mockito.mock(IParameterLoaderService.class);
        when(parameterLoaderService.loadCategoryEntitiesByHid(any(Long.class)))
            .thenReturn(Mockito.mock(CategoryEntities.class));
        CategoryWikiPictureRowService categoryWikiPictureRowService = new CategoryWikiPictureRowService(
            namedContentJdbcTemplate, categoryWikiDbSelector, Mockito.mock(AvatarImageDepotService.class));

        categoryWikiService = new CategoryWikiService(namedContentJdbcTemplate, categoryWikiDbSelector,
            categoryWikiPictureRowService,
            contentTransactionTemplate, parameterLoaderService);

        child = createTovarCategory(CHILD, ROOT);
        child1 = createTovarCategory(CHILD_1, CHILD);
        root = createTovarCategory(ROOT, -1);
        emptyRoot = createTovarCategory(EMPTY_ROOT, -1);
        root.addChild(child);
        child.addChild(child1);
    }

    @Test
    public void insertAndGetCategoryWiki() {
        categoryWikiService.insertCategoryWiki(CATEGORY_ID);
        Assertions.assertThat(categoryWikiService.getCategoryWiki(CATEGORY_ID)).isNotNull();
    }

    @Test(expected = EmptyResultDataAccessException.class)
    public void insertThenDelete() {
        categoryWikiService.insertCategoryWiki(CATEGORY_ID);
        categoryWikiService.deleteCategoryWiki(CATEGORY_ID);
        categoryWikiService.getCategoryWiki(CATEGORY_ID);
    }

    @Test
    public void updateCategoryWiki() {
        EnhancedRandom enhancedRandom = new EnhancedRandomBuilder()
            .seed(29883)
            .overrideDefaultInitialization(true)
            .build();

        CategoryWiki expectedCategoryWiki = enhancedRandom.nextObject(CategoryWiki.class);

        long categoryId = expectedCategoryWiki.getCategoryId();
        CategoryWikiPictureRow pictureRow1 = new CategoryWikiPictureRow()
            .setCategoryId(categoryId).setComments("comment1");
        CategoryWikiPictureRow pictureRow2 = new CategoryWikiPictureRow()
            .setCategoryId(categoryId).setComments("comment2");
        expectedCategoryWiki.setCategoryWikiPictureRows(Arrays.asList(
            pictureRow1, pictureRow2
        ));

        expectedCategoryWiki.setDefiningParams(null);

        categoryWikiService.insertCategoryWiki(expectedCategoryWiki);

        CategoryWiki actualCategoryWiki = categoryWikiService.getCategoryWiki(categoryId);

        Assertions.assertThat(actualCategoryWiki)
            .isEqualToComparingFieldByField(expectedCategoryWiki);
    }

    @Test
    public void getAllCategoryWikiToRootTest() {
        categoryWikiService.insertCategoryWiki(CATEGORY_WIKI_ROOT);
        categoryWikiService.insertCategoryWiki(CATEGORY_WIKI_CHILD);
        categoryWikiService.insertCategoryWiki(CATEGORY_WIKI_CHILD_2);
        categoryWikiService.insertCategoryWiki(createEmptyCategoryWiki(emptyRoot.getHid()));

        List<CategoryWiki> list = categoryWikiService.getAllCategoryWikiToRoot(child1)
            .getInheritedCategoryWikis();
        Assertions.assertThat(list).usingElementComparatorOnFields("inCategory")
            .contains(CATEGORY_WIKI_CHILD, CATEGORY_WIKI_ROOT);
    }

    @Test
    public void validateEmptyCategoryWikiTest() {
        categoryWikiService.insertCategoryWiki(CATEGORY_WIKI_ROOT);
        categoryWikiService.insertCategoryWiki(CATEGORY_WIKI_CHILD);
        categoryWikiService.insertCategoryWiki(CATEGORY_WIKI_CHILD_2);
        categoryWikiService.insertCategoryWiki(createEmptyCategoryWiki(emptyRoot.getHid()));

        Assertions.assertThatThrownBy(() -> {
            categoryWikiService.validateCategoryWiki(createEmptyCategoryWiki(emptyRoot.getHid()), emptyRoot);
        }).isInstanceOf(OperationException.class)
            .hasMessageContaining("Не заполнено поле 'Входит в категорию'")
            .hasMessageContaining("Не заполнено поле 'Не входит в категорию'")
            .hasMessageContaining("Не заполнено поле 'Принцип деления моделей: комментарий'")
            .hasMessageContaining("Не заполнено поле 'Способ заведения SKU'")
            .hasMessageContaining("Не заполнено поле 'Ссылки на тикет'")
            .hasMessageContaining("Не заполнено поле комментария в таблице 'Уникальная информация'");
    }

    @Test
    public void validateFilledCategoryWikiTest() {
        categoryWikiService.validateCategoryWiki(CATEGORY_WIKI_ROOT, root);
    }

    @Test
    public void validateEmptyNotLeafCategoryWikiTest() {
        CategoryWiki emptyCategoryWiki = createEmptyCategoryWiki(child.getHid());
        categoryWikiService.validateCategoryWiki(emptyCategoryWiki, child);
    }

    @Test
    public void validateInheritanceTest() {
        categoryWikiService.validateCategoryWiki(CATEGORY_WIKI_CHILD, child);
    }

    private static TovarCategoryNode createTovarCategory(long categoryId, long parent) {
        TovarCategory tovarCategory = new TovarCategory();
        tovarCategory.setHid(categoryId);
        tovarCategory.setParentHid(parent);
        return new TovarCategoryNode(tovarCategory);
    }

    private static CategoryWiki createEmptyCategoryWiki(long categoryId) {
        CategoryWiki categoryWiki = new CategoryWiki();
        categoryWiki.setCategoryId(categoryId);
        return categoryWiki;
    }
}
