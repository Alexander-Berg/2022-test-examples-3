package ru.yandex.market.abo.core.ticket.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.ticket.model.OfferDeliverySnapshot
import ru.yandex.market.common.report.model.LocalDeliveryOption
import java.time.LocalDateTime

internal class OfferDeliverySnapshotRepoTest @Autowired constructor(
    private val offerDeliverySnapshotRepo: OfferDeliverySnapshotRepo
) : EmptyTest() {

    @Test
    fun deleteAllByTicketIdTest() {
        val offerDeliverySnapshots = listOf(
            OfferDeliverySnapshot(LocalDeliveryOption(), 1, 1),
            OfferDeliverySnapshot(LocalDeliveryOption(), 1, 2)
        )
        offerDeliverySnapshotRepo.saveAll(offerDeliverySnapshots)
        flushAndClear()
        offerDeliverySnapshotRepo.deleteAllByTicketId(2)
        flushAndClear()
        val dbOfferDeliverySnapshots = offerDeliverySnapshotRepo.findAll()
        assertEquals(1, dbOfferDeliverySnapshots.size)
        assertEquals(offerDeliverySnapshots[0].id, dbOfferDeliverySnapshots[0].id)
    }

    @Test
    fun deleteAllByCreationTimeBeforeTest() {
        val now = LocalDateTime.now()
        val offerDeliverySnapshots = listOf(
            OfferDeliverySnapshot(LocalDeliveryOption(), 1).apply {
                creationTime = now
            },
            OfferDeliverySnapshot(LocalDeliveryOption(), 1).apply {
                creationTime = now.minusDays(2)
            },
        )
        offerDeliverySnapshotRepo.saveAll(offerDeliverySnapshots)
        flushAndClear()
        offerDeliverySnapshotRepo.deleteAllByCreationTimeBefore(now.minusDays(1))
        flushAndClear()
        val dbOfferDeliverySnapshots = offerDeliverySnapshotRepo.findAll()
        assertEquals(1, dbOfferDeliverySnapshots.size)
        assertEquals(offerDeliverySnapshots[0].id, dbOfferDeliverySnapshots[0].id)
    }
}
