package ru.yandex.market.dsm.domain.nationality.service

import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.nationality.db.NationalityDbo
import ru.yandex.market.dsm.domain.nationality.db.NationalityDboRepository

class NationalityServiceTest : AbstractTest() {
    @Autowired
    private lateinit var nationalityDboRepository: NationalityDboRepository

    @Autowired
    private lateinit var nationalityService: NationalityService

    @Sql("classpath:truncate.sql")
    @Test
    fun testService() {
        val before = nationalityService.getNationalities()
        val expected = mutableListOf("RUS")
        before.forEach { expected.add(it) }

        nationalityDboRepository.save(
            NationalityDbo(
                "10283759",
                "RUS"
            )
        )

        val nationalities = nationalityService.getNationalities()
        assertThat(nationalities).hasSize(expected.size)

        assertThat(nationalities).containsAll(expected)
    }
}

