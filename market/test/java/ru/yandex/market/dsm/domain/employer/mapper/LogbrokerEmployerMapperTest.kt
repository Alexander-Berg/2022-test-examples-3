package ru.yandex.market.dsm.domain.employer.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.employer.db.EmployerDboRepository
import ru.yandex.market.dsm.domain.employer.model.EmployerType
import ru.yandex.market.dsm.domain.employer.service.EmployerService
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.mj.generated.server.model.EmployerUpsertDto
import ru.yandex.mj.generated.server.model.LogbrokerEmployerTypeDto

internal class LogbrokerEmployerMapperTest : AbstractTest() {
    @Autowired
    private lateinit var employerService: EmployerService
    @Autowired
    private lateinit var employerDboRepository: EmployerDboRepository
    @Autowired
    private lateinit var logbrokerEmployerMapper: LogbrokerEmployerMapper
    @Autowired
    private lateinit var trasactionTemplate: TransactionTemplate

    @Test
    fun `map - success`() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)

        trasactionTemplate.execute {
            val employer = employerDboRepository.getOne(
                employerService.createEmployer(upsertEmployer)
            )

            val result = logbrokerEmployerMapper.map(employer)
            assertThat(result.id).isEqualTo(employer.id)
            assertThat(result.version).isEqualTo(employer.updatedAt.toEpochMilli())
            assertThat(result.type).isEqualTo(mapToEmployerTypeDto(employer.type))
            assertThat(result.name).isEqualTo(employer.name)
            assertThat(result.login).isEqualTo(employer.login)
            assertThat(result.active).isEqualTo(employer.isActive)
        }
    }

    private fun mapToEmployerTypeDto(type: EmployerType) =
        when (type) {
            EmployerType.LINEHAUL -> LogbrokerEmployerTypeDto.LINEHAUL
            EmployerType.SUPPLY -> LogbrokerEmployerTypeDto.SUPPLY
        }
}
