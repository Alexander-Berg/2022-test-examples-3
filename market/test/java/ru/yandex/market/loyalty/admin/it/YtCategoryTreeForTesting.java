package ru.yandex.market.loyalty.admin.it;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.loyalty.admin.config.ITConfig;
import ru.yandex.market.loyalty.admin.tms.CategoryTreeUpdateProcessor;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.model.CategoryTree;
import ru.yandex.market.loyalty.core.service.CategoryTreeService;
import ru.yandex.market.loyalty.core.test.DbCleaner;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;
import ru.yandex.market.loyalty.core.test.SupplementaryDataLoader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotSame;

@Ignore
@ContextConfiguration(classes = ITConfig.class)
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
@TestPropertySource(
        locations = {"/it.secret.local.properties"}
)
public class YtCategoryTreeForTesting {
    @Autowired
    private DbCleaner dbCleaner;
    @Autowired
    private SupplementaryDataLoader supplementaryDataLoader;
    @Autowired
    private CategoryTreeService categoryTreeService;
    @Autowired
    @YtHahn
    private CategoryTreeUpdateProcessor categoryTreeUpdateProcessor;

    @Before
    public void prepareDatabase() {
        dbCleaner.clearDb();
        supplementaryDataLoader.createTechnicalIfNotExists();
        supplementaryDataLoader.createEmptyOperationContext();
        supplementaryDataLoader.populateCategoryTree();
    }

    @Test
    public void shouldUpdateCategoryTree() {
        CategoryTree oldTree = categoryTreeService.getCategoryTree();
        categoryTreeUpdateProcessor.updateCategories();
        CategoryTree newTree = categoryTreeService.getCategoryTree();
        assertNotSame(oldTree, newTree);

        assertThat(newTree.getRoot().getPlane(), hasSize(greaterThan(1001)));
    }
}
