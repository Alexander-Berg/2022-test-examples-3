package ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.CategoryRsl;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryRslRepositoryImplTest extends MdmBaseDbTestClass {

    private static final int SEED = 147929;

    @Autowired
    private CategoryRslRepository categoryRslRepository;
    private Random random = new Random(SEED);

    @Test
    public void testSimpleInsert() {
        var rsl = nextItem().setModifiedAt(Instant.now().minus(100, ChronoUnit.DAYS)); // дата перетрётся на текущую
        categoryRslRepository.insert(rsl);
        var fromDb = categoryRslRepository.findById(rsl.getKey());

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
        var ids = items.stream().map(CategoryRsl::getKey).collect(Collectors.toList());
        categoryRslRepository.insertBatch(items);

        var found = categoryRslRepository.findByIds(ids);
        assertThat(found).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    public void testSimpleDelete() {
        var item = nextItem();
        categoryRslRepository.insert(item);
        assertThat(categoryRslRepository.totalCount()).isEqualTo(1);

        categoryRslRepository.delete(item);
        assertThat(categoryRslRepository.totalCount()).isEqualTo(0);
    }

    @Test
    public void testMultipleDelete() {
        var retainedItem1 = nextItem();
        var retainedItem2 = nextItem();
        var removedItem1 = nextItem();
        var removedItem2 = nextItem();
        var idsToRemove = List.of(removedItem1.getKey(), removedItem2.getKey());

        categoryRslRepository.insertBatch(retainedItem1, removedItem1, retainedItem2, removedItem2);
        assertThat(categoryRslRepository.totalCount()).isEqualTo(4);
        categoryRslRepository.delete(idsToRemove);
        assertThat(categoryRslRepository.findAll())
            .containsExactlyInAnyOrder(retainedItem1, retainedItem2);

        categoryRslRepository.deleteAll();
        assertThat(categoryRslRepository.totalCount()).isEqualTo(0);
    }

    @Test
    public void testSimpleInsertOrUpdate() {
        var item = nextItem();
        categoryRslRepository.insertOrUpdate(item);
        var found = categoryRslRepository.findById(item.getKey());

        item.setInRslDays(0);
        item.setInRslPercents(0);
        item.setOutRslDays(10);
        item.setOutRslPercents(10);
        assertThat(item).isNotEqualTo(found);
        categoryRslRepository.insertOrUpdate(item);
        found = categoryRslRepository.findById(item.getKey());
        assertThat(item).isEqualTo(found);
    }

    @Test
    public void testSimpleUpdate() {
        var item = nextItem();
        categoryRslRepository.insert(item);
        var found = categoryRslRepository.findById(item.getKey());

        item.setInRslDays(0);
        item.setInRslPercents(0);
        item.setOutRslDays(10);
        item.setOutRslPercents(10);
        assertThat(item).isNotEqualTo(found);
        categoryRslRepository.update(item);
        found = categoryRslRepository.findById(item.getKey());
        assertThat(item).isEqualTo(found);
    }

    @Test
    public void testMultipleUpdate() {
        var sameItem = nextItem();
        var updatedItem = nextItem();
        var newItem1 = nextItem();
        var newItem2 = nextItem();

        categoryRslRepository.insertBatch(sameItem, updatedItem);
        assertThat(categoryRslRepository.totalCount()).isEqualTo(2);
        updatedItem.setInRslDays(0);
        updatedItem.setInRslPercents(0);
        updatedItem.setOutRslDays(10);
        updatedItem.setOutRslPercents(10);
        categoryRslRepository.insertOrUpdateAll(List.of(sameItem, updatedItem, newItem1, newItem2));
        assertThat(categoryRslRepository.findAll()).containsExactlyInAnyOrder(
            sameItem, updatedItem, newItem1, newItem2
        );
    }

    @Test
    public void testSimpleInsertWithSameSskuKey() {
        var key = nextItem().getCategoryId();
        var rsl1 = nextItem().setCategoryId(key);
        var rsl2 = nextItem().setCategoryId(key);
        var rsl3 = nextItem().setCategoryId(key);

        rsl1.setActivatedAt(LocalDate.now().minusDays(1));
        rsl2.setActivatedAt(LocalDate.now());
        rsl3.setActivatedAt(LocalDate.now().plusDays(1));

        categoryRslRepository.insertBatch(rsl3, rsl2, rsl1);
        assertThat(categoryRslRepository.totalCount()).isEqualTo(3);
        assertThat(categoryRslRepository.findByCategoryId(key)).containsExactlyInAnyOrder(
            rsl1, rsl2, rsl3
        );
    }

    @Test
    public void testInsertOrUpdateWithSameSskuKey() {
        var key = nextItem().getCategoryId();
        var rslToUpdate = nextItem().setCategoryId(key);
        var rslOther = nextItem().setCategoryId(key);

        rslToUpdate.setActivatedAt(LocalDate.now().plusDays(1));
        rslOther.setActivatedAt(LocalDate.now().minusDays(1));

        categoryRslRepository.insertOrUpdateAll(List.of(rslToUpdate, rslOther));
        assertThat(categoryRslRepository.totalCount()).isEqualTo(2);

        rslToUpdate.setInRslDays(10);
        rslToUpdate.setInRslPercents(11);
        rslToUpdate.setOutRslDays(12);
        rslToUpdate.setOutRslPercents(13);
        categoryRslRepository.insertOrUpdateAll(List.of(rslToUpdate, rslOther));
        assertThat(categoryRslRepository.totalCount()).isEqualTo(2);
        assertThat(categoryRslRepository.findByCategoryId(key)).containsExactlyInAnyOrder(
            rslToUpdate, rslOther
        );
    }

    @Test
    public void testInsertAndFindWithSskuKeyGrouping() {
        var key1 = nextItem().getCategoryId();
        var rsl11 = nextItem().setCategoryId(key1);
        var rsl12 = nextItem().setCategoryId(key1);
        var rsl13 = nextItem().setCategoryId(key1);

        var key2 = nextItem().getCategoryId();
        var rsl21 = nextItem().setCategoryId(key2);
        var rsl22 = nextItem().setCategoryId(key2);
        var rsl23 = nextItem().setCategoryId(key2);

        rsl11.setActivatedAt(LocalDate.now().minusDays(1));
        rsl12.setActivatedAt(LocalDate.now());
        rsl13.setActivatedAt(LocalDate.now().plusDays(1));
        rsl21.setActivatedAt(LocalDate.now().minusDays(1));
        rsl22.setActivatedAt(LocalDate.now());
        rsl23.setActivatedAt(LocalDate.now().plusDays(1));

        categoryRslRepository.insertBatch(rsl11, rsl12, rsl13, rsl21, rsl22, rsl23);
        assertThat(categoryRslRepository.totalCount()).isEqualTo(6);
        assertThat(categoryRslRepository.findByCategoryId(key1)).containsExactlyInAnyOrder(
            rsl11, rsl12, rsl13
        );
        assertThat(categoryRslRepository.findByCategoryId(key2)).containsExactlyInAnyOrder(
            rsl21, rsl22, rsl23
        );
    }

    private CategoryRsl nextItem() {
        return new CategoryRsl()
            .setCategoryId(random.nextInt())
            .setInRslDays(random.nextInt())
            .setInRslPercents(random.nextInt())
            .setOutRslDays(random.nextInt())
            .setOutRslPercents(random.nextInt());
    }
}
