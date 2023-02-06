package ru.yandex.market.marketpromo.core.service.task;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.service.impl.CategoryTreeCache;
import ru.yandex.market.marketpromo.core.test.ServiceTaskTestBase;
import ru.yandex.market.marketpromo.core.test.utils.CategoriesTestHelper;
import ru.yandex.market.marketpromo.core.test.utils.YtTestHelper;
import ru.yandex.market.marketpromo.model.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CategoryTreeRefreshTaskTest extends ServiceTaskTestBase {

    @Autowired
    private CategoryTreeCache categoryTreeCache;
    @Autowired
    private CategoryTreeRefreshTask categoryTreeTask;
    @Autowired
    private YtTestHelper ytTestHelper;

    @BeforeEach
    void configure() {
        ytTestHelper.mockCategoryTreeResponse();
        categoryTreeCache.refreshCache();
    }

    @Test
    void shouldInitCacheOnStart() {
        assertFalse(categoryTreeCache.get().getCategoryMap().isEmpty());
    }

    @Test
    void shouldReplaceCache() {
        assertThat(categoryTreeCache.get().getCategoryMap().size(), is(5));

        List<Category> categories = new ArrayList<>(CategoriesTestHelper.defaultCategoryList());
        categories.add(new Category(8888L, 0L, false, false,
                "new category", "just added"));
        ytTestHelper.mockCategoryTreeResponse(categories);

        categoryTreeTask.process();

        assertThat(categoryTreeCache.get().getCategoryMap().size(), is(6));
    }

}
