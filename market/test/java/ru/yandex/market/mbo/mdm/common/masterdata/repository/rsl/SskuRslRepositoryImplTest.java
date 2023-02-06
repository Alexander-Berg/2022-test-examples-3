package ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SskuRsl;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class SskuRslRepositoryImplTest extends MdmBaseDbTestClass {

    private static final int SEED = 147929;

    @Autowired
    private SskuRslRepository sskuRslRepository;
    private Random random = new Random(SEED);

    @Test
    public void testSimpleInsert() {
        var rsl = nextItem().setModifiedAt(Instant.now().minus(100, ChronoUnit.DAYS)); // дата перетрётся на текущую
        sskuRslRepository.insert(rsl);
        var fromDb = sskuRslRepository.findById(rsl.getKey());

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
        var ids = items.stream().map(SskuRsl::getKey).collect(Collectors.toList());
        sskuRslRepository.insertBatch(items);

        var found = sskuRslRepository.findByIds(ids);
        assertThat(found).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    public void testSimpleDelete() {
        var item = nextItem();
        sskuRslRepository.insert(item);
        assertThat(sskuRslRepository.totalCount()).isEqualTo(1);

        sskuRslRepository.delete(item);
        assertThat(sskuRslRepository.totalCount()).isEqualTo(0);
    }

    @Test
    public void testMultipleDelete() {
        var retainedItem1 = nextItem();
        var retainedItem2 = nextItem();
        var removedItem1 = nextItem();
        var removedItem2 = nextItem();
        var idsToRemove = List.of(removedItem1.getKey(), removedItem2.getKey());

        sskuRslRepository.insertBatch(retainedItem1, removedItem1, retainedItem2, removedItem2);
        assertThat(sskuRslRepository.totalCount()).isEqualTo(4);
        sskuRslRepository.delete(idsToRemove);
        assertThat(sskuRslRepository.findAll())
            .containsExactlyInAnyOrder(retainedItem1, retainedItem2);

        sskuRslRepository.deleteAll();
        assertThat(sskuRslRepository.totalCount()).isEqualTo(0);
    }

    @Test
    public void testSimpleInsertOrUpdate() {
        var item = nextItem();
        sskuRslRepository.insertOrUpdate(item);
        var found = sskuRslRepository.findById(item.getKey());

        item.setInRslDays(0);
        item.setInRslPercents(0);
        item.setOutRslDays(10);
        item.setOutRslPercents(10);
        assertThat(item).isNotEqualTo(found);
        sskuRslRepository.insertOrUpdate(item);
        found = sskuRslRepository.findById(item.getKey());
        assertThat(item).isEqualTo(found);
    }

    @Test
    public void testSimpleUpdate() {
        var item = nextItem();
        sskuRslRepository.insert(item);
        var found = sskuRslRepository.findById(item.getKey());

        item.setInRslDays(0);
        item.setInRslPercents(0);
        item.setOutRslDays(10);
        item.setOutRslPercents(10);
        assertThat(item).isNotEqualTo(found);
        sskuRslRepository.update(item);
        found = sskuRslRepository.findById(item.getKey());
        assertThat(item).isEqualTo(found);
    }

    @Test
    public void testMultipleUpdate() {
        var sameItem = nextItem();
        var updatedItem = nextItem();
        var newItem1 = nextItem();
        var newItem2 = nextItem();

        sskuRslRepository.insertBatch(sameItem, updatedItem);
        assertThat(sskuRslRepository.totalCount()).isEqualTo(2);
        updatedItem.setInRslDays(0);
        updatedItem.setInRslPercents(0);
        updatedItem.setOutRslDays(10);
        updatedItem.setOutRslPercents(10);
        sskuRslRepository.insertOrUpdateAll(List.of(sameItem, updatedItem, newItem1, newItem2));
        assertThat(sskuRslRepository.findAll()).containsExactlyInAnyOrder(
            sameItem, updatedItem, newItem1, newItem2
        );
    }

    @Test
    public void testSimpleInsertWithSameSskuKey() {
        var key = nextItem().getShopSkuKey();
        var rsl1 = nextItem().setSupplierId(key.getSupplierId()).setShopSku(key.getShopSku());
        var rsl2 = nextItem().setSupplierId(key.getSupplierId()).setShopSku(key.getShopSku());
        var rsl3 = nextItem().setSupplierId(key.getSupplierId()).setShopSku(key.getShopSku());

        rsl1.setActivatedAt(LocalDate.now().minusDays(1));
        rsl2.setActivatedAt(LocalDate.now());
        rsl3.setActivatedAt(LocalDate.now().plusDays(1));

        sskuRslRepository.insertBatch(rsl3, rsl2, rsl1);
        assertThat(sskuRslRepository.totalCount()).isEqualTo(3);
        assertThat(sskuRslRepository.findByShopSkuKey(key)).containsExactlyInAnyOrder(
            rsl1, rsl2, rsl3
        );
    }

    @Test
    public void testInsertOrUpdateWithSameSskuKey() {
        var key = nextItem().getShopSkuKey();
        var rslToUpdate = nextItem().setSupplierId(key.getSupplierId()).setShopSku(key.getShopSku());
        var rslOther = nextItem().setSupplierId(key.getSupplierId()).setShopSku(key.getShopSku());

        rslToUpdate.setActivatedAt(LocalDate.now().plusDays(1));
        rslOther.setActivatedAt(LocalDate.now().minusDays(1));

        sskuRslRepository.insertOrUpdateAll(List.of(rslToUpdate, rslOther));
        assertThat(sskuRslRepository.totalCount()).isEqualTo(2);

        rslToUpdate.setInRslDays(10);
        rslToUpdate.setInRslPercents(11);
        rslToUpdate.setOutRslDays(12);
        rslToUpdate.setOutRslPercents(13);
        sskuRslRepository.insertOrUpdateAll(List.of(rslToUpdate, rslOther));
        assertThat(sskuRslRepository.totalCount()).isEqualTo(2);
        assertThat(sskuRslRepository.findByShopSkuKey(key)).containsExactlyInAnyOrder(
            rslToUpdate, rslOther
        );
    }

    @Test
    public void testInsertAndFindWithSskuKeyGrouping() {
        var key1 = nextItem().getShopSkuKey();
        var rsl11 = nextItem().setSupplierId(key1.getSupplierId()).setShopSku(key1.getShopSku());
        var rsl12 = nextItem().setSupplierId(key1.getSupplierId()).setShopSku(key1.getShopSku());
        var rsl13 = nextItem().setSupplierId(key1.getSupplierId()).setShopSku(key1.getShopSku());

        var key2 = nextItem().getShopSkuKey();
        var rsl21 = nextItem().setSupplierId(key2.getSupplierId()).setShopSku(key2.getShopSku());
        var rsl22 = nextItem().setSupplierId(key2.getSupplierId()).setShopSku(key2.getShopSku());
        var rsl23 = nextItem().setSupplierId(key2.getSupplierId()).setShopSku(key2.getShopSku());

        rsl11.setActivatedAt(LocalDate.now().minusDays(1));
        rsl12.setActivatedAt(LocalDate.now());
        rsl13.setActivatedAt(LocalDate.now().plusDays(1));
        rsl21.setActivatedAt(LocalDate.now().minusDays(1));
        rsl22.setActivatedAt(LocalDate.now());
        rsl23.setActivatedAt(LocalDate.now().plusDays(1));

        sskuRslRepository.insertBatch(rsl11, rsl12, rsl13, rsl21, rsl22, rsl23);
        assertThat(sskuRslRepository.totalCount()).isEqualTo(6);
        assertThat(sskuRslRepository.findByShopSkuKey(key1)).containsExactlyInAnyOrder(
            rsl11, rsl12, rsl13
        );
        assertThat(sskuRslRepository.findByShopSkuKey(key2)).containsExactlyInAnyOrder(
            rsl21, rsl22, rsl23
        );
    }

    @Test
    public void testFindByShopSkuKeysInClauseLimit() {
        List<ShopSkuKey> keys = IntStream.range(0, Short.MAX_VALUE + 5)
            .mapToObj(i -> new ShopSkuKey(i, String.valueOf(i % 10)))
            .collect(Collectors.toList());
        Assertions.assertThatNoException()
            .isThrownBy(() -> sskuRslRepository.findByShopSkuKeys(keys));
    }

    private SskuRsl nextItem() {
        return new SskuRsl()
            .setSupplierId(random.nextInt())
            .setShopSku(String.valueOf(random.nextInt()))
            .setInRslDays(random.nextInt())
            .setInRslPercents(random.nextInt())
            .setOutRslDays(random.nextInt())
            .setOutRslPercents(random.nextInt());
    }
}
