package ru.yandex.market.mboc.common.masterdata.repository.cutoff;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff.CutoffState;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.http.MboMappings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.market.mboc.common.masterdata.repository.cutoff.OfferCutoffRepositoryImpl.NO_SAME_CUTOFFS_CONSTRAINT;
import static ru.yandex.market.mboc.common.models.HasId.EMPTY_ID;

public class OfferCutoffRepositoryTest extends MdmBaseDbTestClass {

    private static final long SEED = 668L;
    private static final String ERROR_TYPE1 = "mboc.error.mdm-bad-product";
    private static final String ERROR_TYPE2 = "mboc.error.mdm-awful-product";

    private static final ShopSkuKey SHOP_SKU_KEY1 = new ShopSkuKey(1, "1");
    private static final ShopSkuKey SHOP_SKU_KEY2 = new ShopSkuKey(2, "2");
    private static final ShopSkuKey SHOP_SKU_KEY3 = new ShopSkuKey(3, "3");

    @Autowired
    private OfferCutoffRepository cutoffRepository;

    private EnhancedRandom random;

    private static void assertNewerThan(LocalDateTime timeToCheck, LocalDateTime timePoint) {
        assertTrue(timeToCheck.equals(timePoint) || timeToCheck.isAfter(timePoint));
    }

    @Before
    public void insertOffersAndSuppliers() {
        random = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void testSimpleInsert() {
        LocalDateTime now = DateTimeUtils.dateTimeNow().withNano(0);
        OfferCutoff cutoff = createCutoff(SHOP_SKU_KEY1);
        cutoff = cutoffRepository.insert(cutoff);
        assertNotEquals(EMPTY_ID, cutoff.getId());
        assertNewerThan(cutoff.getStateChangeTs(), now);
    }

    @Test
    public void testSimpleInsertFailsOnSpecialConstraint() {
        // Специальными констреинтами называются ограничения на открытые кат-оффы:
        // Не может быть два кат-оффа с одинаковым shop_sku, supplier_id, type_id в состоянии OPEN.
        OfferCutoff cutoff = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1));
        OfferCutoff sameByConstraints = createCutoff(SHOP_SKU_KEY1);
        assertNotEquals(cutoff.getId(), sameByConstraints.getId());
        try {
            cutoffRepository.insert(sameByConstraints);
            fail("Expected exception on DB constraints but none was thrown.");
        } catch (DataAccessException expectedException) {
            assertNotNull(expectedException.getMessage());
            assertTrue(expectedException.getMessage().contains(NO_SAME_CUTOFFS_CONSTRAINT));
        }
        assertEquals(1, cutoffRepository.totalCount()); // Был и остался только первый вставленный.
    }

    @Test
    public void testSimpleInsertOnClosedCutoffsDoesntTriggerSpecialConstraint() {
        OfferCutoff openCutoff = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1));
        OfferCutoff closedCutoff = createCutoff(SHOP_SKU_KEY1).setState(CutoffState.CLOSED);
        closedCutoff = cutoffRepository.insert(closedCutoff);
        assertNotEquals(EMPTY_ID, openCutoff.getId());
        assertNotEquals(EMPTY_ID, closedCutoff.getId());
        assertNotEquals(openCutoff.getId(), closedCutoff.getId());
    }

    @Test
    public void testInsertOrIgnore() {
        LocalDateTime now = DateTimeUtils.dateTimeNow().withNano(0);
        OfferCutoff cutoff = createCutoff(SHOP_SKU_KEY1);
        cutoff = cutoffRepository.insertOrIgnore(cutoff);
        assertNotEquals(EMPTY_ID, cutoff.getId());
        assertNewerThan(cutoff.getStateChangeTs(), now);
    }

    @Test
    public void testInsertOrIgnoreDoesNothingOnSpecialConstraintCollision() {
        OfferCutoff cutoff = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1));
        OfferCutoff sameByConstraints = createCutoff(SHOP_SKU_KEY1);
        assertNotEquals(cutoff.getId(), sameByConstraints.getId());
        assertEquals(EMPTY_ID, sameByConstraints.getId());

        assertEquals(1, cutoffRepository.totalCount());
        sameByConstraints = cutoffRepository.insertOrIgnore(sameByConstraints);
        assertEquals(EMPTY_ID, sameByConstraints.getId());
        assertEquals(1, cutoffRepository.totalCount());
    }

    @Test
    public void testSimpleInsertDifferentCutoffs() {
        OfferCutoff cutoff1 = createCutoff(SHOP_SKU_KEY1);
        OfferCutoff cutoff2 = createCutoff(SHOP_SKU_KEY2);
        cutoffRepository.insert(cutoff1);
        cutoffRepository.insert(cutoff2);
        assertEquals(2, cutoffRepository.totalCount());
    }

    @Test
    public void testInsertOrIgnoreDifferentCutoffs() {
        OfferCutoff cutoff1 = createCutoff(SHOP_SKU_KEY1);
        OfferCutoff cutoff2 = createCutoff(SHOP_SKU_KEY2);
        cutoffRepository.insertOrIgnore(cutoff1);
        cutoffRepository.insertOrIgnore(cutoff2);
        assertEquals(2, cutoffRepository.totalCount());
    }

    @Test
    public void testRemoveAll() {
        cutoffRepository.insertOrIgnore(createCutoff(SHOP_SKU_KEY1));
        cutoffRepository.insertOrIgnore(createCutoff(SHOP_SKU_KEY2));
        assertEquals(2, cutoffRepository.totalCount());

        cutoffRepository.deleteAll();
        assertEquals(0, cutoffRepository.totalCount());
    }

    @Test
    public void testRemoveOne() {
        OfferCutoff toRemain = cutoffRepository.insertOrIgnore(createCutoff(SHOP_SKU_KEY1));
        OfferCutoff toRemove = cutoffRepository.insertOrIgnore(createCutoff(SHOP_SKU_KEY2));
        assertEquals(2, cutoffRepository.totalCount());

        cutoffRepository.delete(toRemove);
        assertEquals(1, cutoffRepository.totalCount());
        assertThat(cutoffRepository.findAll()).containsExactlyInAnyOrder(toRemain);
    }

    @Test
    public void testInsertBatch() {
        cutoffRepository.insertBatch(Arrays.asList(createCutoff(SHOP_SKU_KEY1), createCutoff(SHOP_SKU_KEY2)));
        assertEquals(2, cutoffRepository.totalCount());
    }

    @Test
    public void testInsertBatchFailsOnSpecialConstraint() {
        OfferCutoff cutoff = createCutoff(SHOP_SKU_KEY1);
        OfferCutoff sameByConstraints = createCutoff(SHOP_SKU_KEY1);
        List<OfferCutoff> batch = Arrays.asList(cutoff, sameByConstraints);
        try {
            cutoffRepository.insertBatch(batch);
            fail("Expected exception on DB constraints but none was thrown.");
        } catch (DataAccessException expectedException) {
            assertNotNull(expectedException.getMessage());
            assertTrue(expectedException.getMessage().contains(NO_SAME_CUTOFFS_CONSTRAINT));
        }
        assertEquals(0, cutoffRepository.totalCount());
    }

    @Test
    public void testSimpleUpdate() {
        OfferCutoff cutoff = createCutoff(SHOP_SKU_KEY1, ERROR_TYPE1);
        cutoff = cutoffRepository.insert(cutoff);

        cutoff.setTypeId(ERROR_TYPE2);
        cutoffRepository.update(cutoff);
        assertThat(cutoffRepository.findAll()).containsExactlyInAnyOrder(cutoff);
    }

    @Test
    public void testBatchUpdate() {
        OfferCutoff cutoff1 = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1, ERROR_TYPE1));
        OfferCutoff cutoff2 = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY2, ERROR_TYPE1));

        cutoff1.setTypeId(ERROR_TYPE2);
        cutoff2.setTypeId(ERROR_TYPE2);

        cutoffRepository.updateAll(Arrays.asList(cutoff1, cutoff2));
        assertThat(cutoffRepository.findAll()).containsExactlyInAnyOrder(cutoff1, cutoff2);
    }

    @Test
    public void testUpdateFailsOnSpecialConstraint() {
        OfferCutoff cutoff1 = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1));
        OfferCutoff cutoff2 = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY2));
        assertEquals(2, cutoffRepository.totalCount());

        OfferCutoff sameByConstraints = new OfferCutoff().copyFrom(cutoff2)
            .setSupplierId(SHOP_SKU_KEY1.getSupplierId())
            .setShopSku(SHOP_SKU_KEY1.getShopSku());

        try {
            cutoffRepository.update(sameByConstraints);
            fail("Expected exception on DB constraints but none was thrown.");
        } catch (DataAccessException expectedException) {
            assertNotNull(expectedException.getMessage());
            assertTrue(expectedException.getMessage().contains(NO_SAME_CUTOFFS_CONSTRAINT));
        }
        assertThat(cutoffRepository.findAll()).containsExactlyInAnyOrder(cutoff1, cutoff2);
    }

    @Test
    public void testInsertOrUpdateOneNew() {
        LocalDateTime now = DateTimeUtils.dateTimeNow().withNano(0);
        OfferCutoff cutoff = createCutoff(SHOP_SKU_KEY1);
        cutoff = cutoffRepository.insertOrUpdate(cutoff);
        assertNotEquals(EMPTY_ID, cutoff.getId());
        assertNewerThan(cutoff.getStateChangeTs(), now);
    }

    @Test
    public void testInsertOrUpdateBatchNew() {
        LocalDateTime now = DateTimeUtils.dateTimeNow().withNano(0);
        OfferCutoff cutoff1 = createCutoff(SHOP_SKU_KEY1);
        OfferCutoff cutoff2 = createCutoff(SHOP_SKU_KEY2);
        List<OfferCutoff> inserted = cutoffRepository.insertOrUpdateAll(Arrays.asList(cutoff1, cutoff2));
        assertEquals(2, inserted.size());

        inserted.forEach(cutoff -> assertNotEquals(EMPTY_ID, cutoff.getId()));
        inserted.forEach(cutoff -> assertNewerThan(cutoff.getStateChangeTs(), now));
        assertThat(cutoffRepository.findAll()).containsExactlyInAnyOrder(inserted.get(0), inserted.get(1));
    }

    @Test
    public void testInsertOrUpdateExisting() {
        OfferCutoff cutoff = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1, ERROR_TYPE2));
        cutoff.setTypeId(ERROR_TYPE1);
        cutoffRepository.insertOrUpdate(cutoff);
        assertThat(cutoffRepository.findAll()).containsExactlyInAnyOrder(cutoff);
    }

    @Test
    public void testInsertOrUpdateBatchExisting() {
        OfferCutoff cutoff1 = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1, ERROR_TYPE2));
        OfferCutoff cutoff2 = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY2, ERROR_TYPE2));
        cutoff1.setTypeId(ERROR_TYPE1);
        cutoff2.setTypeId(ERROR_TYPE1);
        cutoffRepository.insertOrUpdateAll(Arrays.asList(cutoff1, cutoff2));
        assertThat(cutoffRepository.findAll()).containsExactlyInAnyOrder(cutoff1, cutoff2);
    }

    @Test
    public void testInsertOrUpdateMixed() {
        OfferCutoff existing = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1, ERROR_TYPE1));
        OfferCutoff created = createCutoff(SHOP_SKU_KEY2, ERROR_TYPE2);

        existing.setTypeId(ERROR_TYPE2);
        List<OfferCutoff> updated = cutoffRepository.insertOrUpdateAll(Arrays.asList(existing, created));
        updated.forEach(cutoff -> assertNotEquals(EMPTY_ID, cutoff.getId()));
        assertNotEquals(EMPTY_ID, created.getId());
        assertThat(updated).containsExactlyInAnyOrder(existing, created);
        assertThat(cutoffRepository.findAll()).containsExactlyInAnyOrder(existing, created);
    }

    @Test
    public void testInsertOrUpdateFailsOnSpecialConstraint() {
        cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1));
        try {
            cutoffRepository.insertOrUpdate(createCutoff(SHOP_SKU_KEY1));
            fail("Expected exception on DB constraints but none was thrown.");
        } catch (DataAccessException expectedException) {
            assertNotNull(expectedException.getMessage());
            assertTrue(expectedException.getMessage().contains(NO_SAME_CUTOFFS_CONSTRAINT));
            return;
        }
        fail("No exception is thrown when expected.");
    }

    @Test
    public void testSearchById() {
        List<OfferCutoff> cutoffs = new ArrayList<>();
        cutoffs.add(cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1, ERROR_TYPE1)));
        cutoffs.add(cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1, ERROR_TYPE2)));
        cutoffs.add(cutoffRepository.insert(createCutoff(SHOP_SKU_KEY2, ERROR_TYPE1)));
        cutoffs.add(cutoffRepository.insert(createCutoff(SHOP_SKU_KEY2, ERROR_TYPE2)));

        List<Long> allIds = cutoffs.stream().map(OfferCutoff::getId).collect(Collectors.toList());
        assertThat(cutoffRepository.findByIds(allIds)).containsExactlyInAnyOrderElementsOf(cutoffs);

        List<Long> someIds = Arrays.asList(cutoffs.get(0).getId(), cutoffs.get(2).getId());
        assertThat(cutoffRepository.findByIds(someIds)).containsExactlyInAnyOrder(cutoffs.get(0), cutoffs.get(2));
    }

    @Test
    public void testSearchByTripleKey() {
        cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1, ERROR_TYPE1));
        OfferCutoff toSearchFor = cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1, ERROR_TYPE2));
        cutoffRepository.insert(createCutoff(SHOP_SKU_KEY2, ERROR_TYPE1));
        cutoffRepository.insert(createCutoff(SHOP_SKU_KEY2, ERROR_TYPE2));

        OfferCutoffFilter filter = new OfferCutoffFilter()
            .setShopSku(toSearchFor.getShopSku())
            .setSupplierId(toSearchFor.getSupplierId())
            .setTypeId(toSearchFor.getTypeId());
        assertThat(cutoffRepository.findBy(filter)).containsExactlyInAnyOrder(toSearchFor);
    }

    @Test
    public void testSearchByState() {
        List<OfferCutoff> cutoffs = new ArrayList<>();
        cutoffs.add(cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1, ERROR_TYPE1).setState(CutoffState.CLOSED)));
        cutoffs.add(cutoffRepository.insert(createCutoff(SHOP_SKU_KEY1, ERROR_TYPE2)));
        cutoffs.add(cutoffRepository.insert(createCutoff(SHOP_SKU_KEY2, ERROR_TYPE1).setState(CutoffState.CLOSED)));
        cutoffs.add(cutoffRepository.insert(createCutoff(SHOP_SKU_KEY2, ERROR_TYPE2)));
        List<OfferCutoff> closedCutoffs = cutoffs.stream()
            .filter(c -> c.getState() == CutoffState.CLOSED).collect(Collectors.toList());

        List<OfferCutoff> foundCutoffs = cutoffRepository.findBy(new OfferCutoffFilter().setState(CutoffState.CLOSED));
        assertThat(foundCutoffs).containsExactlyInAnyOrderElementsOf(closedCutoffs);
    }

    @Test
    public void testSearchBySskuKeys() {
        OfferCutoff co1 = createCutoff(SHOP_SKU_KEY1, ERROR_TYPE1).setState(CutoffState.CLOSED);
        OfferCutoff co2 = createCutoff(SHOP_SKU_KEY2, ERROR_TYPE2);
        OfferCutoff co3 = createCutoff(SHOP_SKU_KEY3, ERROR_TYPE2);
        cutoffRepository.insertBatch(co1, co2, co3);

        List<OfferCutoff> cutoffs = List.of(co1, co3);

        List<OfferCutoff> foundCutoffs = cutoffRepository.findBy(new OfferCutoffFilter().setSskuKeys(
            List.of(SHOP_SKU_KEY1, SHOP_SKU_KEY3)));
        assertThat(foundCutoffs).containsExactlyInAnyOrderElementsOf(cutoffs);
    }

    @Test
    public void testSearchBySskuKeysWithOtherFilters() {
        OfferCutoff co1 = createCutoff(SHOP_SKU_KEY1, ERROR_TYPE1).setState(CutoffState.CLOSED);
        OfferCutoff co2 = createCutoff(SHOP_SKU_KEY1, ERROR_TYPE2);
        OfferCutoff co3 = createCutoff(SHOP_SKU_KEY2, ERROR_TYPE1);
        OfferCutoff co4 = createCutoff(SHOP_SKU_KEY2, ERROR_TYPE2).setState(CutoffState.CLOSED);
        OfferCutoff co5 = createCutoff(SHOP_SKU_KEY3, ERROR_TYPE2);
        cutoffRepository.insertBatch(co1, co2, co3, co4, co5);

        List<OfferCutoff> cutoffsByType = List.of(co2, co5);

        List<OfferCutoff> foundCutoffsByType = cutoffRepository.findBy(new OfferCutoffFilter()
            .setSskuKeys(List.of(SHOP_SKU_KEY1, SHOP_SKU_KEY3))
            .setTypeId(ERROR_TYPE2));
        assertThat(foundCutoffsByType).containsExactlyInAnyOrderElementsOf(cutoffsByType);

        List<OfferCutoff> cutoffsByTypeAndState = List.of(co3);

        List<OfferCutoff> foundCutoffsByTypeAndState = cutoffRepository.findBy(new OfferCutoffFilter()
            .setSskuKeys(List.of(SHOP_SKU_KEY1, SHOP_SKU_KEY2))
            .setTypeId(ERROR_TYPE1)
            .setState(CutoffState.OPEN));
        assertThat(foundCutoffsByTypeAndState).containsExactlyInAnyOrderElementsOf(cutoffsByTypeAndState);
    }

    private OfferCutoff createCutoff(ShopSkuKey shopSkuKey) {
        return createCutoff(shopSkuKey, ERROR_TYPE1);
    }

    private OfferCutoff createCutoff(ShopSkuKey shopSkuKey, String typeId) {
        return new OfferCutoff()
            .setShopSku(shopSkuKey.getShopSku())
            .setSupplierId(shopSkuKey.getSupplierId())
            .setTypeId(typeId)
            .setErrorCode(typeId)
            .setState(CutoffState.OPEN);
    }

    private MboMappings.ApprovedMappingInfo approvedOffer(int supplierId, String shopSku) {
        return TestDataUtils.generateCorrectApprovedMappingInfoBuilder(random)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .build();
    }
}
