package ru.yandex.market.mboc.common;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.mboc.common.config.TestSqlDatasourceConfig;
import ru.yandex.market.mboc.common.config.TestYqlAutoClusterConfig;
import ru.yandex.market.mboc.common.config.TestYqlOverPgDatasourceConfig;
import ru.yandex.market.mboc.common.config.TestYtConfig;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.datacamp.SendDataCampOfferStatesService;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.mboc.common.ydb.TestYdbMockConfig;

/**
 * Класс с "правильной" общей шапкой аннотаций для интеграционных тестов,
 * чтобы создавался и переиспользовался один контекст.
 *
 * @author yuramalinov
 * @created 16.04.18
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = {IntegrationTestSourcesInitializer.class, PGaaSZonkyInitializer.class})
@SpringBootTest(
    properties = "spring.profiles.active=test",
    classes = {
        CommonConfiguration.class,
        DbIntegrationTestConfiguration.class,
        TestSqlDatasourceConfig.class,
        TestYqlOverPgDatasourceConfig.class,
        TestYqlAutoClusterConfig.class,
        TestYtConfig.class,
        TestYdbMockConfig.class,
    }
)
@MockBean({
    LMSClient.class,
    SendDataCampOfferStatesService.class
})
@Transactional
public abstract class BaseIntegrationTestClass {
    @Autowired
    private CategoryRepository categoryRepository;

    @Before
    public void initCategoryTree() {
        categoryRepository.insert(
            new Category()
                .setCategoryId(CategoryTree.ROOT_CATEGORY_ID)
                .setName("root")
                .setParentCategoryId(CategoryTree.NO_ROOT_ID)
                .setPublished(true)
        );
    }
}
