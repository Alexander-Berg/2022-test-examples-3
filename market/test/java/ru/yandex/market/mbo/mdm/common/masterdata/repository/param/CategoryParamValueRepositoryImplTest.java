package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue.Key;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.filter.CategorySearchFilter;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryParamValueRepositoryImplTest extends MdmBaseDbTestClass {

    @Autowired
    private CategoryParamValueRepository repository;
    private EnhancedRandom random;

    @Before
    public void setup() {
        repository.deleteAll();
        random = TestDataUtils.defaultRandom(1280);
    }

    @Test
    public void testSimpleInsert() {
        CategoryParamValue record = record();
        repository.insert(record);
        CategoryParamValue found = repository.findById(record.getKey());
        assertThat(record).isEqualTo(found);
    }

    @Test
    public void testMultipleInsert() {
        List<CategoryParamValue> records = List.of(record(), record(), record());
        repository.insertBatch(records);
        List<CategoryParamValue> found = repository.findByIds(keys(records));
        assertThat(found).containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testMultipleInsert2() {
        List<CategoryParamValue> records = List.of(record(), record(), record());
        repository.insertOrUpdateAll(records);
        List<CategoryParamValue> found = repository.findByIds(keys(records));
        assertThat(found).containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testSimpleDelete() {
        CategoryParamValue record = record();
        repository.insert(record);
        repository.delete(record);
        assertThat(repository.totalCount()).isZero();
    }

    @Test
    public void testMultipleDelete() {
        List<CategoryParamValue> records = List.of(record(), record(), record());
        repository.insertBatch(records);
        repository.delete(keys(records));
        assertThat(repository.totalCount()).isZero();
    }

    @Test
    public void testSimpleUpdate() {
        CategoryParamValue record = record();
        record.setStrings(List.of("Творобушек"));
        record.setProcessed(false);
        repository.insertOrUpdate(record);

        record.setStrings(List.of("Бульонные параллелепипеды"));
        record.setProcessed(true);
        repository.insertOrUpdate(record);

        assertThat(repository.findById(record.getKey())).isEqualTo(record);
    }

    @Test
    public void testMultipleUpdate() {
        List<CategoryParamValue> records = List.of(record(), record(), record());
        foreachEnumerated(records, (i, record) -> {
            record.setStrings(List.of("Творобушек"));
            record.setProcessed(false);
        });
        repository.insertOrUpdateAll(records);

        foreachEnumerated(records, (i, record) -> {
            record.setStrings(List.of("Бульонные параллелепипеды"));
            record.setProcessed(true);
        });
        repository.insertOrUpdateAll(records);

        assertThat(repository.findAll()).containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testInsertAndUpdateInOneGo() {
        List<CategoryParamValue> records = List.of(record(), record(), record(), record());
        foreachEnumerated(records, (i, record) -> {
            record.setStrings(List.of("Творобушек"));
            record.setProcessed(false);
        });
        repository.insertOrUpdateAll(records.stream().limit(2).collect(Collectors.toList()));

        foreachEnumerated(records, (i, record) -> {
            record.setStrings(List.of("Бульонные параллелепипеды"));
            record.setProcessed(true);
        });
        repository.insertOrUpdateAll(records);

        assertThat(repository.findAll()).containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testFindNotProcessed() {
        List<CategoryParamValue> records = List.of(record(), record(), record(), record());
        List<CategoryParamValue> processed = records.stream().limit(2).collect(Collectors.toList());
        List<CategoryParamValue> unprocessed = records.stream().skip(2).collect(Collectors.toList());

        processed.forEach(r -> r.setProcessed(true));
        unprocessed.forEach(r -> r.setProcessed(false));
        repository.insertOrUpdateAll(records);

        repository.findNotProcessed(100, foundNotProcessed -> {
            assertThat(foundNotProcessed).containsExactlyInAnyOrderElementsOf(unprocessed);
        });

        MutableInt timesExecuted = new MutableInt(0);
        Set<CategoryParamValue> found = new HashSet<>();
        repository.findNotProcessed(1, foundNotProcessed -> {
            assertThat(unprocessed.containsAll(foundNotProcessed)).isTrue();
            timesExecuted.increment();
            found.addAll(foundNotProcessed);
        });
        assertThat(timesExecuted.getValue()).isEqualTo(2);
        assertThat(unprocessed).containsExactlyInAnyOrderElementsOf(found);
    }

    @Test
    public void testEmptyEntitiesDecodedEncodedProperly() {
        CategoryParamValue record = record();
        record.setOptions(List.of());

        repository.insertOrUpdate(record);

        CategoryParamValue found = repository.findById(record.getKey());
        assertThat(found).isEqualTo(record);
    }

    @Test
    public void testFindByCategoryId() {
        long relevantCategoryId = 10;
        CategoryParamValue relevantParam1 = record().setCategoryId(relevantCategoryId);
        CategoryParamValue relevantParam2 = record().setCategoryId(relevantCategoryId);
        CategoryParamValue relevantParam3 = record().setCategoryId(relevantCategoryId);
        CategoryParamValue irrelevantParam1 = record().setCategoryId(relevantCategoryId + 1);
        CategoryParamValue irrelevantParam2 = record().setCategoryId(relevantCategoryId + 1);

        repository.insertBatch(irrelevantParam1, irrelevantParam2, relevantParam1, relevantParam2, relevantParam3);

        List<CategoryParamValue> found = repository.findCategoryParamValues(relevantCategoryId);
        assertThat(found).containsExactlyInAnyOrder(relevantParam1, relevantParam2, relevantParam3);
    }

    @Test
    public void testFindCategoryIdByFilter() {
        // given
        long relevantCategoryId = 11;
        CategoryParamValue relevantParam1 = record().setCategoryId(relevantCategoryId);
        CategoryParamValue irrelevantParam1 = record().setCategoryId(relevantCategoryId + 1);
        repository.insertBatch(relevantParam1, irrelevantParam1);
        var filter = CategorySearchFilter.forOptionsIds(relevantParam1.getMdmParamId(),
            CategorySearchFilter.SearchCondition.EQ,
            relevantParam1.getOptions().stream().map(MdmParamOption::getId).collect(Collectors.toList()));

        // when
        List<Long> found = repository.findCategoryIdsByFilter(filter);

        // then
        assertThat(found).containsExactlyInAnyOrder(relevantParam1.getCategoryId());
    }

    private CategoryParamValue record() {
        return random.nextObject(CategoryParamValue.class);
    }

    private List<Key> keys(List<CategoryParamValue> records) {
        return records.stream().map(CategoryParamValue::getKey).collect(Collectors.toList());
    }

    private void foreachEnumerated(Iterable<CategoryParamValue> records,
                                   BiConsumer<Integer, CategoryParamValue> action) {
        var iter = records.iterator();
        int count = 0;
        while (iter.hasNext()) {
            action.accept(count++, iter.next());
        }
    }
}
