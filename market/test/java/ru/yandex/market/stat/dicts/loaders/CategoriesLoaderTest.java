package ru.yandex.market.stat.dicts.loaders;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import com.google.common.collect.Lists;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import ru.yandex.market.stat.dicts.records.CategoryDictionaryRecord;
import ru.yandex.market.stats.test.data.TestDataResolver;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class CategoriesLoaderTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    @Mock
    private MdgPath categoryPath;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testLoadWithSandboxData() throws IOException {
        when(categoryPath.createInputStream())
                .thenReturn(new GZIPInputStream(TestDataResolver.getResource("sandbox:tovar-tree.pb.gz")));

        final CategoriesLoader categoriesLoader = new CategoriesLoader(null, categoryPath);
        final DictionaryLoadIterator<CategoryDictionaryRecord> recordsIterator = categoriesLoader.iterator(DEFAULT_CLUSTER, null);

        final List<CategoryDictionaryRecord> records = Lists.newArrayList(recordsIterator);
        assertThat(records, hasSize(4819));

        final List<Long> hyperIds = asList(90437L, 6296079L, 90401L, 11749453L, 91013L);
        final List<CategoryDictionaryRecord> actualRecords = records.stream()
                                                                    .filter(r -> hyperIds.contains(r.getHyper_id()))
                                                                    .sorted(sortRecordsById())
                                                                    .collect(toList());
        assertEquals(getExpectedRecords(), actualRecords);
    }

    private List<CategoryDictionaryRecord> getExpectedRecords() {
        final CategoryDictionaryRecord expected0 = CategoryDictionaryRecord.builder()
                                                                           .id(0L)
                                                                           .parent_id(null)
                                                                           .hyper_id(90401L)
                                                                           .parent_hyper_id(null)
                                                                           .parent_hyper_ids(emptyList())
                                                                           .name("Все товары")
                                                                           .en_name("All products")
                                                                           .parents_names(emptyList())
                                                                           .parents_en_names(emptyList())
                                                                           .children(asList(
                                                                                   1L, 2L, 4L, 5L, 6L, 7L, 8L, 10L, 12L, 13L, 14L, 15L, 16L, 17L,
                                                                                   19L, 21L, 22L, 1523L, 1524L, 21387L, 22067L, 22987L, 1000000L
                                                                           ))
                                                                           .parents(emptyList())
                                                                           .hierarchy_hyper_ids(singletonList(90401L))
                                                                           .hierarchy_names(singletonList("Все товары"))
                                                                           .not_used(false)
                                                                           .no_search(true)
                                                                           .grouped(false)
                                                                           .build();

        final CategoryDictionaryRecord expected1 = CategoryDictionaryRecord.builder()
                                                                           .id(18825L)
                                                                           .parent_id(17885L)
                                                                           .hyper_id(6296079L)
                                                                           .parent_hyper_id(6091783L)
                                                                           .parent_hyper_ids(asList(90401L, 90509L, 6091783L))
                                                                           .name("Симуляторы для мужчин")
                                                                           .en_name("Simulators for men")
                                                                           .parents_names(asList("Все товары", "Красота и здоровье", "Интим-товары"))
                                                                           .parents_en_names(asList(
                                                                                   "All products", "Heath products", "Intimate goods"
                                                                           ))
                                                                           .children(emptyList())
                                                                           .parents(asList(0L, 2L, 17885L))
                                                                           .hierarchy_hyper_ids(asList(6296079L, 6091783L, 90509L, 90401L))
                                                                           .hierarchy_names(asList(
                                                                                   "Симуляторы для мужчин", "Интим-товары",
                                                                                   "Красота и здоровье", "Все товары"
                                                                           ))
                                                                           .not_used(false)
                                                                           .no_search(true)
                                                                           .grouped(false)
                                                                           .build();

        final CategoryDictionaryRecord expected2 = CategoryDictionaryRecord.builder()
                                                                           .id(24588L)
                                                                           .parent_id(22078L)
                                                                           .hyper_id(11749453L)
                                                                           .parent_hyper_id(8439695L)
                                                                           .parent_hyper_ids(asList(90401L, 8439678L, 8439680L, 8439695L))
                                                                           .name("Умные часы и браслеты")
                                                                           .en_name("Smartwatches and bracelets")
                                                                           .parents_names(asList(
                                                                                   "Все товары", "Мобильный каталог", "Электроника и компьютеры",
                                                                                   "Аксессуары и запчасти для телефонов"
                                                                           ))
                                                                           .parents_en_names(asList(
                                                                                   "All products", "Mobile directory", "Electronics and computers",
                                                                                   "Spare parts and accessories for phones"
                                                                           ))
                                                                           .children(emptyList())
                                                                           .parents(asList(0L, 22067L, 22068L, 22078L))
                                                                           .hierarchy_hyper_ids(asList(
                                                                                   11749453L, 8439695L, 8439680L, 8439678L, 90401L
                                                                           ))
                                                                           .hierarchy_names(asList(
                                                                                   "Умные часы и браслеты", "Аксессуары и запчасти для телефонов",
                                                                                   "Электроника и компьютеры", "Мобильный каталог", "Все товары"
                                                                           ))
                                                                           .not_used(true)
                                                                           .no_search(true)
                                                                           .grouped(false)
                                                                           .build();

        final CategoryDictionaryRecord expected3 = CategoryDictionaryRecord.builder()
                                                                           .id(55L)
                                                                           .parent_id(23567L)
                                                                           .hyper_id(91013L)
                                                                           .parent_hyper_id(10604359L)
                                                                           .parent_hyper_ids(asList(90401L, 91009L, 10604359L))
                                                                           .name("Ноутбуки")
                                                                           .en_name("Laptops")
                                                                           .parents_names(asList("Все товары", "Компьютерная техника", "Компьютеры"))
                                                                           .parents_en_names(asList("All products", "Computers", "Computers"))
                                                                           .children(emptyList())
                                                                           .parents(asList(0L, 10L, 23567L))
                                                                           .hierarchy_hyper_ids(asList(91013L, 10604359L, 91009L, 90401L))
                                                                           .hierarchy_names(asList(
                                                                                   "Ноутбуки", "Компьютеры", "Компьютерная техника", "Все товары"
                                                                           ))
                                                                           .not_used(false)
                                                                           .no_search(false)
                                                                           .grouped(true)
                                                                           .build();

        final CategoryDictionaryRecord expected4 = CategoryDictionaryRecord.builder()
                                                                           .id(659L)
                                                                           .parent_id(642L)
                                                                           .hyper_id(90437L)
                                                                           .parent_hyper_id(90435L)
                                                                           .parent_hyper_ids(asList(90401L, 90402L, 90435L))
                                                                           .name("Двигатель")
                                                                           .en_name("[NO TR] Двигатель")
                                                                           .parents_names(asList("Все товары", "Авто", "Запчасти"))
                                                                           .parents_en_names(asList("All products", "Auto", "Spare Parts"))
                                                                           .children(emptyList())
                                                                           .parents(asList(0L, 1L, 642L))
                                                                           .hierarchy_hyper_ids(asList(90437L, 90435L, 90402L, 90401L))
                                                                           .hierarchy_names(asList("Двигатель", "Запчасти", "Авто", "Все товары"))
                                                                           .not_used(false)
                                                                           .no_search(false)
                                                                           .grouped(false)
                                                                           .build();

        return Stream.of(expected0, expected1, expected2, expected3, expected4)
                     .sorted(sortRecordsById())
                     .collect(toList());
    }

    private Comparator<CategoryDictionaryRecord> sortRecordsById() {
        return comparing(CategoryDictionaryRecord::getId);
    }
}
