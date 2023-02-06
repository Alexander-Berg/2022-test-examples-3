package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.repository.mapper.ClientProcessingDurationMapper

class ClientProcessingDurationMapperTest(@Autowired val mapper: ClientProcessingDurationMapper) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/client_processing_duration/before.xml"])
    @ExpectedDatabase(
        "classpath:fixtures/repository/client_processing_duration/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun update() {
        mapper.calculate(100)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/client_processing_duration/after.xml"])
    @ExpectedDatabase(
        "classpath:fixtures/repository/client_processing_duration/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun delete() {
        mapper.delete()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/client_processing_duration/after.xml"])
    @ExpectedDatabase(
        "classpath:fixtures/repository/client_processing_duration/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun findBySiteId() {
        val durationEntity = mapper.findByServiceIdAndSiteId(2001003, 200)

        assertions().assertThat(durationEntity?.siteId).isEqualTo(200)
        assertions().assertThat(durationEntity?.serviceId).isEqualTo(2001003)
        assertions().assertThat(durationEntity?.duration).isEqualTo(86400)
    }
}
