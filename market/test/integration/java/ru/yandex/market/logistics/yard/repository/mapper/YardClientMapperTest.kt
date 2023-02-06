package ru.yandex.market.logistics.yard.repository.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClient
import ru.yandex.market.logistics.yard_v2.repository.mapper.YardClientMapper
import java.time.Clock
import java.time.LocalDateTime

class YardClientMapperTest(
    @Autowired val mapper: YardClientMapper,
    @Autowired val clock: Clock) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_client/before.xml"])
    fun getById() {
        val client = mapper.getFullById(1)

        assertions().assertThat(client?.id).isEqualTo(1)
        assertions().assertThat(client?.name).isEqualTo("client")
        assertions().assertThat(client?.externalId).isEqualTo("extClient")
        assertions().assertThat(client?.phone).isEqualTo("123123123")
        assertions().assertThat(client?.stateId).isEqualTo(1000L)
        assertions().assertThat(client?.car?.id).isEqualTo(1L)
        assertions().assertThat(client?.documents).isNotEmpty

        val expectedDateTime = LocalDateTime.of(2021, 5, 1, 10, 0, 0)
        assertions().assertThat(client?.createdAt).isEqualTo(expectedDateTime)
        assertions().assertThat(client?.updatedAt).isEqualTo(expectedDateTime)

        val node = ObjectMapper().readTree("{\"requestType\": \"FOR_SHIPMENT\", \"testInfo2\": \"INFO2\"}")
        assertions().assertThat(client?.meta).isEqualTo(node)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_client/register/before.xml"])
    @ExpectedDatabase(value = "classpath:fixtures/repository/yard_client/register/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persist() {
        mapper.persist(YardClient(null, "name_", "id_", 1, "phone_", 1000,
            LocalDateTime.now(clock), LocalDateTime.now(clock)))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_client/register/before.xml"])
    fun getByExternalIdAndServiceIdSuccess() {
        assertions().assertThat(mapper.getByExternalIdAndServiceId("extClient", 1)?.id).isEqualTo(0)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_client/register/before.xml"])
    fun getByExternalIdAndServiceIdFail() {
        assertions().assertThat(mapper.getByExternalIdAndServiceId("ext", 1)).isNull()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_client/before.xml"])
    fun findAllNotInQueue() {
        val all = mapper.findAllNotInQueue(listOf(1000L))
        val ids = all.map { it.id }.toSet()

        assertions().assertThat(all.size).isEqualTo(1)
        assertions().assertThat(ids).contains(2)
    }
}
