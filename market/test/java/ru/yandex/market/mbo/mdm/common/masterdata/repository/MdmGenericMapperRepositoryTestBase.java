package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.lightmapper.GenericMapperRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

/**
 * Базовый тест для репозиториев, проверяющий, что ликвибейз-схема и маппер описаны верно.
 * Чтобы написать свой тест, отнаследуйтесь от класса и реализуйте методы:
 * <p>
 * - {@link MdmGenericMapperRepositoryTestBase#randomRecord}
 * - {@link MdmGenericMapperRepositoryTestBase#getIdSupplier}
 * - {@link MdmGenericMapperRepositoryTestBase#getUpdaters}
 * <p>
 * Если ID автогенерируется, также нужно возвращать true в {@link MdmGenericMapperRepositoryTestBase#isIdGenerated}.
 * Также можно настраивать сравнение элементов, переопределяя методы
 * {@link MdmGenericMapperRepositoryTestBase#getFieldsToIgnore} и
 * {@link MdmGenericMapperRepositoryTestBase#getCustomComparatorForFields}
 *
 * @author dmserebr
 * @date 24/08/2020
 */
public abstract class MdmGenericMapperRepositoryTestBase<R extends GenericMapperRepository<T, K>, T, K>
    extends MdmBaseDbTestClass {

    @Autowired
    protected R repository;
    protected EnhancedRandom random;

    protected abstract T randomRecord();

    protected abstract Function<T, K> getIdSupplier();

    protected abstract List<BiConsumer<Integer, T>> getUpdaters();

    protected String[] getFieldsToIgnore() {
        return new String[0];
    }

    protected Pair<Comparator<?>, String[]> getCustomComparatorForFields() {
        return Pair.of(Comparator.naturalOrder(), new String[]{""});
    }

    protected boolean isIdGenerated() {
        return false;
    }

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(12312);
    }

    @Test
    public void testSimpleInsert() {
        T record = randomRecord();
        K key = getIdSupplier().apply(record);
        repository.insert(record);

        if (isIdGenerated()) {
            key = getIdSupplier().apply(record);
        }
        T found = repository.findById(key);
        Assertions.assertThat(record)
            .usingComparatorForFields(
                getCustomComparatorForFields().getLeft(), getCustomComparatorForFields().getRight())
            .isEqualToIgnoringGivenFields(found, getFieldsToIgnore());
    }

    @Test
    public void testMultipleInsert() {
        List<T> records = List.of(randomRecord(), randomRecord(), randomRecord());
        List<K> keys = keys(records);
        repository.insertBatch(records);

        if (isIdGenerated()) {
            keys = keys(records);
        }
        List<T> found = repository.findByIds(keys);
        Assertions.assertThat(found)
            .usingComparatorForElementFieldsWithNames(
                getCustomComparatorForFields().getLeft(), getCustomComparatorForFields().getRight())
            .usingElementComparatorIgnoringFields(getFieldsToIgnore())
            .containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testMultipleInsert2() {
        List<T> records = List.of(randomRecord(), randomRecord(), randomRecord());
        List<K> keys = keys(records);
        repository.insertOrUpdateAll(records);

        if (isIdGenerated()) {
            keys = keys(records);
        }
        List<T> found = repository.findByIds(keys);
        Assertions.assertThat(found)
            .usingComparatorForElementFieldsWithNames(
                getCustomComparatorForFields().getLeft(), getCustomComparatorForFields().getRight())
            .usingElementComparatorIgnoringFields(getFieldsToIgnore())
            .containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testSimpleUpdate() {
        T record = randomRecord();
        K key = getIdSupplier().apply(record);
        getUpdaters().get(0).accept(0, record);
        repository.insertOrUpdate(record);

        getUpdaters().get(1).accept(0, record);
        repository.insertOrUpdate(record);

        if (isIdGenerated()) {
            key = getIdSupplier().apply(record);
        }
        Assertions.assertThat(repository.findById(key))
            .usingComparatorForFields(
                getCustomComparatorForFields().getLeft(), getCustomComparatorForFields().getRight())
            .isEqualToIgnoringGivenFields(record, getFieldsToIgnore());
    }

    @Test
    public void testMultipleUpdate() {
        List<T> records = List.of(randomRecord(), randomRecord(), randomRecord());
        List<K> keys = records.stream().map(getIdSupplier()).collect(Collectors.toList());

        var updaterIndex = new MutableInt(0);
        int updatersCount = getUpdaters().size();
        foreachEnumerated(records, (i, record) -> {
            getUpdaters().get(updaterIndex.getAndIncrement() % updatersCount).accept(i, record);
        });
        repository.insertOrUpdateAll(records);

        foreachEnumerated(records, (i, record) -> {
            getUpdaters().get(updaterIndex.getAndIncrement() % updatersCount).accept(i, record);
        });
        repository.insertOrUpdateAll(records);

        if (isIdGenerated()) {
            keys = keys(records);
        }
        Assertions.assertThat(repository.findByIds(keys))
            .usingComparatorForElementFieldsWithNames(
                getCustomComparatorForFields().getLeft(), getCustomComparatorForFields().getRight())
            .usingElementComparatorIgnoringFields(getFieldsToIgnore())
            .containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testInsertAndUpdateInOneGo() {
        List<T> records = List.of(randomRecord(), randomRecord(), randomRecord(), randomRecord());
        List<K> keys = records.stream().map(getIdSupplier()).collect(Collectors.toList());

        var updaterIndex = new MutableInt(0);
        int updatersCount = getUpdaters().size();
        foreachEnumerated(records, (i, record) -> {
            getUpdaters().get(updaterIndex.getAndIncrement() % updatersCount).accept(i, record);
        });
        repository.insertOrUpdateAll(records.stream().limit(2).collect(Collectors.toList()));

        foreachEnumerated(records, (i, record) -> {
            getUpdaters().get(updaterIndex.getAndIncrement() % updatersCount).accept(i, record);
        });
        repository.insertOrUpdateAll(records);

        if (isIdGenerated()) {
            keys = keys(records);
        }
        Assertions.assertThat(repository.findByIds(keys))
            .usingComparatorForElementFieldsWithNames(
                getCustomComparatorForFields().getLeft(), getCustomComparatorForFields().getRight())
            .usingElementComparatorIgnoringFields(getFieldsToIgnore())
            .containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testSimpleDelete() {
        int beforeCount = repository.totalCount();
        T record = randomRecord();
        repository.insert(record);
        repository.delete(record);
        Assertions.assertThat(repository.totalCount()).isEqualTo(beforeCount);
    }

    @Test
    public void testMultipleDelete() {
        int beforeCount = repository.totalCount();
        List<T> records = List.of(randomRecord(), randomRecord(), randomRecord());
        List<K> keys = keys(records);

        repository.insertBatch(records);
        if (isIdGenerated()) {
            keys = keys(records);
        }
        repository.delete(keys);
        Assertions.assertThat(repository.totalCount()).isEqualTo(beforeCount);
    }

    protected List<K> keys(Collection<T> records) {
        return records.stream().map(getIdSupplier()).collect(Collectors.toList());
    }

    protected void foreachEnumerated(Iterable<T> records,
                                     BiConsumer<Integer, T> action) {
        var iter = records.iterator();
        int count = 0;
        while (iter.hasNext()) {
            action.accept(count++, iter.next());
        }
    }
}
