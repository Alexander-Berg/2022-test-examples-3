package ru.yandex.market.tpl.core.domain.surcharge.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.tpl.core.domain.company.Company
import ru.yandex.market.tpl.core.domain.partner.SortingCenter
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeResolution
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeType
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeValidationStatus
import ru.yandex.market.tpl.core.domain.surcharge.repository.SurchargeRepository
import ru.yandex.market.tpl.core.domain.surcharge.repository.SurchargeTypeRepository
import ru.yandex.market.tpl.core.domain.surcharge.service.model.SurchargeCreateRequest
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand
import ru.yandex.market.tpl.core.test.TplAbstractTest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal class SurchargeEditServiceTest : TplAbstractTest() {

    @Autowired
    private lateinit var userHelper: TestUserHelper
    @Autowired
    private lateinit var commandService: UserShiftCommandService
    @Autowired
    private lateinit var surchargeRepository: SurchargeRepository
    @Autowired
    private lateinit var surchargeTypeRepository: SurchargeTypeRepository
    @Autowired
    private lateinit var surchargeEditService: SurchargeEditService

    private lateinit var surchargeTypeNoShift: SurchargeType
    private lateinit var surchargeTypeShiftRequired: SurchargeType
    private lateinit var company: Company
    private lateinit var sc: SortingCenter

    @BeforeEach
    fun beforeEach() {
        surchargeRepository.deleteAll()
        surchargeTypeRepository.deleteAll()

        surchargeTypeNoShift = surchargeTypeRepository.save(
            SurchargeType(
                id = UUID.randomUUID().toString(),
                code = "test-code",
                name = "test",
                type = SurchargeType.Type.PENALTY,
                description = "test",
                userShiftIsRequired = false,
                deleted = false,
            )
        )
        surchargeTypeShiftRequired = surchargeTypeRepository.save(
            SurchargeType(
                id = UUID.randomUUID().toString(),
                code = "test-code-2",
                name = "test-2",
                type = SurchargeType.Type.PENALTY,
                description = "test-2",
                userShiftIsRequired = true,
                deleted = false,
            )
        )

        company = userHelper.findOrCreateCompany("test-company")
        sc = userHelper.sortingCenter(SortingCenter.DEFAULT_SC_ID)
    }

    @Test
    fun `create - status invalid for type with userShiftIsRequired = true`() {
        val user = userHelper.findOrCreateUser(123L)

        val result = surchargeEditService.create(
            getSurchargeCreateRequest(
                surchargeTypeShiftRequired.code,
                company.id,
                sc.id,
                user.id,
                LocalDate.of(2021, 2, 5)
            )
        )

        assertThat(result.validationStatus).isEqualTo(SurchargeValidationStatus.INVALID)
    }

    @Test
    fun `create - status invalid if user doesnt exist`() {
        val result = surchargeEditService.create(
            getSurchargeCreateRequest(
                surchargeTypeNoShift.code,
                company.id,
                sc.id,
                -15,
                LocalDate.of(2021, 2, 5)
            )
        )

        assertThat(result.validationStatus).isEqualTo(SurchargeValidationStatus.INVALID)
    }

    @Test
    fun `create - status invalid if sc doesnt exist`() {
        val result = surchargeEditService.create(
            getSurchargeCreateRequest(
                surchargeTypeNoShift.code,
                company.id,
                -999,
                null,
                LocalDate.of(2021, 2, 5)
            )
        )

        assertThat(result.validationStatus).isEqualTo(SurchargeValidationStatus.INVALID)
    }

    @Test
    fun `create - status invalid if company doesnt exist`() {
        val result = surchargeEditService.create(
            getSurchargeCreateRequest(
                surchargeTypeNoShift.code,
                -999,
                sc.id,
                null,
                LocalDate.of(2021, 2, 5)
            )
        )

        assertThat(result.validationStatus).isEqualTo(SurchargeValidationStatus.INVALID)
    }

    @Test
    fun `create - status invalid if multiplier is negative`() {
        val result = surchargeEditService.create(
            getSurchargeCreateRequest(
                surchargeTypeNoShift.code,
                company.id,
                sc.id,
                null,
                LocalDate.of(2021, 2, 5)
            ).copy(
                multiplier = -1
            )
        )

        assertThat(result.validationStatus).isEqualTo(SurchargeValidationStatus.INVALID)
    }

    @Test
    fun `create - status invalid if amount is negative`() {
        val result = surchargeEditService.create(
            getSurchargeCreateRequest(
                surchargeTypeNoShift.code,
                company.id,
                sc.id,
                null,
                LocalDate.of(2021, 2, 5)
            ).copy(
                amount = BigDecimal.valueOf(-1)
            )
        )

        assertThat(result.validationStatus).isEqualTo(SurchargeValidationStatus.INVALID)
    }

    @Test
    fun `create - status valid if no user`() {
        val result = surchargeEditService.create(
            getSurchargeCreateRequest(
                surchargeTypeNoShift.code,
                company.id,
                sc.id,
                null,
                LocalDate.of(2021, 2, 5)
            )
        )

        assertThat(result.validationStatus).isEqualTo(SurchargeValidationStatus.VALID)
    }

    @Test
    fun `create - status valid for type with userShiftIsRequired = false`() {
        val user = userHelper.findOrCreateUser(123L)

        val result = surchargeEditService.create(
            getSurchargeCreateRequest(
                surchargeTypeNoShift.code,
                company.id,
                sc.id,
                user.id,
                LocalDate.of(1990, 2, 5)
            )
        )

        assertThat(result.validationStatus).isEqualTo(SurchargeValidationStatus.VALID)
    }

    @Test
    fun `create - status valid for type with userShiftIsRequired = true`() {
        val user = userHelper.findOrCreateUser(123L)
        val shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock))
        val userShiftId = commandService.createUserShift(
            UserShiftCommand.Create.builder()
                .userId(user.id)
                .shiftId(shift.id)
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build()
        )

        val result = surchargeEditService.create(
            getSurchargeCreateRequest(
                surchargeTypeShiftRequired.code,
                company.id,
                shift.sortingCenter.id,
                user.id,
                shift.shiftDate
            )
        )

        assertThat(result.validationStatus).isEqualTo(SurchargeValidationStatus.VALID)
        assertThat(result.userShiftId).isEqualTo(userShiftId)
    }

    @Test
    fun `create - status invalid for non existing type`() {
        val result = surchargeEditService.create(
            getSurchargeCreateRequest(
                "non-existing-type",
                company.id,
                sc.id,
                null,
                LocalDate.of(2021, 2, 5)
            )
        )

        assertThat(result.validationStatus).isEqualTo(SurchargeValidationStatus.INVALID)
    }

    private fun getSurchargeCreateRequest(
        type: String,
        companyId: Long,
        scId: Long,
        userId: Long?,
        eventDate: LocalDate
    ) = SurchargeCreateRequest(
        resolution = SurchargeResolution.COMMIT,
        type = type,
        cargoType = "cargo type",
        eventDate = eventDate,
        companyId = companyId,
        scId = scId,
        userId = userId,
        amount = BigDecimal.TEN,
        multiplier = 1,
        trackerTicket = "TEST-01",
        trackerChangelogId = "changelog-id",
    )

}
