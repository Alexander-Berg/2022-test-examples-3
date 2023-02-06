package ru.yandex.market.deepmind.tms.executors.category;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

/**
 * @author moskovkin@yandex-team.ru
 * @created 23.07.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ImportCategoriesExecutorTest extends DeepmindBaseDbTestClass {
    private static final int SEED = 33;
    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED).build();

    private static final int TEST_DATA_COUNT = 100;

    private ImportCategoriesExecutor importCategoriesExecutor;

    @Resource(name = "deepmindTransactionHelper")
    private TransactionHelper transactionHelper;
    @Resource(name = "deepmindSqlJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DeepmindCategoryRepository deepmindCategoryRepository;
    private StorageKeyValueServiceMock storageKeyValueServiceMock;
    private DeepmindYtStuffSessionReaderMock deepmindYtStuffSessionReaderMock;

    @Before
    public void setup() {
        storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        deepmindYtStuffSessionReaderMock = new DeepmindYtStuffSessionReaderMock();

        importCategoriesExecutor = Mockito.spy(
            new ImportCategoriesExecutor(
                transactionHelper,
                storageKeyValueServiceMock,
                deepmindYtStuffSessionReaderMock,
                jdbcTemplate,
                Mockito.mock(DeepmindYtCategoriesReader.class)
            )
        );
    }

    @Test
    public void testReplaceCategories() {
        List<Category> oldData = randomCategories();
        deepmindCategoryRepository.insertBatch(oldData);
        List<Category> newData = randomCategories();
        importCategoriesExecutor.persist(newData);

        List<Category> repositoryData = deepmindCategoryRepository.findAll();

        Assertions.assertThat(repositoryData)
            .usingElementComparatorIgnoringFields(
                "id",
                //TODO: import flag from MBO in non-MVP https://st.yandex-team.ru/MCPROJECT-849
                "allowFastSkuCreation"
            )
            .hasSameElementsAs(newData);
    }

    private List<Category> randomCategories() {
        return RANDOM.objects(Category.class, TEST_DATA_COUNT)
            .map(it -> it.setParameterValues(List.of()))
            .collect(Collectors.toList());
    }

    @Test
    public void testLastImportedStuffEmpty() {
        deepmindYtStuffSessionReaderMock.setLastSessionId("1");
        importCategoriesExecutor.execute();

        Mockito.verify(importCategoriesExecutor, Mockito.times(1)).persist(Mockito.anyList());
    }

    @Test
    public void testLastSessionNotChanged() {
        deepmindYtStuffSessionReaderMock.setLastSessionId("1");
        importCategoriesExecutor.execute();
        Mockito.verify(importCategoriesExecutor, Mockito.times(1)).persist(Mockito.anyList());

        importCategoriesExecutor.execute();
        Mockito.verify(importCategoriesExecutor, Mockito.times(1)).persist(Mockito.anyList());
    }

    @Test
    public void testLastSessionChanged() {
        deepmindYtStuffSessionReaderMock.setLastSessionId("1");
        importCategoriesExecutor.execute();
        Mockito.verify(importCategoriesExecutor, Mockito.times(1)).persist(Mockito.anyList());

        deepmindYtStuffSessionReaderMock.setLastSessionId("2");
        importCategoriesExecutor.execute();
        Mockito.verify(importCategoriesExecutor, Mockito.times(2)).persist(Mockito.anyList());
    }
}
