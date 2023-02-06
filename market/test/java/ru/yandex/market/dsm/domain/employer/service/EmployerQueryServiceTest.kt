package ru.yandex.market.dsm.domain.employer.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.employer.db.EmployerDbo
import ru.yandex.market.dsm.domain.employer.db.EmployerDboRepository
import ru.yandex.market.dsm.domain.employer.model.EmployerType
import ru.yandex.market.dsm.test.AssertsEmployerFactory
import ru.yandex.market.dsm.test.TestUtil
import java.util.UUID
import java.util.stream.IntStream

class EmployerQueryServiceTest : AbstractTest() {
    @Autowired
    private lateinit var employerQueryService: EmployerQueryService
    @Autowired
    private lateinit var employerDboRepository: EmployerDboRepository
    @Autowired
    private lateinit var employersTestFactory: EmployersTestFactory

    @Test
    fun `findByLogin - notFoundEmployer`() {
        assertThat(employerQueryService.findByLogin("notExistedLogin")).isNull()
    }

    @Test
    fun `findByLogin - findExistedEmployer`() {
        //given
        val newEmployerDbo = TestUtil.OBJECT_GENERATOR.nextObject(EmployerDbo::class.java)
        employerDboRepository.save(newEmployerDbo);

        //when
        val foundByLoginProjection = employerQueryService.findByLogin(newEmployerDbo.login)

        //then
        assertThat(foundByLoginProjection).isNotNull;
        AssertsEmployerFactory.asserts(newEmployerDbo, foundByLoginProjection!!)
    }

    @Sql("classpath:truncate.sql")
    @Test
    fun `search all employers`() {
        //given
        IntStream.range(0, 11).forEach { i -> employersTestFactory.createAndSave() }

        //when
        val searchResponse = employerQueryService.findAll(
            EmployerSearchParams(
                pageNumber = 0,
                pageSize = 10
            )
        )

        //then
        assertThat(searchResponse.content).isNotNull
        assertThat(searchResponse.totalPages).isEqualTo(2)
        assertThat(searchResponse.content).hasSize(10)
    }

    @Test
    fun `search employers by name`() {
        //given
        val createdEmployer = employersTestFactory.createAndSave()
        IntStream.range(0, 9).forEach { employersTestFactory.createAndSave() }

        //when
        val searchResponse = employerQueryService.findAll(
            EmployerSearchParams(
                pageNumber = 0,
                pageSize = 10,
                name = createdEmployer.name,
            )
        )

        //then
        assertThat(searchResponse.content).isNotNull
        assertThat(searchResponse.totalPages).isEqualTo(1)
        assertThat(searchResponse.content).hasSize(1)
        assertThat(searchResponse.content).contains(
            createdEmployer
        )
    }

    @Test
    fun `search employers by name substring`() {
        //given
        val createdEmployer1 = employersTestFactory.createAndSave(name = "asdf Employer 1")
        val createdEmployer2 = employersTestFactory.createAndSave(name = "nfgy emploYER 2")
        employersTestFactory.createAndSave(name = "some random name")

        //when
        val searchResponse = employerQueryService.findAll(
            EmployerSearchParams(
                pageNumber = 0,
                pageSize = 10,
                nameSubstring = "empLOyer",
            )
        )

        //then
        assertThat(searchResponse.content).isNotNull
        assertThat(searchResponse.totalPages).isEqualTo(1)
        assertThat(searchResponse.content).hasSize(2)
        assertThat(searchResponse.content).contains(
            createdEmployer1, createdEmployer2
        )
    }

    @Test
    fun `search employers by login`() {
        //given
        val createdEmployer = employersTestFactory.createAndSave()
        IntStream.range(0, 11).forEach { i -> employersTestFactory.createAndSave() }

        //when
        val searchResponse = employerQueryService.findAll(
            EmployerSearchParams(
                pageNumber = 0,
                pageSize = 10,
                login = createdEmployer.login,
            )
        )

        //then
        assertThat(searchResponse.content).isNotNull
        assertThat(searchResponse.totalPages).isEqualTo(1)
        assertThat(searchResponse.content).hasSize(1)
        assertThat(searchResponse.content).containsExactly(createdEmployer)
    }

    @Test
    fun `search employers by cabinet mbi id`() {
        //given
        val createdEmployer = employersTestFactory.createAndSave()
        IntStream.range(0, 5).forEach { i -> employersTestFactory.createAndSave() }

        //when
        val searchResponse = employerQueryService.findAll(
            EmployerSearchParams(
                pageNumber = 0,
                pageSize = 10,
                cabinetMbiId = createdEmployer.companyCabinetMbiId,
            )
        )

        //then
        assertThat(searchResponse.content).isNotNull
        assertThat(searchResponse.totalPages).isEqualTo(1)
        assertThat(searchResponse.content).hasSize(1)
        assertThat(searchResponse.content).containsExactly(createdEmployer)
    }

    @Sql("classpath:truncate.sql")
    @Test
    fun `search employers by type`() {
        //given
        val createdEmployer = employersTestFactory.createAndSave(type = EmployerType.LINEHAUL)
        IntStream.range(0, 11).forEach { i -> employersTestFactory.createAndSave(type = EmployerType.SUPPLY) }

        //when
        val searchResponse = employerQueryService.findAll(
            EmployerSearchParams(
                pageNumber = 0,
                pageSize = 10,
                type = createdEmployer.type,
            )
        )

        //then
        assertThat(searchResponse.content).isNotNull
        assertThat(searchResponse.totalPages).isEqualTo(1)
        assertThat(searchResponse.content).hasSize(1)
        assertThat(searchResponse.content).containsExactly(createdEmployer)
    }

    @Test
    fun `search employers - empty result`() {
        //given
        IntStream.range(0, 11).forEach { i -> employersTestFactory.createAndSave() }

        //when
        val searchResponse = employerQueryService.findAll(
            EmployerSearchParams(
                pageNumber = 0,
                pageSize = 10,
                name = UUID.randomUUID().toString(),
            )
        )

        //then
        assertThat(searchResponse.content).isNotNull
        assertThat(searchResponse.totalPages).isEqualTo(0)
        assertThat(searchResponse.content).hasSize(0)
    }

    @Sql("classpath:truncate.sql")
    @Test
    fun `search employers by active`() {
        //given
        val createdEmployer = employersTestFactory.createAndSave(active = true)
        IntStream.range(0, 11).forEach { i -> employersTestFactory.createAndSave(active = false) }

        //when
        val searchResponse = employerQueryService.findAll(
            EmployerSearchParams(
                pageNumber = 0,
                pageSize = 10,
                active = createdEmployer.isActive,
            )
        )

        //then
        assertThat(searchResponse.content).isNotNull
        assertThat(searchResponse.totalPages).isEqualTo(1)
        assertThat(searchResponse.content).hasSize(1)
        assertThat(searchResponse.content).containsExactly(createdEmployer)
    }

    @Test
    fun `search employers by single id`() {
        //given
        val createdEmployer = employersTestFactory.createAndSave()
        IntStream.range(0, 5).forEach { i -> employersTestFactory.createAndSave() }

        //when
        val searchResponse = employerQueryService.findAll(
            EmployerSearchParams(
                pageNumber = 0,
                pageSize = 10,
                ids = listOf(createdEmployer.id)
            )
        )

        //then
        assertThat(searchResponse.content).isNotNull
        assertThat(searchResponse.totalPages).isEqualTo(1)
        assertThat(searchResponse.content).hasSize(1)
        assertThat(searchResponse.content).containsExactly(createdEmployer)
    }


    @Test
    fun `search employers by multiple ids`() {
        //given
        val createdEmployer1 = employersTestFactory.createAndSave()
        val createdEmployer2 = employersTestFactory.createAndSave()
        IntStream.range(0, 5).forEach { i -> employersTestFactory.createAndSave() }

        //when
        val searchResponse = employerQueryService.findAll(
            EmployerSearchParams(
                pageNumber = 0,
                pageSize = 10,
                ids = listOf(createdEmployer1.id, createdEmployer2.id)
            )
        )

        //then
        assertThat(searchResponse.content).isNotNull
        assertThat(searchResponse.totalPages).isEqualTo(1)
        assertThat(searchResponse.content).hasSize(2)
        assertThat(searchResponse.content).containsExactlyInAnyOrder(createdEmployer1, createdEmployer2)
    }
}
