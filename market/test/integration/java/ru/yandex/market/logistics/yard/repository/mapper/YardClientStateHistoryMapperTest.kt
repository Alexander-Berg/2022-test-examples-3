package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.StateEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClientStateHistoryEntity
import ru.yandex.market.logistics.yard_v2.facade.YardClientStateHistoryFacade
import ru.yandex.market.logistics.yard_v2.repository.mapper.YardClientStateHistoryMapper
import java.time.Clock
import java.time.LocalDateTime

class YardClientStateHistoryMapperTest(@Autowired private val clientStateHistoryMapper: YardClientStateHistoryMapper,
                                       @Autowired private val clock : Clock) :
    AbstractSecurityMockedContextualTest() {


    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/client_state_history/1/before.xml"])
    fun getById() {
        val history = clientStateHistoryMapper.getById(1)
        assertions().assertThat(history?.id).isEqualTo(1)
        assertions().assertThat(history?.stateId).isEqualTo(1000)
        assertions().assertThat(history?.yardClientId).isEqualTo(0)
        assertions().assertThat(history?.createdAt).isEqualTo(LocalDateTime.of(2021, 5, 1, 10, 0,0))

    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/client_state_history/2/before.xml"])
    @ExpectedDatabase(value = "classpath:fixtures/repository/client_state_history/2/after.xml" ,
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persist() {
        clientStateHistoryMapper.persist(YardClientStateHistoryEntity(
            yardClientId = 0,
            stateId = 1000,
            createdAt = LocalDateTime.now(clock)))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/client_state_history/3/before.xml"])
    fun getCurrentStateHistoryEntity() {
        val entity = clientStateHistoryMapper.getCurrentStateHistoryEntity(0)
        assertions().assertThat(entity!!.id).isEqualTo(1)
    }
}
