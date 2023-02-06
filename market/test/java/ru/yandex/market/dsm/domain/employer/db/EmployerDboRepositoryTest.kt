package ru.yandex.market.dsm.domain.employer.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.employer.command.EmployerBaseCommand
import ru.yandex.market.dsm.domain.employer.model.EmployerLegalForm
import ru.yandex.market.dsm.domain.employer.model.EmployerType
import ru.yandex.market.dsm.test.TestUtil
import java.util.UUID

class EmployerDboRepositoryTest : AbstractTest() {

    @Autowired
    private lateinit var repository: EmployerDboRepository

    @Test
    fun `save - success`() {
        val expected = TestUtil.OBJECT_GENERATOR.nextObject(EmployerDbo::class.java)
        val id = expected.id

        repository.save(expected)

        val resultOpt = repository.findById(id)

        assertThat(resultOpt.isPresent).isTrue
        val result = resultOpt.get()

        assertThat(result.id).isEqualTo(expected.id)
        assertThat(result.createdAt).isNotNull
        assertThat(result.updatedAt).isNotNull
        assertThat(result.name).isEqualTo(expected.name)
    }
}
