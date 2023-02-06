package ru.yandex.market.mbo.mdm.common.masterdata.repository.warehouse;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.warehouse.MdmWarehouse;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class MdmWarehouseRepositoryTest extends MdmBaseDbTestClass {

    @Autowired
    private MdmWarehouseRepository repository;
    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(1280);
    }

    @Test
    public void testSimpleInsert() {
        MdmWarehouse record = record();
        repository.insert(record);
        MdmWarehouse found = repository.findById(record.getId());
        assertThat(record).isEqualTo(found);
    }

    @Test
    public void testMultipleInsert() {
        List<MdmWarehouse> records = List.of(record(), record(), record());
        repository.insertBatch(records);
        List<MdmWarehouse> found = repository.findByIds(keys(records));
        assertThat(found).containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testMultipleInsert2() {
        List<MdmWarehouse> records = List.of(record(), record(), record());
        repository.insertOrUpdateAll(records);
        List<MdmWarehouse> found = repository.findByIds(keys(records));
        assertThat(found).containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testSimpleDelete() {
        MdmWarehouse record = record();
        repository.insert(record);
        repository.delete(record);
        assertThat(repository.totalCount()).isZero();
    }

    @Test
    public void testMultipleDelete() {
        List<MdmWarehouse> records = List.of(record(), record(), record());
        repository.insertBatch(records);
        repository.delete(keys(records));
        assertThat(repository.totalCount()).isZero();
    }

    @Test
    public void testSimpleUpdate() {
        MdmWarehouse record = record();
        record.setCargotypes(List.of(12, 14));
        repository.insertOrUpdate(record);

        record.setCargotypes(List.of(300, 400));
        repository.insertOrUpdate(record);

        assertThat(repository.findById(record.getId())).isEqualTo(record);
    }

    @Test
    public void testMultipleUpdate() {
        List<MdmWarehouse> records = List.of(record(), record(), record());
        foreachEnumerated(records, (i, record) -> {
            record.setName("Творобушек");
        });
        repository.insertOrUpdateAll(records);

        foreachEnumerated(records, (i, record) -> {
            record.setName("Бульонные параллелепипеды");
        });
        repository.insertOrUpdateAll(records);

        assertThat(repository.findAll()).containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testInsertAndUpdateInOneGo() {
        List<MdmWarehouse> records = List.of(record(), record(), record(), record());
        foreachEnumerated(records, (i, record) -> {
            record.setName("Творобушек");
        });
        repository.insertOrUpdateAll(records.stream().limit(2).collect(Collectors.toList()));

        foreachEnumerated(records, (i, record) -> {
            record.setName("Бульонные параллелепипеды");
        });
        repository.insertOrUpdateAll(records);

        assertThat(repository.findAll()).containsExactlyInAnyOrderElementsOf(records);
    }

    private MdmWarehouse record() {
        return random.nextObject(MdmWarehouse.class);
    }

    private List<String> keys(Collection<MdmWarehouse> warehouses) {
        return warehouses.stream().map(MdmWarehouse::getId).collect(Collectors.toList());
    }

    private void foreachEnumerated(Iterable<MdmWarehouse> records,
                                   BiConsumer<Integer, MdmWarehouse> action) {
        var iter = records.iterator();
        int count = 0;
        while (iter.hasNext()) {
            action.accept(count++, iter.next());
        }
    }
}
