package ru.yandex.market.tpl.core.domain.surcharge.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.tpl.core.domain.surcharge.entity.Surcharge
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeResolution
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeValidationStatus
import ru.yandex.market.tpl.core.domain.surcharge.repository.SurchargeRepository
import ru.yandex.market.tpl.core.test.TplAbstractTest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal class SurchargeQueryServiceTest : TplAbstractTest() {

    @Autowired
    private lateinit var surchargeRepository: SurchargeRepository
    @Autowired
    private lateinit var surchargeQueryService: SurchargeQueryService

    @BeforeEach
    fun beforeEach() {
        surchargeRepository.deleteAll()
    }

    @Test
    fun `findAll by trackerTicket`() {
        surchargeRepository.save(getSurcharge("ticket-1"))
        surchargeRepository.save(getSurcharge("ticket-2"))

        val result = surchargeQueryService.findAll("ticket-1")

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `findAll by createDate`() {
        surchargeRepository.save(getSurcharge("ticket-1"))
        surchargeRepository.save(getSurcharge("ticket-2"))
        surchargeRepository.save(getSurcharge("ticket-3"))

        val createDate = LocalDate.now()

        surchargeQueryService.findAll(
            createDate,
            SurchargeValidationStatus.VALID,
            pageNumber = 0,
            pageSize = 2,
        ).let {
            assertThat(it.totalPages).isEqualTo(2)
            assertThat(it.pageable.pageNumber).isEqualTo(0)
            assertThat(it.pageable.pageSize).isEqualTo(2)
            assertThat(it.content.size).isEqualTo(2)
        }

        surchargeQueryService.findAll(
            createDate,
            SurchargeValidationStatus.VALID,
            pageNumber = 1,
            pageSize = 2,
        ).let {
            assertThat(it.totalPages).isEqualTo(2)
            assertThat(it.pageable.pageNumber).isEqualTo(1)
            assertThat(it.pageable.pageSize).isEqualTo(2)
            assertThat(it.content.size).isEqualTo(1)
        }
    }

    private fun getSurcharge(trackerTicket: String) = Surcharge(
        id = UUID.randomUUID().toString(),
        validationStatus = SurchargeValidationStatus.VALID,
        validationErrors = null,
        resolution = SurchargeResolution.COMMIT,
        type = "test type",
        cargoType = "cargo type",
        eventDate = LocalDate.of(2021, 2, 5),
        companyId = 1,
        scId = 2,
        userId = 3,
        userShiftId = 2,
        amount = BigDecimal.TEN,
        multiplier = 1,
        trackerTicket = trackerTicket,
        trackerChangelogId = "changelog-id",
    )

}
