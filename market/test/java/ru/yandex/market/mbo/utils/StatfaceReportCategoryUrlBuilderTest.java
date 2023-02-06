package ru.yandex.market.mbo.utils;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.CachedTreeService;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;

import java.net.URLDecoder;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 17.05.2017
 */
@SuppressWarnings("checkstyle:magicNumber")
public class StatfaceReportCategoryUrlBuilderTest {

    private static final long FIRST_CATEGORY_ID = 1L;
    private static final long SECOND_CATEGORY_ID = 2L;
    private static final long THIRD_CATEGORY_ID = 3L;
    private static final long UNKNOWN_CATEGORY_ID = 5L;
    private static final String FIRST_CATEGORY_NAME = "Все товары";
    private static final String SECOND_CATEGORY_NAME = "Авто";
    private static final String THIRD_CATEGORY_NAME = "Шины и диски";
    private static final String TEMPLATE = "{" + StatfaceReportCategoryUrlBuilder.CATEGORY_PATH_PLACEHOLDER + "}";

    @Test
    public void containsAllCategories() throws Exception {
        StatfaceReportCategoryUrlBuilder pathBuilder = makeBuilder();
        String path = URLDecoder.decode(pathBuilder.build(TEMPLATE, THIRD_CATEGORY_ID), "UTF-8");
        String[] names = path.split(StatfaceReportCategoryUrlBuilder.PATH_DELIMITER);

        Assert.assertEquals(4, names.length);
        Assert.assertEquals(FIRST_CATEGORY_NAME, names[1]);
        Assert.assertEquals(SECOND_CATEGORY_NAME, names[2]);
        Assert.assertEquals(THIRD_CATEGORY_NAME, names[3]);
    }

    @Test
    public void emptyWhenUnknownCategory() throws Exception {
        StatfaceReportCategoryUrlBuilder pathBuilder = makeBuilder();
        String path = URLDecoder.decode(pathBuilder.build(TEMPLATE, UNKNOWN_CATEGORY_ID), "UTF-8");
        Assert.assertTrue(path.isEmpty());
    }

    private StatfaceReportCategoryUrlBuilder makeBuilder() {
        return new StatfaceReportCategoryUrlBuilder(mockTreeService(), true);
    }

    private CachedTreeService mockTreeService() {
        CachedTreeService cachedTreeService = Mockito.mock(CachedTreeService.class);
        Mockito.when(cachedTreeService.loadCachedWholeTree()).thenReturn(makeTree());
        return cachedTreeService;
    }

    private TovarTree makeTree() {
        TovarCategoryNode rootNode =
            new TovarCategoryNode(new TovarCategory(FIRST_CATEGORY_NAME, FIRST_CATEGORY_ID, 0));
        TovarCategoryNode secondNode =
            new TovarCategoryNode(new TovarCategory(SECOND_CATEGORY_NAME, SECOND_CATEGORY_ID, FIRST_CATEGORY_ID));

        rootNode.addChild(secondNode);

        TovarCategoryNode thirdNode =
            new TovarCategoryNode(new TovarCategory(THIRD_CATEGORY_NAME, THIRD_CATEGORY_ID, SECOND_CATEGORY_ID));

        secondNode.addChild(thirdNode);

        return new TovarTree(rootNode);
    }
}
