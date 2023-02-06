package ru.yandex.market.tpl.core.domain.surcharge.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.tpl.core.domain.surcharge.entity.Surcharge
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeResolution
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeValidationStatus
import ru.yandex.market.tpl.core.test.TplAbstractTest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class SurchargeRepositoryTest : TplAbstractTest() {

    @Autowired
    private lateinit var surchargeRepository: SurchargeRepository
    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun `create - success`() {
        val id = UUID.randomUUID().toString()

        val expected = Surcharge(
            id = id,
            validationStatus = SurchargeValidationStatus.INVALID,
            validationErrors = "some errors",
            resolution = SurchargeResolution.COMMIT,
            type = "type test",
            cargoType = "cargo type test",
            eventDate = LocalDate.of(2021, 1, 1),
            companyId = 7,
            scId = 5,
            userId = 3,
            userShiftId = 2,
            amount = BigDecimal.ZERO,
            multiplier = 1,
            trackerTicket = "TEST-01",
            trackerChangelogId = "abc",
        )

        surchargeRepository.save(expected)

        transactionTemplate.execute {
            val result = surchargeRepository.getById(id)

            assertThat(result).isEqualTo(expected)
        }
    }
}
