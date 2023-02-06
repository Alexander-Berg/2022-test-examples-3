package ru.yandex.travel.cpa.data_processing.flow.processors

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.travel.cpa.data_processing.flow.model.orders.OrderKey
import ru.yandex.travel.cpa.data_processing.flow.model.snapshots.Snapshot

class SnapshotProcessorTest {

    private val snapshotComparator = compareBy(Snapshot::getPartnerName, Snapshot::getPartnerOrderId, Snapshot::getHash, Snapshot::getUpdatedAt)
    private val orderComparator = compareBy(OrderKey::getPartnerName, OrderKey::getPartnerOrderId)
    private val partnerName: String = "partner1"

    private fun makeSnapshot(partnerOrderId: String, hash: String, updated_at: Long): Snapshot {
        return Snapshot(this.partnerName, partnerOrderId, hash, updated_at, ByteArray(0))
    }

    private fun makeOrderKey(partnerOrderId: String): OrderKey {
        return OrderKey(this.partnerName, partnerOrderId)
    }

    private fun makeSnapshotReader(l: List<Snapshot>): (List<OrderKey>) -> List<Snapshot> {
        return fun (keys: List<OrderKey>): List<Snapshot> {
            val allowedKeys = keys.toSet()
            return l.filter { allowedKeys.contains(OrderKey(it)) }
        }

    }

    private fun getProcessedSnapshots(snapshots: Map<OrderKey, List<Snapshot>>): Map<OrderKey, Snapshot> {
        return snapshots
            .mapValues { it.value.elementAtOrNull(it.value.size - 1) }
            .filterValues { it != null }
            .mapValues { it.value!! }
    }

    private fun checkCase(
        existing: List<Snapshot>,
        incoming: List<Snapshot>,
        expectedNew: List<Snapshot>,
        expectedToDelete: List<Snapshot>,
        expectedOrdersToUpdate: List<OrderKey>,
        expectedProcessedSnapshots: List<Snapshot>,
        expectedDeduplicatedCount: Int,
        expectedNewCount: Int,
        expectedOldCount: Int,
    ) {
        val groupedExistingSnapshots = SnapshotProcessor.getGroupedSnapshots(existing)
        val groupedIncomingSnapshots = SnapshotProcessor.getGroupedSnapshots(incoming)
        val processedSnapshots = getProcessedSnapshots(groupedExistingSnapshots)
        val deduplicatedSnapshots = SnapshotProcessor.getDeduplicatedSnapshots(
            groupedIncomingSnapshots,
            processedSnapshots,
            makeSnapshotReader(existing),
        )
        val newSnapshots = HashMap<OrderKey, List<Snapshot>>()
        for ( item in deduplicatedSnapshots.newSnapshots) {
            newSnapshots[item.key] = item.value.sortedWith(snapshotComparator)
        }
        assertThat(newSnapshots).isEqualTo(SnapshotProcessor.getGroupedSnapshots(expectedNew))
        assertThat(deduplicatedSnapshots.snapshotsToDelete.sortedWith(snapshotComparator)).isEqualTo(expectedToDelete)
        assertThat(deduplicatedSnapshots.ordersToUpdate.sortedWith(orderComparator)).isEqualTo(expectedOrdersToUpdate)
        assertThat(deduplicatedSnapshots.processedSnapshots.sortedWith(snapshotComparator)).isEqualTo(expectedProcessedSnapshots)
        assertThat(deduplicatedSnapshots.deduplicatedCount).isEqualTo(expectedDeduplicatedCount)
        assertThat(deduplicatedSnapshots.newCount).isEqualTo(expectedNewCount)
        assertThat(deduplicatedSnapshots.oldCount).isEqualTo(expectedOldCount)
    }

    @Test
    @Throws(Exception::class)
    fun testNewOnlySingle() {
        checkCase(
            existing = listOf(),
            incoming = listOf(
                makeSnapshot("order1", "hash_1_1", 1),
            ),
            expectedNew = listOf(
                makeSnapshot("order1", "hash_1_1", 1),
            ),
            expectedToDelete = listOf(),
            expectedOrdersToUpdate = listOf(
                makeOrderKey("order1"),
            ),
            expectedProcessedSnapshots = listOf(
                makeSnapshot("order1", "hash_1_1", 1),
            ),
            expectedDeduplicatedCount = 1,
            expectedNewCount = 1,
            expectedOldCount = 0,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testNewOnlyDouble() {
        checkCase(
            existing = listOf(),
            incoming = listOf(
                makeSnapshot("order1", "hash_1_1", 1),
                makeSnapshot("order1", "hash_1_2", 2),
            ),
            expectedNew = listOf(
                makeSnapshot("order1", "hash_1_1", 1),
                makeSnapshot("order1", "hash_1_2", 2),
            ),
            expectedToDelete = listOf(),
            expectedOrdersToUpdate = listOf(
                makeOrderKey("order1"),
            ),
            expectedProcessedSnapshots = listOf(
                makeSnapshot("order1", "hash_1_2", 2),
            ),
            expectedDeduplicatedCount = 2,
            expectedNewCount = 2,
            expectedOldCount = 0,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testExisting() {
        checkCase(
            existing = listOf(
                makeSnapshot("order1", "hash_1_1", 1),
            ),
            incoming = listOf(
                makeSnapshot("order1", "hash_1_1", 1),
                makeSnapshot("order1", "hash_1_2", 2),
            ),
            expectedNew = listOf(
                makeSnapshot("order1", "hash_1_2", 2),
            ),
            expectedToDelete = listOf(),
            expectedOrdersToUpdate = listOf(
                makeOrderKey("order1"),
            ),
            expectedProcessedSnapshots = listOf(
                makeSnapshot("order1", "hash_1_2", 2),
            ),
            expectedDeduplicatedCount = 2,
            expectedNewCount = 2,
            expectedOldCount = 0,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testOldInsert() {
        checkCase(
            existing = listOf(
                makeSnapshot("order1", "hash_1_1", 1),
                makeSnapshot("order1", "hash_1_3", 3),
            ),
            incoming = listOf(
                makeSnapshot("order1", "hash_1_2", 2),
            ),
            expectedNew = listOf(
                makeSnapshot("order1", "hash_1_2", 2),
            ),
            expectedToDelete = listOf(
            ),
            expectedOrdersToUpdate = listOf(
                makeOrderKey("order1"),
            ),
            expectedProcessedSnapshots = listOf(
            ),
            expectedDeduplicatedCount = 1,
            expectedNewCount = 0,
            expectedOldCount = 1,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testOldUpdate() {
        checkCase(
            existing = listOf(
                makeSnapshot("order1", "hash_1_1", 1),
                makeSnapshot("order1", "hash_1_3", 4),
            ),
            incoming = listOf(
                makeSnapshot("order1", "hash_1_2", 2),
                makeSnapshot("order1", "hash_1_3", 3),
                makeSnapshot("order1", "hash_1_4", 4),
            ),
            expectedNew = listOf(
                makeSnapshot("order1", "hash_1_2", 2),
                makeSnapshot("order1", "hash_1_3", 3),
                makeSnapshot("order1", "hash_1_4", 4),
            ),
            expectedToDelete = listOf(
                makeSnapshot("order1", "hash_1_3", 4),
            ),
            expectedOrdersToUpdate = listOf(
                makeOrderKey("order1"),
            ),
            expectedProcessedSnapshots = listOf(
                makeSnapshot("order1", "hash_1_4", 4),
            ),
            expectedDeduplicatedCount = 3,
            expectedNewCount = 0,
            expectedOldCount = 3,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testMultipleOrders() {
        checkCase(
            existing = listOf(
                makeSnapshot("order1", "hash_1_1", 1),
                makeSnapshot("order2", "hash_2_1", 2),
            ),
            incoming = listOf(
                makeSnapshot("order1", "hash_1_2", 3),
                makeSnapshot("order2", "hash_2_2", 4),
            ),
            expectedNew = listOf(
                makeSnapshot("order1", "hash_1_2", 3),
                makeSnapshot("order2", "hash_2_2", 4),
            ),
            expectedToDelete = listOf(),
            expectedOrdersToUpdate = listOf(
                makeOrderKey("order1"),
                makeOrderKey("order2"),
            ),
            expectedProcessedSnapshots = listOf(
                makeSnapshot("order1", "hash_1_2", 3),
                makeSnapshot("order2", "hash_2_2", 4),
            ),
            expectedDeduplicatedCount = 2,
            expectedNewCount = 2,
            expectedOldCount = 0,
        )
    }
}
