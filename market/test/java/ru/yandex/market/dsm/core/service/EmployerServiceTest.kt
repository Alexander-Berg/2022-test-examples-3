package ru.yandex.market.dsm.core.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.exception.BadRequestException
import ru.yandex.market.dsm.core.exception.EntityAlreadyExistsException
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.employer.service.EmployerQueryService
import ru.yandex.market.dsm.domain.employer.service.EmployerService
import ru.yandex.market.dsm.test.AssertsEmployerFactory
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.mj.generated.server.model.EmployerUpsertDto

class EmployerServiceTest : AbstractTest() {

    @Autowired
    private lateinit var employerService: EmployerService

    @Autowired
    private lateinit var employerQueryService: EmployerQueryService

    @Test
    fun `createEmployer`() {
        //given
        val upsertDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        upsertDto.id = null

        //when
        val createdId = employerService.createEmployer(upsertDto)
        val projectionByIdO = employerQueryService.findById(createdId)

        //then
        assertThat(createdId).isNotNull
        assertThat(projectionByIdO).isNotNull
        AssertsEmployerFactory.asserts(projectionByIdO!!, upsertDto)
    }

    @Test
    fun `createEmployer - employerContactInfo is nullable`() {
        //given
        val upsertDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        upsertDto.id = null
        upsertDto.employerContactInfo = null

        //when
        val createdId = employerService.createEmployer(upsertDto)
        val projectionByIdO = employerQueryService.findById(createdId)

        //then
        assertThat(createdId).isNotNull
        assertThat(projectionByIdO).isNotNull
        AssertsEmployerFactory.asserts(projectionByIdO!!, upsertDto)
    }

    @Test
    fun `createEmployer_idempotence`() {
        //create_employer
        val upsertDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        upsertDto.id = null
        val createdId1 = employerService.createEmployer(upsertDto)

        //check_login_unique
        val dtoName = upsertDto.name
        upsertDto.name = upsertDto.name + "a"
        assertThrows<EntityAlreadyExistsException> {
            employerService.createEmployer(upsertDto)
        }
        upsertDto.name = dtoName

        //check_name_unique
        upsertDto.login = upsertDto.login + "a"
        assertThrows<BadRequestException> {
            employerService.createEmployer(upsertDto)
        }
    }

    @Test
    fun `updateEmployer`() {
        //given
        val createdDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        createdDto.id = null

        //when
        val createdId = employerService.createEmployer(createdDto)
        assertThat(createdId).isNotEmpty

        val updatedDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        updatedDto.id(createdId)
        val updatedId = employerService.updateEmployer(updatedDto)

        //then
        assertThat(createdId).isEqualTo(updatedId);
        val projectionByIdO = employerQueryService.findById(updatedId)
        assertThat(projectionByIdO).isNotNull
        AssertsEmployerFactory.asserts(projectionByIdO!!, updatedDto)

        //check_unique
        val createdDto2 = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        createdDto2.id = null
        val createdId2 = employerService.createEmployer(createdDto2)
        assertThat(createdId2).isNotEmpty

        //check_login_unique
        val updatedDto2 = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        updatedDto2.id(createdId)
        val login2 = updatedDto2.login
        updatedDto2.login = createdDto2.login
        assertThrows<BadRequestException> {
            employerService.updateEmployer(updatedDto2)
        }
        updatedDto2.login = login2

        //check_name_unique
        updatedDto2.name = createdDto2.name
        assertThrows<BadRequestException> {
            employerService.updateEmployer(updatedDto2)
        }
    }

    @Test
    fun `updateEmployer - employerContactInfo is nullable`() {
        //given
        val createdDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        createdDto.id = null

        //when
        val createdId = employerService.createEmployer(createdDto)
        assertThat(createdId).isNotEmpty

        val updatedDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        updatedDto.id =createdId
        updatedDto.employerContactInfo = null
        val updatedId = employerService.updateEmployer(updatedDto)

        //then
        assertThat(createdId).isEqualTo(updatedId);
        val projectionByIdO = employerQueryService.findById(updatedId)
        assertThat(projectionByIdO).isNotNull
        AssertsEmployerFactory.asserts(projectionByIdO!!, updatedDto)
    }

}
