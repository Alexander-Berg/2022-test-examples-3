package ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl;


import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.RslThreshold;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SupplierRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SupplierRslParam;
import ru.yandex.market.mbo.mdm.common.rsl.RslType;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class SupplierRslRepositoryImplTest extends MdmBaseDbTestClass {

    private static final int SEED = 147929;

    @Autowired
    private SupplierRslRepository supplierRslRepository;
    private EnhancedRandom random = new EnhancedRandomBuilder().seed(SEED).build();

    @Test
    public void testSimpleInsert() {
        var rsl = nextItem().setModifiedAt(Instant.now().minus(100, ChronoUnit.DAYS)); // дата перетрётся на текущую
        supplierRslRepository.insert(rsl);
        var fromDb = supplierRslRepository.findById(rsl.getKey());
        assertThat(fromDb.getModifiedAt()).isNotEqualTo(rsl.getModifiedAt());
        assertThat(fromDb).isEqualTo(rsl);
    }

    @Test
    public void testMultipleInsert() {
        var items = List.of(
            nextItem(),
            nextItem(),
            nextItem(),
            nextItem(),
            nextItem()
        );
        var ids = items.stream().map(SupplierRsl::getKey).collect(Collectors.toList());
        supplierRslRepository.insertBatch(items);

        var found = supplierRslRepository.findByIds(ids);
        assertThat(found).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    public void testSimpleDelete() {
        var item = nextItem();
        supplierRslRepository.insert(item);
        assertThat(supplierRslRepository.totalCount()).isEqualTo(1);

        supplierRslRepository.delete(item);
        assertThat(supplierRslRepository.totalCount()).isEqualTo(0);
    }

    @Test
    public void testMultipleDelete() {
        var retainedItem1 = nextItem();
        var retainedItem2 = nextItem();
        var removedItem1 = nextItem();
        var removedItem2 = nextItem();
        var idsToRemove = List.of(removedItem1.getKey(), removedItem2.getKey());

        supplierRslRepository.insertBatch(retainedItem1, removedItem1, retainedItem2, removedItem2);
        assertThat(supplierRslRepository.totalCount()).isEqualTo(4);
        supplierRslRepository.delete(idsToRemove);
        assertThat(supplierRslRepository.findAll())
            .containsExactlyInAnyOrder(retainedItem1, retainedItem2);

        supplierRslRepository.deleteAll();
        assertThat(supplierRslRepository.totalCount()).isEqualTo(0);
    }

    @Test
    public void testSimpleInsertOrUpdate() {
        var item = nextItem();
        supplierRslRepository.insertOrUpdate(item);
        var found = supplierRslRepository.findById(item.getKey());

        List<RslThreshold> thresholds = List.of(nextThreshold(), nextThreshold(), nextThreshold());
        item.setRslThresholds(thresholds);
        assertThat(item).isNotEqualTo(found);
        supplierRslRepository.insertOrUpdate(item);
        found = supplierRslRepository.findById(item.getKey());
        assertThat(item).isEqualTo(found);
    }

    @Test
    public void testSimpleUpdate() {
        var item = nextItem();
        supplierRslRepository.insert(item);
        var found = supplierRslRepository.findById(item.getKey());

        List<RslThreshold> thresholds = List.of(nextThreshold(), nextThreshold(), nextThreshold());
        item.setRslThresholds(thresholds);
        assertThat(item).isNotEqualTo(found);
        supplierRslRepository.update(item);
        found = supplierRslRepository.findById(item.getKey());
        assertThat(item).isEqualTo(found);
    }

    @Test
    public void testMultipleUpdate() {
        var sameItem = nextItem();
        var updatedItem = nextItem();
        var newItem1 = nextItem();
        var newItem2 = nextItem();

        supplierRslRepository.insertBatch(sameItem, updatedItem);
        assertThat(supplierRslRepository.totalCount()).isEqualTo(2);
        List<RslThreshold> thresholds = List.of(nextThreshold(), nextThreshold(), nextThreshold());
        updatedItem.setRslThresholds(thresholds);

        supplierRslRepository.insertOrUpdateAll(List.of(sameItem, updatedItem, newItem1, newItem2));
        assertThat(supplierRslRepository.findAll()).containsExactlyInAnyOrder(
            sameItem, updatedItem, newItem1, newItem2
        );
    }

    @Test
    public void testInsertOrUpdateBatchExisting() {
        var item1 = supplierRslRepository.insert(nextItem());
        var item2 = supplierRslRepository.insert(nextItem());
        item1.setRslThresholds(List.of(nextThreshold(), nextThreshold()));
        item2.setRslThresholds(List.of(nextThreshold(), nextThreshold()));
        supplierRslRepository.insertOrUpdateAll(Arrays.asList(item1, item2));
        assertThat(supplierRslRepository.findAll()).containsExactlyInAnyOrder(item1, item2);
    }

    @Test
    public void testInsertOrUpdateExisting() {
        var item = supplierRslRepository.insert(nextItem());
        item.setRslThresholds(List.of(nextThreshold(), nextThreshold()));
        supplierRslRepository.insertOrUpdate(item);
        assertThat(supplierRslRepository.findAll()).containsExactlyInAnyOrder(item);
    }


    @Test
    public void shouldBeFoundRslsBySeveralParametersOfKey() {
        //given
        var rsl1 = nextItem();
        var rsl2 = nextItem()
            .setSupplierId(rsl1.getSupplierId())
            .setRealId(rsl1.getRealId())
            .setCategoryId(rsl1.getCategoryId())
            .setCargoType750(rsl1.getCargoType750());
        var rsl3 = nextItem()
            .setSupplierId(rsl1.getSupplierId())
            .setRealId(rsl1.getRealId())
            .setCategoryId(rsl1.getCategoryId())
            .setCargoType750(rsl1.getCargoType750());
        var rsl4 = nextItem();
        supplierRslRepository.insertBatch(rsl1, rsl2, rsl3, rsl4);

        var param1 = new SupplierRslParam()
            .setSupplierId(rsl1.getSupplierId())
            .setCategoryId(rsl1.getCategoryId())
            .setRealId(rsl1.getRealId())
            .setCargoType750(rsl1.getCargoType750());

        //when
        var found = supplierRslRepository.findBySupplierRslParams(List.of(param1));

        //then
        assertThat(found)
            .hasSize(3)
            .containsExactlyInAnyOrder(rsl1, rsl2, rsl3);

        //when
        supplierRslRepository.insertBatch(
            rsl1.setRealId(rsl1.getRealId() + "'"),
            rsl2.setRealId(rsl2.getRealId() + "'"));
        param1.setRealId(param1.getRealId() + "'");

        found = supplierRslRepository.findBySupplierRslParams(List.of(param1));

        //then
        assertThat(found)
            .hasSize(2)
            .containsExactlyInAnyOrder(rsl1, rsl2);

        //when
        var param4 = new SupplierRslParam()
            .setSupplierId(rsl4.getSupplierId())
            .setRealId(rsl4.getRealId())
            .setCategoryId(rsl4.getCategoryId())
            .setCargoType750(rsl4.getCargoType750());

        found = supplierRslRepository.findBySupplierRslParams(List.of(param1, param4));

        //then
        assertThat(found)
            .hasSize(3)
            .containsExactlyInAnyOrder(rsl1, rsl2, rsl4);
    }

    private SupplierRsl nextItem() {
        return new SupplierRsl()
            .setSupplierId(random.nextInt())
            .setCategoryId(random.nextLong())
            .setType(RslType.FIRST_PARTY)
            .setRealId(random.nextObject(String.class))
            .setActivatedAt(random.nextObject(LocalDate.class))
            .setCargoType750(random.nextBoolean())
            .setRslThresholds(random.objects(RslThreshold.class, 5).collect(Collectors.toList()));
    }

    private RslThreshold nextThreshold() {
        return random.nextObject(RslThreshold.class);
    }
}
