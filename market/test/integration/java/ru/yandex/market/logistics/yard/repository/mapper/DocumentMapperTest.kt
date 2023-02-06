package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.DocumentEntity
import ru.yandex.market.logistics.yard_v2.repository.mapper.DocumentMapper
import java.time.Clock

class DocumentMapperTest(
    @Autowired val mapper: DocumentMapper,
    @Autowired val clock: Clock) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/document/before.xml"])
    fun getById() {
        val document = mapper.getById(1)

        assertions().assertThat(document?.id).isEqualTo(1)
        assertions().assertThat(document?.yardClientId).isEqualTo(1)
        assertions().assertThat(document?.externalId).isEqualTo("test_external_id_1")
        assertions().assertThat(document?.name).isEqualTo("test document 1")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/document/before.xml"])
    fun getByClientId() {
        val documentList = mapper.getByClientId(2)

        assertions().assertThat(documentList).isNotEmpty

        val document = documentList[0]

        assertions().assertThat(document.id).isEqualTo(2)
        assertions().assertThat(document.yardClientId).isEqualTo(2)
        assertions().assertThat(document.externalId).isEqualTo("test_external_id_2")
        assertions().assertThat(document.name).isEqualTo("test document 2")
    }


    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/document/persist/before.xml"])
    @ExpectedDatabase(value = "classpath:fixtures/repository/document/persist/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persist() {
        mapper.persist(DocumentEntity(null, 2, "test_external_id_3", "test document 3"))
    }
}
