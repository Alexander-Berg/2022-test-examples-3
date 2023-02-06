package ru.yandex.market.abo.tms.lms

import java.time.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.lms.model.LmsPartnerMovement
import ru.yandex.market.abo.cpa.lms.model.LmsPartnerMovement.Key
import ru.yandex.market.abo.cpa.lms.model.LmsPartnerMovementRepo
import ru.yandex.market.abo.cpa.lms.model.LmsPartnerSchedule
import ru.yandex.market.abo.cpa.lms.model.LmsPartnerWorkingDay
import ru.yandex.market.abo.cpa.lms.model.ShipmentType.WITHDRAW
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 27.06.2022
 */
class LMSPartnerMovementBatchUpdaterTest @Autowired constructor(
    private val lmsPartnerMovementBatchUpdater: PgBatchUpdater<LmsPartnerMovement>,
    private val lmsPartnerMovementRepo: LmsPartnerMovementRepo
): EmptyTest() {
    @Test
    fun `movements insert test`() {
        val movement = LmsPartnerMovement(
            Key(sourcePartnerId = 123L, destinationPartnerId = 124L),
            movementPartnerId = 125L,
            sourceLogisticPointId = 11111,
            destinationLogisticPointId = 11112,
            LmsPartnerSchedule(listOf(
                LmsPartnerWorkingDay(1, LocalTime.of(10, 0), LocalTime.of(19, 0)),
                LmsPartnerWorkingDay(2, LocalTime.of(10, 0), LocalTime.of(19, 0)),
                LmsPartnerWorkingDay(3, LocalTime.of(10, 0), LocalTime.of(19, 0))
            )),
            LocalTime.of(15, 0),
            WITHDRAW
        )

        lmsPartnerMovementBatchUpdater.insertWithoutUpdate(listOf(movement))
        flushAndClear()

        assertThat(lmsPartnerMovementRepo.findFirstByKeySourcePartnerId(partnerId = 123L))
            .usingRecursiveComparison()
            .isEqualTo(movement)
    }
}
