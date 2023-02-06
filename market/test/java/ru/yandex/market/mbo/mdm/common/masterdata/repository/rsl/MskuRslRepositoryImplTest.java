package ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.MskuRsl;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class MskuRslRepositoryImplTest extends MdmBaseDbTestClass {

    private static final int SEED = 147929;

    @Autowired
    private MskuRslRepository mskuRslRepository;
    private Random random = new Random(SEED);

    @Test
    public void testSimpleInsert() {
        var rsl = nextItem().setModifiedAt(Instant.now().minus(100, ChronoUnit.DAYS)); // дата перетрётся на текущую
        mskuRslRepository.insert(rsl);
        var fromDb = mskuRslRepository.findById(rsl.getKey());

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
        var ids = items.stream().map(MskuRsl::getKey).collect(Collectors.toList());
        mskuRslRepository.insertBatch(items);

        var found = mskuRslRepository.findByIds(ids);
        assertThat(found).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    public void testSimpleDelete() {
        var item = nextItem();
        mskuRslRepository.insert(item);
        assertThat(mskuRslRepository.totalCount()).isEqualTo(1);

        mskuRslRepository.delete(item);
        assertThat(mskuRslRepository.totalCount()).isEqualTo(0);
    }

    @Test
    public void testMultipleDelete() {
        var retainedItem1 = nextItem();
        var retainedItem2 = nextItem();
        var removedItem1 = nextItem();
        var removedItem2 = nextItem();
        var idsToRemove = List.of(removedItem1.getKey(), removedItem2.getKey());

        mskuRslRepository.insertBatch(retainedItem1, removedItem1, retainedItem2, removedItem2);
        assertThat(mskuRslRepository.totalCount()).isEqualTo(4);
        mskuRslRepository.delete(idsToRemove);
        assertThat(mskuRslRepository.findAll())
            .containsExactlyInAnyOrder(retainedItem1, retainedItem2);

        mskuRslRepository.deleteAll();
        assertThat(mskuRslRepository.totalCount()).isEqualTo(0);
    }

    @Test
    public void testSimpleInsertOrUpdate() {
        var item = nextItem();
        mskuRslRepository.insertOrUpdate(item);
        var found = mskuRslRepository.findById(item.getKey());

        item.setInRslDays(0);
        item.setInRslPercents(0);
        item.setOutRslDays(10);
        item.setOutRslPercents(10);
        assertThat(item).isNotEqualTo(found);
        mskuRslRepository.insertOrUpdate(item);
        found = mskuRslRepository.findById(item.getKey());
        assertThat(item).isEqualTo(found);
    }

    @Test
    public void testSimpleUpdate() {
        var item = nextItem();
        mskuRslRepository.insert(item);
        var found = mskuRslRepository.findById(item.getKey());

        item.setInRslDays(0);
        item.setInRslPercents(0);
        item.setOutRslDays(10);
        item.setOutRslPercents(10);
        assertThat(item).isNotEqualTo(found);
        mskuRslRepository.update(item);
        found = mskuRslRepository.findById(item.getKey());
        assertThat(item).isEqualTo(found);
    }

    @Test
    public void testMultipleUpdate() {
        var sameItem = nextItem();
        var updatedItem = nextItem();
        var newItem1 = nextItem();
        var newItem2 = nextItem();

        mskuRslRepository.insertBatch(sameItem, updatedItem);
        assertThat(mskuRslRepository.totalCount()).isEqualTo(2);
        updatedItem.setInRslDays(0);
        updatedItem.setInRslPercents(0);
        updatedItem.setOutRslDays(10);
        updatedItem.setOutRslPercents(10);
        mskuRslRepository.insertOrUpdateAll(List.of(sameItem, updatedItem, newItem1, newItem2));
        assertThat(mskuRslRepository.findAll()).containsExactlyInAnyOrder(
            sameItem, updatedItem, newItem1, newItem2
        );
    }

    @Test
    public void testSimpleInsertWithSameSskuKey() {
        var key = nextItem().getMskuId();
        var rsl1 = nextItem().setMskuId(key);
        var rsl2 = nextItem().setMskuId(key);
        var rsl3 = nextItem().setMskuId(key);

        rsl1.setActivatedAt(LocalDate.now().minusDays(1));
        rsl2.setActivatedAt(LocalDate.now());
        rsl3.setActivatedAt(LocalDate.now().plusDays(1));

        mskuRslRepository.insertBatch(rsl3, rsl2, rsl1);
        assertThat(mskuRslRepository.totalCount()).isEqualTo(3);
        assertThat(mskuRslRepository.findByMskuId(key)).containsExactlyInAnyOrder(
            rsl1, rsl2, rsl3
        );
    }

    @Test
    public void testInsertOrUpdateWithSameSskuKey() {
        var key = nextItem().getMskuId();
        var rslToUpdate = nextItem().setMskuId(key);
        var rslOther = nextItem().setMskuId(key);

        rslToUpdate.setActivatedAt(LocalDate.now().plusDays(1));
        rslOther.setActivatedAt(LocalDate.now().minusDays(1));

        mskuRslRepository.insertOrUpdateAll(List.of(rslToUpdate, rslOther));
        assertThat(mskuRslRepository.totalCount()).isEqualTo(2);

        rslToUpdate.setInRslDays(10);
        rslToUpdate.setInRslPercents(11);
        rslToUpdate.setOutRslDays(12);
        rslToUpdate.setOutRslPercents(13);
        mskuRslRepository.insertOrUpdateAll(List.of(rslToUpdate, rslOther));
        assertThat(mskuRslRepository.totalCount()).isEqualTo(2);
        assertThat(mskuRslRepository.findByMskuId(key)).containsExactlyInAnyOrder(
            rslToUpdate, rslOther
        );
    }

    @Test
    public void testInsertAndFindWithSskuKeyGrouping() {
        var key1 = nextItem().getMskuId();
        var rsl11 = nextItem().setMskuId(key1);
        var rsl12 = nextItem().setMskuId(key1);
        var rsl13 = nextItem().setMskuId(key1);

        var key2 = nextItem().getMskuId();
        var rsl21 = nextItem().setMskuId(key2);
        var rsl22 = nextItem().setMskuId(key2);
        var rsl23 = nextItem().setMskuId(key2);

        rsl11.setActivatedAt(LocalDate.now().minusDays(1));
        rsl12.setActivatedAt(LocalDate.now());
        rsl13.setActivatedAt(LocalDate.now().plusDays(1));
        rsl21.setActivatedAt(LocalDate.now().minusDays(1));
        rsl22.setActivatedAt(LocalDate.now());
        rsl23.setActivatedAt(LocalDate.now().plusDays(1));

        mskuRslRepository.insertBatch(rsl11, rsl12, rsl13, rsl21, rsl22, rsl23);
        assertThat(mskuRslRepository.totalCount()).isEqualTo(6);
        assertThat(mskuRslRepository.findByMskuId(key1)).containsExactlyInAnyOrder(
            rsl11, rsl12, rsl13
        );
        assertThat(mskuRslRepository.findByMskuId(key2)).containsExactlyInAnyOrder(
            rsl21, rsl22, rsl23
        );
    }

    private MskuRsl nextItem() {
        return new MskuRsl()
            .setMskuId(random.nextInt())
            .setInRslDays(random.nextInt())
            .setInRslPercents(random.nextInt())
            .setOutRslDays(random.nextInt())
            .setOutRslPercents(random.nextInt());
    }
}
