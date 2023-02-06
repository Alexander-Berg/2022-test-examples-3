package ru.yandex.market.mbo.export.category;

import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.core.temp.TempFileService;
import ru.yandex.market.mbo.db.params.CategoryParametersExtractorService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.modelstorage.utils.SortedCategoriesIterableWrapper;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.protobuf.tools.MessageIterator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CategoryParametersOutputExtractorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CategoryParametersOutputExtractor outputExtractor;
    private CategoryParametersExtractorService extractorService;
    private TovarCategory tovarCategory1;
    private TovarCategory tovarCategory2;
    private List<TovarCategory> allCategories;
    private SortedCategoriesIterableWrapper categories;
    private TempFileService tempFileService;

    @Before
    public void setUp() throws IOException {
        String tmpDir = temporaryFolder.newFolder().toString();
        this.tempFileService = new TempFileService(tmpDir);
        this.extractorService = Mockito.mock(CategoryParametersExtractorService.class);
        this.outputExtractor = new CategoryParametersOutputExtractor(extractorService, tempFileService);

        this.tovarCategory1 = createTovarCategory(1);
        this.tovarCategory2 = createTovarCategory(2);
        this.allCategories = Arrays.asList(tovarCategory1, tovarCategory2);
    }

    @After
    public void tearDown() {
        if (categories != null) {
            categories.close();
        }
    }

    @Test(timeout = 3_000)
    public void testExtractCategoriesParameters() throws IOException {
        Mockito.when(this.extractorService.extractLeafCategory(Mockito.any()))
            .then(invocation -> {
                // небольшая задержка, чтобы немного проэмулировать настоящую работу по загрузке категорий
                Thread.sleep(10);
                TovarCategory tovarCategory = invocation.getArgument(0);
                return createCategory(tovarCategory.getHid());
            });

        categories = outputExtractor
            .extractCategoriesParameters(allCategories, Collections.emptyList());

        List<MboParameters.Category> expected = Arrays.asList(createCategory(1L), createCategory(2L));

        Assertions.assertThat(categories.size()).isEqualTo(2);
        Assertions.assertThat(categories.getCategoryIds()).containsExactlyInAnyOrder(1L, 2L);
        Assertions.assertThat(categories.getByCategoryId(1L)).isEqualTo(createCategory(1L));
        Assertions.assertThat(categories.getByCategoryId(2L)).isEqualTo(createCategory(2L));
        Assertions.assertThatThrownBy(() -> categories.getByCategoryId(3L))
            .hasMessageContaining("No category with id: 3");

        try (MessageIterator<MboParameters.Category> iterator = categories.iterator()) {
            ArrayList<MboParameters.Category> messageCategories = Lists.newArrayList(iterator);
            Assertions.assertThat(messageCategories).containsExactlyInAnyOrderElementsOf(expected);
        }

        Assertions.assertThat(categories)
            .containsExactlyElementsOf(expected);
    }

    /**
     * Тест эмулирует неравномерную загрузку категорий. А также проверяет, что категории загружаются параллельно.
     * <p>
     * Как тест проверяет паралелльную загрузку:
     * Пусть часть категорий загружаются долго - 3 секунды.
     * Если мы будем грузить их последовательно, то общее время работы будет 9 секунд.
     * Но на тесте стоит ограничение в 5 секунд.
     * Если тест завершается в указанный интервал, значит категории загружались параллельно.
     * <p>
     * Также тест проверяет порядок выходных категорий. Он должен быть строго по возрастанию hid.
     * НЕ УВЕЛИЧИВАЙТЕ общий таймаут, так как это аффектить общую длительность всех тестов.
     */
    @Test(timeout = 5_000)
    public void testCategoriesAreExtractedInHidOrderIfOddCategoriesAreSlow() {
        outputExtractor.setBufferSize(5);
        outputExtractor.setThreadsNumber(5);
        Mockito.when(this.extractorService.extractLeafCategory(Mockito.any()))
            .then(invocation -> {
                Thread.sleep(10);
                TovarCategory tovarCategory = invocation.getArgument(0);
                if (tovarCategory.getHid() % 2 == 1) {
                    Thread.sleep(3_000);
                }
                return createCategory(tovarCategory.getHid());
            });
        TovarCategory tovarCategory3 = createTovarCategory(3);
        TovarCategory tovarCategory4 = createTovarCategory(4);
        TovarCategory tovarCategory5 = createTovarCategory(5);

        categories = outputExtractor
            .extractCategoriesParameters(
                Arrays.asList(tovarCategory1, tovarCategory2, tovarCategory3, tovarCategory4, tovarCategory5),
                Collections.emptyList()
            );

        Assertions.assertThat(categories)
            .containsExactly(
                createCategory(1L),
                createCategory(2L),
                createCategory(3L),
                createCategory(4L),
                createCategory(5L)
            );
    }

    /**
     * Тест аналогичен предыдущему, только категории другие выбирались.
     */
    @Test(timeout = 5_000)
    public void testCategoriesAreExtractedInHidOrderIfEvenCategoriesAreSlow() {
        Mockito.when(this.extractorService.extractLeafCategory(Mockito.any()))
            .then(invocation -> {
                Thread.sleep(10);
                TovarCategory tovarCategory = invocation.getArgument(0);
                // Эмулируем неравномерную загрузку категорий
                // Четные категории пусть загружаются "долго".
                if (tovarCategory.getHid() % 2 == 0) {
                    Thread.sleep(3_000);
                }
                return createCategory(tovarCategory.getHid());
            });
        TovarCategory tovarCategory3 = createTovarCategory(3);
        TovarCategory tovarCategory4 = createTovarCategory(4);
        TovarCategory tovarCategory5 = createTovarCategory(5);

        categories = outputExtractor
            .extractCategoriesParameters(
                Arrays.asList(tovarCategory1, tovarCategory2, tovarCategory3, tovarCategory4, tovarCategory5),
                Collections.emptyList()
            );

        Assertions.assertThat(categories)
            .containsExactly(
                createCategory(1L),
                createCategory(2L),
                createCategory(3L),
                createCategory(4L),
                createCategory(5L)
            );
    }

    @Test(timeout = 5_000)
    public void loadThousandCategories() {
        Mockito.when(this.extractorService.extractLeafCategory(Mockito.any()))
            .then(invocation -> {
                Thread.sleep(10);
                TovarCategory tovarCategory = invocation.getArgument(0);
                return createCategory(tovarCategory.getHid());
            });

        List<TovarCategory> input = IntStream.range(1, 1001)
            .mapToObj(this::createTovarCategory)
            .collect(Collectors.toList());

        categories = outputExtractor
            .extractCategoriesParameters(input, Collections.emptyList());
        Assertions.assertThat(categories)
            .extracting(MboParameters.Category::getHid)
            .isSorted()
            .hasSize(1000);
    }

    @Test(timeout = 3_000)
    public void testIfCategoryFailedToLoad() {
        Mockito.when(this.extractorService.extractLeafCategory(Mockito.any()))
            .then(invocation -> {
                Thread.sleep(10);
                TovarCategory tovarCategory = invocation.getArgument(0);
                if (tovarCategory.getHid() == 2) {
                    throw new RuntimeException("Failed to load category " + tovarCategory.getHid());
                }
                return createCategory(tovarCategory.getHid());
            });

        List<TovarCategory> input = IntStream.range(1, 101)
            .mapToObj(this::createTovarCategory)
            .collect(Collectors.toList());

        Assertions.assertThatThrownBy(() ->
            outputExtractor.extractCategoriesParameters(input, Collections.emptyList())
        ).hasMessageContaining("Failed to load category 2");
    }

    @Test
    public void testCategoryDiscardedByPostProcessor() throws IOException {
        Mockito.when(this.extractorService.extractLeafCategory(Mockito.any())).then(invocation -> {
            TovarCategory tovarCategory = invocation.getArgument(0);
            return createCategory(tovarCategory.getHid());
        });

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        outputExtractor.extractCategoriesParameters(
            Arrays.asList(tovarCategory1, tovarCategory2),
            Collections.emptyList(),
            result,
            // пусть постпроцессор исключит категорию 1, но оставит 2.
            category -> Objects.equals(category.getHid(), tovarCategory1.getHid())
                ? Optional.empty() : Optional.of(category)
        );

        byte[] bytes = result.toByteArray();
        ByteArrayInputStream output = new ByteArrayInputStream(bytes);
        MboParameters.Category resultCategory = MboParameters.Category.parseDelimitedFrom(output);
        Assertions.assertThat(resultCategory.getHid()).isEqualTo(tovarCategory2.getHid());
    }

    @Test(timeout = 10_000)
    public void testGlobalTimeout() {
        // Пусть категория зависнет в обработке
        Mockito.when(this.extractorService.extractLeafCategory(Mockito.any())).then(invocation -> {
            Thread.sleep(10_000);
            TovarCategory tovarCategory = invocation.getArgument(0);
            return createCategory(tovarCategory.getHid());
        });

        // Зададим глобальный таймаут на всю обработку таким, чтобы он был меньше времени обработки.
        outputExtractor.setGlobalTimeoutSec(3);
        List<TovarCategory> input = IntStream.range(1, 5)
            .mapToObj(this::createTovarCategory)
            .collect(Collectors.toList());

        Assertions.assertThatThrownBy(() ->
            outputExtractor.extractCategoriesParameters(input, Collections.emptyList())
        ).hasMessageContaining("Extraction took too long");
    }

    private TovarCategory createTovarCategory(long categoryId) {
        TovarCategory tovarCategory = new TovarCategory();
        tovarCategory.setHid(categoryId);
        return tovarCategory;
    }

    private MboParameters.Category createCategory(long categoryId) {
        MboParameters.Word name = MboParameters.Word.newBuilder()
            .setLangId(Language.RUSSIAN.getId()).setName("Category " + categoryId).build();
        MboParameters.Word unique = MboParameters.Word.newBuilder()
            .setLangId(Language.RUSSIAN.getId()).setName("Category unique " + categoryId).build();
        return MboParameters.Category.newBuilder()
            .setHid(categoryId)
            .addName(name)
            .addUniqueName(unique)
            .build();
    }
}
