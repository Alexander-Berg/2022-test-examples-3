package ru.yandex.market.abo.core.export.hidden.snapshot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.offer.hidden.HidingReason;
import ru.yandex.market.abo.core.export.hidden.snapshot.white.HiddenOfferSnapshot;
import ru.yandex.market.abo.core.export.hidden.snapshot.white.HiddenOfferSnapshotService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * @date 12/06/19.
 */
class HiddenOfferSnapshotServiceTest extends EmptyTest {
    @Autowired
    private HiddenOfferSnapshotService snapshotService;
    private final Map<Boolean, List<HiddenOfferSnapshot>> hidingUpdates = new HashMap<>();

    @BeforeEach
    void setUp() {
        assertTrue(snapshotService.findAll().isEmpty());
        hidingUpdates.clear();
    }

    @Test
    void updateWithNew() {
        List<HiddenOfferSnapshot> shouldHide = new ArrayList<>(List.of(someSnapshot(), someSnapshot()));
        hidingUpdates.put(true, shouldHide);

        snapshotService.updateSnapshot(hidingUpdates);
        assertEquals(shouldHide, snapshotService.findAll());
    }

    @Test
    void updateWithGone() {
        var snapshot = someSnapshot();
        snapshot.setDeleted(false);
        List<HiddenOfferSnapshot> snapshotList = List.of(snapshot);
        hidingUpdates.put(true, snapshotList);

        snapshotService.updateSnapshot(hidingUpdates);
        assertEquals(snapshotList, snapshotService.findAll());

        hidingUpdates.clear();

        snapshot.setDeleted(true);
        snapshotList = List.of(snapshot);
        hidingUpdates.put(false, snapshotList);

        snapshotService.updateSnapshot(hidingUpdates);
        assertTrue(snapshotService.findAll().get(0).deleted);
    }

    @Test
    void updateWithEmpty() {
        hidingUpdates.put(true, List.of(someSnapshot()));
        hidingUpdates.put(false, List.of(someSnapshot()));
        snapshotService.updateSnapshot(hidingUpdates);

        List<HiddenOfferSnapshot> snapshot = snapshotService.findAll();
        assertFalse(snapshot.isEmpty());

        hidingUpdates.clear();
        snapshotService.updateSnapshot(hidingUpdates);
        assertEquals(snapshot, snapshotService.findAll());
    }

    @Test
    void updateManyDeleted() {
        var remainedSnapshot1 = someSnapshot();
        remainedSnapshot1.setCreationTime(LocalDateTime.now().minusDays(2));
        var remainedSnapshot2 = someSnapshot();
        remainedSnapshot2.setCreationTime(LocalDateTime.now().minusDays(2));
        var deletedSnapshot1 = someSnapshot();
        deletedSnapshot1.setCreationTime(LocalDateTime.now().minusDays(2));
        var deletedSnapshot2 = someSnapshot();
        deletedSnapshot2.setCreationTime(LocalDateTime.now().minusDays(2));
        var addedSnapshot1 = someSnapshot();
        var addedSnapshot2 = someSnapshot();

        List<HiddenOfferSnapshot> initialSnapshots = List.of(
                remainedSnapshot1,
                remainedSnapshot2,
                deletedSnapshot1,
                deletedSnapshot2
        );
        hidingUpdates.put(true, initialSnapshots);

        snapshotService.updateSnapshot(hidingUpdates);
        flushAndClear();

        assertEquals(initialSnapshots, snapshotService.findAll());

        hidingUpdates.clear();

        List<HiddenOfferSnapshot> addedSnapshots = List.of(
                addedSnapshot1,
                addedSnapshot2
        );
        deletedSnapshot1.setDeleted(true);
        deletedSnapshot2.setDeleted(true);
        List<HiddenOfferSnapshot> deletedSnapshots = List.of(
                deletedSnapshot1,
                deletedSnapshot2
        );
        hidingUpdates.put(true, addedSnapshots);
        hidingUpdates.put(false, deletedSnapshots);

        snapshotService.updateSnapshot(hidingUpdates);
        flushAndClear();
        List<HiddenOfferSnapshot> expected = List.of(
                remainedSnapshot1,
                remainedSnapshot2,
                deletedSnapshot1,
                deletedSnapshot2,
                addedSnapshot1,
                addedSnapshot2
        );
        List<HiddenOfferSnapshot> result = snapshotService.findAll();
        assertEquals(expected, result);

        List<Long> expectedDeletedShopIds = List.of(
                deletedSnapshot1.getShopId(),
                deletedSnapshot2.getShopId()
        );
        List<Long> resultDeletedShopIds = StreamEx.of(result)
                .filter(x -> x.getCreationTime().isAfter(LocalDateTime.now().minusDays(1)))
                .filter(OfferSnapshot::getDeleted)
                .map(HiddenOfferSnapshot::getShopId)
                .toList();
        assertEquals(expectedDeletedShopIds, resultDeletedShopIds);
    }

    @Test
    void deleteOldRemovedHidings() {
        var snapshot = someSnapshot();
        snapshot.setDeleted(false);
        snapshot.setCreationTime(LocalDateTime.now().minusDays(10));
        hidingUpdates.put(true, List.of(snapshot));
        snapshotService.updateSnapshot(hidingUpdates);

        assertEquals(snapshot, snapshotService.findAll().get(0));

        flushAndClear();
        snapshotService.deleteOldSnapshotRows(7);
        flushAndClear();
        assertEquals(snapshot, snapshotService.findAll().get(0));
    }

    private static HiddenOfferSnapshot someSnapshot() {
        HiddenOfferSnapshot snapshot = new HiddenOfferSnapshot();
        snapshot.setFeedId(RND.nextLong());
        snapshot.setOfferId(String.valueOf(RND.nextLong()));
        snapshot.setShopId(RND.nextLong());
        snapshot.setSourceId(RND.nextLong());
        snapshot.setHidingReason(HidingReason.BAD_QUALITY);
        snapshot.setRgb(RND.nextInt(3));
        snapshot.setDeleted(false);
        return snapshot;
    }
}
