package ru.yandex.market.mboc.tms.executors;

import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.tms.executors.category.YtCategoriesReader;
import ru.yandex.market.mboc.tms.service.YtStuffSessionReaderMock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author moskovkin@yandex-team.ru
 * @created 23.07.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RefreshCategoriesExecutorTest extends BaseDbTestClass {
    private static final int SEED = 33;
    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED).build();

    private static final int TEST_DATA_COUNT = 100;

    private RefreshCategoriesExecutor refreshCategoriesExecutor;

    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CategoryRepository categoryRepository;
    private StorageKeyValueServiceMock storageKeyValueServiceMock;
    private YtStuffSessionReaderMock ytStuffSessionReaderMock;

    @Before
    public void setup() {
        storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        ytStuffSessionReaderMock = new YtStuffSessionReaderMock();

        refreshCategoriesExecutor = Mockito.spy(
            new RefreshCategoriesExecutor(
                transactionHelper,
                storageKeyValueServiceMock,
                ytStuffSessionReaderMock,
                jdbcTemplate,
                Mockito.mock(YtCategoriesReader.class)
            )
        );
    }

    @Test
    public void testGtinRequiredParamsNotOverwriteByExecutor() {
        List<Category> oldData = randomCategories();
        categoryRepository.insertBatch(oldData);
        List<Category> newData = randomCategories();

        Category categoryWithRequiredGtin1p = newData.get(0);
        categoryWithRequiredGtin1p.setGtinRequired1p(true);
        Category categoryWithRequiredGtin3p = newData.get(1);
        categoryWithRequiredGtin3p.setGtinRequired3p(true);

        refreshCategoriesExecutor.persist(newData);

        Category categoryWithRequired1pGtinFromRepository =
            categoryRepository.findById(categoryWithRequiredGtin1p.getCategoryId());
        Category categoryWithRequired3pGtinFromRepository =
            categoryRepository.findById(categoryWithRequiredGtin3p.getCategoryId());

        Assertions.assertThat(categoryWithRequired1pGtinFromRepository.isGtinRequired1p()).isEqualTo(false);
        Assertions.assertThat(categoryWithRequired3pGtinFromRepository.isGtinRequired3p()).isEqualTo(false);
    }

    @Test
    public void testReplaceCategories() {
        List<Category> oldData = randomCategories();
        categoryRepository.insertBatch(oldData);
        List<Category> newData = randomCategories();
        refreshCategoriesExecutor.persist(newData);

        List<Category> repositoryData = categoryRepository.findAll();

        assertThat(repositoryData)
            .usingElementComparatorIgnoringFields(
                "id",
                //TODO: import flag from MBO in non-MVP https://st.yandex-team.ru/MCPROJECT-849
                "allowFastSkuCreation",
                //TODO: 20.07.2022 import flag from MBO in non-MVP https://st.yandex-team.ru/MCPROJECT-1236
                "gtinRequired1p",
                //TODO: 20.07.2022 import flag from MBO in non-MVP https://st.yandex-team.ru/MCPROJECT-1236
                "gtinRequired3p"
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
        ytStuffSessionReaderMock.setLastSessionId("1");
        refreshCategoriesExecutor.execute();

        Mockito.verify(refreshCategoriesExecutor, Mockito.times(1)).persist(Mockito.anyList());
    }

    @Test
    public void testLastSessionNotChanged() {
        ytStuffSessionReaderMock.setLastSessionId("1");
        refreshCategoriesExecutor.execute();
        Mockito.verify(refreshCategoriesExecutor, Mockito.times(1)).persist(Mockito.anyList());

        refreshCategoriesExecutor.execute();
        Mockito.verify(refreshCategoriesExecutor, Mockito.times(1)).persist(Mockito.anyList());
    }

    @Test
    public void testLastSessionChanged() {
        ytStuffSessionReaderMock.setLastSessionId("1");
        refreshCategoriesExecutor.execute();
        Mockito.verify(refreshCategoriesExecutor, Mockito.times(1)).persist(Mockito.anyList());

        ytStuffSessionReaderMock.setLastSessionId("2");
        refreshCategoriesExecutor.execute();
        Mockito.verify(refreshCategoriesExecutor, Mockito.times(2)).persist(Mockito.anyList());
    }
}
