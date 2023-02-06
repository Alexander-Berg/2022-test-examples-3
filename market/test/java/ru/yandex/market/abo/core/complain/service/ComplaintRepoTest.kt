package ru.yandex.market.abo.core.complain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.complain.model.Complaint
import java.time.LocalDateTime

class ComplaintRepoTest @Autowired constructor(
    private val complaintRepo: ComplaintRepo
) : EmptyTest() {
    @Test
    fun deleteAllByDeletedTrueAndDeletionTimeBefore() {
        val now = LocalDateTime.now()
        val complaints = listOf(
            Complaint().apply {
                setDeleted(false)
                setDeletionTime(now)
            },
            Complaint().apply {
                setDeleted(false)
                setDeletionTime(now.minusDays(2))
            },
            Complaint().apply {
                setDeleted(true)
                setDeletionTime(now)
            },
            Complaint().apply {
                setDeleted(true)
                setDeletionTime(now.minusDays(2))
            },
        )
        complaintRepo.saveAll(complaints)
        flushAndClear()
        complaintRepo.deleteAllByDeletedTrueAndDeletionTimeBefore(now.minusDays(1))
        flushAndClear()
        val dbComplaints = complaintRepo.findAll()
        assertEquals(dbComplaints, complaints.take(3))
    }
}
